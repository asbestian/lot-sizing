package de.asbestian.productionproblem.optimisation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.jgrapht.Graph;

/**
 * Randomly removes at most percentage % of the decision variables.
 *
 * @author Sebastian Schenker
 */
public class RandomDecisionVertexRemover {

  private final static long SEED = 1;
  private final static Random rand = new Random(SEED);

  public static <E> Graph<Vertex, E> of(
      final Problem problem,
      double percentage,
      final Graph<Vertex, E> resGraph) {
    final List<DecisionVertex> decisionVertices = new ArrayList<>(problem.getDecisionVertices());
    Collections.shuffle(decisionVertices, rand);
    final var numberOfVars = decisionVertices.size();
    decisionVertices.stream().limit(Double.valueOf(numberOfVars * percentage).longValue())
        .forEach(resGraph::removeVertex);
    return resGraph;
  }
}
