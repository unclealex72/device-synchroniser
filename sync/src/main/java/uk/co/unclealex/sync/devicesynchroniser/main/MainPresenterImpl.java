package uk.co.unclealex.sync.devicesynchroniser.main;

import android.content.Context;
import android.os.AsyncTask;
import lombok.RequiredArgsConstructor;
import uk.co.unclealex.sync.devicesynchroniser.changes.ChangeService;
import uk.co.unclealex.sync.devicesynchroniser.changes.Changelog;
import uk.co.unclealex.sync.devicesynchroniser.changes.RelativePath;
import uk.co.unclealex.sync.devicesynchroniser.dates.Iso8601;
import uk.co.unclealex.sync.devicesynchroniser.prefs.NotInitialisedException;
import uk.co.unclealex.sync.devicesynchroniser.prefs.Preferences;
import uk.co.unclealex.sync.devicesynchroniser.sync.R;
import uk.co.unclealex.sync.devicesynchroniser.sync.SynchroniseService;
import uk.co.unclealex.sync.devicesynchroniser.tags.Tags;
import uk.co.unclealex.sync.devicesynchroniser.tags.TagsService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by alex on 21/12/14.
 */
@RequiredArgsConstructor
public class MainPresenterImpl implements MainPresenter {

    private final Context context;
    private final MainView mainView;
    private final Preferences preferences;
    private final Iso8601 iso8601;
    private final ChangeService changeService;
    private final TagsService tagsService;

    @Override
    public void synchronise() {
        SynchroniseService.synchronise(context);
    }

    @Override
    public void updateLastSynchronisedTime() {
        Date lastSynchronisedDate = iso8601.parse(preferences.getSince());
        String date = new SimpleDateFormat(context.getString(R.string.last_synchronised_date_format)).format(lastSynchronisedDate);
        mainView.getLastSynchronisedTextView().setText(context.getString(R.string.last_synchronised_text) + " " + date);
    }

    @Override
    public void updatePendingChanges() {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    return preferences.getOffset() != 0 || changeService.countChangesSince(preferences.getUser(), preferences.getSince()) != 0;
                } catch (IOException e) {
                    return false;
                } catch (NotInitialisedException e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean changesPending) {
                int id = changesPending ? R.string.changes_pending : R.string.no_changes_pending;
                mainView.getPendingChangesTextView().setText(context.getString(id));
            }
        };
        task.execute();
    }

    @Override
    public void loadNextChangelogPage(Changelog changelog) throws IOException {
        changeService.loadNextChangelogPage(changelog);
    }

    @Override
    public Tags loadTags(RelativePath relativePath) throws IOException, NotInitialisedException {
        return tagsService.loadTags(relativePath);
    }
}
