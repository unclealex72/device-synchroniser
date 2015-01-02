package uk.co.unclealex.sync.devicesynchroniser.dates;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Created by alex on 26/12/14.
 */
@Module(
        complete = false,
        library = true
)
public class DateModule {

    @Provides
    @Singleton
    public Iso8601 provideIso8601() {
        return new Iso8601Impl();
    }
}
