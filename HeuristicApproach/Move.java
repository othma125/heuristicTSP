package HeuristicApproach;

import java.util.List;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */
public class Move {
    private int Index1, Index2;
    
//    void display(){
//        System.out.println("( " + this.Index1 + " , " + this.Index2 + " )");
//    }
    
    public Move(int a, int b) {
        this.Index1 = a;
        this.Index2 = b;
    }
    
//    boolean EqualsTo(Move m) {
//        return (this.Index1 == m.Index2 && this.Index2 == m.Index1) || (this.Index1 == m.Index1 && this.Index2 == m.Index2);
//    }

    public void Insertion(int[] sequence) {
        if (this.Index1 < this.Index2) {
            int aux = sequence[this.Index2];
            for (int k = this.Index2; k > this.Index1; k--)
                sequence[k] = sequence[k - 1];
            sequence[this.Index1] = aux;
        }
    }
    
    public void InverseInsertion(int[] array){
        if(this.Index1 < this.Index2){
            int aux = array[this.Index1];
            for(int k = this.Index1; k < this.Index2; k++)
                array[k] = array[k+1];
            array[this.Index2] = aux;
        }
    }

//    <T> void Insertion(List<Integer> sequence) {
//        if (this.Index1 < this.Index2) {
//            int aux = sequence.get(this.Index2);
//            for (int k = this.Index2; k > this.Index1; k--)
//                sequence.set(k, sequence.get(k - 1));
//            sequence.set(this.Index1, aux);
//        }
//    }
    
    public void Swap(int[] array){
       int aux = array[this.Index1];
       array[this.Index1] = array[this.Index2];
       array[this.Index2] = aux;
    }    
    
    <T> void Swap(List<T> Array){
        T aux = Array.get(this.Index1);
        Array.set(this.Index1, Array.get(this.Index2));
        Array.set(this.Index2, aux);
    }
    
    <T> void _2opt(List<T> list){
        if (this.Index1 < this.Index2) {
            for (int k = this.Index1, l = this.Index2; k < l; k++, l--)
                new Move(k, l).Swap(list);
        }
    }

    public void _2opt(int[] array) {
        if (this.Index1 < this.Index2) {
            for (int k = this.Index1, l = this.Index2; k < l; k++, l--)
                new Move(k, l).Swap(array);
        }
    }
}