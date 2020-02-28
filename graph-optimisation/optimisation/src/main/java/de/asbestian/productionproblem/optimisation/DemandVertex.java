package de.asbestian.productionproblem.optimisation;

/** @author Sebastian Schenker */
public class DemandVertex implements Vertex {

  private final int id;
  private final int type;
  private final int timeSlot; // the time slot before item needs to be produced;

  public DemandVertex(int id, int type, int timeSlot) {
    this.id = id;
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
  public int getId() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DemandVertex)) {
      return false;
    }
    final var v = (DemandVertex) obj;
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
    return "DemandVertex(" + id + ")";
  }
}
