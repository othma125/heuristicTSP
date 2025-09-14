

import Data.InputData;
import HeuristicApproach.*;
import java.io.File;
/**
 *
 * @author Othmane
 */
public class heuristic_algorithm_main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String file_name = "bier127.tsp";
//        String file_name = "burma14.tsp";
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
