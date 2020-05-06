package de.asbestian.lotsizing.graph.vertex;

/** @author Sebastian Schenker */
public class DemandVertex extends Vertex {

  private final int type;
  private final int timeSlot; // the time slot before item needs to be produced;

  public DemandVertex(final int id, final int type, final int timeSlot) {
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

  @Override
  public Type getVertexType() {
    return Type.DEMAND_VERTEX;
  }

  @Override
  public String toString() {
    return "DemandVertex(" + getId() + ")";
  }
}
