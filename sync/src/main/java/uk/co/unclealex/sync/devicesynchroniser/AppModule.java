package uk.co.unclealex.sync.devicesynchroniser;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import uk.co.unclealex.sync.devicesynchroniser.changes.ChangeServiceImpl;
import uk.co.unclealex.sync.devicesynchroniser.changes.ChangesModule;
import uk.co.unclealex.sync.devicesynchroniser.dates.DateModule;
import uk.co.unclealex.sync.devicesynchroniser.io.IoModule;
import uk.co.unclealex.sync.devicesynchroniser.notifications.NotificationsModule;
import uk.co.unclealex.sync.devicesynchroniser.prefs.PreferencesModule;
import uk.co.unclealex.sync.devicesynchroniser.sync.SynchroniseService;
import uk.co.unclealex.sync.devicesynchroniser.tags.TagsModule;

import javax.inject.Singleton;

/**
 * Created by alex on 17/12/14.
 */
@Module(
        injects = {
                App.class,
                SynchroniseService.class,
                ChangeServiceImpl.class
        },
        includes = {
                PreferencesModule.class,
                NotificationsModule.class,
                IoModule.class,
                TagsModule.class,
                DateModule.class,
                ChangesModule.class
        }

)
public class AppModule {

    private App app;

    public AppModule(App app) {
        this.app = app;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return app;
    }
}