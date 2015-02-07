package uk.co.unclealex.sync.devicesynchroniser.tags;

import android.net.Uri;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.unclealex.sync.devicesynchroniser.changes.RelativePath;
import uk.co.unclealex.sync.devicesynchroniser.io.Io;
import uk.co.unclealex.sync.devicesynchroniser.prefs.NotInitialisedException;
import uk.co.unclealex.sync.devicesynchroniser.prefs.Preferences;

import java.io.IOException;

/**
 * Created by alex on 31/12/14.
 */
@RequiredArgsConstructor
public class TagsServiceImpl implements TagsService {

    private final Io io;
    private final Preferences preferences;

    @Override
    public Tags loadTags(RelativePath relativePath) throws IOException, NotInitialisedException {
        try {
            JSONObject obj = io.loadJson(relativePath.prefixedWith("tags", preferences.getUser()));
            Uri coverArtUri = io.uriOf(relativePath.prefixedWith("artwork", preferences.getUser()));
            return Tags.of(obj, coverArtUri);
        } catch (JSONException e) {
            throw new IOException("Could not read the tags from " + relativePath, e);
        }
    }
}
