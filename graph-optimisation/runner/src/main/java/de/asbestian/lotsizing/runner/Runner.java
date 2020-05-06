package de.asbestian.lotsizing.runner;

import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
import de.asbestian.lotsizing.visualisation.Visualisation;
import java.io.BufferedReader;
import java.io.FileReader;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

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
  public Integer call() {
    if (!Files.exists(Paths.get(file))) {
      System.err.println("Given file cannot be found.");
      return 1;
    }
    final Input input = new Input();
    input.read(file);
    final Problem problem = new Problem(input);
    problem.build();
    final Schedule initSchedule = problem.computeInitialSchedule();
    LOGGER.info("Initial schedule: {}", initSchedule);
    LOGGER.info(
        "Initial cost: {} (changeover cost = {}, inventory cost = {})",
        initSchedule.getCost(),
        initSchedule.getChangeOverCost(),
        initSchedule.getInventoryCost());
    final Visualisation initScheduleVis = getScheduleVis(problem, initSchedule);
    initScheduleVis.saveToJPG("initSchedule.jpg");
    final Graph<Vertex, DefaultEdge> resGraph = problem.getResidualGraph(initSchedule);

    final Schedule bestSchedule = initSchedule;
    assert initSchedule.getEdges().size() == bestSchedule.getEdges().size();
    final Visualisation bestScheduleVis = getScheduleVis(problem, bestSchedule);
    bestScheduleVis.saveToJPG("bestSchedule.jpg");
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
    return 0;
  }

  private Visualisation getScheduleVis(final Problem problem, final Schedule schedule) {
    final Visualisation visualisation = new Visualisation();
    visualisation.addVertices(
        problem.getDemandVertices(),
        problem.getDecisionVertices(),
        problem.getTimeSlotVertices(),
        problem.getSuperSink());
    visualisation.addEdges(schedule.getEdges());
    return visualisation;
  }
}
