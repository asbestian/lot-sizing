package de.asbestian.lotsizing.algorithm.scc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.asbestian.lotsizing.graph.vertex.SuperSink;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.junit.jupiter.api.Test;

class TarjanTest {

  @Test
  void emptyGraph_noSCCs() {
    final Graph<Vertex, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

    final Tarjan tarjan = new Tarjan(graph);
    assertTrue(tarjan.computeSCCs(Integer.MIN_VALUE).isEmpty());
  }

  @Test
  void directedAcyclicGraph_numberOfSCCsEqualsNumberOfVertices() {
    final Graph<Vertex, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
    final Vertex u = new Vertex(0);
    final Vertex v = new Vertex(1);
    final Vertex w = new Vertex(2);
    graph.addVertex(u);
    graph.addVertex(v);
    graph.addVertex(w);
    graph.addEdge(u, v);
    graph.addEdge(v, w);

    final Tarjan tarjan = new Tarjan(graph);
    final Collection<Set<Vertex>> scc = tarjan.computeSCCs(0);

    assertEquals(3, scc.size());
    assertTrue(scc.contains(Set.of(u)));
    assertTrue(scc.contains(Set.of(v)));
    assertTrue(scc.contains(Set.of(w)));
  }

  @Test
  void cyclicGraph_twoSCCs() {
    final Graph<Vertex, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
    final Vertex one = new Vertex(1);
    final Vertex two = new Vertex(2);
    final Vertex three = new Vertex(3);
    final Vertex four = new Vertex(4);
    final Vertex five = new Vertex(5);
    final Vertex six = new Vertex(6);
    graph.addVertex(one);
    graph.addVertex(two);
    graph.addVertex(three);
    graph.addVertex(four);
    graph.addVertex(five);
    graph.addVertex(six);
    graph.addEdge(one, two);
    graph.addEdge(two, three);
    graph.addEdge(three, four);
    graph.addEdge(three, one);
    graph.addEdge(four, five);
    graph.addEdge(five, six);
    graph.addEdge(six, four);

    final Tarjan tarjan = new Tarjan(graph);
    final Collection<Set<Vertex>> components = tarjan.computeSCCs(0);

    assertEquals(2, components.size());
    assertThat(components, hasItem(containsInAnyOrder(one, two, three)));
    assertThat(components, hasItem(containsInAnyOrder(four, five, six)));
  }

  @Test
  void vertexThreshold_threeVertexSCCRemaining() {
    final Graph<Vertex, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
    final Vertex one = new Vertex(1);
    final Vertex two = new Vertex(2);
    final Vertex three = new Vertex(3);
    final Vertex four = new Vertex(4);
    final Vertex five = new Vertex(5);
    graph.addVertex(one);
    graph.addVertex(two);
    graph.addVertex(three);
    graph.addVertex(four);
    graph.addVertex(five);
    graph.addEdge(one, two);
    graph.addEdge(two, three);
    graph.addEdge(three, four);
    graph.addEdge(four, five);
    graph.addEdge(five, three);

    final Tarjan tarjan = new Tarjan(graph);
    final Collection<Set<Vertex>> components = tarjan.computeSCCs(3);

    assertEquals(1, components.size());
    assertEquals(3, components.iterator().next().size());
    assertThat(components, hasItem(containsInAnyOrder(three, four, five)));
  }

  @Test
  void initialResGraphOfFiveTimeSlotsAnd2TypesInstance_superSinkInComponentOfSizeTwelve() {
    final Graph<Vertex, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
    final List<Vertex> vertices = new ArrayList<>(20);
    vertices.add(0, new SuperSink(0));
    IntStream.rangeClosed(1, 19).forEach(i -> vertices.add(i, new Vertex(i)));
    vertices.forEach(graph::addVertex);
    graph.addEdge(vertices.get(1), vertices.get(5));
    graph.addEdge(vertices.get(6), vertices.get(1));
    graph.addEdge(vertices.get(2), vertices.get(5));
    graph.addEdge(vertices.get(2), vertices.get(6));
    graph.addEdge(vertices.get(2), vertices.get(7));
    graph.addEdge(vertices.get(8), vertices.get(2));
    graph.addEdge(vertices.get(2), vertices.get(9));
    graph.addEdge(vertices.get(10), vertices.get(3));
    graph.addEdge(vertices.get(4), vertices.get(10));
    graph.addEdge(vertices.get(4), vertices.get(11));
    graph.addEdge(vertices.get(12), vertices.get(4));
    graph.addEdge(vertices.get(4), vertices.get(13));
    graph.addEdge(vertices.get(4), vertices.get(14));
    graph.addEdge(vertices.get(15), vertices.get(10));
    graph.addEdge(vertices.get(5), vertices.get(15));
    graph.addEdge(vertices.get(11), vertices.get(16));
    graph.addEdge(vertices.get(16), vertices.get(6));
    graph.addEdge(vertices.get(17), vertices.get(12));
    graph.addEdge(vertices.get(7), vertices.get(17));
    graph.addEdge(vertices.get(13), vertices.get(18));
    graph.addEdge(vertices.get(18), vertices.get(8));
    graph.addEdge(vertices.get(14), vertices.get(19));
    graph.addEdge(vertices.get(9), vertices.get(19));
    graph.addEdge(vertices.get(0), vertices.get(15));
    graph.addEdge(vertices.get(0), vertices.get(16));
    graph.addEdge(vertices.get(0), vertices.get(17));
    graph.addEdge(vertices.get(0), vertices.get(18));
    graph.addEdge(vertices.get(19), vertices.get(0));

    final Tarjan tarjan = new Tarjan(graph);
    final Collection<Set<Vertex>> scc = tarjan.computeSCCs(0);

    assertEquals(9, scc.size());
    assertEquals(8, scc.stream().filter(component -> component.size() == 1).count());
  }
}
