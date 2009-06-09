package de.blau.android.osm;

public class OsmElementFactory {

	private static long wayId = 0;

	private static long nodeId = 0;

	public static Node createNode(long osmId, byte status, int lat, int lon) {
		return new Node(osmId, status, lat, lon);
	}

	public static Node createNodeWithNewId(int lat, int lon) {
		return createNode(--nodeId, OsmElement.STATE_CREATED, lat, lon);
	}

	public static Way createWay(long osmId, byte status) {
		return new Way(osmId, status);
	}

	public static Way createWayWithNewId() {
		return createWay(--wayId, OsmElement.STATE_CREATED);
	}
}
