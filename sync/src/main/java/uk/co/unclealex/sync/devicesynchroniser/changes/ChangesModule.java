package uk.co.unclealex.sync.devicesynchroniser.changes;

import dagger.Module;
import dagger.Provides;
import uk.co.unclealex.sync.devicesynchroniser.dates.Iso8601;
import uk.co.unclealex.sync.devicesynchroniser.io.Io;
import uk.co.unclealex.sync.devicesynchroniser.prefs.Preferences;

import javax.inject.Singleton;

/**
 * Created by alex on 17/12/14.
 */
@Module(
        complete = false,
        library = true
)
public class ChangesModule {

    @Provides
    @Singleton
    public ChangeService provideChangeService(Io io, Preferences preferences, Iso8601 iso8601) {
        return new ChangeServiceImpl(io, preferences, iso8601);
    }

}