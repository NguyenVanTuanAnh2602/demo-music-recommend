package benchmark;

public class EvaluationResult {
    private final String algorithmName;
    private final String versionLabel;
    private final int    inputSize;
    private final int    topK;

    private final long   execTimeNs;

    private final long   opCountSimilarity;
    private final long   opCountHeap;
    private final long   opCountScore;

    // CachingRecommender: cache hits saved re-computation
    private final long   opCountCacheHit;
    private final long   opCountCacheMiss;

    // Memory
    private final long   extraMemoryBytes;

    // Correctness
    private final int    usersWithRecommendations;
    private final int    totalUsersEvaluated;
    private final boolean passed;

    // Scenario
    private final String scenario;

    // Constructor
    public EvaluationResult(String algorithmName,
                            String versionLabel,
                            int inputSize, int topK,
                            long execTimeNs,
                            long opCountSimilarity, long opCountHeap, long opCountScore,
                            long opCountCacheHit, long opCountCacheMiss,
                            long extraMemoryBytes,
                            int usersWithRecommendations, int totalUsersEvaluated,
                            boolean passed, String scenario) {
        this.algorithmName = algorithmName;
        this.versionLabel = versionLabel;
        this.inputSize = inputSize;
        this.topK = topK;
        this.execTimeNs = execTimeNs;
        this.opCountSimilarity = opCountSimilarity;
        this.opCountHeap = opCountHeap;
        this.opCountScore = opCountScore;
        this.opCountCacheHit = opCountCacheHit;
        this.opCountCacheMiss = opCountCacheMiss;
        this.extraMemoryBytes = extraMemoryBytes;
        this.usersWithRecommendations = usersWithRecommendations;
        this.totalUsersEvaluated = totalUsersEvaluated;
        this.passed = passed;
        this.scenario = scenario;
    }

    // Getters
    public String getAlgorithmName() {
        return algorithmName;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public int getInputSize() {
        return inputSize;
    }

    public int getTopK() {
        return topK;
    }

    public long getExecTimeNs() {
        return execTimeNs;
    }

    public double getExecTimeMs() {
        return execTimeNs / 1_000_000.0;
    }

    public long getOpCountSimilarity() {
        return opCountSimilarity;
    }

    public long getOpCountHeap() {
        return opCountHeap;
    }

    public long getOpCountScore() {
        return opCountScore;
    }

    public long getOpCountTotal() {
        return opCountSimilarity + opCountHeap + opCountScore;
    }

    public long getOpCountCacheHit() {
        return opCountCacheHit;
    }

    public long getOpCountCacheMiss() {
        return opCountCacheMiss;
    }

    public long getExtraMemoryBytes() {
        return extraMemoryBytes;
    }

    public double getExtraMemoryKB() {
        return extraMemoryBytes / 1024.0;
    }

    public double getExtraMemoryMB() {
        return extraMemoryBytes / (1024.0 * 1024.0);
    }

    public int getUsersWithRecommendations() {
        return usersWithRecommendations;
    }

    public int getTotalUsersEvaluated() {
        return totalUsersEvaluated;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getScenario() {
        return scenario;
    }
}
