package Algorithm.HeuristicApproach.LSM;

import Algorithm.Data.InputData;

/**
 * Base class for a single local-search move on a tour sequence.
 *
 * <p>A move is defined by two positions {@code (I, J)} in a sequence. It can
 * evaluate its cost change ({@link #setGain}/{@link #getGain}) without touching
 * the sequence, and later {@link #Perform} the change in place if it improves
 * the tour. Concrete subclasses are 2-opt, swap, and left/right shift.
 *
 * @author Othmane
 */
public abstract class LocalSearchMove {

    final int[] Sequence;
    double Gain = 0d;
    final int I, J;
    final String Name;

    /**
     * Initializes a move on the given sequence positions.
     *
     * @param name the move's name (for diagnostics)
     * @param sequence the tour sequence the move applies to
     * @param i the first position
     * @param j the second position
     */
    public LocalSearchMove(String name, int[] sequence, int i, int j) {
        this.Name = name;
        this.Sequence = sequence;
        this.I = i;
        this.J = j;
    }

    /**
     * Returns the move's name.
     *
     * @return the name
     */
    public String getName() {
        return Name;
    }

    /**
     * Returns the last computed gain (negative means an improvement).
     *
     * @return the cached gain
     */
    public double getGain() {
        return this.Gain;
    }

    /**
     * Computes and returns the move's gain for the given instance.
     *
     * @param data the instance providing distances
     * @return the gain (negative means an improvement)
     */
    public double getGain(InputData data) {
        this.setGain(data);
        return this.Gain;
    }

    /**
     * Applies the move to the given sequence in place.
     *
     * @param sequence the tour sequence to modify
     */
    public abstract void Perform(int[] sequence);

    /**
     * Computes the cost change this move would produce and stores it in
     * {@link #Gain}.
     *
     * @param data the instance providing distances
     */
    public abstract void setGain(InputData data);
}
