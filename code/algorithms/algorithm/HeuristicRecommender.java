package algorithm;

import data.*;
import util.*;

import java.util.*;

public class HeuristicRecommender implements Recommender {
    private Map<User, Map<Song, Interaction>> data;
    private Map<Song, Set<User>> invertedIndex;
    private HeuristicVersion  version;

    public long opCountSimilarity = 0;
    public long opCountHeap       = 0;
    public long opCountScore      = 0;
    public long extraMemoryBytes  = 0;

    public void resetCounters() {
        opCountSimilarity = 0;
        opCountHeap = 0;
        opCountScore = 0;
        extraMemoryBytes = 0;
    }

    public HeuristicRecommender(Map<User, Map<Song, Interaction>> data) {
        this.data = data;
        this.invertedIndex = buildInvertedIndex(data);
        this.version = HeuristicVersion.V2;
    }

    public HeuristicRecommender(Map<User, Map<Song, Interaction>> data, HeuristicVersion version) {
        this.data = data;
        this.invertedIndex = buildInvertedIndex(data);
        this.version = version;
    }

    @Override
    public List<Song> recommend(User user, int k) {
        // similarity
        Map<User, Map<Song, Interaction>> filteredData;

        switch(version) {
            case V1:
                filteredData = filterData(user);
                break;
            case V2:
                filteredData = filterDataBySong(user);
                break;
            default:
                filteredData = new HashMap<>();
        }

        Map<User, Double> similarityMap = new HashMap<>();

        for (User other : filteredData.keySet()) {
            if (other.equals(user)) continue;

            opCountSimilarity++;

            double sim =  Similarity.cosine(filteredData.get(user), filteredData.get(other));

            if (sim > 0) {
                similarityMap.put(other, sim);
            }
        }

        if (similarityMap.isEmpty()) {
            return Collections.emptyList();
        }

        // score songs
        Map<Song, Double> scores = Scoring.score(user, filteredData, similarityMap);

        if (scores == null || scores.isEmpty()) {
            return Collections.emptyList();
        }

        opCountScore += scores.size();

        // extract top k
        PriorityQueue<Map.Entry<Song, Double>> heap = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

        for (Map.Entry<Song, Double> entry : scores.entrySet()) {
            heap.offer(entry);
            opCountHeap++;

            if (heap.size() > k) {
                heap.poll();
                opCountHeap++;
            }
        }

        extraMemoryBytes += (similarityMap.size() * 64L) + (scores.size() * 64L) + ((k + 1) * 32L);

        List<Song> results = new ArrayList<>();

        while (!heap.isEmpty()) {
            results.add(0, heap.poll().getKey());
        }

        return results;
    }

    // v1
    private Map<User, Map<Song, Interaction>> filterData(User user) {
        Map<User, Map<Song, Interaction>> filterData = new HashMap<>();

        for(Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            if (user.equals(entry.getKey())) {
                filterData.put(entry.getKey(), entry.getValue());
                continue;
            }

            int count =  0;
            double alpha = 0.2;
            double n = alpha * Math.min(data.get(user).size(), entry.getValue().size());

            for (Song song : entry.getValue().keySet()) {
                if(data.get(user).containsKey(song)) {
                    count++;
                }

                if (count >= n) {
                    filterData.put(entry.getKey(), entry.getValue());
                    break;
                }
            }
        }

        return filterData;
    }


    //inverted index
    private Map<Song, Set<User>> buildInvertedIndex(Map<User, Map<Song, Interaction>> data) {
        Map<Song, Set<User>> index = new HashMap<>();

        for (Map.Entry<User, Map<Song, Interaction>> entry : data.entrySet()) {
            for (Song song : entry.getValue().keySet()) {
                index.computeIfAbsent(song, k -> new HashSet<>()).add(entry.getKey());
            }
        }

        return index;
    }

    // v2
    private Map<User, Map<Song, Interaction>> filterDataBySong(User user) {
        Map<User, Integer> overlapCount = new HashMap<>();

        for (Song song : data.get(user).keySet()) {
            Set<User> listeners = invertedIndex.getOrDefault(song, Collections.emptySet());

            for (User other : listeners) {
                if (!other.equals(user)) {
                    overlapCount.merge(other, 1, Integer::sum);
                }
            }
        }

        Map<User, Map<Song, Interaction>> filteredData = new HashMap<>();
        filteredData.put(user, data.get(user));

        int userSongCount = data.get(user).size();
        double alpha = 0.2;

        for (Map.Entry<User, Integer> entry : overlapCount.entrySet()) {
            User other = entry.getKey();
            double threshold = alpha * Math.min(userSongCount, data.get(other).size());

            if (entry.getValue() >= threshold) {
                filteredData.put(other, data.get(other));
            }
        }

        return filteredData;
    }
}
