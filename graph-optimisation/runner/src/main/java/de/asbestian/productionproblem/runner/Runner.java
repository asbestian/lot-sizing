package de.asbestian.productionproblem.runner;

import de.asbestian.productionproblem.input.Input;
import de.asbestian.productionproblem.optimisation.Problem;
import de.asbestian.productionproblem.optimisation.RandomEdgeRemover;
import de.asbestian.productionproblem.optimisation.Schedule;
import de.asbestian.productionproblem.optimisation.SubGraphGenerator;
import de.asbestian.productionproblem.optimisation.Vertex;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * @author Sebastian Schenker
 */
@Command(name = "graph-opt", mixinStandardHelpOptions = true, version = "0.9",
    description = "Production planning optimisation via graph algorithms.")
public class Runner implements Callable<Integer> {

  @Parameters(paramLabel = "file", description = "The file containing the problem "
      + "instance.")
  private String file;

  @Option(names = "--edges",
      description = "The maximal number of edges each subgraph can have.")
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
    System.out.println("Initial schedule: " + initSchedule);
    System.out.println(
        "Initial cost: " + initSchedule.getCost() +
            " (changeover cost = " + initSchedule.getChangeOverCost()
            + ", inventory cost = " + initSchedule.getInventoryCost() + ")");
    System.out.println();
    final Graph<Vertex, DefaultEdge> resGraph = problem.getResidualGraph(initSchedule);
    final SubGraphGenerator subGraphGenerator = new RandomEdgeRemover(edgeThreshold,
        resGraph);
    Optional<Schedule> min = subGraphGenerator.generate()
        .limit(iterations)
        .map(Problem::computeCycles)
        .peek(cycles -> System.out.println("Checking |cycles| =  " + cycles.size()))
        .flatMap(Collection::stream)
        .map(cycle -> initSchedule.compute(cycle, input))
        .filter(schedule -> schedule.getCost() < initSchedule.getCost())
        .min(Comparator.comparing(Schedule::getCost));
    if (min.isPresent()) {
      final Schedule schedule = min.get();
      System.out.println("\nBest found schedule: " + schedule);
      System.out.println(
          "Best found cost: " + schedule.getCost() +
              " (changeover cost = " + schedule.getChangeOverCost()
              + ", inventory cost = " + schedule.getInventoryCost() + ")");
      System.out.println();
    }
    return 0;
  }

}

