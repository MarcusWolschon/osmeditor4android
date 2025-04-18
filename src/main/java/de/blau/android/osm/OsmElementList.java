package de.blau.android.osm;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OsmElementList<T extends OsmElement> {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, OsmElementList.class.getSimpleName().length());
    private static final String DEBUG_TAG = OsmElementList.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Our list of elements
     */
    private List<T> elements;

    /**
     * Set an initial element
     * 
     * @param element the initial element
     */
    public void set(@Nullable final T element) {
        if (element != null) { // always restart
            elements = new LinkedList<>();
            elements.add(element);
        } else {
            elements = null;
        }
    }

    /**
     * Add elements to the internal list
     * 
     * @param element node to add to selection
     */
    public void add(@NonNull final T element) {
        if (elements == null) {
            set(element);
        } else {
            if (!elements.contains(element)) {
                elements.add(element);
            }
        }
    }

    /**
     * Remove an element
     * 
     * @param elemente osmElement to remove
     * @return true if the element was removed
     */
    public boolean remove(@NonNull T elemente) {
        if (elements != null) {
            boolean success = elements.remove(elemente);
            if (elements.isEmpty()) {
                elements = null;
            }
            return success;
        }
        return false;
    }

    /**
     * Get the first Element in the list
     * 
     * @return the first element in the list or null
     */
    @Nullable
    public final T get() {
        if (elements != null && !elements.isEmpty()) {
            return elements.get(0);
        }
        return null;
    }

    /**
     * Get the list elements
     * 
     * @return a List of OsmElements
     */
    @Nullable
    public List<T> getElements() {
        return elements;
    }

    /**
     * Return how many elements are in the list
     * 
     * @return a count of the OsmElements
     */
    public int count() {
        return elements == null ? 0 : elements.size();
    }

    /**
     * Check if the element is contained in this container
     * 
     * @param element the OsmElement
     * @return true if we contain the element
     */
    public boolean contains(@NonNull T element) {
        return elements != null && elements.contains(element);
    }

    /**
     * Get an array of the elements ids
     * 
     * @return an array of long
     */
    @NonNull
    public long[] getIdArray() {
        final int size = elements != null ? elements.size() : 0;
        long[] result = new long[size];
        for (int i = 0; i < size; i++) {
            result[i] = elements.get(i).getOsmId();
        }
        return result;
    }

    /**
     * Get actual OsmElement references from memory
     * 
     * @param delegator the StorageDelegator to use
     * @param type the element type
     * @param ids the ids
     */
    public void fromIds(@NonNull StorageDelegator delegator, @NonNull String type, @Nullable long[] ids) {
        if (ids == null) {
            return;
        }
        Storage apiStorage = delegator.getApiStorage();
        Storage currentStorage = delegator.getCurrentStorage();
        switch (type) {
        case Node.NAME:
            addElements(ids, apiStorage::getNode, currentStorage::getNode);
            return;
        case Way.NAME:
            addElements(ids, apiStorage::getWay, currentStorage::getWay);
            return;
        case Relation.NAME:
            addElements(ids, apiStorage::getRelation, currentStorage::getRelation);
            return;
        default:
            Log.e(DEBUG_TAG, "Unknown element type " + type);
        }
    }

    interface Get {
        OsmElement get(long id);
    }

    /**
     * Add actual OsmElement references from an array of ids
     * 
     * @param ids array of ids
     * @param api function to retrieve an element from API storage
     * @param current function to retrieve an element from current storage
     */
    @SuppressWarnings("unchecked")
    private void addElements(long[] ids, Get api, Get current) {
        LinkedHashSet<T> set = new LinkedHashSet<>(ids.length);
        for (long id : ids) {
            T e = (T) api.get(id);
            if (e == null) {
                e = (T) current.get(id);
            }
            if (e != null) {
                set.add(e);
            }
        }
        elements = new LinkedList<>(set);
    }
}
