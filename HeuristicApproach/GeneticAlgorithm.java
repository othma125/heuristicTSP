/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HeuristicApproach;

import Data.InputData;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author Othmane
 */
public class GeneticAlgorithm extends MetaHeuristic {
    
    private final double MutationRate = 0.1d;
    private final double CrossoverRate = 0.9d;
    private final GiantTour[] Population;
    private final int PopulationSize;
    private final int TournamentSize = 5;
    private long EndTime;
    private final Set<Future<GiantTour>> Futures;
    
    public GeneticAlgorithm(InputData data) {
        super(data);
        this.PopulationSize = (int) Math.max(20, 10 * Math.log10(data.StopsCount));
        this.Population = new GiantTour[this.PopulationSize];
        this.Futures = new HashSet<>(4 * this.PopulationSize, 1f);
    }
    
    @Override
    public void Run() {
        System.out.println("File to solve = " + this.Data.FileName);
        System.out.println("Stops Count = " + this.Data.StopsCount);
        System.out.println("Solution approach = Memetic Algorithm");
        System.out.println();
        this.StartTime = System.currentTimeMillis();
        try {
            this.InitialPopulation();
        } catch (Exception ex) {
            Logger.getLogger(GeneticAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
        }
        do {
            for (int i = 0; i < this.PopulationSize; i++)
                try {
                    this.Selection();
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(GeneticAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
                }
            this.FuturesJoin();
        } while(this.nonStopCondition());
        this.Executor.shutdown();
        this.EndTime = System.currentTimeMillis();
    }
    
    private void Selection() throws InterruptedException, ExecutionException {
        GiantTour parent1 = this.tournamentSelection();
        GiantTour parent2 = this.tournamentSelection();
        boolean CrossoverCondition = Math.random() < this.CrossoverRate;
        int CutPoint1 = (int) (this.Data.StopsCount * Math.random());
        int CutPoint2 = Math.random() < 0.7d ? CutPoint1 : CutPoint1 + (int)((this.Data.StopsCount - CutPoint1) * Math.random());
        if (CrossoverCondition && parent1 != parent2) {
            Callable<GiantTour> task1 = () -> {
                GiantTour Child = parent1.Crossover(this.Data , parent2, Math.random() < this.MutationRate, CutPoint1, CutPoint2);
                this.UpdatePopulation(Child);
                return Child;
            };
            this.Futures.add(this.Executor.submit(task1));
            Callable<GiantTour> task2 = () -> {
                GiantTour Child = parent2.Crossover(this.Data , parent1, Math.random() < this.MutationRate, CutPoint1, CutPoint2);
                this.UpdatePopulation(Child);
                return Child;
            };
            this.Futures.add(this.Executor.submit(task2));
        }
        else {
            Callable<GiantTour> task = () -> {
                GiantTour RandomSolution = Math.random() < 0.7d ? new GiantTour(this.Data) : new GiantTour(this.Data, this.getBestSolution());
                this.UpdatePopulation(RandomSolution);
                return RandomSolution;
            };
            this.Futures.add(this.Executor.submit(task));
        }
    }
    
    private void UpdatePopulation(GiantTour newSolution) {
        if (newSolution == null)
            return;
        this.Lock.lock();
        try {
            if (newSolution.compareTo(this.Population[this.PopulationSize - 1]) < 0) {
                int half = this.PopulationSize / 2;
                int randomIndex = half + (int) (Math.random() * (this.PopulationSize - half));
                this.Population[randomIndex] = newSolution;
                this.setBestSolution(newSolution);
                Arrays.sort(this.Population);
            }
        } finally {
            this.Lock.unlock();
        }
    }
    
    private void InitialPopulation() throws Exception {
        for (int i = 0; i < this.PopulationSize; i++) {
            Callable<GiantTour> task = () -> {
                GiantTour random_solution = new GiantTour(this.Data);
                this.setBestSolution(random_solution);
                return random_solution;
            };
            this.Futures.add(this.Executor.submit(task));
        }
        int i = -1;
        for (Future<GiantTour> future: this.Futures)
            this.Population[++i] = future.get();
        this.Futures.clear();
        Arrays.sort(this.Population);
    }
    
    private void FuturesJoin() {
        for (Future futur : this.Futures)
            try {
                futur.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(GeneticAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
            }
        this.Futures.clear();
    }
    
    private GiantTour tournamentSelection() {
        GiantTour bestInTournament = null;
        for (int i = 0; i < this.TournamentSize; i++) {
            int randomIndex = (int) (Math.random() * this.PopulationSize);
            GiantTour randomCompetitor = this.Population[randomIndex];
            if (bestInTournament == null || randomCompetitor.compareTo(bestInTournament) < 0)
                bestInTournament = randomCompetitor;
        }
        return bestInTournament;
    }
    
    private boolean nonStopCondition() {
        long current_time = System.currentTimeMillis();
        double probability = current_time - this.BestSolutionReachingTime;
        probability /= (double) (current_time - this.StartTime);
        return current_time - this.BestSolutionReachingTime < this.StagnationMinTime || Math.random() > probability;
    }

    @Override
    public long getRunningTime() {
        return this.EndTime - this.StartTime;
    }
}