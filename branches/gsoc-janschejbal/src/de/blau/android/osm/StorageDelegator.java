package de.blau.android.osm;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.res.Resources;
import android.util.Log;
import de.blau.android.exception.OsmServerException;
import de.blau.android.util.SavingHelper;

public class StorageDelegator implements Serializable {

	private static final long serialVersionUID = 1L;

	private Storage currentStorage;

	private Storage apiStorage;

	/**
	 * Indicates whether changes have been made since the last save to disk.
	 * Since a newly created storage is not saved, the constructor sets it to true.
	 * After a successful save or load, it is set to false.
	 * If it is false, save does nothing.
	 */
	private transient boolean dirty;	
	
	private final static String DEBUG_TAG = StorageDelegator.class.getSimpleName();

	public final static String FILENAME = "lastActivity.res";

	private transient SavingHelper<StorageDelegator> savingHelper = new SavingHelper<StorageDelegator>();
	
	public void setCurrentStorage(final Storage currentStorage) {
		this.currentStorage = currentStorage;
	}

	public StorageDelegator() {
		dirty = true;
		apiStorage = new Storage();
		currentStorage = new Storage();
	}

	public void reset() {
		dirty = true;
		apiStorage = new Storage();
		currentStorage = new Storage();
	}

	public void insertElementSafe(final OsmElement elem) {
		dirty = true;
		currentStorage.insertElementSafe(elem);
		apiStorage.insertElementSafe(elem);
	}

	public void insertTags(final OsmElement elem, final Map<String, String> tags) {
		dirty = true;
		if (elem.setTags(tags)) {
			// OsmElement tags have changed
			elem.updateState(OsmElement.STATE_MODIFIED);
			apiStorage.insertElementSafe(elem);
		}
	}

	private void insertElementUnsafe(final OsmElement elem) {
		dirty = true;
		currentStorage.insertElementUnsafe(elem);
		apiStorage.insertElementUnsafe(elem);
	}

	/**
	 * @param firstWayNode
	 * @return
	 */
	public Way createAndInsertWay(final Node firstWayNode) {
		dirty = true;
		Way way = OsmElementFactory.createWayWithNewId();
		way.addNode(firstWayNode);
		insertElementUnsafe(way);
		return way;
	}

	public void addNodeToWay(final Node node, final Way way) {
		dirty = true;
		apiStorage.insertElementSafe(way);
		way.addNode(node);
		way.updateState(OsmElement.STATE_MODIFIED);
	}

	public void addNodeToWayAfter(final Node nodeBefore, final Node newNode, final Way way) {
		dirty = true;
		apiStorage.insertElementSafe(way);
		way.addNodeAfter(nodeBefore, newNode);
		way.updateState(OsmElement.STATE_MODIFIED);
	}

	public void appendNodeToWay(final Node refNode, final Node nextNode, final Way way) {
		dirty = true;
		apiStorage.insertElementSafe(way);
		way.appendNode(refNode, nextNode);
		way.updateState(OsmElement.STATE_MODIFIED);
	}

	public void updateLatLon(final Node node, final int latE7, final int lonE7) {
		dirty = true;
		apiStorage.insertElementSafe(node);
		node.setLat(latE7);
		node.setLon(lonE7);
		node.updateState(OsmElement.STATE_MODIFIED);
	}

	public void removeNode(final Node node) {
		dirty = true;
		if (node.state == Node.STATE_CREATED) {
			apiStorage.removeElement(node);
		} else {
			apiStorage.insertElementUnsafe(node);
		}
		removeWayNodes(node);
		currentStorage.removeNode(node);
		node.updateState(OsmElement.STATE_DELETED);
	}

	public void splitAtNode(final Node node) {
		dirty = true;
		List<Way> ways = currentStorage.getWays(node);
		for (Way way : ways) {
			splitAtNode(way, node);
		}
	}

	public void splitAtNode(final Way way, final Node node) {
		dirty = true;
		List<Node> nodes = way.getNodes();
		if (nodes.size() < 3) {
			return;
		}
		// we assume this node is only contained in the way once.
		// else the user needs to split the remaining way again.
		List<Node> nodesForNewWay = new LinkedList<Node>();
		boolean found = false;
		for (Iterator<Node> it = way.getRemovableNodes(); it.hasNext();) {
			Node wayNode = it.next();
			if (!found && wayNode.getOsmId() == node.getOsmId()) {
				found = true;
				nodesForNewWay.add(wayNode);
			} else if (found) {
				nodesForNewWay.add(wayNode);
				it.remove();
			}
		}
		if (nodesForNewWay.size() < 1) {
			return; // do not create 1-node way
		}

		way.updateState(OsmElement.STATE_MODIFIED);
		apiStorage.insertElementSafe(way);

		// create the new way
		Way newWay = OsmElementFactory.createWayWithNewId();
		newWay.updateState(OsmElement.STATE_CREATED);
		newWay.addTags(way.getTags());
		for (Node wayNode : nodesForNewWay) {
			newWay.addNode(wayNode);
		}

		insertElementUnsafe(newWay);
	}

	private int removeWayNodes(final Node node) {
		dirty = true;
		int deleted = 0;
		List<Way> ways = currentStorage.getWays(node);
		for (int i = 0, size = ways.size(); i < size; ++i) {
			Way way = ways.get(i);
			way.removeAllNodes(node);
			//remove way when less than two waynodes exist
			if (way.getNodes().size() < 2) {
				removeWay(way);
			} else {
				way.updateState(OsmElement.STATE_MODIFIED);
				apiStorage.insertElementSafe(way);
			}
			deleted++;
		}
		return deleted;
	}

	private void removeWay(final Way way) {
		dirty = true;
		currentStorage.removeWay(way);
		if (apiStorage.contains(way)) {
			if (way.getState() == OsmElement.STATE_CREATED) {
				apiStorage.removeElement(way);
			}
		} else {
			apiStorage.insertElementUnsafe(way);
		}
		way.updateState(OsmElement.STATE_DELETED);
	}

	public Storage getCurrentStorage() {
		return currentStorage;
	}

	public BoundingBox getOriginalBox() {
		return currentStorage.getBoundingBox().copy();
	}

	public void setOriginalBox(final BoundingBox box) {
		dirty = true;
		currentStorage.setBoundingBox(box);
	}

	public int getApiNodeCount() {
		return apiStorage.getNodes().size();
	}

	public int getApiWayCount() {
		return apiStorage.getWays().size();
	}

	public OsmElement getOsmElement(final String type, final long osmId) {
		OsmElement elem = apiStorage.getOsmElement(type, osmId);
		if (elem == null) {
			elem = currentStorage.getOsmElement(type, osmId);
		}
		return elem;
	}

	public boolean hasChanges() {
		return !apiStorage.isEmpty();
	}

	public boolean isEmpty() {
		return currentStorage.isEmpty() && apiStorage.isEmpty();
	}

	/**
	 * Stores the current storage data to the default storage file
	 * @throws IOException
	 */
	public void writeToFile() throws IOException {
		if (!dirty) {
			Log.i("StorageDelegator", "storage delegator not dirty, skipping save");
			return;
		}
		
		if (savingHelper.save(FILENAME, this)) {
			dirty = false;
		}
	}

	/**
	 * Loads the storage data from the default storage file
	 */
	public void readFromFile() {
		StorageDelegator newDelegator = savingHelper.load(FILENAME);
		if (newDelegator != null) {
			currentStorage = newDelegator.currentStorage;
			apiStorage = newDelegator.apiStorage;
			dirty = false; // data was just read, i.e. memory and file are in sync
		}
	}

	/**
	 * Return a localized list of strings describing the changes we would upload on {@link #uploadToServer(Server)}.
	 * 
	 * @param aResources the translations
	 * @return the changes
	 */
	public List<String> listChanges(final Resources aResources) {
		List<String> retval = new ArrayList<String>();
		
		for (Node node : apiStorage.getNodes()) {
			retval.add(node.getStateDescription(aResources));
		}
		
		for (Way way : apiStorage.getWays()) {
			retval.add(way.getStateDescription(aResources));
		}
		
		// we do not support editing relations yet
		return retval;
	}

	/**
	 * 
	 * @param server Server to upload changes to.
	 * @param comment Changeset comment.
	 * @throws MalformedURLException
	 * @throws ProtocolException
	 * @throws OsmServerException
	 * @throws IOException
	 */
	public synchronized void uploadToServer(final Server server, final String comment) throws MalformedURLException, ProtocolException,
			OsmServerException, IOException {
		dirty = true; // storages will get modified as data is uploaded, these changes need to be saved to file
		// upload methods set dirty flag too, in case the file is saved during an upload
		server.openChangeset(comment);
		uploadCreatedOrModifiedElements(server, apiStorage.getNodes());
		uploadCreatedOrModifiedElements(server, apiStorage.getWays());
		uploadDeletedElements(server, apiStorage.getWays());
		uploadDeletedElements(server, apiStorage.getNodes());
		server.closeChangeset();
	}

	private void uploadDeletedElements(final Server server, final List<? extends OsmElement> elements)
			throws MalformedURLException, ProtocolException, OsmServerException, IOException {
		for (int i = 0, size = elements.size(); i < size; ++i) {
			OsmElement element = elements.get(i);
			if (element.getState() == OsmElement.STATE_DELETED) {
				server.deleteElement(element);
				if (apiStorage.removeElement(element)) {
					--i;
					--size;
				}
				Log.w(DEBUG_TAG, element + " deleted in API");
				dirty = true;
			}
		}
	}

	private void uploadCreatedOrModifiedElements(final Server server, final List<? extends OsmElement> elements)
			throws MalformedURLException, ProtocolException, OsmServerException, IOException {
		for (int i = 0, size = elements.size(); i < size; ++i) {
			OsmElement element = elements.get(i);
			switch (element.getState()) {
			case OsmElement.STATE_CREATED:
				int osmId = server.createElement(element);
				if (osmId > 0) {
					element.setOsmId(osmId);
					if (apiStorage.removeElement(element)) {
						--i;
						--size;
					}
					Log.w(DEBUG_TAG, "New " + element + " added to API");
					element.setState(OsmElement.STATE_UNCHANGED);
				}
				break;
			case OsmElement.STATE_MODIFIED:
				int osmVersion = server.updateElement(element);
				if (osmVersion > 0) {
					element.osmVersion = osmVersion;
					if (apiStorage.removeElement(element)) {
						--i;
						--size;
					}
					Log.w(DEBUG_TAG, element + " updated in API");
					element.setState(OsmElement.STATE_UNCHANGED);
				}
				break;
			}
			dirty = true;
		}
	}
}
