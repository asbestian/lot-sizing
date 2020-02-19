package de.asbestian.productionproblem.optimisation;

/** @author Sebastian Schenker */
class TimeSlotVertex extends Vertex {

  private final int timeSlot;

  TimeSlotVertex(final int id, final int timeSlot) {
    super(id);
    this.timeSlot = timeSlot;
  }

  int getTimeSlot() {
    return timeSlot;
  }
}
