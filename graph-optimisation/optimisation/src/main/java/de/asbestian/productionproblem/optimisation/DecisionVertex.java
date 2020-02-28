package de.asbestian.productionproblem.optimisation;

/** @author Sebastian Schenker */
public class DecisionVertex implements Vertex {

  private final int id;
  private final int type;
  private final int timeSlot;

  public DecisionVertex(int id, int type, int timeSlot) {
    this.id = id;
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
  public int getId() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DecisionVertex)) {
      return false;
    }
    final var v = (DecisionVertex) obj;
    return v.id == this.id;
  }

  @Override
  public int hashCode() {
    return 31 * Integer.hashCode(id)
        + 31 * Integer.hashCode(type)
        + 31 * Integer.hashCode(timeSlot);
  }

  @Override
  public String toString() {
    return "DecisionVertex(" + id + ")";
  }
}
