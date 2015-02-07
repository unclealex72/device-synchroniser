package uk.co.unclealex.sync.devicesynchroniser.main;

import uk.co.unclealex.sync.devicesynchroniser.changes.Changelog;
import uk.co.unclealex.sync.devicesynchroniser.changes.RelativePath;
import uk.co.unclealex.sync.devicesynchroniser.prefs.NotInitialisedException;
import uk.co.unclealex.sync.devicesynchroniser.tags.Tags;

import java.io.IOException;

/**
 * Created by alex on 21/12/14.
 */
public interface MainPresenter {

    /**
     * Synchronise the music files on this device.
     */
    public void synchronise();

    public void updateLastSynchronisedTime();

    public void updatePendingChanges();

    public Tags loadTags(RelativePath relativePath) throws IOException, NotInitialisedException;

    public void loadNextChangelogPage(Changelog changelog) throws IOException;
}
