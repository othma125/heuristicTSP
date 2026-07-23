// Author: Othmane

package Web.Server;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Request parsing and response writing shared by every handler: query strings,
 * the file-name trust boundary, JSON escaping, and byte/file responses.
 *
 * @author Othmane EL YAAKOUBI
 */
final class Http {

    /** Static utility class; not instantiable. */
    private Http() {
    }

    /**
     * Parses the query string of the request into a key-value map.
     *
     * @param exchange the HTTP exchange
     * @return the parsed query parameters
     */
    static Map<String, String> query(HttpExchange exchange) {
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
     * Trust boundary: rejects anything but a plain file name (no traversal).
     *
     * @param name the candidate file name
     * @return the name, or {@code null} if it is missing or unsafe
     */
    static String safeName(String name) {
        if (name == null || name.isEmpty() || name.contains("..") || name.contains("/") || name.contains("\\")) {
            return null;
        }
        return name;
    }

    /**
     * Builds a JSON string array from the given strings, escaping quotes and
     * backslashes.
     *
     * @param items the strings to encode
     * @return a JSON string array
     */
    static String jsonStringArray(List<String> items) {
        return "[" + items.stream()
                .map(s -> "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    /**
     * Sends a file as the response body, or a 404 if the file does not exist.
     *
     * @param ex the HTTP exchange
     * @param f the file to serve
     * @param type the MIME type to set
     * @throws IOException when reading the file or writing the response fails
     */
    static void serveFile(HttpExchange ex, File f, String type) throws IOException {
        if (f.exists()) {
            send(ex, 200, type, Files.readAllBytes(f.toPath()));
            return;
        }
        notFound(ex);
    }

    /**
     * Sends a plain-text 404 response.
     *
     * @param ex the HTTP exchange
     * @throws IOException when writing the response fails
     */
    static void notFound(HttpExchange ex) throws IOException {
        send(ex, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
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
    static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
