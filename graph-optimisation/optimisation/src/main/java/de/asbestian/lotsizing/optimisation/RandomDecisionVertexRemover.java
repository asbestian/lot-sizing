package de.asbestian.lotsizing.optimisation;

import de.asbestian.lotsizing.optimisation.vertex.Vertex;
import org.jgrapht.Graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Randomly removes at most percentage % of the decision variables.
 *
 * @author Sebastian Schenker
 */
public class RandomDecisionVertexRemover {

  private static final long SEED = 1;
  private static final Random rand = new Random(SEED);

  public static <E> Graph<Vertex, E> of(
      final Problem problem, final double percentage, final Graph<Vertex, E> resGraph) {
    final List<Vertex> decisionVertices = new ArrayList<>(problem.getDecisionVertices());
    Collections.shuffle(decisionVertices, rand);
    final var numberOfVars = decisionVertices.size();
    decisionVertices.stream()
        .limit(Double.valueOf(numberOfVars * percentage).longValue())
        .forEach(resGraph::removeVertex);
    return resGraph;
  }
}
