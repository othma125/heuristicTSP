# TSP Memetic Algorithm Solver

![TSP solver web landing page](thumbnail.png)

This project solves symmetric Traveling Salesman Problem (TSP) instances from TSPLIB using a Memetic Algorithm (Genetic Algorithm + Local Search). It can run a single instance or benchmark a directory of instances and compare results to best-known TSPLIB optima.

Reference TSPLIB STSP page: http://comopt.ifi.uni-heidelberg.de/software/TSPLIB95/STSP.html

## Project layout

- Main programs
  - [benchmark.java](Algorithm/benchmark.java): batch benchmark over [ALL_tsp](Algorithm/ALL_tsp) and writes “heuristic approach results.csv”.
  - [main.java](Algorithm/main.java): runs one chosen instance.
- Core algorithm
  - [`Algorithm.HeuristicApproach.GeneticAlgorithm`](Algorithm/HeuristicApproach/GeneticAlgorithm.java): Memetic algorithm (selection, crossover, mutation, local search).
  - [`Algorithm.HeuristicApproach.MetaHeuristic`](Algorithm/HeuristicApproach/MetaHeuristic.java): base class handling timing, best-so-far, and thread pool.
  - [`Algorithm.HeuristicApproach.Tour`](Algorithm/HeuristicApproach/Tour.java): permutation representation, cost evaluation, and local search.
- Data layer
  - [`Algorithm.Data.InputData`](Algorithm/Data/InputData.java): TSPLIB parser, distance computation with matrix/cache strategy.
  - [ALL_tsp](Algorithm/ALL_tsp): TSPLIB instances (.tsp).
  - [tsplib_best_known.csv](Algorithm/ALL_tsp/tsplib_best_known.csv): best-known costs for benchmarks.

## TSP (STSP) in brief

- Input: n cities and symmetric distances d(i, j).
- Goal: find a minimum-cost Hamiltonian cycle visiting each city exactly once and returning to the start.
- This solver assumes symmetric TSP data consistent with TSPLIB STSP instances.

## Memetic Algorithm overview

- Population-based search with genetic operators, hybridized with local search.
- Key components:
  - Representation: permutation handled by [`Algorithm.HeuristicApproach.Tour`](Algorithm/HeuristicApproach/Tour.java).
  - Initialization: randomized permutations locally improved.
  - Selection: tournament selection (size 5).
  - Crossover: applied with rate 0.9.
  - Mutation: applied with rate 0.1.
  - Local search: invoked inside `Tour` construction/improvement.
  - Parallelism: work submitted to a fixed thread pool sized to available CPU cores (see [`Algorithm.HeuristicApproach.MetaHeuristic`](Algorithm/HeuristicApproach/MetaHeuristic.java)).
  - Stopping: dynamic time budget based on instance size (`StopTime = max(200, 200 * ln(n))` ms).

## TSPLIB parsing and distances

- [`Algorithm.Data.InputData`](Algorithm/Data/InputData.java) parses TSPLIB headers and coordinates.
- Distance storage strategy:
  - For small n, uses a dense matrix for speed.
  - For larger n, uses a concurrent cache.
  - Override threshold via JVM property: -Dtsp.matrix.max=<n>
- Thread-safe access allows parallel evaluations.

## Benchmarking against TSPLIB best-known

- [tsplib_best_known.csv](Algorithm/ALL_tsp/tsplib_best_known.csv) provides best-known costs by file name (without extension).
- [benchmark.java](Algorithm/benchmark.java) computes:
  - Cost
  - Time to reach best-so-far (ms)
  - Gap (%) vs. best-known
- Output CSV: “heuristic approach results.csv”

## Web landing page

A dependency-free landing page (built on the JDK `HttpServer`) lets you pick a TSPLIB instance, watch the live solver log, and visualize the best tour in the browser.

- [`Web.Server`](Web/Server.java): serves [Web/index.html](Web/index.html), [Web/app.js](Web/app.js), [Web/styles.css](Web/styles.css); lists [ALL_tsp](Algorithm/ALL_tsp) instances, streams the solver log over Server-Sent Events, and returns the final tour (with cost, time, and gap vs. [tsplib_best_known.csv](Algorithm/ALL_tsp/tsplib_best_known.csv)).
- The **Solve** button runs the memetic algorithm; **Stop** ends it early (`GeneticAlgorithm.requestStop()`); **Visualize** draws the closed Hamiltonian cycle over the cities; **Save** exports a TSPLIB `.tour` file.
- Instances with explicit distance matrices (no `NODE_COORD_SECTION`) still solve, but cannot be plotted.

Run it (from the project root):

```bash
./run-server.sh          # compiles everything, starts the server, opens the browser
```

Or manually:

```bash
javac -encoding UTF-8 -d out $(find . -name '*.java' ! -path './out/*')
java -cp out Web.Server   # then open http://localhost:8080
```

The port comes from the CLI argument, then `PORT` in `.env`, then `8080`. Use `./kill-server.sh` to stop it.

## Requirements

- JDK 23
- VS Code (optional), Java extensions recommended
- OS: Windows/Linux/macOS

## Run in VS Code

- Open the folder in VS Code.
- To run one instance:
  - Edit file name in [Algorithm/main.java](Algorithm/main.java).
  - Run the main class `Algorithm.heuristic_algorithm_main`.
- To run the full benchmark:
  - Ensure [ALL_tsp](Algorithm/ALL_tsp) contains the instances and [tsplib_best_known.csv](Algorithm/ALL_tsp/tsplib_best_known.csv) is present.
  - Run the main class `Algorithm.benchmark_main_class`.

## Run from terminal

- Windows PowerShell:
  ```powershell
  mkdir out
  $files = Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName }
  javac -encoding UTF-8 -d out $files
  # Single instance
  java -cp out Algorithm.heuristic_algorithm_main
  # Benchmark all
  java -cp out Algorithm.benchmark_main_class
  ```
- Linux/macOS (bash):
  ```bash
  mkdir -p out
  javac -encoding UTF-8 -d out $(find . -name "*.java")
  # Single instance
  java -cp out Algorithm.heuristic_algorithm_main
  # Benchmark all
  java -cp out Algorithm.benchmark_main_class
  ```

Java options you may find useful:
- Increase heap for large instances: -Xmx2g
- Force dense matrix up to n: -Dtsp.matrix.max=300

Example:
```bash
java -Xmx2g -Dtsp.matrix.max=300 -cp out Algorithm.benchmark_main_class
```

## Customizing what runs

- Single instance file:
  - Edit the constant `file_name` in [main.java](Algorithm/main.java).
- Benchmark scope:
  - Edit directory and max dimension in [`benchmark`](Algorithm/benchmark.java):
    - Directory: “ALL_tsp”
    - Max dimension filter: `final int max_dimension = 500;`
- Algorithm parameters:
  - Crossover/mutation rates and population sizing are in [`Algorithm.HeuristicApproach.GeneticAlgorithm`](Algorithm/HeuristicApproach/GeneticAlgorithm.java).
  - Time budget and threading in [`Algorithm.HeuristicApproach.MetaHeuristic`](Algorithm/HeuristicApproach/MetaHeuristic.java).

## Output

- Console prints instance name, dimension, approach, and the best solution found (with cost).
- Benchmark writes “heuristic approach results.csv” with columns:
  - File Name, Dimension, Best Solution Reach Time(ms), Cost Value, Known Optimal, Gap(%)

## Notes

- Stochastic: results vary run-to-run due to Math.random().
- CPU utilization: parallel operators use all
