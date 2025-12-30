package HeuristicApproach;

import Data.InputData;
import HeuristicApproach.LSM.*;
import java.util.*;
import java.util.stream.IntStream;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Othmane
 */
public final class Tour implements Comparable<Tour> {

    private int[] Sequence;
    private double Cost;
    
    public double getCost() {
        return this.Cost;
    }

    public Tour(InputData data) {
        this.Sequence = IntStream.range(0, data.StopsCount).toArray();
        for (int i = 0; i < this.Sequence.length; i++)
            new Move(i, (int) (Math.random() * this.Sequence.length)).Swap(this.Sequence);
        this.setCost(data);
        this.LocalSearch(data);
    }
    
    public Tour() {
        this.Cost = Double.POSITIVE_INFINITY;
    }
    
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
        
    private Tour(InputData data, List<Integer> seq) {
        this(data, seq.stream().flatMapToInt(IntStream::of).toArray());
    }
    
    public Tour(InputData data, int[] seq) {
        this.Sequence = seq;
        this.setCost(data);
    }
    
    public Tour(int[] seq, double cost) {
        this.Sequence = seq;
        this.Cost = cost;
    }

    private void setCost(InputData data) {
        this.Cost = 0d;
        int i = 0;
        while (i < this.Sequence.length - 1)
            this.Cost += data.getCost(this.Sequence[i], this.Sequence[++i]);
        this.Cost += data.getCost(this.Sequence[i], this.Sequence[0]);
    }

    public static void mutation(List<Integer> sequence_list, boolean mutation) {
        if (mutation) {
            int i = (int) (Math.random() * sequence_list.size()), j = (int) (Math.random() * sequence_list.size());
            new Move(Math.min(i, j), Math.max(i, j))._2opt(sequence_list);
        }
    }

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
        non_duplication_set.clear();
        Tour.mutation(crossover_child, mutation);
        return new Tour(data, crossover_child);
    }
    
    public boolean StagnationBreaker(InputData data) {
	int max = (int) Math.sqrt(data.StopsCount);
        for (int i = 0; i < this.Sequence.length - 1; i++) {
            LocalSearchMove best_lsm = null;
            for (int j = i + 1; j < this.Sequence.length; j++) {   
                if(j > i + 1) {
                    LocalSearchMove lsm = new Swap(this.Sequence, i, j);
                    if(lsm.getGain(data) < 0d && (best_lsm == null || lsm.getGain() < best_lsm.getGain()))
                        best_lsm = lsm;
                }
                for (int n = j == i + 1 ? 1 : 0; n <= max && j + n < this.Sequence.length; n++) {
                    LocalSearchMove lsm1 = new RightShift(this.Sequence, i, j, n, true);
                    if(lsm1.getGain(data) < 0d && (best_lsm == null || lsm1.getGain() < best_lsm.getGain()))
                        best_lsm = lsm1;
                    if(n == 0)
                        continue;
                    LocalSearchMove lsm2 = new RightShift(this.Sequence, i, j, n, false);
                    if(lsm2.getGain(data) < 0d && (best_lsm == null || lsm2.getGain() < best_lsm.getGain()))
                        best_lsm = lsm2;
                }
                for (int n = j == i + 1 ? 1 : 0; n <= max && i - n >= 0; n++) {
                    LocalSearchMove lsm1 = new LeftShift(this.Sequence, i, j, n, true);
                    if(lsm1.getGain(data) < 0d && (best_lsm == null || lsm1.getGain() < best_lsm.getGain()))
                        best_lsm = lsm1;
                    if(n == 0)
                        continue;
                    LocalSearchMove lsm2 = new LeftShift(this.Sequence, i, j, n, false);
                    if(lsm2.getGain(data) < 0d && (best_lsm == null || lsm2.getGain() < best_lsm.getGain()))
                        best_lsm = lsm2;
                }
            }          
            if (best_lsm != null) {
                best_lsm.Perform(this.Sequence);
                this.Cost += best_lsm.getGain();
                return true;
            }
        }
        return false;
    }

    public void LocalSearch(InputData data) {
        if (this.Sequence.length < 2)
            return;
        boolean improved = false;
        for (int i = 0; i < this.Sequence.length - 1; i++)
            for (int j = i + 1; j < this.Sequence.length ; j++) {
//            for (int l = this.Sequence.length - 1; l > k ; l--) {
                LocalSearchMove lsm = new _2opt(this.Sequence, i , j);
                double gain = lsm.getGain(data);
                if (gain < 0d) {
                    lsm.Perform(this.Sequence);
                    this.Cost += gain;
                    improved = true;
                }
            }
        double probability = Math.sqrt(data.StopsCount) / (double) data.StopsCount;
        boolean again = Math.random() < probability;
        if ((!again && improved)
		|| (again && !improved && this.StagnationBreaker(data))) {
            int max = (int) (Math.random() * Math.sqrt(data.StopsCount));
            Move move = new Move(0, this.Sequence.length - 1);
            for (int i = 0; i < max; i++)
                move.LeftShift(this.Sequence);
            this.LocalSearch(data);
        }
    }
    
    @Override
    public int compareTo(Tour tour) {
        return Double.compare(this.Cost * 100d, tour.getCost() * 100d);
    }
    
    @Override
    public String toString() {
        if (this.Sequence == null)
            return "NULL";
        return Arrays.toString(IntStream.of(this.Sequence).map(x -> x + 1).toArray());
    }

    public int[] getSequence() {
        return this.Sequence;
    }
    
    public int getStop(int index) {
        return this.Sequence[index];
    }

    public void displaySolution() {
        System.out.println("Traveled distance = " + this.Cost);
        System.out.print(Arrays.toString(IntStream.of(this.Sequence).map(stop -> stop + 1).toArray()));
        System.out.println();
        System.out.println();
    }

    public int getLength() {
        return this.Sequence.length;
    }
}