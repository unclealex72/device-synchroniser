package uk.co.unclealex.sync.devicesynchroniser.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import uk.co.unclealex.sync.devicesynchroniser.main.MainActivity;
import uk.co.unclealex.sync.devicesynchroniser.sync.R;

/**
 * Created by alex on 21/12/14.
 */
public class NotificationsServiceImpl implements NotificationsService {

    private static final int ONGOING_NOTIFICATION_ID = 1;
    private static final int FINISHED_NOTIFICATION_ID = 2;


    private final NotificationManager notificationManager;
    private final Context context;

    public NotificationsServiceImpl(Context context, NotificationManager notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
    }

    public Notification notificationOf(int messageId, boolean ongoing, Object... args) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        return new NotificationCompat.Builder(context)
                .setContentTitle(context.getText(R.string.notification_title))
                .setContentText(context.getString(messageId, args))
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing)
                .setSmallIcon(R.drawable.ic_action_sync).build();
    }

    public void showNotification(int messageId, int notificationId, Object... args) {
        notificationManager.notify(notificationId, notificationOf(messageId, notificationId == ONGOING_NOTIFICATION_ID, args));
    }

    @Override
    public void showOngoingNotification(int messageId, Object... args) {
        showNotification(messageId, ONGOING_NOTIFICATION_ID, args);
    }

    @Override
    public void showClearableNotification(int messageId, Object... args) {
        showNotification(messageId, FINISHED_NOTIFICATION_ID, args);
    }

    @Override
    public void initialiseOngoingNotification(Service service, int messageId) {
        service.startForeground(ONGOING_NOTIFICATION_ID, notificationOf(messageId, true));
    }
}
