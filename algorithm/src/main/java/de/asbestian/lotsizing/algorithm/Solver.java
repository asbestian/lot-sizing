package de.asbestian.lotsizing.algorithm;

import de.asbestian.lotsizing.graph.Schedule;

/** @author Sebastian Schenker */
public interface Solver {

  Schedule search(double timeLimit);
}
