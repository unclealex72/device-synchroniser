package uk.co.unclealex.sync.devicesynchroniser.changes;

import lombok.Value;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class to encapsulate a music file being added or removed.
 * Created by alex on 03/12/14.
 */
@Value
public class Change {

    public static enum Type {
        ADDED, REMOVED
    }

    static Change of(JSONObject obj, String user) throws JSONException {
        return new Change(Type.valueOf(obj.getString("action").toUpperCase()), RelativePath.of(obj.getString("relativePath")), user);
    }

    private final Type type;
    private final RelativePath relativePath;
    private final String user;

}

