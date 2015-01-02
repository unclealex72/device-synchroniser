package uk.co.unclealex.sync.devicesynchroniser.main;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import uk.co.unclealex.sync.devicesynchroniser.AppModule;
import uk.co.unclealex.sync.devicesynchroniser.changes.ChangeService;
import uk.co.unclealex.sync.devicesynchroniser.dates.Iso8601;
import uk.co.unclealex.sync.devicesynchroniser.prefs.Preferences;
import uk.co.unclealex.sync.devicesynchroniser.tags.TagsService;

import javax.inject.Singleton;

@Module(
        injects = {MainActivity.class, MainActivity.PlaceholderFragment.class},
        addsTo = AppModule.class
)
public class MainModule {

    private MainView view;

    public MainModule(MainView view) {
        this.view = view;
    }

    @Provides
    @Singleton
    public MainPresenter providePresenter(Context context, Preferences preferences, Iso8601 iso8601, ChangeService changeService, TagsService tagsService) {
        return new MainPresenterImpl(context, view, preferences, iso8601, changeService, tagsService);
    }
}
