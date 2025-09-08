package HeuristicApproach;

import Data.InputData;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */
public abstract class MetaHeuristic {
    
    InputData Data;
    public long StartTime;// Start Time in milliseconds
    public long BestSolutionReachingTime;
    private GiantTour BestSolution = null;
    final ReentrantLock Lock = new ReentrantLock();
    final long StagnationMinTime;
    final ExecutorService Executor;

    public MetaHeuristic(InputData data){
        this.Data = data;
        this.StagnationMinTime = (long) Math.max(100, 100 * Math.log(data.StopsCount));
        int AvailableProcessorCors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
        this.Executor = Executors.newFixedThreadPool(AvailableProcessorCors);
    }

    public GiantTour getBestSolution() {
        return this.BestSolution;
    }

    void setBestSolution(GiantTour solution) {
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
    
    public abstract long getRunningTime();
    
    public abstract void Run();
}