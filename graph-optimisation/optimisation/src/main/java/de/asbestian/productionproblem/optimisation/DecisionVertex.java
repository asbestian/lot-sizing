package de.asbestian.productionproblem.optimisation;

/** @author Sebastian Schenker */
class DecisionVertex extends Vertex {

  private final int type;
  private final int timeSlot;

  DecisionVertex(final int id, final int type, final int timeSlot) {
    super(id);
    this.timeSlot = timeSlot;
    this.type = type;
  }

  int getType() {
    return type;
  }

  int getTimeSlot() {
    return timeSlot;
  }
}
