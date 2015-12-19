package de.blau.android.osb;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.rtree.BoundedObject;
import de.blau.android.util.rtree.RTree;

/**
 * Storage for bugs and the corresponding coverage bounding boxes
 * @author simon
 *
 */
public class BugStorage implements Serializable {
	private static final long serialVersionUID = 3L;
	private final static String DEBUG_TAG = BugStorage.class.getSimpleName();
	private int newId=0;
	private RTree bugs;
	private RTree boxes;
	private transient boolean dirty = true;
	
	/**
	 * when reading state lockout writing/reading 
	 */
	private transient ReentrantLock readingLock = new ReentrantLock();
	
	public final static String FILENAME = "bugs.res";

	private transient SavingHelper<BugStorage> savingHelper = new SavingHelper<BugStorage>();
	
	public BugStorage() {
		reset();
		dirty = false;
	}
	
	public synchronized void reset() {
		bugs = new RTree(2,100);
		boxes = new RTree(2,20);
		dirty = true;
	}

	public synchronized void add(Bug b) {
		bugs.insert(b);
		dirty = true;
	}
	
	public synchronized void add(BoundingBox b) {
		boxes.insert(b);
		dirty = true;
	}
	
	public synchronized void delete(Bug b) {
		bugs.remove(b);
		dirty = true;
	}
	
	public synchronized void delete(BoundingBox b) {
		boxes.remove(b);
		dirty = true;
	}
	
	/**
	 * Returns true if there is an bug with the same id in the same location 
	 * (bug location is immutable so shoudn't be a problem)
	 * @param b
	 * @return
	 */
	public boolean contains(Bug b) {
		Collection<BoundedObject> queryResult = new ArrayList<BoundedObject>();
		bugs.query(queryResult, b.getLon(), b.getLat());
		Log.d(DEBUG_TAG,"candidates for contain " + queryResult.size());
		for (BoundedObject bo:queryResult) {
			if (b instanceof Note && bo instanceof Note && b.getId() == ((Bug)bo).getId()) {
				return true;
			} else if (b instanceof OsmoseBug && bo instanceof OsmoseBug && b.getId() == ((Bug)bo).getId()) {
				return true;
			} 
		}
		return false;
	}
	
	/**
	 * Return all bugs
	 * @return
	 */
	public ArrayList<Bug>getBugs() {
		Collection<BoundedObject> queryResult = new ArrayList<BoundedObject>();
		bugs.query(queryResult);
		ArrayList<Bug>result = new ArrayList<Bug>();
		for (BoundedObject bo:queryResult) {
			result.add((Bug)bo);
		}
		return result;
	}
	
	public ArrayList<Bug>getBugs(BoundingBox box) {
		Collection<BoundedObject> queryResult = new ArrayList<BoundedObject>();
		bugs.query(queryResult,box.getBounds());
		Log.d(DEBUG_TAG,"result count " + queryResult.size());
		ArrayList<Bug>result = new ArrayList<Bug>();
		for (BoundedObject bo:queryResult) {
			result.add((Bug)bo);
		}
		return result;
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return bugs.count() == 0;
	}
	
	
	/**
	 * Stores the current storage data to the default storage file
	 * @param ctx TODO
	 * @throws IOException
	 */
	public synchronized void writeToFile(Context ctx) throws IOException { 
		if (isEmpty()) {
			// don't write empty state files FIXME if the state file is empty on purpose we -should- write it
			Log.i(DEBUG_TAG, "storage empty, skipping save");
			return;
		}
		if (!dirty) {
			Log.i(DEBUG_TAG, "storage not dirty, skipping save");
			return;
		}

		if (readingLock.tryLock()) {
			// TODO this doesn't really help with error conditions need to throw exception
			if (savingHelper.save(FILENAME, this, true)) { 
				dirty = false;
			} else {
				// this is essentially catastrophic and can only happen if something went really wrong
				// running out of memory or disk, or HW failure
				if (ctx != null) {
					Toast.makeText(ctx, R.string.toast_statesave_failed, Toast.LENGTH_LONG).show();
				}
			}
			readingLock.unlock();
		} else {
			Log.i(DEBUG_TAG, "bug state being read, skipping save");
		}
	}
	
	/**
	 * Loads the storage data from the default storage file
	 * NOTE: lock is acquired in logic before this is called
	 */
	public synchronized boolean readFromFile() {
		try{
			readingLock.lock();
			BugStorage newStorage = savingHelper.load(FILENAME, true); 

			if (newStorage != null) {
				Log.d(DEBUG_TAG, "read saved state");
				bugs = newStorage.bugs;
				boxes = newStorage.boxes;
				dirty = false; // data was just read, i.e. memory and file are in sync
				return true;
			} else {
				Log.d(DEBUG_TAG, "saved state null");
				return false;
			}
		} finally {
			readingLock.unlock();
		}
	}
	
	public void setDirty() {
		dirty = true;
	}
	
	public String toString() {
		return "bug r-tree: " + bugs.count() + " boxes r-tree " + boxes.count();
	}

	public long getNextId() {
		return newId--;
	}

	public ArrayList<BoundingBox> getBoundingBoxes() {
		Collection<BoundedObject> queryResult = new ArrayList<BoundedObject>();
		boxes.query(queryResult);
		ArrayList<BoundingBox>result = new ArrayList<BoundingBox>();
		for (BoundedObject bo:queryResult) {
			result.add((BoundingBox)bo);
		}
		return result;
	}
	
	public ArrayList<BoundingBox> getBoundingBoxes(BoundingBox box) {
		Collection<BoundedObject> queryResult = new ArrayList<BoundedObject>();
		boxes.query(queryResult,box.getBounds());
		Log.d(DEBUG_TAG,"result count " + queryResult.size());
		ArrayList<BoundingBox>result = new ArrayList<BoundingBox>();
		for (BoundedObject bo:queryResult) {
			result.add((BoundingBox)bo);
		}
		return result;
	}
	
	/**
	 * Check for changes
	 * @return
	 */
	public boolean hasChanges() {
		Collection<BoundedObject> queryResult = new ArrayList<BoundedObject>();
		bugs.query(queryResult);
		for (BoundedObject b:queryResult) {
			if (((Bug)b).hasBeenChanged()) {
				return true;
			}
		}
		return false;
	}
}
