package de.asbestian.lotsizing.algorithm.scc;

import de.asbestian.lotsizing.graph.vertex.Vertex;

import java.util.Collection;
import java.util.List;

public interface StronglyConnectedComponentFinder {

  Collection<List<Vertex>> get();
}
