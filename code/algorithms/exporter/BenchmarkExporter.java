package exporter;

import benchmark.*;
import algorithm.*;

import java.io.*;
import java.util.List;

public class BenchmarkExporter {

    public static void exportToCSV(String filePath, List<BenchmarkResult> results) {
        try {
            File file = new File(filePath);
            File parent = file.getParentFile();

            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("Algorithm|AvgTime(ns)|K\n");

                for (BenchmarkResult r : results) {
                    writer.write(sanitize(r.getAlgorithmName()) + "|" + r.getTimeNano() + "|" + r.getK() + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sanitize(String s) {
        if (s == null) return "";

        return s.replace("|", " ").replace("\n", " ");
    }
}