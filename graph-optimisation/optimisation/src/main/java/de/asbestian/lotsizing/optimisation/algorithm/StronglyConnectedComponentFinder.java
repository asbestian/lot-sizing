package de.asbestian.lotsizing.optimisation.algorithm;

import de.asbestian.lotsizing.optimisation.vertex.Vertex;

import java.util.Collection;
import java.util.List;

public interface StronglyConnectedComponentFinder {

  Collection<List<Vertex>> get();
}
