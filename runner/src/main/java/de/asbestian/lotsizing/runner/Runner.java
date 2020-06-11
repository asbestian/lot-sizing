package de.asbestian.lotsizing.runner;

import de.asbestian.lotsizing.algorithm.Enumeration;
import de.asbestian.lotsizing.algorithm.LocalSearchImpl;
import de.asbestian.lotsizing.algorithm.Solver;
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

  @Option(
      names = {"-t", "--timeLimit"},
      description = "Time limit (in seconds) of computation. Default value is ${DEFAULT-VALUE}.",
      defaultValue = "600")
  private double timeLimit;

  @Option(
      names = {"-e", "--enumerate"},
      description =
          "Attempt full enumeration of the search space. (Note that given time limit applies.)",
      defaultValue = "false")
  private boolean enumerate;

  @Option(
      names = {"-n", "--neighbourhood"},
      description =
          "Size of initial demand vertex neighbourhood used in local search procedure. Default value is ${DEFAULT-VALUE}.",
      defaultValue = "4")
  private int neighbourhoodSize;

  @Option(
      names = {"-p", "--percentage"},
      description =
          "Percentage of demand vertex set indices covered by local search iteration before shuffling. Default value is ${DEFAULT-VALUE}.",
      defaultValue = "0.6")
  private double percentage;

  @Option(
      names = {"-r", "--random"},
      description =
          "Use random schedule as initial schedule. Default is to use the optimal inventory cost schedule is used as initial schedule.",
      defaultValue = "false")
  private boolean randomSchedule;

  @Option(
      names = {"-g", "--greatestDescent"},
      description = "Use greatest descent improvement. Default is to first descent improvement.",
      defaultValue = "false")
  private boolean greatestDescent;

  @Parameters(paramLabel = "file", description = "The file containing the problem instance.")
  private String file;

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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Initial schedule: {}", randomSchedule ? "random" : "optimal inventory cost");
      LOGGER.debug("Neighbourhood size: {}", neighbourhoodSize);
      LOGGER.debug("Time limit: {} seconds", timeLimit);
    }
    final Input input = new Input();
    input.read(file);
    final Problem problem = new Problem(input);
    problem.build();
    if (enumerate) {
      final Enumeration enumeration = new Enumeration(input, problem);
      final Schedule initSchedule =
          randomSchedule
              ? problem.computeRandomSchedule()
              : problem.computeOptimalInventoryCostSchedule();
      final Schedule schedule = enumeration.search(initSchedule, timeLimit);
      LOGGER.info(
          "{} schedule: {}", enumeration.isSearchSpaceExhausted() ? "Optimal" : "Best", schedule);
      LOGGER.info(
          "{} cost: {} (changeover cost = {}, inventory cost = {})",
          enumeration.isSearchSpaceExhausted() ? "Optimal" : "Best",
          schedule.getCost(),
          schedule.getChangeOverCost(),
          schedule.getInventoryCost());
    } else {
      final Solver localSearch =
          new LocalSearchImpl(input, problem, neighbourhoodSize, greatestDescent, percentage);
      final Schedule initSchedule =
          randomSchedule
              ? problem.computeRandomSchedule()
              : problem.computeOptimalInventoryCostSchedule();
      final Schedule schedule = localSearch.search(initSchedule, timeLimit);
      LOGGER.info("Best schedule: {}", schedule);
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
