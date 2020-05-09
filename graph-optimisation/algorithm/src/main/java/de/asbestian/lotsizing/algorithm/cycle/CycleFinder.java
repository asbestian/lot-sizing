package de.asbestian.lotsizing.algorithm.cycle;

import de.asbestian.lotsizing.algorithm.scc.Tarjan;
import de.asbestian.lotsizing.graph.Cycle;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds all simple directed cycles
 *
 * @author Sebastian Schenker
 */
public class CycleFinder {
  private final Logger LOGGER = LoggerFactory.getLogger(CycleFinder.class);

  private final Graph<Vertex, DefaultEdge> graph;
  private final Set<Vertex> blocked;
  private final Map<Vertex, Set<Vertex>> blockedMap;
  private final ArrayDeque<Vertex> stack;
  private final Tarjan tarjan;

  public CycleFinder(final Graph<Vertex, DefaultEdge> graph) {
    this.graph = graph;
    this.blocked = new HashSet<>();
    this.blockedMap = new HashMap<>();
    this.stack = new ArrayDeque<>();
    this.tarjan = new Tarjan(graph);
  }

  public void computeCycles(final BlockingQueue<Cycle> queue) throws InterruptedException {
    clearState();
    int threshold = 0;
    while (threshold < graph.vertexSet().size()) { // ToDo proper vertex indices
      final Collection<Set<Vertex>> stronglyConnectedComponents = tarjan.computeSCCs(threshold);
      if (stronglyConnectedComponents.isEmpty()) {
        return;
      }
      final Pair<Graph<Vertex, DefaultEdge>, Vertex> result =
          getMinVertexSCC(stronglyConnectedComponents);
      final Graph<Vertex, DefaultEdge> leastSCC = result.getFirst();
      final Vertex leastVertex = result.getSecond();
      for (final DefaultEdge edge : leastSCC.outgoingEdgesOf(leastVertex)) {
        final Vertex vertex = leastSCC.getEdgeTarget(edge);
        blocked.remove(vertex);
        getBlockedVertices(vertex);
      }
      threshold = leastVertex.getId();
      findCyclesInSCC(threshold, leastVertex, leastSCC, queue);
      ++threshold;
    }
    queue.put(new de.asbestian.lotsizing.graph.Cycle(Collections.emptyList()));
  }

  private void clearState() {
    blocked.clear();
    blockedMap.clear();
    stack.clear();
  }

  private Pair<Graph<Vertex, DefaultEdge>, Vertex> getMinVertexSCC(
      final Collection<Set<Vertex>> scc) {
    int minIndex = Integer.MAX_VALUE;
    Set<Vertex> minComponent = null;
    Vertex minVertex = null;
    for (final Set<Vertex> component : scc) {
      final Vertex leastVertexInComponent =
          component.stream().min(Comparator.comparingInt(Vertex::getId)).orElseThrow();
      if (leastVertexInComponent.getId() < minIndex) {
        minIndex = leastVertexInComponent.getId();
        minComponent = component;
        minVertex = leastVertexInComponent;
      }
    }
    Objects.requireNonNull(minComponent);
    Objects.requireNonNull(minVertex);
    final AsSubgraph<Vertex, DefaultEdge> subgraph = new AsSubgraph<>(graph, minComponent);
    return Pair.of(subgraph, minVertex);
  }

  private Set<Vertex> getBlockedVertices(final Vertex vertex) {
    return blockedMap.computeIfAbsent(vertex, v -> new HashSet<>());
  }

  private boolean findCyclesInSCC(
      final int startIndex,
      final Vertex vertex,
      final Graph<Vertex, DefaultEdge> scc,
      final BlockingQueue<Cycle> queue)
      throws InterruptedException {
    boolean foundCycle = false;
    stack.push(vertex);
    blocked.add(vertex);
    for (final DefaultEdge edge : scc.outgoingEdgesOf(vertex)) {
      final Vertex target = scc.getEdgeTarget(edge);
      if (target.getId() == startIndex) { // cycle found
        final List<Vertex> vertices = new ArrayList<>(stack.size());
        stack.descendingIterator().forEachRemaining(vertices::add);
        foundCycle = true;
        queue.put(new Cycle(vertices));
      } else if (!blocked.contains(target)) {
        final boolean gotCycle = findCyclesInSCC(startIndex, target, scc, queue);
        foundCycle = foundCycle || gotCycle;
      }
    }
    if (foundCycle) {
      unblock(vertex);
    } else {
      scc.outgoingEdgesOf(vertex).stream()
          .map(scc::getEdgeTarget)
          .forEach(target -> getBlockedVertices(target).add(vertex));
    }
    stack.pop();
    return foundCycle;
  }

  private void unblock(final Vertex vertex) {
    blocked.remove(vertex);
    final Set<Vertex> blockedVertices = getBlockedVertices(vertex);
    while (!blockedVertices.isEmpty()) {
      final Vertex v = blockedVertices.iterator().next();
      blockedVertices.remove(v);
      if (blocked.contains(v)) {
        unblock(v);
      }
    }
  }
}
