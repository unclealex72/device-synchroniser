package uk.co.unclealex.sync.devicesynchroniser.sync;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SynchroniseService extends IntentService {

    private static final int ONGOING_NOTIFICATION_ID = 1;
    private static final int FINISHED_NOTIFICATION_ID = 2;

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startAction(Context context) {
        Intent intent = new Intent(context, SynchroniseService.class);
        context.startService(intent);
    }

    public SynchroniseService() {
        super("SynchroniseService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        startForeground(ONGOING_NOTIFICATION_ID, notificationOf(R.string.notification_start_message, true));
        try {
            new Action(getHost(), getPort(), getRootDir(), getSince(), getUser(), getOffset()).execute();
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            Log.e(SynchroniseService.class.getName(), writer.toString());
            showNotification(FINISHED_NOTIFICATION_ID, R.string.notification_failure_message, e.getMessage());
        } finally {
            stopForeground(false);
        }
    }

    class Action {

        private final String host;
        private final String port;
        private final DocumentFile rootDir;
        private final String since;
        private final String user;
        private final int offset;

        public Action(String host, String port, DocumentFile rootDir, String since, String user, int offset) {
            this.host = host;
            this.port = port;
            this.rootDir = rootDir;
            this.since = since;
            this.user = user;
            this.offset = offset;
        }

        public Uri.Builder baseUri() {
            return new Uri.Builder().scheme("http").encodedAuthority(host + ":" + port);
        }

        public void loadData(OutputStream out, String... pathSegments) throws IOException {
            loadData(out, Arrays.asList(pathSegments));
        }

        public void loadData(OutputStream out, List<String> pathSegments) throws IOException {
            Uri.Builder builder = baseUri();
            for (String pathSegment : pathSegments) {
                builder = builder.appendPath(pathSegment);
            }
            String url = builder.build().toString();
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

        public JSONObject lookForChanges() throws JSONException, IOException {
            return loadJson("changes", user, since);
        }

        public JSONObject loadJson(String... pathSegments) throws IOException, JSONException {
            return loadJson(Arrays.asList(pathSegments));
        }

        public JSONObject loadJson(List<String> pathSegments) throws IOException, JSONException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            loadData(out, pathSegments);
            JSONObject obj = new JSONObject(new String(out.toByteArray(), "UTF-8"));
            return obj;
        }

        public void execute() throws Exception {
            Date now = new Date();
            JSONArray changes = lookForChanges().getJSONArray("changes");
            int len = changes.length();
            int idx = 0;
            try {
                for (; idx < len; idx++) {
                    if (idx >= offset) {
                        Change change = Change.of(changes.getJSONObject(idx));
                        showNotification(ONGOING_NOTIFICATION_ID, R.string.notification_ongoing_message, idx, len, change.getRelativePath());
                        if (Change.Type.ADDED.equals(change.getType())) {
                            add(change);
                        } else {
                            remove(change);
                        }
                    }
                }
                setSince(now);
                showNotification(FINISHED_NOTIFICATION_ID, R.string.notification_success_message, len);
            } catch (Exception e) {
                setOffset(idx);
                throw e;
            }
            setOffset(0);
        }

        public void add(Change change) throws IOException, JSONException {
            List<String> changePathSegments = change.toPathSegments();
            DocumentFile albumDir = getRootDir();
            while (changePathSegments.size() != 1) {
                String dir = changePathSegments.remove(0);
                DocumentFile[] children = albumDir.listFiles();
                DocumentFile child = null;
                for (int idx = 0; child == null && idx < children.length; idx++) {
                    if (children[idx].getName().equalsIgnoreCase(dir)) {
                        child = children[idx];
                    }
                }
                if (child == null) {
                    albumDir = albumDir.createDirectory(dir);
                }
                else {
                    albumDir = child;
                }
            }
            String filename = changePathSegments.get(0);
            DocumentFile trackFile = albumDir.findFile(filename);
            if (trackFile == null) {
                trackFile = albumDir.createFile("audio/mp3", filename);
            }
            List<String> pathSegments = change.toPathSegments("music", user);
            OutputStream out = getContentResolver().openOutputStream(trackFile.getUri());
            loadData(out, pathSegments);
            out.close();
            registerMusicFile(trackFile);
        }

        public void registerMusicFile(DocumentFile documentFile) throws IOException, JSONException {
            broadcast(documentFile.getUri());
        }

        public void broadcast(Uri uri) {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        }

        public void remove(Change change) throws IOException {
            DocumentFile documentFile = getRootDir();
            List<String> pathSegments = change.toPathSegments();
            do {
                String ps = pathSegments.remove(0);
                documentFile = documentFile.findFile(ps);
            } while (!pathSegments.isEmpty());
            documentFile.delete();
            DocumentFile parent = documentFile.getParentFile();
            while (parent.listFiles().length == 0) {
                parent.delete();
                parent = parent.getParentFile();
            }
        }
    }

    public Notification notificationOf(int messageId, boolean ongoing, Object... args) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        return new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getString(messageId, args))
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing)
                .setSmallIcon(R.drawable.ic_action_sync).build();
    }

    public void showNotification(int notificationId, int messageId, Object... args) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notificationOf(messageId, notificationId == ONGOING_NOTIFICATION_ID, args));
    }

    public String getHost() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString("pref_host_name", "");
    }

    @TargetApi(Build.VERSION_CODES.L)
    public File[] directoryCandidates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.L) {
            return getBaseContext().getExternalMediaDirs();
        }
        else {
            return getBaseContext().getExternalFilesDirs(Environment.DIRECTORY_MUSIC);
        }
    }

    public DocumentFile getRootDir() throws IOException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String rootDir = preferences.getString("pref_root_dir", "");
        if (rootDir.contains(":")) {
            return DocumentFile.fromTreeUri(getBaseContext(), Uri.parse(rootDir));
        }
        else {
            return DocumentFile.fromFile(new File(rootDir));
        }
    }

    public String getPort() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString("pref_host_port", "80");
    }

    public String getUser() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString("pref_username", "");
    }

    public int getOffset() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return Integer.parseInt(preferences.getString("pref_offset", "0"));
    }

    public String getSince() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String since = preferences.getString("pref_since", "");
        if (since.trim().isEmpty()) {
            return "1970-01-01T00:00:00.000Z";
        }
        else {
            return since;
        }
    }

    public void setOffset(int offset) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putString("pref_offset", Integer.toString(offset)).commit();
    }

    public void setSince(Date since) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        preferences.edit().putString("pref_since", fmt.format(since)).commit();
    }
}
