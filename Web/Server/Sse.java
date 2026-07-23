// Author: Othmane

package Web.Server;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * One Server-Sent Events connection. Owns the response stream so callers never
 * touch raw bytes, and serialises writes so the solver log and the keep-alive
 * ping cannot interleave.
 *
 * @author Othmane EL YAAKOUBI
 */
final class Sse {

    private final OutputStream out;

    /**
     * Wraps an already-opened event stream.
     *
     * @param out the response body of the SSE exchange
     */
    private Sse(OutputStream out) {
        this.out = out;
    }

    /**
     * Sets the event-stream headers, commits the response, and returns the
     * connection to write events to.
     *
     * @param ex the HTTP exchange
     * @return the open SSE connection
     * @throws IOException when the response headers cannot be sent
     */
    static Sse open(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "text/event-stream");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, 0);
        return new Sse(ex.getResponseBody());
    }

    /**
     * Writes a single event, splitting multi-line data into {@code data:} lines.
     *
     * @param name the event name
     * @param data the event data, possibly multi-line
     * @throws IOException when the client has hung up
     */
    synchronized void event(String name, String data) throws IOException {
        StringBuilder sb = new StringBuilder("event: ").append(name).append('\n');
        for (String line : data.split("\n", -1))
            sb.append("data: ").append(line).append('\n');
        sb.append('\n');
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * Closes the connection.
     *
     * @throws IOException when closing the stream fails
     */
    void close() throws IOException {
        out.close();
    }

    /**
     * A {@link PrintStream} that turns every line printed to it into a
     * {@code log} event, so {@code System.out} can be piped to the browser.
     *
     * @param onHangUp run once the client is gone (a print cannot report failure)
     * @return the auto-flushing print stream
     */
    PrintStream logStream(Runnable onHangUp) {
        return new PrintStream(new LineStream(onHangUp), true, StandardCharsets.UTF_8);
    }

    /** Buffers written bytes into lines and emits each as a {@code log} event. */
    private final class LineStream extends OutputStream {

        private final Runnable onHangUp;
        private final StringBuilder buf = new StringBuilder();

        /**
         * Creates the line buffer.
         *
         * @param onHangUp callback invoked when writing to the client fails
         */
        LineStream(Runnable onHangUp) {
            this.onHangUp = onHangUp;
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
         * Emits the buffered line and clears the buffer.
         *
         * @throws IOException when writing the event fails
         */
        private void flushLine() throws IOException {
            try {
                event("log", buf.toString());
            } catch (IOException hungUp) {
                // The browser is gone: let the owner stop instead of running on unwatched.
                onHangUp.run();
                throw hungUp;
            } finally {
                buf.setLength(0);
            }
        }
    }
}
