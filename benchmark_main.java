import Data.InputData;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import HeuristicApproach.*;




/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */
public class benchmark_main_class {

    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) {
        File dir = new File("ALL_tsp");
        File[] files = dir.listFiles();
        if (files == null) {
            System.err.println("Directory not found or empty: " + dir.getAbsolutePath());
            return;
        }
        long total_run_time = 0l;
        final int max_dimension = 500;
        // Load best known values from CSV
        Map<String, String> bestKnownMap = loadBestKnown("tsplib_best_known.csv");
//        System.out.println(bestKnownMap.toString());
//        System.exit(0);

        // Output CSV
        String outputFile = "heuristic approach results.csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Header
            writer.println("File Name,Dimension,Best Solution Reach Time(ms),Cost Value,Known Optimal,Gap(%)");

            // Build and sort by StopsCount
            List<InputData> datasets = Arrays.stream(files)
                                            .parallel()
                                            .map(file -> {
                                                try {
                                                    return new InputData(file, max_dimension);
                                                } catch (RuntimeException ex) {
                                                    System.err.println("Skipping " + file.getName() + " (load failed): " + ex.getMessage());
                                                    return null;
                                                }
                                            })
                                            .filter(Objects::nonNull)
                                            .filter(data -> {
                                                if (data.StopsCount <= max_dimension)
                                                    return true;
                                                else {
                                                    data.close();
                                                    return false;
                                                }
                                            })
                                            .sorted()
                                            .toList();

            // Process in sorted order
            for (InputData d : datasets) {
                MetaHeuristic algorithm = new GeneticAlgorithm(d);
                algorithm.Run();
                GiantTour sol = algorithm.getBestSolution();

                // Print/display solution
                long end_time = algorithm.getRunningTime();
                System.out.println("\nEnd Time = " + end_time + " ms");
                sol.displaySolution();
                // Lookup best known
                String best = bestKnownMap.getOrDefault(stripExtension(d.FileName), "NA");

                // Compute gap
                String gapStr = "NA";
                if (best.compareTo("NA") != 0) {
                    double value = Double.parseDouble(best);
                    double gap = (sol.getCost() - value) / value;
                    gapStr = String.format(Locale.US, "%.2f", gap  * 100d);
                }
                total_run_time += end_time;
                // Write result to CSV
                writer.printf(Locale.US, "%s,%s,%s,%s,%s,%s\n", d.FileName, d.StopsCount, end_time, sol.getCost(), best, gapStr);
                d.close();
            }
        } catch (IOException e) {
            System.err.println("Error writing results: " + e.getMessage());
        }

        System.out.println("All results time " + total_run_time + " ms");
        System.out.println("All results stored in \"" + outputFile + "\"");
    }

    // Helper: read tsplib_best_known.csv into a map
    private static Map<String, String> loadBestKnown(String csvPath) {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(Path.of(csvPath))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    map.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading best known CSV: " + e.getMessage());
        }
        return map;
    }

    // Helper: strip extension like .tsp or .txt
    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? filename : filename.substring(0, dot);
    }
}