package de.asbestian.lotsizing.optimisation.vertex;

/** @author Sebastian Schenker */
public class DecisionVertex extends Vertex {

  private final int type;
  private final int timeSlot;

  public DecisionVertex(final int id, final int type, final int timeSlot) {
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

  @Override
  public Type getVertexType() {
    return Type.DECISION_VERTEX;
  }

  @Override
  public String toString() {
    return "DecisionVertex(" + getId() + ")";
  }
}
