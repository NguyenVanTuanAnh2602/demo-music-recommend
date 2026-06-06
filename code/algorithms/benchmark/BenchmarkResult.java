package benchmark;

public class BenchmarkResult {
    private String algorithmName;
    private long timeNano;
    private int k;

    public BenchmarkResult(String algorithmName, long timeNano, int k) {
        this.algorithmName = algorithmName;
        this.timeNano = timeNano;
        this.k = k;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public long getTimeNano() {
        return timeNano;
    }

    public int getK() {
        return k;
    }

    @Override
    public String toString() {
        return algorithmName + " | avgTime(ns): " + timeNano;
    }
}
