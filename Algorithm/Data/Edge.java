package Algorithm.Data;

/**
 * An ordered pair of stop indices identifying a directed edge, used as the key
 * for distance lookups. Two edges are equal only when both endpoints match in
 * order; {@link #Inverse()} yields the reversed edge for symmetric lookups.
 *
 * @author Othmane
 */
public class Edge implements Cloneable {

    private int X, Y;

    /**
     * Creates an edge from stop {@code x} to stop {@code y}.
     *
     * @param x the source stop index
     * @param y the target stop index
     */
    public Edge(int x, int y){
        this.X = x;
        this.Y = y;
    }

    /**
     * {@inheritDoc}
     *
     * @return a hash combining both endpoints
     */
    @Override
    public int hashCode() {
        return 31 * this.X + this.Y;
    }

    /**
     * Creates a self-loop edge {@code (x, x)}.
     *
     * @param x the stop index for both endpoints
     */
    public Edge(int x){
        this(x, x);
    }

    /**
     * Copy constructor.
     *
     * @param edge the edge to copy
     */
    Edge(Edge edge){
        this(edge.X, edge.Y);
    }

    /**
     * Returns the reversed edge {@code (Y, X)}.
     *
     * @return a new edge with the endpoints swapped
     */
    public Edge Inverse() {
        return new Edge(this.Y, this.X);
    }

    /**
     * {@inheritDoc}
     *
     * @return a copy of this edge
     */
    @Override
    public Edge clone(){
        return new Edge(this);
    }

    /**
     * {@inheritDoc}
     *
     * @return the endpoints separated by a space
     */
    @Override
    public String toString(){
        return this.X + " " + this.Y;
    }

    /**
     * Returns the source stop index.
     *
     * @return the {@code X} endpoint
     */
    public int getX() {
        return this.X;
    }

    /**
     * Returns the target stop index.
     *
     * @return the {@code Y} endpoint
     */
    public int getY() {
        return this.Y;
    }

    /**
     * Reports whether this edge is a self-loop (zero-cost by convention).
     *
     * @return {@code true} if both endpoints are equal
     */
    public boolean isFake() {
        return this.X == this.Y;
    }

    /**
     * Tests endpoint-wise equality with another edge.
     *
     * @param edge the edge to compare with
     * @return {@code true} if both endpoints match in order
     */
    boolean isEqualsTo(Edge edge){
        return this.X == edge.X && this.Y == edge.Y;
    }

    /**
     * {@inheritDoc}
     *
     * @param other the object to compare with
     * @return {@code true} if {@code other} is an edge with the same endpoints
     */
    @Override
    public boolean equals(Object other) {
        if(this == other)
            return true;
        if(other == null || this.getClass() != other.getClass())
            return false;
        return this.isEqualsTo((Edge) other);
    }
}
