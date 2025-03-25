package io.vespucci.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.annotation.NonNull;
import bentleyottmann.BentleyOttmann;
import bentleyottmann.IPoint;
import bentleyottmann.ISegment;
import io.vespucci.osm.Node;
import io.vespucci.osm.Way;

public final class BentleyOttmannForOsm {
    private static Map<Long, Coordinates> nodeMap = new HashMap<>();

    /**
     * Private constructor
     */
    private BentleyOttmannForOsm() {
        // empty
    }

    static class Segment implements ISegment {
        IPoint p1;
        IPoint p2;

        /**
         * Construct a new segment
         * 
         * @param p1 1st point
         * @param p2 2nd point
         */
        Segment(@NonNull IPoint p1, @NonNull IPoint p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public @Nullable String name() {
            return null;
        }

        @Override
        public @NotNull IPoint p1() {
            return p1;
        }

        @Override
        public @NotNull IPoint p2() {
            return p2;
        }
    }

    /**
     * Find intersections between Ways
     * 
     * @param ways a List of Ways
     * @return a List of Mercator Coordinates for intersections
     */
    @NonNull
    public static List<Coordinates> findIntersections(@NonNull List<Way> ways) {
        final BentleyOttmann bentleyOttmann = new BentleyOttmann(Coordinates::new);
        final List<ISegment> segments = new ArrayList<>();
        for (Way w : ways) {
            List<Node> nodes = w.getNodes();
            for (int i = 0; i < nodes.size() - 1; i++) {
                Coordinates c1 = getCoordinates(nodes.get(i));
                Coordinates c2 = getCoordinates(nodes.get(i + 1));
                segments.add(new Segment(c1, c2));
            }
        }
        bentleyOttmann.addSegments(segments);
        bentleyOttmann.findIntersections();
        List<IPoint> intersections = bentleyOttmann.intersections();
        List<Coordinates> coordinates = new ArrayList<>();
        for (IPoint p : intersections) {
            coordinates.add(new Coordinates(p.x(), p.y()));
        }
        return coordinates;
    }

    /**
     * Get Coordinates object with Mercator coords for a Node
     * 
     * @param n the Node
     * @return Coordinates
     */
    @NonNull
    private static Coordinates getCoordinates(@NonNull Node n) {
        Coordinates c = nodeMap.get(n.getOsmId());
        if (c == null) {
            c = new Coordinates(n.getLon() / 1E7D, GeoMath.latE7ToMercator(n.getLat()));
            nodeMap.put(n.getOsmId(), c);
        }
        return c;
    }
}
