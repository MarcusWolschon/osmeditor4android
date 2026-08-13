package de.blau.android.osm;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import androidx.annotation.NonNull;

public class WaySegment implements WayInterface, Serializable {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, WaySegment.class.getSimpleName().length());
    private static final String DEBUG_TAG = WaySegment.class.getSimpleName().substring(0, TAG_LEN);

    private static final long serialVersionUID = 1L;

    private final Way way;
    private final int start;
    private final int end;

    /**
     * Create a way segment from an existing way
     * 
     * Essentially this is a "view" of the way, note that it makes no representations as to if the List returned from
     * getNodes is a subList or a newly constructed one.
     * 
     * @param way the original Way
     * @param start the start of the segment
     * @param end the end of the segment
     */
    public WaySegment(@NonNull Way way, int start, int end) {
        Log.d(DEBUG_TAG, way.getDescription() + " start:" + start + " end:" + end);
        this.way = way;
        this.start = start;
        this.end = end;
    }

    @Override
    public List<Node> getNodes() {
        final List<Node> nodes = way.getNodes();
        if (end >= start) {
            return nodes.subList(start, end + 1);
        }
        // wraps over the end of the way, allocate a new List
        List<Node> result = new ArrayList<>(nodes.subList(start, way.nodeCount()));
        result.addAll(nodes.subList(0, end + 1));
        return result;
    }

    @Override
    public int nodeCount() {
        return end >= start ? end - start + 1 : way.nodeCount() - start + end;
    }

    @Override
    public boolean isClosed() {
        final List<Node> nodes = way.getNodes();
        return nodes.get(start).equals(nodes.get(end));
    }

    @Override
    public double length() {
        return Way.length(getNodes());
    }
}
