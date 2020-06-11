package de.asbestian.lotsizing.graph;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.asbestian.lotsizing.graph.vertex.DecisionVertex;
import de.asbestian.lotsizing.graph.vertex.DemandVertex;
import de.asbestian.lotsizing.graph.vertex.TimeSlotVertex;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.graph.vertex.Vertex.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jgrapht.alg.util.Pair;
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
    cycle = new Cycle(vertices);
  }

  @Test
  void getEdges() {
    final List<Pair<Vertex, Vertex>> expected =
        Arrays.asList(
            Pair.of(demandVertex, decisionVertex),
            Pair.of(decisionVertex, timeSlotVertex),
            Pair.of(timeSlotVertex, otherDecisionVertex),
            Pair.of(otherDecisionVertex, demandVertex));

    assertIterableEquals(expected, cycle.getEdges());
  }

  @Test
  void getActivatedEdges() {
    final var expected = Collections.singletonList(Pair.of(demandVertex, decisionVertex));

    assertIterableEquals(expected, cycle.getActivatedEdges());
  }

  @Test
  void getDeactivatedEdges() {
    final var expectedVertices =
        Collections.singletonList(Pair.of(demandVertex, otherDecisionVertex));

    assertIterableEquals(expectedVertices, cycle.getDeactivatedEdges());
  }
}
