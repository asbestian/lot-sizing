package de.asbestian.productionproblem.optimisation;

/**
 * The vertex types for the considered graph.
 *
 * @author Sebastian Schenker
 */
public interface Vertex {

  enum Type {
    DEMAND_VERTEX,
    DECISION_VERTEX,
    TIME_SLOT_VERTEX,
    SUPER_SINK
  }

  Vertex.Type getVertexType();

  int getId();
}
