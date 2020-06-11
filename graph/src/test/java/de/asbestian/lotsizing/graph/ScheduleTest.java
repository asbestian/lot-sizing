package de.asbestian.lotsizing.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.asbestian.lotsizing.graph.vertex.DecisionVertex;
import de.asbestian.lotsizing.graph.vertex.DemandVertex;
import de.asbestian.lotsizing.graph.vertex.SuperSink;
import de.asbestian.lotsizing.graph.vertex.TimeSlotVertex;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.Test;

/** @author Sebastian Schenker */
class ScheduleTest {

  @Test
  void constructor() {
    final String path =
        "src/test/resources/Instance-3timeslots_3types.txt"; // instance has one feasible solution
    assert Files.exists(Paths.get(path));
    final Input input = new Input();
    input.read(path);
    final Problem problem = new Problem(input);
    problem.build();
    final List<DemandVertex> demandVertices = problem.getDemandVertices();
    final List<DecisionVertex> decisionVertices = problem.getDecisionVertices();
    final List<TimeSlotVertex> timeSlotVertices = problem.getTimeSlotVertices();
    final SuperSink superSink = problem.getSuperSink();
    final Set<Pair<Vertex, Vertex>> usedEdges =
        Set.of(
            Pair.of(demandVertices.get(0), decisionVertices.get(1)),
            Pair.of(demandVertices.get(1), decisionVertices.get(3)),
            Pair.of(demandVertices.get(2), decisionVertices.get(8)),
            Pair.of(decisionVertices.get(1), timeSlotVertices.get(1)),
            Pair.of(decisionVertices.get(3), timeSlotVertices.get(0)),
            Pair.of(decisionVertices.get(8), timeSlotVertices.get(2)),
            Pair.of(timeSlotVertices.get(1), superSink),
            Pair.of(timeSlotVertices.get(0), superSink),
            Pair.of(timeSlotVertices.get(2), superSink));
    final String expectedSchedule = "[1, 0, 2]";
    final int expectedInventoryCost = 0;
    final int expectedChangeOverCost = 3 + 2;

    final Schedule schedule = new Schedule(input, usedEdges);
    final Set<Pair<Vertex, Vertex>> edgesInSchedule = problem.getUsedGraphEdges(schedule);

    assertEquals(expectedSchedule, schedule.toString());
    assertEquals(expectedInventoryCost, schedule.getInventoryCost());
    assertEquals(expectedChangeOverCost, schedule.getChangeOverCost());
    assertEquals(expectedInventoryCost + expectedChangeOverCost, schedule.getCost());
    assertEquals(usedEdges.size(), edgesInSchedule.size());
  }

  @Test
  void compute() {
    final String path =
        "src/test/resources/Instance-4timeslots_2types.txt"; // instance has two feasible solutions
    assert Files.exists(Paths.get(path));
    final Input input = new Input();
    input.read(path);
    final Problem problem = new Problem(input);
    problem.build();
    final Schedule initSchedule = problem.computeRandomSchedule();
    assertEquals("[1, 0, 1, -1]", initSchedule.toString());

    final HashMap<Integer, Vertex> vertices = new HashMap<>();
    problem.getDemandVertices().forEach(v -> vertices.put(v.getId(), v));
    problem.getDecisionVertices().forEach(v -> vertices.put(v.getId(), v));
    problem.getTimeSlotVertices().forEach(v -> vertices.put(v.getId(), v));
    vertices.put(problem.getSuperSink().getId(), problem.getSuperSink());

    final Cycle cycle =
        new Cycle(
            List.of(
                vertices.get(3),
                vertices.get(11),
                vertices.get(15),
                vertices.get(0),
                vertices.get(14),
                vertices.get(10)));

    final String expectedSchedule = "[1, 0, -1, 1]";
    final int expectedInventoryCost = 0;
    final int expectedChangerOverCost = 4 + 3;

    final Schedule schedule = initSchedule.compute(cycle, input);

    assertEquals(expectedSchedule, schedule.toString());
    assertEquals(expectedInventoryCost, schedule.getInventoryCost());
    assertEquals(expectedChangerOverCost, schedule.getChangeOverCost());
    assertEquals(expectedInventoryCost + expectedChangerOverCost, schedule.getCost());
  }

  @Test
  void equals() {
    final String path =
        "src/test/resources/Instance-3timeslots_3types.txt"; // instance has one feasible solution
    assert Files.exists(Paths.get(path));
    final Input input = new Input();
    input.read(path);
    final Problem problem = new Problem(input);
    problem.build();
    final List<DemandVertex> demandVertices = problem.getDemandVertices();
    final List<DecisionVertex> decisionVertices = problem.getDecisionVertices();
    final List<TimeSlotVertex> timeSlotVertices = problem.getTimeSlotVertices();
    final SuperSink superSink = problem.getSuperSink();
    final List<Pair<Vertex, Vertex>> usedEdges =
        Arrays.asList(
            Pair.of(demandVertices.get(0), decisionVertices.get(1)),
            Pair.of(demandVertices.get(1), decisionVertices.get(3)),
            Pair.of(demandVertices.get(2), decisionVertices.get(8)),
            Pair.of(decisionVertices.get(1), timeSlotVertices.get(1)),
            Pair.of(decisionVertices.get(3), timeSlotVertices.get(0)),
            Pair.of(decisionVertices.get(8), timeSlotVertices.get(2)),
            Pair.of(timeSlotVertices.get(1), superSink),
            Pair.of(timeSlotVertices.get(0), superSink),
            Pair.of(timeSlotVertices.get(2), superSink));

    final Schedule schedule = new Schedule(input, usedEdges);
    final Set<Pair<Vertex, Vertex>> edgesInSchedule = problem.getUsedGraphEdges(schedule);
    Collections.shuffle(usedEdges);
    final Schedule sameSchedule = new Schedule(input, usedEdges);
    final Set<Pair<Vertex, Vertex>> edgesInSameSchedule = problem.getUsedGraphEdges(sameSchedule);

    assertEquals(schedule, sameSchedule);
    assertEquals(edgesInSchedule, edgesInSameSchedule);
  }
}
