package de.asbestian.lotsizing.runner;

import dagger.BindsInstance;
import dagger.Component;
import de.asbestian.lotsizing.algorithm.EnumerationModule;
import de.asbestian.lotsizing.algorithm.LocalSearchModule;
import de.asbestian.lotsizing.algorithm.Solver;
import de.asbestian.lotsizing.graph.Problem;
import de.asbestian.lotsizing.input.InputModule;
import java.util.Map;
import javax.inject.Singleton;

/** @author Sebastian Schenker */
@Component(modules = {InputModule.class, EnumerationModule.class, LocalSearchModule.class})
@Singleton
public abstract class RunnerComponent {
  abstract Problem problem();

  abstract Map<String, Solver> solvers();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder fileName(final String filename);

    @BindsInstance
    Builder resGraphVertexSize(final int size);

    @BindsInstance
    Builder greatestDescent(final boolean descent);

    RunnerComponent build();
  }
}
