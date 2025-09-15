package Data;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */
public class Edge implements Cloneable {
    
    private int X, Y;

    public Edge(int x, int y){
        this.X = x;
        this.Y = y;
    }

    @Override
    public int hashCode() {
        return 31 * this.X + this.Y;
    }

    public Edge(int x){
        this(x, x);
    }

    Edge(Edge edge){
        this(edge.X, edge.Y);
    }

    public Edge Inverse() {
        return new Edge(this.Y, this.X);
    }

    @Override
    public Edge clone(){
        return new Edge(this);
    }

    @Override
    public String toString(){
        return this.X + " " + this.Y;
    }

    public int getX() {
        return this.X;
    }

    public int getY() {
        return this.Y;
    }

    public boolean isFake() {
        return this.X == this.Y;
    }

    boolean isEqualsTo(Edge edge){
        return this.X == edge.X && this.Y == edge.Y;
    }

    @Override
    public boolean equals(Object other) {
        if(this == other)
            return true;
        if(other == null || this.getClass() != other.getClass())
            return false;
        return this.isEqualsTo((Edge) other);
    }
}
