package Algorithm.HeuristicApproach;

import Algorithm.Data.InputData;
import Algorithm.HeuristicApproach.LSM.LeftShift;
import Algorithm.HeuristicApproach.LSM.LocalSearchMove;
import Algorithm.HeuristicApproach.LSM.RightShift;
import Algorithm.HeuristicApproach.LSM.Swap;
import Algorithm.HeuristicApproach.LSM._2opt;
import java.util.stream.IntStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

/**
 * A candidate TSP solution: a permutation of the stops together with its total
 * cost. Besides representation, {@code Tour} carries the memetic operators —
 * order-based {@link #Crossover crossover}, 2-opt {@link #mutation}, and the
 * {@link #LocalSearch local search} (2-opt, swap, left/right shift) that refines
 * every offspring. Tours are ordered by cost so populations can be kept sorted.
 *
 * @author Othmane
 */
public final class Tour implements Comparable<Tour> {

    private int[] Sequence;
    private double Cost;

    /**
     * Returns the total length of this tour.
     *
     * @return the tour cost
     */
    public double getCost() {
        return this.Cost;
    }

    /**
     * Builds a random tour (a shuffled permutation) and immediately improves it
     * with local search.
     *
     * @param data the instance providing distances
     */
    public Tour(InputData data) {
        this.Sequence = IntStream.range(0, data.StopsCount).toArray();
        for (int i = 0; i < this.Sequence.length; i++)
            new Move(i, (int) (Math.random() * this.Sequence.length)).Swap(this.Sequence);
        this.setCost(data);
        this.LocalSearch(data);
    }
    
    /** Creates an empty sentinel tour with infinite cost (worst possible). */
    public Tour() {
        this.Cost = Double.POSITIVE_INFINITY;
    }

    /**
     * Builds a tour by applying a randomized 3-segment reordering to an existing
     * tour, then refining it with local search (a diversification restart).
     *
     * @param data the instance providing distances
     * @param gt the tour to perturb
     */
    public Tour(InputData data, Tour gt) {
        int n = gt.getSequence().length; 
        if (n < 8)
            return;
        int i = 1 + (int)(Math.random() * (n/4));
        int j = i + 1 + (int)(Math.random() * (n/4));
        int k = j + 1 + (int)(Math.random() * (n/4));
        int l = n; // wrap at the end

        int[] newSeq = new int[n];
        int idx = 0;
        // segment order: [0..i], [j..k], [i..j], [k..end]
        for (int t = 0; t < i; t++) newSeq[idx++] = gt.getSequence()[t];
        for (int t = j; t < k; t++) newSeq[idx++] = gt.getSequence()[t];
        for (int t = i; t < j; t++) newSeq[idx++] = gt.getSequence()[t];
        for (int t = k; t < l; t++) newSeq[idx++] = gt.getSequence()[t];

        this.Sequence = newSeq;
        this.setCost(data);
        this.LocalSearch(data);
    }
        
    /**
     * Builds a tour from a list-valued sequence.
     *
     * @param data the instance providing distances
     * @param seq the visiting order as a list of stop indices
     */
    private Tour(InputData data, List<Integer> seq) {
        this(data, seq.stream().flatMapToInt(IntStream::of).toArray());
    }

    /**
     * Builds a tour from an explicit sequence and computes its cost.
     *
     * @param data the instance providing distances
     * @param seq the visiting order as an array of stop indices
     */
    public Tour(InputData data, int[] seq) {
        this.Sequence = seq;
        this.setCost(data);
    }

    /**
     * Builds a tour from a sequence and a precomputed cost (no recomputation).
     *
     * @param seq the visiting order as an array of stop indices
     * @param cost the already-known tour cost
     */
    public Tour(int[] seq, double cost) {
        this.Sequence = seq;
        this.Cost = cost;
    }

    /**
     * Recomputes and stores the total cost of the current sequence, including
     * the closing edge back to the start.
     *
     * @param data the instance providing distances
     */
    private void setCost(InputData data) {
        this.Cost = 0d;
        int i = 0;
        while (i < this.Sequence.length - 1)
            this.Cost += data.getCost(this.Sequence[i], this.Sequence[++i]);
        this.Cost += data.getCost(this.Sequence[i], this.Sequence[0]);
    }

    /**
     * Optionally mutates a sequence in place with a single random 2-opt move.
     *
     * @param sequence_list the sequence to mutate
     * @param mutation whether the mutation should be applied
     */
    public static void mutation(List<Integer> sequence_list, boolean mutation) {
        if (mutation) {
            int i = (int) (Math.random() * sequence_list.size()), j = (int) (Math.random() * sequence_list.size());
            new Move(Math.min(i, j), Math.max(i, j))._2opt(sequence_list);
        }
    }

    /**
     * Order-based crossover: copies the parent's segment between the cut points,
     * then fills the remaining stops from this tour in order (skipping
     * duplicates), optionally mutating the child before evaluating it.
     *
     * @param data the instance providing distances
     * @param parent the other parent contributing the inherited segment
     * @param mutation whether to apply a 2-opt mutation to the child
     * @param cut_points one or two cut positions delimiting the inherited segment
     * @return the resulting child tour
     */
    Tour Crossover(InputData data, Tour parent, boolean mutation, int ... cut_points) {
        int n = cut_points.length == 1 ? 0 : cut_points[0];
        int p = cut_points[cut_points.length == 1 ? 0 : 1];
        LinkedList<Integer> crossover_child = new LinkedList<>();
        Set<Integer> non_duplication_set = new HashSet<>(this.Sequence.length, 1f);
        for (int j = n; j < p; j++) {
            int stop = parent.Sequence[j];
            crossover_child.add(stop);
            non_duplication_set.add(stop);
        }
//        int i = 0;
//        for (int j = p; non_duplication_set.size() < this.Sequence.length; j++) {
//            int stop = this.Sequence[j % this.Sequence.length];
//            if (non_duplication_set.contains(stop))
//                continue;
//            if (crossover_child.size() < this.Sequence.length - n) {
//                crossover_child.add(stop);
//                non_duplication_set.add(stop);
//            }
//            else {
//                crossover_child.add(i++, stop);
//                non_duplication_set.add(stop);
//            }
//        }
        int k = 0;
        while (this.Sequence[k] != parent.Sequence[p])
            k++;
        for (int j = k; non_duplication_set.size() < this.Sequence.length; j++) {
            int stop = this.Sequence[j % this.Sequence.length];
            if (non_duplication_set.contains(stop))
                continue;
            crossover_child.add(stop);
            non_duplication_set.add(stop);
        }
        Tour.mutation(crossover_child, mutation);
        return new Tour(data, crossover_child);
    }
    
    /**
     * Escapes a local optimum by scanning swap and bounded left/right shift
     * moves and applying the single best improving move found.
     *
     * @param data the instance providing distances
     * @return {@code true} if an improving move was applied, {@code false} otherwise
     */
    public boolean StagnationBreaker(InputData data) {
	int max = (int) Math.sqrt(data.StopsCount);
        for (int i = 0; i < this.Sequence.length - 1; i++) {
            LocalSearchMove best_lsm = null;
            for (int j = i + 1; j < this.Sequence.length; j++) {   
                if (j > i + 1) {
                    LocalSearchMove lsm = new Swap(this.Sequence, i, j);
                    if (best_lsm == null || lsm.getGain() < best_lsm.getGain())
                        best_lsm = lsm;
                }
                for (int n = j == i + 1 ? 1 : 0; n <= max && j + n < this.Sequence.length; n++) {
                    LocalSearchMove lsm1 = new RightShift(this.Sequence, i, j, n, true);
                    if (best_lsm == null || lsm1.getGain() < best_lsm.getGain())
                        best_lsm = lsm1;
                    if (n == 0)
                        continue;
                    LocalSearchMove lsm2 = new RightShift(this.Sequence, i, j, n, false);
                    if (best_lsm == null || lsm2.getGain() < best_lsm.getGain())
                        best_lsm = lsm2;
                }
                for (int n = j == i + 1 ? 1 : 0; n <= max && i - n >= 0; n++) {
                    LocalSearchMove lsm1 = new LeftShift(this.Sequence, i, j, n, true);
                    if (best_lsm == null || lsm1.getGain() < best_lsm.getGain())
                        best_lsm = lsm1;
                    if (n == 0)
                        continue;
                    LocalSearchMove lsm2 = new LeftShift(this.Sequence, i, j, n, false);
                    if (best_lsm == null || lsm2.getGain() < best_lsm.getGain())
                        best_lsm = lsm2;
                }
            }          
            if (best_lsm != null && best_lsm.getGain(data) < 0d) {
                best_lsm.Perform(this.Sequence);
                this.Cost += best_lsm.getGain();
                //if (!"Swap".equals(best_lsm.getName()))
                return true;
            }
        }
        return false;
    }

    /**
     * Runs local search on this tour with a size-derived restart probability.
     *
     * @param data the instance providing distances
     */
    public void LocalSearch(InputData data) {
        if (this.Sequence.length <= 2)
            return;
		this.LocalSearch(data, Math.sqrt(data.StopsCount) / data.StopsCount);
	}

    /**
     * Applies 2-opt improvements up to a size-bounded budget and, depending on a
     * random draw against {@code probability}, either restarts after a random
     * shift or invokes the stagnation breaker, recursing while it keeps improving.
     *
     * @param data the instance providing distances
     * @param probability controls how often the search restarts versus stops
     */
    public void LocalSearch(InputData data, double probability) {
	int max = (int) Math.sqrt(data.StopsCount);
        int improvementCounter = 0;
        // The scan is quadratic in the tour length, so a stopped run checks here rather than
        // only at the recursion below: on a large instance one pass alone runs for minutes.
        for (int i = 0; improvementCounter < max && i < this.Sequence.length - 1 && !data.isStopRequested(); i++)
            for (int j = i + 1; improvementCounter < max && j < this.Sequence.length ; j++) {
                LocalSearchMove lsm = new _2opt(this.Sequence, i , j);
                double gain = lsm.getGain(data);
                if (gain < 0d) {
                    lsm.Perform(this.Sequence);
                    this.Cost += gain;
                    improvementCounter++;
                }
            }
        boolean again = Math.random() > probability;
        if (!data.isStopRequested()
                && ((again && improvementCounter > 0) || (!again && improvementCounter < max && this.StagnationBreaker(data)))) {
            int n = (int) (Math.random() * Math.sqrt(data.StopsCount));
            Move move = new Move(0, this.Sequence.length - 1);
            for (; n > 0; n--)
                move.LeftShift(this.Sequence);
            this.LocalSearch(data, probability);
        }
    }
    
    /**
     * Orders tours by ascending cost.
     *
     * @param tour the tour to compare with
     * @return a negative, zero, or positive value as this tour is cheaper,
     *         equal, or costlier
     */
    @Override
    public int compareTo(Tour tour) {
        return Double.compare(this.Cost * 100d, tour.getCost() * 100d);
    }

    /**
     * {@inheritDoc}
     *
     * @return the 1-based visiting order, or {@code "NULL"} for an empty tour
     */
    @Override
    public String toString() {
        if (this.Sequence == null)
            return "NULL";
        return Arrays.toString(IntStream.of(this.Sequence).map(x -> x + 1).toArray());
    }

    /**
     * Returns the underlying 0-based visiting order.
     *
     * @return the internal sequence array (not a copy)
     */
    public int[] getSequence() {
        return this.Sequence;
    }

    /**
     * Returns the stop visited at the given position.
     *
     * @param index the position in the tour
     * @return the 0-based stop index at that position
     */
    public int getStop(int index) {
        return this.Sequence[index];
    }

    /** Prints the tour's length and its 1-based visiting order to standard output. */
    public void displaySolution() {
        System.out.println("Traveled distance = " + this.Cost);
        System.out.print(Arrays.toString(IntStream.of(this.Sequence).map(stop -> stop + 1).toArray()));
        System.out.println();
        System.out.println();
    }

    /**
     * Returns the number of stops in this tour.
     *
     * @return the sequence length
     */
    public int getLength() {
        return this.Sequence.length;
    }
}

