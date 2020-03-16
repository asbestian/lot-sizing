package de.asbestian.productionproblem.optimisation;

import java.util.stream.Stream;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

/**
 * @author Sebastian Schenker
 */
public interface SubGraphGenerator {

  Stream<Graph<Vertex, DefaultEdge>> generate();

}
