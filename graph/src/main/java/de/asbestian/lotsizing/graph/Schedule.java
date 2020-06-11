package de.asbestian.lotsizing.graph;

import de.asbestian.lotsizing.graph.vertex.DecisionVertex;
import de.asbestian.lotsizing.graph.vertex.DemandVertex;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.graph.vertex.Vertex.Type;
import de.asbestian.lotsizing.input.Input;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import org.jgrapht.alg.util.Pair;

/** @author Sebastian Schenker */
public class Schedule {

  private final int length; // number of time slots in entire schedule
  private final Int2ObjectSortedMap<DemandVertex> production;
  private final double changeOverCost;
  private final double inventoryCost;

  public Schedule(final Input input, final Collection<Pair<Vertex, Vertex>> usedEdges) {
    this.length = input.getNumTimeSlots();
    this.production = new Int2ObjectRBTreeMap<>();
    usedEdges.stream()
        .filter(
            pair ->
                pair.getFirst().getVertexType() == Type.DEMAND_VERTEX
                    && pair.getSecond().getVertexType() == Type.DECISION_VERTEX)
        .forEach(
            pair -> {
              final var decisionVertex = (DecisionVertex) pair.getSecond();
              this.production.put(decisionVertex.getTimeSlot(), (DemandVertex) pair.getFirst());
            });
    final Pair<Double, Double> costs = computeCost(this.production, input);
    this.changeOverCost = costs.getFirst();
    this.inventoryCost = costs.getSecond();
  }

  private Schedule(
      final Input input, final int length, final Int2ObjectSortedMap<DemandVertex> production) {
    this.length = length;
    this.production = production;
    final Pair<Double, Double> costs = computeCost(this.production, input);
    this.changeOverCost = costs.getFirst();
    this.inventoryCost = costs.getSecond();
  }

  private static Pair<Double, Double> computeCost(
      final Int2ObjectSortedMap<DemandVertex> production, final Input input) {
    double changeoverCost = 0.;
    double inventoryCost = 0.;
    if (production.size() == 1) {
      final int timeSlot = production.firstIntKey();
      final DemandVertex vertex = production.get(timeSlot);
      inventoryCost += (vertex.getTimeSlot() - timeSlot) * input.getInventoryCost();
    } else if (production.size() >= 2) {
      final var iter = production.keySet().iterator();
      int currentSlot = iter.nextInt();
      int nextSlot;
      DemandVertex current, next;
      while (true) {
        // add inventory cost of current time slot
        current = production.get(currentSlot);
        inventoryCost += (current.getTimeSlot() - currentSlot) * input.getInventoryCost();
        // add change over cost between current and next
        nextSlot = iter.nextInt();
        next = production.get(nextSlot);
        changeoverCost += input.getChangeOverCost(current.getType(), next.getType());
        if (iter.hasNext()) {
          currentSlot = nextSlot;
        } else {
          inventoryCost += (next.getTimeSlot() - nextSlot) * input.getInventoryCost();
          break;
        }
      }
    }
    return Pair.of(changeoverCost, inventoryCost);
  }

  public Int2ObjectMap<DemandVertex> getSlot2Demand() {
    return Int2ObjectMaps.unmodifiable(production);
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

  /** Computes a new schedule based on given parameters. */
  public Schedule compute(final Cycle cycle, final Input input) {
    final Int2ObjectSortedMap<DemandVertex> prod = new Int2ObjectRBTreeMap<>(this.production);
    cycle.getDeactivatedEdges().forEach(pair -> prod.remove(pair.getSecond().getTimeSlot()));
    cycle
        .getActivatedEdges()
        .forEach(pair -> prod.put(pair.getSecond().getTimeSlot(), pair.getFirst()));
    assert prod.size() == this.production.size();
    return new Schedule(input, this.length, prod);
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
    return this.length == other.length && this.production.equals(other.production);
  }

  @Override
  public int hashCode() {
    return production.hashCode() + 31 * length;
  }

  @Override
  public String toString() {
    final List<Integer> schedule =
        IntStream.generate(() -> -1)
            .limit(length)
            .collect(IntArrayList::new, IntArrayList::add, IntArrayList::addAll);
    production.forEach((k, v) -> schedule.set(k, v.getType()));
    return schedule.toString();
  }
}
