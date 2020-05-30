package de.asbestian.lotsizing.runner;

import de.asbestian.lotsizing.algorithm.Enumeration;
import de.asbestian.lotsizing.algorithm.NeighbourhoodSearch;
import de.asbestian.lotsizing.graph.Cycle;
import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
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
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** @author Sebastian Schenker */
@Command(
    name = "graph-opt",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Lot sizing optimisation via graph algorithms.")
public class Runner implements Callable<Integer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

  @Parameters(paramLabel = "file", description = "The file containing the problem instance.")
  private String file;

  @Option(
      names = {"-t", "--timeLimit"},
      description = "Time limit (in seconds) for computation. Default is no time limit.")
  private double timeLimit = Double.POSITIVE_INFINITY;

  @Option(
      names = {"-e", "--enumerate"},
      description = "Do full enumeration of the search space.")
  private boolean enumerate = false;

  @Option(
      names = {"--demandSize"},
      description =
          "Number of demand vertices used in neightbourhood search iterations. Default is 8.")
  private int demandSize = 8;

  public static void main(final String... args) {
    final int exitCode = new CommandLine(new Runner()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    if (!Files.exists(Paths.get(file))) {
      System.err.println("Given file cannot be found.");
      return 1;
    }
    final Input input = new Input();
    input.read(file);
    final Problem problem = new Problem(input);
    problem.build();
    if (enumerate) {
      final Enumeration enumeration = new Enumeration(input, problem);
      final Schedule schedule = enumeration.search(timeLimit);
      LOGGER.info(
          "{} schedule: {}", enumeration.isSearchSpaceExhausted() ? "Optimal" : "Best", schedule);
      LOGGER.info(
          "{} cost: {} (changeover cost = {}, inventory cost = {})",
          enumeration.isSearchSpaceExhausted() ? "Optimal" : "Best",
          schedule.getCost(),
          schedule.getChangeOverCost(),
          schedule.getInventoryCost());
    } else {
      final NeighbourhoodSearch nSearch = new NeighbourhoodSearch(input, problem, timeLimit);
      final Schedule schedule = nSearch.run(demandSize);
      LOGGER.info("schedule: {}", schedule);
      LOGGER.info(
          "Cost: {} (changeover cost = {}, inventory cost = {})",
          schedule.getCost(),
          schedule.getChangeOverCost(),
          schedule.getInventoryCost());
    }
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
    visualisation.addEdges(cycle.getOriginalGraphEdges());
    visualisation.addEdges(cycle.getReverseGraphEdges());
    visualisation.saveToJPG(filename);
  }

  private void visualiseSchedule(
      final Problem problem, final Schedule schedule, final String filename) {
    final Visualisation visualisation = visualiseVertices(problem);
    visualisation.addEdges(schedule.getEdges());
    visualisation.saveToJPG(filename);
  }
}
