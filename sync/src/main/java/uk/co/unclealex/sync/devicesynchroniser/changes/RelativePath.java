package uk.co.unclealex.sync.devicesynchroniser.changes;

import lombok.Data;

import java.io.File;
import java.util.*;

/**
 * Created by alex on 26/12/14.
 */
@Data
public class RelativePath {

    private final List<String> pathSegments;

    public static RelativePath of(String path) {
        File file = new File(path);
        List<String> newPathSegments = new LinkedList<String>();
        do {
            newPathSegments.add(0, file.getName());
            file = file.getParentFile();
        } while (file != null);
        return new RelativePath(Collections.unmodifiableList(newPathSegments));
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Iterator<String> iter = getPathSegments().iterator(); iter.hasNext(); ) {
            String segment = iter.next();
            builder.append(segment);
            if (iter.hasNext()) {
                builder.append('/');
            }
        }
        return builder.toString();
    }

    public List<String> prefixedWith(String... pathSegments) {
        List<String> segments = new ArrayList<String>();
        segments.addAll(Arrays.asList(pathSegments));
        segments.addAll(getPathSegments());
        return Collections.unmodifiableList(segments);
    }
}
