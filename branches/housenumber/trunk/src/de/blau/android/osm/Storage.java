package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.blau.android.exception.OsmException;

public class Storage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3838107046050083564L;

	private final ArrayList<Node> nodes;

	private final ArrayList<Way> ways;

	private final ArrayList<Relation> relations;

	private BoundingBox bbox;

	Storage() {
		this.nodes = new ArrayList<Node>();
		this.ways = new ArrayList<Way>();
		this.relations = new ArrayList<Relation>();
		try {
			this.bbox = new BoundingBox(-BoundingBox.MAX_LON, -BoundingBox.MAX_LAT, BoundingBox.MAX_LON,
					BoundingBox.MAX_LAT);
		} catch (OsmException e) {
			e.printStackTrace();
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

	public Relation getRelation(final long relationOsmId) {
		for (int i = 0, size = relations.size(); i < size; ++i) {
			if (relations.get(i).getOsmId() == relationOsmId) {
				return relations.get(i);
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
		} else if (type.equalsIgnoreCase(Relation.NAME)) {
			return getRelation(osmId);
		}
		return null;
	}

	public List<Node> getNodes() {
		return Collections.unmodifiableList(nodes);
	}

	public List<Relation> getRelations() {
		return Collections.unmodifiableList(relations);
	}

	public List<Way> getWays() {
		return Collections.unmodifiableList(ways);
	}

	public boolean contains(final OsmElement elem) {
		if (elem instanceof Way) {
			return ways.contains(elem);
		} else if (elem instanceof Node) {
			return nodes.contains(elem);
		} else if (elem instanceof Relation) {
			return relations.contains(elem);
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

	void insertRelationUnsafe(final Relation relation) {
		relations.add(relation);
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
		} else if (elem instanceof Relation) {
			insertRelationUnsafe((Relation) elem);
		}
	}

	boolean removeNode(final Node node) {
		return nodes.remove(node);
	}

	boolean removeWay(final Way way) {
		return ways.remove(way);
	}

	boolean removeRelation(final Relation relation) {
		return relations.remove(relation);
	}

	boolean removeElement(final OsmElement element) {
		if (element instanceof Way) {
			return ways.remove(element);
		} else if (element instanceof Node) {
			return nodes.remove(element);
		} else if (element instanceof Relation) {
			return relations.remove(element);
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
		return nodes.isEmpty() && ways.isEmpty() && relations.isEmpty();
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
