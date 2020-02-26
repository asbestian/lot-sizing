package de.asbestian.productionproblem.runner;

import de.asbestian.productionproblem.input.Input;
import de.asbestian.productionproblem.optimisation.Problem;
import de.asbestian.productionproblem.optimisation.RandomEdgeRemover;
import de.asbestian.productionproblem.optimisation.Schedule;
import de.asbestian.productionproblem.optimisation.SubGraphGenerator;
import de.asbestian.productionproblem.optimisation.Vertex;
import de.asbestian.productionproblem.visualisation.Visualisation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.jgrapht.Graph;
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

  public static void main(String... args) {
    int exitCode = new CommandLine(new Runner()).execute(args);
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
    final SubGraphGenerator subGraphGenerator = new RandomEdgeRemover(edgeThreshold, resGraph);
    Optional<Schedule> minSchedule =
        subGraphGenerator
            .generate()
            .limit(iterations)
            .map(Problem::computeCycles)
            .peek(cycles -> LOGGER.info("Number of investigated cycles: {}", cycles.size()))
            .flatMap(Collection::stream)
            .map(cycle -> initSchedule.compute(cycle, input))
            .filter(schedule -> schedule.getCost() < initSchedule.getCost())
            .min(Comparator.comparing(Schedule::getCost));

    final Schedule bestSchedule = minSchedule.orElse(initSchedule);
    assert initSchedule.getUsedEdges().size() == bestSchedule.getUsedEdges().size();
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
    try {
      System.in.read();
    } catch (Exception e) {
    }
    return 0;
  }

  private Visualisation getScheduleVis(final Problem problem, final Schedule schedule) {
    final Visualisation visualisation = new Visualisation();
    visualisation.addVertices(
        problem.getDemandVertices(),
        problem.getDecisionVertices(),
        problem.getTimeSlotVertices(),
        problem.getSuperSink());
    visualisation.addEdges(schedule.getUsedEdges());
    return visualisation;
  }
}
