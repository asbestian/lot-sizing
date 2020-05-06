package de.asbestian.lotsizing.graph;

import de.asbestian.lotsizing.input.Input;
import de.asbestian.lotsizing.graph.vertex.*;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.JohnsonSimpleCycles;
import org.jgrapht.alg.flow.PushRelabelMFImpl;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Graph representing the production problem based on the file input.
 *
 * <p>The constructed graph has 3 vertex layers (excluding the additional superSink). Assume we have
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

  private static class IdSupplier implements Supplier<Integer> {

    private int id = 0;

    @Override
    public Integer get() {
      return id++;
    }
  }

  private final Logger LOGGER = LoggerFactory.getLogger(Problem.class);
  private final Input input;
  private final IdSupplier idSupplier;
  private final Graph<Vertex, DefaultEdge> graph;
  private final SuperSink superSink;
  private final DemandVertex[] demandVertices;
  private final Map<Pair<Integer, Integer>, DecisionVertex> decisionVertices;
  private final TimeSlotVertex[] timeSlotVertices;

  public Problem(final Input input) {
    this.input = input;
    this.idSupplier = new IdSupplier();
    this.graph = buildEmptyGraph();
    this.superSink = new SuperSink(idSupplier.get());
    this.graph.addVertex(this.superSink);
    this.demandVertices = new DemandVertex[input.getNumProducedItems()];
    this.decisionVertices = new HashMap<>();
    this.timeSlotVertices = new TimeSlotVertex[input.getNumTimeSlots()];
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

  public int getNumberOfEdges() {
    return graph.edgeSet().size();
  }

  /** Returns unmodifiable list of demand vertices in ascending Id order */
  public List<Vertex> getDemandVertices() {
    return List.of(demandVertices);
  }

  /** Returns unmodifiable list of decision vertices in ascending Id order */
  public List<Vertex> getDecisionVertices() {
    return decisionVertices.values().stream()
        .sorted(Comparator.comparingInt(Vertex::getId))
        .collect(Collectors.toUnmodifiableList());
  }

  /** Returns unmodifiable list of time slot vertices in ascending Id order */
  public List<Vertex> getTimeSlotVertices() {
    return List.of(timeSlotVertices);
  }

  public SuperSink getSuperSink() {
    return superSink;
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

  /** Computes a schedule based on the maximum flow of the graph. */
  public Schedule computeInitialSchedule() {
    final int originalNumberOfEdges = graph.edgeSet().size();
    final int originalNumberOfVertices = graph.vertexSet().size();
    // add super source and connect it to demand vertices
    final Vertex superSource = new Vertex(idSupplier.get());
    graph.addVertex(superSource);
    for (final DemandVertex demandVertex : demandVertices) {
      graph.addEdge(superSource, demandVertex);
    }
    // connect super sink to super source and set corresponding capacity to infinity
    final DefaultEdge sinkSourceEdge = graph.addEdge(superSink, superSource);
    graph.setEdgeWeight(sinkSourceEdge, Double.POSITIVE_INFINITY);
    // compute max flow from super source to super sink
    final var maxFlowFinder = new PushRelabelMFImpl<>(graph);
    final var maxFlow = maxFlowFinder.getMaximumFlow(superSource, superSink);
    if (maxFlow.getValue() != demandVertices.length) {
      throw new OptimisationException(
          "Computed max flow value: "
              + maxFlow.getValue()
              + "; Expected max flow value: "
              + demandVertices.length);
    }
    // remove super source and corresponding edges
    graph.removeVertex(superSource);
    assert graph.edgeSet().size() == originalNumberOfEdges;
    assert graph.vertexSet().size() == originalNumberOfVertices;
    final Collection<Pair<Vertex, Vertex>> usedEdges =
        graph.edgeSet().stream()
            .filter(edge -> maxFlow.getFlow(edge) > 0.)
            .map(edge -> Pair.of(graph.getEdgeSource(edge), graph.getEdgeTarget(edge)))
            .collect(Collectors.toUnmodifiableList());
    assert usedEdges.size() == 3 * demandVertices.length;
    return new Schedule(input, usedEdges);
  }

  public static <E> List<Cycle> computeCycles(final Graph<Vertex, E> graph) {
    final var cycleFinder = new JohnsonSimpleCycles<>(graph);
    final List<List<Vertex>> cycles = cycleFinder.findSimpleCycles();
    return cycles.stream().map(vertices -> new Cycle(vertices, graph)).collect(Collectors.toList());
  }

  private void addDemandVertices() {
    int counter = 0;
    for (int type = 0; type < input.getNumTypes(); ++type) {
      final var demand = input.getDemand(type);
      for (int slot = 0; slot < input.getNumTimeSlots(); ++slot) {
        if (demand.get(slot) == 1) {
          final var demandVertex = new DemandVertex(idSupplier.get(), type, slot);
          demandVertices[counter++] = demandVertex;
          graph.addVertex(demandVertex);
        }
      }
    }
    LOGGER.info("Number of added demand vertices: {}", counter);
  }

  private void addDecisionVertices() {
    for (int type = 0; type < input.getNumTypes(); ++type) {
      for (int slot = 0; slot < input.getNumTimeSlots(); ++slot) {
        final var decisionVertex = new DecisionVertex(idSupplier.get(), type, slot);
        decisionVertices.put(Pair.of(type, slot), decisionVertex);
        graph.addVertex(decisionVertex);
      }
    }
    LOGGER.info("Number of added decision vertices: {}", decisionVertices.size());
  }

  private void addTimeSlotVertices() {
    for (int slot = 0; slot < input.getNumTimeSlots(); ++slot) {
      final var timeSlotVertex = new TimeSlotVertex(idSupplier.get(), slot);
      timeSlotVertices[slot] = timeSlotVertex;
      graph.addVertex(timeSlotVertex);
    }
    LOGGER.info("Number of added time slot vertices: {}", timeSlotVertices.length);
  }

  private void addVertices() {
    addDemandVertices();
    addDecisionVertices();
    addTimeSlotVertices();
  }

  private void addEdgesFromDemandVerticesToDecisionVertices() {
    for (final DemandVertex demandVertex : demandVertices) {
      final int type = demandVertex.getType();
      final int timeSlot = demandVertex.getTimeSlot();
      IntStream.rangeClosed(0, timeSlot)
          .forEach(slot -> graph.addEdge(demandVertex, decisionVertices.get(Pair.of(type, slot))));
    }
  }

  // Adds edges between decision vertices and time slot vertices. Only decision vertices with
  // positive incoming edge degree are considered.
  private void addEdgesFromDecisionVerticesToTimeSlotVertices() {
    decisionVertices.values().stream()
        .filter(vertex -> graph.inDegreeOf(vertex) > 0)
        .forEach(vertex -> graph.addEdge(vertex, timeSlotVertices[vertex.getTimeSlot()]));
  }

  // Adds edges between time slot vertices and super sink. Only time slot vertices with positive
  // incoming edge degree are considered.
  private void addEdgesFromTimeSlotVerticesToSuperSink() {
    Arrays.stream(timeSlotVertices)
        .filter(vertex -> graph.inDegreeOf(vertex) > 0)
        .forEach(vertex -> graph.addEdge(vertex, superSink));
  }

  private void addEdges() {
    // CAUTION: order of below function call is important as edges from decision vertices to time
    // slow vertices are only added if the considered decision vertex has positive in-degree.
    // Equivalently, for edges between time slot vertices and super sink.
    addEdgesFromDemandVerticesToDecisionVertices();
    addEdgesFromDecisionVerticesToTimeSlotVertices();
    addEdgesFromTimeSlotVerticesToSuperSink();
  }
}
