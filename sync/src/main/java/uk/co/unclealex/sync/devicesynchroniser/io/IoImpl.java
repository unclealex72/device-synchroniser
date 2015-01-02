package uk.co.unclealex.sync.devicesynchroniser.io;

import android.net.Uri;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.unclealex.sync.devicesynchroniser.prefs.Preferences;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by alex on 31/12/14.
 */
@RequiredArgsConstructor
public class IoImpl implements Io {

    private final Preferences preferences;

    protected Uri.Builder baseUri() {
        return new Uri.Builder().scheme("http").encodedAuthority(preferences.getHost() + ":" + preferences.getPort());
    }

    @Override
    public void loadData(OutputStream out, String... pathSegments) throws IOException {
        loadData(out, Arrays.asList(pathSegments));
    }

    @Override
    public void loadData(OutputStream out, List<String> pathSegments) throws IOException {
        String url = uriOf(pathSegments).toString();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = client.execute(httpGet);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == 200) {
            HttpEntity entity = response.getEntity();
            try {
                entity.writeTo(out);
            } finally {
                entity.consumeContent();
            }
        } else {
            throw new IOException("Calling to " + url + " returned status code " + statusCode);
        }
    }

    @Override
    public Uri uriOf(List<String> pathSegments) {
        Uri.Builder builder = baseUri();
        for (String pathSegment : pathSegments) {
            builder = builder.appendPath(pathSegment);
        }
        return builder.build();
    }

    @Override
    public JSONObject loadJson(String... pathSegments) throws IOException, JSONException {
        return loadJson(Arrays.asList(pathSegments));
    }

    @Override
    public JSONObject loadJson(List<String> pathSegments) throws IOException, JSONException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        loadData(out, pathSegments);
        JSONObject obj = new JSONObject(new String(out.toByteArray(), "UTF-8"));
        return obj;
    }


}
