package de.blau.android.tasks;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import de.blau.android.util.rtree.BoundedObject;
import de.blau.android.util.rtree.RTree;

/**
 * Storage for tasks and the corresponding coverage bounding boxes
 * 
 * @author simon
 *
 */
public class TaskStorage implements Serializable {
    private static final long               serialVersionUID = 6L;
    private static final String             DEBUG_TAG        = TaskStorage.class.getSimpleName();
    private int                             newId            = -1;
    private RTree                           tasks;
    private RTree                           boxes;
    private Map<Long, MapRouletteChallenge> challenges;
    private transient boolean               dirty            = true;

    /**
     * when reading state lockout writing/reading
     */
    private transient ReentrantLock readingLock = new ReentrantLock();

    public static final String FILENAME = "tasks.res";

    private transient SavingHelper<TaskStorage> savingHelper = new SavingHelper<>();

    /**
     * Default constructor
     */
    public TaskStorage() {
        reset();
        challenges = new HashMap<>();
        dirty = false;
    }

    /**
     * Reset storage to initial values
     */
    public synchronized void reset() {
        tasks = new RTree(30, 100);
        boxes = new RTree(2, 20);
        dirty = true;
    }

    /**
     * Add task to storage
     * 
     * Set storage state to dirty as a side effect
     * 
     * @param t Task to add
     */
    public synchronized void add(@NonNull Task t) {
        tasks.insert(t);
        dirty = true;
    }

    /**
     * Add bounding box to storage
     * 
     * Set storage state to dirty as a side effect
     * 
     * @param b Boundingbox to add
     */
    public synchronized void add(@NonNull BoundingBox b) {
        boxes.insert(b);
        dirty = true;
    }

    /**
     * Remove task from storage
     * 
     * Set storage state to dirty as a side effect
     * 
     * @param t Task to remove
     */
    public synchronized void delete(@NonNull Task t) {
        tasks.remove(t);
        dirty = true;
    }

    /**
     * Remove bounding box from storage
     * 
     * Set storage state to dirty as a side effect
     * 
     * @param b BoundingBox to remove
     */
    public synchronized void delete(@NonNull BoundingBox b) {
        boxes.remove(b);
        dirty = true;
    }

    /**
     * Returns true if there is a task with the same id in the same location (task location is immutable so shoudn't be
     * a problem)
     * 
     * @param t Task to check for
     * @return true if t was found
     */
    public boolean contains(@NonNull Task t) {
        Collection<BoundedObject> queryResult = new ArrayList<>();
        tasks.query(queryResult, t.getLon(), t.getLat());
        Log.d(DEBUG_TAG, "candidates for contain " + queryResult.size());
        for (BoundedObject bo : queryResult) {
            if (t instanceof Note && bo instanceof Note && t.getId() == ((Task) bo).getId()) {
                return true;
            } else if (t instanceof OsmoseBug && bo instanceof OsmoseBug && t.getId() == ((Task) bo).getId()) {
                return true;
            } else if (t instanceof CustomBug && bo instanceof CustomBug && t.getId() == ((Task) bo).getId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get any Task in storage that is of the same type and has the same id and is at the same position
     * 
     * @param t the Task we are looking for
     * @return the in storage instance or null if not found
     */
    @Nullable
    public Task get(@NonNull Task t) {
        Collection<BoundedObject> queryResult = new ArrayList<>();
        tasks.query(queryResult, t.getLon(), t.getLat());
        Log.d(DEBUG_TAG, "candidates for get " + queryResult.size());
        for (BoundedObject bo : queryResult) {
            if (t.getId() == ((Task) bo).getId() && t.getClass().equals(bo.getClass())) {
                return (Task) bo;
            }
        }
        return null;
    }

    /**
     * Return all tasks in storage
     * 
     * @return a List of Tasks
     */
    @NonNull
    public List<Task> getTasks() {
        Collection<BoundedObject> queryResult = new ArrayList<>();
        tasks.query(queryResult);
        Log.d(DEBUG_TAG, "getTasks result count (no BB) " + queryResult.size());
        List<Task> result = new ArrayList<>();
        for (BoundedObject bo : queryResult) {
            result.add((Task) bo);
        }
        return result;
    }

    /**
     * Return all tasks in a bounding box
     * 
     * @param box BoundingBox that should be searched
     * @return a List of Tasks
     */
    @NonNull
    public List<Task> getTasks(@NonNull BoundingBox box) {
        Collection<BoundedObject> queryResult = new ArrayList<>();
        tasks.query(queryResult, box.getBounds());
        Log.d(DEBUG_TAG, "getTasks result count " + queryResult.size());
        List<Task> result = new ArrayList<>();
        for (BoundedObject bo : queryResult) {
            result.add((Task) bo);
        }
        return result;
    }

    /**
     * 
     * @return true if storage is empty
     */
    public boolean isEmpty() {
        return tasks.count() == 0;
    }

    /**
     * Stores the current storage data to the default storage file
     * 
     * @param ctx Android Context
     * @throws IOException on errors writing the file
     */
    public synchronized void writeToFile(@NonNull Context ctx) throws IOException {
        if (!dirty) {
            Log.i(DEBUG_TAG, "storage not dirty, skipping save");
            return;
        }
        if (readingLock.tryLock()) {
            try {
                // TODO this doesn't really help with error conditions need to throw exception
                if (savingHelper.save(ctx, FILENAME, this, true)) {
                    dirty = false;
                } else {
                    // this is essentially catastrophic and can only happen if something went really wrong
                    // running out of memory or disk, or HW failure
                    if (ctx instanceof Activity) {
                        Snack.barError((Activity) ctx, R.string.toast_statesave_failed);
                    }
                }
            } finally {
                readingLock.unlock();
            }
        } else {
            Log.i(DEBUG_TAG, "bug state being read, skipping save");
        }
    }

    /**
     * Loads the storage data from the default storage file
     * 
     * NOTE: lock is acquired in logic before this is called
     * 
     * @param context Android context
     * @return true if the saved state was successfully read
     */
    public synchronized boolean readFromFile(@NonNull Context context) {
        try {
            readingLock.lock();
            TaskStorage newStorage = savingHelper.load(context, FILENAME, true);

            if (newStorage != null) {
                Log.d(DEBUG_TAG, "read saved state");
                tasks = newStorage.tasks;
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

    /**
     * Set the state of the storage to dirty (needs to be saved)
     */
    public void setDirty() {
        dirty = true;
    }

    /**
     * Return a new temporary id for a task
     * 
     * @return a new temporary (negative) id
     */
    public long getNextId() {
        return newId--;
    }

    /**
     * Retrieve all bounding boxes from storage
     * 
     * @return list of BoundingBoxes
     */
    @NonNull
    public ArrayList<BoundingBox> getBoundingBoxes() {
        Collection<BoundedObject> queryResult = new ArrayList<>();
        boxes.query(queryResult);
        ArrayList<BoundingBox> result = new ArrayList<>();
        for (BoundedObject bo : queryResult) {
            result.add((BoundingBox) bo);
        }
        return result;
    }

    /**
     * Retrieve bounding boxes that are "in" a bounding box
     * 
     * @param box BoundingBox that needs to be searched
     * @return list of BoundingBox intersecting box
     */
    @NonNull
    public ArrayList<BoundingBox> getBoundingBoxes(@NonNull BoundingBox box) {
        Collection<BoundedObject> queryResult = new ArrayList<>();
        boxes.query(queryResult, box.getBounds());
        Log.d(DEBUG_TAG, "getBoundingBoxes result count " + queryResult.size());
        ArrayList<BoundingBox> result = new ArrayList<>();
        for (BoundedObject bo : queryResult) {
            result.add((BoundingBox) bo);
        }
        return result;
    }

    /**
     * Check if any of the tasks has been changed
     * 
     * @return true if a changed task is found
     */
    public boolean hasChanges() {
        for (BoundedObject b : getTasks()) {
            if (((Task) b).hasBeenChanged()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the challenges
     */
    public Map<Long, MapRouletteChallenge> getChallenges() {
        return challenges;
    }

    @Override
    public String toString() {
        return "task r-tree: " + tasks.count() + " boxes r-tree " + boxes.count();
    }
}
