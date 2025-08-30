# TSP Memetic Algorithm Solver

This project solves symmetric Traveling Salesman Problem (TSP) instances from TSPLIB using a Memetic Algorithm (Genetic Algorithm + Local Search). It can run a single instance or benchmark a directory of instances and compare results to best-known TSPLIB optima.

Reference TSPLIB STSP page: http://comopt.ifi.uni-heidelberg.de/software/TSPLIB95/STSP.html

## Project layout

- Main programs
  - [benchmark_main_class.java](benchmark_main_class.java): batch benchmark over [ALL_tsp](ALL_tsp) and writes “heuristic approach results.csv”.
  - [heuristic_algorithm_main.java](heuristic_algorithm_main.java): runs one chosen instance.
- Core algorithm
  - [`HeuristicApproach.GeneticAlgorithm`](HeuristicApproach/GeneticAlgorithm.java): Memetic algorithm (selection, crossover, mutation, local search).
  - [`HeuristicApproach.MetaHeuristic`](HeuristicApproach/MetaHeuristic.java): base class handling timing, best-so-far, and thread pool.
  - [`HeuristicApproach.GiantTour`](HeuristicApproach/GiantTour.java): permutation representation, cost evaluation, and local search.
- Data layer
  - [`Data.InputData`](Data/InputData.java): TSPLIB parser, distance computation with matrix/cache strategy.
  - [ALL_tsp](ALL_tsp): TSPLIB instances (.tsp).
  - [tsplib_best_known.csv](tsplib_best_known.csv): best-known costs for benchmarks.

## TSP (STSP) in brief

- Input: n cities and symmetric distances d(i, j).
- Goal: find a minimum-cost Hamiltonian cycle visiting each city exactly once and returning to the start.
- This solver assumes symmetric TSP data consistent with TSPLIB STSP instances.

## Memetic Algorithm overview

- Population-based search with genetic operators, hybridized with local search.
- Key components:
  - Representation: permutation handled by [`HeuristicApproach.GiantTour`](HeuristicApproach/GiantTour.java).
  - Initialization: randomized permutations locally improved.
  - Selection: tournament selection (size 5).
  - Crossover: applied with rate 0.9.
  - Mutation: applied with rate 0.1.
  - Local search: invoked inside `GiantTour` construction/improvement.
  - Parallelism: work submitted to a fixed thread pool sized to available CPU cores (see [`HeuristicApproach.MetaHeuristic`](HeuristicApproach/MetaHeuristic.java)).
  - Stopping: dynamic time budget based on instance size (`StopTime = max(200, 200 * ln(n))` ms).

## TSPLIB parsing and distances

- [`Data.InputData`](Data/InputData.java) parses TSPLIB headers and coordinates.
- Distance storage strategy:
  - For small n, uses a dense matrix for speed.
  - For larger n, uses a concurrent cache.
  - Override threshold via JVM property: -Dtsp.matrix.max=<n>
- Thread-safe access allows parallel evaluations.

## Benchmarking against TSPLIB best-known

- [tsplib_best_known.csv](tsplib_best_known.csv) provides best-known costs by file name (without extension).
- [benchmark_main_class.java](benchmark_main_class.java) computes:
  - Cost
  - Time to reach best-so-far (ms)
  - Gap (%) vs. best-known
- Output CSV: “heuristic approach results.csv”

## Requirements

- JDK 23
- VS Code (optional), Java extensions recommended
- OS: Windows/Linux/macOS

## Run in VS Code

- Open the folder in VS Code.
- To run one instance:
  - Edit file name in [heuristic_algorithm_main.java](heuristic_algorithm_main.java).
  - Run the main class “heuristic_algorithm_main”.
- To run the full benchmark:
  - Ensure [ALL_tsp](ALL_tsp) contains the instances and [tsplib_best_known.csv](tsplib_best_known.csv) is present.
  - Run the main class “benchmark_main_class”.

## Run from terminal

- Windows PowerShell:
  ```powershell
  mkdir out
  $files = Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName }
  javac -encoding UTF-8 -d out $files
  # Single instance
  java -cp out heuristic_algorithm_main
  # Benchmark all
  java -cp out benchmark_main_class
  ```
- Linux/macOS (bash):
  ```bash
  mkdir -p out
  javac -encoding UTF-8 -d out $(find . -name "*.java")
  # Single instance
  java -cp out heuristic_algorithm_main
  # Benchmark all
  java -cp out benchmark_main_class
  ```

Java options you may find useful:
- Increase heap for large instances: -Xmx2g
- Force dense matrix up to n: -Dtsp.matrix.max=300

Example:
```bash
java -Xmx2g -Dtsp.matrix.max=300 -cp out benchmark_main_class
```

## Customizing what runs

- Single instance file:
  - Edit the constant `file_name` in [heuristic_algorithm_main.java](heuristic_algorithm_main.java).
- Benchmark scope:
  - Edit directory and max dimension in [`benchmark_main_class.main`](benchmark_main_class.java):
    - Directory: “ALL_tsp”
    - Max dimension filter: `final int max_dimension = 500;`
- Algorithm parameters:
  - Crossover/mutation rates and population sizing are in [`HeuristicApproach.GeneticAlgorithm`](HeuristicApproach/GeneticAlgorithm.java).
  - Time budget and threading in [`HeuristicApproach.MetaHeuristic`](HeuristicApproach/MetaHeuristic.java).

## Output

- Console prints instance name, dimension, approach, and the best solution found (with cost).
- Benchmark writes “heuristic approach results.csv” with columns:
  - File Name, Dimension, Best Solution Reach Time(ms), Cost Value, Known Optimal, Gap(%)

## Notes

- Stochastic: results vary run-to-run due to Math.random().
- CPU utilization: parallel operators use all