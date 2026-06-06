package util;

import data.*;

import java.util.Map;

public class Similarity {
    private static SimilarityVersion version =  SimilarityVersion.V3;

    public Similarity(SimilarityVersion version) {
        this.version = version;
    }

    public static double cosine(Map<Song, Interaction> u1, Map<Song, Interaction> u2) {
        if (u1 == null || u2 == null || u1.isEmpty() || u2.isEmpty()) {
            return 0.0;
        }

        double dot = 0.0;

        switch (version) {
            case V1:
                dot = computeDotV1(u1, u2);
                break;
            case V2:
                dot = computeDotV2(u1, u2);
                break;
            case V3:
                dot = computeDotV3(u1, u2);
                break;
            default:
                dot = 0.0;
        }

        if (dot == 0.0) return 0.0;

        double norm1 = computeNorm(u1);
        double norm2 = computeNorm(u2);

        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;

        return dot / Math.sqrt(norm1 * norm2);
    }


    // V1 - Brute Force O(N*M)
    private static double computeDotV1(Map<Song, Interaction> u1, Map<Song, Interaction> u2) {
        double dot = 0.0;

        for (Song s1 : u1.keySet()) {
            for (Song s2 : u2.keySet()) {
                if (s1.equals(s2)) {
                    dot += u1.get(s1).getPlayCount() * u2.get(s2).getPlayCount();
                }
            }
        }

        return dot;
    }


    // V2 - Hash lookup + O(min(N,M)) loop
    private static double computeDotV2(Map<Song, Interaction> u1, Map<Song, Interaction> u2) {
        double dot = 0.0;

        Map<Song, Interaction> smaller = (u1.size() < u2.size()) ? u1 : u2;

        Map<Song, Interaction> larger = (u1.size() < u2.size()) ? u2 : u1;

        for (Song s : smaller.keySet()) {
            Interaction i = larger.get(s);

            if (i != null) {
                dot += smaller.get(s).getPlayCount() * i.getPlayCount();
            }
        }

        return dot;
    }


    // V3 - Clean canonical version (entrySet, minimal overhead)
    private static double computeDotV3(Map<Song, Interaction> u1, Map<Song, Interaction> u2) {
        double dot = 0.0;

        Map<Song, Interaction> smaller = (u1.size() < u2.size()) ? u1 : u2;

        Map<Song, Interaction> larger = (u1.size() < u2.size()) ? u2 : u1;

        for (Map.Entry<Song, Interaction> e : smaller.entrySet()) {
            Interaction other = larger.get(e.getKey());

            if (other != null) {
                dot += e.getValue().getPlayCount() * other.getPlayCount();
            }
        }

        return dot;
    }


    // Norm (shared)
    private static double computeNorm(Map<Song, Interaction> u) {
        double norm = 0.0;

        for (Interaction i : u.values()) {
            double v = i.getPlayCount();
            norm += v * v;
        }

        return norm;
    }
}