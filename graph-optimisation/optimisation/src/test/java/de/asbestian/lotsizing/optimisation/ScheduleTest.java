package de.asbestian.lotsizing.optimisation;

import de.asbestian.lotsizing.input.Input;
import de.asbestian.lotsizing.optimisation.vertex.SuperSink;
import de.asbestian.lotsizing.optimisation.vertex.Vertex;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** @author Sebastian Schenker */
class ScheduleTest {

  private Schedule schedule;

  @Test
  void constructor() {
    final String path =
        "src/test/resources/Instance-3timeslots_3types.txt"; // instance has one feasible solution
    assert Files.exists(Paths.get(path));
    final Input input = new Input();
    input.read(path);
    final Problem problem = new Problem(input);
    problem.build();
    final List<Vertex> demandVertices = problem.getDemandVertices();
    final List<Vertex> decisionVertices = problem.getDecisionVertices();
    final List<Vertex> timeSlotVertices = problem.getTimeSlotVertices();
    final SuperSink superSink = problem.getSuperSink();
    final Collection<Pair<Vertex, Vertex>> usedEdges =
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
    final String expectedSchedule = "[1, 0, 2]";
    final int expectedInventoryCost = 0;
    final int expectedChangeOverCost = 3 + 2;

    final Schedule schedule = new Schedule(input, usedEdges);

    assertEquals(expectedSchedule, schedule.toString());
    assertEquals(expectedInventoryCost, schedule.getInventoryCost());
    assertEquals(expectedChangeOverCost, schedule.getChangeOverCost());
    assertEquals(expectedInventoryCost + expectedChangeOverCost, schedule.getCost());
    assertEquals(usedEdges.size(), schedule.getEdges().size());
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
    final Schedule initSchedule = problem.computeInitialSchedule(); //
    assertEquals("[1, 0, 1, -1]", initSchedule.toString());
    final Graph<Vertex, DefaultEdge> resGraph = problem.getResidualGraph(initSchedule);
    final List<Cycle> cycles = Problem.computeCycles(resGraph);
    assertEquals(1, cycles.size());
    final String expectedSchedule = "[1, 0, -1, 1]";
    final int expectedInventoryCost = 0;
    final int expectedChangerOverCost = 4 + 3;

    final Schedule schedule = initSchedule.compute(cycles.get(0), input);

    assertEquals(expectedSchedule, schedule.toString());
    assertEquals(expectedInventoryCost, schedule.getInventoryCost());
    assertEquals(expectedChangerOverCost, schedule.getChangeOverCost());
    assertEquals(expectedInventoryCost + expectedChangerOverCost, schedule.getCost());
    assertEquals(initSchedule.getEdges().size(), schedule.getEdges().size());
  }
}
