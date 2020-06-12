package de.asbestian.lotsizing.algorithm.cycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.asbestian.lotsizing.graph.Cycle;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.junit.jupiter.api.Test;

/** @author Sebastian Schenker */
class CycleFinderTest {

  @Test
  void computeCycles_consecutiveVertexIndices() {
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
    graph.addEdge(three, one);
    graph.addEdge(four, five);
    graph.addEdge(five, six);
    graph.addEdge(six, four);
    graph.addEdge(three, five);
    graph.addEdge(four, one);
    final BlockingQueue<Cycle> queue = new LinkedBlockingDeque<>(5);
    final CycleFinder cycleFinder = new CycleFinder(graph, queue);

    cycleFinder.run();

    assertEquals(4, queue.size()); // includes empty graph
  }

  @Test
  void computeCycles_nonConsecutiveVertexIndices() {
    final Graph<Vertex, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
    final Vertex one = new Vertex(1);
    final Vertex two = new Vertex(2);
    final Vertex four = new Vertex(4);
    final Vertex five = new Vertex(5);
    final Vertex six = new Vertex(6);
    final Vertex eight = new Vertex(8);
    graph.addVertex(one);
    graph.addVertex(two);
    graph.addVertex(four);
    graph.addVertex(five);
    graph.addVertex(six);
    graph.addVertex(eight);
    graph.addEdge(one, two);
    graph.addEdge(two, four);
    graph.addEdge(four, one);
    graph.addEdge(five, six);
    graph.addEdge(six, eight);
    graph.addEdge(eight, five);
    graph.addEdge(four, eight);
    graph.addEdge(five, one);
    final BlockingQueue<Cycle> queue = new LinkedBlockingDeque<>(5);
    final CycleFinder cycleFinder = new CycleFinder(graph, queue);

    cycleFinder.run();

    assertEquals(4, queue.size());
  }
}
