package de.blau.android.osm;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.util.Log;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.exception.OsmStorageException;

// TODO Extract an interface (possibly "Storage"?) to be able to make other
// StorageDelegators that use other storage methods (other than a DB)
public class StorageDelegator implements Serializable {

	private static final long serialVersionUID = 1L;

	private final static String DEBUG_TAG = StorageDelegator.class
			.getSimpleName();

	public final static String FILENAME = "lastActivity.res";
	private final static Lock FILE_LOCK = new ReentrantLock();

	private final DBAdapter database;

	private BoundingBox boundingBox;

	private final Map<Long, Node> nodes;
	private final Map<Long, Way> ways;
	private final Map<Long, Relation> relations;

	private final Set<Node> modifiedNodes;
	private final Set<Way> modifiedWays;
	private final Set<Relation> modifiedRelations;

	private boolean threadWriteMode = false;

	public StorageDelegator(DBAdapter db) {
		database = db;

		nodes = Collections.synchronizedMap(new TreeMap<Long, Node>());
		ways = Collections.synchronizedMap(new TreeMap<Long, Way>());
		relations = Collections.synchronizedMap(new TreeMap<Long, Relation>());

		modifiedNodes = Collections.synchronizedSet(new HashSet<Node>());
		modifiedWays = Collections.synchronizedSet(new HashSet<Way>());
		modifiedRelations = Collections
				.synchronizedSet(new HashSet<Relation>());

		try {
			this.boundingBox = new BoundingBox(-BoundingBox.MAX_LON,
					-BoundingBox.MAX_LAT, BoundingBox.MAX_LON,
					BoundingBox.MAX_LAT);
		} catch (OsmException e) {
			e.printStackTrace();
		}
	}

	public void startThreadWriteMode() {
		threadWriteMode = true;
	}

	public void stopThreadWriteMode() {
		threadWriteMode = false;
	}

	public boolean isThreadWriteMode() {
		return threadWriteMode;
	}

	public void setBoundingBox(BoundingBox boundingBox) {
		this.boundingBox = boundingBox;
		database.updateBoundingBox(boundingBox);
	}

	public BoundingBox getOriginalBox() {
		return boundingBox.clone();
	}

	public Collection<Node> getNodes() {
		if (threadWriteMode)
			return Collections.emptyList();
		return Collections.unmodifiableCollection(nodes.values());
	}

	public Collection<Way> getWays() {
		if (threadWriteMode)
			return Collections.emptyList();
		return Collections.unmodifiableCollection(ways.values());
	}

	public Collection<Relation> getRelations() {
		if (threadWriteMode)
			return Collections.emptyList();
		return Collections.unmodifiableCollection(relations.values());
	}

	public List<Way> getWays(final Node node) {
		ArrayList<Way> mWays = new ArrayList<Way>();
		for (Way way : ways.values()) {
			if (way.hasNode(node))
				mWays.add(way);
		}
		return mWays;
	}

	public int getNodeCount() {
		return nodes.size();
	}

	public int getWayCount() {
		return ways.size();
	}

	public int getRelationCount() {
		return relations.size();
	}

	public int getModifiedNodeCount() {
		return modifiedNodes.size();
	}

	public int getModifiedWayCount() {
		return modifiedWays.size();
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
	 * Store a node that is {@linkplain State#UNCHANGED unchanged}, that is for
	 * example loaded from the server.
	 * 
	 * @param node
	 * @throws OsmStorageException
	 *             If the node is not unchanged.
	 */
	public void storeNode(Node node) throws OsmStorageException {
		if (node.getState() != OsmElement.STATE_UNCHANGED)
			throw new OsmStorageException("Only unchanged node can be stored.");
		putNode(node);
	}

	/**
	 * Creates, stores and returns a new node. The new node will have a negative
	 * OSM ID and be marked {@linkplain State#CREATED created}.
	 * 
	 * @param lat
	 * @param lon
	 * @return
	 */
	public Node createNode(int lat, int lon) {
		Node node = OsmElementFactory.createNodeWithNewId(lat, lon);
		putNode(node);
		putModified(node);
		return node;
	}

	public Node createNodeAndAddToWay(Way way, int lat, int lon) {
		Node node = createNode(lat, lon);
		addNodeToWay(way, node);
		return node;
	}

	public void addNodeToWay(Way way, Node node) {
		int position = way.addNode(node);
		database.addNodeToWay(way, node, position);
		setState(way, OsmElement.STATE_MODIFIED);
	}

	public void appendNodeToWay(final Node refNode, final Node nextNode,
			final Way way) throws OsmException {
		int position = way.appendNode(refNode, nextNode);
		database.addNodeToWay(way, nextNode, position);
		setState(way, OsmElement.STATE_MODIFIED);
	}

	public void addNodeToWayAfter(final Node nodeBefore, final Node newNode,
			final Way way) {
		int position = way.addNodeAfter(nodeBefore, newNode);
		database.addNodeToWay(way, newNode, position);
		setState(way, OsmElement.STATE_MODIFIED);
	}

	private void putNode(Node node) {
		nodes.put(node.osmId, node);
		database.insertNode(node);
	}

	public void removeNode(final Node node) {
		nodes.remove(node.getOsmId());
		removeWayNodes(node);
		if (node.getState() == OsmElement.STATE_CREATED)
			database.deleteNode(node);
		else
			setState(node, OsmElement.STATE_DELETED);
	}

	private int removeWayNodes(final Node node) {
		int deleted = 0;
		List<Way> ways = getWays(node);
		for (int i = 0, size = ways.size(); i < size; ++i) {
			Way way = ways.get(i);
			way.removeAllNodes(node);
			// remove way when less than two waynodes exist
			if (way.getNodes().size() < 2) {
				removeWay(way);
			} else
				setState(way, OsmElement.STATE_MODIFIED);
			deleted++;
		}
		return deleted;
	}

	public void storeWay(Way way) throws OsmStorageException {
		if (way.getState() != OsmElement.STATE_UNCHANGED)
			throw new OsmStorageException("Only unchanged way can be stored.");
		putWay(way);
	}

	public Way createWayAndAddNode(final Node node) {
		Way way = OsmElementFactory.createWayWithNewId();
		way.addNode(node);
		putWay(way);
		putModified(way);
		return way;
	}

	private void putWay(Way way) {
		ways.put(way.osmId, way);
		database.insertWay(way);
	}

	private void removeWay(final Way way) {
		ways.remove(way.getOsmId());
		if (way.getState() == OsmElement.STATE_CREATED)
			database.deleteWay(way);
		else
			setState(way, OsmElement.STATE_DELETED);

	}

	public void updateLatLon(final Node node, final int latE7, final int lonE7) {
		node.setLat(latE7);
		node.setLon(lonE7);
		database.updateNode(node);
		setState(node, OsmElement.STATE_MODIFIED);
	}

	public void setTags(final OsmElement element, final Map<String, String> tags) {
		if (!element.getTags().equals(tags)) {
			element.setTags(tags);
			setState(element, OsmElement.STATE_MODIFIED);
			database.updateTags(element);
		}
	}

	private void setState(final OsmElement element, final byte newState) {
		if (element.getState() != newState
				&& (element.getState() != OsmElement.STATE_CREATED || newState == OsmElement.STATE_DELETED)) {
			element.setState(newState);
			database.updateState(element);
			putModified(element);
		}
	}

	private void putModified(OsmElement element) {
		switch (element.getType()) {
		case OsmElement.TYPE_NODE:
			modifiedNodes.add((Node) element);
			break;
		case OsmElement.TYPE_WAY:
			modifiedWays.add((Way) element);
			break;
		case OsmElement.TYPE_RELATION:
			modifiedRelations.add((Relation) element);
			break;
		}
	}

	private void dropModified(OsmElement element) {
		switch (element.getType()) {
		case OsmElement.TYPE_NODE:
			modifiedNodes.remove(element);
			break;
		case OsmElement.TYPE_WAY:
			modifiedWays.remove(element);
			break;
		case OsmElement.TYPE_RELATION:
			modifiedRelations.remove(element);
			break;
		}
	}

	private void dropElement(OsmElement element) {
		dropModified(element);
		switch (element.getType()) {
		case OsmElement.TYPE_NODE:
			nodes.values().remove(element);
			break;
		case OsmElement.TYPE_WAY:
			ways.values().remove(element);
			break;
		case OsmElement.TYPE_RELATION:
			relations.values().remove(element);
			break;
		}
	}

	public void loadFromStorage() {
		startThreadWriteMode();
		boundingBox = database.loadBoundingBox();
		database.loadNodes(nodes, modifiedNodes);
		database.loadWays(nodes, ways, modifiedWays);
		stopThreadWriteMode();
	}

	public void reset() {
		database.deleteAll();
		nodes.clear();
		ways.clear();
		relations.clear();
		modifiedNodes.clear();
		modifiedWays.clear();
		modifiedRelations.clear();
	}

	public boolean isEndNode(final Node node) {
		for (Way way : ways.values())
			if (way.isEndNode(node)) {
				return true;
			}
		return false;
	}

	public boolean hasChanges() {
		return !modifiedNodes.isEmpty() || !modifiedWays.isEmpty()
				|| !modifiedRelations.isEmpty();
	}

	public boolean isEmpty() {
		return nodes.isEmpty() && ways.isEmpty() && relations.isEmpty();
	}

	public boolean storageEmpty() {
		return database.storageEmpty();
	}

	public void writeToFile(final Context context) throws IOException {
		OutputStream out = null;
		ObjectOutputStream objectOut = null;
		try {
			FILE_LOCK.lock();
			out = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
			objectOut = new ObjectOutputStream(out);
			objectOut.writeObject(this);
		} finally {
			Server.close(objectOut);
			Server.close(out);
			FILE_LOCK.unlock();
		}
	}

	public synchronized void uploadToServer(final Server server)
			throws MalformedURLException, ProtocolException,
			OsmServerException, IOException {
		server.openChangeset();
		uploadCreatedOrModifiedElements(server, modifiedNodes);
		uploadCreatedOrModifiedElements(server, modifiedWays);
		uploadDeletedElements(server, modifiedWays);
		uploadDeletedElements(server, modifiedNodes);
		server.closeChangeset();
	}

	private void uploadCreatedOrModifiedElements(final Server server,
			final Collection<? extends OsmElement> elements)
			throws MalformedURLException, ProtocolException,
			OsmServerException, IOException {
		for (OsmElement element : elements) {
			switch (element.getState()) {
			case OsmElement.STATE_CREATED:
				int osmId = server.createElement(element);
				if (osmId > 0) {
					setElementUnchanged(element, 1l, osmId);
					Log.w(DEBUG_TAG, "New " + element + " added to API");
				}
				break;
			case OsmElement.STATE_MODIFIED:
				int osmVersion = server.updateElement(element);
				if (osmVersion > 0) {
					setElementUnchanged(element, osmVersion);
					Log.w(DEBUG_TAG, element + " updated in API");
				}
			}
		}
	}

	private void setElementUnchanged(OsmElement element, long version) {
		setElementUnchanged(element, version, null);
	}

	private void setElementUnchanged(OsmElement element, long version,
			Integer osmId) {
		if (osmId != null) {
			database.updateOsmId(element, osmId);
			element.setOsmId(osmId);
		}

		element.setState(OsmElement.STATE_UNCHANGED);
		database.updateState(element);

		element.osmVersion = version;
		database.updateVersion(element);

		dropModified(element);
	}

	private void uploadDeletedElements(final Server server,
			final Collection<? extends OsmElement> elements)
			throws MalformedURLException, ProtocolException,
			OsmServerException, IOException {
		ArrayList<OsmElement> dropFromStorage = new ArrayList<OsmElement>();
		for (OsmElement element : elements) {
			if (element.getState() == OsmElement.STATE_DELETED) {
				server.deleteElement(element);
				dropFromStorage.add(element);
				Log.w(DEBUG_TAG, element + " deleted in API");
			}
		}
		for (OsmElement element : dropFromStorage) {
			dropElement(element);
			database.deleteElement(element);
		}
	}
}
