package de.asbestian.de.productionproblem.optimisation;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.asbestian.productionproblem.optimisation.Cycle;
import de.asbestian.productionproblem.optimisation.DecisionVertex;
import de.asbestian.productionproblem.optimisation.DemandVertex;
import de.asbestian.productionproblem.optimisation.TimeSlotVertex;
import de.asbestian.productionproblem.optimisation.Vertex;
import de.asbestian.productionproblem.optimisation.Vertex.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** @author Sebastian Schenker */
class CycleTest {

  private Cycle cycle;
  private DemandVertex demandVertex;
  private DecisionVertex decisionVertex;
  private TimeSlotVertex timeSlotVertex;
  private DecisionVertex otherDecisionVertex;

  @BeforeEach
  void setUp() {
    demandVertex = mock(DemandVertex.class);
    when(demandVertex.getVertexType()).thenReturn(Type.DEMAND_VERTEX);
    decisionVertex = mock(DecisionVertex.class);
    when(decisionVertex.getVertexType()).thenReturn(Type.DECISION_VERTEX);
    timeSlotVertex = mock(TimeSlotVertex.class);
    when(timeSlotVertex.getVertexType()).thenReturn(Type.TIME_SLOT_VERTEX);
    otherDecisionVertex = mock(DecisionVertex.class);
    when(otherDecisionVertex.getVertexType()).thenReturn(Type.DECISION_VERTEX);
    final List<Vertex> vertices =
        Arrays.asList(demandVertex, decisionVertex, timeSlotVertex, otherDecisionVertex);
    final Graph<Vertex, DefaultEdge> resGraph = mock(Graph.class);
    when(resGraph.containsEdge(any(), any())).thenReturn(true);
    cycle = new Cycle(vertices, resGraph);
  }

  @Test
  void getOriginalGraphEdges() {
    final List<Pair<Vertex, Vertex>> expectedEdges =
        Arrays.asList(
            Pair.of(demandVertex, decisionVertex), Pair.of(decisionVertex, timeSlotVertex));

    assertIterableEquals(expectedEdges, cycle.getOriginalGraphEdges());
  }

  @Test
  void getReverseGraphEdges() {
    final List<Pair<Vertex, Vertex>> expectedEdges =
        Arrays.asList(
            Pair.of(timeSlotVertex, otherDecisionVertex),
            Pair.of(otherDecisionVertex, demandVertex));

    assertIterableEquals(expectedEdges, cycle.getReverseGraphEdges());
  }

  @Test
  void getActivatedDecisionVertices() {
    final List<DecisionVertex> expectedVertices = Collections.singletonList(decisionVertex);

    assertIterableEquals(expectedVertices, cycle.getActivatedDecisionVertices());
  }

  @Test
  void getDeactivatedDecisionVertices() {
    final List<DecisionVertex> expectedVertices = Collections.singletonList(otherDecisionVertex);

    assertIterableEquals(expectedVertices, cycle.getDeactivatedDecisionVertices());
  }
}
