package uk.co.unclealex.sync.devicesynchroniser.changes;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by alex on 25/12/14.
 */
public interface ChangeService {

    int countChangesSince(String user, String since) throws IOException;

    List<Change> changesSince(String user, String since) throws IOException;

    void copyChange(Change change, OutputStream out) throws IOException;

    void loadNextChangelogPage(Changelog changelog) throws IOException;
}
