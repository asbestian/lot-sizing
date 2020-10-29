package de.asbestian.lotsizing.runner;

import de.asbestian.lotsizing.algorithm.Solver;
import de.asbestian.lotsizing.graph.Cycle;
import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.visualisation.Visualisation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** @author Sebastian Schenker */
public class Runner extends CmdArgs implements Callable<Integer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

  public static void main(String... args) {
    final int exitCode = new CommandLine(new Runner()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    if (!Files.exists(Paths.get(file))) {
      System.err.println("Given file cannot be found.");
      return 1;
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Initial schedule: {}", randomSchedule ? "random" : "optimal inventory cost");
      LOGGER.debug("Neighbourhood size: {}", neighbourhoodSize);
      LOGGER.debug("Time limit: {} seconds", timeLimit);
    }

    final RunnerComponent dagger =
        DaggerRunnerComponent.builder()
            .fileName(file)
            .resGraphVertexSize(neighbourhoodSize)
            .greatestDescent(greatestDescent)
            .build();
    Problem problem = dagger.problem();
    final Schedule initSchedule =
        randomSchedule
            ? problem.computeRandomSchedule()
            : problem.computeOptimalInventoryCostSchedule();
    Solver solver = enumerate ? dagger.solvers().get("enum") : dagger.solvers().get("lns");
    final Schedule schedule = solver.search(initSchedule, timeLimit);
    LOGGER.info("Best found schedule: {}", schedule);
    LOGGER.info(
        "cost: {} (changeover cost = {}, inventory cost = {})",
        schedule.getCost(),
        schedule.getChangeOverCost(),
        schedule.getInventoryCost());
    return 0;
  }

  private Visualisation visualiseVertices(final Problem problem) {
    final Visualisation visualisation = new Visualisation();
    visualisation.addVertices(
        problem.getDemandVertices(),
        problem.getDecisionVertices(),
        problem.getTimeSlotVertices(),
        problem.getSuperSink());
    return visualisation;
  }

  private void visualiseGraph(
      final Problem problem, final Graph<Vertex, DefaultEdge> graph, final String filename) {
    final Visualisation visualisation = visualiseVertices(problem);
    final List<Pair<Vertex, Vertex>> edges =
        graph.edgeSet().stream()
            .map(edge -> Pair.of(graph.getEdgeSource(edge), graph.getEdgeTarget(edge)))
            .collect(Collectors.toList());
    visualisation.addEdges(edges);
    visualisation.saveToJPG(filename);
  }

  private void visualiseCycle(final Problem problem, final Cycle cycle, final String filename) {
    final Visualisation visualisation = visualiseVertices(problem);
    visualisation.addEdges(cycle.getEdges());
    visualisation.saveToJPG(filename);
  }

  private void visualiseSchedule(
      final Problem problem, final Schedule schedule, final String filename) {
    final Visualisation visualisation = visualiseVertices(problem);
    visualisation.addEdges(problem.getUsedGraphEdges(schedule));
    visualisation.saveToJPG(filename);
  }
}
