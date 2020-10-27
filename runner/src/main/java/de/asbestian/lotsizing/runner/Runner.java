package de.asbestian.lotsizing.runner;

import de.asbestian.lotsizing.algorithm.Enumeration;
import de.asbestian.lotsizing.algorithm.LocalSearchImpl;
import de.asbestian.lotsizing.algorithm.Solver;
import de.asbestian.lotsizing.graph.Cycle;
import de.asbestian.lotsizing.graph.DaggerProblemFactory;
import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.FileInput;
import de.asbestian.lotsizing.input.Input;
import de.asbestian.lotsizing.visualisation.Visualisation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** @author Sebastian Schenker */
public class Runner {

  private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

  public static void main(final String... args) {
    CmdArgs cmdArgs = new CmdArgs();
    new CommandLine(cmdArgs).parseArgs(args);
    if (!Files.exists(Paths.get(cmdArgs.file))) {
      System.err.println("Given file cannot be found.");
      System.exit(1);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Initial schedule: {}", cmdArgs.randomSchedule ? "random" : "optimal inventory cost");
      LOGGER.debug("Neighbourhood size: {}", cmdArgs.neighbourhoodSize);
      LOGGER.debug("Time limit: {} seconds", cmdArgs.timeLimit);
    }
    Problem prob = DaggerProblemFactory.builder().fileName(cmdArgs.file).build().problem();
    final Input input = new FileInput(cmdArgs.file);
    final Problem problem = new Problem(input);
    final Schedule initSchedule =
        cmdArgs.randomSchedule
            ? problem.computeRandomSchedule()
            : problem.computeOptimalInventoryCostSchedule();
    final Solver solver =
        cmdArgs.enumerate
            ? new Enumeration(input, problem)
            : new LocalSearchImpl(
                input, problem, cmdArgs.neighbourhoodSize, cmdArgs.greatestDescent);
    final Schedule schedule = solver.search(initSchedule, cmdArgs.timeLimit);
    LOGGER.info("Best found schedule: {}", schedule);
    LOGGER.info(
        "cost: {} (changeover cost = {}, inventory cost = {})",
        schedule.getCost(),
        schedule.getChangeOverCost(),
        schedule.getInventoryCost());
    System.exit(0);
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
