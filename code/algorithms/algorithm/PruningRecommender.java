/*
package algorithm;

import data.*;
import util.*;

import java.util.*;
import java.util.stream.Collectors;

public class PruningRecommender implements Recommender {

    private final Map<User, Map<Song, Interaction>>              data;
    private final Map<User, Double>                              norm;
    private final Map<User, List<Map.Entry<Song, Interaction>>>  sortedEntries;
    private final Map<User, double[]>                            suffixSqSum;

    public long opCountSimilarity = 0;
    public long opCountHeap       = 0;
    public long opCountScore      = 0;
    public long extraMemoryBytes  = 0;

    public void resetCounters() {
        opCountSimilarity = 0;
        opCountHeap       = 0;
        opCountScore      = 0;
        extraMemoryBytes  = 0;
    }

    public PruningRecommender(Map<User, Map<Song, Interaction>> data) {
        this.data          = data;
        this.norm          = new HashMap<>();
        this.sortedEntries = new HashMap<>();
        this.suffixSqSum   = new HashMap<>();
        precompute();
    }

    // Tính trước norm, sorted entries và suffix squared sum cho mọi user
    private void precompute() {
        for (Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            User user = entry.getKey();
            List<Map.Entry<Song, Interaction>> sorted = sortByPlayCountDesc(entry.getValue());
            double[] sqSum = buildSuffixSqSum(sorted);

            sortedEntries.put(user, sorted);
            suffixSqSum.put(user, sqSum);
            norm.put(user, Math.sqrt(sqSum[0]));
        }
    }

    private List<Map.Entry<Song, Interaction>> sortByPlayCountDesc(Map<Song, Interaction> songMap) {
        List<Map.Entry<Song, Interaction>> sorted = new ArrayList<>(songMap.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue().getPlayCount(), a.getValue().getPlayCount()));
        return sorted;
    }

    private double[] buildSuffixSqSum(List<Map.Entry<Song, Interaction>> sorted) {
        int n = sorted.size();
        double[] sqSum = new double[n + 1];
        for (int i = n - 1; i >= 0; i--) {
            double w = sorted.get(i).getValue().getPlayCount();
            sqSum[i] = sqSum[i + 1] + w * w;
        }
        return sqSum;
    }

    @Override
    public List<Song> recommend(User user, int k) {
        double normU = norm.getOrDefault(user, 0.0);
        if (normU == 0) return new ArrayList<>();

        Map<Song, Interaction> userHistory = data.getOrDefault(user, new HashMap<>());
        Map<User, Double> neighbours = findNeighboursWithPruning(user, normU, k);

        Map<Song, Double> scores = Scoring.score(user, data, neighbours);
        if (scores == null || scores.isEmpty()) return new ArrayList<>();
        opCountScore += scores.size();

        PriorityQueue<Map.Entry<Song, Double>> heap = new PriorityQueue<>(
                Comparator.comparingDouble(Map.Entry::getValue));

        for (Map.Entry<Song, Double> entry : scores.entrySet()) {
            if (userHistory.containsKey(entry.getKey())) continue;
            heap.offer(entry);
            opCountHeap++;
            if (heap.size() > k) {
                heap.poll();
                opCountHeap++;
            }
        }

        extraMemoryBytes += (neighbours.size() * 64L)
                + (scores.size() * 64L)
                + ((k + 1) * 32L);

        List<Song> results = new ArrayList<>();
        while (!heap.isEmpty()) results.add(0, heap.poll().getKey());
        return results;
    }

    private Map<User, Double> findNeighboursWithPruning(User user, double normU, int k) {
        List<Map.Entry<Song, Interaction>> userSorted = sortedEntries.getOrDefault(user, List.of());
        double[] sqSum = suffixSqSum.getOrDefault(user, new double[]{0});

        Map<User, Double> neighbours = new HashMap<>();

        for (Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            User other = entry.getKey();
            if (other.equals(user)) continue;

            double normV = norm.getOrDefault(other, 0.0);
            if (normV == 0) continue;

            opCountSimilarity++;
            // threshold = 0: chỉ prune những user không overlap bài nào với user (sim = 0)
            // Không dùng top-K similarity làm threshold vì BruteForce dùng TẤT CẢ neighbour có sim > 0
            double sim = cosineWithEarlyStopping(userSorted, sqSum, entry.getValue(), normU, normV, 0.0);

            if (sim <= 0) continue;

            neighbours.put(other, sim);
        }

        return neighbours;
    }

    // Tính cosine, trả về cosine thực hoặc ngược lại -1
    private double cosineWithEarlyStopping(
            List<Map.Entry<Song, Interaction>> userSorted,
            double[] sqSum,
            Map<Song, Interaction> otherMap,
            double normU, double normV,
            double threshold) {

        double denominator = normU * normV;
        double partialDot  = 0.0;

        for (int i = 0; i < userSorted.size(); i++) {
            Interaction iOther = otherMap.get(userSorted.get(i).getKey());
            if (iOther != null) {
                partialDot += userSorted.get(i).getValue().getPlayCount() * iOther.getPlayCount();
            }

            double upperBound = (partialDot + Math.sqrt(sqSum[i + 1]) * normV) / denominator;
            if (upperBound <= threshold) return -1;
        }

        return partialDot / denominator;
    }


}
*/





package algorithm;


import data.*;
import util.*;

import java.util.*;
import java.util.stream.Collectors;

public class PruningRecommender implements Recommender {

    private final Map<User, Map<Song, Interaction>> data;
    private final Map<User, Double> norm;
    private final Map<User, List<Map.Entry<Song, Interaction>>> sortedEntries;
    private final Map<User, double[]> suffixSqSum;

    public long opCountSimilarity = 0;
    public long opCountHeap = 0;
    public long opCountScore = 0;
    public long extraMemoryBytes = 0;

    public void resetCounters() {
        opCountSimilarity = 0;
        opCountHeap = 0;
        opCountScore = 0;
        extraMemoryBytes = 0;
    }

    public PruningRecommender(Map<User, Map<Song, Interaction>> data) {
        this.data = data;
        this.norm = new HashMap<>();
        this.sortedEntries = new HashMap<>();
        this.suffixSqSum = new HashMap<>();
        precompute();
    }

    private void precompute() {
        for (Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            User user = entry.getKey();
            List<Map.Entry<Song, Interaction>> sorted = sortByPlayCountDesc(entry.getValue());
            double[] sqSum = buildSuffixSqSum(sorted);

            sortedEntries.put(user, sorted);
            suffixSqSum.put(user, sqSum);
            norm.put(user, Math.sqrt(sqSum[0]));
        }
    }

    private List<Map.Entry<Song, Interaction>> sortByPlayCountDesc(Map<Song, Interaction> songMap) {
        List<Map.Entry<Song, Interaction>> sorted = new ArrayList<>(songMap.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue().getPlayCount(), a.getValue().getPlayCount()));

        return sorted;
    }

    private double[] buildSuffixSqSum(List<Map.Entry<Song, Interaction>> sorted) {
        int n = sorted.size();
        double[] sqSum = new double[n + 1];

        for (int i = n - 1; i >= 0; i--) {
            double w = sorted.get(i).getValue().getPlayCount();
            sqSum[i] = sqSum[i + 1] + w * w;
        }

        return sqSum;
    }

    @Override
    public List<Song> recommend(User user, int k) {
        double normU = norm.getOrDefault(user, 0.0);
        if (normU == 0) {
            return new ArrayList<>();
        }

        Map<Song, Interaction> userHistory = data.getOrDefault(user, new HashMap<>());
        Map<User, Double> neighbours = findNeighboursWithPruning(user, normU, k);

        Map<Song, Double> scores = Scoring.score(user, data, neighbours);
        if (scores == null || scores.isEmpty()) {
            return new ArrayList<>();
        }
        opCountScore += scores.size();

        PriorityQueue<Map.Entry<Song, Double>> heap = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

        for (Map.Entry<Song, Double> entry : scores.entrySet()) {
            if (userHistory.containsKey(entry.getKey())) continue;

            heap.offer(entry);
            opCountHeap++;

            if (heap.size() > k) {
                heap.poll();
                opCountHeap++;
            }
        }

        extraMemoryBytes += (neighbours.size() * 64L) + (scores.size() * 64L) + ((k + 1) * 32L);

        List<Song> results = new ArrayList<>();

        while (!heap.isEmpty()) {
            results.add(0, heap.poll().getKey());
        }

        return results;
    }

    private Map<User, Double> findNeighboursWithPruning(User user, double normU, int k) {
        List<Map.Entry<Song, Interaction>> userSorted = sortedEntries.getOrDefault(user, List.of());
        double[] sqSum = suffixSqSum.getOrDefault(user, new double[]{0});

        PriorityQueue<Double> topKHeap = new PriorityQueue<>(Math.max(k, 1)); // min-heap
        Map<User, Double> neighbours = new HashMap<>();

        for (Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            User other = entry.getKey();
            if (other.equals(user)) continue;

            double normV = norm.getOrDefault(other, 0.0);
            if (normV == 0) continue;

            opCountSimilarity++;
            double threshold = (topKHeap.size() >= k) ? topKHeap.peek() : 0.0;
            double sim = cosineWithEarlyStopping(userSorted, sqSum, entry.getValue(), normU, normV, threshold);

            if (sim <= 0) continue;

            neighbours.put(other, sim);
            updateHeap(topKHeap, sim, k);
        }

        return neighbours;
    }

    private double cosineWithEarlyStopping(List<Map.Entry<Song, Interaction>> userSorted, double[] sqSum, Map<Song, Interaction> otherMap, double normU, double normV, double threshold) {
        double denominator = normU * normV;
        double partialDot  = 0.0;

        for (int i = 0; i < userSorted.size(); i++) {
            Interaction iOther = otherMap.get(userSorted.get(i).getKey());

            if (iOther != null) {
                partialDot += userSorted.get(i).getValue().getPlayCount() * iOther.getPlayCount();
            }

            double upperBound = (partialDot + Math.sqrt(sqSum[i + 1]) * normV) / denominator;

            if (upperBound <= threshold) {
                return -1;
            }
        }

        return partialDot / denominator;
    }

    private void updateHeap(PriorityQueue<Double> heap, double sim, int k) {
        heap.offer(sim);

        if (heap.size() > k) {
            heap.poll();
        }
    }
}

