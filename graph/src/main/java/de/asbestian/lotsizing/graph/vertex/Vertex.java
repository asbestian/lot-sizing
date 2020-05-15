package de.asbestian.lotsizing.graph.vertex;

/** @author Sebastian Schenker */
public class Vertex {

  public enum Type {
    DECISION_VERTEX,
    DEMAND_VERTEX,
    TIME_SLOT_VERTEX,
    SUPER_SINK,
    UNSPECIFIED
  }

  private final int id;

  public Vertex(final int id) {
    this.id = id;
  }

  public Vertex.Type getVertexType() {
    return Type.UNSPECIFIED;
  }

  public int getId() {
    return id;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Vertex)) {
      return false;
    }
    final var v = (Vertex) obj;
    return v.id == this.id;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(id);
  }

  @Override
  public String toString() {
    return "Vertex(" + id + ")";
  }
}
