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
        this.Sequence = seq.stream().flatMapToInt(IntStream::of)
                                    .toArray();
        seq.clear();
        this.setCost(data);
        this.LocalSearch(data);
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
    
    private boolean stagnation_breaker(InputData data) {
        for (int i = 0; i < this.Sequence.length - 1; i++) {
            Set<LocalSearchMove> lsm_set = new HashSet<>();
            for (int j = i + 1; j < this.Sequence.length; j++) {    
                if(j > i + 1) {
                    LocalSearchMove lsm = new Swap(this.Sequence, i, j);
                    if(lsm.getGain(data) < 0d)
                        lsm_set.add(lsm);
                }
                for (int n = j == i + 1 ? 1 : 0; n >= 2 && j + n < this.Sequence.length; n++) {
                    LocalSearchMove lsm1 = new Insertion(this.Sequence, i, j, n, true);
                    if(lsm1.getGain(data) < 0d)
                        lsm_set.add(lsm1);
                    if(n == 0)
                        continue;
                    LocalSearchMove lsm2 = new Insertion(this.Sequence, i, j, n, false);
                    if(lsm2.getGain(data) < 0d)
                        lsm_set.add(lsm2);
                }
                for (int n = j == i + 1 ? 1 : 0; n >= 2 && i - n >= 0; n++) {
                    LocalSearchMove lsm1 = new InverseInsertion(this.Sequence, i, j, n, true);
                    if(lsm1.getGain(data) < 0d)
                        lsm_set.add(lsm1);
                    if(n == 0)
                        continue;
                    LocalSearchMove lsm2 = new InverseInsertion(this.Sequence, i, j, n, false);
                    if(lsm2.getGain(data) < 0d)
                        lsm_set.add(lsm2);
                }
            }
            LocalSearchMove best_lsm = lsm_set.stream()
                                                .min(Comparator.comparingDouble(LocalSearchMove::getGain))
                                                .orElse(null);            
            lsm_set.clear();
            if (best_lsm == null)
                continue;
            best_lsm.Perform(this.Sequence);
            this.Cost += best_lsm.getGain();
            return true;
        }
        return false;
    }

    private void LocalSearch(InputData data) {
        if (this.Sequence.length < 2)
            return;
        double cost = this.Cost;
        double probability = Math.sqrt(data.StopsCount) / (double) data.StopsCount;
        for (int i = 0; i < this.Sequence.length - 1; i++)
            for (int j = i + 1; j < this.Sequence.length ; j++) {
//            for (int l = this.Sequence.length - 1; l > k ; l--) {
                LocalSearchMove lsm = new _2opt(this.Sequence, i , j);
                double gain = lsm.getGain(data);
                if (gain < 0d) {
                    lsm.Perform(this.Sequence);
                    this.Cost += gain;
                }
            }
        if (this.compareTo(cost) < 0) {
            int max = (int) (Math.random() * Math.sqrt(data.StopsCount));
            Move move = new Move(0, this.Sequence.length - 1);
            for (int i = 0; i < max; i++)
                move.Insertion(this.Sequence);
            this.LocalSearch(data);
        }
        else if (Math.random() < probability && this.stagnation_breaker(data))
            this.LocalSearch(data);
    }
    
    @Override
    public int compareTo(Tour gt) {
        double diff = this.Cost - gt.getCost();
        return (int) (diff * 100d);
    }
    
    private int compareTo(double d) {
        double diff = this.Cost - d;
        return (int) (diff * 100d);
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