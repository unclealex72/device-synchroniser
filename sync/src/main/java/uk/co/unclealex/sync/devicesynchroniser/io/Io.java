package uk.co.unclealex.sync.devicesynchroniser.io;

import android.net.Uri;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by alex on 31/12/14.
 */
public interface Io {
    void loadData(OutputStream out, String... pathSegments) throws IOException;

    void loadData(OutputStream out, List<String> pathSegments) throws IOException;

    JSONObject loadJson(String... pathSegments) throws IOException, JSONException;

    JSONObject loadJson(List<String> pathSegments) throws IOException, JSONException;

    Uri uriOf(List<String> pathSegments);
}
