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
public class Pair implements Cloneable {
    
    private int X, Y;

    public Pair(int x, int y){
        this.X = x;
        this.Y = y;
    }

    @Override
    public int hashCode() {
        return 31 * this.X + this.Y;
    }

    public Pair(int x){
        this(x, x);
    }

    Pair(Pair pair){
        this(pair.X, pair.Y);
    }
    
    public Pair Inverse() {
        return new Pair(this.Y, this.X);
    }

    @Override
    public Pair clone(){
        return new Pair(this);
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

    boolean isEqualsTo(Pair p){
        return this.X == p.X && this.Y == p.Y;
    }

    @Override
    public boolean equals(Object other) {
        if(this == other)
            return true;
        if(other == null || this.getClass() != other.getClass())
            return false;
        return this.isEqualsTo((Pair)other);
    }
}
