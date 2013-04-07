package de.blau.android.osm;

import java.io.Serializable;

public class OsmElementFactory implements Serializable {

	private static final long serialVersionUID = 1L;

	private long wayId = 0;

	private long nodeId = 0;

	public static Node createNode(long osmId, long osmVersion, byte status, int lat, int lon) {
		return new Node(osmId, osmVersion, status, lat, lon);
	}

	public Node createNodeWithNewId(int lat, int lon) {
		return createNode(--nodeId, 1, OsmElement.STATE_CREATED, lat, lon);
	}

	public static Way createWay(long osmId, long osmVersion, byte status) {
		return new Way(osmId, osmVersion, status);
	}

	public Way createWayWithNewId() {
		return createWay(--wayId, 1, OsmElement.STATE_CREATED);
	}
	
	public static Relation createRelation(long osmId, long osmVersion, byte status) {
		return new Relation(osmId, osmVersion, status);
	}

	public Relation createRelationWithNewId() {
		return createRelation(--wayId, 1, OsmElement.STATE_CREATED);
	}
}
