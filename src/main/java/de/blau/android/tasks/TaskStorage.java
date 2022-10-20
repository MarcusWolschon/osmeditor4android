package de.blau.android.tasks;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.exception.IllegalOperationException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.util.DataStorage;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.rtree.RTree;

/**
 * Storage for tasks and the corresponding coverage bounding boxes
 * 
 * @author simon
 *
 */
public class TaskStorage implements Serializable, DataStorage {
    private static final long               serialVersionUID = 8L;
    private static final String             DEBUG_TAG        = TaskStorage.class.getSimpleName();
    private int                             newId            = -1;
    private RTree<Task>                     tasks;
    private RTree<BoundingBox>              boxes;
    private Map<Long, MapRouletteChallenge> challenges;
    private OsmoseMeta                      osmoseMeta;
    private transient boolean               dirty            = true;

    /**
     * when reading state lockout writing/reading
     */
    private transient ReentrantLock readingLock = new ReentrantLock();

    public static final String FILENAME = "tasks" + "." + FileExtensions.RES;

    private transient SavingHelper<TaskStorage> savingHelper = new SavingHelper<>();

    /**
     * Default constructor
     */
    public TaskStorage() {
        challenges = new HashMap<>();
        reset();
        dirty = false;
    }

    /**
     * Reset storage to initial values
     */
    public synchronized void reset() {
        tasks = new RTree<>(30, 100);
        boxes = new RTree<>(2, 20);
        challenges.clear();
        osmoseMeta = null;
        dirty = true;
    }

    /**
     * Get the number of stored tasks
     * 
     * @return the number of stored tasks
     */
    public int count() {
        return tasks.count();
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

    @Override
    public synchronized void addBoundingBox(@NonNull BoundingBox b) {
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
    @Override
    public synchronized void deleteBoundingBox(@NonNull BoundingBox b) {
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
        Collection<Task> queryResult = new ArrayList<>();
        tasks.query(queryResult, t.getLon(), t.getLat());
        Log.d(DEBUG_TAG, "candidates for contain " + queryResult.size());
        for (Task t2 : queryResult) {
            if (t.equals(t2)) {
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
        Collection<Task> queryResult = new ArrayList<>();
        tasks.query(queryResult, t.getLon(), t.getLat());
        Log.d(DEBUG_TAG, "candidates for get " + queryResult.size());
        for (Task t2 : queryResult) {
            if (t.equals(t2)) {
                return t2;
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
        List<Task> queryResult = new ArrayList<>();
        tasks.query(queryResult);
        Log.d(DEBUG_TAG, "getTasks result count (no BB) " + queryResult.size());
        return queryResult;
    }

    /**
     * Return all tasks in a bounding box
     * 
     * @param box BoundingBox that should be searched
     * @return a List of Tasks
     */
    @NonNull
    public List<Task> getTasks(@NonNull BoundingBox box) {
        List<Task> queryResult = new ArrayList<>();
        tasks.query(queryResult, box.getBounds());
        Log.d(DEBUG_TAG, "getTasks result count " + queryResult.size());
        return queryResult;
    }

    /**
     * Return all tasks in a bounding box
     * 
     * @param box BoundingBox that should be searched
     * @param queryResult a List to use for the result
     * @return a List of Tasks
     */
    @NonNull
    public List<Task> getTasks(@NonNull BoundingBox box, @NonNull List<Task> queryResult) {
        queryResult.clear();
        tasks.query(queryResult, box.getBounds());
        return queryResult;
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
                challenges = newStorage.challenges;
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

    @Override
    @NonNull
    public List<BoundingBox> getBoundingBoxes() {
        return getBoundingBoxes(new ArrayList<>());
    }

    /**
     * Get all BoundingBoxes
     * 
     * @param result a pre-allocated List for the results
     * @return a List of BoundingBox
     */
    @NonNull
    public List<BoundingBox> getBoundingBoxes(@NonNull List<BoundingBox> result) {
        result.clear();
        boxes.query(result);
        return result;
    }

    /**
     * Retrieve bounding boxes that are "in" a bounding box
     * 
     * @param box BoundingBox that needs to be searched
     * @return list of BoundingBox intersecting box
     */
    @NonNull
    public List<BoundingBox> getBoundingBoxes(@NonNull BoundingBox box) {
        List<BoundingBox> queryResult = new ArrayList<>();
        boxes.query(queryResult, box.getBounds());
        Log.d(DEBUG_TAG, "getBoundingBoxes result count " + queryResult.size());
        return queryResult;
    }

    @Override
    public synchronized void prune(@NonNull BoundingBox box) {
        for (Task b : getTasks()) {
            if (!(b instanceof Todo) && !b.hasBeenChanged() && !box.contains(b.getLon(), b.getLat())) {
                tasks.remove(b);
            }
        }
        BoundingBox.prune(this, box);
        dirty = true;
    }

    /**
     * Check if any of the tasks has been changed
     * 
     * @return true if a changed task is found
     */
    public boolean hasChanges() {
        for (Task b : getTasks()) {
            if (b.hasBeenChanged()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the currently referenced MapRoulette Challenges
     * 
     * Note: if the value for a key is null that Challenge needs to be downloaded
     * 
     * @return a Map containing the challenges
     */
    @NonNull
    public Map<Long, MapRouletteChallenge> getChallenges() {
        return challenges;
    }

    /**
     * Move a tasks position
     * 
     * Only newly created Notes can be moved
     * 
     * @param t the Task to be moved
     * @param newLatE7 the new latitude in WGS84*1E7
     * @param newLonE7 the new longitude in WGS84*1E7
     */
    public synchronized void move(@NonNull Task t, int newLatE7, int newLonE7) {
        if (t.isNew()) {
            tasks.remove(t);
            ((Note) t).move(newLatE7, newLonE7);
            tasks.insert(t);
            setDirty();
        } else {
            throw new IllegalOperationException("Can only move new Notes, not " + t.getDescription());
        }
    }

    /**
     * Get Osmose meta information on its issues
     * 
     * @return an OsmoseMeta object
     */
    public synchronized OsmoseMeta getOsmoseMeta() {
        if (osmoseMeta == null) {
            osmoseMeta = new OsmoseMeta();
        }
        return osmoseMeta;
    }

    /**
     * Get Bug Tasks for a specific element
     * 
     * @param element the OsmElement
     * @return a List of Bugs
     */
    @NonNull
    public List<Bug> getTasksForElement(@NonNull OsmElement element) {
        List<Bug> result = new ArrayList<>();
        final long osmId = element.getOsmId();
        final String elementType = element.getName();
        final BoundingBox bounds = element.getBounds();
        if (bounds != null) {
            for (Task t : getTasks(bounds)) {
                if ((t instanceof Bug) && !t.isClosed() && ((Bug) t).hasElement(elementType, osmId)) {
                    result.add((Bug) t);
                }
            }
        } else {
            Log.w(DEBUG_TAG, element.getDescription() + " has null bounding box");
        }
        return result;
    }

    /**
     * Close all Bug tasks that reference the OsmElement
     * 
     * @param element the OsmElement
     */
    public void closeTasksForElement(@NonNull OsmElement element) {
        List<Bug> taskList = getTasksForElement(element);
        if (!taskList.isEmpty()) {
            for (Bug t : taskList) {
                t.close();
                t.setChanged(true);
            }
            setDirty();
        }
    }

    /**
     * Get stored Todos // NOSONAR
     * 
     * @param listName the optional Todo list name // NOSONAR
     * @param all if true return all todos, otherwise just open ones
     * @return a List of Todos // NOSONAR
     */
    @NonNull
    public List<Todo> getTodos(@Nullable String listName, boolean all) {
        List<Todo> todos = new ArrayList<>();
        for (Task t : getTasks()) {
            if (t instanceof Todo && (all || !t.isClosed()) && (listName == null || listName.equals(((Todo) t).getListName()))) {
                todos.add((Todo) t);
            }
        }
        return todos;
    }

    /**
     * Get all current todo lists names // NOSONAR
     * 
     * Note this will always return an entry for the default list
     * 
     * @param context an Android Context
     * @return a List of todo list names // NOSONAR
     */
    @NonNull
    public List<StringWithDescription> getTodoLists(@NonNull Context context) {
        Set<StringWithDescription> todoLists = new LinkedHashSet<>();
        todoLists.add(new StringWithDescription(Todo.DEFAULT_LIST, context.getString(R.string.default_)));
        for (Task t : getTasks()) {
            if (t instanceof Todo && !t.isClosed()) {
                final StringWithDescription listName = ((Todo) t).getListName(context);
                if (!Todo.DEFAULT_LIST.equals(listName.getValue())) {
                    todoLists.add(listName);
                }
            }
        }
        return new ArrayList<>(todoLists);
    }

    /**
     * Get all todos for an OsmElement
     * 
     * @param element the OsmElement
     * @return a List of Todos // NOSONAR
     */
    @NonNull
    public List<Todo> getTodosForElement(@NonNull OsmElement element) {
        List<Todo> result = new ArrayList<>();
        List<Task> taskList = getTasks(element.getBounds());
        if (!taskList.isEmpty()) {
            final long osmId = element.getOsmId();
            final String elementType = element.getName();
            for (Task t : taskList) {
                if (t instanceof Todo && ((Bug) t).hasElement(elementType, osmId)) {
                    result.add((Todo) t);
                }
            }
        }
        return result;
    }

    /**
     * Check if a todo for an OsmElement exists already // NOSONAR
     * 
     * @param e the OsmElement
     * @param listName the name of the todo list // NOSONAR
     * @return true if a todo already exists // NOSONAR
     */
    public boolean contains(@NonNull OsmElement e, @NonNull String listName) {
        List<Todo> existing = getTodosForElement(e);
        for (Todo t : existing) {
            if (listName.equals(t.getListName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "task r-tree: " + tasks.count() + " boxes r-tree " + boxes.count();
    }
}
