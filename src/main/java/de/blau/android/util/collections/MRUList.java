package de.blau.android.util.collections;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list that can be used in a stack like fashion for implementing MRU (Most Recently Used) semantics
 * 
 * @author simon
 *
 * @param <T>
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
    public MRUList(Collection<T> collection) {
        super(collection);
        this.capacity = collection.size();
    }

    /**
     * Add o to top of stack, if o is already present move it there
     * 
     * @param o Object to push
     */
    public void push(T o) {
        int index = indexOf(o);
        if (index >= 0) {
            remove(index);
        }
        if (size() >= capacity) {
            remove(size() - 1);
        }
        add(0, o);
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
        final int prime = 37;
        int result = super.hashCode();
        result = prime * result + capacity;
        return result;
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
