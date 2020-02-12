package de.asbestian.productionproblem.optimisation;

import de.asbestian.productionproblem.input.Input;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.JohnsonSimpleCycles;
import org.jgrapht.alg.flow.PushRelabelMFImpl;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;

/**
 * Graph representing the production problem based on the file input.
 *
 * <p>The constructed graph has 4 vertex layers (including an additional superSink). Assume we have
 * m different item types and n time slots. Then the first vertex layer consists of _demand
 * vertices_ which correspond to the items that need to be produced. E.g., if an item of type i
 * needs to be produced before time slot j, then there is a corresponding demand vertex. The number
 * of demand vertices corresponds to the number of items that need to be produced.
 *
 * <p>The second vertex layer consists of _decision vertices_ which corresponds to the binary
 * decision variables x^i_j in the integer formulation where x^i_j \in {0,1} and x^i_j = 1 if and
 * only if item i is produces in time slot j. The number of decision vertices is given by the
 * product of m and n.
 *
 * @author Sebastian Schenker
 */
public class Problem {

  private final Input input;
  private final IdSupplier idSupplier;
  private final Graph<Vertex, DefaultEdge> graph;
  private final SuperSink superSink;
  private final DemandVertex[] demandVertices;
  private final Map<Pair<Integer, Integer>, DecisionVertex> decisionVertices;
  private final TimeSlotVertex[] timeSlotVertices;
  private final Collection<DefaultEdge> edgesBetweenDemandAndDecisionVertices;

  public Problem(final Input input) {
    this.input = input;
    this.idSupplier = new IdSupplier();
    this.graph = buildEmptyGraph();
    this.superSink = new SuperSink(idSupplier.get());
    this.graph.addVertex(this.superSink);
    this.demandVertices = new DemandVertex[input.getNumProducedItems()];
    this.decisionVertices = new HashMap<>();
    this.timeSlotVertices = new TimeSlotVertex[input.getNumTimeSlots()];
    this.edgesBetweenDemandAndDecisionVertices = new ArrayList<>();
  }

  public static Graph<Vertex, DefaultEdge> buildEmptyGraph() {
    return GraphTypeBuilder.<Vertex, DefaultEdge>directed()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false)
        .edgeClass(DefaultEdge.class)
        .weighted(true)
        .buildGraph();
  }

  public void build() {
    addVertices();
    addEdges();
  }

  public Collection<DecisionVertex> getDecisionVertices() {
    return Collections.unmodifiableCollection(decisionVertices.values());
  }

  public Graph<Vertex, DefaultEdge> getResidualGraph(final Schedule schedule) {
    final Graph<Vertex, DefaultEdge> resGraph = buildEmptyGraph();
    graph.vertexSet().forEach(resGraph::addVertex); // add graph vertices to residual graph
    for (final var edge : graph.edgeSet()) { // add edges with positive residual capacity
      final Vertex source = graph.getEdgeSource(edge);
      final Vertex target = graph.getEdgeTarget(edge);
      if (schedule.containsEdge(source, target)) { // => reverse edge has positive residual capacity
        resGraph.addEdge(target, source);
      } else { // => original has positive residual capacity
        resGraph.addEdge(source, target);
      }
    }
    return resGraph;
  }

  /**
   * Computes a schedule based on the maximum flow of the graph.
   */
  public Schedule computeInitialSchedule() {
    // add super source and connect it to demand vertices
    final var superSource = new Vertex(idSupplier.get());
    graph.addVertex(superSource);
    for (final var demandVertex : demandVertices) {
      graph.addEdge(superSource, demandVertex);
    }
    // connect super sink to super source and set corresponding capacity to infinity
    final var sinkSourceEdge = graph.addEdge(superSink, superSource);
    graph.setEdgeWeight(sinkSourceEdge, Double.POSITIVE_INFINITY);
    // compute max flow from super source to super sink
    final var maxFlowFinder = new PushRelabelMFImpl<>(graph);
    final var maxFlow = maxFlowFinder.getMaximumFlow(superSource, superSink);
    if (maxFlow.getValue() != demandVertices.length) {
      throw new GraphException(
          "Computed max flow value: " + maxFlow.getValue() + "; Expected max flow value: "
              + demandVertices.length);
    }
    // remove super source and corresponding edges
    graph.removeVertex(superSource);
    return new Schedule(input, graph, maxFlow.getFlowMap());
  }

  public static <E> List<Cycle> computeCycles(final Graph<Vertex, E> graph) {
    final var cycleFinder = new JohnsonSimpleCycles<>(graph);
    final List<List<Vertex>> cycles = cycleFinder.findSimpleCycles();
    return cycles.stream()
        .map(vertices -> new Cycle(vertices, graph))
        .collect(Collectors.toList());
  }

  private void addDemandVertices() {
    int index = 0;
    for (int type = 0; type < input.getNumTypes(); ++type) {
      final var demand = input.getDemand(type);
      for (int slot = 0; slot < input.getNumTimeSlots(); ++slot) {
        if (demand.get(slot) == 1) {
          final var demandVertex = new DemandVertex(idSupplier.get(), type, slot);
          demandVertices[index++] = demandVertex;
          graph.addVertex(demandVertex);
        }
      }
    }
    assert index == input.getNumProducedItems();
  }

  private void addDecisionVertices() {
    for (int type = 0; type < input.getNumTypes(); ++type) {
      for (int slot = 0; slot < input.getNumTimeSlots(); ++slot) {
        final var decisionVertex = new DecisionVertex(idSupplier.get(), type, slot);
        decisionVertices.put(Pair.of(type, slot), decisionVertex);
        graph.addVertex(decisionVertex);
      }
    }
  }

  private void addTimeSlotVertices() {
    for (int slot = 0; slot < input.getNumTimeSlots(); ++slot) {
      final var timeSlotVertex = new TimeSlotVertex(idSupplier.get(), slot);
      timeSlotVertices[slot] = timeSlotVertex;
      graph.addVertex(timeSlotVertex);
    }
  }

  private void addVertices() {
    addDemandVertices();
    addDecisionVertices();
    addTimeSlotVertices();
  }

  private void addEdgesFromDemandVerticesToDecisionVertices() {
    for (final var demandVertex : demandVertices) {
      final var type = demandVertex.getType();
      final var timeSlot = demandVertex.getTimeSlot();
      IntStream.rangeClosed(0, timeSlot)
          .forEach(
              slot -> {
                final var edge =
                    graph.addEdge(demandVertex, decisionVertices.get(Pair.of(type, slot)));
                edgesBetweenDemandAndDecisionVertices.add(edge);
              });
    }
  }

  private void addEdgesFromDecisionVerticesToTimeSlotVertices() {
    decisionVertices
        .values()
        .forEach(
            vertex -> {
              final var timeSlot = vertex.getTimeSlot();
              graph.addEdge(vertex, timeSlotVertices[timeSlot]);
            });
  }

  private void addEdgesFromTimeSlotVerticesToSuperSink() {
    for (final var timeSlotVertex : timeSlotVertices) {
      graph.addEdge(timeSlotVertex, superSink);
    }
  }

  private void addEdges() {
    addEdgesFromDemandVerticesToDecisionVertices();
    addEdgesFromDecisionVerticesToTimeSlotVertices();
    addEdgesFromTimeSlotVerticesToSuperSink();
  }
}
