package algorithm;

import data.*;
import util.Scoring;
import util.Similarity;

import java.util.*;
public class GreedyRecommender implements Recommender {

    private final Map<User, Map<Song, Interaction>> data;

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

    public GreedyRecommender(Map<User, Map<Song, Interaction>> data) {
        this.data = data;
    }

    @Override
    public List<Song> recommend(User targetUser, int k) {
        if (!data.containsKey(targetUser)) {
            return new ArrayList<>();
        }

        return recommendV2_GreedySong(targetUser, k);
    }

    public List<Song> recommendV1_GreedyNeighbor(User target, int k) {
        Map<Song, Interaction> targetHistory = data.get(target);

        List<Map.Entry<User, Double>> rankedNeighbors = new ArrayList<>();

        for (Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            if (entry.getKey().equals(target)) continue;

            opCountSimilarity++;
            double sim = Similarity.cosine(targetHistory, entry.getValue());
            rankedNeighbors.add(Map.entry(entry.getKey(), sim));
        }
        extraMemoryBytes += rankedNeighbors.size() * 64L;

        rankedNeighbors.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        opCountHeap += rankedNeighbors.size();

        Map<User, Double> selectedNeighbors = new HashMap<>();
        Set<Song> newSongPool = new HashSet<>();

        for (Map.Entry<User, Double> entry : rankedNeighbors) {
            if (newSongPool.size() >= k) break;

            User neighbor = entry.getKey();
            double sim = entry.getValue();

            selectedNeighbors.put(neighbor, sim);

            for (Song song : data.get(neighbor).keySet()) {
                if (!targetHistory.containsKey(song)) {
                    newSongPool.add(song);
                }
            }
        }
        extraMemoryBytes += selectedNeighbors.size() * 64L + newSongPool.size() * 32L;

        if (selectedNeighbors.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Song, Double> songScores = Scoring.score(target, data, selectedNeighbors);
        opCountScore += songScores.size();

        PriorityQueue<Map.Entry<Song, Double>> heap = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

        for (Map.Entry<Song, Double> entry : songScores.entrySet()) {
            heap.offer(entry);
            opCountHeap++;

            if (heap.size() > k) {
                heap.poll();
                opCountHeap++;
            }
        }
        extraMemoryBytes += (k + 1) * 32L;

        List<Song> results = new ArrayList<>();

        while (!heap.isEmpty()) {
            results.add(0, heap.poll().getKey());
        }

        return results;
    }


    public List<Song> recommendV2_GreedySong(User target, int k) {
        Map<Song, Interaction> targetHistory = data.get(target);

        List<Map.Entry<User, Double>> rankedNeighbors = new ArrayList<>();

        for (Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            if (entry.getKey().equals(target)) continue;

            opCountSimilarity++;
            double sim = Similarity.cosine(targetHistory, entry.getValue());
            rankedNeighbors.add(Map.entry(entry.getKey(), sim));
        }

        extraMemoryBytes += rankedNeighbors.size() * 64L;

        rankedNeighbors.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        opCountHeap += rankedNeighbors.size();

        Set<Song> seen = new HashSet<>(targetHistory.keySet());
        List<Song> results = new ArrayList<>();

        for (Map.Entry<User, Double> neighborEntry : rankedNeighbors) {
            if (results.size() >= k) break;

            User neighbor = neighborEntry.getKey();

            List<Map.Entry<Song, Interaction>> neighborSongs = new ArrayList<>(data.get(neighbor).entrySet());
            neighborSongs.sort((a, b) -> Double.compare(b.getValue().getPlayCount(), a.getValue().getPlayCount()));
            opCountHeap += neighborSongs.size();

            for (Map.Entry<Song, Interaction> songEntry : neighborSongs) {
                if (results.size() >= k) break;

                Song song = songEntry.getKey();

                if (!seen.contains(song)) {
                    results.add(song);
                    seen.add(song);
                    opCountScore++;
                }
            }
        }

        extraMemoryBytes += seen.size() * 32L + results.size() * 32L;

        return results;
    }
}