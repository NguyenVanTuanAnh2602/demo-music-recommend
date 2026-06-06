"""
DAA Music Recommender — Evaluation Metrics Visualization
=========================================================
Reads evaluation_metrics_test2.csv and generates multiple charts:
  1.  Execution time (ms) — Algorithm × Scenario  [grouped bar]
  2.  Total operations    — Algorithm × Scenario  [grouped bar]
  3.  Memory usage (KB)  — Algorithm × Scenario  [grouped bar]
  4.  Scalability        — Exec time vs InputSize per algorithm  [line]
  5.  Ops breakdown      — Similarity / Heap / Score per algo (large) [stacked bar]
  6.  Recovery rate (%)  — All configs  [heatmap]
  7.  Cache performance  — CachingRecommender only  [grouped bar]
  8.  Speed-up vs BruteForce — relative exec time  [bar]

All charts saved to  ./output/  folder.
"""

import os
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import numpy as np
from matplotlib.gridspec import GridSpec

# ── Config ──────────────────────────────────────────────────────────────────
INPUT_CSV = "evaluation_metrics_test2.csv"
OUT_DIR   = "outputs"
os.makedirs(OUT_DIR, exist_ok=True)

# Colour palette (one per unique algo/version combo)
PALETTE = {
    "BruteForceRecommender/V1_BruteForce"      : "#E63946",
    "PruningRecommender/V1_Pruning"            : "#F4A261",
    #"TopKRecommender/V1_NaiveSort"             : "#2A9D8F",
    #"TopKRecommender/V2_MinHeap"               : "#1D6A63",
    "GreedyRecommender/V1_GreedyNeighbour"     : "#2A9D8F", ##FAC3D2
    "GreedyRecommender/V2_GreedySong"          : "#1D6A63", #E4ADC5
    "CachingRecommender/V1_TopDownMemo"        : "#457B9D",
    "CachingRecommender/V2_BottomUpTable" : "#1D3557",
    "HeuristicRecommender/V1_FilterOverlap"    : "#9B5DE5",
    "HeuristicRecommender/V2_InvertedIndex"    : "#5A189A",
}

SCENARIO_ORDER = ["small", "medium", "large"]
SCENARIO_LABELS = {"small": "Small (N=50)", "medium": "Medium (N=100)", "large": "Large (N=500)"}

STYLE = {
    "figure.facecolor"  : "#FFFFFF", #0F1117
    "axes.facecolor"    : "#FFFFFF", #161B22
    "axes.edgecolor"    : "#30363D",
    "axes.labelcolor"   : "#000000", #C9D1D9
    "axes.titlecolor"   : "#000000", #E6EDF3
    "xtick.color"       : "#000000", #8B949E
    "ytick.color"       : "#000000", #8B949E
    "text.color"        : "#000000", #C9D1D9
    "grid.color"        : "#C6C6C6", #21262D
    "grid.linewidth"    : 0.6,
    "legend.facecolor"  : "#FFFFFF", #161B22
    "legend.edgecolor"  : "#30363D",
}
plt.rcParams.update(STYLE)
plt.rcParams["font.family"] = "DejaVu Sans"

# ── Load data ────────────────────────────────────────────────────────────────
df = pd.read_csv(INPUT_CSV, sep="|")
df.columns = df.columns.str.strip()
df["AlgoVersion"] = df["Algorithm"] + "/" + df["Version"]
df["Scenario"]    = pd.Categorical(df["Scenario"], categories=SCENARIO_ORDER, ordered=True)
df = df.sort_values(["AlgoVersion", "Scenario"])

combos  = list(df["AlgoVersion"].unique())
n_combo = len(combos)


# ── Helper: save figure ──────────────────────────────────────────────────────
def save(fig, name):
    path = os.path.join(OUT_DIR, name)
    fig.savefig(path, dpi=150, bbox_inches="tight", facecolor=fig.get_facecolor())
    print(f"  ✔  Saved  {path}")
    plt.close(fig)


# ════════════════════════════════════════════════════════════════════════════
# Chart 1 — Execution Time per Algorithm × Scenario
# ════════════════════════════════════════════════════════════════════════════
def chart_exec_time():
    fig, ax = plt.subplots(figsize=(14, 6))
    fig.patch.set_facecolor("#FFFFFF") #0F1117

    x       = np.arange(len(SCENARIO_ORDER))
    width   = 0.8 / n_combo
    offsets = np.linspace(-(0.8 - width) / 2, (0.8 - width) / 2, n_combo)

    for i, av in enumerate(combos):
        sub  = df[df["AlgoVersion"] == av].sort_values("Scenario")
        vals = [sub[sub["Scenario"] == s]["ExecTime(ms)"].values[0] if len(sub[sub["Scenario"] == s]) else 0
                for s in SCENARIO_ORDER]
        color = PALETTE.get(av, "#888")
        bars = ax.bar(x + offsets[i], vals, width=width*0.9,
                      color=color, alpha=0.88, label=av.split("/")[1],
                      zorder=3)

    ax.set_xticks(x)
    ax.set_xticklabels([SCENARIO_LABELS[s] for s in SCENARIO_ORDER])
    ax.set_ylabel("Execution Time (ms)")
    ax.set_title("Execution Time — All Algorithms × Scenario", fontsize=14, fontweight="bold", pad=12)
    ax.yaxis.grid(True, zorder=0)
    ax.set_axisbelow(True)

    # Custom legend showing full algo name grouped
    handles = [mpatches.Patch(color=PALETTE[av], label=av) for av in combos]
    ax.legend(handles=handles, bbox_to_anchor=(1.01, 1), loc="upper left",
              fontsize=7.5, framealpha=0.7)

    save(fig, "1_exec_time_by_scenario.png")


# ════════════════════════════════════════════════════════════════════════════
# Chart 2 — Total Operations per Algorithm × Scenario
# ════════════════════════════════════════════════════════════════════════════
def chart_ops_total():
    fig, ax = plt.subplots(figsize=(14, 6))
    fig.patch.set_facecolor("#FFFFFF") #0F1117

    x       = np.arange(len(SCENARIO_ORDER))
    width   = 0.8 / n_combo
    offsets = np.linspace(-(0.8 - width) / 2, (0.8 - width) / 2, n_combo)

    for i, av in enumerate(combos):
        sub  = df[df["AlgoVersion"] == av].sort_values("Scenario")
        vals = [sub[sub["Scenario"] == s]["Ops_Total"].values[0] if len(sub[sub["Scenario"] == s]) else 0
                for s in SCENARIO_ORDER]
        color = PALETTE.get(av, "#888")
        ax.bar(x + offsets[i], vals, width=width*0.9,
               color=color, alpha=0.88, label=av, zorder=3)

    ax.set_xticks(x)
    ax.set_xticklabels([SCENARIO_LABELS[s] for s in SCENARIO_ORDER])
    ax.set_ylabel("Total Operations")
    ax.set_title("Total Operation Count — All Algorithms × Scenario", fontsize=14, fontweight="bold", pad=12)
    ax.yaxis.grid(True, zorder=0)
    ax.set_axisbelow(True)
    # Format y-axis with commas
    ax.yaxis.set_major_formatter(plt.FuncFormatter(lambda v, _: f"{v:,.0f}"))

    handles = [mpatches.Patch(color=PALETTE[av], label=av) for av in combos]
    ax.legend(handles=handles, bbox_to_anchor=(1.01, 1), loc="upper left",
              fontsize=7.5, framealpha=0.7)

    save(fig, "2_ops_total_by_scenario.png")


# ════════════════════════════════════════════════════════════════════════════
# Chart 3 — Memory Usage (KB) per Algorithm × Scenario
# ════════════════════════════════════════════════════════════════════════════
def chart_memory():
    fig, ax = plt.subplots(figsize=(14, 6))
    fig.patch.set_facecolor("#FFFFFF") #0F1117

    x       = np.arange(len(SCENARIO_ORDER))
    width   = 0.8 / n_combo
    offsets = np.linspace(-(0.8 - width) / 2, (0.8 - width) / 2, n_combo)

    for i, av in enumerate(combos):
        sub  = df[df["AlgoVersion"] == av].sort_values("Scenario")
        vals = [sub[sub["Scenario"] == s]["ExtraMemory(KB)"].values[0] if len(sub[sub["Scenario"] == s]) else 0
                for s in SCENARIO_ORDER]
        color = PALETTE.get(av, "#888")
        ax.bar(x + offsets[i], vals, width=width*0.9,
               color=color, alpha=0.88, label=av, zorder=3)

    ax.set_xticks(x)
    ax.set_xticklabels([SCENARIO_LABELS[s] for s in SCENARIO_ORDER])
    ax.set_ylabel("Extra Memory (KB)")
    ax.set_title("Memory Usage — All Algorithms × Scenario", fontsize=14, fontweight="bold", pad=12)
    ax.yaxis.grid(True, zorder=0)
    ax.set_axisbelow(True)
    ax.yaxis.set_major_formatter(plt.FuncFormatter(lambda v, _: f"{v:,.0f}"))

    handles = [mpatches.Patch(color=PALETTE[av], label=av) for av in combos]
    ax.legend(handles=handles, bbox_to_anchor=(1.01, 1), loc="upper left",
              fontsize=7.5, framealpha=0.7)

    save(fig, "3_memory_by_scenario.png")


# ════════════════════════════════════════════════════════════════════════════
# Chart 4 — Scalability: Exec Time vs Input Size (line per algo/version)
# ════════════════════════════════════════════════════════════════════════════
def chart_scalability():
    fig, axes = plt.subplots(1, 2, figsize=(16, 6))
    fig.patch.set_facecolor("#FFFFFF") #0F1117
    fig.suptitle("Scalability: Execution Time & Operations vs Input Size",
                 fontsize=14, fontweight="bold", color="#000000", y=1.01) #E6EDF3

    for ax, metric, label in zip(
        axes,
        ["ExecTime(ms)", "Ops_Total"],
        ["Execution Time (ms)", "Total Operations"]
    ):
        for av in combos:
            sub = df[df["AlgoVersion"] == av].sort_values("InputSize(N)")
            if sub.empty:
                continue
            color = PALETTE.get(av, "#000000") #888
            ax.plot(sub["InputSize(N)"], sub[metric],
                    marker="o", linewidth=2, markersize=6,
                    color=color, label=av, alpha=0.9)

        ax.set_xlabel("Input Size N (users)")
        ax.set_ylabel(label)
        ax.yaxis.grid(True, alpha=0.4)
        ax.set_axisbelow(True)
        ax.set_xticks([50, 100, 500])
        ax.yaxis.set_major_formatter(plt.FuncFormatter(lambda v, _: f"{v:,.0f}"))

    handles = [mpatches.Patch(color=PALETTE[av], label=av) for av in combos]
    axes[1].legend(handles=handles, bbox_to_anchor=(1.01, 1), loc="upper left",
                   fontsize=7.5, framealpha=0.7)
    axes[0].set_title("Exec Time Scaling", color="#000000") #C9D1D9
    axes[1].set_title("Ops Count Scaling", color="#000000") #C9D1D9

    plt.tight_layout()
    save(fig, "4_scalability_lines.png")


# ════════════════════════════════════════════════════════════════════════════
# Chart 5 — Operation Breakdown (Similarity / Heap / Score) — large scenario
# ════════════════════════════════════════════════════════════════════════════
def chart_ops_breakdown():
    sub = df[df["Scenario"] == "large"].copy()
    labels = sub["AlgoVersion"].tolist()

    sim   = sub["Ops_Similarity"].values
    heap  = sub["Ops_Heap"].values
    score = sub["Ops_Score"].values

    x = np.arange(len(labels))
    w = 0.55

    fig, ax = plt.subplots(figsize=(14, 7))
    fig.patch.set_facecolor("#FFFFFF") #0F1117

    b1 = ax.bar(x, sim,   width=w, color="#E63946", alpha=0.9, label="Similarity Ops", zorder=3)
    b2 = ax.bar(x, heap,  width=w, bottom=sim, color="#F4A261", alpha=0.9, label="Heap Ops", zorder=3)
    b3 = ax.bar(x, score, width=w, bottom=sim+heap, color="#2A9D8F", alpha=0.9, label="Score Ops", zorder=3)

    ax.set_xticks(x)
    ax.set_xticklabels([lbl.replace("/", "\n") for lbl in labels], fontsize=8)
    ax.set_ylabel("Operation Count")
    ax.set_title("Operation Breakdown per Algorithm — Large Scenario (N=500)",
                 fontsize=13, fontweight="bold", pad=12)
    ax.yaxis.grid(True, zorder=0)
    ax.set_axisbelow(True)
    ax.yaxis.set_major_formatter(plt.FuncFormatter(lambda v, _: f"{v:,.0f}"))
    ax.legend(loc="upper left", fontsize=9)

    save(fig, "5_ops_breakdown_large.png")


# ════════════════════════════════════════════════════════════════════════════
# Chart 6 — Recovery Rate heatmap  (AlgoVersion × Scenario)
# ════════════════════════════════════════════════════════════════════════════
def chart_recovery_heatmap():
    pivot = df.pivot_table(index="AlgoVersion", columns="Scenario",
                           values="RecoveryRate(%)", aggfunc="first")
    pivot = pivot[SCENARIO_ORDER]

    fig, ax = plt.subplots(figsize=(9, 7))
    fig.patch.set_facecolor("#FFFFFF") #0F1117

    data = pivot.values.astype(float)
    im   = ax.imshow(data, cmap="RdYlGn", vmin=0, vmax=100, aspect="auto")

    ax.set_xticks(range(len(SCENARIO_ORDER)))
    ax.set_xticklabels([SCENARIO_LABELS[s] for s in SCENARIO_ORDER])
    ax.set_yticks(range(len(pivot.index)))
    ax.set_yticklabels(pivot.index, fontsize=9)
    ax.set_title("Recovery Rate (%) — Algorithm × Scenario",
                 fontsize=13, fontweight="bold", pad=12)

    for i in range(data.shape[0]):
        for j in range(data.shape[1]):
            val = data[i, j]
            txt_color = "black" if val > 55 else "white"
            ax.text(j, i, f"{val:.1f}%", ha="center", va="center",
                    fontsize=10, fontweight="bold", color=txt_color)

    cbar = fig.colorbar(im, ax=ax, fraction=0.03, pad=0.04)
    cbar.set_label("Recovery Rate (%)", color="#C9D1D9")
    cbar.ax.yaxis.set_tick_params(color="#C9D1D9")

    save(fig, "6_recovery_rate_heatmap.png")


# ════════════════════════════════════════════════════════════════════════════
# Chart 7 — Cache performance: CachingRecommender only
# ════════════════════════════════════════════════════════════════════════════
def chart_cache():
    sub = df[df["Algorithm"] == "CachingRecommender"].copy()
    if sub.empty:
        print("  ⚠  No CachingRecommender rows — skipping cache chart.")
        return

    versions = sub["Version"].unique().tolist()
    x = np.arange(len(SCENARIO_ORDER))
    w = 0.35

    fig, axes = plt.subplots(1, 2, figsize=(14, 6))
    fig.patch.set_facecolor("#FFFFFF") #0F1117
    fig.suptitle("CachingRecommender — Cache Performance & Exec Time",
                 fontsize=13, fontweight="bold", color="#E6EDF3")

    # Left: Cache hits vs misses
    ax = axes[0]
    offsets = [-w/2, w/2]
    ver_colors = ["#457B9D", "#1D3557"]
    for vi, ver in enumerate(versions):
        vsub = sub[sub["Version"] == ver].sort_values("Scenario")
        hits   = [vsub[vsub["Scenario"] == s]["CacheHits"].values[0]   for s in SCENARIO_ORDER]
        misses = [vsub[vsub["Scenario"] == s]["CacheMisses"].values[0] for s in SCENARIO_ORDER]
        ax.bar(x + offsets[vi] - 0.1, hits,   width=w*0.45, color=ver_colors[vi], alpha=0.9,
               label=f"{ver} — Hits",   zorder=3)
        ax.bar(x + offsets[vi] + 0.1, misses, width=w*0.45, color=ver_colors[vi], alpha=0.45,
               label=f"{ver} — Misses", zorder=3, hatch="//")

    ax.set_xticks(x)
    ax.set_xticklabels([SCENARIO_LABELS[s] for s in SCENARIO_ORDER])
    ax.set_ylabel("Count")
    ax.set_title("Cache Hits vs Misses", color="#C9D1D9")
    ax.yaxis.grid(True, zorder=0)
    ax.set_axisbelow(True)
    ax.legend(fontsize=8)

    # Right: Exec time comparison between versions
    ax2 = axes[1]
    for vi, ver in enumerate(versions):
        vsub = sub[sub["Version"] == ver].sort_values("Scenario")
        times = [vsub[vsub["Scenario"] == s]["ExecTime(ms)"].values[0] for s in SCENARIO_ORDER]
        ax2.plot(SCENARIO_ORDER, times, marker="o", linewidth=2.5, markersize=8,
                 color=ver_colors[vi], label=ver)
        for si, t in enumerate(times):
            ax2.annotate(f"{t:.0f} ms", (SCENARIO_ORDER[si], t),
                         textcoords="offset points", xytext=(0, 8),
                         ha="center", fontsize=8, color=ver_colors[vi])

    ax2.set_ylabel("Execution Time (ms)")
    ax2.set_title("V1 TopDownMemo vs V2 BottomUpTable", color="#000000") #C9D1D9
    ax2.yaxis.grid(True, alpha=0.4)
    ax2.set_axisbelow(True)
    ax2.legend(fontsize=9)

    plt.tight_layout()
    save(fig, "7_caching_performance.png")


# ════════════════════════════════════════════════════════════════════════════
# Chart 8 — Speed-up relative to BruteForce (large scenario)
# ════════════════════════════════════════════════════════════════════════════
def chart_speedup():
    large = df[df["Scenario"] == "large"].copy()
    brute = large[large["AlgoVersion"] == "BruteForceRecommender/V1_BruteForce"]["ExecTime(ms)"].values
    if len(brute) == 0:
        print("  ⚠  BruteForce not found — skipping speed-up chart.")
        return
    brute_time = brute[0]

    others = large[large["AlgoVersion"] != "BruteForceRecommender/V1_BruteForce"].copy()
    others["SpeedUp"] = brute_time / others["ExecTime(ms)"]
    others = others.sort_values("SpeedUp", ascending=True)

    fig, ax = plt.subplots(figsize=(10, 6))
    fig.patch.set_facecolor("#FFFFFF") #0F1117

    colors = [PALETTE.get(av, "#888") for av in others["AlgoVersion"]]
    bars = ax.barh(others["AlgoVersion"], others["SpeedUp"], color=colors, alpha=0.9, zorder=3)

    ax.axvline(1.0, color="#E63946", linewidth=1.5, linestyle="--", alpha=0.7, label="BruteForce baseline")
    ax.set_xlabel("Speed-up factor (×)")
    ax.set_title(f"Speed-up vs BruteForce — Large Scenario (N=500)\nBruteForce: {brute_time:.1f} ms",
                 fontsize=13, fontweight="bold", pad=12)
    ax.xaxis.grid(True, zorder=0)
    ax.set_axisbelow(True)
    ax.legend(fontsize=9)

    for bar, val in zip(bars, others["SpeedUp"]):
        ax.text(val + 0.05, bar.get_y() + bar.get_height() / 2,
                f"{val:.2f}×", va="center", fontsize=9, color="#000000") #

    plt.tight_layout()
    save(fig, "8_speedup_vs_bruteforce.png")


# ════════════════════════════════════════════════════════════════════════════
# Run all
# ════════════════════════════════════════════════════════════════════════════
if __name__ == "__main__":
    print(f"\n DAA Music Recommender — Generating charts → {OUT_DIR}\n")
    chart_exec_time()
    chart_ops_total()
    chart_memory()
    chart_scalability()
    chart_ops_breakdown()
    chart_recovery_heatmap()
    chart_cache()
    chart_speedup()
    print("\n Done! All 8 charts saved.\n")