package Algorithm.HeuristicApproach;

import java.util.List;

/**
 * A pair of positions {@code (Index1, Index2)} and the elementary array/list
 * operations that act on them: swap, left/right shift of one element, and the
 * segment reversal used by 2-opt. These are the low-level primitives the local
 * search moves are built from.
 *
 * @author Othmane
 */
public class Move {
    private int Index1, Index2;

    /**
     * Creates a move over the two given positions.
     *
     * @param a the first position
     * @param b the second position
     */
    public Move(int a, int b) {
        this.Index1 = a;
        this.Index2 = b;
    }

    /**
     * Rotates the element at {@code Index2} back to {@code Index1}, shifting the
     * intervening elements one position to the right.
     *
     * @param sequence the array to modify in place
     */
    public void RightShift(int[] sequence) {
        if (this.Index1 < this.Index2) {
            int aux = sequence[this.Index2];
            for (int k = this.Index2; k > this.Index1;)
                sequence[k] = sequence[--k];
            sequence[this.Index1] = aux;
        }
    }
    
    /**
     * Rotates the element at {@code Index1} forward to {@code Index2}, shifting
     * the intervening elements one position to the left.
     *
     * @param array the array to modify in place
     */
    public void LeftShift(int[] array){
        if(this.Index1 < this.Index2){
            int aux = array[this.Index1];
            for(int k = this.Index1; k < this.Index2;)
                array[k] = array[++k];
            array[this.Index2] = aux;
        }
    }

    /**
     * Exchanges the two elements at {@code Index1} and {@code Index2}.
     *
     * @param array the array to modify in place
     */
    public void Swap(int[] array){
       int aux = array[this.Index1];
       array[this.Index1] = array[this.Index2];
       array[this.Index2] = aux;
    }

    /**
     * Exchanges the two elements at {@code Index1} and {@code Index2} in a list.
     *
     * @param <T> the element type
     * @param Array the list to modify in place
     */
    <T> void Swap(List<T> Array){
        T aux = Array.get(this.Index1);
        Array.set(this.Index1, Array.get(this.Index2));
        Array.set(this.Index2, aux);
    }

    /**
     * Reverses the list segment between {@code Index1} and {@code Index2} (the
     * 2-opt move) by swapping elements inward from both ends.
     *
     * @param <T> the element type
     * @param list the list to modify in place
     */
    <T> void _2opt(List<T> list){
        if (this.Index1 < this.Index2) {
            for (int k = this.Index1, l = this.Index2; k < l; k++, l--)
                new Move(k, l).Swap(list);
        }
    }

    /**
     * Reverses the array segment between {@code Index1} and {@code Index2} (the
     * 2-opt move) by swapping elements inward from both ends.
     *
     * @param array the array to modify in place
     */
    public void _2opt(int[] array) {
        if (this.Index1 < this.Index2) {
            for (int k = this.Index1, l = this.Index2; k < l; k++, l--)
                new Move(k, l).Swap(array);
        }
    }
}
