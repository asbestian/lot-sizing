package de.asbestian.lotsizing.algorithm.scc;

import de.asbestian.lotsizing.graph.vertex.Vertex;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

/** Computes strongly connected components based on Tarjan's algorithm. */
public class Tarjan implements StronglyConnectedComponentFinder {

  private final Graph<Vertex, DefaultEdge> graph;
  private final Map<Vertex, Integer> smallestReachableDfsIndex;
  private final Map<Vertex, Integer> dfsIndex;
  private final Map<Vertex, Boolean> isVertexOnStack;
  private final Deque<Vertex> stack;
  private final Map<Vertex, Set<Vertex>> stronglyConnectedComponent;

  /** Constructor. */
  public Tarjan(final Graph<Vertex, DefaultEdge> graph) {
    this.graph = graph;
    smallestReachableDfsIndex = new HashMap<>();
    dfsIndex = new HashMap<>();
    isVertexOnStack = new HashMap<>();
    stack = new ArrayDeque<>();
    stronglyConnectedComponent = new HashMap<>();
  }

  @Override
  public Collection<Set<Vertex>> computeSCCs(final int threshold) {
    clearState();
    graph.vertexSet().stream()
        .filter(v -> v.getId() >= threshold)
        .filter(v -> !dfsIndex.containsKey(v))
        .forEach(v -> findSCC(v, threshold));
    return Collections.unmodifiableCollection(stronglyConnectedComponent.values());
  }

  private void clearState() {
    smallestReachableDfsIndex.clear();
    dfsIndex.clear();
    isVertexOnStack.clear();
    stack.clear();
    stronglyConnectedComponent.clear();
  }

  private void findSCC(final Vertex u, final int threshold) {
    final int index = dfsIndex.size();
    dfsIndex.put(u, index);
    smallestReachableDfsIndex.put(u, index);
    stack.push(u);
    isVertexOnStack.put(u, true);

    for (final DefaultEdge edge : graph.outgoingEdgesOf(u)) {
      final Vertex v = graph.getEdgeTarget(edge);
      if (v.getId() < threshold) {
        continue;
      } else if (!dfsIndex.containsKey(v)) {
        findSCC(v, threshold);
        final int minIndex = Math.min(index, smallestReachableDfsIndex.get(v));
        smallestReachableDfsIndex.put(u, minIndex);
      } else if (isVertexOnStack.get(v)) {
        // If v is on the stack already, (u, v) is a back-edge in the DFS tree and therefore v is
        // not in the subtree of u. Because smallestReachableDfsIndex considers only vertices
        // reachable via the subtree of u we must stop at v and use dfSIndex(v) instead of
        // smallestReachableDfsIndex(v).
        final int minIndex = Math.min(index, dfsIndex.get(v));
        smallestReachableDfsIndex.put(u, minIndex);
      }
    }
    // If u is a root node, pop the stack and generate an SCC.
    if (dfsIndex.get(u).equals(smallestReachableDfsIndex.get(u))) {
      final Set<Vertex> scc = new HashSet<>();
      Vertex vertex;
      do {
        vertex = stack.pop();
        isVertexOnStack.put(vertex, false);
        scc.add(vertex);
      } while (!vertex.equals(u));
      stronglyConnectedComponent.put(u, scc);
    }
  }
}
