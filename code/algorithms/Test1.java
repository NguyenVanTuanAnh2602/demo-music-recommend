import algorithm.*;
import benchmark.*;
import data.*;
import exporter.*;
import java.util.*;

public class Test1 {
    private static final String DATA_FILE  = "../data/samples_csv/play_count_sample_500users.csv";
    private static final String OUTPUT_DIR = "output/";
    private static final int    TOP_K      = 5;
    private static final int    WARMUP_RUNS = 2;  

    // Scenario user counts
    // best    = small N 
    // average = medium N
    // worst   = full N  
    private static final int BEST_N    = 20;
    private static final int AVG_N     = 50;
    public static void main(String[] args) {

        banner("HỆ THỐNG GỢI Ý ÂM NHẠC");
        section("LOADING DATA");
        long t0 = System.nanoTime();
        Map<User, Map<Song, Interaction>> data = DataLoader.loadUserData(DATA_FILE);
        long loadMs = (System.nanoTime() - t0) / 1_000_000;

        if (data == null || data.isEmpty()) {
            System.out.println("Cannot read data from: " + DATA_FILE);
            return;
        }

        int totalInteractions = data.values().stream().mapToInt(Map::size).sum();
        System.out.printf("  %-28s %d%n", "Users loaded:",       data.size());
        System.out.printf("  %-28s %d%n", "Total interactions:", totalInteractions);
        System.out.printf("  %-28s %.1f%n","Avg songs/user:",     (double) totalInteractions / data.size());
        System.out.printf("  %-28s %d ms%n","Load time:",         loadMs);

        List<User> allUsers = new ArrayList<>(data.keySet());
        int N = allUsers.size();

        int bestN  = Math.min(BEST_N, N);
        int avgN   = Math.min(AVG_N,  N);
        int worstN = N;

        section("JVM WARM-UP");
        //System.out.println("  Running " + WARMUP_RUNS + " warm-up passes");
        List<User> warmupUsers = allUsers.subList(0, Math.min(10, N));
        TopKRecommender warmupRec = new TopKRecommender(data);
        for (int w = 0; w < WARMUP_RUNS; w++) {
            for (User u : warmupUsers) warmupRec.recommend(u, TOP_K);
        }
        System.out.println("Warm-up complete\n");

        List<EvaluationResult> evalResults = new ArrayList<>();

        section("TOPK RECOMMENDER EVALUATION");

        String[][] topkVersionScenarios = {
                {"V1_NaiveSort",    "best",    String.valueOf(bestN)},
                {"V1_NaiveSort",    "average", String.valueOf(avgN)},
                {"V1_NaiveSort",    "worst",   String.valueOf(worstN)},
                {"V2_MinHeap",      "best",    String.valueOf(bestN)},
                {"V2_MinHeap",      "average", String.valueOf(avgN)},
                {"V2_MinHeap",      "worst",   String.valueOf(worstN)},
                {"V3_DynamicFloor", "best",    String.valueOf(bestN)},
                {"V3_DynamicFloor", "average", String.valueOf(avgN)},
                {"V3_DynamicFloor", "worst",   String.valueOf(worstN)},
        };

        for (String[] cfg : topkVersionScenarios) {
            String version  = cfg[0];
            String scenario = cfg[1];
            int    userCount = Integer.parseInt(cfg[2]);

            List<User> testUsers = allUsers.subList(0, userCount);

            TopKRecommender rec = new TopKRecommender(data);
            rec.resetCounters();

            Map<User, List<Song>> results = new LinkedHashMap<>();

            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long timeStart = System.nanoTime();

            for (User u : testUsers) {
                List<Song> songs;
                switch (version) {
                    case "V1_NaiveSort":    songs = rec.recommendV1_NaiveSort(u, TOP_K);    break;
                    default:                songs = rec.recommend(u, TOP_K);                // V2
                }
                results.put(u, songs);
            }

            long timeEnd  = System.nanoTime();
            System.gc();
            long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            long execNs    = timeEnd - timeStart;
            long jvmMemDelta = Math.max(0, memAfter - memBefore);
            long totalMemKB = (rec.extraMemoryBytes + jvmMemDelta) / 1024;

            int usersWithRec = (int) results.values().stream().filter(l -> !l.isEmpty()).count();
            boolean passed   = (usersWithRec >= userCount / 2);

            EvaluationResult er = new EvaluationResult(
                    "TopKRecommender", version,
                    userCount, TOP_K,
                    execNs,
                    rec.opCountSimilarity, rec.opCountHeap, rec.opCountScore,
                    0, 0,
                    rec.extraMemoryBytes + jvmMemDelta,
                    usersWithRec, userCount,
                    passed, scenario
            );
            evalResults.add(er);

            System.out.printf("  [TopK %-16s | %-7s | N=%3d] time=%.3fms  ops=%,d  mem=%.1fKB  rec=%d/%d  %s%n",
                    version, scenario, userCount,
                    execNs / 1_000_000.0,
                    rec.opCountSimilarity + rec.opCountHeap + rec.opCountScore,
                    (rec.extraMemoryBytes + jvmMemDelta) / 1024.0,
                    usersWithRec, userCount,
                    passed ? "PASS" : "FAIL");
        }

        section("CACHING RECOMMENDER EVALUATION");

        System.out.println("  [CachingRecommender] Building similarity table (DP warm-up)");
        CachingRecommender sharedCachingRec = new CachingRecommender(data);
        long buildStart = System.nanoTime();
        sharedCachingRec.warmUp();
        long buildMs = (System.nanoTime() - buildStart) / 1_000_000;
        System.out.printf("  [CachingRecommender] Table built in %d ms.%n%n", buildMs);

        String[][] cachingVersionScenarios = {
                {"V1_TopDownMemo", "best",    String.valueOf(bestN)},
                {"V1_TopDownMemo", "average", String.valueOf(avgN)},
                {"V1_TopDownMemo", "worst",   String.valueOf(worstN)},
                {"V2_BottomUpTable","best",   String.valueOf(bestN)},
                {"V2_BottomUpTable","average",String.valueOf(avgN)},
                {"V2_BottomUpTable","worst",  String.valueOf(worstN)},
        };

        for (String[] cfg : cachingVersionScenarios) {
            String version   = cfg[0];
            String scenario  = cfg[1];
            int    userCount = Integer.parseInt(cfg[2]);

            List<User> testUsers = allUsers.subList(0, userCount);

            CachingRecommender rec;
            if ("V1_TopDownMemo".equals(version)) {
                rec = new CachingRecommender(data);
            } else {
                rec = sharedCachingRec;
            }
            rec.resetCounters();

            Map<User, List<Song>> results = new LinkedHashMap<>();

            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long timeStart = System.nanoTime();

            for (User u : testUsers) {
                List<Song> songs;
                if ("V1_TopDownMemo".equals(version)) {
                    songs = rec.recommendV1_TopDownMemo(u, TOP_K);
                } else {
                    songs = rec.recommend(u, TOP_K); // V2
                }
                results.put(u, songs);
            }

            long timeEnd  = System.nanoTime();
            System.gc();
            long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            long execNs       = timeEnd - timeStart;
            long jvmMemDelta  = Math.max(0, memAfter - memBefore);
            long totalMem     = rec.extraMemoryBytes + jvmMemDelta;

            int  usersWithRec = (int) results.values().stream().filter(l -> !l.isEmpty()).count();
            boolean passed    = (usersWithRec >= userCount / 2);

            EvaluationResult er = new EvaluationResult(
                    "CachingRecommender", version,
                    userCount, TOP_K,
                    execNs,
                    0, rec.opCountHeap, rec.opCountScore,
                    rec.opCountCacheHit, rec.opCountCacheMiss,
                    totalMem,
                    usersWithRec, userCount,
                    passed, scenario
            );
            evalResults.add(er);

            System.out.printf("  [Cache %-16s | %-7s | N=%3d] time=%.3fms  hits=%,d  misses=%,d  mem=%.1fKB  rec=%d/%d  %s%n",
                    version, scenario, userCount,
                    execNs / 1_000_000.0,
                    rec.opCountCacheHit, rec.opCountCacheMiss,
                    totalMem / 1024.0,
                    usersWithRec, userCount,
                    passed ? "PASS" : "FAIL");
        }

        section("FULL RECOMMENDATIONS EXPORT  (CachingRecommender V2, tất cả users)");

        Map<User, List<Song>> fullRecommendations = new LinkedHashMap<>();
        System.out.println(allUsers.size() + " users");
        long recStart = System.nanoTime();
        for (User u : allUsers) {
            fullRecommendations.put(u, sharedCachingRec.recommend(u, TOP_K));
        }
        long recMs = (System.nanoTime() - recStart) / 1_000_000;

        long usersWithFullRec = fullRecommendations.values().stream()
                .filter(l -> l != null && !l.isEmpty()).count();
        System.out.printf("  Users có gợi ý: %d / %d  (%.1f%%)%n",
                usersWithFullRec, allUsers.size(),
                100.0 * usersWithFullRec / allUsers.size());
        System.out.printf("  Thời gian:      %d ms%n", recMs);

        // In mẫu 5 user đầu
        System.out.println();
        System.out.printf("  %-22s  %s%n", "USER", "GỢI Ý (Artist — Track)");
        System.out.println("  " + "-".repeat(80));
        int shown = 0;
        for (Map.Entry<User, List<Song>> entry : fullRecommendations.entrySet()) {
            if (shown++ >= 5) break;
            String uid = entry.getKey().getUserId();
            List<Song> songs = entry.getValue();
            if (songs == null || songs.isEmpty()) {
                System.out.printf("  %-22s  (không có gợi ý)%n", uid);
            } else {
                System.out.printf("  %-22s  1. %s — %s%n",
                        uid, songs.get(0).getArtistName(), songs.get(0).getTrackName());
                for (int i = 1; i < songs.size(); i++) {
                    System.out.printf("  %-22s  %d. %s — %s%n",
                            "", i + 1, songs.get(i).getArtistName(), songs.get(i).getTrackName());
                }
            }
            System.out.println("  " + "-".repeat(80));
        }
        if (allUsers.size() > 5)
            System.out.println(" (" + (allUsers.size() - 5) + " users còn lại xem trong file CSV)");

        section("EXPORTING RESULTS");

        // Recommendations CSV — toàn bộ users
        String recPath = OUTPUT_DIR + "recommendations.csv";
        RecommendationExporter.exportToCSV(recPath, fullRecommendations);
        System.out.println(recPath + "  (" + allUsers.size() + " users × top-" + TOP_K + " songs)");

        // Evaluation metrics CSV
        String csvPath = OUTPUT_DIR + "evaluation_metrics.csv";
        EvaluationExporter.exportCSV(csvPath, evalResults);
        System.out.println(csvPath);

        // Evaluation report TXT
        String txtPath = OUTPUT_DIR + "evaluation_report.txt";
        EvaluationExporter.exportTXT(txtPath, evalResults);
        System.out.println(txtPath);

        section("QUICK SUMMARY");

        long minTime = evalResults.stream()
                .mapToLong(EvaluationResult::getExecTimeNs).min().orElse(1L);

        System.out.printf("  %-32s %14s %14s %12s%n",
                "Algorithm/Version/Scenario", "Time(ms)", "Ops_Total", "Speedup");
        System.out.println("  " + "-".repeat(76));
        for (EvaluationResult r : evalResults) {
            double speedup = (double) r.getExecTimeNs() / minTime;
            System.out.printf("  %-32s %14.3f %14d %11.2fx%n",
                    r.getAlgorithmName().replace("Recommender","") + "/"
                            + r.getVersionLabel() + "/" + r.getScenario(),
                    r.getExecTimeMs(),
                    r.getOpCountTotal(),
                    speedup);
        }
        System.out.println("  " + "-".repeat(76));
        System.out.println("  (1.00x = fastest run; higher = slower)");

        banner("DONE");
    }

    private static void banner(String title) {
        String bar = "=".repeat(80);
        System.out.println("\n" + bar);
        int pad = Math.max(0, (80 - title.length()) / 2);
        System.out.println(" ".repeat(pad) + title);
        System.out.println(bar + "\n");
    }

    private static void section(String title) {
        System.out.println("\n╔══ " + title + " " + "═".repeat(Math.max(0, 74 - title.length())) + "╗\n");
    }
}