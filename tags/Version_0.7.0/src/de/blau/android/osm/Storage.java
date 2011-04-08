package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import de.blau.android.exception.OsmException;

public class Storage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3838107046050083564L;

	private final ArrayList<Node> nodes;

	private final ArrayList<Way> ways;

	private BoundingBox bbox;

	Storage() {
		nodes = new ArrayList<Node>();
		ways = new ArrayList<Way>();
		try {
			bbox = new BoundingBox(-BoundingBox.MAX_LON, -BoundingBox.MAX_LAT, BoundingBox.MAX_LON,
					BoundingBox.MAX_LAT);
		} catch (OsmException e) {
			Log.e("Vespucci", "Problem with bounding box", e);
		}
	}

	public Node getNode(final long nodeOsmId) {
		for (int i = 0, size = nodes.size(); i < size; ++i) {
			if (nodes.get(i).getOsmId() == nodeOsmId) {
				return nodes.get(i);
			}
		}
		return null;
	}

	public Way getWay(final long wayOsmId) {
		for (int i = 0, size = ways.size(); i < size; ++i) {
			if (ways.get(i).getOsmId() == wayOsmId) {
				return ways.get(i);
			}
		}
		return null;

	}

	public OsmElement getOsmElement(final String type, final long osmId) {
		if (type.equalsIgnoreCase(Node.NAME)) {
			return getNode(osmId);
		} else if (type.equalsIgnoreCase(Way.NAME)) {
			return getWay(osmId);
		}
		return null;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public List<Way> getWays() {
		return ways;
	}

	public boolean contains(final OsmElement elem) {
		if (elem instanceof Way) {
			return ways.contains(elem);
		} else if (elem instanceof Node) {
			return nodes.contains(elem);
		}
		return false;
	}

	public Way getFirstWay(final Node node) {
		Way way = null;
		for (int i = 0, size = ways.size(); i < size; ++i) {
			way = ways.get(i);
			if (way.getNodes().contains(node)) {
				return way;
			}
		}
		return null;
	}

	/**
	 * @param node
	 * @return all ways containing that node
	 */
	public List<Way> getWays(final Node node) {
		ArrayList<Way> mWays = new ArrayList<Way>();
		for (int i = 0, size = ways.size(); i < size; ++i) {
			Way way = ways.get(i);
			if (way.hasNode(node)) {
				mWays.add(way);
			}
		}
		return mWays;
	}

	public List<Node> getWaynodes() {
		ArrayList<Node> waynodes = new ArrayList<Node>();
		for (int i = 0, size = ways.size(); i < size; ++i) {
			waynodes.addAll(ways.get(i).getNodes());
		}
		return waynodes;
	}

	void insertNodeUnsafe(final Node node) {
		nodes.add(node);
	}

	void insertWayUnsafe(final Way way) {
		ways.add(way);
	}

	void insertElementSafe(final OsmElement elem) {
		if (!contains(elem)) {
			insertElementUnsafe(elem);
		}
	}

	void insertElementUnsafe(final OsmElement elem) {
		if (elem instanceof Way) {
			insertWayUnsafe((Way) elem);
		} else if (elem instanceof Node) {
			insertNodeUnsafe((Node) elem);
		}
	}

	boolean removeNode(final Node node) {
		return nodes.remove(node);
	}

	boolean removeWay(final Way way) {
		return ways.remove(way);
	}

	boolean removeElement(final OsmElement element) {
		if (element instanceof Way) {
			return ways.remove(element);
		} else if (element instanceof Node) {
			return nodes.remove(element);
		}
		return false;
	}

	public BoundingBox getBoundingBox() {
		return bbox;
	}

	void setBoundingBox(final BoundingBox bbox) {
		this.bbox = bbox;
	}

	public boolean isEmpty() {
		return nodes.isEmpty() && ways.isEmpty();
	}

	public boolean isEndNode(final Node node) {
		for (int i = 0, size = ways.size(); i < size; ++i) {
			Way way = ways.get(i);
			if (way.isEndNode(node)) {
				return true;
			}
		}
		return false;
	}
}
