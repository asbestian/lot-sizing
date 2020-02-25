package de.asbestian.productionproblem.visualisation;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import de.asbestian.productionproblem.optimisation.DecisionVertex;
import de.asbestian.productionproblem.optimisation.DemandVertex;
import de.asbestian.productionproblem.optimisation.SuperSink;
import de.asbestian.productionproblem.optimisation.TimeSlotVertex;
import de.asbestian.productionproblem.optimisation.Vertex;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;

/** @author Sebastian Schenker */
public class GraphVisualisation {

  private final int frameWidth = 1600;
  private final int frameHeight = 900;
  private final int borderOffset = 30;
  private final int vertexWidth = 5;
  private final int vertexHeight = 5;
  private final JFrame frame;
  private final mxGraph graph;
  private final Map<Vertex, Object> vertices;

  public GraphVisualisation(final String title) {
    frame = new JFrame(title);
    graph = new mxGraph();
    vertices = new HashMap<>();
  }

  public void visualiseVertices(
      final Collection<DemandVertex> demandVertices,
      final Collection<DecisionVertex> decisionVertices,
      final Collection<TimeSlotVertex> timeSlotVertices,
      final SuperSink superSink) {

    graph.getModel().beginUpdate();
    Object parent = graph.getDefaultParent();
    final int verticalOffset = (frameHeight - borderOffset) / 4;
    try {
      final double demandVertexOffset =
          (frameWidth - borderOffset) / (double) demandVertices.size();
      double horizontalOffset = demandVertexOffset;
      for (DemandVertex demandVertex : demandVertices) {
        final Object mxDemandVertex =
            graph.insertVertex(
                parent, null, "", horizontalOffset, verticalOffset, vertexWidth, vertexHeight);
        vertices.put(demandVertex, mxDemandVertex);
        horizontalOffset += demandVertexOffset;
      }

      final double decisionVertexOffset =
          (frameWidth - borderOffset) / (double) decisionVertices.size();
      horizontalOffset = decisionVertexOffset;
      for (DecisionVertex decisionVertex : decisionVertices) {
        final Object mxDecisionVertex =
            graph.insertVertex(
                parent, null, "", horizontalOffset, 2 * verticalOffset, vertexWidth, vertexHeight);
        vertices.put(decisionVertex, mxDecisionVertex);
        horizontalOffset += decisionVertexOffset;
      }

      final double timeSlotOffset = (frameWidth - borderOffset) / (double) timeSlotVertices.size();
      horizontalOffset = timeSlotOffset;
      for (TimeSlotVertex timeSlotVertex : timeSlotVertices) {
        final Object mxTimeSlotVertex =
            graph.insertVertex(
                parent, null, "", horizontalOffset, 3 * verticalOffset, vertexWidth, vertexHeight);
        vertices.put(timeSlotVertex, mxTimeSlotVertex);
        horizontalOffset += timeSlotOffset;
      }

      final Object mxSuperSink =
          graph.insertVertex(
              parent,
              null,
              "",
              (frameWidth - borderOffset) / 2.,
              4 * verticalOffset,
              vertexWidth,
              vertexHeight);
      vertices.put(superSink, mxSuperSink);
    } finally {
      graph.getModel().endUpdate();
    }
    mxGraphComponent graphComponent = new mxGraphComponent(graph);
    frame.getContentPane().add(graphComponent);
    frame.setSize(frameWidth, frameHeight);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }
}
