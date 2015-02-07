package uk.co.unclealex.sync.devicesynchroniser.io;

import android.net.Uri;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.unclealex.sync.devicesynchroniser.prefs.NotInitialisedException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by alex on 31/12/14.
 */
public interface Io {
    void loadData(OutputStream out, String... pathSegments) throws IOException, NotInitialisedException;

    void loadData(OutputStream out, List<String> pathSegments) throws IOException, NotInitialisedException;

    JSONObject loadJson(String... pathSegments) throws IOException, JSONException, NotInitialisedException;

    JSONObject loadJson(List<String> pathSegments) throws IOException, JSONException, NotInitialisedException;

    Uri uriOf(List<String> pathSegments) throws NotInitialisedException;
}
