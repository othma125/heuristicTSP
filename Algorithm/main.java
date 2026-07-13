package Algorithm;

import Algorithm.Data.InputData;
import Algorithm.HeuristicApproach.GeneticAlgorithm;
import Algorithm.HeuristicApproach.MetaHeuristic;
import Algorithm.HeuristicApproach.Tour;
import java.io.File;

/**
 * Command-line entry point that solves a single TSPLIB instance with the memetic
 * algorithm and prints the resulting tour.
 *
 * @author Othmane
 */
class heuristic_algorithm_main {

    /**
     * Loads the chosen instance, runs the solver, and displays the best tour.
     *
     * @param args ignored; edit {@code file_name} to choose the instance
     */
    public static void main(String[] args) {
//        String file_name = "bier127.tsp";
//        String file_name = "burma14.tsp";
        String file_name = "dsj1000.tsp";
        File file = new File("ALL_tsp\\" + file_name);
        try (InputData data = new InputData(file)) {  // ensures close()
            MetaHeuristic algorithm = new GeneticAlgorithm(data);
            algorithm.Run();
            System.out.println("\nEnd Time = " + algorithm.getRunningTime() + " ms");
            Tour sol = algorithm.getBestSolution();
            sol.displaySolution();
        } catch (Exception ex) {
            System.err.println("Failed on instance " + file_name + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
