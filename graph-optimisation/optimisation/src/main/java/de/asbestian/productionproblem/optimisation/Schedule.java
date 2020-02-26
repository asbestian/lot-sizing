package de.asbestian.productionproblem.optimisation;

import de.asbestian.productionproblem.input.Input;
import de.asbestian.productionproblem.optimisation.Vertex.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;

/**
 * @author Sebastian Schenker
 */
public class Schedule {

  private final Set<Pair<Vertex, Vertex>> usedEdges;
  private final int[] schedule;
  private final double changeOverCost;
  private final double inventoryCost;

  Schedule(final Input input,
      final Graph<Vertex, DefaultEdge> graph, final Map<DefaultEdge, Double> flows) {
    this.usedEdges = new HashSet<>();
    this.schedule = new int[input.getNumTimeSlots()];
    Arrays.fill(schedule, -1);
    graph.edgeSet().stream()
        .filter(edge -> flows.get(edge) > 0)
        .map(edge -> {
          final Vertex source = graph.getEdgeSource(edge);
          final Vertex target = graph.getEdgeTarget(edge);
          return Pair.of(source, target);
        })
        .peek(usedEdges::add)
        .filter(pair -> pair.getFirst().getVertexType() == Type.DEMAND_VERTEX
            && pair.getSecond().getVertexType() == Type.DECISION_VERTEX)
        .forEach(
            pair -> {
              final var decisionVertex = (DecisionVertex) pair.getSecond();
              this.schedule[decisionVertex.getTimeSlot()] = decisionVertex.getType();
            });
    // needs to be called after schedule has been generated
    this.changeOverCost = computeChangeoverCost(this.schedule, input);
    this.inventoryCost = computeInventoryCost(this.usedEdges, input.getInventoryCost());
  }

  private Schedule(
      final Set<Pair<Vertex, Vertex>> usedEdges,
      final int[] schedule,
      final double changeOverCost,
      final double inventoryCost) {
    this.usedEdges = usedEdges;
    this.schedule = schedule;
    this.changeOverCost = changeOverCost;
    this.inventoryCost = inventoryCost;
  }

  private static double computeChangeoverCost(final int[] schedule, final Input input) {
    double cost = 0.;
    final int[] scheduleWithoutIdleTimeSlots =
        Arrays.stream(schedule)
            .filter(type -> type != -1) // remove idle time slots of schedule
            .toArray();
    for (int j = 1; j < scheduleWithoutIdleTimeSlots.length; ++j) {
      final var predType = scheduleWithoutIdleTimeSlots[j - 1];
      final var currentType = scheduleWithoutIdleTimeSlots[j];
      cost += input.getChangeOverCost(predType, currentType);
    }
    return cost;
  }

  private static double computeInventoryCost(final Collection<Pair<Vertex, Vertex>> usedEdges,
      int inventoryCost) {
    return usedEdges.stream()
        .filter(pair -> pair.getFirst().getVertexType() == Type.DEMAND_VERTEX
            && pair.getSecond().getVertexType() == Type.DECISION_VERTEX)
        .mapToInt(pair -> {
          final DemandVertex source = (DemandVertex) pair.getFirst();
          final DecisionVertex target = (DecisionVertex) pair.getSecond();
          assert source.getType() == target.getType();
          assert source.getTimeSlot() <= target.getTimeSlot();
          return source.getTimeSlot() - target.getTimeSlot();
        })
        .sum() * inventoryCost;
  }

  public double getChangeOverCost() {
    return this.changeOverCost;
  }

  public double getInventoryCost() {
    return this.inventoryCost;
  }

  public double getCost() {
    return getChangeOverCost() + getInventoryCost();
  }

  public Collection<Pair<Vertex, Vertex>> getUsedEdges() {
    return Collections.unmodifiableSet(usedEdges);
  }

  public boolean containsEdge(final Vertex source, final Vertex target) {
    return usedEdges.contains(Pair.of(source, target));
  }

  /**
   * Computes a new schedule based on given parameters.
   */
  public Schedule compute(final Cycle cycle, final Input input) {
    cycle.getReverseGraphEdges()
        .forEach(edge -> usedEdges.remove(Pair.of(edge.getSecond(), edge.getFirst())));
    usedEdges.addAll(cycle.getOriginalGraphEdges());
    final int[] schedule = Arrays.copyOf(this.schedule, this.schedule.length);
    cycle.getDeactivatedDecisionVertices().forEach(vertex -> schedule[vertex.getTimeSlot()] = -1);
    cycle
        .getActivatedDecisionVertices()
        .forEach(vertex -> schedule[vertex.getTimeSlot()] = vertex.getType());
    final double changeOverCost = computeChangeoverCost(schedule, input);
    final double inventoryCost = computeInventoryCost(usedEdges, input.getInventoryCost());
    return new Schedule(usedEdges, schedule, changeOverCost, inventoryCost);
  }

  @Override
  public String toString() {
    return Arrays.toString(schedule);
  }
}
