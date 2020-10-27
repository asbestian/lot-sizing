package de.asbestian.lotsizing.input;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

/** @author Sebastian Schenker */
@Module
public abstract class InputModule {
  @Binds
  abstract Input getInput(final FileInput fileInput);

  @Provides
  static FileInput getFileInput(final String file) {
    return new FileInput(file);
  }
}
