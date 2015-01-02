package uk.co.unclealex.sync.devicesynchroniser.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import dagger.Module;
import dagger.Provides;
import uk.co.unclealex.sync.devicesynchroniser.dates.Iso8601;

import javax.inject.Singleton;

/**
 * Created by alex on 17/12/14.
 */
@Module(
        complete = false,
        library = true
)
public class PreferencesModule {

    @Provides
    @Singleton
    public SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    public Preferences providePreferences(SharedPreferences sharedPreferences, Context context, Iso8601 iso8601) {
        return new PreferencesImpl(sharedPreferences, context, iso8601);
    }

}