package Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */

public class InputData implements CostMatrix, Comparable<InputData>, AutoCloseable {

    public int StopsCount = 0;
    // Thread-safe distance cache: multiple threads may call getCost concurrently
    private ConcurrentMap<Pair, Double> CostSet = null;          // explicit or lazily cached distances
    // Dense matrix strategy for small instances
    private double[][] CostMatrix = null;                        // used when useMatrix == true
    private boolean useMatrix = false;                           // decided dynamically per instance
    private boolean explicitWeights = false;                     // true when instance supplies explicit weights
    private static final int DEFAULT_MATRIX_THRESHOLD = 150;     // minimal dimension always using matrix
    // System property override: -Dtsp.matrix.max=<n> forces matrix for n<=value (still subject to memory check)
    private static final String MATRIX_PROP = "tsp.matrix.max";
    private final Map<String, String> header = new HashMap<>();
    private final List<Location> Coordinates = new ArrayList<>();
    public final String FileName;
    private volatile boolean closed = false; // indicates resources released
    
    public InputData(File file) {
        this(file, Integer.MAX_VALUE);
    }
    
    public InputData(File file, int max_dimension) {
        this.FileName = file.getName();
//        System.out.println(this.FileName);
        boolean c = true;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            c = parseTSPLIB(br, max_dimension);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file " + file, e);
        }
        if (c)
            return;
        if (this.useMatrix && this.StopsCount > 0)
            this.allocateMatrix();
        else if (!this.useMatrix && this.CostSet == null)
            this.CostSet = new ConcurrentHashMap<>();
    }

    private boolean parseTSPLIB(BufferedReader br, int max_dimension) throws IOException {
        String line;
        String section = null;

        // -------- Read header until a section starts --------
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty())
                continue;
            String upper = line.toUpperCase(Locale.ROOT);
            if (upper.startsWith("NODE_COORD_SECTION")) {
                section = "NODE_COORD_SECTION";
                break;
            }
            else if (upper.startsWith("EDGE_WEIGHT_SECTION")) {
                section = "EDGE_WEIGHT_SECTION";
                break;
            }
            else if (upper.startsWith("EOF"))
                break;
            else {
                String key;
                String value;
                if (line.contains(":")) {
                    int idx = line.indexOf(':');
                    key = line.substring(0, idx).trim().toUpperCase(Locale.ROOT);
                    value = line.substring(idx + 1).trim();
                }
                else {
                    // Lines like "DIMENSION 29"
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length == 2) {
                        key = parts[0].trim().toUpperCase(Locale.ROOT);
                        value = parts[1].trim();
                    }
                    else
                        continue;
                }
                this.header.put(key, value);
            }
        }
        if (this.header.containsKey("DIMENSION")) {
            this.StopsCount = Integer.parseInt(header.get("DIMENSION").replaceAll("[^0-9]", ""));
            if (this.StopsCount > max_dimension)
                return true;
        }
        if (this.StopsCount > 0)
            this.useMatrix = decideMatrixStrategy();
        String edgeWeightType = this.header.getOrDefault("EDGE_WEIGHT_TYPE", "").toUpperCase(Locale.ROOT);
        String edgeWeightFormat = this.header.getOrDefault("EDGE_WEIGHT_FORMAT", "").toUpperCase(Locale.ROOT);

        // Decide parsing mode
        if ("EXPLICIT".equals(edgeWeightType) || "EDGE_WEIGHT_SECTION".equals(section)) {
            this.explicitWeights = true;
            this.readExplicitWeights(br, edgeWeightFormat);
            // Fill coordinates with dummies for interface consistency if not already
//            if (this.Coordinates.isEmpty())
//                for (int i = 0; i < this.StopsCount; i++)
//                    this.Coordinates.add(new Location(0, 0));
        }
        else {
            // Coordinate based (EUC_2D / CEIL_2D / GEO etc.)
            if (!"NODE_COORD_SECTION".equals(section) && section == null)
                // We stopped on EOF without seeing NODE_COORD_SECTION (some files put it later)
                // Scan forward until NODE_COORD_SECTION
                while ((line = br.readLine()) != null) {
                    if (line.trim().equalsIgnoreCase("NODE_COORD_SECTION")) {
                        break;
                    }
                }
            this.readNodeCoords(br);
            // For coordinate-based, we fill lazily if matrix and set up sentinel values
//            if (this.useMatrix && this.StopsCount > 0)
//                this.allocateMatrix();
//            else if (!this.useMatrix && this.CostSet == null)
//                this.CostSet = new ConcurrentHashMap<>();
        }
        return false;
    }

    private void allocateMatrix() {
        if (this.CostMatrix != null)
            return;
        this.CostMatrix = new double[this.StopsCount][this.StopsCount];
//        for (int i = 0; i < this.StopsCount; i++) {
//            for (int j = 0; j < this.StopsCount; j++) {
//                if (i == j)
//                    costMatrix[i][j] = 0d;
//                else
//                    costMatrix[i][j] = -1d; // -1 sentinel means not computed
//            }
//        }
    }

    // Decide whether to use a dense matrix.
    // Heuristic: always for n <= DEFAULT_MATRIX_THRESHOLD. For larger n, estimate memory and compare
    // to available heap headroom. Allow user override via system property tsp.matrix.max.
    private boolean decideMatrixStrategy() {
        if (this.StopsCount <= DEFAULT_MATRIX_THRESHOLD) return true;
        int override = -1;
        try {
            override = Integer.parseInt(System.getProperty(MATRIX_PROP, "-1"));
        } catch (Exception ignore) {}
        if (override > 0 && this.StopsCount <= override) {
            // Still ensure it fits comfortably
            return this.hasHeapForMatrix(0.5d); // require 50% free headroom
        }
        // For larger sizes use matrix if it occupies less than ~35% of remaining available heap
        return this.hasHeapForMatrix(0.35d);
    }

    private boolean hasHeapForMatrix(double allowanceRatio) {
        long needed = this.estimateFullMatrixBytes();
        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long used = rt.totalMemory() - rt.freeMemory();
        long remaining = max - used;
        return needed < (long) (remaining * allowanceRatio);
    }

    // Rough estimate: 8*n*n bytes for doubles + per-row header (~16 bytes each) + top array + padding.
    private long estimateFullMatrixBytes() {
        return 8L * this.StopsCount * this.StopsCount + 16L * this.StopsCount + 64; // simplified upper-boundish estimate
    }

    private void readNodeCoords(BufferedReader br) throws IOException {
        this.Coordinates.clear();
        String line;
        while ((line = br.readLine()) != null) {
            String t = line.trim();
            if (t.isEmpty())
                continue;
            String u = t.toUpperCase(Locale.ROOT);
            if (u.startsWith("EOF") ||
                u.startsWith("EDGE_WEIGHT_SECTION") ||
                u.startsWith("DISPLAY_DATA_SECTION") ||
                u.startsWith("TOUR_SECTION"))
                break;

            String[] parts = t.split("\\s+");
            if (parts.length < 2)
                continue;
            int start = 0;
            // Many TSPLIB coordinate lines are: index longitude y
            if (parts.length >= 3 && isNumeric(parts[0]))
                start = 1;
            if (parts.length - start < 2)
                continue;

            Double x = this.parseDoubleSafe(parts[start]);
            Double y = this.parseDoubleSafe(parts[start + 1]);
            if (x != null && y != null) {
                this.Coordinates.add(new Location(x, y));
                if (this.StopsCount > 0 && this.Coordinates.size() >= this.StopsCount)
                    // Location early if dimension tells us enough points
                    break;
            }
        }
        // If dimension > 0 but file omitted some, we rely on what we have.
    }

    private void readExplicitWeights(BufferedReader br, String format) throws IOException {
        if (this.StopsCount <= 0)
            throw new IllegalArgumentException("DIMENSION must be specified for EXPLICIT instances");
        if (format == null || format.isEmpty())
            format = "FULL_MATRIX";

        format = format.toUpperCase(Locale.ROOT);
    if (this.useMatrix) {
            allocateMatrix();
        } else {
            this.CostSet = new ConcurrentHashMap<>();
        }
        List<Double> nums = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            String t = line.trim();
            if (t.isEmpty())
                continue;
            String u = t.toUpperCase(Locale.ROOT);
            if (u.startsWith("EOF") ||
                u.startsWith("DISPLAY_DATA_SECTION") ||
                u.startsWith("NODE_COORD_SECTION") ||
                u.startsWith("TOUR_SECTION"))
                break;

            String[] toks = t.split("\\s+");
            for (String tok : toks) {
                Double v = parseDoubleSafe(tok);
                if (v != null) nums.add(v);
            }
        }

        int expectedSize = expectedExplicitCount(format);

        if (nums.size() != expectedSize) {
            // Try auto-detect if format misdeclared
            if (nums.size() == this.StopsCount * (this.StopsCount + 1) / 2) {
                format = format.contains("LOWER") ? "LOWER_DIAG_ROW" : "UPPER_DIAG_ROW";
                expectedSize = nums.size();
            }
            else if (nums.size() == (this.StopsCount * (this.StopsCount - 1)) / 2) {
                format = format.contains("LOWER") ? "LOWER_ROW" : "UPPER_ROW";
                expectedSize = nums.size();
            }
            else if (nums.size() == this.StopsCount * this.StopsCount) {
                format = "FULL_MATRIX";
                expectedSize = nums.size();
            }
        }

        if (nums.size() != expectedSize) {
            throw new IllegalArgumentException(
                "Mismatch between expected and actual number of weights. Expected: " +
                    expectedSize + " (" + format + "), Found: " + nums.size());
        }

        int idx = 0;
    if (this.useMatrix) {
            switch (format) {
                case "FULL_MATRIX":
                    for (int i = 0; i < this.StopsCount; i++)
                        for (int j = 0; j < this.StopsCount; j++)
                            this.CostMatrix[i][j] = nums.get(idx++);
                    break;
                case "UPPER_ROW":
                    for (int i = 0; i < this.StopsCount; i++)
                        for (int j = i + 1; j < this.StopsCount; j++) {
                            double w = nums.get(idx++);
                            this.CostMatrix[i][j] = w;
                            this.CostMatrix[j][i] = w;
                        }
                    break;
                case "LOWER_ROW":
                    for (int i = 0; i < this.StopsCount; i++)
                        for (int j = 0; j < i; j++) {
                            double w = nums.get(idx++);
                            this.CostMatrix[i][j] = w;
                            this.CostMatrix[j][i] = w;
                        }
                    break;
                case "UPPER_DIAG_ROW":
                    for (int i = 0; i < this.StopsCount; i++)
                        for (int j = i; j < this.StopsCount; j++) {
                            double w = nums.get(idx++);
                            this.CostMatrix[i][j] = w;
                            this.CostMatrix[j][i] = w;
                        }
                    break;
                case "LOWER_DIAG_ROW":
                    for (int i = 0; i < this.StopsCount; i++)
                        for (int j = 0; j <= i; j++) {
                            double w = nums.get(idx++);
                            this.CostMatrix[i][j] = w;
                            this.CostMatrix[j][i] = w;
                        }
                    break;
                default:
                    throw new UnsupportedOperationException("EDGE_WEIGHT_FORMAT not supported: " + format);
            }
        }
    else {
            switch (format) {
                case "FULL_MATRIX":
                    for (int i = 0; i < this.StopsCount; i++)
                        for (int j = 0; j < this.StopsCount; j++)
                            this.CostSet.put(new Pair(i, j), nums.get(idx++));
                    break;
                case "UPPER_ROW": // strictly upper triangle (no diag)
                    for (int i = 0; i < this.StopsCount; i++)
                        for (int j = i + 1; j < this.StopsCount; j++) {
                            double w = nums.get(idx++);
                            this.CostSet.put(new Pair(i, j), w);
                            this.CostSet.put(new Pair(j, i), w);
                        }
//                    this.fillMissingDiagonalZero();
                    break;
                case "LOWER_ROW":
                    for (int i = 0; i < this.StopsCount; i++)
                        for (int j = 0; j < i; j++) {
                            double w = nums.get(idx++);
                            this.CostSet.put(new Pair(i, j), w);
                            this.CostSet.put(new Pair(j, i), w);
                        }
//                    this.fillMissingDiagonalZero();
                    break;
                case "UPPER_DIAG_ROW": // upper incl diagonal
                    for (int i = 0; i < this.StopsCount; i++)
                        for (int j = i; j < this.StopsCount; j++) {
                            double w = nums.get(idx++);
                            CostSet.put(new Pair(i, j), w);
                            CostSet.put(new Pair(j, i), w);
                        }
                    break;
                case "LOWER_DIAG_ROW":
                    for (int i = 0; i < this.StopsCount; i++)
                        for (int j = 0; j <= i; j++) {
                            double w = nums.get(idx++);
                            this.CostSet.put(new Pair(i, j), w);
                            this.CostSet.put(new Pair(j, i), w);
                        }
                    break;
                default:
                    throw new UnsupportedOperationException("EDGE_WEIGHT_FORMAT not supported: " + format);
            }
            // Ensure diagonal zeros exist for map-based explicit storage
            for (int i = 0; i < this.StopsCount; i++) {
                this.CostSet.putIfAbsent(new Pair(i, i), 0d);
            }
        }
    }

    private int expectedExplicitCount(String format) {
        return switch (format) {
            case "FULL_MATRIX" -> this.StopsCount * this.StopsCount;
            case "UPPER_ROW", "LOWER_ROW" -> this.StopsCount * (this.StopsCount - 1) / 2;
            case "UPPER_DIAG_ROW", "LOWER_DIAG_ROW" -> this.StopsCount * (this.StopsCount + 1) / 2;
            default -> throw new UnsupportedOperationException("EDGE_WEIGHT_FORMAT not supported: " + format);
        };
    }

//    private void fillMissingDiagonalZero() {
//        for (int i = 0; i < this.StopsCount; i++)
//            this.CostSet.putIfAbsent(new Pair(i), 0d);
//    }

    private static Double parseDoubleSafe(String s) {
        try {
            return Double.valueOf(s);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private static boolean isNumeric(String s) {
        try {
            Double.valueOf(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public double getCost(Pair p) {
        this.ensureOpen();
        if (p.isFake())
            return 0d;
        int x = p.getX();
        int y = p.getY();
        if (x < 0 || y < 0 || x >= this.StopsCount || y >= this.StopsCount)
            throw new IndexOutOfBoundsException("Invalid node index");
        if (x == y) return 0d; // diagonal always zero
        if (this.useMatrix) {
            double existing = this.CostMatrix[x][y];
            if (this.explicitWeights) {
                return existing; // explicit matrix fully populated
            } else if (existing > 0d) {
                return existing; // already computed for coordinate-based
            } else {
                return this.computeAndStore(new Pair(x, y));
            }
        } else {
            Double cached = this.CostSet.getOrDefault(p, this.CostSet.get(p.Inverse()));
            if (cached != null) return cached;
            if (this.explicitWeights) {
                // Missing explicit edge should not happen (unless diagonal which we handled)
                throw new IllegalStateException("Missing explicit distance for pair ("+x+","+y+")");
            }
            return this.computeAndStore(new Pair(x, y));
        }
    }

    private double computeAndStore(Pair p) {
        int x = p.getX();
        int y = p.getY();
        // If coordinates present, compute lazily

        Location location1 = this.Coordinates.get(x);
        Location location2 = this.Coordinates.get(y);

        String edgeWeightType = this.header.getOrDefault("EDGE_WEIGHT_TYPE", "").toUpperCase(Locale.ROOT);
        double cost;
        if ("CEIL_2D".equals(edgeWeightType))
            cost = Math.ceil(location1.getEuclidean(location2));
        else if (edgeWeightType.startsWith("EUC"))
            // TSPLIB EUC_2D/EUC_3D: round to nearest integer with .5 always rounded up
            cost = Math.round(location1.getEuclidean(location2));
        else if ("GEO".equals(edgeWeightType)) 
            cost = location1.toGEO().get_GEO_great_circle_distance(location2.toGEO());
        else if ("ATT".equals(edgeWeightType)) {
            // ATT (pseudo-Euclidean) distance per TSPLIB specification:
            // r_ij = sqrt( ( (x_i - x_j)^2 + (y_i - y_j)^2 ) / 10.0 )
            // t_ij = round(r_ij)
            // if t_ij < r_ij then d_ij = t_ij + 1 else d_ij = t_ij
            cost = location1.getPseudoEuclidean(location2);
        }
        else
            cost = location1.getEuclidean(location2); // fallback: raw Euclidean

        if (this.useMatrix) {
            this.CostMatrix[x][y] = cost;
            this.CostMatrix[y][x] = cost;
        }
        else
            this.CostSet.put(p, cost);
        return cost;
    }

    /**
     * Release internal references to help GC; idempotent.
     */
    @Override
    public void close() {
        if (this.closed)
            return;
        this.closed = true;
        if (this.CostSet != null)
            this.CostSet.clear();
        if (this.CostMatrix != null)
            this.CostSet = null;
        this.Coordinates.clear();
        this.header.clear();
    }

    private void ensureOpen() {
        if (this.closed)
            throw new IllegalStateException("InputData instance already closed: " + FileName);
    }

    @Override
    public int compareTo(InputData data) {
        return this.StopsCount - data.StopsCount;
    }

    // Simple coordinate holder
    static class Location {
        
        final double latitude;
        final double longitude;
        
        Location(double x, double y) {
            this.latitude = x;
            this.longitude = y;
        }

        private static double toGeoRadians(double ddmm) {
            double deg = (int) ddmm;
            double min = ddmm - deg;
            return Math.PI * (deg + 5d * min / 3d) / 180d;
        }
        
        Location toGEO() {
            double x = Location.toGeoRadians(this.latitude);
            double y = Location.toGeoRadians(this.longitude);
            return new Location(x, y);
        }
        
        double get_GEO_great_circle_distance(Location location) {
            // TSPLIB GEO great-circle distance on a sphere (radius 6378.388 km)
            double q1 = Math.cos(this.longitude - location.longitude);
            double q2 = Math.cos(this.latitude - location.latitude);
            double q3 = Math.cos(this.latitude + location.latitude);
            double dij = 6378.388d * Math.acos(0.5d * ((1d + q1) * q2 - (1d - q1) * q3)) + 1d;
            return (int) dij; // integer truncation per TSPLIB (floor)
        }
        
        double getPseudoEuclidean(Location location) {
            double dx = Math.abs(this.latitude - location.latitude);
            double dy = Math.abs(this.longitude - location.longitude);
            double rij = Math.sqrt((dx * dx + dy * dy) / 10d);
            double tij = Math.round(rij);
            return tij + (tij < rij ? 1 : 0);
        }
        
        double getEuclidean(Location location) {
            double dx = this.latitude - location.latitude;
            double dy = this.longitude - location.longitude;
            return Math.sqrt(dx * dx + dy * dy);
        }
        
        @Override
        public String toString() {
            return "Location {X = " + this.latitude + ", Y = " + this.longitude + '}';
        }
    }
}