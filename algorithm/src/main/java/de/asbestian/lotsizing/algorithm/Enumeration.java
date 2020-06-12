package de.asbestian.lotsizing.algorithm;

import de.asbestian.lotsizing.algorithm.cycle.CycleFinder;
import de.asbestian.lotsizing.graph.Cycle;
import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sebastian Schenker */
public class Enumeration {

  private static final Logger LOGGER = LoggerFactory.getLogger(Enumeration.class);
  private static final int QUEUE_CAPACITY = 10;
  private final Input input;
  private final Problem problem;
  private final long timeLimit;
  private boolean searchSpaceExhausted;

  public Enumeration(final Input input, final Problem problem, final long timeLimit) {
    this.input = input;
    this.problem = problem;
    this.timeLimit = timeLimit;
    this.searchSpaceExhausted = false;
  }

  public boolean isSearchSpaceExhausted() {
    return searchSpaceExhausted;
  }

  public Schedule search(final Schedule initSchedule) {
    final Instant start = Instant.now();
    searchSpaceExhausted = false;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Initial schedule: {}", initSchedule);
      LOGGER.debug(
          "Cost: {} (changeover cost = {}, inventory cost = {})",
          initSchedule.getCost(),
          initSchedule.getChangeOverCost(),
          initSchedule.getInventoryCost());
    }
    final Graph<Vertex, DefaultEdge> resGraph = problem.getResidualGraph(initSchedule);
    final BlockingQueue<Cycle> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    final Thread computeCycles = new Thread(new CycleFinder(resGraph, queue));
    computeCycles.start();
    Schedule bestSchedule = initSchedule;
    int numIterations = 0;
    while (Duration.between(start, Instant.now()).toSeconds() <= timeLimit) {
      Cycle cycle;
      try {
        cycle = queue.take();
      } catch (final InterruptedException e) {
        LOGGER.info(e.getMessage());
        Thread.currentThread().interrupt();
        break;
      }
      if (cycle.isEmpty()) {
        searchSpaceExhausted = true;
        break;
      }
      Schedule schedule = initSchedule.compute(cycle, input);
      if (schedule.getCost() < bestSchedule.getCost()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Improvement: {} with overall cost: {}", schedule, schedule.getCost());
        }
        bestSchedule = schedule;
      }
      ++numIterations;
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Number of iterations: {}", numIterations);
      LOGGER.debug("Search space exhausted: {}", searchSpaceExhausted);
      LOGGER.debug("Time spent: {} seconds.", Duration.between(start, Instant.now()).toSeconds());
    }
    return bestSchedule;
  }
}
