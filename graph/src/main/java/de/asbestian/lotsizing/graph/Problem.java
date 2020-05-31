package de.asbestian.lotsizing.graph;

import de.asbestian.lotsizing.graph.vertex.DecisionVertex;
import de.asbestian.lotsizing.graph.vertex.DemandVertex;
import de.asbestian.lotsizing.graph.vertex.SuperSink;
import de.asbestian.lotsizing.graph.vertex.TimeSlotVertex;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.graph.vertex.Vertex.Type;
import de.asbestian.lotsizing.input.Input;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jgrapht.Graph;
import org.jgrapht.alg.flow.PushRelabelMFImpl;
import org.jgrapht.alg.flow.mincost.CapacityScalingMinimumCostFlow;
import org.jgrapht.alg.flow.mincost.MinimumCostFlowProblem;
import org.jgrapht.alg.interfaces.MinimumCostFlowAlgorithm;
import org.jgrapht.alg.interfaces.MinimumCostFlowAlgorithm.MinimumCostFlow;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.AsWeightedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOGGER = LoggerFactory.getLogger(Problem.class);
  private final Input input;
  private final IdSupplier idSupplier;
  private final SimpleDirectedGraph<Vertex, DefaultEdge> graph;
  private final SuperSink superSink;
  private final DemandVertex[] demandVertices;
  private final Map<Pair<Integer, Integer>, DecisionVertex> decisionVertices;
  private final TimeSlotVertex[] timeSlotVertices;

  public Problem(final Input input) {
    this.input = input;
    this.idSupplier = new IdSupplier();
    this.graph = new SimpleDirectedGraph<>(DefaultEdge.class);
    this.superSink = new SuperSink(idSupplier.get());
    this.graph.addVertex(this.superSink);
    this.demandVertices = new DemandVertex[input.getNumProducedItems()];
    this.decisionVertices = new HashMap<>();
    this.timeSlotVertices = new TimeSlotVertex[input.getNumTimeSlots()];
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
    final Graph<Vertex, DefaultEdge> resGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
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

  /** Computes a schedule with minimal inventory cost. */
  public Schedule computeOptimalInventoryCostSchedule() {
    final Function<DefaultEdge, Double> edgeWeights =
        edge -> {
          final boolean sourceIsDemandVertex =
              graph.getEdgeSource(edge).getVertexType() == Type.DEMAND_VERTEX;
          final boolean targetIsDecisionVertex =
              graph.getEdgeTarget(edge).getVertexType() == Type.DECISION_VERTEX;
          if (sourceIsDemandVertex && targetIsDecisionVertex) {
            final DemandVertex source = (DemandVertex) graph.getEdgeSource(edge);
            final DecisionVertex target = (DecisionVertex) graph.getEdgeTarget(edge);
            return (double) input.getInventoryCost()
                * (source.getTimeSlot() - target.getTimeSlot());
          }
          return 1.;
        };
    final var weightedGraph = new AsWeightedGraph<>(graph, edgeWeights, false, false);
    final Map<Vertex, Integer> supplies = new HashMap<>();
    for (final DemandVertex demandVertex : demandVertices) {
      supplies.put(demandVertex, 1);
    }
    supplies.put(superSink, -demandVertices.length);
    final Function<DefaultEdge, Integer> upperArcCapacities = e -> 1;
    final MinimumCostFlowProblem<Vertex, DefaultEdge> minCostProb =
        new MinimumCostFlowProblem.MinimumCostFlowProblemImpl<>(
            weightedGraph, vertex -> supplies.getOrDefault(vertex, 0), upperArcCapacities);
    final MinimumCostFlowAlgorithm<Vertex, DefaultEdge> minCostAlgo =
        new CapacityScalingMinimumCostFlow<>();
    final MinimumCostFlow<DefaultEdge> minCostFlow = minCostAlgo.getMinimumCostFlow(minCostProb);
    final Collection<Pair<Vertex, Vertex>> usedEdges =
        minCostFlow.getFlowMap().keySet().stream()
            .filter(edge -> minCostFlow.getFlow(edge) > 0.)
            .map(edge -> Pair.of(graph.getEdgeSource(edge), graph.getEdgeTarget(edge)))
            .collect(Collectors.toUnmodifiableList());
    assert usedEdges.size() == 3 * demandVertices.length;
    return new Schedule(input, usedEdges);
  }

  /** Computes a random schedule based on a maximum flow computation. */
  public Schedule computeRandomSchedule() {
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
    final Map<DefaultEdge, Double> edgeWeights =
        Collections.singletonMap(sinkSourceEdge, Double.POSITIVE_INFINITY);

    final var weightedGraph =
        new AsWeightedGraph<>(graph, edge -> edgeWeights.getOrDefault(edge, 1.), false, false);
    // compute max flow from super source to super sink
    final var maxFlowFinder = new PushRelabelMFImpl<>(weightedGraph);
    final var maxFlow = maxFlowFinder.getMaximumFlow(superSource, superSink);
    // remove super source and corresponding edges
    graph.removeVertex(superSource);
    assert graph.edgeSet().size() == originalNumberOfEdges;
    assert graph.vertexSet().size() == originalNumberOfVertices;

    if (maxFlow.getValue() != demandVertices.length) {
      throw new OptimisationException(
          "Computed max flow value: "
              + maxFlow.getValue()
              + "; Expected: "
              + demandVertices.length);
    }
    final Collection<Pair<Vertex, Vertex>> usedEdges =
        graph.edgeSet().stream()
            .filter(edge -> maxFlow.getFlow(edge) > 0.)
            .map(edge -> Pair.of(graph.getEdgeSource(edge), graph.getEdgeTarget(edge)))
            .collect(Collectors.toUnmodifiableList());
    assert usedEdges.size() == 3 * demandVertices.length;
    return new Schedule(input, usedEdges);
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
    LOGGER.debug("Number of added demand vertices: {}", counter);
  }

  private void addDecisionVertices() {
    for (int type = 0; type < input.getNumTypes(); ++type) {
      for (int slot = 0; slot < input.getNumTimeSlots(); ++slot) {
        final var decisionVertex = new DecisionVertex(idSupplier.get(), type, slot);
        decisionVertices.put(Pair.of(type, slot), decisionVertex);
        graph.addVertex(decisionVertex);
      }
    }
    LOGGER.debug("Number of added decision vertices: {}", decisionVertices.size());
  }

  private void addTimeSlotVertices() {
    for (int slot = 0; slot < input.getNumTimeSlots(); ++slot) {
      final var timeSlotVertex = new TimeSlotVertex(idSupplier.get(), slot);
      timeSlotVertices[slot] = timeSlotVertex;
      graph.addVertex(timeSlotVertex);
    }
    LOGGER.debug("Number of added time slot vertices: {}", timeSlotVertices.length);
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
