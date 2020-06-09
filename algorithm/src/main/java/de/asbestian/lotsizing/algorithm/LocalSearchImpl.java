package de.asbestian.lotsizing.algorithm;

import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.vertex.DecisionVertex;
import de.asbestian.lotsizing.graph.vertex.DemandVertex;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sebastian Schenker */
public class LocalSearchImpl extends LocalSearch {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalSearchImpl.class);
  private static final int SEED = 1;
  private final Random random;
  private final int subResGraphVertexSize;
  private final int shufflingThreshold;
  private List<DemandVertex> demandVertices;
  private int counter;

  public LocalSearchImpl(
      final Input input, final Problem problem, final int subResGraphVertexSize, final boolean useGreatestDescent) {
    super(input, problem, useGreatestDescent);
    this.random = new Random(SEED);
    this.subResGraphVertexSize =
        Math.min(problem.getDemandVertices().size(), subResGraphVertexSize);
    this.shufflingThreshold = 2*problem.getDemandVertices().size() / subResGraphVertexSize;
    this.demandVertices = new ArrayList<>(problem.getDemandVertices());
    this.counter = 0;
  }

  @Override
  protected Graph<Vertex, DefaultEdge> createSubResidualGraph(
      final boolean newResGraph, Graph<Vertex, DefaultEdge> resGraph) {
    final Set<Vertex> subResGraphVertices = getVerticesInSubResGraph(newResGraph);
    return new AsSubgraph<>(resGraph, subResGraphVertices);
    // return buildSubResGraph(subResGraphVertices, resGraph);
  }

  private Graph<Vertex, DefaultEdge> buildSubResGraph(
      final Set<Vertex> vertices, final Graph<Vertex, DefaultEdge> resGraph) {
    final Graph<Vertex, DefaultEdge> subResGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
    vertices.forEach(subResGraph::addVertex);
    for (final Vertex vertex : vertices) {
      resGraph.outgoingEdgesOf(vertex).stream()
          .map(resGraph::getEdgeTarget)
          .filter(subResGraph::containsVertex)
          .forEach(target -> subResGraph.addEdge(vertex, target));
    }
    return subResGraph;
  }

  private Set<Vertex> getVerticesInSubResGraph(final boolean newResGraph) {
    if (newResGraph) {
      demandVertices = new ArrayList<>(problem.getDemandVertices());
      counter = 0;
    } else {
      ++counter;
    }
    if (counter >= shufflingThreshold) {
      Collections.shuffle(demandVertices);
      LOGGER.debug("Shuffling demand vertices.");
      counter = 0;
    }
    int begin = random.nextInt(Math.max(1, demandVertices.size() - subResGraphVertexSize));
    final List<DemandVertex> demandVerticesInSubResGraph =
        demandVertices.subList(begin, begin + subResGraphVertexSize);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(
          "Demand vertex indices in SubResGraph: [{}, {}]", begin, begin + subResGraphVertexSize);
    }
    final Set<Vertex> verticesInSubResGraph = new HashSet<>(demandVerticesInSubResGraph);
    for (final DemandVertex demandVertex : demandVerticesInSubResGraph) {
      for (int slot = 0; slot <= demandVertex.getTimeSlot(); ++slot) {
        final DecisionVertex decisionVertex =
            problem.getDecisionVertex(demandVertex.getType(), slot);
        verticesInSubResGraph.add(decisionVertex);
        verticesInSubResGraph.add(problem.getTimeSlotVertex(decisionVertex.getTimeSlot()));
      }
    }
    verticesInSubResGraph.add(problem.getSuperSink());
    return verticesInSubResGraph;
  }
}
