package algorithm;

import data.*;
import util.*;
import java.util.*;
import java.util.stream.Collectors;

public class BruteForceRecommender implements Recommender {
    private Map<User, Map<Song, Interaction>> data;

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

    public BruteForceRecommender(Map<User, Map<Song, Interaction>> data) {
        this.data = data;
    }

    @Override
    public List<Song> recommend(User user, int k) {
        Map<Song, Interaction> userHistory = data.getOrDefault(user, new HashMap<>());
        Set<Song> heardSongs = userHistory.keySet();

        Map<User, Double> neighbours = new HashMap<>();
        for (User other : data.keySet()) {
            if (!other.equals(user)) {
                opCountSimilarity++;
                double sim = Similarity.cosine(userHistory, data.get(other));
                neighbours.put(other, sim);
            }
        }

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
}
