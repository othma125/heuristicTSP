// Author: Othmane

package Web.Server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Executors;

/**
 * Entry point of the landing-page backend, built on the JDK's {@link HttpServer}
 * (no dependencies). Owns only the wiring: port resolution and the route table.
 * Each route delegates to the class that owns the concern —
 * {@link Instances} for the instance folder, {@link SolveHandler} for runs,
 * {@link Http} for responses.
 *
 * @author Othmane EL YAAKOUBI
 */
public final class Main {

    /** Static utility class; not instantiable. */
    private Main() {
    }

    /**
     * Starts the HTTP server on the port supplied as the first argument, or on the
     * port read from {@code .env} (default 8080), and registers all contexts.
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
                    + " (./kill-server.sh) or pick another port: java -cp out Web.Server.Main 9090");
            System.exit(1);
            return;
        }
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/", Main::serveIndex);
        server.createContext("/app.js", ex -> Http.serveFile(ex, new File("Web/app.js"), "text/javascript; charset=utf-8"));
        server.createContext("/styles.css", ex -> Http.serveFile(ex, new File("Web/styles.css"), "text/css; charset=utf-8"));
        server.createContext("/assets/profile.jpg", ex -> Http.serveFile(ex, new File("profile.jpg"), "image/jpeg"));
        server.createContext("/api/instances", Main::instances);
        server.createContext("/api/tsp", Main::tsp);
        server.createContext("/api/solve", SolveHandler::solve);
        server.createContext("/api/stop", SolveHandler::stop);

        server.start();
        System.out.println("Landing page ready -> http://localhost:" + port);
    }

    /**
     * Reads PORT from the .env file at the project root.
     *
     * @return the configured port, or 8080
     */
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

    /**
     * Serves the landing page ({@code Web/index.html}) for the root path only.
     *
     * @param ex the HTTP exchange
     * @throws IOException when writing the response fails
     */
    private static void serveIndex(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) {
            Http.notFound(ex);
            return;
        }
        Http.serveFile(ex, new File("Web/index.html"), "text/html; charset=utf-8");
    }

    /**
     * Returns the available TSPLIB instances as a JSON string array.
     *
     * @param ex the HTTP exchange
     * @throws IOException when writing the response fails
     */
    private static void instances(HttpExchange ex) throws IOException {
        byte[] json = Http.jsonStringArray(Instances.names()).getBytes(StandardCharsets.UTF_8);
        Http.send(ex, 200, "application/json", json);
    }

    /**
     * Streams the raw {@code .tsp} file of the requested instance.
     *
     * @param ex the HTTP exchange
     * @throws IOException when reading the file or writing the response fails
     */
    private static void tsp(HttpExchange ex) throws IOException {
        File f = Instances.resolve(Http.query(ex));
        if (f == null || !f.exists()) {
            Http.notFound(ex);
            return;
        }
        Http.serveFile(ex, f, "text/plain; charset=utf-8");
    }
}
