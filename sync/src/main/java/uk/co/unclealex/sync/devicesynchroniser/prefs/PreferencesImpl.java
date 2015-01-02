package uk.co.unclealex.sync.devicesynchroniser.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v4.provider.DocumentFile;
import lombok.RequiredArgsConstructor;
import uk.co.unclealex.sync.devicesynchroniser.dates.Iso8601;

import java.io.File;
import java.util.Date;

/**
 * Created by alex on 17/12/14.
 */
@RequiredArgsConstructor
public class PreferencesImpl implements Preferences {

    private final SharedPreferences preferences;
    private final Context context;
    private final Iso8601 iso8601;

    @Override
    public String getHost() {
        return preferences.getString("pref_host_name", "");
    }

    @Override
    public int getPort() {
        return Integer.parseInt(preferences.getString("pref_host_port", "80"));
    }

    @Override
    public String getUser() {
        return preferences.getString("pref_username", "");
    }

    @Override
    public int getOffset() {
        return Integer.parseInt(preferences.getString("pref_offset", "0"));
    }

    @Override
    public String getSince() {
        String since = preferences.getString("pref_since", "");
        if (since.trim().isEmpty()) {
            return iso8601.format(new Date(0));
        } else {
            return since;
        }
    }

    @Override
    public DocumentFile getRootDocumentFile() {
        String rootDir = preferences.getString("pref_root_dir", "");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentFile.fromTreeUri(context, Uri.parse(rootDir));
        } else {
            return DocumentFile.fromFile(new File(rootDir));
        }
    }

    @Override
    public void setOffset(int offset) {
        put("pref_offset", offset);
    }

    @Override
    public void setSince(Date since) {
        put("pref_since", iso8601.format(since));
    }

    @Override
    public void setPort(int port) {
        put("pref_host_port", port);
    }

    protected void put(String key, Object value) {
        preferences.edit().putString(key, value == null ? "" : value.toString()).commit();
    }
}
