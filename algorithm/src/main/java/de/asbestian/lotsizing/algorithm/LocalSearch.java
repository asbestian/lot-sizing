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
public class LocalSearch {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalSearch.class);
  private final Input input;
  private final Problem problem;
  private final double timeLimit;
  private final ExecutorService execServive;

  public LocalSearch(final Input input, final Problem problem, final double timeLimit) {
    this.input = input;
    this.problem = problem;
    this.timeLimit = timeLimit;
    this.execServive = Executors.newFixedThreadPool(4); // newCachedThreadPool();
  }

  public Schedule run(final int demandSize) throws ExecutionException, InterruptedException {
    final Instant start = Instant.now();
    Schedule currentBestSchedule = problem.computeOptimalInventoryCostSchedule();
    LOGGER.info("Initial schedule: {}", currentBestSchedule);
    LOGGER.info(
        "Cost: {} (changeover cost = {}, inventory cost = {})",
        currentBestSchedule.getCost(),
        currentBestSchedule.getChangeOverCost(),
        currentBestSchedule.getInventoryCost());
    final List<DemandVertex> demandVertices =
        problem.getDemandVertices().stream()
            .map(v -> (DemandVertex) v)
            .collect(Collectors.toList());
    List<DemandVertex> dVertices = new ArrayList<>(demandVertices);
    long iterations = 0;
    boolean improvement = false;
    while (Duration.between(start, Instant.now()).toSeconds() <= timeLimit) {
      final Graph<Vertex, DefaultEdge> resGraph = problem.getResidualGraph(currentBestSchedule);
      final List<Callable<Schedule>> callables = new ArrayList<>();
      Iterable<List<DemandVertex>> listIterable =
          improvement
              ? Iterables.partition(demandVertices, demandSize)
              : Iterables.partition(dVertices, demandSize);
      final Schedule finalCurrentBestSchedule = currentBestSchedule;
      for (final List<DemandVertex> partition : listIterable) {
        callables.add(() -> computeOptimalSchedule(partition, resGraph, finalCurrentBestSchedule));
      }
      List<Future<Schedule>> futures = execServive.invokeAll(callables);
      improvement = false;
      for (Future<Schedule> future : futures) {
        Schedule schedule = future.get();
        if (schedule.getCost() < currentBestSchedule.getCost()) {
          currentBestSchedule = schedule;
          improvement = true;
        }
      }
      LOGGER.debug("Currently best schedule: {}", currentBestSchedule);
      LOGGER.debug(
          "Cost: {} (changeover cost = {}, inventory cost = {})",
          currentBestSchedule.getCost(),
          currentBestSchedule.getChangeOverCost(),
          currentBestSchedule.getInventoryCost());
      Collections.shuffle(dVertices);
      ++iterations;
    }
    LOGGER.debug("Number of iterations: {}", iterations);
    LOGGER.debug("Time spent: {} seconds.", Duration.between(start, Instant.now()).toSeconds());
    execServive.shutdown();
    return currentBestSchedule;
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
