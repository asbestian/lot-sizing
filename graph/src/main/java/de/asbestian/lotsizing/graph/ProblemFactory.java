package de.asbestian.lotsizing.graph;

import dagger.BindsInstance;
import dagger.Component;
import de.asbestian.lotsizing.input.InputModule;

/** @author Sebastian Schenker */
@Component(modules = InputModule.class)
public interface ProblemFactory {
  Problem problem();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder fileName(final String filename);

    ProblemFactory build();
  }
}
