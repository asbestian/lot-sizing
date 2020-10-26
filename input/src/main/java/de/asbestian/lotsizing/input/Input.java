package de.asbestian.lotsizing.input;

import java.util.List;

/** @author Sebastian Schenker */
public interface Input {

  int getNumTimeSlots();

  int getNumTypes();

  int getInventoryCost();

  int getOverallDemandPerType(final int type);

  int getNumProducedItems();

  List<Integer> getDemand(final int type);

  /** Returns the change over cost from predType to succType; */
  int getChangeOverCost(final int predType, final int succType);
}
