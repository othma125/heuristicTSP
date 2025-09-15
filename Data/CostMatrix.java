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
public interface CostMatrix {
    
    public abstract double getCost(Edge edge);
    
    public default double getCost(int i, int j) {
        return this.getCost(new Edge(i, j));
    }
}
