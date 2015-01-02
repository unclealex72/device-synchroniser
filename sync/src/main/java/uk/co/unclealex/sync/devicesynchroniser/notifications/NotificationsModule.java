package uk.co.unclealex.sync.devicesynchroniser.notifications;

import android.app.NotificationManager;
import android.content.Context;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Created by alex on 17/12/14.
 */
@Module(
        complete = false,
        library = true
)
public class NotificationsModule {

    @Provides
    @Singleton
    public NotificationManager provideNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Provides
    @Singleton
    public NotificationsService provideNotificationsService(Context context, NotificationManager notificationManager) {
        return new NotificationsServiceImpl(context, notificationManager);
    }
}