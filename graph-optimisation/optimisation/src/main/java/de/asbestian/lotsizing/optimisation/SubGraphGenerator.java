package de.asbestian.lotsizing.optimisation;

import de.asbestian.lotsizing.optimisation.vertex.Vertex;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.stream.Stream;

/** @author Sebastian Schenker */
public interface SubGraphGenerator {

  Stream<Graph<Vertex, DefaultEdge>> generate();
}
