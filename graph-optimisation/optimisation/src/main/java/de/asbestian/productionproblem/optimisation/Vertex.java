package de.asbestian.productionproblem.optimisation;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The vertex types for the considered graph.
 *
 * @author Sebastian Schenker
 */
public class Vertex {

  private final int id;

  public Vertex(final int id) {
    this.id = id;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Vertex)) {
      return false;
    }
    final var v = (Vertex) o;
    return v.id == this.id;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return String.valueOf(id);
  }
}

class IdSupplier implements Supplier<Integer> {

  private int id = 0;

  @Override
  public Integer get() {
    return id++;
  }
}

class DemandVertex extends Vertex {

  private final int type;
  private final int timeSlot; // the time slot before item needs to be produced;

  DemandVertex(final int id, final int type, final int timeSlot) {
    super(id);
    this.type = type;
    this.timeSlot = timeSlot;
  }

  public int getTimeSlot() {
    return timeSlot;
  }

  public int getType() {
    return type;
  }
}

class DecisionVertex extends Vertex {

  private final int type;
  private final int timeSlot;

  DecisionVertex(final int id, final int type, final int timeSlot) {
    super(id);
    this.timeSlot = timeSlot;
    this.type = type;
  }

  public int getType() {
    return type;
  }

  public int getTimeSlot() {
    return timeSlot;
  }
}

class TimeSlotVertex extends Vertex {

  private final int timeSlot;

  TimeSlotVertex(final int id, final int timeSlot) {
    super(id);
    this.timeSlot = timeSlot;
  }
}

class SuperSink extends Vertex {

  SuperSink(final int id) {
    super(id);
  }
}
