"""
DAA Music Recommender — Recommendation Correctness Comparison
=============================================================
So sánh kết quả gợi ý của các thuật toán với BruteForce làm chuẩn.

Cách dùng:
    Đặt tất cả file CSV cùng thư mục với script này, rồi chạy:
        python compare_recommendations.py

Output:
    outputs/01_match_rate_bar.png         — % khớp theo từng thuật toán (bar)
    outputs/02_per_user_heatmap.png       — heatmap match rate theo user × algo
    outputs/03_strict_vs_setmatch.png     — so sánh strict order vs set match
    outputs/04_pruning_caching_check.png  — kiểm tra 100% cho Pruning & Caching
    outputs/summary_comparison.csv        — bảng tổng hợp
"""

import os
import sys
import glob
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.colors import LinearSegmentedColormap

# ── Config ───────────────────────────────────────────────────────────────────
OUT_DIR      = "outputs"
BRUTE_FILE   = "recommendations_BruteForce.csv"
STRICT_CHECK = ["Pruning", "Caching"]   # các algo BẮT BUỘC phải 100%

os.makedirs(OUT_DIR, exist_ok=True)

STYLE = {
    "figure.facecolor" : "#FFFFFF",
    "axes.facecolor"   : "#FFFFFF",
    "axes.edgecolor"   : "#CCCCCC",
    "axes.labelcolor"  : "#222222",
    "axes.titlecolor"  : "#111111",
    "xtick.color"      : "#444444",
    "ytick.color"      : "#444444",
    "text.color"       : "#222222",
    "grid.color"       : "#E0E0E0",
    "grid.linewidth"   : 0.6,
    "legend.facecolor" : "#FFFFFF",
    "legend.edgecolor" : "#CCCCCC",
}
plt.rcParams.update(STYLE)
plt.rcParams["font.family"] = "DejaVu Sans"

PALETTE = {
    "Pruning"   : "#F4A261",
    "Caching"   : "#457B9D",
    "Greedy"    : "#2A9D8F",
    "Heuristic" : "#9B5DE5",
}
DEFAULT_COLOR = "#AAAAAA"

def algo_color(name):
    for k, v in PALETTE.items():
        if k.lower() in name.lower():
            return v
    return DEFAULT_COLOR

def save(fig, name):
    path = os.path.join(OUT_DIR, name)
    fig.savefig(path, dpi=150, bbox_inches="tight")
    print(f"  ✔  Saved  {path}")
    plt.close(fig)


# ── Load ─────────────────────────────────────────────────────────────────────
def load_csv(path):
    df = pd.read_csv(path, sep="|")
    df.columns = df.columns.str.strip()
    return df

print("Loading BruteForce baseline...")
if not os.path.exists(BRUTE_FILE):
    print(f"[ERROR] Không tìm thấy file: {BRUTE_FILE}")
    sys.exit(1)

brute = load_csv(BRUTE_FILE)

# Tìm tất cả file CSV còn lại (không phải BruteForce)
other_files = [f for f in glob.glob("recommendations_*.csv")
               if "BruteForce" not in f]

if not other_files:
    print("[ERROR] Không tìm thấy file CSV nào ngoài BruteForce.")
    sys.exit(1)

print(f"Found {len(other_files)} file(s) to compare: {other_files}\n")


# ── Build per-user recommendation sets from BruteForce ───────────────────────
def build_user_recs(df):
    """{ user -> list of (Artist, Track) in order }"""
    result = {}
    for user, group in df.groupby("User"):
        result[user] = list(zip(group["Artist"], group["Track"]))
    return result

brute_recs = build_user_recs(brute)
all_users  = set(brute_recs.keys())


# ── Comparison metrics ────────────────────────────────────────────────────────
def compare(target_recs, brute_recs):
    """
    Returns per-user metrics dict:
      strict_match  : True/False — thứ tự và nội dung giống hệt
      set_match_pct : % bài trùng nhau (không quan tâm thứ tự)
      top1_match    : bài #1 có trùng không
    """
    rows = []
    for user in sorted(all_users):
        b = brute_recs.get(user, [])
        t = target_recs.get(user, [])
        if not b:
            continue

        strict = (b == t)

        b_set = set(b)
        t_set = set(t)
        overlap = len(b_set & t_set)
        set_pct = overlap / len(b_set) * 100 if b_set else 0.0

        top1 = (b[0] == t[0]) if (b and t) else False

        rows.append({
            "user"          : user,
            "strict_match"  : strict,
            "set_match_pct" : round(set_pct, 1),
            "top1_match"    : top1,
        })
    return pd.DataFrame(rows)


# ── Run comparisons ───────────────────────────────────────────────────────────
summary_rows = []
all_per_user = {}   # algo_name -> per-user DataFrame

for fpath in sorted(other_files):
    algo_name = (os.path.basename(fpath)
                 .replace("recommendations_", "")
                 .replace(".csv", ""))
    df_algo = load_csv(fpath)
    target_recs = build_user_recs(df_algo)
    per_user = compare(target_recs, brute_recs)
    all_per_user[algo_name] = per_user

    strict_pct  = per_user["strict_match"].mean()  * 100
    set_avg     = per_user["set_match_pct"].mean()
    top1_pct    = per_user["top1_match"].mean()     * 100
    n_users     = len(per_user)
    must_100    = any(s.lower() in algo_name.lower() for s in STRICT_CHECK)
    status      = "[PASS]" if (not must_100 or strict_pct == 100) else "[FAIL]"

    summary_rows.append({
        "Algorithm"           : algo_name,
        "Users compared"      : n_users,
        "Strict match (%)"    : round(strict_pct, 2),
        "Set match avg (%)"   : round(set_avg, 2),
        "Top-1 match (%)"     : round(top1_pct, 2),
        "Must be 100%"        : must_100,
        "Status"              : status,
    })
    print(f"  {algo_name:20s} | strict={strict_pct:6.2f}% | set={set_avg:6.2f}% | top1={top1_pct:6.2f}% | {status}")

summary_df = pd.DataFrame(summary_rows)
summary_df.to_csv(os.path.join(OUT_DIR, "summary_comparison.csv"), index=False)
print(f"\n  ✔  Saved  {os.path.join(OUT_DIR, 'summary_comparison.csv')}\n")

algos = [r["Algorithm"] for r in summary_rows]


# ══════════════════════════════════════════════════════════════════════════════
# Chart 1 — Match rate overview (Strict vs Set)
# ══════════════════════════════════════════════════════════════════════════════
fig, ax = plt.subplots(figsize=(max(8, len(algos) * 1.6), 5))

x      = np.arange(len(algos))
width  = 0.35
colors = [algo_color(a) for a in algos]

bars1 = ax.bar(x - width/2,
               [r["Strict match (%)"] for r in summary_rows],
               width, label="Strict match (thứ tự + nội dung)",
               color=colors, alpha=0.95, zorder=3)
bars2 = ax.bar(x + width/2,
               [r["Set match avg (%)"] for r in summary_rows],
               width, label="Set match avg (chỉ nội dung)",
               color=colors, alpha=0.45, zorder=3, hatch="//")

# Thêm số lên bar
for bar in bars1:
    h = bar.get_height()
    ax.text(bar.get_x() + bar.get_width()/2, h + 1,
            f"{h:.1f}%", ha="center", va="bottom", fontsize=8, fontweight="bold")
for bar in bars2:
    h = bar.get_height()
    ax.text(bar.get_x() + bar.get_width()/2, h + 1,
            f"{h:.1f}%", ha="center", va="bottom", fontsize=8, color="#555555")

# Gạch đỏ 100%
ax.axhline(100, color="#E63946", linewidth=1.2, linestyle="--", label="100% (perfect)", zorder=2)

ax.set_xticks(x)
ax.set_xticklabels(algos, rotation=15, ha="right", fontsize=10)
ax.set_ylim(0, 115)
ax.set_ylabel("Match rate (%)", fontsize=11)
ax.set_title("So sánh kết quả gợi ý với BruteForce (chuẩn)", fontsize=13, fontweight="bold", pad=12)
ax.grid(axis="y", zorder=0)
ax.legend(fontsize=9, loc="lower right")
fig.tight_layout()
save(fig, "01_match_rate_bar.png")


# ══════════════════════════════════════════════════════════════════════════════
# Chart 2 — Per-user set match heatmap
# ══════════════════════════════════════════════════════════════════════════════
# Lấy tối đa 60 user để heatmap không quá dày
sample_users = sorted(all_users)[:60]

heatmap_data = []
for algo in algos:
    pu = all_per_user[algo].set_index("user")
    row = [pu.loc[u, "set_match_pct"] if u in pu.index else np.nan
           for u in sample_users]
    heatmap_data.append(row)

hm = np.array(heatmap_data, dtype=float)

cmap = LinearSegmentedColormap.from_list("rg", ["#E63946", "#FFD166", "#06D6A0"])

fig, ax = plt.subplots(figsize=(max(14, len(sample_users) * 0.22), max(3, len(algos) * 0.65)))
im = ax.imshow(hm, aspect="auto", cmap=cmap, vmin=0, vmax=100)

ax.set_yticks(range(len(algos)))
ax.set_yticklabels(algos, fontsize=9)
ax.set_xticks(range(len(sample_users)))
ax.set_xticklabels([u.replace("user_", "") for u in sample_users],
                   rotation=90, fontsize=6)
ax.set_title(f"Set match rate (%) theo user × thuật toán  [hiển thị {len(sample_users)} users đầu]",
             fontsize=12, fontweight="bold", pad=10)

cbar = fig.colorbar(im, ax=ax, fraction=0.02, pad=0.02)
cbar.set_label("Set match (%)", fontsize=9)
fig.tight_layout()
save(fig, "02_per_user_heatmap.png")


# ══════════════════════════════════════════════════════════════════════════════
# Chart 3 — Strict vs Set vs Top-1 so sánh
# ══════════════════════════════════════════════════════════════════════════════
metrics    = ["Strict match (%)", "Set match avg (%)", "Top-1 match (%)"]
metric_lbl = ["Strict\n(order+content)", "Set match\n(content only)", "Top-1\nmatch"]
n_metrics  = len(metrics)

fig, axes = plt.subplots(1, n_metrics, figsize=(n_metrics * max(4, len(algos)*0.8), 5),
                         sharey=False)

for ax_i, (met, lbl) in enumerate(zip(metrics, metric_lbl)):
    ax = axes[ax_i]
    vals   = [r[met] for r in summary_rows]
    colors = [algo_color(a) for a in algos]
    bars   = ax.bar(algos, vals, color=colors, alpha=0.88, zorder=3)

    for bar, v in zip(bars, vals):
        ax.text(bar.get_x() + bar.get_width()/2, v + 1,
                f"{v:.1f}%", ha="center", va="bottom", fontsize=8, fontweight="bold")

    ax.axhline(100, color="#E63946", linewidth=1.1, linestyle="--", zorder=2)
    ax.set_ylim(0, 115)
    ax.set_title(lbl, fontsize=10, fontweight="bold")
    ax.set_ylabel("%" if ax_i == 0 else "", fontsize=10)
    ax.set_xticks(range(len(algos)))
    ax.set_xticklabels(algos, rotation=20, ha="right", fontsize=9)
    ax.grid(axis="y", zorder=0)

fig.suptitle("So sánh ba loại metric (BruteForce = chuẩn 100%)",
             fontsize=13, fontweight="bold", y=1.02)
fig.tight_layout()
save(fig, "03_strict_vs_setmatch_vs_top1.png")


# ══════════════════════════════════════════════════════════════════════════════
# Chart 4 — Kiểm tra đặc biệt Pruning & Caching (bắt buộc 100%)
# ══════════════════════════════════════════════════════════════════════════════
strict_algos = [a for a in algos if any(s.lower() in a.lower() for s in STRICT_CHECK)]

if strict_algos:
    fig, axes = plt.subplots(1, len(strict_algos),
                             figsize=(len(strict_algos) * 5, 4.5),
                             squeeze=False)

    for col, algo in enumerate(strict_algos):
        ax  = axes[0][col]
        pu  = all_per_user[algo]
        n   = len(pu)
        n_ok   = pu["strict_match"].sum()
        #n_ok = pu["set_match_pct"].sum()
        n_fail = n - n_ok
        pct    = n_ok / n * 100 if n else 0

        wedge_colors = ["#06D6A0", "#E63946"] if n_fail > 0 else ["#06D6A0", "#EEEEEE"]
        sizes  = [n_ok, n_fail] if n_fail > 0 else [n_ok, 0]
        labels = [f"Khớp\n{n_ok}/{n}", f"Khác\n{n_fail}/{n}"] if n_fail > 0 else [f"Khớp\n{n_ok}/{n}", ""]

        wedges, texts = ax.pie(
            [max(s, 1e-9) for s in sizes],
            labels=labels,
            colors=wedge_colors,
            startangle=90,
            wedgeprops={"edgecolor": "white", "linewidth": 2},
            textprops={"fontsize": 10},
        )

        status_txt = "PASS -- 100% khop!" if n_fail == 0 else f"FAIL -- {n_fail} user sai"
        status_col = "#06D6A0" if n_fail == 0 else "#E63946"

        ax.set_title(f"{algo}\n{status_txt}", fontsize=11, fontweight="bold",
                     color=status_col, pad=8)
        ax.text(0, -1.35, f"Strict match: {pct:.2f}%",
                ha="center", fontsize=10, color=status_col, fontweight="bold")

    fig.suptitle("Kiểm tra bắt buộc: Pruning & Caching phải đạt 100% strict match",
                 fontsize=13, fontweight="bold", y=1.02)
    fig.tight_layout()
    save(fig, "04_pruning_caching_strict_check.png")
else:
    print("  (Không có algo Pruning/Caching nào để kiểm tra chart 4)")


# ══════════════════════════════════════════════════════════════════════════════
# Print summary table
# ══════════════════════════════════════════════════════════════════════════════
print("\n" + "="*75)
print("SUMMARY TABLE")
print("="*75)
print(summary_df.to_string(index=False))
print("="*75)
print("\nDone! Tất cả chart đã được lưu vào thư mục:", OUT_DIR)