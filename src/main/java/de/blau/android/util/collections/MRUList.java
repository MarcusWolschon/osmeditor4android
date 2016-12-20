package de.blau.android.util.collections;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list that can be used in a stack like fashion for implementing MRU (Most Recently Used) semantics
 * @author simon
 *
 * @param <T>
 */
public class MRUList<T> extends ArrayList<T> {
	private static final long serialVersionUID = 1L;
	int capacity;
	
	public MRUList(final int capacity) {
		super(capacity);
		this.capacity = capacity;
	}
	
	public MRUList(Collection<T> collection) {
		super(collection);
		this.capacity = collection.size();
	}

	/**
	 * Add o to top of stack, if o is already present move it there
	 * @param o
	 */
	public void push(T o) {
		int index = indexOf(o);
		if (index >= 0) {
			remove(index);
		}
		if (size()>=capacity){
			remove(size()-1);
		}
		add(0,o);
	}
	
	/**
	 * Get the most recently added object or null if empty
	 * @return
	 */
	public T last() {
		return isEmpty()?null:get(0);
	}
	
	@Override
	public void ensureCapacity(int minimumCapacity) {
		super.ensureCapacity(minimumCapacity);
		this.capacity = minimumCapacity;
	}
}
