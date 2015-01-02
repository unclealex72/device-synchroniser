package uk.co.unclealex.sync.devicesynchroniser.notifications;

import android.app.Service;

/**
 * Created by alex on 21/12/14.
 */
public interface NotificationsService {

    void showOngoingNotification(int messageId, Object... args);

    void showClearableNotification(int messageId, Object... args);

    void initialiseOngoingNotification(Service service, int messageId);
}
