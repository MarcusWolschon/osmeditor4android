package de.blau.android.osm;

import java.util.Date;

public class OsmElementFactory {

	private static long wayId = 0;

	private static long nodeId = 0;

	private static long relationId = 0;

	public static Node createNode(long osmId, String user, Date dateChanged, byte status, int lat, int lon) {
		return new Node(osmId, user, dateChanged, status, lat, lon);
	}

	public static Node createNodeWithNewId(int lat, int lon) {
		return createNode(--nodeId, null, null, OsmElement.STATE_CREATED, lat, lon);
	}

	public static Way createWay(long osmId, String user, Date dateChanged, byte status) {
		return new Way(osmId, user, dateChanged, status);
	}

	public static Way createWayWithNewId() {
		return createWay(--wayId, null, null, OsmElement.STATE_CREATED);
	}

	public static Relation createRelation(long osmId, String user, Date dateChanged, byte status) {
		return new Relation(osmId, user, dateChanged, status);
	}

	public static Relation createRelationWithNewId() {
		return createRelation(--relationId, null, null, OsmElement.STATE_CREATED);
	}
}
