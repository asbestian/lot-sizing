package de.asbestian.lotsizing.visualisation;

import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import de.asbestian.lotsizing.graph.vertex.SuperSink;
import de.asbestian.lotsizing.graph.vertex.Vertex;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.jgrapht.alg.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Sebastian Schenker */
public class Visualisation {

  private final Logger LOGGER = LoggerFactory.getLogger(Visualisation.class);
  private static final int FRAME_WIDTH = 1600;
  private static final int FRAME_HEIGHT = 900;
  private static final int BORDER_OFFSET = 30;
  private static final int VERTEX_WIDTH = 5;
  private static final int VERTEX_HEIGHT = 5;
  private final mxGraph graph;
  private final Map<Vertex, Object> vertices;
  private final Map<Pair<Vertex, Vertex>, Object> edges;

  public Visualisation() {
    graph = new mxGraph();
    vertices = new HashMap<>();
    edges = new HashMap<>();
  }

  public void addEdges(final Collection<Pair<Vertex, Vertex>> edges) {
    final mxIGraphModel model = graph.getModel();
    final Object parent = graph.getDefaultParent();
    model.beginUpdate();
    try {
      for (final Pair<Vertex, Vertex> pair : edges) {
        final Object mxSource = vertices.getOrDefault(pair.getFirst(), null);
        final Object mxTarget = vertices.getOrDefault(pair.getSecond(), null);
        if (Objects.nonNull(mxSource) && Objects.nonNull(mxTarget)) {
          final Object mxEdge = graph.insertEdge(parent, null, "", mxSource, mxTarget);
          this.edges.put(pair, mxEdge);
        } else {
          if (Objects.isNull(mxSource)) {
            LOGGER.warn("{} not found.", pair.getFirst());
          }
          if (Objects.isNull(mxTarget)) {
            LOGGER.warn("{} not found.", pair.getSecond());
          }
          LOGGER.warn(
              "Edge betweeen {} and {} cannot be drawn.", pair.getFirst(), pair.getSecond());
        }
      }
    } finally {
      model.endUpdate();
    }
  }

  public void addVertices(
      final Collection<Vertex> demandVertices,
      final Collection<Vertex> decisionVertices,
      final Collection<Vertex> timeSlotVertices,
      final SuperSink superSink) {

    graph.getModel().beginUpdate();
    final int verticalOffset = (FRAME_HEIGHT - BORDER_OFFSET) / 5;
    try {
      final double demandVertexOffset =
          (FRAME_WIDTH - BORDER_OFFSET) / (double) demandVertices.size();
      addToGraph(demandVertices, demandVertexOffset, verticalOffset);

      final double decisionVertexOffset =
          (FRAME_WIDTH - BORDER_OFFSET) / (double) decisionVertices.size();
      addToGraph(decisionVertices, decisionVertexOffset, 2 * verticalOffset);

      final double timeSlotOffset =
          (FRAME_WIDTH - BORDER_OFFSET) / (double) timeSlotVertices.size();
      addToGraph(timeSlotVertices, timeSlotOffset, 3 * verticalOffset);

      addToGraph(
          Collections.singleton(superSink), (FRAME_WIDTH - BORDER_OFFSET) / 2., 4 * verticalOffset);

    } finally {
      graph.getModel().endUpdate();
    }
  }

  /**
   * Writes visualisation as jpg-file to tmp-directory.
   *
   * @param filename name of the file
   */
  public void saveToJPG(final String filename) {
    final BufferedImage image =
        mxCellRenderer.createBufferedImage(graph, null, 1, Color.WHITE, true, null);
    final Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
    final File file = new File(tmp.toString() + tmp.getFileSystem().getSeparator() + filename);
    try {
      ImageIO.write(image, "JPG", file);
    } catch (final IOException e) {
      LOGGER.warn("Not able to write {}", file.toString());
    }
  }

  private void addToGraph(
      final Collection<Vertex> vertices,
      final double horizontalOffset,
      final double verticalOffset) {
    double offset = horizontalOffset;
    final Object parent = graph.getDefaultParent();
    for (final Vertex vertex : vertices) {
      final Object mxVertex =
          graph.insertVertex(parent, null, "", offset, verticalOffset, VERTEX_WIDTH, VERTEX_HEIGHT);
      this.vertices.put(vertex, mxVertex);
      offset += horizontalOffset;
    }
  }
}
