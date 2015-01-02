package uk.co.unclealex.sync.devicesynchroniser.changes;

import lombok.Data;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.unclealex.sync.devicesynchroniser.dates.Iso8601;

import java.util.Date;

/**
 * Created by alex on 26/12/14.
 */
@Data
public class ChangelogItem {

    private final RelativePath parentRelativePath;
    private final Date at;
    private final RelativePath relativePath;

    public static ChangelogItem of(JSONObject obj, Iso8601 iso8601) throws JSONException {
        RelativePath parentRelativePath = RelativePath.of(obj.getString("parentRelativePath"));
        Date at = iso8601.parse(obj.getString("at"));
        RelativePath relativePath = RelativePath.of(obj.getString("relativePath"));
        return new ChangelogItem(parentRelativePath, at, relativePath);
    }
}