package de.asbestian.lotsizing.algorithm;

import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.graph.vertex.DecisionVertex;
import de.asbestian.lotsizing.graph.vertex.DemandVertex;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sebastian Schenker */
public class LocalSearchImpl {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalSearchImpl.class);
  private static final int SEED = 1;
  private final Random random;
  private final int subResGraphVertexSize;
  private final Input input;
  private final Problem problem;
  private IntList indices;
  private IntListIterator iter;
  private List<DemandVertex> demand;

  public LocalSearchImpl(
      final Input input,
      final Problem problem,
      final int subResGraphVertexSize,
      final boolean useGreatestDescent) {
    this.random = new Random(SEED);
    this.subResGraphVertexSize = subResGraphVertexSize;
    this.input = input;
    this.problem = problem;
    indices = createShuffledIndices();
    demand = null;
    iter = null;
  }

  protected Graph<Vertex, DefaultEdge> createSubResidualGraph(
      final boolean newResGraph, Graph<Vertex, DefaultEdge> resGraph, final Schedule schedule) {
    if (subResGraphVertexSize >= input.getNumProducedItems()) {
      return resGraph;
    } else if (newResGraph || Objects.isNull(demand)) {
      demand = schedule.getNonIdleProduction();
      iter = indices.iterator();
    }
    final Set<Vertex> subResGraphVertices = computeVerticesInSubResGraph();
    return new AsSubgraph<>(resGraph, subResGraphVertices);
  }

  private Set<Vertex> computeVerticesInSubResGraph() {
    if (!iter.hasNext()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Shuffling.");
      }
      shuffleDemand();
      iter = indices.iterator();
    }
    int index = iter.nextInt();
    final Collection<DemandVertex> demandVerticesInSubResGraph =
        demand.subList(index, index + subResGraphVertexSize);
    final Set<Vertex> verticesInSubResGraph = new HashSet<>(demandVerticesInSubResGraph);
    for (final DemandVertex demandVertex : demandVerticesInSubResGraph) {
      for (int slot = 0; slot <= demandVertex.getTimeSlot(); ++slot) {
        final DecisionVertex decisionVertex =
            problem.getDecisionVertex(demandVertex.getType(), slot);
        verticesInSubResGraph.add(decisionVertex);
        verticesInSubResGraph.add(problem.getTimeSlotVertex(decisionVertex.getTimeSlot()));
      }
    }
    verticesInSubResGraph.add(problem.getSuperSink());
    return verticesInSubResGraph;
  }

  private void shuffleDemand() {
    int begin = 0, end;
    do {
      end = Math.min(begin + subResGraphVertexSize, demand.size());
      Collections.shuffle(demand.subList(begin, end));
      begin += subResGraphVertexSize;
    } while (begin < demand.size());
  }

  private IntList createShuffledIndices() {
    final IntList indices =
        IntStream.range(0, input.getNumProducedItems() - subResGraphVertexSize)
            .collect(IntArrayList::new, IntArrayList::add, IntArrayList::addAll);
    Collections.shuffle(indices, random);
    return indices;
  }
}
