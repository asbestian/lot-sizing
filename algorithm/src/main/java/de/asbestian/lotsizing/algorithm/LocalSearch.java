package de.asbestian.lotsizing.algorithm;

import de.asbestian.lotsizing.algorithm.cycle.CycleFinder;
import de.asbestian.lotsizing.graph.Cycle;
import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sebastian Schenker */
abstract class LocalSearch implements Solver {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalSearch.class);
  private static final int QUEUE_CAPACITY = 100;
  protected final Input input;
  protected final Problem problem;
  protected final boolean useGreatestDescent;

  LocalSearch(final Input input, final Problem problem, final boolean useGreatestDescent) {
    this.input = input;
    this.problem = problem;
    this.useGreatestDescent = useGreatestDescent;
  }

  @Override
  public Schedule search(final Schedule initSchedule, double timeLimit) {
    final Instant start = Instant.now();
    Schedule currentSchedule = initSchedule;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Initial schedule: {}", currentSchedule);
      LOGGER.debug(
          "Cost: {} (changeover cost = {}, inventory cost = {})",
          currentSchedule.getCost(),
          currentSchedule.getChangeOverCost(),
          currentSchedule.getInventoryCost());
    }
    Graph<Vertex, DefaultEdge> resGraph = null;
    boolean newScheduleFound = true;
    while (Duration.between(start, Instant.now()).toSeconds() <= timeLimit) {
      if (newScheduleFound) {
        resGraph = problem.getResidualGraph(currentSchedule);
      }
      final Graph<Vertex, DefaultEdge> subResGraph =
          createSubResidualGraph(newScheduleFound, resGraph, currentSchedule);
      Pair<Boolean, Schedule> ret = useGreatestDescent ? computeBestImprovementSchedule(subResGraph, currentSchedule) : computeFirstImprovementSchedule(subResGraph, currentSchedule);
      newScheduleFound = ret.getFirst();
      currentSchedule = ret.getSecond();
      if (LOGGER.isTraceEnabled() && newScheduleFound) {
        LOGGER.trace("Improvement: {}", currentSchedule);
      }
      if (LOGGER.isDebugEnabled() && newScheduleFound) {
        LOGGER.debug(
            "Cost: {} (changeover cost = {}, inventory cost = {})",
            currentSchedule.getCost(),
            currentSchedule.getChangeOverCost(),
            currentSchedule.getInventoryCost());
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Time spent: {} seconds.", Duration.between(start, Instant.now()).toSeconds());
    }
    return currentSchedule;
  }

  /**
   * Creates a subgraph of the currently considered residual graph.
   *
   * @param newResGraph Indicates whether given residual graph is different from last iteration
   * @param resGraph Currently considered residual graph
   * @param schedule Currently considered (best) schedule
   * @return Subgraph of residual graph
   */
  protected abstract Graph<Vertex, DefaultEdge> createSubResidualGraph(
      final boolean newResGraph, final Graph<Vertex, DefaultEdge> resGraph,
      final Schedule schedule);

  private Pair<Boolean, Schedule> computeBestImprovementSchedule(final Graph<Vertex, DefaultEdge> subResGraph, final Schedule currentSchedule) {
    final CycleFinder cycleFinder = new CycleFinder();
    final List<Cycle> cycles = cycleFinder.computeCycles(subResGraph);
    final Optional<Schedule> bestSchedule = cycles.stream().map(cycle -> currentSchedule.compute(cycle, input))
        .filter(schedule -> schedule.getCost() < currentSchedule.getCost())
        .min(Comparator.comparingDouble(Schedule::getCost));
    return bestSchedule.isEmpty() ? Pair.of(false, currentSchedule) : Pair.of(true, bestSchedule.get());
  }

  /**
   * Attempts to compute new (and better) schedule.
   *
   * @param subResGraph Subgraph of residual graph whose cycles are considered for finding new
   *     schedules
   * @param currentSchedule Currently considered schedule
   * @return true if better schedule was found
   */
  private Pair<Boolean, Schedule> computeFirstImprovementSchedule(
      final Graph<Vertex, DefaultEdge> subResGraph, final Schedule currentSchedule) {
    final CycleFinder cycleFinder = new CycleFinder();
    final BlockingQueue<Cycle> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    final Thread computeCycles = new Thread(() -> cycleFinder.computeCycles(subResGraph, queue));
    computeCycles.start();
    Cycle cycle;
    boolean improvement = false;
    Schedule bestSchedule = currentSchedule;
    long cycleCounter = 0;
    do {
      try {
        cycle = queue.take();
      } catch (final InterruptedException e) {
        computeCycles.interrupt();
        break;
      }
      final Schedule schedule = currentSchedule.compute(cycle, input);
      ++cycleCounter;
      if (schedule.getCost() < currentSchedule.getCost()) {
        computeCycles.interrupt();
        bestSchedule = schedule;
        improvement = true;
        break;
      }
    } while (!cycle.isEmpty());
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Number of investigated cycles: {}", cycleCounter);
    }
    return Pair.of(improvement, bestSchedule);
  }
}
