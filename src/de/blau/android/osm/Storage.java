package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.StorageException;
import de.blau.android.util.collections.LongOsmElementMap;

public class Storage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3838107046050083566L;

	private final LongOsmElementMap<Node> nodes;

	private final LongOsmElementMap<Way> ways;
	
	private final LongOsmElementMap<Relation> relations;

	private List<BoundingBox> bboxes;

	Storage() {
		nodes = new LongOsmElementMap<Node>(1000);
		ways = new LongOsmElementMap<Way>();
		relations = new LongOsmElementMap<Relation>();
		try {
			bboxes = Collections.synchronizedList(new ArrayList<BoundingBox>());
			// a default entry may not make sense
			bboxes.add(new BoundingBox(-BoundingBox.MAX_LON_E7, -BoundingBox.MAX_LAT_E7, BoundingBox.MAX_LON_E7,
					BoundingBox.MAX_LAT_E7));
		} catch (OsmException e) {
			Log.e("Vespucci", "Problem with bounding box", e);
		}
	}
	
	Storage(Storage s) {
		nodes = new LongOsmElementMap<Node>(s.nodes);
		ways = new LongOsmElementMap<Way>(s.ways);
		relations = new LongOsmElementMap<Relation>(s.relations);
		bboxes = Collections.synchronizedList(new ArrayList<BoundingBox>(s.bboxes));
	}

	public Node getNode(final long nodeOsmId) { 
		return nodes.get(nodeOsmId);
	}

	public Way getWay(final long wayOsmId) {
		return ways.get(wayOsmId);
	}
	
	public Relation getRelation(final long relationOsmId) {
        return relations.get(relationOsmId);
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
		return Collections.unmodifiableList(nodes.values());
	}

	/**
	 * Return all nodes in a bounding box, currently this does a sequential scan of all nodes
	 * @param viewBox
	 * @return
	 */
	public List<Node> getNodes(BoundingBox viewBox) {
		ArrayList<Node> result = new ArrayList<Node>(nodes.size());
		for (Node n:nodes) {
			if (viewBox.isIn(n.getLat(), n.getLon())) {
				result.add(n);
			}	
		}
		return result;
	}
	/**
	 * @return
	 */
	public List<Way> getWays() {
		return Collections.unmodifiableList(ways.values());
	}	
	
	/**
	 * @return
	 */
	public List<Relation> getRelations() {
		return Collections.unmodifiableList(relations.values());
	}
	
	/**
	 * Get a unmodifiable list of all elements
	 * @return list containing all elements
	 */
	public List<OsmElement> getElements() {
		List<OsmElement> l = new ArrayList<OsmElement>();
		l.addAll(nodes.values());
		l.addAll(ways.values());
		l.addAll(relations.values());
		return Collections.unmodifiableList(l);
	}

	public boolean contains(final OsmElement elem) {
		if (elem instanceof Way) {
            return ways.containsKey(elem.getOsmId());
		} else if (elem instanceof Node) {
            return nodes.containsKey(elem.getOsmId());
		} else if (elem instanceof Relation) {
			return relations.containsKey(elem.getOsmId());
		}
		return false;
	}

	void insertNodeUnsafe(final Node node) throws StorageException {
		try {
			nodes.put(node.getOsmId(),node);
		} catch (Error err) { // should really only be OutOfMemory
			throw new StorageException(StorageException.OOM);
		}

	}

	void insertWayUnsafe(final Way way)  throws StorageException  {
		try {
			ways.put(way.getOsmId(),way);
		} catch (Error err) { // should really only be OutOfMemory
			throw new StorageException(StorageException.OOM);
		}
	}

	void insertRelationUnsafe(final Relation relation) throws StorageException  {
		try {
			relations.put(relation.getOsmId(),relation);
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
		return nodes.remove(node.getOsmId())!=null;
	}

	boolean removeWay(final Way way) {
		return ways.remove(way.getOsmId())!=null;
	}

	boolean removeRelation(final Relation relation) {
		return relations.remove(relation.getOsmId())!=null;
	}
	
	boolean removeElement(final OsmElement element) {
		if (element instanceof Way) {
			return ways.remove(element.getOsmId())!=null;
		} else if (element instanceof Node) {
			return nodes.remove(element.getOsmId())!=null;
		} else if (element instanceof Relation) {
			return relations.remove(element.getOsmId())!=null;
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
		this.bboxes = Collections.synchronizedList(new ArrayList<BoundingBox>());
		this.bboxes.add(bbox);
	}
	
	/**
	 * Add this boundingbox to list
	 * @param bbox
	 */
	void addBoundingBox(final BoundingBox bbox) {
		if (this.bboxes == null)
			this.bboxes = Collections.synchronizedList(new ArrayList<BoundingBox>());
		this.bboxes.add(bbox);
	}
	
	/**
	 * Remove boundingbox from list
	 * @param box
	 */
	public void deleteBoundingBox(BoundingBox box) {
		if (this.bboxes != null) {
			this.bboxes.remove(box);
		}
	}

	/**
	 * Return true if this storage is empty
	 * @return true if empty
	 */
	public boolean isEmpty() {
		return nodes.isEmpty() && ways.isEmpty() && relations.isEmpty();
	}

	/**
	 * maintaining a reference list in the node would make more sense
	 * @param node
	 * @return
	 */
	public Way getFirstWay(final Node node) {
		for (Way way:ways) {
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
		for (Way way:ways) {
			if (way.hasNode(node)) {
				mWays.add(way);
			}
		}
		return mWays;
	}

	public List<Node> getWaynodes() {
		ArrayList<Node> waynodes = new ArrayList<Node>();
		for (Way way:ways) {
			waynodes.addAll(way.getNodes());
		}
		return waynodes;
	}
	
	public boolean isEndNode(final Node node) {
		for (Way way:ways) {
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
	
	public LongOsmElementMap<Node> getNodeIndex() {
		return nodes;
	}
	
	public LongOsmElementMap<Way> getWayIndex() {
		return ways;
	}
	
	public LongOsmElementMap<Relation> getRelationIndex() {
		return relations;
	}
	
	public void rehash() {
		nodes.rehash();
		ways.rehash();
		relations.rehash();
	}
}
