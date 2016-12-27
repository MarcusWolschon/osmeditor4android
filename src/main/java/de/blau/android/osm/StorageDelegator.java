package de.blau.android.osm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.acra.ACRA;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.exception.StorageException;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SavingHelper.Exportable;
import de.blau.android.util.Util;
import de.blau.android.util.collections.LongOsmElementMap;

public class StorageDelegator implements Serializable, Exportable {

	private static final long serialVersionUID = 9L;

	private Storage currentStorage;

	private Storage apiStorage;

	private UndoStorage undo;
	
	private ClipboardStorage clipboard;
	
	private ArrayList<String> imagery;
	
	/**
	 * when reading state lockout writing/reading 
	 */
	private transient ReentrantLock readingLock = new ReentrantLock();
	
	/**
	 * Indicates whether changes have been made since the last save to disk.
	 * Since a newly created storage is not saved, the constructor sets it to true.
	 * After a successful save or load, it is set to false.
	 * If it is false, save does nothing.
	 */
	private transient boolean dirty;	
	
	/**
	 * if false we need to check if the current imagery has been recorded
	 */
	private transient boolean imageryRecorded = false;
	
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
		apiStorage = new Storage();
		clipboard = new ClipboardStorage();
		this.currentStorage = currentStorage;
		undo = new UndoStorage(currentStorage, apiStorage);
	}

	public StorageDelegator() {
		reset(false); // don't set dirty on instantiation
	}

	public void reset(boolean dirty) {
		this.dirty = dirty;
		apiStorage = new Storage();
		currentStorage = new Storage();
		clipboard = new ClipboardStorage();
		undo = new UndoStorage(currentStorage, apiStorage);
		factory = new OsmElementFactory();
		imagery = new ArrayList<String>();
	}

	public boolean isDirty() {
		return dirty;
	}
	
	/**
	 * set dirty to true
	 */
	public void dirty() {
		dirty = true;
		Log.d("StorageDelegator", "setting delegator to dirty");
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
		
		try {
			currentStorage.insertElementSafe(elem);
			apiStorage.insertElementSafe(elem);
			recordImagery();
		} catch (StorageException e) {
			// TODO handle OOMk
			e.printStackTrace();
		}
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
			try {
				apiStorage.insertElementSafe(elem);
				recordImagery();
			} catch (StorageException e) {
				// TODO handle OOM
				e.printStackTrace();
			}
		}
	}

	private void insertElementUnsafe(final OsmElement elem) {
		dirty = true;
		undo.save(elem);
		try {
			currentStorage.insertElementUnsafe(elem);
			apiStorage.insertElementUnsafe(elem);
			recordImagery();
		} catch (StorageException e) {
			// TODO handle OOMk
			e.printStackTrace();
		}
	}
	
	/**
	 * store the currently used imagery
	 */
	private void recordImagery() {
		if (!imageryRecorded) { // flag is reset when we change imagery 
			try {
				if (App.mainActivity != null) { // currently we only modify data when the main activity exists
					ArrayList<String>currentImagery = App.mainActivity.getMap().getImageryNames();
					for (String i:currentImagery) {
						if (!imagery.contains(i) && !"None".equalsIgnoreCase(i)) {
							imagery.add(i);
						}
					}
					imageryRecorded = true;
				}
			} 
			catch (Exception ignored) { // never fail on anything here
			}
			catch (Error ignored) {
			}
		}
	}
	
	public void setImageryRecorded(boolean recorded) {
		imageryRecorded = recorded;
	}

	
	/**
	 * Create apiStorage (aka the changes to the original data) based on state field of the elements.
	 * Assumes that apiStorage is empty. As a side effect it updates the id sequences for the creation of new elements.
	 */
	public synchronized void fixupApiStorage() {
		try {
			long minNodeId = 0;
			long minWayId = 0;
			long minRelationId = 0;
			List<Node> nl = new ArrayList<Node>(currentStorage.getNodes());
			for (Node n:nl) {
				if (n.getState()!=OsmElement.STATE_UNCHANGED) {
					apiStorage.insertElementUnsafe(n);
					if (n.getOsmId() < minNodeId) {
						minNodeId = n.getOsmId();
					}
				}
				if (n.getState()==OsmElement.STATE_DELETED) {
					currentStorage.removeElement(n);
				}
			}
			List<Way> wl = new ArrayList<Way>(currentStorage.getWays());
			for (Way w:wl) {
				if (w.getState()!=OsmElement.STATE_UNCHANGED) {
					apiStorage.insertElementUnsafe(w);
					if (w.getOsmId() < minWayId) {
						minWayId = w.getOsmId();
					}
				}
				if (w.getState()==OsmElement.STATE_DELETED) {
					currentStorage.removeElement(w);
				}
			}
			List<Relation> rl = new ArrayList<Relation>(currentStorage.getRelations());
			for (Relation r:rl) {
				if (r.getState()!=OsmElement.STATE_UNCHANGED) {
					apiStorage.insertElementUnsafe(r);
					if (r.getOsmId() < minRelationId) {
						minRelationId = r.getOsmId();
					}
				}
				if (r.getState()==OsmElement.STATE_DELETED) {
					currentStorage.removeElement(r);
				}
			}
			getFactory().setIdSequences(minNodeId, minWayId, minRelationId);
		} catch (StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Create empty relation
	 * @param members 
	 * @return
	 */
	public Relation createAndInsertRelation(List<OsmElement> members) {
		// undo - nothing done here, way gets saved/marked on insert
		dirty = true;
		
		Relation relation = factory.createRelationWithNewId();
		insertElementUnsafe(relation);
		if (members != null) {
			for (OsmElement e:members) {
				undo.save(e);
				RelationMember rm = new RelationMember("", e);
				relation.addMember(rm);
				e.addParentRelation(relation);
			}
		}
		return relation;
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

	public void addNodeToWay(final Node node, final Way way) throws OsmIllegalOperationException {
		dirty = true;
		undo.save(way);
		
		try {
			if (way.nodeCount() + 1 > Way.maxWayNodes)
				throw new OsmIllegalOperationException(App.mainActivity.getString(R.string.exception_too_many_nodes));
			apiStorage.insertElementSafe(way);
			way.addNode(node);
			way.updateState(OsmElement.STATE_MODIFIED);
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}

	public void addNodeToWayAfter(final Node nodeBefore, final Node newNode, final Way way) throws OsmIllegalOperationException {
		dirty = true;
		undo.save(way);
		
		try {
			if (way.nodeCount() + 1 > Way.maxWayNodes)
				throw new OsmIllegalOperationException(App.mainActivity.getString(R.string.exception_too_many_nodes));
			apiStorage.insertElementSafe(way);
			way.addNodeAfter(nodeBefore, newNode);
			way.updateState(OsmElement.STATE_MODIFIED);
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}

	public void appendNodeToWay(final Node refNode, final Node nextNode, final Way way) throws OsmIllegalOperationException {
		dirty = true;
		undo.save(way);
		try {
			if (way.nodeCount() + 1 > Way.maxWayNodes)
				throw new OsmIllegalOperationException(App.mainActivity.getString(R.string.exception_too_many_nodes));
			apiStorage.insertElementSafe(way);
			way.appendNode(refNode, nextNode);
			way.updateState(OsmElement.STATE_MODIFIED);
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}

	public void updateLatLon(final Node node, final int latE7, final int lonE7) {
		dirty = true;
		undo.save(node);
		try {
			apiStorage.insertElementSafe(node);
			node.setLat(latE7);
			node.setLon(lonE7);
			node.updateState(OsmElement.STATE_MODIFIED);
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}

	/**
	 * Mode all nodes in a way, since the nodes keep their ids, the way itself doesn't change and doesn't need to be saved
	 * apply translation only once to the first node if way is closed
	 * @param way
	 * @param deltaLatE7
	 * @param deltaLonE7
	 */
	public void moveWay(final Way way, final int deltaLatE7, final int deltaLonE7) {
		if (way.getNodes() == null) {
			Log.d("StorageDelegator", "moveWay way " + way.getOsmId() + " has no nodes!");
			return;
		}
		dirty = true;
		try {
			HashSet<Node> nodes = new HashSet<Node>(way.getNodes()); // Guarantee uniqueness
			for (Node nd:nodes) { 
				undo.save(nd);
				apiStorage.insertElementSafe(nd);
				nd.setLat(nd.getLat() + deltaLatE7);
				nd.setLon(nd.getLon() + deltaLonE7);
				nd.updateState(OsmElement.STATE_MODIFIED);
			}
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}
	
	/**
	 * Move a list of nodes apply translation only once 
	 * @param allNodes
	 * @param deltaLatE7
	 * @param deltaLonE7
	 */
	public void moveNodes(final List<Node> allNodes, final int deltaLatE7, final int deltaLonE7) {
		if (allNodes == null) {
			Log.d("StorageDelegator", "moveNodes  no nodes!");
			return;
		}
		dirty = true;
		try {
			HashSet<Node> nodes = new HashSet<Node>(allNodes); // Guarantee uniqueness
			for (Node nd:nodes) { 
				undo.save(nd);
				apiStorage.insertElementSafe(nd);
				nd.setLat(nd.getLat() + deltaLatE7);
				nd.setLon(nd.getLon() + deltaLonE7);
				nd.updateState(OsmElement.STATE_MODIFIED);
			}
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}
	
	/**
	 * arrange way nodes in a circle
	 * @param center
	 * @param way
	 */
	public void circulizeWay(int[] c, Way way) {
		if ((way.getNodes() == null) || (way.getNodes().size()<3)) {
			Log.d("StorageDelegator", "circulize way " + way.getOsmId() + " has no nodes or less than 3!");
			return;
		}
		dirty = true;
		try {
			HashSet<Node> nodes = new HashSet<Node>(way.getNodes()); // Guarantee uniqueness
			Coordinates coords[] = nodeListToCooardinateArray(new ArrayList<Node>(nodes));
			
			// save nodes for undo
			for (Node nd:nodes) { 
				undo.save(nd);
			}

			int width = App.mainActivity.getMap().getWidth();
			int height = App.mainActivity.getMap().getHeight();
			BoundingBox box = App.mainActivity.getMap().getViewBox();
			Coordinates center = new Coordinates(GeoMath.lonE7ToX(width, box, c[1]), GeoMath.latE7ToY(height,width, box, c[0]));
			
			// caclulate average radius
			double r = 0.0f;
			for (Coordinates p:coords) {
				Log.d("StorageDelegator","r="+Math.sqrt((p.x-center.x)*(p.x-center.x)+(p.y-center.y)*(p.y-center.y)));
				r = r + Math.sqrt((p.x-center.x)*(p.x-center.x)+(p.y-center.y)*(p.y-center.y));
			}
			r = r / coords.length;
			for (Coordinates p:coords) {
				double ratio = r/Math.sqrt((p.x-center.x)*(p.x-center.x)+(p.y-center.y)*(p.y-center.y));
				p.x = (float) ((p.x-center.x) * ratio)+center.x;
				p.y = (float) ((p.y-center.y) * ratio)+center.y;
			}
			int i=0;
			for (Node nd:nodes) { 
				nd.setLon(GeoMath.xToLonE7(width, box, coords[i].x));
				nd.setLat(GeoMath.yToLatE7(height, width, box, coords[i].y));
				apiStorage.insertElementSafe(nd);
				nd.updateState(OsmElement.STATE_MODIFIED);
				i++;
			}
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}
	
	/**
	 * Build groups of ways that have common nodes
	 * There must be a better way to do this, but they likely all fall afoul of our current data model
	 * @param ways
	 * @return
	 */
	private ArrayList<ArrayList<Way>> groupWays(List<Way> ways) {
		ArrayList<ArrayList<Way>> groups = new ArrayList<ArrayList<Way>>();
		int group = 0;
		int index = 0;
		int groupIndex = 1;
		groups.add(new ArrayList<Way>());
		Way startWay = ways.get(index);
		groups.get(group).add(startWay);
		do {
			do {
				for (Node nd:startWay.getNodes()) {
					for (Way w:ways) {
						if (w.getNodes().contains(nd) && !groups.get(group).contains(w)) {
							groups.get(group).add(w);
						}
					}
				}
				if (groupIndex < groups.get(group).size()) {
					startWay = groups.get(group).get(groupIndex);
					groupIndex++;
				}
			} while (groupIndex < groups.get(group).size());
			// repeat until no new ways are added in the loop

			// find the next way that is not in a group and start a new one
			for (;index<ways.size();index++) {
				Way w = ways.get(index);
				boolean found = false;
				for (ArrayList<Way>list:groups) {
					found = found || list.contains(w);
				}
				if (!found) {
					group++;
					groups.add(new ArrayList<Way>());
					startWay = w;
					groupIndex = 1;
					break;
				}
			}
		} while (index<ways.size());

		Log.d(DEBUG_TAG,"number of groups found " + groups.size());
		return groups;
	}
	
	/**
	 * "square" a way/polygon, based on the algorithm used by iD and before that by P2, originally written by Matt Amos
	 * If multiple ways are selected the ways are grouped in groups that share nodes and the groups individually squared.
	 * @param way
	 */
	public void orthogonalizeWay(List<Way> ways) {
		final int threshold = 10; // degrees within right or straight to alter
		final double lowerThreshold = Math.cos((90 - threshold) * Math.PI / 180);
		final double upperThreshold = Math.cos(threshold * Math.PI / 180);
		final double epsilon = 1e-4;
		
		dirty = true;
		try {
			// save nodes for undo
			// adding to a Set first removes duplication
			HashSet<Node> save = new HashSet<Node>();
			for (Way way:ways) {
				if (way.getNodes() != null) {
					save.addAll(way.getNodes()); 
				}
			}
			for (Node nd:save) {
				undo.save(nd);
			}
			
			List<ArrayList<Way>> groups = groupWays(ways);
			
			for (ArrayList<Way> wayList:groups) { 
				// Coordinates coords[] = nodeListToCooardinateArray(nodes);
				ArrayList<Coordinates[]> coordsArray = new ArrayList<Coordinates[]>();
				int totalNodes = 0;
				for (Way w:wayList) {
					coordsArray.add(nodeListToCooardinateArray(w.getNodes()));
					totalNodes += w.getNodes().size();
				}
				Coordinates a, b, c, p, q;
				
				double loopEpsilon = epsilon*(totalNodes/4D); //NOTE the original algorithm didn't take the number of corners in to account
				
				// iterate until score is low enough
				for (int iteration = 0; iteration < 1000; iteration++) {
					for (int coordIndex=0;coordIndex<coordsArray.size();coordIndex++) {
						Coordinates[] coords = coordsArray.get(coordIndex);
						int start = 0;
						int end = coords.length;
						if (!wayList.get(coordIndex).isClosed()) {
							start = 1;
							end = end-1;
						}
						Coordinates motions[] = new Coordinates[coords.length];
						for (int i=start;i<end;i++) {
							a = coords[(i - 1 + coords.length) % coords.length];
							b = coords[i];
							c = coords[(i + 1) % coords.length];
							p = a.subtract(b);
							q = c.subtract(b);
							double scale = 2 * Math.min(Math.hypot(p.x,p.y), Math.hypot(q.x,q.y));
							p = normalize(p, 1.0);
							q = normalize(q, 1.0);
							double dotp = filter((p.x * q.x + p.y * q.y), lowerThreshold, upperThreshold);

							// nasty hack to deal with almost-straight segments (angle is closer to 180 than to 90/270).   
							if (dotp < -0.707106781186547) {
								dotp += 1.0;
							}
							motions[i] = normalize(p.add(q), 0.1 * dotp * scale);
						}
						// apply position changes
						for (int i=start;i<end;i++) {
							coords[i] = coords[i].add(motions[i]);
						}
					}
					// calculate score
					double score = 0.0;
					for (int coordIndex=0;coordIndex<coordsArray.size();coordIndex++) {
						Coordinates[] coords = coordsArray.get(coordIndex);
						int start = 0;
						int end = coords.length;
						if (!wayList.get(coordIndex).isClosed()) {
							start = 1;
							end = end-1;
						}
						for (int i=start;i<end;i++) {
							// yes I know that this -nearly- duplicates the code above
							a = coords[(i - 1 + coords.length) % coords.length];
							b = coords[i];
							c = coords[(i + 1) % coords.length];
							p = a.subtract(b);
							q = c.subtract(b);
							p = normalize(p, 1.0);
							q = normalize(q, 1.0);
							double dotp = filter((p.x * q.x + p.y * q.y), lowerThreshold, upperThreshold);

							score = score + 2.0 * Math.min(Math.abs(dotp-1.0), Math.min(Math.abs(dotp), Math.abs(dotp+1.0)));
						}
					}
					// Log.d("StorageDelegator", "orthogonalize way iteration/score " + iteration + "/" + score);
					if (score < loopEpsilon) break; 
				}

				// prepare updated nodes for upload
				int width = App.mainActivity.getMap().getWidth();
				int height = App.mainActivity.getMap().getHeight();
				BoundingBox box = App.mainActivity.getMap().getViewBox();
				for (int wayIndex=0;wayIndex<wayList.size();wayIndex++) {
					List<Node> nodes = wayList.get(wayIndex).getNodes();
					Coordinates[] coords = coordsArray.get(wayIndex);
					for (int i = 0; i < nodes.size(); i++) { 
						Node nd = nodes.get(i);
						//	if (i == 0 || !nd.equals(firstNode)) {
						nd.setLon(GeoMath.xToLonE7(width, box, coords[i].x));
						nd.setLat(GeoMath.yToLatE7(height, width, box, coords[i].y));
						apiStorage.insertElementSafe(nd);
						nd.updateState(OsmElement.STATE_MODIFIED);
						//	}
					}
				}
			}
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}
	
	// TODO move this and following code somewhere else and generalize
	private Coordinates normalize(Coordinates p, double scale) {
		Coordinates result = p;
		double length = p.length();
		if (length != 0) {
			result = p.divide(length);
		}
		return result.multiply(scale);
	}
	
	private double filter(double v, double lower, double upper) {
		return (lower > Math.abs(v)) || (Math.abs(v) > upper) ? v : 0.0;
	}
	
	private class Coordinates {
		float x;
		float y;
		
		Coordinates (float x, float y) {
			this.x = x;
			this.y = y;
		}
		
		Coordinates subtract(Coordinates s) {
			return new Coordinates(this.x-s.x,this.y-s.y);
		}
		
		Coordinates add(Coordinates p) {
			return new Coordinates(this.x+p.x,this.y+p.y);
		}
		
		Coordinates multiply(double m) {
			return new Coordinates((float)(this.x*m),(float)(this.y*m));
		}
		
		Coordinates divide(double d) {
			return new Coordinates((float)(this.x/d),(float)(this.y/d));
		}
		
		float length() {
			return (float)Math.hypot(x, y);
		}
	}
	
	private Coordinates[] nodeListToCooardinateArray(List<Node>nodes) {
		Coordinates points[] = new Coordinates[nodes.size()];
		int width = App.mainActivity.getMap().getWidth();
		int height = App.mainActivity.getMap().getHeight();
		BoundingBox box = App.mainActivity.getMap().getViewBox();
		
		//loop over all nodes
		for (int i=0;i<nodes.size();i++) {
			points[i] = new Coordinates(0.0f,0.0f);
			points[i].x = GeoMath.lonE7ToX(width, box, nodes.get(i).getLon());
			points[i].y = GeoMath.latE7ToY(height, width, box, nodes.get(i).getLat());
		}
		return points;
	}
	
	/**
	 * Rotate all nodes in a way, since the nodes keep their ids, the way itself doesn't change and doesn't need to be saved
	 * apply translation only once to the first node if way is closed. Rotation is done in screen coords
	 * @param way
	 * @param v 
	 * @param k 
	 * @param j 
	 * @param deltaLatE7
	 * @param deltaLonE7
	 */
	public void rotateWay(final Way way, final float angle, final int direction, final float pivotX, final float pivotY, int w, int h, BoundingBox v) {
		if (way.getNodes() == null) {
			Log.d("StorageDelegator", "rotateWay way " + way.getOsmId() + " has no nodes!");
			return;
		}
		// Log.d("StorageDelegator","Roating " + angle + " around " + pivotY + " " + pivotX );
		dirty = true;
		try {
			HashSet<Node> nodes = new HashSet<Node>(way.getNodes()); // Guarantee uniqness
			for (Node nd:nodes) { 
				undo.save(nd);		
				apiStorage.insertElementSafe(nd);

				float nodeX = GeoMath.lonE7ToX(w, v, nd.getLon());
				float nodeY = GeoMath.latE7ToY(h, w, v, nd.getLat());
				float newX = pivotX + (nodeX-pivotX)*(float)Math.cos(angle) - direction * (nodeY-pivotY)*(float)Math.sin(angle);
				float newY = pivotY + direction * (nodeX-pivotX)*(float)Math.sin(angle) + (nodeY-pivotY)*(float)Math.cos(angle);
				int lat = GeoMath.yToLatE7(h, w, v, newY);
				int lon = GeoMath.xToLonE7(w, v, newX);
				nd.setLat(lat);
				nd.setLon(lon);
				nd.updateState(OsmElement.STATE_MODIFIED);
			}
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}
	
	/**
	 * updated for relation support
	 * @param node
	 */
	public void removeNode(final Node node) {
		// undo - node saved here, affected ways saved in removeWayNodes
		dirty = true;
		if (node.state == OsmElement.STATE_DELETED) {
			Log.d("StorageDelegator", "removeNode: nore already deleted " + node.getOsmId());
			return; // node was already deleted
		}
		undo.save(node);
		try {
			if (node.state == OsmElement.STATE_CREATED) {
				apiStorage.removeElement(node);
			} else {
				apiStorage.insertElementSafe(node);
			}
			removeWayNodes(node);
			removeElementFromRelations(node);
			currentStorage.removeNode(node);
			node.updateState(OsmElement.STATE_DELETED);
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}

	public void splitAtNode(final Node node) {
		Log.d("StorageDelegator", "splitAtNode for all ways");
		// undo - nothing done here, everything done in splitAtNode
		dirty = true;
		List<Way> ways = currentStorage.getWays(node);
		for (Way way : ways) {
			splitAtNode(way, node);
		}
	}

	/** 
	 * split a (closed) way at two points
	 * @param way
	 * @param node1
	 * @param node2
	 * @param createPolygons split in to two polygons 
	 * @return null if split failed or wasn't possible, the two resulting ways otherwise
	 */
	public Way[] splitAtNodes(Way way, Node node1, Node node2, boolean createPolygons) {
		Log.d("StorageDelegator", "splitAtNodes way " + way.getOsmId() + " node1 " + node1.getOsmId() + " node2 " + node2.getOsmId());
		// undo - old way is saved here, new way is saved at insert
		dirty = true;
		undo.save(way);
		
		List<Node> nodes = way.getNodes();
		if (nodes.size() < 3) {
			return null;
		}
		
		/* convention iterate over list, copy everything between first split node found and 2nd split node found
		 * if 2nd split node found first the same
		 */
		List<Node> nodesForNewWay = new LinkedList<Node>();
		List<Node> nodesForOldWay1 = new LinkedList<Node>();
		List<Node> nodesForOldWay2 = new LinkedList<Node>();
		boolean found1 = false;
		boolean found2 = false;
		for (Iterator<Node> it = way.getRemovableNodes(); it.hasNext();) {
			Node wayNode = it.next();
			if (!found1 && wayNode.getOsmId() == node1.getOsmId()) {
				found1 = true;
				nodesForNewWay.add(wayNode); 
				if (!found2)
					nodesForOldWay1.add(wayNode);
				else
					nodesForOldWay2.add(wayNode);
			} else if (!found2 && wayNode.getOsmId() == node2.getOsmId()) {
				found2 = true;
				nodesForNewWay.add(wayNode);
				if (!found1)
					nodesForOldWay1.add(wayNode);
				else
					nodesForOldWay2.add(wayNode);
			} else if ((found1 && !found2) || (!found1 && found2)) {
				nodesForNewWay.add(wayNode);
			} else if (!found1 && !found2) {
				nodesForOldWay1.add(wayNode);
			} else if (found1 && found2) {
				nodesForOldWay2.add(wayNode);
			}
		}
		
		// shuffle the nodes around for the original way so that they are in sequence and the way isn't closed
		Log.d("StorageDelegator","nodesForNewWay " + nodesForNewWay.size() + " oldNodes1 " + nodesForOldWay1.size() + " oldNodes2 " + nodesForOldWay2.size());
		List<Node> oldNodes = way.getNodes();
		oldNodes.clear();
		if (nodesForOldWay1.size() == 0) {
			oldNodes.addAll(nodesForOldWay2);
		} else if (nodesForOldWay2.size() == 0) {
			oldNodes.addAll(nodesForOldWay1);
		} else if (nodesForOldWay1.get(0) == nodesForOldWay2.get(nodesForOldWay2.size()-1)) {
			oldNodes.addAll(nodesForOldWay2);
			nodesForOldWay1.remove(0);
			oldNodes.addAll(nodesForOldWay1);
		} else {
			oldNodes.addAll(nodesForOldWay1);
			nodesForOldWay2.remove(0);
			oldNodes.addAll(nodesForOldWay2);
		}
		try {
			if (createPolygons && way.length() > 2) { // close the original way now
				way.addNode(way.getFirstNode());
			}
			way.updateState(OsmElement.STATE_MODIFIED);
			apiStorage.insertElementSafe(way);
	
			// create the new way
			Way newWay = factory.createWayWithNewId();
			newWay.addTags(way.getTags());
			newWay.addNodes(nodesForNewWay, false);
			if (createPolygons  && newWay.length() > 2) { // close the new way now
				newWay.addNode(newWay.getFirstNode());
			}
			insertElementUnsafe(newWay);
			
			// check for relation membership
			if (way.hasParentRelations()) {
				ArrayList<Relation> relations = new ArrayList<Relation>(way.getParentRelations()); // copy !
				dirty = true;
				/* iterate through relations, add the new way to the relation, for now simply after the old way */
				for (Relation r : relations) {
					Log.d("StorageDelegator", "splitAtNode processing relation (#" + r.getOsmId() + "/" + relations.size()  + ") " +  r.getDescription());
					RelationMember rm = r.getMember(way);
					undo.save(r);
					// no role specific code for now
					RelationMember newMember = new RelationMember(rm.getRole(), newWay);
					r.addMemberAfter(rm, newMember);
					newWay.addParentRelation(r);
					r.updateState(OsmElement.STATE_MODIFIED);
					apiStorage.insertElementSafe(r);
				}
			}
			recordImagery();
			Way[] result = new Way[2];
			result[0] = way;
			result[1] = newWay;
			return result;
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * split way at node with relation support
	 * @param way
	 * @param node
	 */
	public Way splitAtNode(final Way way, final Node node) {
		Log.d("StorageDelegator", "splitAtNode way " + way.getOsmId() + " node " + node.getOsmId());
		// undo - old way is saved here, new way is saved at insert
		dirty = true;
		undo.save(way);
		
		List<Node> nodes = way.getNodes();
		int occurances = Collections.frequency(way.getNodes(), node);
		// the following condition is fairly obscure and should likely be replaced by checking for position of the node in the way 
		if (nodes.size() < 3 || (way.isEndNode(node) && (way.isClosed()?occurances==2:occurances==1))) { 
			// protect against producing single node ways FIXME give feedback that this is not good
			Log.d("StorageDelegator", "splitAtNode can't split " + nodes.size() + " node long way at this node");
			return null;
		}
		// we assume this node is only contained in the way once.
		// else the user needs to split the remaining way again.
		List<Node> nodesForNewWay = new LinkedList<Node>();
		boolean found = false;
		boolean first = true; // node to split at can't be the first one
		for (Iterator<Node> it = way.getRemovableNodes(); it.hasNext();) {
			Node wayNode = it.next();
			if (!found && wayNode.getOsmId() == node.getOsmId() && !first) {
				found = true;
				nodesForNewWay.add(wayNode);
			} else if (found) {
				nodesForNewWay.add(wayNode);
				it.remove();	
			}
			first = false;
		}
		if (nodesForNewWay.size() <= 1) {
			Log.d("StorageDelegator", "splitAtNode can't split, new way would have " + nodesForNewWay.size() + " node(s)");
			return null; // do not create 1-node way
		}
		try {
			way.updateState(OsmElement.STATE_MODIFIED);
			apiStorage.insertElementSafe(way);
	
			// create the new way
			Way newWay = factory.createWayWithNewId();
			newWay.addTags(way.getTags());
			newWay.addNodes(nodesForNewWay, false);
			insertElementUnsafe(newWay);
			
			// check for relation membership
			if (way.getParentRelations() != null) {
				ArrayList<Relation> relations = new ArrayList<Relation>(way.getParentRelations()); // copy !
				dirty = true;
				/* iterate through relations, for all except restrictions add the new way to the relation, for now simply after the old way */
				for (Relation r : relations) {
					Log.d("StorageDelegator", "splitAtNode processing relation (#" + r.getOsmId() + "/" + relations.size()  + ") " +  r.getDescription());
					RelationMember rm = r.getMember(way);
					if (rm == null) {
						Log.d("StorageDelegator", "Unconsistent state detected way " + way.getOsmId() + " should be relation member" );
						ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
						ACRA.getErrorReporter().handleException(null);	
						continue;
					}
					undo.save(r);
					String type = r.getTagWithKey(Tags.KEY_TYPE);
					if (type != null){
						// attempt to handle turn restrictions correctly, if element is the via way, copying relation membership to both is ok
						if (type.equals(Tags.VALUE_RESTRICTION) && !rm.getRole().equals(Tags.VALUE_VIA)) { 
							// check if the old way has a node in common with the via relation member, if no assume the new way has
							ArrayList<RelationMember> rl =  r.getMembersWithRole(Tags.VALUE_VIA);
							boolean foundVia=false;
							for (int j=0;j<rl.size();j++)
							{
								RelationMember viaRm = rl.get(j);
								OsmElement viaE = viaRm.getElement();
								Log.d("StorageDelegator", "splitAtNode " + viaE.getOsmId());
								if (viaE instanceof Node) {
									if (((Way)rm.getElement()).hasNode((Node)viaE)) {
										foundVia = true;
									}
								} else if (viaE instanceof Way) {
									if (((Way)rm.getElement()).hasCommonNode((Way)viaE)) {
										foundVia = true;
									}
								}
							}
							Log.d("StorageDelegator", "splitAtNode foundVia " + foundVia);
							if (!foundVia) {
								// remove way from relation, add newWay to it
								RelationMember newMember = new RelationMember(rm.getRole(), newWay);
								r.replaceMember(rm, newMember);
								way.removeParentRelation(r); // way is dirty and will be changes anyway 
								newWay.addParentRelation(r);
							}
						} else {
							RelationMember newMember = new RelationMember(rm.getRole(), newWay);
							r.addMemberAfter(rm, newMember);
							newWay.addParentRelation(r);
						}
						
					} else {
						RelationMember newMember = new RelationMember(rm.getRole(), newWay);
						r.addMemberAfter(rm, newMember);
						newWay.addParentRelation(r);
					}
					r.updateState(OsmElement.STATE_MODIFIED);
					apiStorage.insertElementSafe(r);
				}
			}
			recordImagery();
			return newWay;
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
			return null;
		}
	}	

	/**
	 * Merge two nodes into one.
	 * Updated for relation support
	 * @param mergeInto The node to merge into. Tags are combined.
	 * @param mergeFrom The node to merge from. Is deleted.
	 */
	public boolean mergeNodes(Node mergeInto, Node mergeFrom) {
		boolean mergeOK = true;
		dirty = true;
		// first determine if one of the nodes already has a valid id, if it is not and other node has valid id swap
		// else check version numbers this helps preserve history
		if (((mergeInto.getOsmId() < 0) && (mergeFrom.getOsmId() > 0)) || mergeInto.getOsmVersion() < mergeFrom.getOsmVersion()) {
		// swap
			Log.d("StorageDelegator", "swap into #" + mergeInto.getOsmId() + " with from #" + mergeFrom.getOsmId());
			Node tmpNode = mergeInto;
			mergeInto = mergeFrom;
			mergeFrom = tmpNode;
			Log.d("StorageDelegator", "now into #" + mergeInto.getOsmId() + " from #" + mergeFrom.getOsmId());
		}
		mergeOK = !roleConflict(mergeInto, mergeFrom); // need to do this before we remove objects from relations.
		// merge tags
		setTags(mergeInto, OsmElement.mergedTags(mergeInto, mergeFrom));
		// if merging the tags creates multiple-value tags, mergeOK should be set to false
		for (String v:mergeInto.getTags().values()) {
			if (v.indexOf(";") >= 0) {
				mergeOK = false;
				break;
			}
		}
		// replace references to mergeFrom node in ways with mergeInto
		for (Way way : currentStorage.getWays(mergeFrom)) {
			replaceNodeInWay(mergeFrom, mergeInto, way);
		}
		// belt and suspenders not really necessary
		for (Way way : apiStorage.getWays(mergeFrom)) {
			replaceNodeInWay(mergeFrom, mergeInto, way);
		}
		mergeElementsRelations(mergeInto, mergeFrom); 
		// delete mergeFrom node
		removeNode(mergeFrom);
		recordImagery();
		return mergeOK;
	}
	
	/**
	 * Merges two ways by prepending/appending all nodes from the second way to the first one, then deleting the second one.
	 * 
	 * Updated for relation support if roles are not the same the merge will fail.
	 * @param mergeInto Way to merge the other way into. This way will be kept if it has a valid id.
	 * @param mergeFrom Way to merge into the other. 
	 * @return false if we had tag conflicts
	 * @throws OsmIllegalOperationException 
	 */
	public boolean mergeWays(Way mergeInto, Way mergeFrom) throws OsmIllegalOperationException {
		boolean mergeOK = true;
		
		if ((mergeInto.nodeCount() + mergeFrom.nodeCount()) > Way.maxWayNodes)
			throw new OsmIllegalOperationException(App.mainActivity.getString(R.string.exception_too_many_nodes));
		
		// first determine if one of the nodes already has a valid id, if it is not and other node has valid id swap
		// else check version numbers this helps preserve history
		if (((mergeInto.getOsmId() < 0) && (mergeFrom.getOsmId() > 0)) || mergeInto.getOsmVersion() < mergeFrom.getOsmVersion()) {
			// swap
			Log.d("StorageDelegator", "swap into #" + mergeInto.getOsmId() + " with from #" + mergeFrom.getOsmId());
			Way tmpWay = mergeInto;
			mergeInto = mergeFrom;
			mergeFrom = tmpWay;
			Log.d("StorageDelegator", "now into #" + mergeInto.getOsmId() + " from #" + mergeFrom.getOsmId());
		}
		
		mergeOK = !roleConflict(mergeInto, mergeFrom); // need to do this before we remove ways from relations.
		
		// undo - mergeInto way saved here, mergeFrom way will not be changed directly and will be saved in removeWay
		dirty = true;
		undo.save(mergeInto);
		removeWay(mergeFrom); // have to do this here because otherwise the way will be saved with potentially reversed tags
		
		List<Node> newNodes = new ArrayList<Node>(mergeFrom.getNodes());
		boolean atBeginning;
		
		if (mergeInto.getFirstNode().equals(mergeFrom.getFirstNode())) {
			// Result: f3 f2 f1 (f0=)i0 i1 i2 i3   (f0 = 0th node of mergeFrom, i1 = 1st node of mergeInto)
			atBeginning = true;
			//check for direction dependent tags
			Map<String, String> dirTags = Reverse.getDirectionDependentTags(mergeFrom);
			if (dirTags != null) {
				Reverse.reverseDirectionDependentTags(mergeFrom,dirTags, true);
			}
			mergeOK = !mergeFrom.notReversable();
			Collections.reverse(newNodes);
			newNodes.remove(newNodes.size()-1); // remove "last" (originally first) node after reversing
			reverseWayNodeTags(newNodes);
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
			//check for direction dependent tags
			Map<String, String> dirTags = Reverse.getDirectionDependentTags(mergeFrom);
			if (dirTags != null) {
				Reverse.reverseDirectionDependentTags(mergeFrom, dirTags, true);
			}
			mergeOK = !mergeFrom.notReversable();
			newNodes.remove(newNodes.size()-1); // remove last node before reversing
			reverseWayNodeTags(newNodes);
			Collections.reverse(newNodes);
		} else {
			throw new RuntimeException("attempted to merge non-mergeable nodes. this is a bug.");
		}
		
		// merge tags (after any reversal has been done)
		setTags(mergeInto, OsmElement.mergedTags(mergeInto, mergeFrom));
		// if merging the tags creates multiple-value tags, mergeOK should be set to false
		for (String v:mergeInto.getTags().values()) {
			if (v.indexOf(";") >= 0) {
				mergeOK = false;
				break;
			}
		}
		
		mergeInto.addNodes(newNodes, atBeginning);
		mergeInto.updateState(OsmElement.STATE_MODIFIED);
		insertElementSafe(mergeInto);
		mergeElementsRelations(mergeInto, mergeFrom);
		
		return mergeOK;
	}
	
	/**
	 * reverse any direction dependent tags on the way nodes
	 * @param nodes
	 */
	private void reverseWayNodeTags(List<Node> nodes) {
		for (Node n:nodes) {
			Map<String,String> nodeDirTags = Reverse.getDirectionDependentTags(n);
			if (nodeDirTags!=null) {
				undo.save(n);
				Reverse.reverseDirectionDependentTags(n,nodeDirTags, true);
				n.updateState(OsmElement.STATE_MODIFIED);
				try {
					apiStorage.insertElementSafe(n);
				} catch (StorageException e) {
					//TODO handle OOM
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * return true if elements have different roles in the same relation
	 * @param o1
	 * @param o2
	 * @return
	 */
	private boolean roleConflict(OsmElement o1, OsmElement o2) {	
		ArrayList<Relation> r1 = o1.getParentRelations() != null ? o1.getParentRelations() : new ArrayList<Relation>();
		ArrayList<Relation> r2 = o2.getParentRelations() != null ? o2.getParentRelations() : new ArrayList<Relation>();
		for (Relation r : r1) {
			if (r2.contains(r)) {
				RelationMember rm1 = r.getMember(o1);
				RelationMember rm2 = r.getMember(o2);
				if (rm1 != null && rm2 != null) { // if either of these are null something is broken
					String role1 = rm1.getRole();
					String role2 = rm2.getRole();
					if ((role1 != null && role2 == null) || (role1 == null && role2 != null) || (role1 != role2 && !role1.equals(role2))) {
						Log.d(DEBUG_TAG,"role conflict between " + o1.getDescription() + " role " + role1 + " and " + o2.getDescription() + " role " + role2);
						return true;
					}
				} else {
					Log.d(DEBUG_TAG,"inconsistent relation membership in " + r.getOsmId() + " for " + o1.getOsmId() + " and " + o2.getOsmId());
					ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
					ACRA.getErrorReporter().handleException(null);
					return true;
				}
			}
		}
		return false;
	}
	

	
	/**
	 * Unjoins ways connected at the given node.
	 * Updated for relation support
	 * @param node The node connecting ways that are to be unjoined.
	 */
	/**
	 * @param node
	 */
	public void unjoinWays(final Node node) {
		List<Way> ways = currentStorage.getWays(node);
		try {
			if (ways.size() > 1) {
				boolean first = true;
				for (Way way : ways) {
					if (first) {
						// first way doesn't need to be changed
						first = false;
					} else {
						// subsequent ways
						dirty = true;
						// create a new node that duplicates the given node
						Node newNode = factory.createNodeWithNewId(node.lat, node.lon);
						newNode.addTags(node.getTags());
						insertElementUnsafe(newNode);
						// replace the given node in the way with the new node
						undo.save(way);
						List<Node> nodes = way.getNodes();
						nodes.set(nodes.indexOf(node), newNode);
						way.updateState(OsmElement.STATE_MODIFIED);
						apiStorage.insertElementSafe(way);
						
						// check if node is in a relation, if yes, add to new node
						// should probably check for restrictions
						if  (node.hasParentRelations()) {
							ArrayList<Relation> relations = node.getParentRelations();
							/* iterate through relations, for all except restrictions add the new node to the relation, for now simply after the old node */
							for (Relation r : relations) {
								RelationMember rm = r.getMember(node);
								undo.save(r);
								String type = r.getTagWithKey(Tags.KEY_TYPE);
								if (type != null){
									if (type.equals(Tags.VALUE_RESTRICTION)) {
										// doing nothing for now at least gives a chance of being right :-)
									} else {
										RelationMember newMember = new RelationMember(rm.getRole(), newNode);
										r.addMemberAfter(rm, newMember);
										newNode.addParentRelation(r);
									}
									
								} else {
									RelationMember newMember = new RelationMember(rm.getRole(), newNode);
									r.addMemberAfter(rm, newMember);
									newNode.addParentRelation(r);
								}
								r.updateState(OsmElement.STATE_MODIFIED);
								apiStorage.insertElementSafe(r);
							}
						}
					}
				}
				recordImagery();
			}
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}
	
	/**
	 * Replace the given node in any ways it is member of.
	 * @param node The node to be replaced.
	 * @return null if node was not member of a way, the replacement node if it was
	 */
	public Node replaceNode(final  Node node) {
		List<Way> ways = currentStorage.getWays(node);
		if (ways.size() > 0) {
			Node newNode = factory.createNodeWithNewId(node.lat, node.lon);
			insertElementUnsafe(newNode);
			dirty = true;
			for (Way way : ways) {
				replaceNodeInWay(node, newNode,  way);
			}
			return newNode;
		}
		return null;
	}
	
	/**
	 * Reverses a way
	 * @param way
	 * @return true is way had tags that needed to be reversed
	 */
	public boolean reverseWay(final Way way) {
		dirty = true;
		undo.save(way);
		//check for direction dependent tags
		Map<String, String> dirTags = Reverse.getDirectionDependentTags(way);
		//TODO inform user about the tags
		if (dirTags != null) {
			Reverse.reverseDirectionDependentTags(way, dirTags, false); // assume he only wants to change the oneway direction for now
		}
		reverseWayNodeTags(way.getNodes());
		way.reverse();
		List<Relation>relations = Reverse.getRelationsWithDirectionDependentRoles(way);
		if (relations != null) {
			Reverse.reverseRoleDirection(way,relations);
			for (Relation r:relations) {
				r.updateState(OsmElement.STATE_MODIFIED);
				try {
					apiStorage.insertElementSafe(r);
				} catch (StorageException e) {
					// TODO handle OOM
					e.printStackTrace();
				}
			}
		}
		way.updateState(OsmElement.STATE_MODIFIED);
		try {
			apiStorage.insertElementSafe(way);
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
		return ((dirTags != null) && dirTags.containsKey(Tags.KEY_ONEWAY));
	}

	private void replaceNodeInWay(final Node existingNode, final Node newNode, final Way way) {
		dirty = true;
		undo.save(way);
		way.replaceNode(existingNode, newNode);
		way.updateState(OsmElement.STATE_MODIFIED);
		try {
			apiStorage.insertElementSafe(way);
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}

	private int removeWayNodes(final Node node) {
		// undo - node is not changed, affected way(s) are stored below
		dirty = true;
		int deleted = 0;
		try {
			List<Way> ways = currentStorage.getWays(node);
			for (Way way:ways) {
				undo.save(way);
				if (way.isClosed() && way.isEndNode(node) && way.getNodes().size() > 1) { // note protection against degenerate closed ways
					way.removeNode(node);
					way.addNode(way.getFirstNode());
				} else {
					way.removeNode(node);
				}
				//remove way when less than two waynodes exist
				if (way.getNodes().size() < 2) {
					removeWay(way);
				} else {
					way.updateState(OsmElement.STATE_MODIFIED);
					apiStorage.insertElementSafe(way);
				}
				deleted++;
			}
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
		return deleted;
	}

	/**
	 * Deletes a way
	 * updated for relations
	 * @param way
	 */
	public void removeWay(final Way way) {
		dirty = true;
		undo.save(way);
		try {
			currentStorage.removeWay(way);
			if (apiStorage.contains(way)) {
				if (way.getState() == OsmElement.STATE_CREATED) {
					apiStorage.removeElement(way);
				}
			} else {
				apiStorage.insertElementSafe(way);
			}
			removeElementFromRelations(way);
			way.updateState(OsmElement.STATE_DELETED);
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}

	/**
	 * updated for relation support
	 * @param node
	 */
	public void removeRelation(final Relation relation) {
		// undo - node saved here, affected ways saved in removeWayNodes
		dirty = true;
		undo.save(relation);
		try {
			if (relation.state == OsmElement.STATE_CREATED) {
				apiStorage.removeElement(relation);
			} else {
				apiStorage.insertElementSafe(relation);
			}
			removeElementFromRelations(relation);
			removeRelationFromMembers(relation);
			currentStorage.removeRelation(relation);
			relation.updateState(OsmElement.STATE_DELETED);
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove backlinks in elements
	 * @param relation
	 */
	private void removeRelationFromMembers(final Relation relation) {
		for (RelationMember rm: relation.getMembers()) {
			OsmElement e = rm.getElement();
			if (e != null) { // if null the element wasn't downloaded
				undo.save(e);
				e.removeParentRelation(relation);
			}
		}
	}
	
	/**
	 * Note the element does not need to have its state changed of be stored in the API sotrage since the 
	 * parent relation back link is just internal.
	 * @param element
	 */
	private void removeElementFromRelations(final OsmElement element) {
		try {
			if (element.hasParentRelations()) {
				ArrayList<Relation> relations = new ArrayList<Relation>(element.getParentRelations()); // need copy!
				for (Relation r : relations) {
					Log.i("StorageDelegator", "removing " + element.getName() + " #" + element.getOsmId() + " from relation #" + r.getOsmId());
					dirty = true;
					undo.save(r);
					r.removeMember(r.getMember(element));
					r.updateState(OsmElement.STATE_MODIFIED);
					apiStorage.insertElementSafe(r);
					undo.save(element);
					element.removeParentRelation(r);
					Log.i("StorageDelegator", "... done");
				}
				recordImagery();
			}
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}
	
	/**
	 * Note the element does not need to have its state changed of be stored in the API storage since the 
	 * parent relation back link is just internal.
	 * @param element
	 */
	private void removeElementFromRelation(final OsmElement element, Relation r) {
		Log.i("StorageDelegator", "removing " + element.getName() + " #" + element.getOsmId() + " from relation #" + r.getOsmId());
		dirty = true;
		undo.save(r);
		try {
			r.removeMember(r.getMember(element));
			r.updateState(OsmElement.STATE_MODIFIED);
			apiStorage.insertElementSafe(r);
			undo.save(element);
			element.removeParentRelation(r);
			Log.i("StorageDelegator", "... done");
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
	}
	
	/*
	 * remove non-downloaded element from relation
	 */
	public void removeElementFromRelation(String type, final Long elementId, Relation r) {
		Log.i("StorageDelegator", "removing  #" + elementId + " from relation #" + r.getOsmId());
		dirty = true;
		undo.save(r);
		r.removeMember(r.getMember(type, elementId));
		r.updateState(OsmElement.STATE_MODIFIED);
		try {
			apiStorage.insertElementSafe(r);
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
		//
		Log.i("StorageDelegator", "... done");
	}
	
	/**
	 * add element to relation at a specific position
	 * @param e
	 * @param pos
	 * @param role
	 * @param rel
	 */
	private void addElementToRelation(final OsmElement e, final int pos, final String role, final Relation rel)
	{
		dirty = true;
		undo.save(rel);
		undo.save(e);

		RelationMember newMember = new RelationMember(role, e);
		rel.addMember(pos, newMember);
		e.addParentRelation(rel);

		rel.updateState(OsmElement.STATE_MODIFIED);
		try {
			apiStorage.insertElementSafe(rel);
			recordImagery();
		} catch (StorageException sex) {
			//TODO handle OOM
			sex.printStackTrace();
		}
	}
	
	/**
	 * add element to relation at end
	 * @param e
	 * @param role
	 * @param rel
	 */
	public void addElementToRelation(final OsmElement e, final String role, final Relation rel)
	{

		dirty = true;
		undo.save(rel);
		undo.save(e);

		RelationMember newMember = new RelationMember(role, e);
		rel.addMember(newMember);
		e.addParentRelation(rel);

		rel.updateState(OsmElement.STATE_MODIFIED);
		try {
			apiStorage.insertElementSafe(rel);
			recordImagery();
		} catch (StorageException sex) {
			//TODO handle OOM
			sex.printStackTrace();
		}
	}
	
	/**
	 * add element to relation at end
	 * @param e
	 * @param role
	 * @param rel
	 */
	public void addElementToRelation(final RelationMember newMember, final Relation rel)
	{
		OsmElement e = newMember.getElement();
		if (e == null) {
			Log.e(DEBUG_TAG, "addElementToRelation element not found");
			return;
		}
	
		dirty = true;
		undo.save(rel);

		undo.save(e);

		rel.addMember(newMember);
		e.addParentRelation(rel);

		rel.updateState(OsmElement.STATE_MODIFIED);
		try {
			apiStorage.insertElementSafe(rel);
			recordImagery();
		} catch (StorageException sex) {
			//TODO handle OOM
			sex.printStackTrace();
		}
	}
	
	/**
	 * set role for e in relation rel to new value role
	 * @param e
	 * @param role
	 * @param rel
	 */
	private void setRole(final OsmElement e, final String role, final Relation rel)
	{
		dirty = true;
		undo.save(rel);

		RelationMember oldRm = rel.getMember(e);
		RelationMember rm = new RelationMember(oldRm); // necessary or else we will overwrite the role string in undo storage

		rm.setRole(role);
		rel.replaceMember(oldRm, rm);
		
		rel.updateState(OsmElement.STATE_MODIFIED);
		try {
			apiStorage.insertElementSafe(rel);
			recordImagery();
		} catch (StorageException sex) {
			//TODO handle OOM
			sex.printStackTrace();
		}
		Log.w("StorageDelegator", "set role for #" + e.getOsmId() + " to " + role + " in relation #" + rel.getOsmId());
	}
	
	/**
	 * set role for e in relation rel to new value role
	 * @param e
	 * @param role
	 * @param rel
	 */
	public void setRole(final String type, final long elementId, final String role, final Relation rel)
	{
		dirty = true;
		undo.save(rel);

		RelationMember oldRm = rel.getMember(type, elementId);
		RelationMember rm = new RelationMember(oldRm); // necessary or else we will overwrite the role string in undo storage

		rm.setRole(role);
		rel.replaceMember(oldRm, rm);
		
		rel.updateState(OsmElement.STATE_MODIFIED);
		try {
			apiStorage.insertElementSafe(rel);
			recordImagery();
		} catch (StorageException e) {
			//TODO handle OOM
			e.printStackTrace();
		}
		Log.w("StorageDelegator", "set role for #" + elementId+ " to " + role + " in relation #" + rel.getOsmId());
	}
	
	/**
	 * compare current relations e is a member of to new state parents and make it so
	 * @param e
	 * @param parents
	 */
	public void updateParentRelations(final OsmElement e,
			final HashMap<Long, String> parents) {
		Log.d(DEBUG_TAG,"updateParentRelations new parents size " + parents.size());
		ArrayList<Relation> origParents = e.getParentRelations() != null ? new ArrayList<Relation>(e.getParentRelations()) : new ArrayList<Relation>();
		
		for (Relation o: origParents) { // find changes to existing memberships
			if (!parents.containsKey(Long.valueOf(o.getOsmId()))) {
				removeElementFromRelation(e, o); // saves undo state
				continue;
			}
			if (parents.containsKey(Long.valueOf(o.getOsmId()))){
				String newRole = parents.get(Long.valueOf(o.getOsmId()));
				if (!o.getMember(e).getRole().equals(newRole)) {
					setRole(e, newRole, o);
				}
			}
		}
		// add as new member to relation
		for (Long l : parents.keySet()) {
			Log.d(DEBUG_TAG,"updateParentRelations new parent " + l.longValue());
			if (l.longValue() != -1) { // 
				Relation r = (Relation) currentStorage.getOsmElement(Relation.NAME, l.longValue());
				if (!origParents.contains(r)) {
					Log.d(DEBUG_TAG,"updateParentRelations adding " + e.getDescription() + " to " + r.getDescription());
					addElementToRelation(e, -1, parents.get(l), r); // append for now only
				}
			}
		}
	}
	
	/**
	 * compare current list of relations members to new list and apply the necessary changes
	 * currently doesn't handle additions or changes in sequence
	 * @param r			the relation
	 * @param members  	new list of members
	 */
	public void updateRelation(Relation r, ArrayList<RelationMemberDescription> members) {
		
		dirty = true;
		undo.save(r);
		boolean changed = false;
		ArrayList<RelationMember> origMembers = new ArrayList<RelationMember>(r.getMembers());
		LinkedHashMap<String,RelationMember> membersHash = new LinkedHashMap<String,RelationMember>();
		for (RelationMember rm: r.getMembers()) {
			membersHash.put(rm.getType()+"-"+rm.getRef(),rm);
		}
		ArrayList<RelationMember> newMembers = new ArrayList<RelationMember>(); 
		for (int i = 0; i < members.size(); i++) {
			RelationMemberDescription rmd = members.get(i);
			String key = rmd.getType()+"-"+rmd.getRef();
			OsmElement e = rmd.getElement();
			RelationMember rm = membersHash.get(key);
			if (rm != null) {
				int origPos = origMembers.indexOf(rm);
				String newRole = rmd.getRole();
				if (!rm.getRole().equals(newRole)) {
					changed = true;
					rm = new RelationMember(rm); // allocate new element
					rm.setRole(newRole);
				}
				newMembers.add(rm); // existing member simply add to list
				if (origPos != i) {
					changed = true;
				}
				membersHash.remove(key);
			} else { // new member
				changed = true;
				RelationMember newMember = null;
				if (e != null) {  // downloaded
					newMember = new RelationMember(rmd.getRole(), e);
				} else {
					newMember = new RelationMember(rmd.getType(), rmd.getRef(), rmd.getRole());
				}
				newMembers.add(newMember);
			}
		}
		for (RelationMember rm: membersHash.values()) {
			changed = true;
			OsmElement e = rm.getElement();
			if (e != null) {
				undo.save(e);
				e.removeParentRelation(r);
			} 
		}
		
		if (changed) {
			r.replaceMembers(newMembers);
			r.updateState(OsmElement.STATE_MODIFIED);
			try {
				apiStorage.insertElementSafe(r);
				recordImagery();
			} catch (StorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// FIXME remove relation from undo storage
		}
	}

	/**
	 * Add further members without role to an existing relation
	 * @param relation
	 * @param members
	 */
	public void addMembersToRelation(Relation relation,	ArrayList<OsmElement> members) {
		dirty = true;
		for (OsmElement e:members) {
			undo.save(e);
			RelationMember rm = new RelationMember("", e);
			relation.addMember(rm);
			e.addParentRelation(relation);
		}
		relation.updateState(OsmElement.STATE_MODIFIED);
		insertElementSafe(relation);
	}

	
	/**
	 * Assumes mergeFrom will deleted by caller and doesn't update back refs
	 * @param mergeInto
	 * @param mergeFrom
	 */
	private void mergeElementsRelations(final OsmElement mergeInto, final OsmElement mergeFrom) {
		ArrayList<Relation> fromRelations = mergeFrom.getParentRelations() != null ? new ArrayList<Relation>(mergeFrom.getParentRelations()) : new ArrayList<Relation>(); // copy just to be safe
		ArrayList<Relation> toRelations = mergeInto.getParentRelations() != null ? mergeInto.getParentRelations() : new ArrayList<Relation>();
		try {
			for (Relation r : fromRelations) {
				if (!toRelations.contains(r)) {
					dirty = true;
					undo.save(r);
					RelationMember rm = r.getMember(mergeFrom);
					// create new member with same role
					RelationMember newRm = new RelationMember(rm.getRole(), mergeInto);
					// insert at same place
					r.replaceMember(rm, newRm);
					r.updateState(OsmElement.STATE_MODIFIED);
					apiStorage.insertElementSafe(r);
					mergeInto.addParentRelation(r);
					mergeInto.updateState(OsmElement.STATE_MODIFIED);
					apiStorage.insertElementSafe(mergeInto);
				}
			}
			recordImagery();
		} catch (StorageException sex) {
			//TODO handle OOM
		}
	}
	
	/**
	 * ake a copy of the element and store it in the clipboard
	 * @param e
	 * @param lat
	 * @param lon
	 */
	public void copyToClipboard(OsmElement e, int lat, int lon) {
		dirty = true; // otherwise clipboard will not get saved without other changes
		if (e instanceof Node) {
			Node newNode = factory.createNodeWithNewId(((Node) e).getLat(), ((Node) e).getLon());
			newNode.setTags(e.getTags());
			clipboard.copyTo(newNode, lat, lon);
		} else if (e instanceof Way) {
			Way newWay = factory.createWayWithNewId();
			newWay.setTags(e.getTags());
			for (Node nd: ((Way)e).getNodes()) {
				Node newNode = factory.createNodeWithNewId(nd.getLat(), nd.getLon());
				newNode.setTags(nd.getTags());
				newWay.addNode(nd);
			}
			clipboard.copyTo(newWay, lat, lon);
		}	
	}
	
	/**
	 * cut original element to clipboard, does -not- preserve relation memberships
	 * @param e
	 * @param lat
	 * @param lon
	 */
	public void cutToClipboard(OsmElement e, int lat, int lon) {
		dirty = true; // otherwise clipboard will not get saved without other changes
		if (e instanceof Node) {
			clipboard.cutTo(e, lat, lon);
			removeNode((Node)e);
		} else if (e instanceof Way) {
			// clone all nodes that are members of other ways
			ArrayList<Node> nodes = new ArrayList<Node>(((Way)e).getNodes());
			for (Node nd: nodes) {
				if (currentStorage.getWays(nd).size() > 1) { // 1 is expected (our way will be deleted later)
					Log.d("StorageDelegator","Duplicating node");
					Node newNode = factory.createNodeWithNewId(nd.getLat(), nd.getLon());
					newNode.setTags(nd.getTags());
					((Way)e).replaceNode(nd, newNode);
				}
			}
			clipboard.cutTo(e, lat, lon);
			removeWay((Way)e);
			nodes = new ArrayList<Node>(((Way)e).getNodes());
			for (Node nd: nodes) {
				removeNode(nd); // 
			}
		}
	}
	
	public boolean pasteFromClipboard(int lat, int lon) {
		OsmElement e = clipboard.pasteFrom();
		// if the clipboard isn't empty now we need to clone the element
		if (!clipboard.isEmpty()) { // paste from copy
			if (e instanceof Node) {
				Node newNode = factory.createNodeWithNewId(lat, lon);
				newNode.setTags(e.getTags());
				insertElementSafe(newNode);
			} else if (e instanceof Way) {
				Way newWay = factory.createWayWithNewId();
				newWay.setTags(e.getTags());
				int deltaLat = lat - clipboard.getSelectionLat();
				int deltaLon = lon - clipboard.getSelectionLon();
				Node firstNode = ((Way)e).getFirstNode();
				List<Node> nodes = ((Way)e).getNodes();
				for (int i = 0; i < nodes.size(); i++) { 
					Node nd = nodes.get(i);
					if (i == 0 || !nd.equals(firstNode)) {
						Log.d("StorageDelegator", "Pasting to " + (nd.getLat() + deltaLat) + " " + (nd.getLon() + deltaLon));
						Node newNode = factory.createNodeWithNewId(nd.getLat() + deltaLat, nd.getLon() + deltaLon);
						newNode.setTags(nd.getTags());
						newWay.addNode(newNode);
						insertElementSafe(newNode);
					} else if (nd.equals(firstNode)) {
						newWay.addNode(newWay.getFirstNode());
					}
				}
				insertElementSafe(newWay);
			}	
		} else { // paste from cut
			if (e instanceof Node) {
				((Node)e).setLat(lat);
				((Node)e).setLon(lon);
			} else if (e instanceof Way) {
				int deltaLat = lat - clipboard.getSelectionLat();
				int deltaLon = lon - clipboard.getSelectionLon();
				Node firstNode = ((Way)e).getFirstNode();
				for (int i = 0; i < ((Way)e).getNodes().size(); i++) { 
					Node nd = ((Way)e).getNodes().get(i);
					if (i == 0 || !nd.equals(firstNode)) {
						nd.setLat(nd.getLat() + deltaLat);
						nd.setLon(nd.getLon() + deltaLon);
						nd.updateState(OsmElement.STATE_MODIFIED);
						insertElementSafe(nd); 
					} 
				}
			}
			e.updateState(OsmElement.STATE_MODIFIED);
			insertElementSafe(e); 
		}
		return e != null;
	}
	
	public boolean clipboardIsEmpty() {
		return clipboard.isEmpty();
	}

	public Storage getApiStorage() {
		return apiStorage;
	}
	
	public Storage getCurrentStorage() {
		return currentStorage;
	}

//	public BoundingBox getOriginalBox() {
//		return currentStorage.getBoundingBox().copy();
//	}

	public List<BoundingBox> getBoundingBoxes() {
		// TODO make a copy?
		return  currentStorage.getBoundingBoxes();
	}
	
	public void setOriginalBox(final BoundingBox box) {
		dirty = true;
		currentStorage.setBoundingBox(box);
	}
	
	public void addBoundingBox(BoundingBox box) {
		dirty = true;
		currentStorage.addBoundingBox(box);
	}
	
	public void deleteBoundingBox(BoundingBox box) {
		dirty = true;
		currentStorage.deleteBoundingBox(box);
	}
	
	public int getApiNodeCount() {
		return apiStorage.getNodes().size();
	}

	public int getApiWayCount() {
		return apiStorage.getWays().size();
	}
	
	public int getApiRelationCount() {
		return apiStorage.getRelations().size();
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
	 * @param ctx TODO
	 * @throws IOException
	 */
	public void writeToFile(Context ctx) throws IOException { 
		if (apiStorage == null || currentStorage == null) {
			// don't write empty state files
			Log.i("StorageDelegator", "storage delegator empty, skipping save");
			return;
		}
		if (!dirty) { // dirty flag should only be set if we have actually read/loaded/changed something
			Log.i("StorageDelegator", "storage delegator not dirty, skipping save");
			return;
		}

		if (readingLock.tryLock()) {
			// TODO this doesn't really help with error conditions need to throw exception
			if (savingHelper.save(ctx, FILENAME, this, true)) { 
				dirty = false;
			} else {
				// this is essentially catastrophic and can only happen if something went really wrong
				// running out of memory or disk, or HW failure
				if (ctx != null) {
					try {
						Toast.makeText(ctx, R.string.toast_statesave_failed, Toast.LENGTH_LONG).show();
					} catch (Exception ignored) {
						Log.e(DEBUG_TAG,"Emergency toast failed with " + ignored.getMessage());
					} catch (Error ignored) {
						Log.e(DEBUG_TAG,"Emergency toast failed with " + ignored.getMessage());
					}
				}
				SavingHelper.asyncExport(ctx, this); // ctx == null is checked in method
				Log.d("StorageDelegator", "save of state file failed, written emergency change file" );
			}
			readingLock.unlock();
		} else {
			Log.i("StorageDelegator", "storage delegator state being read, skipping save");
		}
	}

	/**
	 * Read save data from standard file
	 * Loads the storage data from the default storage file
	 * NOTE: lock is acquired in logic before this is called
	 */
	public boolean readFromFile(Context context) {
		return readFromFile(context, FILENAME);
	}
	
	/**
	 * Read save data from file
	 * @param filename
	 * @return
	 */
	public boolean readFromFile(Context context, String filename) {
		try{
			lock();
			StorageDelegator newDelegator = savingHelper.load(context, filename, true); 

			if (newDelegator != null) {
				Log.d("StorageDelegator", "read saved state");
				currentStorage = newDelegator.currentStorage;
				if (currentStorage.getBoundingBoxes() == null) { // can happen if data was added before load
					try {
						currentStorage.setBoundingBox(currentStorage.calcBoundingBoxFromData());
					} catch (OsmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				apiStorage = newDelegator.apiStorage;
				undo = newDelegator.undo;
				clipboard = newDelegator.clipboard;
				factory = newDelegator.factory;
				dirty = false; // data was just read, i.e. memory and file are in sync
				return true;
			} else {
				Log.d("StorageDelegator", "saved state null");
				return false;
			}
		} finally {
			unlock();
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
		
		for (Node node : new ArrayList<Node>(apiStorage.getNodes())) {
			retval.add(node.getStateDescription(aResources));
		}
		
		for (Way way : new ArrayList<Way>(apiStorage.getWays())) {
			retval.add(way.getStateDescription(aResources));
		}
		
		for (Relation relation : new ArrayList<Relation>(apiStorage.getRelations())) {
			retval.add(relation.getStateDescription(aResources));
		}
		return retval;
	}

	/**
	 * 
	 * @param server Server to upload changes to.
	 * @param comment Changeset comment.
	 * @param source 
	 * @param closeChangeset
	 * @throws MalformedURLException
	 * @throws ProtocolException
	 * @throws OsmServerException
	 * @throws IOException
	 */
	public synchronized void uploadToServer(final Server server, final String comment, String source, boolean closeChangeset) throws MalformedURLException, ProtocolException,
			OsmServerException, IOException {
			
		dirty = true; // storages will get modified as data is uploaded, these changes need to be saved to file
		// upload methods set dirty flag too, in case the file is saved during an upload
		server.openChangeset(comment, source, Util.listToOsmList(imagery));

		server.diffUpload(this);
		
		if (closeChangeset) {
			server.closeChangeset();
		}
		// yes, again, just to be sure
		dirty = true;
		
		// reset imagery recording for next upload
		imagery = new ArrayList<String>();
		setImageryRecorded(false);
		
		// sanity check
		if (!apiStorage.isEmpty()) {
			Log.d(DEBUG_TAG, "apiStorage not empty");
		}
	}
	
	/**
	 * Exports changes as a OsmChange file. 
	 */
	@Override
	public void export(OutputStream outputStream) throws Exception {
		writeOsmChange(outputStream, null);
	}
	
	@Override
	public String exportExtension() {
		return "osc";
	}
		
	/**
	 * Writes created/changed/deleted data to outputStream in OsmChange format
	 * http://wiki.openstreetmap.org/wiki/OsmChange
	 * @param outputStream
	 * @param changeSetId
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @throws IllegalArgumentException 
	 * @throws XmlPullParserException 
	 * @throws Exception
	 */
	public void writeOsmChange(OutputStream outputStream, Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException, XmlPullParserException  {
		Log.d(DEBUG_TAG, "writing osm change with changesetid " + changeSetId);
		XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
		serializer.setOutput(outputStream, "UTF-8");
		serializer.startDocument("UTF-8", null);
		serializer.startTag(null, "osmChange");
		serializer.attribute(null, "generator", App.userAgent);
		serializer.attribute(null, "version", "0.6");
		
		ArrayList<OsmElement> createdNodes = new ArrayList<OsmElement>();
		ArrayList<OsmElement> modifiedNodes = new ArrayList<OsmElement>();
		ArrayList<OsmElement> deletedNodes = new ArrayList<OsmElement>();
		ArrayList<OsmElement> createdWays = new ArrayList<OsmElement>();
		ArrayList<OsmElement> modifiedWays = new ArrayList<OsmElement>();
		ArrayList<OsmElement> deletedWays = new ArrayList<OsmElement>();
		ArrayList<Relation> createdRelations = new ArrayList<Relation>();
		ArrayList<Relation> modifiedRelations = new ArrayList<Relation>();
		ArrayList<Relation> deletedRelations = new ArrayList<Relation>();
		
		for (OsmElement elem : apiStorage.getNodes()) {
			Log.d("StorageDelegator", "node added to list for upload, id " + elem.osmId);
			switch (elem.state) {
			case OsmElement.STATE_CREATED:   createdNodes.add(elem);   break;
			case OsmElement.STATE_MODIFIED:  modifiedNodes.add(elem);  break;
			case OsmElement.STATE_DELETED:   deletedNodes.add(elem);   break;
			}
		}
		for (OsmElement elem : apiStorage.getWays()) {
			Log.d("StorageDelegator", "way added to list for upload, id " + elem.osmId);
			switch (elem.state) {
			case OsmElement.STATE_CREATED:   createdWays.add(elem);   break;
			case OsmElement.STATE_MODIFIED:  modifiedWays.add(elem);  break;
			case OsmElement.STATE_DELETED:   deletedWays.add(elem);   break;
			}
		}
		for (OsmElement elem : apiStorage.getRelations()) {
			Log.d("StorageDelegator", "relation added to list for upload, id " + elem.osmId);
			switch (elem.state) {
			case OsmElement.STATE_CREATED:   createdRelations.add((Relation) elem);   break;
			case OsmElement.STATE_MODIFIED:  modifiedRelations.add((Relation) elem);  break;
			case OsmElement.STATE_DELETED:   deletedRelations.add((Relation) elem);   break;
			}
		}
		Comparator<Relation> relationOrder = new Comparator<Relation>() {
			@Override
			public int compare(Relation r1, Relation r2) {
				if (r1.hasParentRelation(r2)) {
					return -1;
				} 
				if (r2.hasParentRelation(r1)) {
					return 1;
				} 
				return 0;
			}};
		if (!createdRelations.isEmpty()) {
			// sort the relations so that childs come first, will not handle loops and similar brokenness
			Collections.sort(createdRelations, relationOrder);
		}
		if (!modifiedRelations.isEmpty()) {
			// sort the relations so that childs come first, will not handle loops and similar brokenness
			Collections.sort(modifiedRelations, relationOrder);
		}
		if (!deletedRelations.isEmpty()) {
			// sort the relations so that parents come first, will not handle loops and similar brokenness
			Collections.sort(deletedRelations, new Comparator<Relation>() {
				@Override
				public int compare(Relation r1, Relation r2) {
					if (r1.hasParentRelation(r2)) {
						return 1;
					} 
					if (r2.hasParentRelation(r1)) {
						return -1;
					} 
					return 0;
				}});
		}

		if (!createdNodes.isEmpty() || !createdWays.isEmpty() || !createdRelations.isEmpty()) {
			serializer.startTag(null, "create");
			for (OsmElement elem : createdNodes) elem.toXml(serializer, changeSetId);
			for (OsmElement elem : createdWays) elem.toXml(serializer, changeSetId);
			for (OsmElement elem : createdRelations) elem.toXml(serializer, changeSetId);
			serializer.endTag(null, "create");
		}

		if (!modifiedNodes.isEmpty() || !modifiedWays.isEmpty() || !modifiedRelations.isEmpty()) {
			serializer.startTag(null, "modify");
			for (OsmElement elem : modifiedNodes) elem.toXml(serializer, changeSetId);
			for (OsmElement elem : modifiedWays) elem.toXml(serializer, changeSetId);
			for (OsmElement elem : modifiedRelations) elem.toXml(serializer, changeSetId);
			serializer.endTag(null, "modify");
		}
				
		// delete in opposite order
		if (!deletedNodes.isEmpty() || !deletedWays.isEmpty() || !deletedRelations.isEmpty()) {
			serializer.startTag(null, "delete");
			for (OsmElement elem : deletedRelations) elem.toXml(serializer, changeSetId);
			for (OsmElement elem : deletedWays) elem.toXml(serializer, changeSetId);
			for (OsmElement elem : deletedNodes) elem.toXml(serializer, changeSetId);
			serializer.endTag(null, "delete");
		}
		
		serializer.endTag(null, "osmChange");
		serializer.endDocument();
	}
	
	/**
	 * saves currentStorage + deleted objects to a file. 
	 * @throws XmlPullParserException 
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @throws IllegalArgumentException 
	 */
	public void save(OutputStream outputStream) throws XmlPullParserException, IllegalArgumentException, IllegalStateException, IOException  {
		XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
		serializer.setOutput(outputStream, "UTF-8");
		serializer.startDocument("UTF-8", null);
		serializer.startTag(null, "osm");
		serializer.attribute(null, "generator", App.userAgent);
		serializer.attribute(null, "version", "0.6");
		serializer.attribute(null, "upload", "true");
		
		ArrayList<Node> saveNodes = new ArrayList<Node>(currentStorage.getNodes());
		ArrayList<Way> saveWays = new ArrayList<Way>(currentStorage.getWays());
		ArrayList<Relation> saveRelations = new ArrayList<Relation>(currentStorage.getRelations());
		
		for (Node elem : apiStorage.getNodes()) {
			if (elem.state == OsmElement.STATE_DELETED) {
				Log.d("StorageDelegator", "deleted node added to list for save, id " + elem.osmId);
				saveNodes.add(elem);
			}
		}
		for (Way elem : apiStorage.getWays()) {
			if (elem.state == OsmElement.STATE_DELETED) {
				Log.d("StorageDelegator", "deleted way added to list for save, id " + elem.osmId);
				saveWays.add(elem);
			}
		}
		for (Way elem : apiStorage.getWays()) {
			if (elem.state == OsmElement.STATE_DELETED) {
				Log.d("StorageDelegator", "deleted way added to list for save, id " + elem.osmId);
				saveWays.add(elem);
			}
		}
		
		// 
		for (BoundingBox b:currentStorage.getBoundingBoxes()) {
			b.toJosmXml(serializer);
		}
		
		//TODO sort arrays here
		
		if (!saveNodes.isEmpty()) {
			for (OsmElement elem : saveNodes) elem.toJosmXml(serializer);
		}
		if (!saveWays.isEmpty()) {
			for (OsmElement elem : saveWays) elem.toJosmXml(serializer);
		}
		if (!saveRelations.isEmpty()) {
			for (OsmElement elem : saveRelations) elem.toJosmXml(serializer);
		}
		
		
		serializer.endTag(null, "osm");
		serializer.endDocument();
	}
	
	/**
	 * Merge additional data with existing, copy to a new storage because this may fail
	 * @param storage
	 */
	synchronized public boolean mergeData(Storage storage, PostMergeHandler postMerge) {
		Log.d("StorageDelegator","mergeData called");
		// make temp copy of current storage (we may have to abort
		Storage temp = new Storage(currentStorage);

		// retrieve the maps
		LongOsmElementMap<Node> nodeIndex = temp.getNodeIndex();
		LongOsmElementMap<Way> wayIndex = temp.getWayIndex();
		LongOsmElementMap<Relation> relationIndex = temp.getRelationIndex();
		
		Log.d("StorageDelegator","mergeData finished init");
		
		try {
			// add nodes
			for (Node n:storage.getNodes()) {
				Node apiNode = apiStorage.getNode(n.getOsmId()); // can contain deleted elements
				if (!nodeIndex.containsKey(n.getOsmId()) &&  apiNode == null) { // new node no problem
					temp.insertNodeUnsafe(n);
					if (postMerge != null) {
						postMerge.handler(n);
					}
				} else {
					if (apiNode != null && apiNode.getState() == OsmElement.STATE_DELETED) {
						if (apiNode.getOsmVersion() >= n.getOsmVersion()) {
							continue; // can use node we already have
						} else {
							return false; // can't resolve conflicts, upload first
						}
					}
					Node existingNode = nodeIndex.get(n.getOsmId());
					if (existingNode.getOsmVersion() >= n.getOsmVersion()) { // larger just to be on the safe side
						continue; // can use node we already have
					} else {
						if (existingNode.isUnchanged()) {
							temp.insertNodeUnsafe(n);
							if (postMerge != null) {
								postMerge.handler(n);
							}
						} else {
							return false; // can't resolve conflicts, upload first
						}
					}
				}
			}
			
			Log.d("StorageDelegator","mergeData added nodes");

			// add ways
			for (Way w:storage.getWays()) {
				Way apiWay = apiStorage.getWay(w.getOsmId()); // can contain deleted elements
				if (!wayIndex.containsKey(w.getOsmId()) && apiWay == null) { // new way no problem
					temp.insertWayUnsafe(w);
					if (postMerge != null) {
						postMerge.handler(w);
					}
				} else {
					if (apiWay != null && apiWay.getState() == OsmElement.STATE_DELETED) {
						if (apiWay.getOsmVersion() >= w.getOsmVersion()) {
							continue; // can use way we already have
						} else {
							return false; // can't resolve conflicts, upload first
						}
					}
					Way existingWay = wayIndex.get(w.getOsmId());
					if (existingWay != null) {
						if (existingWay.getOsmVersion() >= w.getOsmVersion()) {// larger just to be on the safe side  
							continue; // can use way we already have
						} else {
							if (existingWay.isUnchanged()) {
								temp.insertWayUnsafe(w);
								if (postMerge != null) {
									postMerge.handler(w);
								}
							} else {
								return false; // can't resolve conflicts, upload first
							}
						}
					} else {
						// this shouldn't be able to happen
						Log.e("StorageDelegator","mergeData null existing way " + w.getOsmId());
						ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
						ACRA.getErrorReporter().handleException(null);
						return false;
					}
				}
			}
			
			Log.d("StorageDelegator","mergeData added ways");

			// fix up way nodes
			// all nodes should be in storage now, however new ways will have references to copies not in storage
			for (Way w:wayIndex) {
				List<Node> nodes = w.getNodes();
				for (int i=0;i<nodes.size();i++) {
					Node wayNode = nodes.get(i);
					long wayNodeId = wayNode.getOsmId();
					Node n = nodeIndex.get(wayNodeId);
					if (n != null) {
						nodes.set(i,n);
					} else {
						// node might have been deleted, aka somebody deleted nodes outside of the down loaded data bounding box
						// that belonged to a not downloaded way
						Node apiNode = apiStorage.getNode(wayNodeId);
						if (apiNode != null && apiNode.getState() == OsmElement.STATE_DELETED) {
							// attempt to fix this up, reinstate the original node so that any existing references remain
							// FIXME undoing the original delete will likely cause havoc
							Log.e("StorageDelegator","mergeData null undeleting node " + wayNodeId);
							if (apiNode.getOsmVersion() == wayNode.getOsmVersion() 
									&& (apiNode.isTagged() && apiNode.getTags().equals(wayNode.getTags()))
									&& apiNode.getLat() == wayNode.getLat()
									&& wayNode.getLon() == wayNode.getLon()) {
								apiNode.setState(OsmElement.STATE_UNCHANGED);
								apiStorage.removeNode(apiNode);
							} else {
								apiNode.setState(OsmElement.STATE_MODIFIED);
							}
							temp.insertNodeUnsafe(apiNode);
							nodes.set(i,apiNode);
						} else {
							Log.e("StorageDelegator","mergeData null way node for way " + w.getOsmId() + " v" + w.getOsmVersion() + " node " + wayNodeId + " v" + wayNode.getOsmVersion());
							ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
							ACRA.getErrorReporter().handleException(null);
							return false;
						}
					}
				}
			}
			
			Log.d("StorageDelegator","mergeData fixup way nodes nodes");

			// add relations
			for (Relation r:storage.getRelations()) {
				Relation apiRelation = apiStorage.getRelation(r.getOsmId()); // can contain deleted elements
				if (!relationIndex.containsKey(r.getOsmId()) && apiRelation == null) { // new relation no problem
					temp.insertRelationUnsafe(r);
					if (postMerge != null) {
						postMerge.handler(r);
					}
				} else {
					if (apiRelation != null && apiRelation.getState() == OsmElement.STATE_DELETED) {
						if (apiRelation.getOsmVersion() >= r.getOsmVersion())
							continue; // can use relation we already have
						else
							return false; // can't resolve conflicts, upload first
					}
					Relation existingRelation = relationIndex.get(r.getOsmId());
					if (existingRelation.getOsmVersion() >= r.getOsmVersion()) { // larger just to be on the safe side
						continue; // can use relation we already have
					}
					else {
						if (existingRelation.isUnchanged()) {
							temp.insertRelationUnsafe(r);
							if (postMerge != null) {
								postMerge.handler(r);
							}
						} else
							return false; // can't resolve conflicts, upload first
					}
				}
			}

			Log.d("StorageDelegator","mergeData added relations");
			
			// fixup relation back links and memberships 
			for (Relation r:temp.getRelations()) {
				for (RelationMember rm:r.getMembers()) {
					if (rm.getType().equals(Node.NAME)) {
						if (nodeIndex.containsKey(rm.getRef())) { // if node is downloaded always re-set it
							Node n = nodeIndex.get(rm.getRef());
							rm.setElement(n);
							if (n.hasParentRelation(r.getOsmId())) {
								n.removeParentRelation(r.getOsmId()); // this removes based on id
							}							   			  // net effect is to remove the old rel
							n.addParentRelation(r);		   			  // and add the updated one
						} else { // check if deleted
							Node apiNode = apiStorage.getNode(rm.getRef());
							if (apiNode != null && apiNode.getState() == OsmElement.STATE_DELETED) {
								Log.e("StorageDelegator","mergeData deleted node in downloaded relation " + r.getOsmId());
								ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
								ACRA.getErrorReporter().handleException(null);
								return false; // can't resolve conflicts, upload first
							}
						}
					} else if (rm.getType().equals(Way.NAME)) { // same logic as for nodes
						if (wayIndex.containsKey(rm.getRef())) {
							Way w = wayIndex.get(rm.getRef());
							rm.setElement(w);
							if (w.hasParentRelation(r.getOsmId())) {
								w.removeParentRelation(r.getOsmId());
							}
							w.addParentRelation(r);
						} else { // check if deleted
							Way apiWay = apiStorage.getWay(rm.getRef());
							if (apiWay != null && apiWay.getState() == OsmElement.STATE_DELETED) {
								Log.e("StorageDelegator","mergeData deleted way in downloaded relation");
								ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
								ACRA.getErrorReporter().handleException(null);
								return false; // can't resolve conflicts, upload first
							}
						}
					} else if (rm.getType().equals(Relation.NAME)) { // same logic as for nodes
						if (relationIndex.containsKey(rm.getRef())) {
							Relation r2 = relationIndex.get(rm.getRef());
							rm.setElement(r2);
							if (r2.hasParentRelation(r.getOsmId())) {
								r2.removeParentRelation(r.getOsmId());
							}
							r2.addParentRelation(r);
						} else { // check if deleted
							Relation apiRel = apiStorage.getRelation(rm.getRef());
							if (apiRel != null && apiRel.getState() == OsmElement.STATE_DELETED) {
								Log.e("StorageDelegator","mergeData deleted relation in downloaded relation");
								ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
								ACRA.getErrorReporter().handleException(null);
								return false; // can't resolve conflicts, upload first
							}
						}
					}
				}
			}
			
			Log.d("StorageDelegator","mergeData fixuped relations");
			
		} catch (StorageException sex) {
			// ran of memory
			return false;
		}

		currentStorage = temp;
		undo.setCurrentStorage(temp);
		return true; // Success
	}

	/**
	 * This is only used when trying to fix conflicts
	 * @param element
	 */
	public void removeFromUpload(OsmElement element) {
		apiStorage.removeElement(element);
		element.setState(OsmElement.STATE_UNCHANGED);
	}
	
	/**
	 * This is only used when trying to fix conflicts
	 * @param element
	 * @param version
	 */
	public void setOsmVersion(OsmElement element, long version) {
		element.setOsmVersion(version);
		element.setState(OsmElement.STATE_MODIFIED);
		insertElementSafe(element);
	}
	
	/**
	 * Return true if coordinates were in the original bboxes from downloads, needs a more efficient implementation
	 * @param lat
	 * @param lon
	 * @return
	 */
	public boolean isInDownload(int lat, int lon) {
		for (BoundingBox bb:new ArrayList<BoundingBox>(currentStorage.getBoundingBoxes())) { // make shallow copy
			if (bb.isIn(lat, lon))
				return true;
		}
		return false;
	}

	public BoundingBox getLastBox() {
		int s = getBoundingBoxes().size();
		if (s > 0) {
			return currentStorage.getBoundingBoxes().get(getBoundingBoxes().size()-1);
		} 
		Log.e(DEBUG_TAG,"Bounding box list empty");
		return new BoundingBox(); // empty box
	}

	/**
	 * for debugging only
	 */
	public void logStorage() {
		Log.d("StorageDelegator","storage dirty? " + isDirty());
		Log.d("StorageDelegator","currentStorage");
		currentStorage.logStorage();
		Log.d("StorageDelegator","apiStorage");
		apiStorage.logStorage();
	}
	
	public void lock() {
		readingLock.lock();
	}
	
	public void unlock() {
		readingLock.unlock();
	}
}
