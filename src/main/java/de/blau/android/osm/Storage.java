package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.support.annotation.Nullable;
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

	/**
	 * Default constructor
	 * <p>
	 * Initializes the storage and adds a maximum valid mercator size bounding box 
	 */
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
	
	/**
	 * Construct a new storage object with the contents of an existing one
	 * @param s storage object to duplicate
	 */
	Storage(Storage s) {
		nodes = new LongOsmElementMap<Node>(s.nodes);
		ways = new LongOsmElementMap<Way>(s.ways);
		relations = new LongOsmElementMap<Relation>(s.relations);
		bboxes = Collections.synchronizedList(new ArrayList<BoundingBox>(s.bboxes));
	}

	/**
	 * Get a specific node by id
	 * @param nodeOsmId id of the node
	 * @return the node or null if not found
	 */
	@Nullable
	public Node getNode(final long nodeOsmId) { 
		return nodes.get(nodeOsmId);
	}

	/**
	 * Get a specific way by id
	 * @param wayOsmId id of the way
	 * @return the way or null if not found
	 */
	@Nullable
	public Way getWay(final long wayOsmId) {
		return ways.get(wayOsmId);
	}
	
	/**
	 * Get a specific relation by id
	 * @param relationOsmId id of the relation
	 * @return the relation or null if not found
	 */
	@Nullable
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
	 * Get a unmodifiable list of all nodes
	 * 
	 * @return list containing all nodes
	 */
	public List<Node> getNodes() {
		return Collections.unmodifiableList(nodes.values());
	}

	/**
	 * Return all nodes in a bounding box
	 * 
	 * Note: currently this does a sequential scan of all nodes
	 * @param box bounding box to search in
	 * @return a list of all nodes in box
	 */
	public List<Node> getNodes(BoundingBox box) {
		ArrayList<Node> result = new ArrayList<Node>(nodes.size());
		for (Node n:nodes) {
			if (box.isIn(n.getLat(), n.getLon())) {
				result.add(n);
			}	
		}
		return result;
	}
	
	/**
	 * Get a unmodifiable list of all ways
	 * 
	 * @return list containing all ways
	 */
	public List<Way> getWays() {
		return Collections.unmodifiableList(ways.values());
	}
	
	/**
	 * Return all ways covered or possibly intersecting a bounding box
	 * <p> 
	 * Note: currently this does a sequential scan of all ways
	 * @param box bounding box to search in
	 * @return a list of all ways in box
	 */
	public List<Way> getWays(BoundingBox box) {
		ArrayList<Way> result = new ArrayList<Way>(ways.size());
		BoundingBox newBox = new BoundingBox(); // avoid creating new instances
		for (Way w:ways) {
			BoundingBox wayBox = w.getBounds(newBox);
			if (wayBox.intersects(box)) {
				result.add(w);
			}	
		}
		return result;
	}
	
	/**
	 * Get a unmodifiable list of all relations
	 * 
	 * @return list containing all relations
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

	/**
	 * Test if an element is present in storage
	 * @param element element to check for
	 * @return true if element is in storage
	 */
	public boolean contains(final OsmElement element) {
		if (element instanceof Way) {
            return ways.containsKey(element.getOsmId());
		} else if (element instanceof Node) {
            return nodes.containsKey(element.getOsmId());
		} else if (element instanceof Relation) {
			return relations.containsKey(element.getOsmId());
		}
		return false;
	}

	/**
	 * Insert a node in to storage regardless of it is already present or not
	 * @param node node to insert
	 * @throws StorageException
	 */
	void insertNodeUnsafe(final Node node) throws StorageException {
		try {
			nodes.put(node.getOsmId(),node);
		} catch (Error err) { // should really only be OutOfMemory
			throw new StorageException(StorageException.OOM);
		}

	}

	/**
	 * Insert a way in to storage regardless of it is already present or not
	 * @param way way to insert
	 * @throws StorageException
	 */
	void insertWayUnsafe(final Way way)  throws StorageException  {
		try {
			ways.put(way.getOsmId(),way);
		} catch (Error err) { // should really only be OutOfMemory
			throw new StorageException(StorageException.OOM);
		}
	}

	/**
	 * Insert a relation in to storage regardless of it is already present or not
	 * @param relation relation to insert
	 * @throws StorageException
	 */
	void insertRelationUnsafe(final Relation relation) throws StorageException  {
		try {
			relations.put(relation.getOsmId(),relation);
		} catch (Error err) { // should really only be OutOfMemory
			throw new StorageException(StorageException.OOM);
		}
	}
	
	/**
	 * Insert an element if it is not already present in storage
	 * <p>
	 * Note: the current data structures do not allow multiple entries for the same object in any case
	 * @param element element to insert
	 * @throws StorageException
	 */
	void insertElementSafe(final OsmElement element) throws StorageException {
		if (!contains(element)) {
			insertElementUnsafe(element);
		}
	}

	/**
	 * Insert an element in to storage regardless of it is already present or not
	 * @param element element to insert
	 * @throws StorageException
	 */
	void insertElementUnsafe(final OsmElement element) throws StorageException {
		if (element instanceof Way) {
			insertWayUnsafe((Way) element);
		} else if (element instanceof Node) {
			insertNodeUnsafe((Node) element);
		} else if (element instanceof Relation) {
			insertRelationUnsafe((Relation) element);
		}
	}

	/**
	 * Remove a node from storage
	 * @param node node to remove
	 * @return true if the node was in storage
	 */
	boolean removeNode(final Node node) {
		return nodes.remove(node.getOsmId())!=null;
	}

	/**
	 * Remove a way from storage
	 * @param way way to remove
	 * @return true if the way was in storage
	 */
	boolean removeWay(final Way way) {
		return ways.remove(way.getOsmId())!=null;
	}

	/**
	 * Remove a relation from storage
	 * @param relation relation to remove
	 * @return true if the relation was in storage
	 */
	boolean removeRelation(final Relation relation) {
		return relations.remove(relation.getOsmId())!=null;
	}
	
	/**
	 * Remove an element of any type from storage 
	 * @param element element to remove
	 * @return true if the element was in storage
	 */
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

	/**
	 * Get all bounding boxes of downloaded data
	 * @return all bounding boxes
	 */
	public List<BoundingBox> getBoundingBoxes() {
		return bboxes;
	}

	/**
	 * Resets bounding box list and adds this boundingbox
	 * @param bbox bounding box to add
	 */
	void setBoundingBox(final BoundingBox bbox) {
		this.bboxes = Collections.synchronizedList(new ArrayList<BoundingBox>());
		this.bboxes.add(bbox);
	}
	
	/**
	 * Add this bounding box to list
	 * @param bbox bounding box to add
	 */
	void addBoundingBox(final BoundingBox bbox) {
		if (this.bboxes == null)
			this.bboxes = Collections.synchronizedList(new ArrayList<BoundingBox>());
		this.bboxes.add(bbox);
	}
	
	/**
	 * Remove bounding box from list
	 * @param box bounding box to remove
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
	 * Get a the "first" way containing node
	 * @param node node to search for
	 * @return the way or null if none was found
	 */
	@Nullable
	public Way getFirstWay(final Node node) {
		for (Way way:ways) {
			if (way.getNodes().contains(node)) {
				return way;
			}
		}
		return null;
	}

	/**
	 * Get all ways that node is a vertex of
	 * <p>
	 * This method currently does a sequential scan of all ways in storage and should be avoided
	 * @param node node to search for
	 * @return list containing all ways containing node
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

	/**
	 * Get all nodes that are vertexes in a way
	 * <p>
	 * This method currently does a sequential scan of all ways in storage and should be avoided
	 * 
	 * @return all way nodes
	 */
	public List<Node> getWaynodes() {
		ArrayList<Node> waynodes = new ArrayList<Node>();
		for (Way way:ways) {
			waynodes.addAll(way.getNodes());
		}
		return waynodes;
	}
	
	/**
	 * Tests if node is first or last node of any way in storage
	 * <p>
	 * This method currently does a sequential scan of all ways in storage and should be avoided
	 * 
	 * @param node node to check
	 * @return true if node is the first or last node of at least one way
	 */
	public boolean isEndNode(final Node node) {
		for (Way way:ways) {
			if (way.isEndNode(node)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Calculate a bounding box just covering the data
	 * @return a bounding box
	 * @throws OsmException if no valid BoundingBox could be created
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
		return new BoundingBox(left, bottom, right, top);
	}
	
	/**
	 * Get the node map
	 * @return the map indexing nodes
	 */
	public LongOsmElementMap<Node> getNodeIndex() {
		return nodes;
	}
	
	/**
	 * Get the way map
	 * @return the map indexing ways
	 */
	public LongOsmElementMap<Way> getWayIndex() {
		return ways;
	}
	
	/**
	 * Get the relation map
	 * @return the map indexing relations
	 */
	public LongOsmElementMap<Relation> getRelationIndex() {
		return relations;
	}
	
	/**
	 * Rehash the maps used for storing elements. 
	 * <p>
	 * This is required since elements will change their id when being saved to the OSM database the first time.
	 */
	public void rehash() {
		nodes.rehash();
		ways.rehash();
		relations.rehash();
	}
	
	/**
	 * Log the contents
	 */
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
		for (Relation r:relations) {
			Log.d("Storage","Relation " + r.getOsmId());
			for (String k:r.getTags().keySet()) {
				Log.d("Storage",k + "=" + r.getTags().get(k));
			}
		}
	}
}
