package de.asbestian.de.productionproblem.optimisation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.asbestian.productionproblem.input.Input;
import de.asbestian.productionproblem.optimisation.Problem;
import de.asbestian.productionproblem.optimisation.Vertex;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** @author Satalia team. */
class ProblemTest {

  private Problem problem;

  @BeforeEach
  void setUp() {
    final String path =
        "src/test/resources/Instance-4timeslots_2types.txt"; // instance has one feasible solution
    assert Files.exists(Paths.get(path));
    final Input input = new Input();
    input.read(path);
    problem = new Problem(input);
  }

  @Test
  void buildEmptyGraph() {
    final Graph<Vertex, DefaultEdge> graph = Problem.buildEmptyGraph();

    assertTrue(graph.vertexSet().isEmpty());
    assertTrue(graph.edgeSet().isEmpty());
  }

  @Test
  void build() {
    assertDoesNotThrow(() -> problem.build());
  }
}
