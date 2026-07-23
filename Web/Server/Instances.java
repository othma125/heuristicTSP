// Author: Othmane

package Web.Server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The on-disk TSPLIB instance folder: what it contains, how a request maps to a
 * file, and the best-known cost of an instance. The only place that knows the
 * layout of {@code Algorithm/ALL_tsp}.
 *
 * @author Othmane EL YAAKOUBI
 */
final class Instances {

    private static final File DIR = new File("Algorithm/ALL_tsp");
    private static final File BEST_KNOWN = new File(DIR, "tsplib_best_known.csv");
    private static final String EXT = ".tsp";

    /** Static utility class; not instantiable. */
    private Instances() {
    }

    /**
     * Lists the available instances, extension stripped, case-insensitively sorted.
     *
     * @return the instance names
     */
    static List<String> names() {
        String[] files = DIR.list((d, n) -> n.endsWith(EXT));
        return files == null ? List.of()
                : Arrays.stream(files).map(n -> n.substring(0, n.length() - EXT.length()))
                        .sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
    }

    /**
     * Resolves the {@code file} query parameter into an instance file.
     *
     * @param query the parsed query parameters
     * @return the {@code .tsp} file, or {@code null} if the parameter is missing or unsafe
     */
    static File resolve(Map<String, String> query) {
        String file = Http.safeName(query.get("file"));
        return file == null ? null : new File(DIR, file + EXT);
    }

    /**
     * Returns the instance name of a file, without the {@code .tsp} extension.
     *
     * @param tsp the instance file
     * @return the bare instance name
     */
    static String nameOf(File tsp) {
        return tsp.getName().replaceFirst("\\.tsp$", "");
    }

    /**
     * Reads the best-known cost for the instance from {@code tsplib_best_known.csv}.
     *
     * @param tsp the instance file
     * @return the best-known cost, or {@code NaN} if unknown
     */
    static double optimalOf(File tsp) {
        if (!BEST_KNOWN.exists()) return Double.NaN;
        String name = nameOf(tsp);
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
}
