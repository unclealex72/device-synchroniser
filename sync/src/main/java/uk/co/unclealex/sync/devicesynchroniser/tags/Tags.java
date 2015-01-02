package uk.co.unclealex.sync.devicesynchroniser.tags;

import android.net.Uri;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by alex on 31/12/14.
 */
@RequiredArgsConstructor
@Value
public class Tags {

    private final String albumArtist;
    private final String album;
    private final Uri coverArt;

    public static Tags of(JSONObject obj, Uri coverArtUri) throws JSONException {
        String albumArtist = obj.getString("albumArtist");
        String album = obj.getString("album");
        return new Tags(albumArtist, album, coverArtUri);
    }
}
