package de.asbestian.productionproblem.optimisation;

/**
 * @author Sebastian Schenker
 */
public class TimeSlotVertex implements Vertex {

  private final int id;
  private final int timeSlot;

  TimeSlotVertex(final int id, final int timeSlot) {
    this.id = id;
    this.timeSlot = timeSlot;
  }

  public int getTimeSlot() {
    return timeSlot;
  }

  @Override
  public Type getVertexType() {
    return Type.TIME_SLOT_VERTEX;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TimeSlotVertex)) {
      return false;
    }
    final var v = (TimeSlotVertex) obj;
    return v.id == this.id;
  }

  @Override
  public int hashCode() {
    return 31 * Integer.hashCode(id) + 31 * Integer.hashCode(timeSlot);
  }

  @Override
  public String toString() {
    return "TimeSlotVertex(" + id + ")";
  }
}
