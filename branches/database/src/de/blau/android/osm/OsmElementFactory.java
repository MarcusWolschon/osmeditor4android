package de.blau.android.osm;

import de.blau.android.osm.OsmElement.State;

public class OsmElementFactory {

	private static long wayId = 0;

	private static long nodeId = 0;

	public static Node createNode(long osmId, State status, int lat, int lon) {
		return new Node(osmId, status, lat, lon);
	}

	public static Node createNodeWithNewId(int lat, int lon) {
		return createNode(--nodeId, State.CREATED, lat, lon);
	}

	public static Way createWay(long osmId, State state) {
		return new Way(osmId, state);
	}

	public static Way createWayWithNewId() {
		return createWay(--wayId, State.CREATED);
	}
}
