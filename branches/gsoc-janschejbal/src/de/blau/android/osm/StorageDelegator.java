package de.blau.android.osm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.res.Resources;
import android.util.Log;
import de.blau.android.Main;
import de.blau.android.exception.OsmServerException;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SavingHelper.Exportable;

public class StorageDelegator implements Serializable, Exportable {

	private static final long serialVersionUID = 3L;

	private Storage currentStorage;

	private Storage apiStorage;

	private UndoStorage undo;

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

	/**
	 * A OsmElementFactory that is used to create new elements.
	 * Needs to be persisted together with currentStorage/apiStorage to avoid duplicate IDs
	 * when the application is restarted after some elements have been created.
	 */
	private OsmElementFactory factory;

	public void setCurrentStorage(final Storage currentStorage) {
		dirty = true;
		this.apiStorage = new Storage();
		this.currentStorage = currentStorage;
		this.undo = new UndoStorage(currentStorage, apiStorage);
	}

	public StorageDelegator() {
		reset();
	}

	public void reset() {
		dirty = true;
		apiStorage = new Storage();
		currentStorage = new Storage();
		undo = new UndoStorage(currentStorage, apiStorage);
		factory = new OsmElementFactory();
	}

	/**
	 * Get the current undo instance.
	 * For immediate use only - DO NOT CACHE THIS.
	 * @return the UndoStorage, allowing operations like creation of checkpoints and undo/redo.  
	 */
	public UndoStorage getUndo() {
		return undo;
	}
	
	/**
	 * Clears the undo storage. Must be called on the main thread due to menu invalidation.
	 */
	public void clearUndo() {
		undo = new UndoStorage(currentStorage, apiStorage);
		Main.triggerMenuInvalidationStatic();
	}

	/**
	 * Get the current OsmElementFactory instance used by this delegator.
	 * Use only the factory returned by this to create new element IDs for insertion into this delegator!
	 * For immediate use only - DO NOT CACHE THIS.
	 * @return the OsmElementFactory for creating nodes/ways with new IDs
	 */
	public OsmElementFactory getFactory() {
		return factory;
	}

	public void insertElementSafe(final OsmElement elem) {
		dirty = true;
		undo.save(elem);
		
		currentStorage.insertElementSafe(elem);
		apiStorage.insertElementSafe(elem);
	}

	/**
	 * Sets the tags of the element, replacing all existing ones
	 * @param elem the element to tag
	 * @param tags the new tags
	 */
	public void setTags(final OsmElement elem, final Map<String, String> tags) {
		dirty = true;
		undo.save(elem);
		
		if (elem.setTags(tags)) {
			// OsmElement tags have changed
			elem.updateState(OsmElement.STATE_MODIFIED);
			apiStorage.insertElementSafe(elem);
		}
	}

	private void insertElementUnsafe(final OsmElement elem) {
		dirty = true;
		undo.save(elem);
		
		currentStorage.insertElementUnsafe(elem);
		apiStorage.insertElementUnsafe(elem);
	}

	/**
	 * @param firstWayNode
	 * @return
	 */
	public Way createAndInsertWay(final Node firstWayNode) {
		// undo - nothing done here, way gets saved/marked on insert
		dirty = true;
		
		Way way = factory.createWayWithNewId();
		way.addNode(firstWayNode);
		insertElementUnsafe(way);
		return way;
	}

	public void addNodeToWay(final Node node, final Way way) {
		dirty = true;
		undo.save(way);
		
		apiStorage.insertElementSafe(way);
		way.addNode(node);
		way.updateState(OsmElement.STATE_MODIFIED);
	}

	public void addNodeToWayAfter(final Node nodeBefore, final Node newNode, final Way way) {
		dirty = true;
		undo.save(way);
		
		apiStorage.insertElementSafe(way);
		way.addNodeAfter(nodeBefore, newNode);
		way.updateState(OsmElement.STATE_MODIFIED);
	}

	public void appendNodeToWay(final Node refNode, final Node nextNode, final Way way) {
		dirty = true;
		undo.save(way);
		
		apiStorage.insertElementSafe(way);
		way.appendNode(refNode, nextNode);
		way.updateState(OsmElement.STATE_MODIFIED);
	}

	public void updateLatLon(final Node node, final int latE7, final int lonE7) {
		dirty = true;
		undo.save(node);
		
		apiStorage.insertElementSafe(node);
		node.setLat(latE7);
		node.setLon(lonE7);
		node.updateState(OsmElement.STATE_MODIFIED);
	}

	public void removeNode(final Node node) {
		// undo - node saved here, affected ways saved in removeWayNodes
		dirty = true;
		undo.save(node);
		
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
		// undo - nothing done here, everything done in splitAtNode
		dirty = true;
		List<Way> ways = currentStorage.getWays(node);
		for (Way way : ways) {
			splitAtNode(way, node);
		}
	}

	public void splitAtNode(final Way way, final Node node) {
		// undo - old way is saved here, new way is saved at insert
		dirty = true;
		undo.save(way);
		
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
		Way newWay = factory.createWayWithNewId();
		newWay.updateState(OsmElement.STATE_CREATED);
		newWay.addTags(way.getTags());
		for (Node wayNode : nodesForNewWay) {
			newWay.addNode(wayNode);
		}

		insertElementUnsafe(newWay);
	}
	
	/**
	 * Merges two ways by prepending/appending all nodes from the second way to the first one, then deleting the second one.
	 * Tags are not handled; all tags from the second way are lost.
	 * @param mergeInto Way to merge the other way into. This way will be kept.
	 * @param mergeFrom Way to merge into the other. This way will be deleted.
	 */
	public void mergeWays(Way mergeInto, Way mergeFrom) {
		// undo - mergeInto way saved here, mergeFrom way will not be changed directly and will be saved in removeWay
		dirty = true;
		undo.save(mergeInto);
		
		List<Node> newNodes = new ArrayList<Node>(mergeFrom.getNodes());
		boolean atBeginning;
		
		if (mergeInto.getFirstNode().equals(mergeFrom.getFirstNode())) {
			// Result: f3 f2 f1 (f0=)i0 i1 i2 i3   (f0 = 0th node of mergeFrom, i1 = 1st node of mergeInto)
			atBeginning = true;
			Collections.reverse(newNodes);
			newNodes.remove(newNodes.size()-1); // remove "last" (originally first) node after reversing
		} else if (mergeInto.getLastNode().equals(mergeFrom.getFirstNode())) {
			// Result: i0 i1 i2 i3(=f0) f1 f2 f3
			atBeginning = false;
			newNodes.remove(0);			
		} else if (mergeInto.getFirstNode().equals(mergeFrom.getLastNode())) {
			// Result: f0 f1 f2 (f3=)i0 i1 i2 i3
			atBeginning = true;
			newNodes.remove(newNodes.size()-1);			
		} else if (mergeInto.getLastNode().equals(mergeFrom.getLastNode())) {
			// Result: i0 i1 i2 i3(=f3) f2 f1 f0
			atBeginning = false;
			newNodes.remove(newNodes.size()-1); // remove last node before reversing
			Collections.reverse(newNodes);
		} else {
			throw new RuntimeException("attempted to merge non-mergeable nodes. this is a bug.");
		}
		
		mergeInto.addNodes(newNodes, atBeginning);
		insertElementSafe(mergeInto);
		
		removeWay(mergeFrom);
	}
	
	/**
	 * Reverses a way
	 * @param way
	 */
	public void reverseWay(final Way way) {
		dirty = true;
		undo.save(way);
		way.reverse();
	}

	private int removeWayNodes(final Node node) {
		// undo - node is not changed, affected way(s) are stored below
		dirty = true;
		
		int deleted = 0;
		List<Way> ways = currentStorage.getWays(node);
		for (int i = 0, size = ways.size(); i < size; ++i) {
			Way way = ways.get(i);
			undo.save(way);
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

	/**
	 * Deletes a way
	 * @param way
	 */
	public void removeWay(final Way way) {
		dirty = true;
		undo.save(way);

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
		
		if (savingHelper.save(FILENAME, this, true)) {
			dirty = false;
		}
	}

	/**
	 * Loads the storage data from the default storage file
	 */
	public void readFromFile() {
		StorageDelegator newDelegator = savingHelper.load(FILENAME, true);
		if (newDelegator != null) {
			currentStorage = newDelegator.currentStorage;
			apiStorage = newDelegator.apiStorage;
			undo = newDelegator.undo;
			factory = newDelegator.factory;
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
		// yes, again, just to be sure
		dirty = true;
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
	
	/**
	 * Exports changes as a OsmChange file. 
	 */
	@Override
	public void export(OutputStream outputStream) throws Exception {
		XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
		serializer.setOutput(outputStream, "UTF-8");
		serializer.startDocument("UTF-8", null);
		serializer.startTag(null, "osmChange");
		serializer.attribute(null, "generator", "Vespucci");
		serializer.attribute(null, "version", "0.6");
		
		ArrayList<OsmElement> createdElements = new ArrayList<OsmElement>();
		ArrayList<OsmElement> modifiedElements = new ArrayList<OsmElement>();
		ArrayList<OsmElement> deletedElements = new ArrayList<OsmElement>();
		
		for (OsmElement elem : apiStorage.getNodes()) {
			switch (elem.state) {
			case OsmElement.STATE_CREATED:   createdElements.add(elem);   break;
			case OsmElement.STATE_MODIFIED:  modifiedElements.add(elem);  break;
			case OsmElement.STATE_DELETED:   deletedElements.add(elem);   break;
			}
		}
		for (OsmElement elem : apiStorage.getWays()) {
			switch (elem.state) {
			case OsmElement.STATE_CREATED:   createdElements.add(elem);   break;
			case OsmElement.STATE_MODIFIED:  modifiedElements.add(elem);  break;
			case OsmElement.STATE_DELETED:   deletedElements.add(elem);   break;
			}
		}

		if (!createdElements.isEmpty()) {
			serializer.startTag(null, "create");
			for (OsmElement elem : createdElements) elem.toXml(serializer, null);
			serializer.endTag(null, "create");
		}

		if (!modifiedElements.isEmpty()) {
			serializer.startTag(null, "modify");
			for (OsmElement elem : modifiedElements) elem.toXml(serializer, null);
			serializer.endTag(null, "modify");
		}
				
		if (!deletedElements.isEmpty()) {
			serializer.startTag(null, "delete");
			for (OsmElement elem : deletedElements) elem.toXml(serializer, null);
			serializer.endTag(null, "delete");
		}
		
		serializer.endTag(null, "osmChange");
		serializer.endDocument();
	}

	@Override
	public String exportExtension() {
		return "osc";
	}
}
