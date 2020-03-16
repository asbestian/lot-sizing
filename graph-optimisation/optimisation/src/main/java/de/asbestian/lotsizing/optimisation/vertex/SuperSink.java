package de.asbestian.lotsizing.optimisation;

/** @author Sebastian Schenker */
public class SuperSink extends Vertex {

  public SuperSink(final int id) {
    super(id);
  }

  @Override
  public Type getVertexType() {
    return Type.SUPER_SINK;
  }

  @Override
  public String toString() {
    return "SuperSink(" + getId() + ")";
  }
}
