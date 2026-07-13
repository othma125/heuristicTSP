package Algorithm.HeuristicApproach;

import Algorithm.Data.InputData;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for population-based metaheuristics over the TSP.
 *
 * <p>Holds the shared infrastructure: the instance data, a fixed thread pool
 * sized to the available cores, timing bookkeeping, and a thread-safe
 * best-so-far solution. Subclasses implement {@link #Run()} and
 * {@link #getRunningTime()}.
 *
 * @author Othmane
 */
public abstract class MetaHeuristic {

    InputData Data;
    /** Wall-clock start time of the run, in milliseconds. */
    public long StartTime;
    /** Wall-clock time at which the current best solution was reached, in milliseconds. */
    public long BestSolutionReachingTime;
    private Tour BestSolution = null;
    final ReentrantLock Lock = new ReentrantLock();
    final long StagnationMinTime;
    final ExecutorService Executor;

    /**
     * Initializes shared state for the given instance: the stagnation budget and
     * a fixed thread pool sized to the available processor cores.
     *
     * @param data the instance to solve
     */
    public MetaHeuristic(InputData data){
        this.Data = data;
        this.StagnationMinTime = (long) Math.max(100, 100 * Math.sqrt(data.StopsCount));
        int AvailableProcessorCors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
        this.Executor = Executors.newFixedThreadPool(AvailableProcessorCors);
    }

    /**
     * Returns the best solution found so far.
     *
     * @return the current best tour, or {@code null} before initialization
     */
    public Tour getBestSolution() {
        return this.BestSolution;
    }

    /**
     * Atomically adopts {@code solution} as the new best if it improves on the
     * current one, recording the time it was reached and logging the cost.
     *
     * @param solution the candidate solution
     */
    void setBestSolution(Tour solution) {
        this.Lock.lock();
        try {
            if (this.BestSolution == null || solution.compareTo(this.BestSolution) < 0) {
                this.BestSolutionReachingTime = System.currentTimeMillis();
                this.BestSolution = solution;
                System.out.println(solution.getCost() + " after " + (this.BestSolutionReachingTime  - this.StartTime) + " ms");
            }
        } finally {
            this.Lock.unlock();
        }
    }
    
    /**
     * Returns the total running time of the completed search.
     *
     * @return the elapsed time in milliseconds
     */
    public abstract long getRunningTime();

    /** Executes the search, updating the best-so-far solution as it improves. */
    public abstract void Run();
}
