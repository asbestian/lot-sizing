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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.IntStream;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sebastian Schenker */
public class LocalSearch implements Solver {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalSearch.class);
  private static final int QUEUE_CAPACITY = 20;
  private static final int MAX_SOLUTION_POOL_SIZE = 10;
  protected final Input input;
  protected final Problem problem;
  private final int subResGraphVertexSize;
  protected final boolean useGreatestDescent;
  private final TreeSet<Schedule> solutionPool;

  public LocalSearch(
      final Input input,
      final Problem problem,
      final int subResGraphVertexSize,
      final boolean useGreatestDescent) {
    this.input = input;
    this.problem = problem;
    this.subResGraphVertexSize = subResGraphVertexSize;
    this.useGreatestDescent = useGreatestDescent;
    this.solutionPool = new TreeSet<>(Comparator.comparingDouble(Schedule::getCost));
  }

  @Override
  public Schedule search(final Schedule initSchedule, double timeLimit) {
    final Instant start = Instant.now();
    Iteration iteration = new Iteration(initSchedule);
    SortedSet<Schedule> improvements = iteration.compute();
    if (improvements.isEmpty()) {
      return initSchedule;
    }
    solutionPool.addAll(improvements);
    if (LOGGER.isDebugEnabled()) {
      improvements.forEach(
          s ->
              LOGGER.debug(
                  "Improvment - Cost: {} (changeover cost = {}, inventory cost = {})",
                  s.getCost(),
                  s.getChangeOverCost(),
                  s.getInventoryCost()));
    }
    Iterator<Schedule> iter = solutionPool.iterator();
    Schedule schedule;
    while (Duration.between(start, Instant.now()).toSeconds() <= timeLimit && iter.hasNext()) {
      schedule = iter.next();
      iteration = new Iteration(schedule);
      improvements = iteration.compute();
      if (LOGGER.isDebugEnabled()) {
        improvements.forEach(
            s ->
                LOGGER.debug(
                    "Improvment - Cost: {} (changeover cost = {}, inventory cost = {})",
                    s.getCost(),
                    s.getChangeOverCost(),
                    s.getInventoryCost()));
      }
      if (!improvements.isEmpty()) {
        solutionPool.addAll(improvements);
        while (solutionPool.size() >= MAX_SOLUTION_POOL_SIZE) {
          solutionPool.pollLast();
        }
        iter = solutionPool.iterator();
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Time spent: {} seconds.", Duration.between(start, Instant.now()).toSeconds());
    }
    return solutionPool.first();
  }

  /*private Pair<Boolean, Schedule> computeBestImprovementSchedule(
      final Graph<Vertex, DefaultEdge> subResGraph, final Schedule currentSchedule) {
    final CycleFinder cycleFinder = new CycleFinder();
    final List<Cycle> cycles = cycleFinder.computeCycles(subResGraph);
    final Optional<Schedule> bestSchedule =
        cycles.stream()
            .map(cycle -> currentSchedule.compute(cycle, input))
            .filter(schedule -> schedule.getCost() < currentSchedule.getCost())
            .min(Comparator.comparingDouble(Schedule::getCost));
    return bestSchedule.isEmpty()
        ? Pair.of(false, currentSchedule)
        : Pair.of(true, bestSchedule.get());
  }/

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

  class Iteration {
    private static final int SEED = 1;
    private static final int MAX_SHUFFLE_ATTEMPTS = 10;
    private final Random random;
    private final Schedule currentSchedule;
    private final IntList indices;
    private final Graph<Vertex, DefaultEdge> resGraph;
    private final List<DemandVertex> demand;
    private final SortedSet<Schedule> improvementSchedules;
    private IntListIterator iter;
    private int shuffleCounter;

    Iteration(final Schedule schedule) {
      this.random = new Random(SEED);
      this.currentSchedule = schedule;
      this.indices = createShuffledIndices();
      this.improvementSchedules = new TreeSet<>(Comparator.comparingDouble(Schedule::getCost));
      this.demand = schedule.getNonIdleProduction();
      this.resGraph = problem.getResidualGraph(schedule);
      this.iter = indices.iterator();
      this.shuffleCounter = 0;
    }

    SortedSet<Schedule> compute() {
      while (improvementSchedules.size() <= MAX_SOLUTION_POOL_SIZE
          && shuffleCounter <= MAX_SHUFFLE_ATTEMPTS) {
        final Graph<Vertex, DefaultEdge> subResGraph = createSubResidualGraph();
        final CycleFinder cycleFinder = new CycleFinder();
        final BlockingQueue<Cycle> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        final Thread computeCycles =
            new Thread(() -> cycleFinder.computeCycles(subResGraph, queue));
        computeCycles.start();
        Cycle cycle;
        do {
          try {
            cycle = queue.take();
          } catch (final InterruptedException e) {
            computeCycles.interrupt();
            break;
          }
          final Schedule schedule = currentSchedule.compute(cycle, input);
          if (schedule.getCost() < currentSchedule.getCost()) {
            computeCycles.interrupt();
            improvementSchedules.add(schedule);
            break;
          }
        } while (!cycle.isEmpty());
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
      ++shuffleCounter;
    }
  }
}
