package de.blau.android.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A list that can be used in a stack like fashion for implementing MRU (Most Recently Used) semantics
 * 
 * @author Simon Poole
 *
 * @param <T> type stored in the collection
 */
public class MRUList<T> extends ArrayList<T> {

    private static final long serialVersionUID = 1L;
    private int               capacity;

    /**
     * Construct a fixed size stack like list
     * 
     * @param capacity the size of the list
     */
    public MRUList(final int capacity) {
        super(capacity);
        this.capacity = capacity;
    }

    /**
     * Construct a new MRUList from an existing Collection
     * 
     * @param collection the Collection to use for construction
     */
    public MRUList(@NonNull Collection<T> collection) {
        super(collection);
        this.capacity = collection.size();
    }

    /**
     * Add o to top of stack, if o is already present move it there
     * 
     * @param o Object to push
     * @return the Object removed from the list or null if there was none
     */
    @Nullable
    public T push(T o) {
        int index = indexOf(o);
        if (index >= 0) {
            remove(index);
        }
        T removed = null;
        if (size() >= capacity) {
            removed = remove(size() - 1);
        }
        add(0, o);
        return removed;
    }

    /**
     * Add all elements in collection to the stack
     * 
     * @param collection a Collection of T
     */
    public void pushAll(@NonNull Collection<T> collection) {
        for (T o : collection) {
            push(o);
        }
    }

    /**
     * Get the most recently added object or null if empty
     * 
     * @return the last added Object
     */
    public T last() {
        return isEmpty() ? null : get(0);
    }

    @Override
    public void ensureCapacity(int minimumCapacity) {
        super.ensureCapacity(minimumCapacity);
        this.capacity = minimumCapacity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), capacity);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MRUList<?> other = (MRUList<?>) obj;
        return capacity == other.capacity;
    }
}
