package uk.co.unclealex.sync.devicesynchroniser.changes;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alex on 26/12/14.
 */
@Data
public class Changelog {

    private int totalChanges = -1;
    private List<ChangelogItem> changelogItems = new ArrayList<ChangelogItem>();


}
