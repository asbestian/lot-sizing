package de.asbestian.lotsizing.input;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/** @author Sebastian Schenker */
@Module
public abstract class InputModule {
  @Binds
  @Singleton
  abstract Input getInput(FileInput fileInput);

  @Provides
  static FileInput getFileInput(String file) {
    return new FileInput(file);
  }
}
