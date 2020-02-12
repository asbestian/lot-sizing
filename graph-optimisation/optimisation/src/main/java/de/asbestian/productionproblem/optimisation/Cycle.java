package de.asbestian.productionproblem.optimisation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;

/**
 * @author Sebastian Schenker
 */
public class Cycle {

  private final List<Pair<Vertex, Vertex>> originalGraphEdges;
  private final List<Pair<Vertex, Vertex>> reverseGraphEdges;
  private final List<DecisionVertex> activatedDecisionVertices;
  private final List<DecisionVertex> deactivatedDecisionVertices;

  public <E> Cycle(final List<Vertex> vertices, final Graph<Vertex, E> residualGraph) {
    final var numVertices = vertices.size();
    assert numVertices > 2;
    reverseGraphEdges = new ArrayList<>();
    originalGraphEdges = new ArrayList<>();
    activatedDecisionVertices = new ArrayList<>();
    deactivatedDecisionVertices = new ArrayList<>();
    for (int j = 1; j < numVertices; ++j) {
      final var source = vertices.get(j - 1);
      final var target = vertices.get(j);
      assert residualGraph.containsEdge(source, target);
      addToDataStructures(source, target);
    }
    final var source = vertices.get(numVertices - 1);
    final var target = vertices.get(0);
    assert residualGraph.containsEdge(source, target);
    addToDataStructures(source, target);
    assert activatedDecisionVertices.size() == deactivatedDecisionVertices.size();
  }

  private void addToDataStructures(final Vertex source, final Vertex target) {
    if (isOriginalEdge(source, target)) {
      originalGraphEdges.add(Pair.of(source, target));
    } else {
      reverseGraphEdges.add(Pair.of(source, target));
    }
    if (isActivatingEdge(source, target)) {
      activatedDecisionVertices.add((DecisionVertex) target);
    } else if (isDeactivatingEdge(source, target)) {
      deactivatedDecisionVertices.add((DecisionVertex) source);
    }
  }

  /**
   * Checks if edge corresponds to using a decision vertex
   *
   * @param source source vertex of the considered edge
   * @param target target vertex of the considered edge
   * @return true if source corresponds to demand vertex and target corresponds to decision vertex;
   * otherwise, false.
   */
  private boolean isActivatingEdge(final Vertex source, final Vertex target) {
    return source.getClass() == DemandVertex.class && target.getClass() == DecisionVertex.class;
  }

  /**
   * Checks if edge correspoinds to not using anymore a decision vertex
   *
   * @param source source vertex of the considered edge
   * @param target target vertex of the considered edge
   * @return true if source corresponds to decision vertex and target corresponds to demand vertex;
   */
  private boolean isDeactivatingEdge(final Vertex source, final Vertex target) {
    return source.getClass() == DecisionVertex.class && target.getClass() == DemandVertex.class;
  }

  /**
   * An edge is an edge in the original graph if it connects either a) a demand vertex to a decision
   * vertex, b) a decision vertex to a time slot vertex or c) a time slot vertex to the super sink.
   */
  private boolean isOriginalEdge(final Vertex source, final Vertex target) {
    return source.getClass() == DemandVertex.class && target.getClass() == DecisionVertex.class
        || source.getClass() == DecisionVertex.class && target.getClass() == TimeSlotVertex.class
        || source.getClass() == TimeSlotVertex.class && target.getClass() == SuperSink.class;
  }

  public List<Pair<Vertex, Vertex>> getOriginalGraphEdges() {
    return Collections.unmodifiableList(originalGraphEdges);
  }

  public List<Pair<Vertex, Vertex>> getReverseGraphEdges() {
    return Collections.unmodifiableList(reverseGraphEdges);
  }

  public List<DecisionVertex> getActivatedDecisionVertices() {
    return Collections.unmodifiableList(activatedDecisionVertices);
  }

  public List<DecisionVertex> getDeactivatedDecisionVertices() {
    return Collections.unmodifiableList(deactivatedDecisionVertices);
  }
}
