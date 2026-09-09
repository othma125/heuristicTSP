// Author: Othmane

package Web.Server;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code /api/solve} and {@code /api/stop} endpoints. Runs are keyed by the
 * {@code run} id the browser sends with both requests, so several tabs can solve
 * at once and each stop request reaches its own {@link SolveRun}.
 *
 * @author Othmane EL YAAKOUBI
 */
final class SolveHandler {

    /** Runs in progress by client-supplied id, so {@link #stop} hits the right one. */
    private static final Map<String, SolveRun> RUNNING = new ConcurrentHashMap<>();

    /** Static utility class; not instantiable. */
    private SolveHandler() {
    }

    /**
     * SSE: streams the live solver log, then a final {@code result} event with
     * cost, time, and tour.
     *
     * @param ex the HTTP exchange
     * @throws IOException when writing the event stream fails
     */
    static void solve(HttpExchange ex) throws IOException {
        File instance = Instances.resolve(Http.query(ex));
        Sse sse = Sse.open(ex);
        if (instance == null || !instance.exists()) {
            sse.event("log", "Instance not found");
            sse.close();
            return;
        }
        String id = Http.query(ex).getOrDefault("run", "");
        SolveRun run = new SolveRun(instance, sse);
        RUNNING.put(id, run);
        try {
            run.run();
        } finally {
            RUNNING.remove(id);
        }
    }

    /**
     * Asks the solve identified by the {@code run} query parameter to stop early.
     * Unknown ids are ignored: the run has already finished.
     *
     * @param ex the HTTP exchange
     * @throws IOException when writing the response fails
     */
    static void stop(HttpExchange ex) throws IOException {
        SolveRun run = RUNNING.get(Http.query(ex).getOrDefault("run", ""));
        if (run != null)
            run.requestStop();
        Http.send(ex, 200, "text/plain", "stopping".getBytes(StandardCharsets.UTF_8));
    }
}
