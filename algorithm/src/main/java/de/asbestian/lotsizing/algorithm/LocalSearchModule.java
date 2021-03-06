package de.asbestian.lotsizing.algorithm;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;

/** @author Sebastian Schenker */
@Module
public abstract class LocalSearchModule {
  @Binds
  @IntoMap
  @StringKey("lns")
  abstract Solver getSolver(LocalSearchImpl localSearch);
}
