package algorithm;

import data.*;
import java.util.*;
import util.Scoring;
import util.Similarity;

public class CachingRecommender implements Recommender {

    private final Map<User, Map<Song, Interaction>> data;
    private final Map<String, Double> memoCache;
    private Map<Song, Map<User, Double>> dpSongTable;
    private boolean dpTableBuilt = false;
    private Map<User, Double> userNorms;

    private long dpTableMemoryBytes = 0;

    public long opCountCacheMiss = 0;
    public long opCountCacheHit  = 0;
    public long opCountHeap      = 0;
    public long opCountScore     = 0;
    public long extraMemoryBytes = 0;

    public void resetCounters() {
        opCountCacheMiss = 0;
        opCountCacheHit  = 0;
        opCountHeap      = 0;
        opCountScore     = 0;
        extraMemoryBytes = dpTableMemoryBytes;
    }

    public CachingRecommender(Map<User, Map<Song, Interaction>> data) {
        this.data      = data;
        this.memoCache = new HashMap<>();
    }

    private String getMemoKey(User u1, User u2) {
        String id1 = u1.getUserId();
        String id2 = u2.getUserId();

        return (id1.compareTo(id2) < 0) ? id1 + "||" + id2 : id2 + "||" + id1;
    }

    @Override
    public List<Song> recommend(User targetUser, int k) {
        if (!data.containsKey(targetUser)) {
            return new ArrayList<>();
        }

        return recommendV2_BottomUpTabulation(targetUser, k);
    }

    public void warmUp() { buildDPTable(); }
    public boolean isWarmedUp() { return dpTableBuilt; }

    private List<Song> scoreAndExtractTopK(User targetUser, Map<User, Double> neighbors, int k) {
        Map<Song, Double> songScores = Scoring.score(targetUser, data, neighbors);
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

        extraMemoryBytes += (neighbors.size() * 64L) + (songScores.size() * 64L) + ((k + 1) * 32L);

        List<Song> results = new ArrayList<>();

        while (!heap.isEmpty()) {
            results.add(0, heap.poll().getKey());
        }
        return results;
    }

    public List<Song> recommendV1_TopDownMemo(User targetUser, int k) {
        Map<Song, Interaction> targetHistory = data.get(targetUser);
        Map<User, Double>      neighbors     = new HashMap<>();

        long missAtStart = opCountCacheMiss;

        for (Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            User otherUser = entry.getKey();
            if (otherUser.equals(targetUser)) continue;

            String key = getMemoKey(targetUser, otherUser);
            double sim;

            if (memoCache.containsKey(key)) {
                sim = memoCache.get(key);
                opCountCacheHit++;
            } else {
                sim = Similarity.cosine(targetHistory, entry.getValue());
                memoCache.put(key, sim);
                opCountCacheMiss++;
            }

            if (sim > 0) neighbors.put(otherUser, sim);
        }

        long newEntries = opCountCacheMiss - missAtStart;
        extraMemoryBytes += newEntries * 80L;

        return scoreAndExtractTopK(targetUser, neighbors, k);
    }

    private void buildDPTable() {
        if (dpTableBuilt) return;

        dpSongTable = new HashMap<>();
        userNorms   = new HashMap<>();
        long cellsStored = 0;

        for (Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            User v = entry.getKey();
            double normSq  = 0.0;

            for (Map.Entry<Song, Interaction> songEntry : entry.getValue().entrySet()) {
                Song s = songEntry.getKey();
                double play = songEntry.getValue().getPlayCount();

                dpSongTable.computeIfAbsent(s, x -> new HashMap<>()).put(v, play);
                cellsStored++;

                normSq += play * play;
            }

            userNorms.put(v, Math.sqrt(normSq));
        }

        dpTableMemoryBytes = cellsStored * 64L + (long) data.size() * 32L;
        extraMemoryBytes += dpTableMemoryBytes;
        dpTableBuilt = true;
    }

    public List<Song> recommendV2_BottomUpTabulation(User targetUser, int k) {
        buildDPTable();

        Map<Song, Interaction> targetHistory = data.get(targetUser);
        double normTarget = userNorms.getOrDefault(targetUser, 0.0);

        if (normTarget == 0.0) {
            return new ArrayList<>();
        }

        Map<User, Double> dotProducts = new HashMap<>();

        for (Map.Entry<Song, Interaction> targetSongEntry : targetHistory.entrySet()) {
            Song song = targetSongEntry.getKey();
            double playTarget = targetSongEntry.getValue().getPlayCount();

            Map<User, Double> listeners = dpSongTable.get(song);
            if (listeners == null) continue;

            for (Map.Entry<User, Double> listenerEntry : listeners.entrySet()) {
                User v = listenerEntry.getKey();
                if (v.equals(targetUser)) continue;

                double contrib = playTarget * listenerEntry.getValue();
                dotProducts.merge(v, contrib, Double::sum);
                opCountCacheMiss++;
            }
        }

        opCountCacheHit += dotProducts.size();

        Map<User, Double> neighbors = new HashMap<>();

        for (Map.Entry<User, Double> entry : dotProducts.entrySet()) {
            User v = entry.getKey();
            double dot = entry.getValue();
            double normV = userNorms.getOrDefault(v, 0.0);

            if (normV == 0.0) continue;

            double sim = dot / (normTarget * normV);
            if (sim > 0) {
                neighbors.put(v, sim);
            }
        }

        if (neighbors.isEmpty()) {
            return new ArrayList<>();
        }

        return scoreAndExtractTopK(targetUser, neighbors, k);
    }
}