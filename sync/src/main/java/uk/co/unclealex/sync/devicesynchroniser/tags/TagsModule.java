package uk.co.unclealex.sync.devicesynchroniser.tags;

import dagger.Module;
import dagger.Provides;
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
public class TagsModule {

    @Provides
    @Singleton
    public TagsService provideTagsService(Io io, Preferences preferences) {
        return new TagsServiceImpl(io, preferences);
    }

}