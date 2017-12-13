package de.blau.android.osm;

import java.io.Serializable;

public class OsmElementFactory implements Serializable {

    private static final long serialVersionUID = 2L;

    private long wayId = 0;

    private long nodeId = 0;

    private long relationId = -2; // -1 might have a magic meaning

    /**
     * Create a node from parameters
     * 
     * @param osmId id to use
     * @param osmVersion version of the object
     * @param timestamp timestamp (seconds since the UNIX epoch)
     * @param status status (created, modified, deleted)
     * @param lat WGS84 decimal Latitude-Coordinate times 1E7.
     * @param lon WGS84 decimal Longitude-Coordinate times 1E7.
     * @return the new Node
     */
    public static Node createNode(long osmId, long osmVersion, long timestamp, byte status, int lat, int lon) {
        return new Node(osmId, osmVersion, timestamp, status, lat, lon);
    }

    /**
     * Create a new node with a temporary id, suitable for uploading to the API to receive a proper OSM id
     * 
     * @param lat WGS84 decimal Latitude-Coordinate times 1E7.
     * @param lon WGS84 decimal Longitude-Coordinate times 1E7.
     * @return the new Node
     */
    public Node createNodeWithNewId(int lat, int lon) {
        return createNode(--nodeId, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, lat, lon);
    }

    /**
     * Create a way from parameters
     * 
     * Note: way nodes need to be added before the way can actually be used
     * 
     * @param osmId id to use
     * @param osmVersion version of the object
     * @param timestamp timestamp (seconds since the UNIX epoch)
     * @param status status (created, modified, deleted)
     * @return the new way
     */
    public static Way createWay(long osmId, long osmVersion, long timestamp, byte status) {
        return new Way(osmId, osmVersion, timestamp, status);
    }

    /**
     * Create a new way with a temporary id, suitable for uploading to the API to receive a proper OSM id
     * 
     * Note: way nodes need to be added before the way can actually be used
     * 
     * @return the new way
     */
    public Way createWayWithNewId() {
        return createWay(--wayId, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED);
    }

    /**
     * Create a relation from parameters
     * 
     * @param osmId id to use
     * @param osmVersion version of the object
     * @param timestamp timestamp (seconds since the UNIX epoch)
     * @param status status (created, modified, deleted)
     * @return the new relation
     */
    public static Relation createRelation(long osmId, long osmVersion, long timestamp, byte status) {
        return new Relation(osmId, osmVersion, timestamp, status);
    }

    /**
     * Create a new relation with a temporary id, suitable for uploading to the API to receive a proper OSM id
     * 
     * @return the new relation
     */
    public Relation createRelationWithNewId() {
        return createRelation(--relationId, 1, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED);
    }

    /**
     * Set the initial id values to use
     * 
     * @param n initial node id
     * @param w initial way id
     * @param r initial relation id
     */
    public synchronized void setIdSequences(long n, long w, long r) {
        nodeId = n < nodeId ? n : nodeId;
        wayId = w < wayId ? w : wayId;
        relationId = r < relationId ? r : relationId;
    }
}
