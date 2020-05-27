package de.asbestian.lotsizing.graph;

import de.asbestian.lotsizing.graph.vertex.DecisionVertex;
import de.asbestian.lotsizing.graph.vertex.DemandVertex;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.graph.vertex.Vertex.Type;
import de.asbestian.lotsizing.input.Input;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jgrapht.alg.util.Pair;

/** @author Sebastian Schenker */
public class Schedule {

  private final Collection<Pair<Vertex, Vertex>>
      edges; // the original graph edges used in this schedule
  private final int[] schedule;
  private final double changeOverCost;
  private final double inventoryCost;

  public Schedule(final Input input, final Collection<Pair<Vertex, Vertex>> usedEdges) {
    this.edges = new HashSet<>();
    this.schedule = new int[input.getNumTimeSlots()];

    Arrays.fill(schedule, -1);
    usedEdges.stream()
        .peek(this.edges::add)
        .filter(
            pair ->
                pair.getFirst().getVertexType() == Type.DEMAND_VERTEX
                    && pair.getSecond().getVertexType() == Type.DECISION_VERTEX)
        .forEach(
            pair -> {
              final var decisionVertex = (DecisionVertex) pair.getSecond();
              this.schedule[decisionVertex.getTimeSlot()] = decisionVertex.getType();
            });
    // needs to be called after schedule has been generated
    this.changeOverCost = computeChangeoverCost(this.schedule, input);
    this.inventoryCost = computeInventoryCost(this.edges, input.getInventoryCost());
  }

  public IntList getScheduleWithoutIdleSlots() {
    return getScheduleWithoutIdleSlots(this.schedule);
  }

  private static IntList getScheduleWithoutIdleSlots(final int[] schedule) {
    return Arrays.stream(schedule)
        .filter(type -> type != -1)
        .collect(IntArrayList::new, IntArrayList::add, IntArrayList::addAll);
  }

  private Schedule(
      final Collection<Pair<Vertex, Vertex>> edges,
      final int[] schedule,
      final double changeOverCost,
      final double inventoryCost) {
    this.edges = edges;
    this.schedule = schedule;
    this.changeOverCost = changeOverCost;
    this.inventoryCost = inventoryCost;
  }

  private double computeChangeoverCost(final int[] schedule, final Input input) {
    double cost = 0.;
    final IntList scheduleWithoutIdleTimeSlots = getScheduleWithoutIdleSlots(schedule);

    for (int j = 1; j < scheduleWithoutIdleTimeSlots.size(); ++j) {
      final int predType = scheduleWithoutIdleTimeSlots.getInt(j - 1);
      final int currentType = scheduleWithoutIdleTimeSlots.getInt(j);
      cost += input.getChangeOverCost(predType, currentType);
    }
    return cost;
  }

  private static double computeInventoryCost(
      final Collection<Pair<Vertex, Vertex>> usedEdges, final int inventoryCost) {
    return usedEdges.stream()
            .filter(
                pair ->
                    pair.getFirst().getVertexType() == Type.DEMAND_VERTEX
                        && pair.getSecond().getVertexType() == Type.DECISION_VERTEX)
            .mapToInt(
                pair -> {
                  final var demandVertex = (DemandVertex) pair.getFirst();
                  final var decisionVertex = (DecisionVertex) pair.getSecond();
                  assert demandVertex.getType() == decisionVertex.getType();
                  assert demandVertex.getTimeSlot() >= decisionVertex.getTimeSlot();
                  return demandVertex.getTimeSlot() - decisionVertex.getTimeSlot();
                })
            .sum()
        * inventoryCost;
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

  public Collection<Pair<Vertex, Vertex>> getEdges() {
    return Collections.unmodifiableCollection(edges);
  }

  public boolean containsEdge(final Vertex source, final Vertex target) {
    return edges.contains(Pair.of(source, target));
  }

  /** Computes a new schedule based on given parameters. */
  public Schedule compute(final Cycle cycle, final Input input) {
    final Set<Pair<Vertex, Vertex>> usedEdges = new HashSet<>(this.edges);
    cycle
        .getReverseGraphEdges()
        .forEach(edge -> usedEdges.remove(Pair.of(edge.getSecond(), edge.getFirst())));
    usedEdges.addAll(cycle.getOriginalGraphEdges());
    final int[] newSchedule = Arrays.copyOf(this.schedule, this.schedule.length);
    cycle
        .getDeactivatedDecisionVertices()
        .forEach(vertex -> newSchedule[vertex.getTimeSlot()] = -1);
    cycle
        .getActivatedDecisionVertices()
        .forEach(vertex -> newSchedule[vertex.getTimeSlot()] = vertex.getType());
    final double newChangeOverCost = computeChangeoverCost(newSchedule, input);
    final double newInventoryCost = computeInventoryCost(usedEdges, input.getInventoryCost());
    assert usedEdges.size() == this.edges.size();
    return new Schedule(usedEdges, newSchedule, newChangeOverCost, newInventoryCost);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Schedule)) {
      return false;
    }
    final Schedule other = (Schedule) obj;
    return Arrays.equals(this.schedule, other.schedule)
        && this.edges.size() == other.edges.size()
        && this.edges.containsAll(other.edges);
  }

  @Override
  public String toString() {
    return Arrays.toString(schedule);
  }
}
