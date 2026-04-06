package de.blau.android.util;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.annotation.NonNull;
import bentleyottmann.BentleyOttmann;
import bentleyottmann.IPoint;
import bentleyottmann.ISegment;
import de.blau.android.osm.Node;
import de.blau.android.osm.Way;

public final class BentleyOttmannForOsm {

    /**
     * Private constructor
     */
    private BentleyOttmannForOsm() {
        // empty
    }

    private static class Segment implements ISegment {
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
        Coordinates offset = null; // offset of first node from origin
        for (Way w : ways) {
            List<Node> nodes = w.getNodes();
            if (offset == null) {
                offset = getCoordinates(nodes.get(0));
            }
            addSegmentsFromNodes(segments, nodes, offset);
        }
        bentleyOttmann.addSegments(segments);
        bentleyOttmann.findIntersections();
        List<IPoint> intersections = bentleyOttmann.intersections();
        List<Coordinates> coordinates = new ArrayList<>();
        for (IPoint p : intersections) {
            coordinates.add(new Coordinates(p.x(), p.y()).add(offset)); // NOSONAR
        }
        return coordinates;
    }

    /**
     * Add new segments from a list of nodes
     * 
     * @param segments target list of segments
     * @param nodes list of nodes
     * @param offset
     */
    private static void addSegmentsFromNodes(@NonNull final List<ISegment> segments, @NonNull List<Node> nodes, @NonNull Coordinates offset) {
        for (int i = 0; i < nodes.size() - 1; i++) {
            Coordinates c1 = getCoordinates(nodes.get(i)).subtract(offset);
            Coordinates c2 = getCoordinates(nodes.get(i + 1)).subtract(offset);
            segments.add(new Segment(c1, c2));
        }
    }

    /**
     * Check if the polygon defined by a list of nodes is self intersecting
     * 
     * @param nodes a List of Node
     * @return true if self intersecting
     */
    @NonNull
    public static boolean isSelfIntersecting(@NonNull List<Node> nodes) {
        final BentleyOttmann bentleyOttmann = new BentleyOttmann(Coordinates::new);
        final List<ISegment> segments = new ArrayList<>();
        addSegmentsFromNodes(segments, nodes, getCoordinates(nodes.get(0)));
        bentleyOttmann.addSegments(segments);
        bentleyOttmann.findIntersections();
        return !bentleyOttmann.intersections().isEmpty();
    }

    /**
     * Get Coordinates object with Mercator coords for a Node
     * 
     * @param n the Node
     * @return Coordinates
     */
    @NonNull
    private static Coordinates getCoordinates(@NonNull Node n) {
        return new Coordinates(n.getLon() / 1E7D, GeoMath.latE7ToMercator(n.getLat()));
    }
}
