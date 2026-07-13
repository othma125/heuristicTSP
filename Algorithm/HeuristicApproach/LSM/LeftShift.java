package Algorithm.HeuristicApproach.LSM;

import Algorithm.Data.InputData;
import Algorithm.HeuristicApproach.Move;
import java.util.stream.IntStream;

/**
 * The left-shift local-search move: relocates a block of {@code Degree + 1} stops
 * ending at position {@code I} to just after position {@code J}, optionally
 * reversing the block ({@code With2Opt}).
 *
 * @author Othmane
 */
public class LeftShift extends LocalSearchMove {

    private final int Degree;
    private final boolean With2Opt;

    /**
     * Creates a left-shift move.
     *
     * @param sequence the tour sequence
     * @param i the position of the block's trailing stop
     * @param j the insertion reference position
     * @param degree the number of extra stops in the block (block size is {@code degree + 1})
     * @param _2opt whether to reverse the relocated block
     */
    public LeftShift(int[] sequence, int i, int j, int degree, boolean _2opt) {
        super("LeftShift", sequence, i, j);
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
        IntStream.range(0, this.Degree + 1).mapToObj(i -> new Move(this.I - i, this.With2Opt ? this.J : this.J - i))
                                            .forEach(move -> move.LeftShift(sequence));
    }

    /**
     * {@inheritDoc}
     *
     * @param data the instance providing distances
     */
    @Override
    public void setGain(InputData data) {
        if (this.I - this.Degree == this.J || (this.I - this.Degree == 0 && this.J + 1 == this.Sequence.length))
            return;
        int x = this.I - this.Degree == 0 ? this.Sequence.length - 1 : this.I - this.Degree - 1;
        int y = this.J + 1 == this.Sequence.length ? 0 : this.J + 1;
        this.Gain += data.getCost(this.Sequence[this.J], this.With2Opt ? this.Sequence[this.I] : this.Sequence[this.I - this.Degree]);
        this.Gain += data.getCost(this.With2Opt ? this.Sequence[this.I - this.Degree] : this.Sequence[this.I], this.Sequence[y]);
        this.Gain -= data.getCost(this.Sequence[this.J], this.Sequence[y]);
        this.Gain -= data.getCost(this.Sequence[this.I], this.Sequence[this.I + 1]);
        this.Gain += data.getCost(this.Sequence[x], this.Sequence[this.I + 1]);
        this.Gain -= data.getCost(this.Sequence[x], this.Sequence[this.I - this.Degree]);
    }
}
