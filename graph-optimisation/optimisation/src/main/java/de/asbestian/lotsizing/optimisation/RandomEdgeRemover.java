package de.asbestian.lotsizing.optimisation;

import de.asbestian.lotsizing.optimisation.vertex.Vertex;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.paukov.combinatorics3.Generator;

import java.util.Collection;
import java.util.stream.Stream;

/** @author Sebastian Schenker */
public class RandomEdgeRemover implements SubGraphGenerator {

  private final int edgeThreshold;
  private final Graph<Vertex, DefaultEdge> graph;

  /** @param edgeThreshold the number of edges that the graph should keep */
  public RandomEdgeRemover(final int edgeThreshold, final Graph<Vertex, DefaultEdge> graph) {
    this.edgeThreshold = Math.min(edgeThreshold, graph.edgeSet().size());
    this.graph = graph;
  }

  private Graph<Vertex, DefaultEdge> computeSubGraph(final Collection<DefaultEdge> edges) {
    final Graph<Vertex, DefaultEdge> subGraph = Problem.buildEmptyGraph();
    graph.vertexSet().forEach(subGraph::addVertex);
    edges.forEach(edge -> subGraph.addEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(edge)));
    return subGraph;
  }

  @Override
  public Stream<Graph<Vertex, DefaultEdge>> generate() {
    return Generator.combination(graph.edgeSet()).simple(edgeThreshold).stream()
        .map(this::computeSubGraph);
  }
}
