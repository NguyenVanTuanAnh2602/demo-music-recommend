package benchmark;

import data.*;
import algorithm.*;

import java.util.*;

public class BenchmarkRunner {
    private static final int WARMUP_RUNS = 2;
    private static final int MEASURE_RUNS = 5;

    public static List<BenchmarkResult> run(List<Recommender> recommenders, List<User> testUsers, int k) {
        List<BenchmarkResult> results = new ArrayList<>();

        for (Recommender rec : recommenders) {
            // warm up
            for (int i = 0; i < WARMUP_RUNS; i++) {
                runOnce(rec, testUsers, k, false);
            }

            // measure
            long totalTime = 0;

            for (int i = 0; i < MEASURE_RUNS; i++) {
                long time = runOnce(rec, testUsers, k, true);
                totalTime += time;
            }

            long avgTime = totalTime / MEASURE_RUNS;

            results.add(new BenchmarkResult(rec.getClass().getSimpleName(), avgTime, k));
        }

        return results;
    }

    private static long runOnce(Recommender rec, List<User> users, int k, boolean measure) {
        List<User> shuffled = new ArrayList<>(users);
        Collections.shuffle(shuffled, new Random(42));

        long start = 0;

        if (measure) {
            start = System.nanoTime();
        }

        for (User u : shuffled) {
            rec.recommend(u, k);
        }

        if (measure) {
            return System.nanoTime() - start;
        }

        return 0;
    }
}
