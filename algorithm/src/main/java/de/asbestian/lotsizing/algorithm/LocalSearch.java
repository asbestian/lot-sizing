package de.asbestian.lotsizing.algorithm;

import de.asbestian.lotsizing.algorithm.cycle.CycleFinder;
import de.asbestian.lotsizing.graph.Cycle;
import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.graph.vertex.DecisionVertex;
import de.asbestian.lotsizing.graph.vertex.DemandVertex;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sebastian Schenker */
public class LocalSearch {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalSearch.class);
  private static final int QUEUE_CAPACITY = 10;
  private static final int MAX_SOLUTION_POOL_SIZE = 20;
  protected final Input input;
  protected final Problem problem;
  private final int subResGraphVertexSize;
  private final long timeLimit;
  private final long iterationTimeLimit;
  private final TreeSet<Schedule> solutionPool;
  private final ExecutorService executorService;

  public LocalSearch(
      final Input input,
      final Problem problem,
      final int subResGraphVertexSize,
      final long timeLimit,
      final long iterationTimeLimit) {
    this.input = input;
    this.problem = problem;
    this.subResGraphVertexSize = subResGraphVertexSize;
    this.timeLimit = timeLimit;
    this.iterationTimeLimit = iterationTimeLimit;
    this.solutionPool = new TreeSet<>(Comparator.comparingDouble(Schedule::getCost));
    this.executorService = Executors.newFixedThreadPool(4);
  }

  public Schedule search(final List<Schedule> initSchedules) {
    final Instant start = Instant.now();
    solutionPool.addAll(initSchedules);
    List<Future<List<Schedule>>> futures;
    while (Duration.between(start, Instant.now()).toSeconds() <= timeLimit) {
      Collection<Callable<List<Schedule>>> callables =
          solutionPool.stream().map(Iteration::new).collect(Collectors.toList());
      try {
        futures = executorService.invokeAll(callables);
      } catch (final InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
        break;
      }
      futures.forEach(this::addSchedules);
      trimSolutionPool();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Current solution pool:");
        solutionPool.forEach(s -> LOGGER.debug("Cost: {}", s.getCost()));
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Time spent: {} seconds.", Duration.between(start, Instant.now()).toSeconds());
    }
    return solutionPool.first();
  }

  private void addSchedules(final Future<List<Schedule>> future) {
    try {
      solutionPool.addAll(future.get(iterationTimeLimit, TimeUnit.SECONDS));
    } catch (TimeoutException e) {
      LOGGER.debug(e.getMessage());
      future.cancel(true);
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.warn(e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  private void trimSolutionPool() {
    while (solutionPool.size() >= MAX_SOLUTION_POOL_SIZE) {
      solutionPool.pollLast();
    }
  }

  class Iteration implements Callable<List<Schedule>> {
    private static final int SEED = 1;
    private static final int NUM_IMPROVEMENT_SCHEDULES = 2;
    private final Random random;
    private final Schedule currentSchedule;
    private final IntList indices;
    private final Graph<Vertex, DefaultEdge> resGraph;
    private final List<DemandVertex> demand;
    private final List<Schedule> improvementSchedules;
    private IntListIterator iter;

    Iteration(final Schedule schedule) {
      this.random = new Random(SEED);
      this.currentSchedule = schedule;
      this.indices = createShuffledIndices();
      this.improvementSchedules = new ArrayList<>();
      this.demand = schedule.getNonIdleProduction();
      this.resGraph = problem.getResidualGraph(schedule);
      this.iter = indices.iterator();
    }

    @Override
    public List<Schedule> call() {
      while (improvementSchedules.size() <= NUM_IMPROVEMENT_SCHEDULES
          && !Thread.currentThread().isInterrupted()) {
        final Graph<Vertex, DefaultEdge> subResGraph = createSubResidualGraph();
        final BlockingQueue<Cycle> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        final Thread computeCycles = new Thread(new CycleFinder(subResGraph, queue));
        computeCycles.start();
        Cycle cycle;
        do {
          try {
            cycle = queue.take();
          } catch (final InterruptedException e) {
            computeCycles.interrupt();
            Thread.currentThread().interrupt();
            break;
          }
          final Schedule schedule = currentSchedule.compute(cycle, input);
          if (schedule.getCost() < currentSchedule.getCost()) {
            improvementSchedules.add(schedule);
          }
        } while (!cycle.isEmpty() && !Thread.currentThread().isInterrupted());
      }
      return improvementSchedules;
    }

    private IntList createShuffledIndices() {
      final IntList indices =
          IntStream.range(0, input.getNumProducedItems() - subResGraphVertexSize)
              .collect(IntArrayList::new, IntArrayList::add, IntArrayList::addAll);
      Collections.shuffle(indices, random);
      return indices;
    }

    private Graph<Vertex, DefaultEdge> createSubResidualGraph() {
      if (subResGraphVertexSize >= input.getNumProducedItems()) {
        return resGraph;
      }
      final Set<Vertex> subResGraphVertices = computeVerticesInSubResGraph();
      return new AsSubgraph<>(resGraph, subResGraphVertices);
    }

    private Set<Vertex> computeVerticesInSubResGraph() {
      if (!iter.hasNext()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Shuffling.");
        }
        shuffleDemand();
        iter = indices.iterator();
      }
      int index = iter.nextInt();
      final Collection<DemandVertex> demandVerticesInSubResGraph =
          demand.subList(index, index + subResGraphVertexSize);
      final Set<Vertex> verticesInSubResGraph = new HashSet<>(demandVerticesInSubResGraph);
      for (final DemandVertex demandVertex : demandVerticesInSubResGraph) {
        for (int slot = 0; slot <= demandVertex.getTimeSlot(); ++slot) {
          final DecisionVertex decisionVertex =
              problem.getDecisionVertex(demandVertex.getType(), slot);
          verticesInSubResGraph.add(decisionVertex);
          verticesInSubResGraph.add(problem.getTimeSlotVertex(decisionVertex.getTimeSlot()));
        }
      }
      verticesInSubResGraph.add(problem.getSuperSink());
      return verticesInSubResGraph;
    }

    private void shuffleDemand() {
      int begin = 0, end;
      do {
        end = Math.min(begin + subResGraphVertexSize, demand.size());
        Collections.shuffle(demand.subList(begin, end));
        begin += subResGraphVertexSize;
      } while (begin < demand.size());
    }
  }
}
