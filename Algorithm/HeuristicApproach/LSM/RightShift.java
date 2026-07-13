package Algorithm.HeuristicApproach.LSM;

import Algorithm.Data.InputData;
import Algorithm.HeuristicApproach.Move;
import java.util.stream.IntStream;

/**
 * The right-shift local-search move: relocates a block of {@code Degree + 1} stops
 * starting at position {@code J} to just before position {@code I}, optionally
 * reversing the block ({@code With2Opt}).
 *
 * @author Othmane
 */
public class RightShift extends LocalSearchMove {

    private final int Degree;
    private final boolean With2Opt;

    /**
     * Creates a right-shift move.
     *
     * @param sequence the tour sequence
     * @param i the insertion reference position
     * @param j the position of the block's leading stop
     * @param degree the number of extra stops in the block (block size is {@code degree + 1})
     * @param _2opt whether to reverse the relocated block
     */
    public RightShift(int[] sequence, int i, int j, int degree, boolean _2opt) {
        super("RightShift", sequence, i, j);
        this.Degree = degree;
        this.With2Opt = _2opt;
    }

    /**
     * {@inheritDoc}
     *
     * @param sequence the tour sequence to modify
     */
    @Override
    public void Perform(int[] sequence) {
        IntStream.range(0, this.Degree + 1).mapToObj(i -> new Move(this.With2Opt ? this.I : this.I + i, this.J + i))
                                            .forEach(move -> move.RightShift(sequence));
    }

    /**
     * {@inheritDoc}
     *
     * @param data the instance providing distances
     */
    @Override
    public void setGain(InputData data) {
        if (this.I == this.J + this.Degree || (this.I == 0 && this.J + this.Degree + 1 == this.Sequence.length))
            return;
        int x = this.I == 0 ? this.Sequence.length - 1 : this.I - 1;
        int y = this.J + this.Degree + 1 == this.Sequence.length ? 0 : this.J + this.Degree + 1;
        this.Gain += data.getCost(this.With2Opt ? this.Sequence[this.J] : this.Sequence[this.J + this.Degree], this.Sequence[this.I]);
        this.Gain += data.getCost(this.Sequence[x], this.With2Opt ? this.Sequence[this.J + this.Degree] : this.Sequence[this.J]);
        this.Gain -= data.getCost(this.Sequence[x], this.Sequence[this.I]);
        this.Gain -= data.getCost(this.Sequence[this.J - 1], this.Sequence[this.J]);
        this.Gain -= data.getCost(this.Sequence[this.J + this.Degree], this.Sequence[y]);
        this.Gain += data.getCost(this.Sequence[this.J - 1], this.Sequence[y]);
    }
}
