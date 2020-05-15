package de.asbestian.lotsizing.algorithm.scc;

import de.asbestian.lotsizing.graph.vertex.Vertex;
import java.util.Collection;
import java.util.Set;

public interface StronglyConnectedComponentFinder {

  Collection<Set<Vertex>> computeSCCs(int threshold);
}
