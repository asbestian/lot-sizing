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
  void cyclicGraph_allCyclesFound() throws InterruptedException {
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

    final CycleFinder cycleFinder = new CycleFinder(graph);
    final BlockingQueue<Cycle> queue = new LinkedBlockingDeque<>(5);
    cycleFinder.computeCycles(queue);

    assertEquals(4, queue.size()); // includes empty graph
  }
}
