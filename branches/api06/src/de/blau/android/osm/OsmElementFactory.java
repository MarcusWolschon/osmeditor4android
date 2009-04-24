package de.blau.android.osm;

public class OsmElementFactory {

	private static long wayId = 0;

	private static long nodeId = 0;

	public static Node createNode(long osmId, long osmVersion, byte status, int lat, int lon) {
		return new Node(osmId, osmVersion, status, lat, lon);
	}

	public static Node createNodeWithNewId(int lat, int lon) {
		return createNode(--nodeId, 0, OsmElement.STATE_CREATED, lat, lon);
	}// TODO: There's no osmVersion when creating new node !

	public static Way createWay(long osmId, long osmVersion, byte status) {
		return new Way(osmId, osmVersion, status);
	}

	public static Way createWayWithNewId() {
		return createWay(--wayId, 0, OsmElement.STATE_CREATED);
	}// TODO: There's no osmVersion when creating new way !
}
