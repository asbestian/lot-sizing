package de.asbestian.lotsizing.algorithm;

import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sebastian Schenker */
public class LocalSearchImpl extends LocalSearch {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalSearchImpl.class);
  private static final int SEED = 1;
  private final Random random;
  private final int neighbourhoodSize;

  public LocalSearchImpl(final Input input, final Problem problem, final int neighbourhoodSize) {
    super(input, problem);
    this.random = new Random(SEED);
    this.neighbourhoodSize = neighbourhoodSize;
  }

  @Override
  protected Graph<Vertex, DefaultEdge> createSubResidualGraph(
      final boolean newResGraph, Graph<Vertex, DefaultEdge> resGraph) {
    final Set<Vertex> neighbourhood = getNeighbourhood();
    final Graph<Vertex, DefaultEdge> subResGraph = new AsSubgraph<>(resGraph);
    problem.getDemandVertices().stream()
        .filter(v -> !neighbourhood.contains(v))
        .forEach(subResGraph::removeVertex);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("SubresidualGraph - number of edges: {}", subResGraph.edgeSet().size());
    }
    return subResGraph;
  }

  private Set<Vertex> getNeighbourhood() {
    return random
        .ints(neighbourhoodSize, 0, problem.getDemandVertices().size())
        .mapToObj(index -> problem.getDemandVertices().get(index))
        .collect(Collectors.toSet());
  }
}
