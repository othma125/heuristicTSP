package HeuristicApproach.LSM;

import Data.InputData;
import HeuristicApproach.Move;
import java.util.stream.IntStream;

/**
 *
 * @author Othmane
 */
public class Insertion extends LocalSearchMove {
    
    private final int Degree;
    private final boolean With2Opt;

    public Insertion(int[] sequence, int i, int j, int degree, boolean _2opt) {
        super(sequence, i, j);
        this.Degree = degree;
        this.With2Opt = _2opt;
    }

    @Override
    public void Perform(int[] sequence) {
        IntStream.range(0, this.Degree + 1).mapToObj(i -> new Move(this.With2Opt ? this.I : this.I + i, this.J + i))
                                            .forEach(motion -> motion.Insertion(sequence));
    }

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
