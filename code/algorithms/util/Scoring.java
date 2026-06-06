package util;

import data.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Scoring {
    private static ScoringVersion version =  ScoringVersion.V4;

    public Scoring(ScoringVersion version) {
        this.version = version;
    }

    public static Map<Song, Double> score(User target, Map<User, Map<Song, Interaction>> data, Map<User, Double> neighbours) {
        if (target == null || data == null || neighbours == null || neighbours.isEmpty()) {
            return new HashMap<>();
        }

        Map<Song, Interaction> targetHistory = data.getOrDefault(target, new HashMap<>());

        switch(version) {
            case V1:
                return computeScoreV1(targetHistory, data, neighbours);
            case V2:
                return computeScoreV2(targetHistory, data, neighbours);
            case V3:
                return computeScoreV3(targetHistory, data, neighbours);
            case V4:
                return computeScoreV4(targetHistory, data, neighbours);
            default:
                return new HashMap<>();
        }
    }


    // V1 - Brute Force toàn bộ bài hát trong hệ thống
    // O(I * K)
    private static Map<Song, Double> computeScoreV1(Map<Song, Interaction> targetHistory, Map<User, Map<Song, Interaction>> data, Map<User, Double> neighbours) {
        Map<Song, Double> scores = new HashMap<>();

        Set<Song> allSongsInSystem = new HashSet<>();

        for (Map<Song, Interaction> userSongs : data.values()) {
            allSongsInSystem.addAll(userSongs.keySet());
        }

        for (Song song : allSongsInSystem) {
            if (targetHistory.containsKey(song)) continue;

            double totalScore = 0.0;

            for (Map.Entry<User, Double> neighborEntry : neighbours.entrySet()) {
                User neighbor = neighborEntry.getKey();
                double similarity = neighborEntry.getValue();

                Map<Song, Interaction> neighborSongs = data.get(neighbor);

                if (neighborSongs != null && neighborSongs.containsKey(song)) {
                    totalScore += similarity * neighborSongs.get(song).getPlayCount();
                }
            }

            if (totalScore > 0) {
                scores.put(song, totalScore);
            }
        }

        return scores;
    }


    // V2 - Chỉ duyệt bài hát của hàng xóm
    // O(K * M)
    private static Map<Song, Double> computeScoreV2(Map<Song, Interaction> targetHistory, Map<User, Map<Song, Interaction>> data, Map<User, Double> neighbours) {
        Map<Song, Double> scores = new HashMap<>();

        for (Map.Entry<User, Double> neighborEntry : neighbours.entrySet()) {
            User neighbor = neighborEntry.getKey();
            double similarity = neighborEntry.getValue();

            Map<Song, Interaction> neighborSongs = data.get(neighbor);

            if (neighborSongs == null) continue;

            for (Interaction interaction : neighborSongs.values()) {
                Song song = interaction.getSong();

                if (!targetHistory.containsKey(song)) {
                    double playCount = interaction.getPlayCount();

                    double currentScore = scores.getOrDefault(song, 0.0);

                    scores.put(song, currentScore + (similarity * playCount));
                }
            }
        }

        return scores;
    }


    // V3 - Similarity pruning + Map.merge()
    private static Map<Song, Double> computeScoreV3(Map<Song, Interaction> targetHistory, Map<User, Map<Song, Interaction>> data, Map<User, Double> neighbours) {
        Map<Song, Double> scores = new HashMap<>();

        for (Map.Entry<User, Double> neighborEntry : neighbours.entrySet()) {
            double similarity = neighborEntry.getValue();

            if (similarity <= 0) {
                continue;
            }

            User neighbor = neighborEntry.getKey();

            Map<Song, Interaction> neighborSongs = data.get(neighbor);

            if (neighborSongs == null) continue;

            for (Interaction interaction : neighborSongs.values()) {
                Song song = interaction.getSong();

                if (!targetHistory.containsKey(song)) {
                    double addedScore = similarity * interaction.getPlayCount();

                    scores.merge(song, addedScore, Double::sum);
                }
            }
        }

        return scores;
    }


    // V4 - Tối ưu iteration và lookup
    private static Map<Song, Double> computeScoreV4(Map<Song, Interaction> targetHistory, Map<User, Map<Song, Interaction>> data, Map<User, Double> neighbours) {
        Map<Song, Double> scores = new HashMap<>();

        for (Map.Entry<User, Double> entry : neighbours.entrySet()) {
            double similarity = entry.getValue();

            if (similarity <= 0) continue;

            Map<Song, Interaction> neighbourSongs = data.get(entry.getKey());

            if (neighbourSongs == null) continue;

            for (Map.Entry<Song, Interaction> songEntry : neighbourSongs.entrySet()) {
                Song song = songEntry.getKey();

                if (targetHistory.containsKey(song)) continue;

                double addedScore = similarity * songEntry.getValue().getPlayCount();

                scores.merge(song, addedScore, Double::sum);
            }
        }

        return scores;
    }
}