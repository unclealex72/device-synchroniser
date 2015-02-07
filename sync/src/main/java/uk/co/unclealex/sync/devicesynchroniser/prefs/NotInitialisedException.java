package uk.co.unclealex.sync.devicesynchroniser.prefs;

/**
 * Created by alex on 07/02/15.
 */
public class NotInitialisedException extends Exception {

    public NotInitialisedException() {
    }

    public NotInitialisedException(String detailMessage) {
        super(detailMessage);
    }

    public NotInitialisedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NotInitialisedException(Throwable throwable) {
        super(throwable);
    }
}
