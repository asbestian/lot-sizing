package de.asbestian.productionproblem.optimisation;

import java.util.Objects;

/**
 * The vertex types for the considered graph.
 *
 * @author Sebastian Schenker
 */
public class Vertex {

  private final int id;

  public Vertex(final int id) {
    this.id = id;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Vertex)) {
      return false;
    }
    final var v = (Vertex) o;
    return v.id == this.id;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return String.valueOf(id);
  }
}
