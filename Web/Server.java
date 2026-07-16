// Author: Othmane

package Web;

import Algorithm.Data.InputData;
import Algorithm.HeuristicApproach.GeneticAlgorithm;
import Algorithm.HeuristicApproach.Tour;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Minimal landing-page backend built on the JDK's {@link HttpServer} (no
 * dependencies). Serves {@code Web/index.html}, lists TSPLIB instances, streams
 * the solver log live over Server-Sent Events, and returns the best tour for
 * in-browser visualization.
 *
 * @author Othmane EL YAAKOUBI
 */
public class Server {

    /** Static utility class; not instantiable. */
    private Server() {
    }

    private static final File TSP_DIR = new File("Algorithm/ALL_tsp");
    private static final File BEST_KNOWN = new File(TSP_DIR, "tsplib_best_known.csv");
    private static final String TSP_EXT = ".tsp";

    // ponytail: one solve at a time — System.out is redirected globally to the
    // SSE stream while solving. Per-session isolation only if concurrency matters.
    private static final Object SOLVE_LOCK = new Object();
    // The solver currently running, so /api/stop can ask it to stop early.
    private static volatile GeneticAlgorithm currentAlgo;

    /**
     * Starts the HTTP server on the port supplied as the first argument, or on the
     * port read from {@code .env} (default 8080), and registers all API contexts.
     *
     * @param args optional port number
     * @throws IOException if the server socket cannot be created
     */
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : envPort();
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (BindException e) {
            System.err.println("Port " + port + " is already in use. Stop the running server"
                    + " (./kill-server.sh) or pick another port: java -cp out Web.Server 9090");
            System.exit(1);
            return;
        }
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/", Server::serveIndex);
        server.createContext("/app.js", ex -> serveFile(ex, new File("Web/app.js"), "text/javascript; charset=utf-8"));
        server.createContext("/styles.css", ex -> serveFile(ex, new File("Web/styles.css"), "text/css; charset=utf-8"));
        server.createContext("/assets/profile.jpg", ex -> serveFile(ex, new File("profile.jpg"), "image/jpeg"));
        server.createContext("/api/instances", Server::instances);
        server.createContext("/api/tsp", Server::tsp);
        server.createContext("/api/solve", Server::solve);
        server.createContext("/api/stop", Server::stop);

        server.start();
        System.out.println("Landing page ready -> http://localhost:" + port);
    }

    /** Reads PORT from the .env file at the project root; defaults to 8080. */
    private static int envPort() {
        File env = new File(".env");
        if (env.exists()) {
            try {
                for (String line : Files.readAllLines(env.toPath())) {
                    String t = line.trim();
                    if (t.startsWith("PORT=")) return Integer.parseInt(t.substring(5).trim());
                }
            } catch (IOException | NumberFormatException ignored) {
                // Fall back to the default port.
            }
        }
        return 8080;
    }

    /* ---------------- endpoints ---------------- */

    /**
     * Serves the landing page ({@code Web/index.html}) for the root path only.
     *
     * @param ex the HTTP exchange
     * @throws IOException when writing the response fails
     */
    private static void serveIndex(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) {
            send(ex, 404, "text/plain", "Not found".getBytes());
            return;
        }
        serveFile(ex, new File("Web/index.html"), "text/html; charset=utf-8");
    }

    /**
     * Returns the list of {@code .tsp} instances in {@link #TSP_DIR} as a JSON
     * string array (without the extension).
     *
     * @param ex the HTTP exchange
     * @throws IOException when writing the response fails
     */
    private static void instances(HttpExchange ex) throws IOException {
        String[] files = TSP_DIR.list((d, n) -> n.endsWith(TSP_EXT));
        List<String> names = files == null ? List.of()
                : Arrays.stream(files).map(n -> n.substring(0, n.length() - TSP_EXT.length()))
                        .sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        send(ex, 200, "application/json", jsonStringArray(names).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Streams the raw {@code .tsp} instance file for the requested file.
     *
     * @param ex the HTTP exchange
     * @throws IOException when reading the file or writing the response fails
     */
    private static void tsp(HttpExchange ex) throws IOException {
        File f = tspFile(query(ex));
        if (f == null || !f.exists()) {
            send(ex, 404, "text/plain", "Not found".getBytes());
            return;
        }
        serveFile(ex, f, "text/plain; charset=utf-8");
    }

    /** SSE: streams the live solver log, then a final {@code result} event with cost/time/tour. */
    private static void solve(HttpExchange ex) throws IOException {
        File instance = tspFile(query(ex));

        ex.getResponseHeaders().set("Content-Type", "text/event-stream");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, 0);

        OutputStream out = ex.getResponseBody();
        if (instance == null || !instance.exists()) {
            sse(out, "log", "Instance not found");
            out.close();
            return;
        }

        synchronized (SOLVE_LOCK) {
            solveLocked(instance, out);
        }
    }

    /**
     * Runs the memetic algorithm for the given instance while redirecting the
     * process standard output to the SSE stream. Sends the final {@code result}
     * event when the run completes or aborts.
     *
     * @param instance the TSP instance file to solve
     * @param out the output stream for the SSE connection
     * @throws IOException when writing SSE events fails
     */
    private static void solveLocked(File instance, OutputStream out) throws IOException {
        PrintStream original = System.out;
        System.setOut(new PrintStream(new SseLineStream(out), true, StandardCharsets.UTF_8));
        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
        // The solver only prints on improvement, so it can run silent for minutes and never
        // notice a closed tab. Ping instead: a failed write means nobody is listening.
        watchdog.scheduleWithFixedDelay(() -> {
            try {
                sse(out, "ping", "");
            } catch (IOException hungUp) {
                GeneticAlgorithm algorithm = currentAlgo;
                if (algorithm != null)
                    algorithm.requestStop();
            }
        }, 5, 5, TimeUnit.SECONDS);
        try (InputData data = new InputData(instance)) {
            GeneticAlgorithm algo = new GeneticAlgorithm(data);
            currentAlgo = algo;
            algo.Run();
            System.setOut(original);

            Tour best = algo.getBestSolution();
            int cost = (int) best.getCost();
            sse(out, "sol", tourText(instance, best));
            sse(out, "result", resultJson(cost, algo.getRunningTime(),
                    tourJson(best), optimalOf(instance)));
        } catch (Exception e) {
            System.setOut(original);
            sse(out, "log", "ERROR: " + e.getMessage());
            sse(out, "result", "{\"cost\":0,\"timeMs\":0,\"optimal\":null,\"gap\":null,\"tour\":[]}");
        } finally {
            watchdog.shutdownNow();
            currentAlgo = null;
            out.close();
        }
    }

    /**
     * Asks the running solve to stop early.
     *
     * @param ex the HTTP exchange
     * @throws IOException when writing the response fails
     */
    private static void stop(HttpExchange ex) throws IOException {
        GeneticAlgorithm algorithm = currentAlgo;
        if (algorithm != null)
            algorithm.requestStop();
        send(ex, 200, "text/plain", "stopping".getBytes());
    }

    /* ---------------- helpers ---------------- */

    /** Node ids (1-based) of the best tour, as a JSON array for the visualizer. */
    private static String tourJson(Tour tour) {
        return Arrays.toString(IntStream.of(tour.getSequence()).map(s -> s + 1).toArray());
    }

    /** Builds a TSPLIB {@code .tour} text for the Save panel. */
    private static String tourText(File instance, Tour tour) {
        String name = instance.getName().replaceFirst("\\.tsp$", "");
        StringBuilder sb = new StringBuilder();
        sb.append("NAME : ").append(name).append(".tour\n");
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
     * Builds the JSON payload for the final {@code result} SSE event.
     *
     * @param cost the length of the best tour
     * @param ms the running time in milliseconds
     * @param tour the JSON array of node ids
     * @param optimal the best-known cost, or {@code NaN} if unknown
     * @return the JSON string for the result event
     */
    private static String resultJson(int cost, long ms, String tour, double optimal) {
        String opt = Double.isNaN(optimal) ? "null" : Double.toString(optimal);
        String gap = Double.isNaN(optimal) || optimal == 0
                ? "null"
                : String.format(Locale.US, "%.2f", (cost - optimal) / optimal * 100d);
        return "{\"cost\":" + cost
                + ",\"timeMs\":" + ms
                + ",\"optimal\":" + opt
                + ",\"gap\":" + gap
                + ",\"tour\":" + tour + "}";
    }

    /** Reads the best-known cost for the instance from {@code tsplib_best_known.csv}. */
    private static double optimalOf(File tsp) {
        String name = tsp.getName().replaceFirst("\\.tsp$", "");
        if (!BEST_KNOWN.exists()) return Double.NaN;
        try {
            for (String line : Files.readAllLines(BEST_KNOWN.toPath())) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && parts[0].trim().equalsIgnoreCase(name))
                    return Double.parseDouble(parts[1].trim());
            }
        } catch (IOException | NumberFormatException ignored) {
            // No best-known value available.
        }
        return Double.NaN;
    }

    /**
     * Resolves the requested {@code file} query parameter into a {@code .tsp}
     * file, or returns {@code null} if the parameter is missing or unsafe.
     *
     * @param query the parsed query parameters
     * @return the TSP file, or {@code null} if not resolvable
     */
    private static File tspFile(Map<String, String> query) {
        String file = safeName(query.get("file"));
        if (file == null) return null;
        return new File(TSP_DIR, file + TSP_EXT);
    }

    /** Trust boundary: reject anything but a plain file name (no traversal). */
    private static String safeName(String name) {
        if (name == null || name.isEmpty() || name.contains("..") || name.contains("/") || name.contains("\\")) {
            return null;
        }
        return name;
    }

    /**
     * Parses the query string of the request into a key-value map.
     *
     * @param exchange the HTTP exchange
     * @return the parsed query parameters
     */
    private static Map<String, String> query(HttpExchange exchange) {
        String raw = exchange.getRequestURI().getRawQuery();
        Map<String, String> params = new HashMap<>();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                int i = pair.indexOf('=');
                if (i > 0) {
                    params.put(pair.substring(0, i), URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
                }
            }
        }
        return params;
    }

    /**
     * Builds a JSON string array from the given strings, escaping quotes and
     * backslashes.
     *
     * @param items the strings to encode
     * @return a JSON string array
     */
    private static String jsonStringArray(List<String> items) {
        return "[" + items.stream()
                .map(s -> "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    /**
     * Writes a single Server-Sent Event to the output stream.
     *
     * @param out the output stream
     * @param event the event name
     * @param data the event data, possibly multi-line
     * @throws IOException when writing fails
     */
    private static void sse(OutputStream out, String event, String data) throws IOException {
        StringBuilder sb = new StringBuilder("event: ").append(event).append('\n');
        for (String line : data.split("\n", -1))
            sb.append("data: ").append(line).append('\n');
        sb.append('\n');
        // Locked so the watchdog's ping cannot interleave with a solver log line.
        synchronized (out) {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    /**
     * Sends a file as the response body, or a 404 if the file does not exist.
     *
     * @param ex the HTTP exchange
     * @param f the file to serve
     * @param type the MIME type to set
     * @throws IOException when reading the file or writing the response fails
     */
    private static void serveFile(HttpExchange ex, File f, String type) throws IOException {
        if (f.exists()) {
            send(ex, 200, type, Files.readAllBytes(f.toPath()));
            return;
        }
        send(ex, 404, "text/plain", "Not found".getBytes());
    }

    /**
     * Sends an HTTP response with the given status, content type, and body.
     *
     * @param ex the HTTP exchange
     * @param code the HTTP status code
     * @param type the content type
     * @param body the response body
     * @throws IOException when writing the response fails
     */
    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    /** Buffers bytes written to System.out into lines and emits each as an SSE {@code log} event. */
    private static final class SseLineStream extends OutputStream {
        private final OutputStream sink;
        private final StringBuilder buf = new StringBuilder();

        /**
         * Creates a new line buffer writing to the provided sink.
         *
         * @param sink the output stream receiving SSE events
         */
        SseLineStream(OutputStream sink) {
            this.sink = sink;
        }

        /**
         * Buffers a single byte; flushes a completed line on a newline.
         *
         * @param b the byte to write
         * @throws IOException when flushing a line fails
         */
        @Override public synchronized void write(int b) throws IOException {
            if (b == '\n')
                flushLine();
            else if (b != '\r')
                buf.append((char) b);
        }
        /**
         * Emits the buffered line as an SSE {@code log} event and clears the buffer.
         *
         * @throws IOException when writing the event fails
         */
        private void flushLine() throws IOException {
            try {
                sse(sink, "log", buf.toString());
            } catch (IOException e) {
                // The browser hung up: ask the solver to stop instead of running on unwatched.
                GeneticAlgorithm algorithm = currentAlgo;
                if (algorithm != null)
                    algorithm.requestStop();
                throw e;
            }
            buf.setLength(0);
        }
    }
}
