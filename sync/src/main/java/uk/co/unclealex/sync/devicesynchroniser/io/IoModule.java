package uk.co.unclealex.sync.devicesynchroniser.io;

import dagger.Module;
import dagger.Provides;
import uk.co.unclealex.sync.devicesynchroniser.prefs.Preferences;

import javax.inject.Singleton;

/**
 * Created by alex on 17/12/14.
 */
@Module(
        complete = false,
        library = true
)
public class IoModule {

    @Provides
    @Singleton
    public Io provideIo(Preferences preferences) {
        return new IoImpl(preferences);
    }

}