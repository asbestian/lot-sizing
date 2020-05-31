package de.asbestian.lotsizing.runner;

import de.asbestian.lotsizing.algorithm.cycle.CycleFinder;
import de.asbestian.lotsizing.graph.Cycle;
import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
import de.asbestian.lotsizing.visualisation.Visualisation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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
      names = {"-b", "--blockingQueueCapacity"},
      description = "Number of elements available in blocking queue. Default is 10.")
  private int blockingQueueCapacity = 10;

  public static void main(final String... args) {
    final int exitCode = new CommandLine(new Runner()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws InterruptedException {
    if (!Files.exists(Paths.get(file))) {
      System.err.println("Given file cannot be found.");
      return 1;
    }
    final Instant start = Instant.now();
    final Input input = new Input();
    input.read(file);
    final Problem problem = new Problem(input);
    problem.build();
    final Schedule initSchedule = problem.computeOptimalInventoryCostSchedule();
    LOGGER.info("Initial schedule: {}", initSchedule);
    LOGGER.info(
        "Cost: {} (changeover cost = {}, inventory cost = {})",
        initSchedule.getCost(),
        initSchedule.getChangeOverCost(),
        initSchedule.getInventoryCost());

    final Graph<Vertex, DefaultEdge> resGraph = problem.getResidualGraph(initSchedule);
    final CycleFinder cycleFinder = new CycleFinder(resGraph);
    final BlockingQueue<Cycle> queue = new ArrayBlockingQueue<>(blockingQueueCapacity);
    final Thread computeCycles =
        new Thread(
            () -> {
              try {
                cycleFinder.computeCycles(queue);
              } catch (final InterruptedException e) {
                e.printStackTrace();
              }
            });
    computeCycles.start();
    Schedule bestSchedule = initSchedule;
    boolean searchSpaceExhausted = false;
    long iterations = 0;
    while (Duration.between(start, Instant.now()).toSeconds() < timeLimit) {
      final Cycle cycle = queue.take();
      if (cycle.isEmpty()) {
        searchSpaceExhausted = true;
        LOGGER.debug("Search space exhausted.");
        break;
      }
      final Schedule schedule = initSchedule.compute(cycle, input);
      if (schedule.getCost() < bestSchedule.getCost()) {
        LOGGER.info("Improvement: {} with overall cost: {}", schedule, schedule.getCost());
        bestSchedule = schedule;
      }
      ++iterations;
    }
    LOGGER.debug("Number of iterations: {}", iterations);
    LOGGER.info("{} schedule: {}", searchSpaceExhausted ? "Optimal" : "Best", bestSchedule);
    LOGGER.info(
        "{} cost: {} (changeover cost = {}, inventory cost = {})",
        searchSpaceExhausted ? "Optimal" : "Best",
        bestSchedule.getCost(),
        bestSchedule.getChangeOverCost(),
        bestSchedule.getInventoryCost());
    LOGGER.info("Time spent: {} seconds.", Duration.between(start, Instant.now()).toSeconds());
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
