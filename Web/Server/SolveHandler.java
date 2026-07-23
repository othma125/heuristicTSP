// Author: Othmane

package Web.Server;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The {@code /api/solve} and {@code /api/stop} endpoints. Serialises runs — the
 * solver logs to the process-wide {@code System.out}, so exactly one
 * {@link SolveRun} may own it — and keeps a handle on the active run so a stop
 * request can reach it.
 *
 * @author Othmane EL YAAKOUBI
 */
final class SolveHandler {

    private static final Object LOCK = new Object();
    // ponytail: one solve at a time. Per-session isolation only if concurrency matters.
    private static volatile SolveRun current;

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
        synchronized (LOCK) {
            SolveRun run = new SolveRun(instance, sse);
            current = run;
            try {
                run.run();
            } finally {
                current = null;
            }
        }
    }

    /**
     * Asks the running solve to stop early.
     *
     * @param ex the HTTP exchange
     * @throws IOException when writing the response fails
     */
    static void stop(HttpExchange ex) throws IOException {
        SolveRun run = current;
        if (run != null)
            run.requestStop();
        Http.send(ex, 200, "text/plain", "stopping".getBytes(StandardCharsets.UTF_8));
    }
}
