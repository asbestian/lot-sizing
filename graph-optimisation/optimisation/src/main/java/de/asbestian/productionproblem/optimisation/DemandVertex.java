package de.asbestian.productionproblem.optimisation;

/** @author Sebastian Schenker */
class DemandVertex extends Vertex {

  private final int type;
  private final int timeSlot; // the time slot before item needs to be produced;

  DemandVertex(final int id, final int type, final int timeSlot) {
    super(id);
    this.type = type;
    this.timeSlot = timeSlot;
  }

  int getTimeSlot() {
    return timeSlot;
  }

  int getType() {
    return type;
  }
}
