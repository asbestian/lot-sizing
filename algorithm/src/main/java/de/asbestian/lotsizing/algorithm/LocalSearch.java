package de.asbestian.lotsizing.algorithm;

import com.google.common.collect.Iterables;
import de.asbestian.lotsizing.algorithm.cycle.CycleFinder;
import de.asbestian.lotsizing.graph.Cycle;
import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.graph.vertex.DemandVertex;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.graph.vertex.Vertex.Type;
import de.asbestian.lotsizing.input.Input;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
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
    final List<DemandVertex> demandVertices =
        problem.getDemandVertices().stream()
            .map(v -> (DemandVertex) v)
            .collect(Collectors.toList());
    List<DemandVertex> dVertices = new ArrayList<>(demandVertices);
    long numIterations = 0;
    boolean improvement = false;
    while (Duration.between(start, Instant.now()).toSeconds() <= timeLimit) {
      final Graph<Vertex, DefaultEdge> resGraph = problem.getResidualGraph(bestSchedule);
      final List<Callable<Schedule>> callables = new ArrayList<>();
      Iterable<List<DemandVertex>> listIterable =
          improvement
              ? Iterables.partition(demandVertices, neighbourhoodSize)
              : Iterables.partition(dVertices, neighbourhoodSize);
      final Schedule finalCurrentBestSchedule = bestSchedule;
      for (final List<DemandVertex> partition : listIterable) {
        callables.add(() -> computeOptimalSchedule(partition, resGraph, finalCurrentBestSchedule));
      }
      try {
        List<Future<Schedule>> futures = executorService.invokeAll(callables);
        improvement = false;
        for (Future<Schedule> future : futures) {
          Schedule schedule = future.get();
          if (schedule.getCost() < bestSchedule.getCost()) {
            bestSchedule = schedule;
            improvement = true;
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        LOGGER.info(e.getMessage());
        Thread.currentThread().interrupt();
        return bestSchedule;
      }
      LOGGER.debug("Currently best schedule: {}", bestSchedule);
      LOGGER.debug(
          "Cost: {} (changeover cost = {}, inventory cost = {})",
          bestSchedule.getCost(),
          bestSchedule.getChangeOverCost(),
          bestSchedule.getInventoryCost());
      Collections.shuffle(dVertices);
      ++numIterations;
    }
    executorService.shutdown();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Number of iterations: {}", numIterations);
      LOGGER.debug("Time spent: {} seconds.", Duration.between(start, Instant.now()).toSeconds());
    }
    return bestSchedule;
  }

  private Schedule computeOptimalSchedule(
      final List<DemandVertex> demandVertices,
      final Graph<Vertex, DefaultEdge> resGraph,
      final Schedule currentBestSchedule) {
    final Graph<Vertex, DefaultEdge> subResGraph = new AsSubgraph<>(resGraph);
    for (final DefaultEdge edge : resGraph.edgeSet()) {
      final Vertex source = resGraph.getEdgeSource(edge);
      final Vertex target = resGraph.getEdgeTarget(edge);
      if (source.getVertexType() == Type.DEMAND_VERTEX
          && target.getVertexType() == Type.DECISION_VERTEX) {
        if (!demandVertices.contains((DemandVertex) source)) {
          subResGraph.removeEdge(edge);
        }
      } else if (source.getVertexType() == Type.DECISION_VERTEX
          && target.getVertexType() == Type.DEMAND_VERTEX) {
        if (!demandVertices.contains((DemandVertex) target)) {
          subResGraph.removeEdge(edge);
        }
      }
    }
    LOGGER.trace("SubResidualGraph: |edges| = {}", subResGraph.edgeSet().size());
    final CycleFinder cycleFinder = new CycleFinder(subResGraph);
    final List<Cycle> cycles = cycleFinder.computeCycles();
    LOGGER.trace("Number of cycles: {} ", cycles.size());
    return cycles.stream()
        .map(cycle -> currentBestSchedule.compute(cycle, input))
        .filter(schedule -> schedule.getCost() < currentBestSchedule.getCost())
        .min(Comparator.comparingDouble(Schedule::getCost))
        .orElse(currentBestSchedule);
  }
}
