package algorithm;

import data.*;
import java.util.*;
import util.Scoring;
import util.Similarity;

public class TopKRecommender implements Recommender {
    private Map<User, Map<Song, Interaction>> data;
    private static final double SIM_THRESHOLD          = 0.1;
    private static final double SIM_THRESHOLD_FALLBACK = 0.05;

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

    public TopKRecommender(Map<User, Map<Song, Interaction>> data) {
        this.data = data;
    }

    @Override
    public List<Song> recommend(User targetUser, int k) {
        if(!data.containsKey(targetUser)){
            return new ArrayList<>();
        }

        return recommendV2_MinHeap(targetUser, k);
    }

    //O(U * log U)
    public List<Song> recommendV1_NaiveSort(User target, int k){
        Map<Song, Interaction> targetHistory = data.get(target);
        Map<User, Double> topNeighbors = new HashMap<>();

        for (Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            if (entry.getKey().equals(target)) continue;
            opCountSimilarity++;
            double sim = Similarity.cosine(targetHistory, entry.getValue());
            if (sim > SIM_THRESHOLD) topNeighbors.put(entry.getKey(), sim);
        }

        Map<Song, Double> songScores = Scoring.score(target, data, topNeighbors);
        opCountScore += songScores.size();

        List<Map.Entry<Song, Double>> allSongs = new ArrayList<>(songScores.entrySet());
        allSongs.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        opCountHeap += allSongs.size(); // counting sort comparisons (approx)

        // ước lượng bộ nhớ (memory estimate): neighbor map + song scores map + sorted list
        extraMemoryBytes += (topNeighbors.size() * 64L)
                + (songScores.size() * 64L)
                + (allSongs.size() * 32L);

        List<Song> results = new ArrayList<>();
        int limit = Math.min(k, allSongs.size());
        for (int i = 0; i < limit; i++) results.add(allSongs.get(i).getKey());
        return results;
    }

    //O(U * log K)
    public List<Song> recommendV2_MinHeap(User target, int k) {
        Map<Song, Interaction> targetHistory = data.get(target);
        Map<User, Double> topNeighbors = new HashMap<>();

        for (Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            if (entry.getKey().equals(target)) continue;
            opCountSimilarity++;
            double sim = Similarity.cosine(targetHistory, entry.getValue());
            if (sim > SIM_THRESHOLD) topNeighbors.put(entry.getKey(), sim);
        }

        Map<Song, Double> songScores = Scoring.score(target, data, topNeighbors);
        opCountScore += songScores.size();

        PriorityQueue<Map.Entry<Song, Double>> heapSongs = new PriorityQueue<>(
                Comparator.comparingDouble(Map.Entry::getValue));

        for (Map.Entry<Song, Double> entry : songScores.entrySet()) {
            heapSongs.offer(entry);
            opCountHeap++;
            if (heapSongs.size() > k) {
                heapSongs.poll();
                opCountHeap++;
            }
        }

        extraMemoryBytes += (topNeighbors.size() * 64L)
                + (songScores.size() * 64L)
                + ((k + 1) * 32L);

        List<Song> results = new ArrayList<>();
        while (!heapSongs.isEmpty()) results.add(0, heapSongs.poll().getKey());
        return results;
    }
}
