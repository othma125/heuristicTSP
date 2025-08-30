package HeuristicApproach.LSM;

import Data.InputData;
import HeuristicApproach.Motion;
import java.util.stream.IntStream;

/**
 *
 * @author pc
 */
public class InverseInsertion extends LocalSearchMotion {
    
    private final int Degree;
    private final boolean With2Opt;

    public InverseInsertion(int[] sequence, int i, int j, int degree, boolean _2opt) {
        super(sequence, i, j);
        this.Degree = degree;
        this.With2Opt = _2opt;
    }

    @Override
    public void Perform(int[] sequence) {
        IntStream.range(0, this.Degree + 1).mapToObj(i -> new Motion(this.I - i, this.With2Opt ? this.J : this.J - i))
                                            .forEach(motion -> motion.InverseInsertion(sequence));
    }

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
