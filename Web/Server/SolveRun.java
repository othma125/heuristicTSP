// Author: Othmane

package Web.Server;

import Algorithm.Data.InputData;
import Algorithm.HeuristicApproach.GeneticAlgorithm;
import Algorithm.HeuristicApproach.Tour;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * A single solve of one instance, streamed to one browser: it owns the log
 * redirection, the keep-alive watchdog, the solver itself, and the final
 * {@code result} event. Created per request, so nothing about a run leaks into
 * static state except the handle {@link SolveHandler} keeps to stop it.
 *
 * @author Othmane EL YAAKOUBI
 */
final class SolveRun {

    /** Sent when the run fails, so the browser always leaves its "solving" state. */
    private static final String FAILED = "{\"cost\":0,\"timeMs\":0,\"optimal\":null,\"gap\":null,\"tour\":[]}";

    private final File instance;
    private final Sse sse;
    private volatile GeneticAlgorithm algorithm;

    /**
     * Prepares a run; nothing starts until {@link #run()} is called.
     *
     * @param instance the {@code .tsp} file to solve
     * @param sse the connection streaming the log and the result
     */
    SolveRun(File instance, Sse sse) {
        this.instance = instance;
        this.sse = sse;
    }

    /**
     * Runs the memetic algorithm to completion (or until stopped) and closes the
     * connection.
     *
     * <p>The solver reports progress on {@code System.out}, which is process-wide:
     * this is why only one run may be active at a time (see {@link SolveHandler}).
     *
     * @throws IOException when writing to the connection fails
     */
    void run() throws IOException {
        PrintStream original = System.out;
        System.setOut(sse.logStream(this::requestStop));
        // The solver only prints on improvement, so it can run silent for minutes and never
        // notice a closed tab. Ping instead: a failed write means nobody is listening.
        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
        watchdog.scheduleWithFixedDelay(this::ping, 5, 5, TimeUnit.SECONDS);
        try (InputData data = new InputData(instance)) {
            algorithm = new GeneticAlgorithm(data);
            algorithm.Run();

            Tour best = algorithm.getBestSolution();
            sse.event("sol", tourText(best));
            sse.event("result", resultJson(best, algorithm.getRunningTime()));
        } catch (Exception e) {
            System.setOut(original);
            sse.event("log", "ERROR: " + e.getMessage());
            sse.event("result", FAILED);
        } finally {
            System.setOut(original);
            watchdog.shutdownNow();
            sse.close();
        }
    }

    /** Asks the solver to stop early; keeps the best tour found so far. */
    void requestStop() {
        GeneticAlgorithm running = algorithm;
        if (running != null)
            running.requestStop();
    }

    /** Keep-alive probe: a failed write means the tab is gone, so stop solving. */
    private void ping() {
        try {
            sse.event("ping", "");
        } catch (IOException hungUp) {
            requestStop();
        }
    }

    /**
     * Builds the JSON payload for the final {@code result} event.
     *
     * @param best the best tour found
     * @param ms the running time in milliseconds
     * @return the JSON string for the result event
     */
    private String resultJson(Tour best, long ms) {
        int cost = (int) best.getCost();
        double optimal = Instances.optimalOf(instance);
        String opt = Double.isNaN(optimal) ? "null" : Double.toString(optimal);
        String gap = Double.isNaN(optimal) || optimal == 0
                ? "null"
                : String.format(Locale.US, "%.2f", (cost - optimal) / optimal * 100d);
        return "{\"cost\":" + cost
                + ",\"timeMs\":" + ms
                + ",\"optimal\":" + opt
                + ",\"gap\":" + gap
                + ",\"tour\":" + tourJson(best) + "}";
    }

    /**
     * Builds a TSPLIB {@code .tour} text for the Save panel.
     *
     * @param tour the best tour found
     * @return the {@code .tour} file contents
     */
    private String tourText(Tour tour) {
        StringBuilder sb = new StringBuilder();
        sb.append("NAME : ").append(Instances.nameOf(instance)).append(".tour\n");
        sb.append("COMMENT : Length ").append((int) tour.getCost()).append('\n');
        sb.append("TYPE : TOUR\n");
        sb.append("DIMENSION : ").append(tour.getLength()).append('\n');
        sb.append("TOUR_SECTION\n");
        for (int s : tour.getSequence())
            sb.append(s + 1).append('\n');
        sb.append("-1\nEOF\n");
        return sb.toString();
    }

    /**
     * Node ids (1-based) of the tour, as a JSON array for the visualizer.
     *
     * @param tour the best tour found
     * @return the JSON array of node ids
     */
    private static String tourJson(Tour tour) {
        return Arrays.toString(IntStream.of(tour.getSequence()).map(s -> s + 1).toArray());
    }
}
