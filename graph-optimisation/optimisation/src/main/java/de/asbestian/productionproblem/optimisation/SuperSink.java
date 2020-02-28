package de.asbestian.productionproblem.optimisation;

/** @author Sebastian Schenker */
public class SuperSink implements Vertex {

  private final int id;

  public SuperSink(final int id) {
    this.id = id;
  }

  @Override
  public Type getVertexType() {
    return Type.SUPER_SINK;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof SuperSink)) {
      return false;
    }
    final var v = (SuperSink) obj;
    return v.id == this.id;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(id);
  }

  @Override
  public String toString() {
    return "SuperSink(" + id + ")";
  }
}
