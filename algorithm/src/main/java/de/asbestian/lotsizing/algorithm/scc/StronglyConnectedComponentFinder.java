package de.asbestian.lotsizing.algorithm.scc;

import de.asbestian.lotsizing.graph.vertex.Vertex;
import java.util.Collection;
import java.util.Set;

/** @author Sebastian Schenker */
public interface StronglyConnectedComponentFinder {

  /**
   * Computes all strongly connected components. The considered graph is given by all vertices whose
   * id is at least the given idThreshold. In other words, all vertices whose id is less than the
   * given idThreshold are discarded from the computation.
   *
   * @param idThreshold threshold which determines which vertices to consider
   * @return vertex subsets which induce strongly connected components
   */
  Collection<Set<Vertex>> computeSCCs(int idThreshold);
}
