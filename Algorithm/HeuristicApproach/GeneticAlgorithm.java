package Algorithm.HeuristicApproach;

import Algorithm.Data.InputData;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Memetic algorithm for the symmetric TSP: a steady-state genetic algorithm
 * whose offspring are refined by the local search embedded in {@link Tour}.
 *
 * <p>The population is kept sorted by cost; each generation performs tournament
 * selection (size 5), order-based crossover (rate 0.9) and 2-opt mutation
 * (rate 0.1), submitting offspring construction to the shared thread pool. The
 * run stops on a stochastic, stagnation-aware condition (see
 * {@link #nonStopCondition()}) or when {@link #requestStop()} is called.
 *
 * @author Othmane
 */
public class GeneticAlgorithm extends MetaHeuristic {
    
    private final double MutationRate = 0.1d;
    private final double CrossoverRate = 0.9d;
    private final Tour[] Population;
    private final int PopulationSize;
    private final int TournamentSize = 5;
    private long EndTime;
    private volatile boolean StopRequested = false;
    private final Set<Future<Tour>> Futures;
    
    /**
     * Builds the solver and sizes the population from the instance dimension
     * ({@code max(20, 10 * log10(n))}).
     *
     * @param data the instance to solve
     */
    public GeneticAlgorithm(InputData data) {
        super(data);
        this.PopulationSize = (int) Math.max(20, 10 * Math.log10(data.StopsCount));
        this.Population = new Tour[this.PopulationSize];
        this.Futures = new HashSet<>(2 * this.PopulationSize, 1f);
    }
    
    /**
     * Runs the memetic search: seeds the initial population, then repeatedly
     * breeds a generation of offspring until the stop condition triggers.
     */
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
        } while(!this.StopRequested && this.nonStopCondition());
        this.Executor.shutdown();
        this.EndTime = System.currentTimeMillis();
    }

    /** Asks the search loop to stop after the current generation (used by the web UI). */
    public void requestStop() {
        this.StopRequested = true;
    }
    
    /**
     * Selects two parents by tournament and submits offspring construction to
     * the thread pool: a pair of crossovers when crossover applies, otherwise a
     * fresh or best-derived random tour.
     *
     * @throws InterruptedException if the submitting thread is interrupted
     * @throws ExecutionException if an offspring task fails
     */
    private void Selection() throws InterruptedException, ExecutionException {
        Tour parent1 = this.tournamentSelection();
        Tour parent2 = this.tournamentSelection();
        boolean CrossoverCondition = Math.random() < this.CrossoverRate;
        int CutPoint1 = (int) (this.Data.StopsCount * Math.random());
        int CutPoint2 = Math.random() < 0.7d ? CutPoint1 : CutPoint1 + (int)((this.Data.StopsCount - CutPoint1) * Math.random());
        if (CrossoverCondition && parent1 != parent2) {
            Callable<Tour> task1 = () -> {
                Tour Child = parent1.Crossover(this.Data , parent2, Math.random() < this.MutationRate, CutPoint1, CutPoint2);
                this.UpdatePopulation(Child);
                return Child;
            };
            this.Futures.add(this.Executor.submit(task1));
            Callable<Tour> task2 = () -> {
                Tour Child = parent2.Crossover(this.Data , parent1, Math.random() < this.MutationRate, CutPoint1, CutPoint2);
                this.UpdatePopulation(Child);
                return Child;
            };
            this.Futures.add(this.Executor.submit(task2));
        }
        else {
            Callable<Tour> task = () -> {
                Tour RandomSolution = Math.random() < 0.3d ? new Tour(this.Data) : new Tour(this.Data, this.getBestSolution());
                this.UpdatePopulation(RandomSolution);
                return RandomSolution;
            };
            this.Futures.add(this.Executor.submit(task));
        }
    }
    
    /**
     * Inserts an offspring into the (sorted) population if it beats the worst
     * member, replacing a random individual in the weaker half and re-sorting.
     *
     * @param newSolution the offspring to consider, ignored if {@code null}
     */
    private void UpdatePopulation(Tour newSolution) {
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
    
    /**
     * Fills the population with locally improved random tours, built in
     * parallel, and sorts it by cost.
     *
     * @throws Exception if an initialization task fails
     */
    private void InitialPopulation() throws Exception {
        for (int i = 0; i < this.PopulationSize; i++) {
            Callable<Tour> task = () -> {
                Tour random_solution = new Tour(this.Data);
                this.setBestSolution(random_solution);
                return random_solution;
            };
            this.Futures.add(this.Executor.submit(task));
        }
        int i = -1;
        for (Future<Tour> future: this.Futures)
            this.Population[++i] = future.get();
        this.Futures.clear();
        Arrays.sort(this.Population);
    }
    
    /** Waits for all pending offspring tasks of the current generation, then clears them. */
    private void FuturesJoin() {
        for (Future futur : this.Futures)
            try {
                futur.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(GeneticAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
            }
        this.Futures.clear();
    }
    
    /**
     * Returns the fittest of {@code TournamentSize} randomly drawn individuals.
     *
     * @return the tournament winner
     */
    private Tour tournamentSelection() {
        Tour bestInTournament = null;
        for (int i = 0; i < this.TournamentSize; i++) {
            int randomIndex = (int) (Math.random() * this.PopulationSize);
            Tour randomCompetitor = this.Population[randomIndex];
            if (bestInTournament == null || randomCompetitor.compareTo(bestInTournament) < 0)
                bestInTournament = randomCompetitor;
        }
        return bestInTournament;
    }
    
    /**
     * Stochastic stopping test: always continues while below the minimum
     * stagnation time, then continues with a probability that decays as
     * stagnation grows relative to the total elapsed time.
     *
     * @return {@code true} to keep searching, {@code false} to stop
     */
    private boolean nonStopCondition() {
        long current_time = System.currentTimeMillis();
        if (current_time - this.BestSolutionReachingTime < this.StagnationMinTime)
            return true;
        double probability = current_time - this.BestSolutionReachingTime - this.StagnationMinTime;
        probability /= (double) (current_time - this.StartTime);
        return Math.random() > probability;
    }

    /**
     * {@inheritDoc}
     *
     * @return the elapsed time between the start and end of {@link #Run()}
     */
    @Override
    public long getRunningTime() {
        return this.EndTime - this.StartTime;
    }
}
