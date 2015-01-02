package uk.co.unclealex.sync.devicesynchroniser.changes;

import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.unclealex.sync.devicesynchroniser.dates.Iso8601;
import uk.co.unclealex.sync.devicesynchroniser.io.Io;
import uk.co.unclealex.sync.devicesynchroniser.prefs.Preferences;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alex on 24/12/14.
 */
@RequiredArgsConstructor
public class ChangeServiceImpl implements ChangeService {

    private static final int PAGE_SIZE = 10;

    private final Io io;
    private final Preferences preferences;
    private final Iso8601 iso8601;

    @Override
    public int countChangesSince(String user, String since) throws IOException {
        try {
            JSONObject obj = io.loadJson("changes", "count", user, since);
            return obj.getInt("count");
        } catch (JSONException e) {
            throw new IOException("Cannot find out the number of changes for " + user + " since " + since, e);
        }
    }

    @Override
    public List<Change> changesSince(String user, String since) throws IOException {
        try {
            JSONArray changesArray = io.loadJson("changes", user, since).getJSONArray("changes");
            List<Change> changes = new ArrayList<Change>();
            for (int idx = 0; idx < changesArray.length(); idx++) {
                changes.add(Change.of(changesArray.getJSONObject(idx), user));
            }
            return changes;
        } catch (JSONException e) {
            throw new IOException("Could not parse JSON whilst loading changes.", e);
        }
    }

    @Override
    public void copyChange(Change change, OutputStream out) throws IOException {
        List<String> pathSegments = change.getRelativePath().prefixedWith("music", change.getUser());
        io.loadData(out, pathSegments);
    }

    @Override
    public void loadNextChangelogPage(Changelog changelog) throws IOException {
        if (changelog.getTotalChanges() < 0 || changelog.getChangelogItems().size() != changelog.getTotalChanges()) {
            int pageNumber = changelog.getChangelogItems().size() / PAGE_SIZE;
            try {
                JSONObject obj = io.loadJson("changelog", preferences.getUser(), Integer.toString(pageNumber), Integer.toString(PAGE_SIZE));
                changelog.setTotalChanges(obj.getInt("total"));
                JSONArray jsonArray = obj.getJSONArray("changelog");
                for (int idx = 0; idx < jsonArray.length(); idx++) {
                    changelog.getChangelogItems().add(ChangelogItem.of(jsonArray.getJSONObject(idx), iso8601));
                }
            } catch (JSONException e) {
                throw new IOException("Could not parse a changelog request.", e);
            }
        }
    }
}
