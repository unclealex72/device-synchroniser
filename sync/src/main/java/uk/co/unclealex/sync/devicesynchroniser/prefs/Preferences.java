package uk.co.unclealex.sync.devicesynchroniser.prefs;

import android.support.v4.provider.DocumentFile;

import java.util.Date;

/**
 * Type safe getting and setting of preferences.
 * Created by alex on 17/12/14.
 */
public interface Preferences {

    public int getPort();

    public void setPort(int port);

    public String getUser();

    public int getOffset();

    public String getSince();

    public String getHost();

    public void setOffset(int offset);

    public void setSince(Date since);

    public DocumentFile getRootDocumentFile();
}
