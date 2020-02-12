package de.asbestian.productionproblem.runner;

import de.asbestian.productionproblem.input.Input;
import de.asbestian.productionproblem.optimisation.Problem;
import de.asbestian.productionproblem.optimisation.RandomEdgeRemover;
import de.asbestian.productionproblem.optimisation.Schedule;
import de.asbestian.productionproblem.optimisation.SubGraphGenerator;
import de.asbestian.productionproblem.optimisation.Vertex;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

/**
 * @author Sebastian Schenker
 */
public class Main {

  public static void main(final String... args) {
    final Input input = new Input();
    input.read(args[0]);
    final Problem problem = new Problem(input);
    problem.build();
    final Schedule initSchedule = problem.computeInitialSchedule();
    System.out.println("Initial schedule: " + initSchedule);
    System.out.println("Initial cost: " + initSchedule.getCost());
    final Graph<Vertex, DefaultEdge> resGraph = problem.getResidualGraph(initSchedule);
    final SubGraphGenerator subGraphGenerator = new RandomEdgeRemover(200, resGraph);
    Optional<Schedule> min = subGraphGenerator.generate()
        .limit(1000)
        .map(Problem::computeCycles)
        .peek(cycles -> System.out.println("Number of cycles: " + cycles.size()))
        .flatMap(Collection::stream)
        .map(cycle -> initSchedule.compute(cycle, input))
        .filter(schedule -> schedule.getCost() < initSchedule.getCost())
        .min(Comparator.comparing(Schedule::getCost));
    if (min.isPresent()) {
      final Schedule schedule = min.get();
      System.out.println("New best schedule: " + schedule);
      System.out.println("Best cost: " + schedule.getCost());
    }
  }
}

