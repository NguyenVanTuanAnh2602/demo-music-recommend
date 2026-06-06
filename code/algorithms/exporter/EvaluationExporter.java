package exporter;

import benchmark.EvaluationResult;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EvaluationExporter {
    public static void exportCSV(String filePath, List<EvaluationResult> results) {
        ensureDir(filePath);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath))) {

            // Header
            w.write(String.join("|",
                    "Algorithm", "Version", "Scenario",
                    "InputSize(N)", "TopK",
                    "ExecTime(ns)", "ExecTime(ms)",
                    "Ops_Similarity", "Ops_Heap", "Ops_Score", "Ops_Total",
                    "CacheHits", "CacheMisses",
                    "ExtraMemory(KB)", "ExtraMemory(MB)",
                    "UsersWithRec", "TotalUsers", "RecoveryRate(%)",
                    "Pass/Fail"
            ));
            w.newLine();

            for (EvaluationResult r : results) {
                double recovery = r.getTotalUsersEvaluated() == 0 ? 0.0
                        : 100.0 * r.getUsersWithRecommendations() / r.getTotalUsersEvaluated();

                w.write(String.join("|",
                        sanitize(r.getAlgorithmName()),
                        sanitize(r.getVersionLabel()),
                        sanitize(r.getScenario()),
                        String.valueOf(r.getInputSize()),
                        String.valueOf(r.getTopK()),
                        String.valueOf(r.getExecTimeNs()),
                        String.format("%.3f", r.getExecTimeMs()),
                        String.valueOf(r.getOpCountSimilarity()),
                        String.valueOf(r.getOpCountHeap()),
                        String.valueOf(r.getOpCountScore()),
                        String.valueOf(r.getOpCountTotal()),
                        String.valueOf(r.getOpCountCacheHit()),
                        String.valueOf(r.getOpCountCacheMiss()),
                        String.format("%.2f", r.getExtraMemoryKB()),
                        String.format("%.4f", r.getExtraMemoryMB()),
                        String.valueOf(r.getUsersWithRecommendations()),
                        String.valueOf(r.getTotalUsersEvaluated()),
                        String.format("%.1f", recovery),
                        r.isPassed() ? "PASS" : "FAIL"
                ));
                w.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── TXT Report Export ─────────────────────────────────────────────
    public static void exportTXT(String filePath, List<EvaluationResult> results) {
        ensureDir(filePath);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filePath)))) {

            String now = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String bar  = "=".repeat(100);
            String dash = "-".repeat(100);

            pw.println(bar);
            pw.println("  ALGORITHM EVALUATION REPORT — DAA Music Recommender");
            pw.printf ("  Generated: %s%n", now);
            pw.println(bar);
            pw.println();

            // ── 1. Summary table ──────────────────────────────────────
            pw.println("┌─ 1. SUMMARY TABLE ─────────────────────────────────────────────────────────────────────────────┐");
            pw.println();
            pw.printf("  %-28s %-12s %-10s %14s %14s %14s %14s %10s%n",
                    "Algorithm+Version", "Scenario", "N (users)",
                    "Time(ms)", "Ops_Total", "Memory(KB)", "RecRate(%)", "Status");
            pw.println("  " + dash);

            for (EvaluationResult r : results) {
                double recovery = r.getTotalUsersEvaluated() == 0 ? 0.0
                        : 100.0 * r.getUsersWithRecommendations() / r.getTotalUsersEvaluated();
                pw.printf("  %-28s %-12s %-10d %14.3f %14d %14.2f %14.1f %10s%n",
                        truncate(r.getAlgorithmName() + "/" + r.getVersionLabel(), 28),
                        r.getScenario(),
                        r.getInputSize(),
                        r.getExecTimeMs(),
                        r.getOpCountTotal(),
                        r.getExtraMemoryKB(),
                        recovery,
                        r.isPassed() ? "PASS ✓" : "FAIL ✗");
            }
            pw.println("  " + dash);
            pw.println();

            // ── 2. Detailed per-algorithm breakdown ───────────────────
            pw.println("┌─ 2. DETAILED BREAKDOWN ────────────────────────────────────────────────────────────────────────┐");
            pw.println();

            // Group by algorithm name
            Map<String, List<EvaluationResult>> grouped = new LinkedHashMap<>();
            for (EvaluationResult r : results) {
                grouped.computeIfAbsent(r.getAlgorithmName(), k -> new ArrayList<>()).add(r);
            }

            for (Map.Entry<String, List<EvaluationResult>> algoGroup : grouped.entrySet()) {
                pw.println("  ╔══ Algorithm: " + algoGroup.getKey() + " " + "═".repeat(Math.max(0, 82 - algoGroup.getKey().length())) + "╗");
                pw.println();

                // Group by version
                Map<String, List<EvaluationResult>> byVersion = new LinkedHashMap<>();
                for (EvaluationResult r : algoGroup.getValue()) {
                    byVersion.computeIfAbsent(r.getVersionLabel(), k -> new ArrayList<>()).add(r);
                }

                for (Map.Entry<String, List<EvaluationResult>> verGroup : byVersion.entrySet()) {
                    pw.println("    ── Version: " + verGroup.getKey());
                    pw.println();

                    for (EvaluationResult r : verGroup.getValue()) {
                        double recovery = r.getTotalUsersEvaluated() == 0 ? 0
                                : 100.0 * r.getUsersWithRecommendations() / r.getTotalUsersEvaluated();

                        pw.printf("    [Scenario: %-8s]  N=%d  TopK=%d%n",
                                r.getScenario().toUpperCase(), r.getInputSize(), r.getTopK());
                        pw.println();

                        pw.println("      ┌── Kích thước đầu vào (Input Size) ──────────────────┐");
                        pw.printf ("      │  N (users in dataset)     : %d%n", r.getInputSize());
                        pw.printf ("      │  TopK recommendations     : %d%n", r.getTopK());
                        pw.printf ("      │  Users evaluated          : %d%n", r.getTotalUsersEvaluated());
                        pw.println("      └─────────────────────────────────────────────────────┘");
                        pw.println();

                        pw.println("      ┌── Thời gian thực thi (Execution Time) ──────────────┐");
                        pw.printf ("      │  Nanoseconds  (ns)        : %,d%n", r.getExecTimeNs());
                        pw.printf ("      │  Milliseconds (ms)        : %.3f ms%n", r.getExecTimeMs());
                        pw.printf ("      │  Microseconds (µs)        : %.1f µs%n", r.getExecTimeNs() / 1_000.0);
                        pw.println("      └─────────────────────────────────────────────────────┘");
                        pw.println();

                        pw.println("      ┌── Số đếm phép toán (Operation Count) ───────────────┐");
                        pw.printf ("      │  Similarity computations  : %,d%n", r.getOpCountSimilarity());
                        pw.printf ("      │  Heap operations          : %,d%n", r.getOpCountHeap());
                        pw.printf ("      │  Score accumulations      : %,d%n", r.getOpCountScore());
                        pw.printf ("      │  TOTAL operations         : %,d%n", r.getOpCountTotal());
                        if (r.getOpCountCacheMiss() + r.getOpCountCacheHit() > 0) {
                            pw.printf ("      │  Cache hits               : %,d%n", r.getOpCountCacheHit());
                            pw.printf ("      │  Cache misses             : %,d%n", r.getOpCountCacheMiss());
                            double hitRate = (r.getOpCountCacheMiss() + r.getOpCountCacheHit()) == 0 ? 0
                                    : 100.0 * r.getOpCountCacheHit() / (r.getOpCountCacheHit() + r.getOpCountCacheMiss());
                            pw.printf ("      │  Cache hit rate           : %.1f%%%n", hitRate);
                        }
                        pw.println("      └─────────────────────────────────────────────────────┘");
                        pw.println();

                        pw.println("      ┌── Bộ nhớ tiêu thụ (Memory Usage) ───────────────────┐");
                        pw.printf ("      │  Extra allocated (KB)     : %.2f KB%n", r.getExtraMemoryKB());
                        pw.printf ("      │  Extra allocated (MB)     : %.4f MB%n", r.getExtraMemoryMB());
                        pw.println("      └─────────────────────────────────────────────────────┘");
                        pw.println();

                        pw.println("      ┌── Tính đúng đắn (Correctness) ──────────────────────┐");
                        pw.printf ("      │  Users with results       : %d / %d%n",
                                r.getUsersWithRecommendations(), r.getTotalUsersEvaluated());
                        pw.printf ("      │  Recovery rate            : %.1f%%%n", recovery);
                        pw.printf ("      │  Status                   : %s%n",
                                r.isPassed() ? "PASS ✓  (majority users received recommendations)"
                                        : "FAIL ✗  (fewer than 50% users received recommendations)");
                        pw.println("      └─────────────────────────────────────────────────────┘");
                        pw.println();
                    }
                }
                pw.println();
            }

            // ── 3. Scenario comparison ────────────────────────────────
            pw.println("┌─ 3. SCENARIO ANALYSIS (Best / Worst / Average) ───────────────────────────────────────────────┐");
            pw.println();

            String[] scenarios = {"best", "worst", "average"};
            String[] scenarioLabels = {
                    "Best-case  (dữ liệu sparse — ít hàng xóm, ít phép tính)",
                    "Worst-case (dữ liệu dense — nhiều hàng xóm, nhiều phép tính)",
                    "Average    (dữ liệu ngẫu nhiên — subset đại diện)"
            };

            for (int si = 0; si < scenarios.length; si++) {
                String sc = scenarios[si];
                pw.println("  ── " + scenarioLabels[si]);
                pw.printf("  %-28s %14s %14s %14s%n",
                        "Algorithm/Version", "Time(ms)", "Ops_Total", "Memory(KB)");
                pw.println("  " + "-".repeat(72));

                for (EvaluationResult r : results) {
                    if (!r.getScenario().equalsIgnoreCase(sc)) continue;
                    pw.printf("  %-28s %14.3f %14d %14.2f%n",
                            truncate(r.getAlgorithmName() + "/" + r.getVersionLabel(), 28),
                            r.getExecTimeMs(),
                            r.getOpCountTotal(),
                            r.getExtraMemoryKB());
                }
                pw.println();
            }

            // ── 4. Complexity reference ───────────────────────────────
            pw.println("┌─ 4. COMPLEXITY REFERENCE ─────────────────────────────────────────────────────────────────────┐");
            pw.println();
            /*
            pw.println("  TopKRecommender");
            pw.println("    V1 NaiveSort        O(U²·I)    sort O(S log S)  — baseline brute force");
            pw.println("    V2 MinHeap          O(U·log K)                  — greedy, prune with min-heap");
            pw.println();

             */
            pw.println("  CachingRecommender");
            pw.println("    V1 TopDownMemo      O(U²) build, O(1) reuse    — lazy memoization per target user");
            pw.println("    V2 BottomUpTable    O(U²) build once, O(1) each— full DP table, best for repeated calls");
            pw.println();
            pw.println("  Similarity (dot product inner loop)");
            pw.println("    V1 Naive            O(N × M)");
            pw.println("    V2 HashMap lookup   O(N)");
            pw.println("    V3 Swap smaller     O(min(N, M))   ← currently used");
            pw.println();
            pw.println("  Scoring");
            pw.println("    V1 All songs        O(I × K)       I = total songs in system");
            pw.println("    V2 Neighbor-first   O(K × M)       M = avg songs per neighbor");
            pw.println("    V3 +merge/pruning   O(K × M)       ← currently used");
            pw.println();

            pw.println(bar);
            pw.println("  END OF REPORT");
            pw.println(bar);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────
    private static void ensureDir(String filePath) {
        File f = new File(filePath);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replace("|", " ").replace("\n", " ");
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }

}
