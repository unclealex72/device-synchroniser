package uk.co.unclealex.sync.devicesynchroniser.tags;

import uk.co.unclealex.sync.devicesynchroniser.changes.RelativePath;
import uk.co.unclealex.sync.devicesynchroniser.prefs.NotInitialisedException;

import java.io.IOException;

/**
 * Created by alex on 31/12/14.
 */
public interface TagsService {
    Tags loadTags(RelativePath relativePath) throws IOException, NotInitialisedException;
}
