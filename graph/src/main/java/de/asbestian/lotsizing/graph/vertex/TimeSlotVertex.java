package de.asbestian.lotsizing.graph.vertex;

/** @author Sebastian Schenker */
public class TimeSlotVertex extends Vertex {

  private final int timeSlot;

  public TimeSlotVertex(final int id, final int timeSlot) {
    super(id);
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
  public String toString() {
    return "TimeSlotVertex(" + getId() + ")";
  }
}
