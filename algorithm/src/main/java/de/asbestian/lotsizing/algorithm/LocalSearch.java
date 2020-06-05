package de.asbestian.lotsizing.algorithm;

import com.google.common.collect.Iterables;
import de.asbestian.lotsizing.algorithm.cycle.CycleFinder;
import de.asbestian.lotsizing.graph.Cycle;
import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.graph.vertex.DemandVertex;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sebastian Schenker */
public class LocalSearch implements Solver {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalSearch.class);
  private final Input input;
  private final Problem problem;
  private final ExecutorService executorService;
  private final int neighbourhoodSize;

  public LocalSearch(
      final Input input, final Problem problem, final int numThreads, final int neighbourhoodSize) {
    this.input = input;
    this.problem = problem;
    this.executorService = Executors.newFixedThreadPool(numThreads);
    this.neighbourhoodSize = neighbourhoodSize;
  }

  @Override
  public Schedule search(double timeLimit) {
    final Instant start = Instant.now();
    Schedule bestSchedule = problem.computeOptimalInventoryCostSchedule();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Initial schedule: {}", bestSchedule);
      LOGGER.debug(
          "Cost: {} (changeover cost = {}, inventory cost = {})",
          bestSchedule.getCost(),
          bestSchedule.getChangeOverCost(),
          bestSchedule.getInventoryCost());
    }
    long numIterations = 0;
    while (Duration.between(start, Instant.now()).toSeconds() <= timeLimit) {
      boolean improvement = false;
      final List<Callable<Schedule>> callables = createCallables(bestSchedule);
      try {
        List<Future<Schedule>> futures = executorService.invokeAll(callables);
        for (Future<Schedule> future : futures) {
          Schedule schedule = future.get();
          if (schedule.getCost() < bestSchedule.getCost()) {
            bestSchedule = schedule;
            improvement = true;
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        LOGGER.info(e.getMessage());
        executorService.shutdown();
        Thread.currentThread().interrupt();
        return bestSchedule;
      }
      if (LOGGER.isDebugEnabled() && improvement) {
        LOGGER.debug("Improvement: {}", bestSchedule);
        LOGGER.debug(
            "Cost: {} (changeover cost = {}, inventory cost = {})",
            bestSchedule.getCost(),
            bestSchedule.getChangeOverCost(),
            bestSchedule.getInventoryCost());
      }
      ++numIterations;
    }
    executorService.shutdown();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Number of iterations: {}", numIterations);
      LOGGER.debug("Time spent: {} seconds.", Duration.between(start, Instant.now()).toSeconds());
    }
    return bestSchedule;
  }

  private List<Callable<Schedule>> createCallables(final Schedule schedule) {
    final Graph<Vertex, DefaultEdge> resGraph = problem.getResidualGraph(schedule);
    final List<Callable<Schedule>> callables = new ArrayList<>();
    for (final Set<DemandVertex> partition : computePartition()) {
      callables.add(() -> computeOptimalNeighbourhoodSchedule(partition, resGraph, schedule));
    }
    return callables;
  }

  private Iterable<Set<DemandVertex>> computePartition() {
    Collection<Set<DemandVertex>> partition = new ArrayList<>();
    for (List<DemandVertex> part :
        Iterables.partition(problem.getDemandVertices(), neighbourhoodSize)) {
      partition.add(new HashSet<>(part));
    }
    return partition;
  }

  private Graph<Vertex, DefaultEdge> createSubResidualGraph(
      final Set<DemandVertex> neighbourhood, final Graph<Vertex, DefaultEdge> resGraph) {
    final Graph<Vertex, DefaultEdge> subResGraph = new AsSubgraph<>(resGraph);
    problem.getDemandVertices().stream()
        .filter(v -> !neighbourhood.contains(v))
        .forEach(subResGraph::removeVertex);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("SubresidualGraph - number of edges: {}", subResGraph.edgeSet().size());
    }
    return subResGraph;
  }

  private Schedule computeOptimalNeighbourhoodSchedule(
      final Set<DemandVertex> neighbourhood,
      final Graph<Vertex, DefaultEdge> resGraph,
      final Schedule currentBestSchedule) {
    final Graph<Vertex, DefaultEdge> subResGraph = createSubResidualGraph(neighbourhood, resGraph);
    final CycleFinder cycleFinder = new CycleFinder();
    final List<Cycle> cycles = cycleFinder.computeCycles(subResGraph);
    return cycles.stream()
        .map(cycle -> currentBestSchedule.compute(cycle, input))
        .filter(schedule -> schedule.getCost() < currentBestSchedule.getCost())
        .min(Comparator.comparingDouble(Schedule::getCost))
        .orElse(currentBestSchedule);
  }
}
