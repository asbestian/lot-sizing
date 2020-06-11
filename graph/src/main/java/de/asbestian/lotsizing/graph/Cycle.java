package de.asbestian.lotsizing.graph;

import de.asbestian.lotsizing.graph.vertex.DecisionVertex;
import de.asbestian.lotsizing.graph.vertex.DemandVertex;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.graph.vertex.Vertex.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jgrapht.alg.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sebastian Schenker */
public class Cycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(Cycle.class);
  private final List<Pair<DemandVertex, DecisionVertex>> activatedEdges;
  private final List<Pair<DemandVertex, DecisionVertex>> deactivatedEdges;
  private final List<Pair<Vertex, Vertex>> edges;

  public Cycle() {
    activatedEdges = new ArrayList<>();
    deactivatedEdges = new ArrayList<>();
    edges = new ArrayList<>();
  }

  public Cycle(final List<Vertex> vertices) {
    this();
    final int numVertices = vertices.size();
    if (numVertices > 1) {
      for (int j = 1; j < numVertices; ++j) {
        final Vertex source = vertices.get(j - 1);
        final Vertex target = vertices.get(j);
        addToDataStructures(source, target);
      }
      final Vertex source = vertices.get(numVertices - 1);
      final Vertex target = vertices.get(0);
      addToDataStructures(source, target);
    }
    if (LOGGER.isWarnEnabled() && activatedEdges.size() != deactivatedEdges.size()) {
      LOGGER.warn("Number of activated pairs unequal to number of deactivated pairs.");
    }
  }

  private void addToDataStructures(final Vertex source, final Vertex target) {
    edges.add(Pair.of(source, target));
    if (source.getVertexType() == Type.DEMAND_VERTEX
        && target.getVertexType() == Type.DECISION_VERTEX) {
      final var demandVertex = (DemandVertex) source;
      final var decisionVertex = (DecisionVertex) target;
      activatedEdges.add(Pair.of(demandVertex, decisionVertex));
    } else if (source.getVertexType() == Type.DECISION_VERTEX
        && target.getVertexType() == Type.DEMAND_VERTEX) {
      final var decisionVertex = (DecisionVertex) source;
      final var demandVertex = (DemandVertex) target;
      deactivatedEdges.add(Pair.of(demandVertex, decisionVertex));
    }
  }

  public boolean isEmpty() {
    return edges.isEmpty();
  }

  public List<Pair<Vertex, Vertex>> getEdges() {
    return Collections.unmodifiableList(edges);
  }

  public List<Pair<DemandVertex, DecisionVertex>> getActivatedEdges() {
    return Collections.unmodifiableList(activatedEdges);
  }

  public List<Pair<DemandVertex, DecisionVertex>> getDeactivatedEdges() {
    return Collections.unmodifiableList(deactivatedEdges);
  }
}
