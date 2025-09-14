package HeuristicApproach.LSM;

import Data.InputData;
import HeuristicApproach.Motion;
/**
 *
 * @author Othmane
 */
public class _2opt extends LocalSearchMove {

    public _2opt(int[] sequence, int i, int j) {
        super(sequence, i, j);
    }    

    @Override
    public void Perform(int[] sequence) {
        if (this.I == this.J)
            return;
        new Motion(this.I, this.J)._2opt(sequence);
    }

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