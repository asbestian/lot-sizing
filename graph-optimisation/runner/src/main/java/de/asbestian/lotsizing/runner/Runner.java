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
    version = "0.9",
    description = "Production planning optimisation via graph algorithms.")
public class Runner implements Callable<Integer> {

  private final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

  @Parameters(paramLabel = "file", description = "The file containing the problem " + "instance.")
  private String file;

  @Option(names = "--edges", description = "The maximal number of edges each subgraph can have.")
  private int edgeThreshold = 200;

  @Option(names = "--iterations", description = "The maximal number of iterations.")
  private int iterations = 15;

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
    final Input input = new Input();
    input.read(file);
    final Problem problem = new Problem(input);
    problem.build();
    final Schedule initSchedule = problem.computeInitialSchedule();
    LOGGER.debug("Initial schedule: {}", initSchedule);
    LOGGER.debug(
        "Initial cost: {} (changeover cost = {}, inventory cost = {})",
        initSchedule.getCost(),
        initSchedule.getChangeOverCost(),
        initSchedule.getInventoryCost());
    final Graph<Vertex, DefaultEdge> resGraph = problem.getResidualGraph(initSchedule);
    visualiseGraph(problem, resGraph, "resgraph.jpg");
    final CycleFinder cycleFinder = new CycleFinder(resGraph);
    final BlockingQueue<Cycle> queue = new ArrayBlockingQueue<>(10);
    cycleFinder.computeCycles(queue);
    while (true) {
      final Cycle cycle = queue.take();
      if (cycle.isEmpty()) {
        break;
      }
      final Schedule schedule = initSchedule.compute(cycle, input);
      System.out.println("Cost:" + schedule.getCost());
    }

    /*final Visualisation bestScheduleVis = getScheduleVis(problem, bestSchedule);

      final Schedule bestSchedule = initSchedule;
      System.out.println("\nBest found schedule: " + bestSchedule);
      System.out.println(
          "Best found cost: "
              + bestSchedule.getCost()
              + " (changeover cost = "
              + bestSchedule.getChangeOverCost()
              + ", inventory cost = "
              + bestSchedule.getInventoryCost()
              + ")");
      System.out.println();
      visualiseGraph(problem, resGraph, "resgraph.jpg");
      return 0;
    }*/
    return 0;
  }

  private void visualiseGraph(
      final Problem problem, final Graph<Vertex, DefaultEdge> graph, final String filename) {
    final Visualisation visualisation = new Visualisation();
    visualisation.addVertices(
        problem.getDemandVertices(),
        problem.getDecisionVertices(),
        problem.getTimeSlotVertices(),
        problem.getSuperSink());
    List<Pair<Vertex, Vertex>> edges =
        graph.edgeSet().stream()
            .map(edge -> Pair.of(graph.getEdgeSource(edge), graph.getEdgeTarget(edge)))
            .collect(Collectors.toList());
    visualisation.addEdges(edges);
    visualisation.saveToJPG(filename);
  }

  private void visualiseSchedule(
      final Problem problem, final Schedule schedule, final String filename) {
    final Visualisation visualisation = new Visualisation();
    visualisation.addVertices(
        problem.getDemandVertices(),
        problem.getDecisionVertices(),
        problem.getTimeSlotVertices(),
        problem.getSuperSink());
    visualisation.addEdges(schedule.getEdges());
    visualisation.saveToJPG(filename);
  }
}
