package io.vespucci.osm;

import androidx.annotation.NonNull;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElementFactory;
import io.vespucci.osm.Relation;
import io.vespucci.osm.RelationMember;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Way;

public class DelegatorUtil {

    /**
     * Add a test way to storage and return it
     * 
     * @param d the StorageDelegator instance
     * @param close if true close the way
     * @return the way
     */
    public static Way addWayToStorage(@NonNull StorageDelegator d, boolean close) {
        d.getUndo().createCheckpoint("add test way", null);
        OsmElementFactory factory = d.getFactory();
        Way w = factory.createWayWithNewId();
        Node n0 = factory.createNodeWithNewId(toE7(51.478), toE7(0));
        d.insertElementSafe(n0);
        w.addNode(n0);
        Node n1 = factory.createNodeWithNewId(toE7(51.478), toE7(0.003));
        d.insertElementSafe(n1);
        w.addNode(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.003));
        d.insertElementSafe(n2);
        w.addNode(n2);
        Node n3 = factory.createNodeWithNewId(toE7(51.476), toE7(0));
        d.insertElementSafe(n3);
        w.addNode(n3);
        if (close) {
            w.addNode(n0); // close
        }
        d.insertElementSafe(w);
        Relation r = factory.createRelationWithNewId();
        RelationMember member = new RelationMember("test", w);
        r.addMember(member);
        d.insertElementSafe(r);
        w.addParentRelation(r);
        return w;
    }
    
    /**
     * Convert to scaled int representation
     * 
     * @param d double coordinate value
     * @return a scaled int
     */
    public static int toE7(double d) {
        return (int) (d * 1E7);
    }
}
