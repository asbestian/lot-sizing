package de.asbestian.lotsizing.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.asbestian.lotsizing.graph.vertex.SuperSink;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import de.asbestian.lotsizing.input.Input;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.Test;

/** @author Sebastian Schenker */
class ProblemTest {

  @Test
  void getVertices() {
    final String path =
        "src/test/resources/Instance-3timeslots_3types.txt"; // instance has one feasible solution
    assert Files.exists(Paths.get(path));
    final Input input = new Input();
    input.read(path);

    final Problem problem = new Problem(input);
    problem.build();

    assertEquals(3, problem.getDemandVertices().size());
    assertEquals(9, problem.getDecisionVertices().size());
    assertEquals(3, problem.getTimeSlotVertices().size());
  }

  @Test
  void getNumberOfEdges() {
    final String path =
        "src/test/resources/Instance-3timeslots_3types.txt"; // instance has one feasible solution
    assert Files.exists(Paths.get(path));
    final Input input = new Input();
    input.read(path);

    final Problem problem = new Problem(input);
    problem.build();

    assertEquals(15, problem.getNumberOfEdges());
  }

  @Test
  void computeInitialSchedule_singleFeasibleSchedule() {
    final String path =
        "src/test/resources/Instance-3timeslots_3types.txt"; // instance has one feasible solution
    assert Files.exists(Paths.get(path));
    final Input input = new Input();
    input.read(path);
    final String expectedSchedule = "[1, 0, 2]";
    final int expectedInventoryCost = 0;
    final int expectedChangeOverCost = 3 + 2;
    final int expectedNumberOfEdges = 9;
    final Problem problem = new Problem(input);
    problem.build();

    final Schedule schedule = problem.computeRandomSchedule();

    assertEquals(expectedSchedule, schedule.toString());
    assertEquals(expectedInventoryCost, schedule.getInventoryCost());
    assertEquals(expectedChangeOverCost, schedule.getChangeOverCost());
    assertEquals(expectedInventoryCost + expectedChangeOverCost, schedule.getCost());
    assertEquals(expectedNumberOfEdges, schedule.getEdges().size());
  }

  @Test
  void computeSchedule_randomScheduleAndOptimalInventoryScheduleCoincide() {
    final String path =
        "src/test/resources/Instance-3timeslots_3types.txt"; // instance has one feasible solution
    assert Files.exists(Paths.get(path));
    final Input input = new Input();
    input.read(path);
    final Problem problem = new Problem(input);
    problem.build();

    final Schedule randSchedule = problem.computeRandomSchedule();
    final Schedule inventorySchedule = problem.computeOptimalInventoryCostSchedule();

    assertEquals(randSchedule, inventorySchedule);
  }

  @Test
  void computeOptimalInventorySchedule() {
    final String path =
        "src/test/resources/Instance-4timeslots_2types.txt"; // instance has two feasible solutions
    assert Files.exists(Paths.get(path));
    final Input input = new Input();
    input.read(path);
    final Problem problem = new Problem(input);
    problem.build();

    final Schedule schedule = problem.computeOptimalInventoryCostSchedule();
    final String expectedSchedule = "[1, 0, -1, 1]";
    final int expectedInventoryCost = 0;
    final int expectedChangerOverCost = 4 + 3;

    assertEquals(expectedSchedule, schedule.toString());
    assertEquals(expectedInventoryCost, schedule.getInventoryCost());
    assertEquals(expectedChangerOverCost, schedule.getChangeOverCost());
    assertEquals(expectedInventoryCost + expectedChangerOverCost, schedule.getCost());
  }

  @Test
  void getResidualGraph_singleFeasibleSchedule() {
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

    final Schedule schedule = problem.computeRandomSchedule();
    final Graph<Vertex, DefaultEdge> resGraph = problem.getResidualGraph(schedule);

    assertEquals(15, resGraph.edgeSet().size());

    assertEquals(1, resGraph.inDegreeOf(demandVertices.get(0)));
    assertEquals(1, resGraph.outDegreeOf(demandVertices.get(0)));
    assertEquals(1, resGraph.inDegreeOf(demandVertices.get(1)));
    assertEquals(0, resGraph.outDegreeOf(demandVertices.get(1)));
    assertEquals(1, resGraph.inDegreeOf(demandVertices.get(2)));
    assertEquals(2, resGraph.outDegreeOf(demandVertices.get(2)));

    assertEquals(2, resGraph.degreeOf(decisionVertices.get(0)));
    assertEquals(2, resGraph.degreeOf(decisionVertices.get(1)));
    assertEquals(0, resGraph.degreeOf(decisionVertices.get(2)));
    assertEquals(2, resGraph.degreeOf(decisionVertices.get(3)));
    assertEquals(0, resGraph.degreeOf(decisionVertices.get(4)));
    assertEquals(0, resGraph.degreeOf(decisionVertices.get(5)));
    assertEquals(2, resGraph.degreeOf(decisionVertices.get(6)));
    assertEquals(2, resGraph.degreeOf(decisionVertices.get(7)));
    assertEquals(2, resGraph.degreeOf(decisionVertices.get(8)));

    assertEquals(3, resGraph.inDegreeOf(timeSlotVertices.get(0)));
    assertEquals(1, resGraph.outDegreeOf(timeSlotVertices.get(0)));
    assertEquals(2, resGraph.inDegreeOf(timeSlotVertices.get(1)));
    assertEquals(1, resGraph.outDegreeOf(timeSlotVertices.get(1)));
    assertEquals(1, resGraph.inDegreeOf(timeSlotVertices.get(2)));
    assertEquals(1, resGraph.outDegreeOf(timeSlotVertices.get(2)));

    assertEquals(3, resGraph.outDegreeOf(superSink));
    assertEquals(0, resGraph.inDegreeOf(superSink));
  }
}
