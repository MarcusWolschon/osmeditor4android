package io.vespucci.osm;

import java.util.Comparator;

/**
 * Implement NWR ordering
 * 
 * If performance is a concern an equal class test should be added as the first test
 * 
 * @author simon
 *
 */
public final class NwrComparator implements Comparator<OsmElement> {
    
    @Override
    public int compare(OsmElement o1, OsmElement o2) {

        if (o1 instanceof Node && !(o2 instanceof Node)) {
            return -1;
        }
        if (!(o1 instanceof Node) && o2 instanceof Node) {
            return 1;
        }
        if (o1 instanceof Relation && !(o2 instanceof Relation)) {
            return 1;
        }
        if (!(o1 instanceof Relation) && o2 instanceof Relation) {
            return -1;
        }
        return 0;
    }
}
