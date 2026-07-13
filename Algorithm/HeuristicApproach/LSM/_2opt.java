package Algorithm.HeuristicApproach.LSM;

import Algorithm.Data.InputData;
import Algorithm.HeuristicApproach.Move;

/**
 * The 2-opt local-search move: reverses the tour segment between positions
 * {@code I} and {@code J}, replacing two edges with their reconnection.
 *
 * @author Othmane
 */
public class _2opt extends LocalSearchMove {

    /**
     * Creates a 2-opt move over the given segment endpoints.
     *
     * @param sequence the tour sequence
     * @param i the segment's first position
     * @param j the segment's last position
     */
    public _2opt(int[] sequence, int i, int j) {
        super("2opt", sequence, i, j);
    }

    /**
     * {@inheritDoc}
     *
     * @param sequence the tour sequence to modify
     */
    @Override
    public void Perform(int[] sequence) {
        if (this.I == this.J)
            return;
        new Move(this.I, this.J)._2opt(sequence);
    }

    /**
     * {@inheritDoc}
     *
     * @param data the instance providing distances
     */
    @Override
    public void setGain(InputData data) {
        if (this.I == this.J || (this.I == 0 && this.J + 1 == this.Sequence.length))
            return;
        int x = this.I == 0 ? this.Sequence.length - 1 : this.I - 1;
        int y = this.J + 1 == this.Sequence.length ? 0 : this.J + 1;
        this.Gain += data.getCost(this.Sequence[x], this.Sequence[this.J]) - data.getCost(this.Sequence[x], this.Sequence[this.I]);
        this.Gain += data.getCost(this.Sequence[this.I], this.Sequence[y]) - data.getCost(this.Sequence[this.J], this.Sequence[y]);
    }
}