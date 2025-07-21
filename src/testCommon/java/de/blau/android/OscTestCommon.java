package de.blau.android;

import java.util.List;

import org.junit.Assert;

import androidx.annotation.NonNull;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;

public final class OscTestCommon {

    public static final String OSM_FILE = "osctest1.osm";
    public static final String OSC_FILE = "osctest1.osc";

    /**
     * Check that the data is in the state after the merge we think it should be
     * 
     * @param delegator the StorageDelegator
     */
    public static void checkNewState(@NonNull StorageDelegator delegator) {
        Way w = (Way) delegator.getOsmElement(Way.NAME, 210558045L);
        Node n = (Node) delegator.getOsmElement(Node.NAME, 416426220L);
        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 1638705L);
        // check new data state
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Way.NAME, 210558043L));
        Assert.assertNull(delegator.getCurrentStorage().getOsmElement(Way.NAME, 210558043L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Way.NAME, 210558043L).getState());
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Node.NAME, 2206392996L));
        Assert.assertNull(delegator.getCurrentStorage().getOsmElement(Node.NAME, 2206392996L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Node.NAME, 2206392996L).getState());
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Node.NAME, 2206392994L));
        Assert.assertNull(delegator.getCurrentStorage().getOsmElement(Node.NAME, 2206392996L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Node.NAME, 2206392994L).getState());
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Node.NAME, 2206392992L));
        Assert.assertNull(delegator.getCurrentStorage().getOsmElement(Node.NAME, 2206392992L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Node.NAME, 2206392992L).getState());
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Node.NAME, 2206392993L));
        Assert.assertNull(delegator.getCurrentStorage().getOsmElement(Node.NAME, 2206392993L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Node.NAME, 2206392993L).getState());
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Node.NAME, 2206392996L));
        Assert.assertNull(delegator.getCurrentStorage().getOsmElement(Node.NAME, 2206392993L));
        Assert.assertEquals(OsmElement.STATE_DELETED, delegator.getApiStorage().getOsmElement(Node.NAME, 2206392996L).getState());

        // placeholder ids are renumbered on input so we need to find the Way some other way
        List<Way> ways = delegator.getCurrentStorage().getWays(new BoundingBox(8.3891745, 47.3899902, 8.3894486, 47.3901275));
        for (Way way : ways) {
            if (way.getOsmId() < 0) {
                w = way;
            }
        }
        Assert.assertNotNull(w);
        Assert.assertNotNull(delegator.getApiStorage().getOsmElement(Way.NAME, w.getOsmId()));
        Assert.assertEquals(OsmElement.STATE_CREATED, w.getState());
        Assert.assertEquals(4, w.getNodes().size());
        Assert.assertTrue(w.isClosed());
        Assert.assertTrue(w.hasTag("name", "new"));

        w = (Way) delegator.getApiStorage().getOsmElement(Way.NAME, 210558045L);
        Assert.assertNotNull(w);
        Assert.assertEquals(OsmElement.STATE_MODIFIED, w.getState());
        Assert.assertTrue(w.hasTag("addr:housenumber", "444"));

        n = (Node) delegator.getApiStorage().getOsmElement(Node.NAME, 416426220L);
        Assert.assertNotNull(n);
        Assert.assertEquals(OsmElement.STATE_MODIFIED, n.getState());
        Assert.assertEquals(47.3898126D, n.getLat() / 1E7D, 0.00000001);
        Assert.assertEquals(8.3894851D, n.getLon() / 1E7D, 0.00000001);

        r = (Relation) delegator.getOsmElement(Relation.NAME, 1638705L);
        Assert.assertEquals(OsmElement.STATE_MODIFIED, r.getState());
        Assert.assertNull(r.getMember(Way.NAME, 119104094L));
    }

    /**
     * Check that the data is in the state we think it should be
     * 
     * @param delegator the StorageDelegator
     */
    public static void checkInitialState(@NonNull StorageDelegator delegator) {
        // check initial data state
        Assert.assertNotNull(delegator.getOsmElement(Way.NAME, 210558043L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 2206392996L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 2206392994L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 2206392992L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 2206392993L));
        Assert.assertNotNull(delegator.getOsmElement(Node.NAME, 2206392996L));

        Way w = (Way) delegator.getOsmElement(Way.NAME, 210558045L);
        Assert.assertNotNull(w);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, w.getState());
        Assert.assertTrue(w.hasTag("addr:housenumber", "4"));

        Node n = (Node) delegator.getOsmElement(Node.NAME, 416426220L);
        Assert.assertNotNull(n);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        System.out.println("Lat " + n.getLat()); // NOSONAR
        Assert.assertEquals(47.3898033D, n.getLat() / 1E7D, 0.00000001);
        Assert.assertEquals(8.3888382D, n.getLon() / 1E7D, 0.00000001);

        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 1638705L);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, r.getState());
        Assert.assertNotNull(r.getMember(Way.NAME, 119104094L));
        Way w2 = (Way) delegator.getOsmElement(Way.NAME, 119104094L);
        Assert.assertNotNull(w2);
        Assert.assertTrue(w2.getParentRelations().contains(r));
    }
}
