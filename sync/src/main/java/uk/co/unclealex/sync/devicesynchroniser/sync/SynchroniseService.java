package uk.co.unclealex.sync.devicesynchroniser.sync;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
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
import java.util.*;

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
            new Action(getHost(), getPort(), getRootDir(), getSince(), getUser()).execute();
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
        private final File rootDir;
        private final String since;
        private final String user;

        public Action(String host, String port, File rootDir, String since, String user) {
            this.host = host;
            this.port = port;
            this.rootDir = rootDir;
            this.since = since;
            this.user = user;
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

        public void execute() throws IOException, JSONException {
            Date now = new Date();
            JSONArray changes = lookForChanges().getJSONArray("changes");
            int len = changes.length();
            for (int idx = 0; idx < len; idx++) {
                Change change = Change.of(changes.getJSONObject(idx));
                showNotification(ONGOING_NOTIFICATION_ID, R.string.notification_ongoing_message, idx, len, change.getRelativePath());
                if (Change.Type.ADDED.equals(change.getType())) {
                    add(change);
                } else {
                    remove(change);
                }
            }
            setSince(now);
            showNotification(FINISHED_NOTIFICATION_ID, R.string.notification_success_message, len);
        }

        public File baseDirectory() throws IOException {
            if (!rootDir.exists() && !rootDir.mkdirs()) {
                throw new IOException("Cannot find any external storage at " + rootDir);
            }
            return new File(rootDir, "synchronised");
        }

        public File toFile(Change change) throws IOException {
            return new File(baseDirectory(), change.getRelativePath().toString());
        }

        public void add(Change change) throws IOException, JSONException {
            File targetFile = toFile(change);
            File albumDirectory = targetFile.getParentFile();
            if (targetFile.exists() && !targetFile.delete()) {
                throw new IOException("Cannot remove existing file " + targetFile);
            }
            if (!albumDirectory.isDirectory() && !albumDirectory.mkdirs()) {
                throw new IOException("Cannot create directory " + albumDirectory);
            }
            File relativePath = change.getRelativePath();
            List<String> pathSegments = toPathSegments(relativePath, "music", user);
            FileOutputStream out = new FileOutputStream(targetFile);
            loadData(out, pathSegments);
            out.close();
            registerMusicFile(targetFile);
        }

        private List<String> toPathSegments(File relativePath, String... pathSegments) {
            List<String> newPathSegments = new LinkedList<String>();
            do {
                newPathSegments.add(0, relativePath.getName());
                relativePath = relativePath.getParentFile();
            } while (relativePath != null);
            for (int idx = pathSegments.length - 1; idx >= 0; idx--) {
                newPathSegments.add(0, pathSegments[idx]);
            }
            return newPathSegments;
        }

        public void registerMusicFile(File targetFile) throws IOException, JSONException {
            unregisterMusicFile(targetFile);
            Uri insertUri = Uri.fromFile(targetFile);
            broadcast(insertUri);
        }

        public void broadcast(Uri uri) {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        }

        public void remove(Change change) throws IOException {
            File targetFile = toFile(change);
            if (targetFile.exists() && !targetFile.delete()) {
                throw new IOException("Cannot remove existing file " + targetFile);
            }
            unregisterMusicFile(targetFile);
            File targetDirectory = targetFile.getParentFile();
            File baseDirectory = baseDirectory();
            while (!targetDirectory.equals(baseDirectory) && targetDirectory.exists() && targetDirectory.isDirectory()) {
                if (targetDirectory.list().length == 0) {
                    targetDirectory.delete();
                    targetDirectory = targetDirectory.getParentFile();
                } else {
                    targetDirectory = baseDirectory;
                }
            }
        }

        public void unregisterMusicFile(File targetFile) {
            String targetPath = targetFile.getAbsolutePath();
            ContentResolver contentResolver = getContentResolver();
            Uri deleteUri = MediaStore.Audio.Media.getContentUriForPath(targetPath);
            contentResolver.delete(
                    deleteUri,
                    MediaStore.MediaColumns.DATA + "=\""
                            + targetPath + "\"", null);
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

    public File getRootDir() throws IOException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String rootDir = preferences.getString("pref_root_dir", "");
        if (rootDir.isEmpty()) {
            File[] rootDirCandidates = directoryCandidates();
            SortedMap<Long, File> candidatesBySize = new TreeMap<Long, File>();
            for (File rootDirCandidate : rootDirCandidates) {
                long size = new StatFs(rootDirCandidate.getPath()).getTotalBytes();
                candidatesBySize.put(size, rootDirCandidate);
            }
            return candidatesBySize.get(candidatesBySize.lastKey());
        } else {
            return new File(rootDir);
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

    public void setSince(Date since) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        preferences.edit().putString("pref_since", fmt.format(since)).commit();
    }
}
