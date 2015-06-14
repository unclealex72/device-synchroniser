package uk.co.unclealex.sync.devicesynchroniser.sync;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import dagger.ObjectGraph;
import org.json.JSONException;
import uk.co.unclealex.sync.devicesynchroniser.App;
import uk.co.unclealex.sync.devicesynchroniser.changes.Change;
import uk.co.unclealex.sync.devicesynchroniser.changes.ChangeService;
import uk.co.unclealex.sync.devicesynchroniser.notifications.NotificationsService;
import uk.co.unclealex.sync.devicesynchroniser.prefs.Preferences;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SynchroniseService extends IntentService {

    @Inject
    Context context;
    @Inject
    NotificationsService notificationsService;
    @Inject
    Preferences preferences;
    @Inject
    ChangeService changeService;

    private ObjectGraph activityGraph;

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void synchronise(Context context) {
        Intent intent = new Intent(context, SynchroniseService.class);
        context.startService(intent);
    }

    public SynchroniseService() {
        super("SynchroniseService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        activityGraph = ((App) getApplication()).getObjectGraph();
        activityGraph.inject(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activityGraph = null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        notificationsService.initialiseOngoingNotification(this, R.string.notification_start_message);
        try {
            new Action(preferences.getRootDocumentFile(), preferences.getSince(), preferences.getUser(), preferences.getOffset()).execute();
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            Log.e(SynchroniseService.class.getName(), writer.toString());
            notificationsService.showClearableNotification(R.string.notification_failure_message, e.getMessage());
        } finally {
            stopForeground(false);
        }
    }

    class Action {

        private final DocumentFile rootDir;
        private final String since;
        private final String user;
        private final int offset;

        public Action(DocumentFile rootDir, String since, String user, int offset) {
            this.rootDir = rootDir;
            this.since = since;
            this.user = user;
            this.offset = offset;
        }
        public void execute() throws Exception {
            Date now = new Date();
            List<Change> changes = changeService.changesSince(user, since);
            int len = changes.size();
            int idx = 0;
            try {
                for (Change change : changes) {
                    if (idx >= offset) {
                        notificationsService.showOngoingNotification(R.string.notification_ongoing_message, idx, len, change.getRelativePath());
                        if (Change.Type.ADDED.equals(change.getType())) {
                            add(change);
                        } else {
                            remove(change);
                        }
                    }
                    idx++;
                }
                preferences.setSince(now);
                notificationsService.showClearableNotification(R.string.notification_success_message, len);
            } catch (Exception e) {
                preferences.setOffset(idx);
                throw e;
            }
            preferences.setOffset(0);
        }

        public void add(Change change) throws IOException, JSONException {
            List<String> changePathSegments = new ArrayList<String>(change.getRelativePath().getPathSegments());
            DocumentFile albumDir = rootDir;
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
            OutputStream out = getContentResolver().openOutputStream(trackFile.getUri());
            changeService.copyChange(change, out);
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
            DocumentFile documentFile = rootDir;
            List<String> pathSegments = new ArrayList<String>(change.getRelativePath().getPathSegments());
            do {
                String ps = pathSegments.remove(0);
                documentFile = documentFile.findFile(ps);
            } while (!pathSegments.isEmpty() && documentFile != null);
            // Only delete files if they exist.
            if (documentFile != null) {
                documentFile.delete();
                DocumentFile parent = documentFile.getParentFile();
                while (parent.listFiles().length == 0) {
                    parent.delete();
                    parent = parent.getParentFile();
                }
            }
        }
    }
}
