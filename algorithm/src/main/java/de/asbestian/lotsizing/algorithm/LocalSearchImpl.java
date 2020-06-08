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
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sebastian Schenker */
public class LocalSearchImpl extends LocalSearch {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalSearchImpl.class);
  private static final int SEED = 1;
  private final Random random;
  private final int neighbourhoodSize;
  private List<DemandVertex> demandVertices;
  private int counter;

  public LocalSearchImpl(
      final Input input, final Problem problem, final int initialNeighbourhoodSize) {
    super(input, problem);
    this.random = new Random(SEED);
    this.neighbourhoodSize = initialNeighbourhoodSize;
    this.demandVertices = new ArrayList<>(problem.getDemandVertices());
    this.counter = 0;
  }

  @Override
  protected Graph<Vertex, DefaultEdge> createSubResidualGraph(
      final boolean newResGraph, Graph<Vertex, DefaultEdge> resGraph) {
    final Set<Vertex> neighbourhood = getVerticesInSubResGraph(newResGraph);

    /*final Graph<Vertex, DefaultEdge> subResGraph = new AsSubgraph<>(resGraph);
    problem.getDemandVertices().stream()
        .filter(v -> !neighbourhood.contains(v))
        .forEach(subResGraph::removeVertex);
    problem.getDecisionVertices().stream()
        .filter(v -> !neighbourhood.contains(v))
        .forEach(subResGraph::removeVertex);
    problem.getTimeSlotVertices().stream()
        .filter(v -> !neighbourhood.contains(v))
        .forEach(subResGraph::removeVertex);*/

    return buildSubResGraph(neighbourhood, resGraph);
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
    if (counter >= problem.getDemandVertices().size() / neighbourhoodSize) {
      LOGGER.debug("Shuffling demand vertex partition.");
      shuffleRandomDemandVertexPartition();
      counter = 0;
    }
    int begin =
        random.nextInt(
            (int) Math.max(1, Math.floor((double) demandVertices.size() - neighbourhoodSize)));
    LOGGER.debug("Demand sublist: [{}, {}]", begin, begin + neighbourhoodSize);
    final Set<Vertex> neighbourhood =
        new HashSet<>(demandVertices.subList(begin, begin + neighbourhoodSize));
    for (final DemandVertex demandVertex :
        demandVertices.subList(begin, begin + neighbourhoodSize)) {
      for (int slot = 0; slot <= demandVertex.getTimeSlot(); ++slot) {
        final DecisionVertex decisionVertex =
            problem.getDecisionVertex(demandVertex.getType(), slot);
        neighbourhood.add(decisionVertex);
        neighbourhood.add(problem.getTimeSlotVertex(decisionVertex.getTimeSlot()));
      }
    }
    neighbourhood.add(problem.getSuperSink());
    return neighbourhood;
  }

  private void shuffleRandomDemandVertexPartition() {
    /*int begin =
        random.nextInt(
            (int) Math.max(1, Math.floor((double) demandVertices.size() - neighbourhoodSize)));
    Collections.shuffle(demandVertices.subList(begin, demandVertices.size()), random);*/
    Collections.shuffle(demandVertices);
  }
}
