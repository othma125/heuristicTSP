package Algorithm.Data;

/**
 * Abstraction over the source of pairwise travel costs between stops.
 *
 * <p>Implementations decide how distances are stored and computed (dense
 * matrix, lazy cache, explicit weights, coordinate-based, ...), while callers
 * only depend on {@link #getCost(Edge)} / {@link #getCost(int, int)}.
 *
 * @author Othmane
 */
public interface CostMatrix {

    /**
     * Returns the travel cost of the given edge.
     *
     * @param edge the edge (ordered pair of stop indices)
     * @return the distance between the edge's endpoints
     */
    public abstract double getCost(Edge edge);

    /**
     * Returns the travel cost between stops {@code i} and {@code j}.
     *
     * @param i the first stop index
     * @param j the second stop index
     * @return the distance between the two stops
     */
    public default double getCost(int i, int j) {
        return this.getCost(new Edge(i, j));
    }
}
