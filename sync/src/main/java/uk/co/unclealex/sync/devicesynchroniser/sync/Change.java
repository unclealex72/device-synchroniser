package uk.co.unclealex.sync.devicesynchroniser.sync;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * A class to encapsulate a music file being added or removed.
 * Created by alex on 03/12/14.
 */
public class Change {

    public static enum Type {
        ADDED, REMOVED
    }

    public static Change of(JSONObject obj) throws JSONException {
        return new Change(Type.valueOf(obj.getString("action").toUpperCase()), new File(obj.getString("relativePath")));
    }

    private final Type type;
    private final File relativePath;

    private Change(Type type, File relativePath) {
        this.type = type;
        this.relativePath = relativePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Change change = (Change) o;

        if (relativePath != null ? !relativePath.equals(change.relativePath) : change.relativePath != null)
            return false;
        if (type != change.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (relativePath != null ? relativePath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Change{" +
                "type=" + type +
                ", relativePath=" + relativePath +
                '}';
    }

    public List<String> toPathSegments(String... prefixes) {
        List<String> newPathSegments = new LinkedList<String>();
        File relativePath = getRelativePath();
        do {
            newPathSegments.add(0, relativePath.getName());
            relativePath = relativePath.getParentFile();
        } while (relativePath != null);
        for (int idx = prefixes.length - 1; idx >= 0; idx--) {
            newPathSegments.add(0, prefixes[idx]);
        }
        return newPathSegments;
    }

    public Type getType() {
        return type;
    }

    public File getRelativePath() {
        return relativePath;
    }
}

