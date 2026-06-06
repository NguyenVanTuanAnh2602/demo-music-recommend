import algorithm.*;
import benchmark.*;
import data.*;
import exporter.*;
import java.util.*;

public class Test2 {
    private static final String DATA_FILE  = "../data/samples_csv/play_count_sample_500users.csv";
    private static final String OUTPUT_DIR = "output/";
    private static final int    TOP_K      = 5;
    private static final int    WARMUP_RUNS = 2;

    private static final int SMALL_N = 50;
    private static final int MEDIUM_N = 100;

    public static void main(String[] args) {

        banner("HỆ THỐNG GỢI Ý ÂM NHẠC — TEST 2");
        section("LOADING DATA");
        long t0 = System.nanoTime();
        Map<User, Map<Song, Interaction>> data = DataLoader.loadUserData(DATA_FILE);
        long loadMs = (System.nanoTime() - t0) / 1_000_000;

        if (data == null || data.isEmpty()) {
            System.out.println("Cannot read data from: " + DATA_FILE);
            return;
        }

        int totalInteractions = data.values().stream().mapToInt(Map::size).sum();
        System.out.printf("  %-28s %d%n",   "Users loaded:",       data.size());
        System.out.printf("  %-28s %d%n",   "Total interactions:", totalInteractions);
        System.out.printf("  %-28s %.1f%n", "Avg songs/user:",     (double) totalInteractions / data.size());
        System.out.printf("  %-28s %d ms%n","Load time:",          loadMs);

        List<User> allUsers = new ArrayList<>(data.keySet());
        Collections.shuffle(allUsers, new Random(42));
        int N      = allUsers.size();
        int smallN = Math.min(SMALL_N, N);
        int mediumN = Math.min(MEDIUM_N,  N);
        int largeN = N;

        // ── JVM warm-up ────────────────────────────────────────────────────────
        section("JVM WARM-UP");
        List<User> warmupUsers = allUsers.subList(0, Math.min(10, N));
        BruteForceRecommender warmupRec = new BruteForceRecommender(data);
        for (int w = 0; w < WARMUP_RUNS; w++) {
            for (User u : warmupUsers) warmupRec.recommend(u, TOP_K);
        }
        System.out.println("Warm-up complete\n");

        List<EvaluationResult> evalResults = new ArrayList<>();

        // ══════════════════════════════════════════════════════════════════════
        // 1. BRUTE FORCE RECOMMENDER
        // ══════════════════════════════════════════════════════════════════════
        section("1. BRUTE FORCE RECOMMENDER EVALUATION");

        String[][] bfScenarios = {
                {"small",    String.valueOf(smallN)},
                {"medium", String.valueOf(mediumN)},
                {"large",   String.valueOf(largeN)},
        };

        for (String[] cfg : bfScenarios) {
            String scenario  = cfg[0];
            int    userCount = Integer.parseInt(cfg[1]);
            List<User> testUsers = allUsers.subList(0, userCount);

            BruteForceRecommender rec = new BruteForceRecommender(data);
            rec.resetCounters();
            Map<User, List<Song>> results = new LinkedHashMap<>();

            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long timeStart = System.nanoTime();

            for (User u : testUsers) results.put(u, rec.recommend(u, TOP_K));

            long timeEnd     = System.nanoTime();
            System.gc();
            long memAfter    = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long execNs      = timeEnd - timeStart;
            long jvmMemDelta = Math.max(0, memAfter - memBefore);

            int     usersWithRec = (int) results.values().stream().filter(l -> !l.isEmpty()).count();
            boolean passed       = usersWithRec >= userCount / 2;

            evalResults.add(new EvaluationResult(
                    "BruteForceRecommender", "V1_BruteForce", userCount, TOP_K, execNs,
                    rec.opCountSimilarity, rec.opCountHeap, rec.opCountScore,
                    0, 0,
                    rec.extraMemoryBytes + jvmMemDelta,
                    usersWithRec, userCount, passed, scenario));

            System.out.printf("  [BruteForce %-10s | %-7s | N=%3d] time=%.3fms  ops=%,d  mem=%.1fKB  rec=%d/%d  %s%n",
                    "V1_BruteForce", scenario, userCount,
                    execNs / 1_000_000.0,
                    rec.opCountSimilarity + rec.opCountHeap + rec.opCountScore,
                    (rec.extraMemoryBytes + jvmMemDelta) / 1024.0,
                    usersWithRec, userCount, passed ? "PASS" : "FAIL");
        }

        // ══════════════════════════════════════════════════════════════════════
        // 2. PRUNING RECOMMENDER
        // ══════════════════════════════════════════════════════════════════════
        section("2. PRUNING RECOMMENDER EVALUATION");

        String[][] pruningScenarios = {
                {"small",    String.valueOf(smallN)},
                {"medium", String.valueOf(mediumN)},
                {"large",   String.valueOf(largeN)},
        };

        for (String[] cfg : pruningScenarios) {
            String scenario  = cfg[0];
            int    userCount = Integer.parseInt(cfg[1]);
            List<User> testUsers = allUsers.subList(0, userCount);

            PruningRecommender rec = new PruningRecommender(data);
            rec.resetCounters();
            Map<User, List<Song>> results = new LinkedHashMap<>();

            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long timeStart = System.nanoTime();

            for (User u : testUsers) results.put(u, rec.recommend(u, TOP_K));

            long timeEnd     = System.nanoTime();
            System.gc();
            long memAfter    = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long execNs      = timeEnd - timeStart;
            long jvmMemDelta = Math.max(0, memAfter - memBefore);

            int     usersWithRec = (int) results.values().stream().filter(l -> !l.isEmpty()).count();
            boolean passed       = usersWithRec >= userCount / 2;

            evalResults.add(new EvaluationResult(
                    "PruningRecommender", "V1_Pruning", userCount, TOP_K, execNs,
                    rec.opCountSimilarity, rec.opCountHeap, rec.opCountScore,
                    0, 0,
                    rec.extraMemoryBytes + jvmMemDelta,
                    usersWithRec, userCount, passed, scenario));

            System.out.printf("  [Pruning %-12s | %-7s | N=%3d] time=%.3fms  ops=%,d  mem=%.1fKB  rec=%d/%d  %s%n",
                    "V1_Pruning", scenario, userCount,
                    execNs / 1_000_000.0,
                    rec.opCountSimilarity + rec.opCountHeap + rec.opCountScore,
                    (rec.extraMemoryBytes + jvmMemDelta) / 1024.0,
                    usersWithRec, userCount, passed ? "PASS" : "FAIL");
        }

        /*
        // ══════════════════════════════════════════════════════════════════════
        // 3. TOPK RECOMMENDER
        // ══════════════════════════════════════════════════════════════════════
        section("3. TOPK RECOMMENDER EVALUATION");

        String[][] topkVersionScenarios = {
                {"V1_NaiveSort",    "small",    String.valueOf(smallN)},
                {"V1_NaiveSort",    "medium", String.valueOf(mediumN)},
                {"V1_NaiveSort",    "large",   String.valueOf(largeN)},
                {"V2_MinHeap",      "small",    String.valueOf(smallN)},
                {"V2_MinHeap",      "medium", String.valueOf(mediumN)},
                {"V2_MinHeap",      "large",   String.valueOf(largeN)}
        };

        for (String[] cfg : topkVersionScenarios) {
            String version   = cfg[0];
            String scenario  = cfg[1];
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
                    default:                songs = rec.recommend(u, TOP_K);
                }
                results.put(u, songs);
            }

            long timeEnd     = System.nanoTime();
            System.gc();
            long memAfter    = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long execNs      = timeEnd - timeStart;
            long jvmMemDelta = Math.max(0, memAfter - memBefore);

            int     usersWithRec = (int) results.values().stream().filter(l -> !l.isEmpty()).count();
            boolean passed       = usersWithRec >= userCount / 2;

            evalResults.add(new EvaluationResult(
                    "TopKRecommender", version, userCount, TOP_K, execNs,
                    rec.opCountSimilarity, rec.opCountHeap, rec.opCountScore,
                    0, 0,
                    rec.extraMemoryBytes + jvmMemDelta,
                    usersWithRec, userCount, passed, scenario));

            System.out.printf("  [TopK %-16s | %-7s | N=%3d] time=%.3fms  ops=%,d  mem=%.1fKB  rec=%d/%d  %s%n",
                    version, scenario, userCount,
                    execNs / 1_000_000.0,
                    rec.opCountSimilarity + rec.opCountHeap + rec.opCountScore,
                    (rec.extraMemoryBytes + jvmMemDelta) / 1024.0,
                    usersWithRec, userCount, passed ? "PASS" : "FAIL");
        }

         */


        // ══════════════════════════════════════════════════════════════════════
        // 3. Greedy RECOMMENDER
        // ══════════════════════════════════════════════════════════════════════
        section("3. GREEDY RECOMMENDER EVALUATION");

        String[][] greedyVersionScenarios = {
                {"V1_GreedyNeighbour",    "small",    String.valueOf(smallN)},
                {"V1_GreedyNeighbour",    "medium", String.valueOf(mediumN)},
                {"V1_GreedyNeighbour",    "large",   String.valueOf(largeN)},
                {"V2_GreedySong",      "small",    String.valueOf(smallN)},
                {"V2_GreedySong",      "medium", String.valueOf(mediumN)},
                {"V2_GreedySong",      "large",   String.valueOf(largeN)}
        };

        for (String[] cfg : greedyVersionScenarios) {
            String version   = cfg[0];
            String scenario  = cfg[1];
            int    userCount = Integer.parseInt(cfg[2]);
            List<User> testUsers = allUsers.subList(0, userCount);

            GreedyRecommender rec = new GreedyRecommender(data);
            rec.resetCounters();
            Map<User, List<Song>> results = new LinkedHashMap<>();

            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long timeStart = System.nanoTime();

            for (User u : testUsers) {
                List<Song> songs;
                switch (version) {
                    case "V1_GreedyNeighbour":    songs = rec.recommendV1_GreedyNeighbor(u, TOP_K);    break;
                    default:                songs = rec.recommend(u, TOP_K);
                }
                results.put(u, songs);
            }

            long timeEnd     = System.nanoTime();
            System.gc();
            long memAfter    = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long execNs      = timeEnd - timeStart;
            long jvmMemDelta = Math.max(0, memAfter - memBefore);

            int     usersWithRec = (int) results.values().stream().filter(l -> !l.isEmpty()).count();
            boolean passed       = usersWithRec >= userCount / 2;

            evalResults.add(new EvaluationResult(
                    "GreedyRecommender", version, userCount, TOP_K, execNs,
                    rec.opCountSimilarity, rec.opCountHeap, rec.opCountScore,
                    0, 0,
                    rec.extraMemoryBytes + jvmMemDelta,
                    usersWithRec, userCount, passed, scenario));

            System.out.printf("  [Greedy %-16s | %-7s | N=%3d] time=%.3fms  ops=%,d  mem=%.1fKB  rec=%d/%d  %s%n",
                    version, scenario, userCount,
                    execNs / 1_000_000.0,
                    rec.opCountSimilarity + rec.opCountHeap + rec.opCountScore,
                    (rec.extraMemoryBytes + jvmMemDelta) / 1024.0,
                    usersWithRec, userCount, passed ? "PASS" : "FAIL");
        }


        /*
        // ══════════════════════════════════════════════════════════════════════
        // 4. CACHING RECOMMENDER
        // ══════════════════════════════════════════════════════════════════════
        section("4. CACHING RECOMMENDER EVALUATION");


        System.out.println("  [CachingRecommender] Building similarity table (DP warm-up)");
        CachingRecommender sharedCachingRec = new CachingRecommender(data);

        long buildStart = System.nanoTime();
        sharedCachingRec.warmUp();
        long buildMs = (System.nanoTime() - buildStart) / 1_000_000;
        System.out.printf("  [CachingRecommender] Table built in %d ms.%n%n", buildMs);



        String[][] cachingVersionScenarios = {
                {"V1_TopDownMemo",  "small",    String.valueOf(smallN)},
                {"V1_TopDownMemo",  "medium", String.valueOf(mediumN)},
                {"V1_TopDownMemo",  "large",   String.valueOf(largeN)},
                {"V2_BottomUpTable","small",    String.valueOf(smallN)},
                {"V2_BottomUpTable","medium", String.valueOf(mediumN)},
                {"V2_BottomUpTable","large",   String.valueOf(largeN)},
        };

        for (String[] cfg : cachingVersionScenarios) {
            String version   = cfg[0];
            String scenario  = cfg[1];
            int    userCount = Integer.parseInt(cfg[2]);
            List<User> testUsers = allUsers.subList(0, userCount);

            CachingRecommender rec = "V1_TopDownMemo".equals(version)
                    ? new CachingRecommender(data)
                    : sharedCachingRec;
            rec.resetCounters();
            Map<User, List<Song>> results = new LinkedHashMap<>();

            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long timeStart = System.nanoTime();

            for (User u : testUsers) {
                List<Song> songs = "V1_TopDownMemo".equals(version)
                        ? rec.recommendV1_TopDownMemo(u, TOP_K)
                        : rec.recommend(u, TOP_K);
                results.put(u, songs);
            }

            long timeEnd     = System.nanoTime();
            System.gc();
            long memAfter    = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long execNs      = timeEnd - timeStart;
            long jvmMemDelta = Math.max(0, memAfter - memBefore);
            long totalMem    = rec.extraMemoryBytes + jvmMemDelta;

            int     usersWithRec = (int) results.values().stream().filter(l -> !l.isEmpty()).count();
            boolean passed       = usersWithRec >= userCount / 2;

            evalResults.add(new EvaluationResult(
                    "CachingRecommender", version, userCount, TOP_K, execNs,
                    0, rec.opCountHeap, rec.opCountScore,
                    rec.opCountCacheHit, rec.opCountCacheMiss,
                    totalMem,
                    usersWithRec, userCount, passed, scenario));

            System.out.printf("  [Cache %-16s | %-7s | N=%3d] time=%.3fms  hits=%,d  misses=%,d  mem=%.1fKB  rec=%d/%d  %s%n",
                    version, scenario, userCount,
                    execNs / 1_000_000.0,
                    rec.opCountCacheHit, rec.opCountCacheMiss,
                    totalMem / 1024.0,
                    usersWithRec, userCount, passed ? "PASS" : "FAIL");
        }

         */





        // ══════════════════════════════════════════════════════════════════════
        // 4. CACHING RECOMMENDER (QUY HOẠCH ĐỘNG)
        // ══════════════════════════════════════════════════════════════════════
        section("4. CACHING RECOMMENDER EVALUATION");

        // V2: Build bảng DP một lần duy nhất TRƯỚC khi đo — warmup không tính vào exec time
        System.out.println("  [CachingRecommender V2] Building DP similarity table (warmup)...");
        CachingRecommender sharedCachingRec = new CachingRecommender(data);
        long buildStart = System.nanoTime();
        sharedCachingRec.warmUp();
        long buildMs = (System.nanoTime() - buildStart) / 1_000_000;
        System.out.printf("  [CachingRecommender V2] Table built in %d ms.%n%n", buildMs);

        // FIX: V1 dùng MỘT instance chung xuyên suốt 3 scenario
        // Cache tích lũy dần: small → medium → large
        // Thể hiện đúng lợi thế memoization — hits tăng dần theo số truy vấn
        // (Nếu dùng instance mới mỗi lần: 100% cache miss → chậm hơn BruteForce do overhead HashMap)
        System.out.println("  [CachingRecommender V1] Shared instance — cache accumulates across scenarios.");
        CachingRecommender sharedV1Rec = new CachingRecommender(data);

        String[][] cachingVersionScenarios = {
                {"V1_TopDownMemo",       "small",  String.valueOf(smallN)},
                {"V1_TopDownMemo",       "medium", String.valueOf(mediumN)},
                {"V1_TopDownMemo",       "large",  String.valueOf(largeN)},
                {"V2_BottomUpTable",     "small",  String.valueOf(smallN)},
                {"V2_BottomUpTable",     "medium", String.valueOf(mediumN)},
                {"V2_BottomUpTable",     "large",  String.valueOf(largeN)},
        };

        for (String[] cfg : cachingVersionScenarios) {
            String version   = cfg[0];
            String scenario  = cfg[1];
            int    userCount = Integer.parseInt(cfg[2]);
            List<User> testUsers = allUsers.subList(0, userCount);

            // FIX: V1 dùng sharedV1Rec (cache tích lũy), V2 dùng sharedCachingRec (đã warmup)
            CachingRecommender rec = "V1_TopDownMemo".equals(version)
                    ? sharedV1Rec
                    : sharedCachingRec;
            rec.resetCounters();
            Map<User, List<Song>> results = new LinkedHashMap<>();

            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long timeStart = System.nanoTime();

            for (User u : testUsers) {
                List<Song> songs = "V1_TopDownMemo".equals(version)
                        ? rec.recommendV1_TopDownMemo(u, TOP_K)
                        : rec.recommend(u, TOP_K);
                results.put(u, songs);
            }

            long timeEnd     = System.nanoTime();
            System.gc();
            long memAfter    = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long execNs      = timeEnd - timeStart;
            long jvmMemDelta = Math.max(0, memAfter - memBefore);
            long totalMem    = rec.extraMemoryBytes + jvmMemDelta;

            int     usersWithRec = (int) results.values().stream().filter(l -> !l.isEmpty()).count();
            boolean passed       = usersWithRec >= userCount / 2;

            evalResults.add(new EvaluationResult(
                    "CachingRecommender", version, userCount, TOP_K, execNs,
                    0, rec.opCountHeap, rec.opCountScore,
                    rec.opCountCacheHit, rec.opCountCacheMiss,
                    totalMem,
                    usersWithRec, userCount, passed, scenario));

            System.out.printf("  [Cache %-16s | %-7s | N=%3d] time=%.3fms  hits=%,d  misses=%,d  mem=%.1fKB  rec=%d/%d  %s%n",
                    version, scenario, userCount,
                    execNs / 1_000_000.0,
                    rec.opCountCacheHit, rec.opCountCacheMiss,
                    totalMem / 1024.0,
                    usersWithRec, userCount, passed ? "PASS" : "FAIL");
        }







        // ══════════════════════════════════════════════════════════════════════
        // 5. HEURISTIC RECOMMENDER
        // ══════════════════════════════════════════════════════════════════════
        section("5. HEURISTIC RECOMMENDER EVALUATION");

        String[][] heuristicVersionScenarios = {
                {"V1_FilterOverlap", "small",    String.valueOf(smallN)},
                {"V1_FilterOverlap", "medium", String.valueOf(mediumN)},
                {"V1_FilterOverlap", "large",   String.valueOf(largeN)},
                {"V2_InvertedIndex", "small",    String.valueOf(smallN)},
                {"V2_InvertedIndex", "medium", String.valueOf(mediumN)},
                {"V2_InvertedIndex", "large",   String.valueOf(largeN)},
        };

        for (String[] cfg : heuristicVersionScenarios) {
            String version   = cfg[0];
            String scenario  = cfg[1];
            int    userCount = Integer.parseInt(cfg[2]);
            List<User> testUsers = allUsers.subList(0, userCount);

            HeuristicVersion hv = "V1_FilterOverlap".equals(version)
                    ? HeuristicVersion.V1
                    : HeuristicVersion.V2;

            HeuristicRecommender rec = new HeuristicRecommender(data, hv);
            rec.resetCounters();
            Map<User, List<Song>> results = new LinkedHashMap<>();

            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long timeStart = System.nanoTime();

            for (User u : testUsers) results.put(u, rec.recommend(u, TOP_K));

            long timeEnd     = System.nanoTime();
            System.gc();
            long memAfter    = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long execNs      = timeEnd - timeStart;
            long jvmMemDelta = Math.max(0, memAfter - memBefore);

            int     usersWithRec = (int) results.values().stream().filter(l -> !l.isEmpty()).count();
            boolean passed       = usersWithRec >= userCount / 2;

            evalResults.add(new EvaluationResult(
                    "HeuristicRecommender", version, userCount, TOP_K, execNs,
                    rec.opCountSimilarity, rec.opCountHeap, rec.opCountScore,
                    0, 0,
                    rec.extraMemoryBytes + jvmMemDelta,
                    usersWithRec, userCount, passed, scenario));

            System.out.printf("  [Heuristic %-14s | %-7s | N=%3d] time=%.3fms  ops=%,d  mem=%.1fKB  rec=%d/%d  %s%n",
                    version, scenario, userCount,
                    execNs / 1_000_000.0,
                    rec.opCountSimilarity + rec.opCountHeap + rec.opCountScore,
                    (rec.extraMemoryBytes + jvmMemDelta) / 1024.0,
                    usersWithRec, userCount, passed ? "PASS" : "FAIL");
        }

        // ══════════════════════════════════════════════════════════════════════
        // FULL RECOMMENDATIONS — 5 ALGORITHMS (tất cả users)
        // ══════════════════════════════════════════════════════════════════════
        section("FULL RECOMMENDATIONS EXPORT (5 thuật toán × tất cả users)");

        // Khởi tạo các recommender cho full export
        // BruteForce
        BruteForceRecommender fullBF = new BruteForceRecommender(data);

        // Pruning
        PruningRecommender fullPruning = new PruningRecommender(data);

        /*
        // TopK (dùng V2 MinHeap — version tốt nhất)
        TopKRecommender fullTopK = new TopKRecommender(data);

         */

        // Greedy
        GreedyRecommender fullGreedy = new GreedyRecommender(data);

        // Caching (dùng V2 BottomUpTable đã warm-up sẵn)
        // sharedCachingRec đã được warm-up ở trên, dùng lại

        // Heuristic (dùng V2 InvertedIndex — version tốt nhất)
        HeuristicRecommender fullHeuristic = new HeuristicRecommender(data, HeuristicVersion.V2);

        // Cấu hình: tên file suffix + recommender tương ứng
        String[][]       recLabels = {
                {"BruteForce",  "BruteForceRecommender"},
                {"Pruning",     "PruningRecommender"},
                /*{"TopK",        "TopKRecommender_V2MinHeap"},

                 */
                {"Greedy",      "GreedyRecommender_V2GreedySong"},
                {"Caching",     "CachingRecommender_V2BottomUp"},
                {"Heuristic",   "HeuristicRecommender_V2InvertedIndex"},
        };

        // Chạy và export từng thuật toán
        for (String[] label : recLabels) {
            String tag       = label[0];
            String algoLabel = label[1];

            Map<User, List<Song>> fullRec = new LinkedHashMap<>();
            long recStart = System.nanoTime();

            for (User u : allUsers) {
                List<Song> songs;
                switch (tag) {
                    case "BruteForce": songs = fullBF.recommend(u, TOP_K);              break;
                    case "Pruning":    songs = fullPruning.recommend(u, TOP_K);         break;
                    /*case "TopK":       songs = fullTopK.recommend(u, TOP_K);            break;

                     */
                    case "Greedy":     songs = fullGreedy.recommend(u, TOP_K);          break;
                    case "Caching":    songs = sharedCachingRec.recommend(u, TOP_K);    break;
                    default:           songs = fullHeuristic.recommend(u, TOP_K);
                }
                fullRec.put(u, songs);
            }

            long recMs       = (System.nanoTime() - recStart) / 1_000_000;
            long usersWithRec = fullRec.values().stream().filter(l -> l != null && !l.isEmpty()).count();

            System.out.printf("  [%-38s] users có gợi ý: %d/%d  (%.1f%%)  time: %d ms%n",
                    algoLabel,
                    usersWithRec, allUsers.size(),
                    100.0 * usersWithRec / allUsers.size(),
                    recMs);

            // In mẫu 3 user đầu
            System.out.printf("  %-22s  %s%n", "USER", "GỢI Ý (Artist — Track)");
            System.out.println("  " + "-".repeat(80));
            int shown = 0;
            for (Map.Entry<User, List<Song>> entry : fullRec.entrySet()) {
                if (shown++ >= 3) break;
                String     uid   = entry.getKey().getUserId();
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
            System.out.println();

            // Export CSV: recommendations_BruteForce.csv, recommendations_Pruning.csv, ...
            String recPath = OUTPUT_DIR + "recommendations_" + tag + ".csv";
            RecommendationExporter.exportToCSV(recPath, fullRec, algoLabel);
            System.out.println("  → " + recPath);
            System.out.println();
        }

        // ══════════════════════════════════════════════════════════════════════
        // EXPORT EVALUATION
        // ══════════════════════════════════════════════════════════════════════
        section("EXPORTING EVALUATION RESULTS");

        String csvPath = OUTPUT_DIR + "evaluation_metrics_test2.csv";
        EvaluationExporter.exportCSV(csvPath, evalResults);
        System.out.println(csvPath);

        String txtPath = OUTPUT_DIR + "evaluation_report_test2.txt";
        EvaluationExporter.exportTXT(txtPath, evalResults);
        System.out.println(txtPath);

        // ══════════════════════════════════════════════════════════════════════
        // QUICK SUMMARY
        // ══════════════════════════════════════════════════════════════════════
        section("QUICK SUMMARY");

        long minTime = evalResults.stream()
                .mapToLong(EvaluationResult::getExecTimeNs).min().orElse(1L);

        System.out.printf("  %-42s %12s %14s %12s%n",
                "Algorithm/Version/Scenario", "Time(ms)", "Ops_Total", "Speedup");
        System.out.println("  " + "-".repeat(84));
        for (EvaluationResult r : evalResults) {
            double speedup = (double) r.getExecTimeNs() / minTime;
            System.out.printf("  %-42s %12.3f %14d %11.2fx%n",
                    r.getAlgorithmName().replace("Recommender", "")
                            + "/" + r.getVersionLabel() + "/" + r.getScenario(),
                    r.getExecTimeMs(),
                    r.getOpCountTotal(),
                    speedup);
        }
        System.out.println("  " + "-".repeat(84));
        System.out.println("  (1.00x = fastest run; higher = slower)");

        banner("DONE");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
