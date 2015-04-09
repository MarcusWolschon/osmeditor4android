package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import android.util.Log;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.StorageException;

public class Storage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3838107046050083565L;

	private final ArrayList<Node> nodes;

	private final ArrayList<Way> ways;
	
	private final ArrayList<Relation> relations;

	private List<BoundingBox> bboxes;

	Storage() {
		nodes = new ArrayList<Node>();
		ways = new ArrayList<Way>();
		relations = new ArrayList<Relation>();
		try {
			bboxes = Collections.synchronizedList(new ArrayList<BoundingBox>());
			// a default entry may not make sense
			bboxes.add(new BoundingBox(-BoundingBox.MAX_LON, -BoundingBox.MAX_LAT_E7, BoundingBox.MAX_LON,
					BoundingBox.MAX_LAT_E7));
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
	
	public Relation getRelation(final long relationOsmId) {
		for (int i = 0, size = relations.size(); i < size; ++i) {
            if (relations.get(i).getOsmId() == relationOsmId) {
                    return relations.get(i);
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

	/**
	 * @return
	 */
	public List<Node> getNodes() {
		return nodes;
	}

	/**
	 * @return
	 */
	public List<Way> getWays() {
		return ways;
	}	
	
	/**
	 * @return
	 */
	public List<Relation> getRelations() {
		return relations;
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

	/**
	 * maintaining a reference list in the node would make more sense
	 * @param node
	 * @return
	 */
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

	void insertNodeUnsafe(final Node node) throws StorageException {
		try {
			nodes.add(node);
		} catch (Error err) { // should really only be OutOfMemory
			throw new StorageException(StorageException.OOM);
		}

	}

	void insertWayUnsafe(final Way way)  throws StorageException  {
		try {
			ways.add(way);
		} catch (Error err) { // should really only be OutOfMemory
			throw new StorageException(StorageException.OOM);
		}
	}

	void insertRelationUnsafe(final Relation relation) throws StorageException  {
		try {
			relations.add(relation);
		} catch (Error err) { // should really only be OutOfMemory
			throw new StorageException(StorageException.OOM);
		}
	}
	
	void insertElementSafe(final OsmElement elem) throws StorageException {
		if (!contains(elem)) {
			insertElementUnsafe(elem);
		}
	}

	void insertElementUnsafe(final OsmElement elem) throws StorageException {
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

	public List<BoundingBox> getBoundingBoxes() {
		return bboxes;
	}

	/**
	 * Resets boundingbox list and adds this boundingbox
	 * @param bbox
	 */
	void setBoundingBox(final BoundingBox bbox) {
		this.bboxes = new ArrayList<BoundingBox>();
		this.bboxes.add(bbox);
	}
	
	/**�
	 * Add this boundingbox to list
	 * @param bbox
	 */
	void addBoundingBox(final BoundingBox bbox) {
		if (this.bboxes == null)
			this.bboxes = new ArrayList<BoundingBox>();
		this.bboxes.add(bbox);
	}

	/**
	 * Return true if this storage is empty
	 * @return true if empty
	 */
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

	public void logStorage() {
		// 
		for (Node n:nodes) {
			Log.d("Storage","Node " + n.getOsmId());
			for (String k:n.getTags().keySet()) {
				Log.d("Storage",k + "=" + n.getTags().get(k));
			}
		}
		for (Way w:ways) {
			Log.d("Storage","Way " + w.getOsmId());
			for (String k:w.getTags().keySet()) {
				Log.d("Storage",k + "=" + w.getTags().get(k));
			}
		}
	}

	/**
	 * Calculate a bounding box just covering the data
	 * @return
	 * @throws OsmException
	 */
	public BoundingBox calcBoundingBoxFromData() throws OsmException {
		int top = 0;
		int bottom = 0;
		int left = 0;
		int right = 0;
		
		if (nodes != null) {
			for (Node n:nodes) {
				if (n.getLat() > top) top = n.getLat();
				else if (n.getLat() < bottom) bottom = n.getLat();
				if (n.getLon() > right) right = n.getLon();
				else if (n.getLon() < left) left = n.getLon();
			}
		}
		BoundingBox result  = new BoundingBox(left, bottom, right, top);
		return result;
	}
}
