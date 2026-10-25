package de.blau.android.osm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import de.blau.android.gpx.TrackPoint;

public final class Gpx {

    private Gpx() {
        // disallow instantiation
    }

    /**
     * Convert a List of GPX TrackPoints points to one or more OSM ways
     * 
     * Handles closing ways and splitting them in to multiple ways if too "long"
     * 
     * @param factory an instance of OsmFactory
     * @param coordinates the TrackPoints
     * @param maxNodes maximum number of Nodes in an OSM Way
     * @return a List of OsmELements (this includes the way Nodes)
     */
    @NonNull
    public static List<OsmElement> trackPointsToWays(@NonNull OsmElementFactory factory, @NonNull List<TrackPoint> coordinates, int maxNodes) {
        List<OsmElement> result = new ArrayList<>();
        Way way = factory.createWayWithNewId();
        int nodeCount = 0;
        final int lastIndex = coordinates.size() - 1;
        boolean closed = coordinates.get(0).equals(coordinates.get(lastIndex));
        List<TrackPoint> points = closed ? coordinates.subList(0, lastIndex) : coordinates;
        TrackPoint lastPoint = points.get(points.size() - 1);
        for (TrackPoint p : points) {
            Node n = factory.createNodeWithNewId(p.getLat(), p.getLon());
            way.addNode(n);
            nodeCount++;
            if (nodeCount >= maxNodes && !p.equals(lastPoint)) {
                result.add(way);
                way = factory.createWayWithNewId();
                way.addNode(n);
                nodeCount = 1;
            }
        }
        result.add(way);
        if (closed) {
            way.addNode(((Way) result.get(0)).getFirstNode());
        }
        // now add the nodes
        Set<Node> nodes = new HashSet<>();
        for (OsmElement e : result) {
            nodes.addAll(((Way) e).getNodes());
        }
        result.addAll(0, nodes);
        return result;
    }
}
