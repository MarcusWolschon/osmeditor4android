package io.vespucci.util;

import java.util.List;

import androidx.annotation.NonNull;
import io.vespucci.osm.Node;

public final class Winding {
    public static final int COLINEAR         = 0;
    public static final int CLOCKWISE        = -1;
    public static final int COUNTERCLOCKWISE = 1;

    /**
     * Private constructor to stop instantation
     */
    private Winding() {
        // private
    }

    /**
     * Determine winding of a List of Nodes
     * 
     * @param nodes the List of Nodes (must contain at least one)
     * @return an int indicating winding direction
     */
    public static int winding(@NonNull List<Node> nodes) {
        double area = 0;
        int s = nodes.size();
        Node n1 = nodes.get(0);
        int lat1 = n1.getLat();
        int lon1 = n1.getLon();
        int size = nodes.size();
        for (int i = 0; i < size; i++) {
            Node n2 = nodes.get((i + 1) % s);
            int lat2 = n2.getLat();
            int lon2 = n2.getLon();
            area = area + (double) (lat2 - lat1) * (double) (lon2 + lon1);
            lat1 = lat2;
            lon1 = lon2;
        }
        return area < 0 ? CLOCKWISE : area > 0 ? COUNTERCLOCKWISE : COLINEAR;
    }
}
