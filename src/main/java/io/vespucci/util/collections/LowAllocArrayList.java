package io.vespucci.util.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import android.util.Log;
import androidx.annotation.NonNull;

/**
 * ArrayList with a far more efficient addAll implementation if the argument is also a LowAllocArrayList
 * 
 * The standard version of addAll allocates an array the size of the arguments list, and copies the contents twice
 * (assuming the target list has enough capacity), this implementation, will do just one array copy in the same
 * situation.
 * 
 * Note that we currently don't replace the two argument addAll variant.
 * 
 * @author simon
 */
public class LowAllocArrayList<V> extends ArrayList<V> {

    private static final String DEBUG_TAG = LowAllocArrayList.class.getSimpleName().substring(0, Math.min(23, LowAllocArrayList.class.getSimpleName().length()));

    private static final long serialVersionUID = 1L;

    private transient Field dataField;
    private transient Field sizeField;

    /**
     * Constructs a new LowAllocArrayList instance with zero initial capacity.
     */
    public LowAllocArrayList() {
        // ensure that we actually have minimum capacity
        // this works around a quirk were ensureCapacity will not work for values <= the default
        // capacity even if the actual capacity is smaller
        super(10);
        reflect();
    }

    /**
     * Constructs a new instance of LowAllocArrayList containing the elements of the specified collection.
     *
     * @param collection the collection of elements to add.
     */
    public LowAllocArrayList(Collection<? extends V> collection) {
        super(collection);
        reflect();
    }

    /**
     * Constructs a new LowAllocArrayList instance with an initial capacity.
     * 
     * @param capacity the initial capacity
     */
    public LowAllocArrayList(int capacity) {
        super(capacity);
        reflect();
    }

    /**
     * As we need to run with different JDK implementations and some fields are private we set up access here
     */
    private void reflect() {
        try {
            sizeField = getField("size");
        } catch (NoSuchFieldException e) {
            Log.e(DEBUG_TAG, "not running on Android or OpenJDK");
        }
        try {
            try { // NOSONAR
                dataField = getField("array");
            } catch (NoSuchFieldException e) {
                Log.w(DEBUG_TAG, "not using 'array'");
                try { // NOSONAR
                    dataField = getField("elementData");
                } catch (NoSuchFieldException e2) {
                    Log.e(DEBUG_TAG, "not using 'elementData'");
                }
            }
        } catch (IllegalArgumentException | SecurityException e) {
            Log.e(DEBUG_TAG, "Reflection error " + e.getMessage());
        }
    }

    /**
     * Retrieve a field from ArrayList and set access
     * 
     * @throws NoSuchFieldException if the field doesn't exist
     */
    @NonNull
    private Field getField(@NonNull String fieldName) throws NoSuchFieldException {
        Field field = ArrayList.class.getDeclaredField(fieldName);
        field.setAccessible(true); // NOSONAR
        return field;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean addAll(Collection<? extends V> c) {
        if (c instanceof LowAllocArrayList && sizeField != null && dataField != null) {
            int newPartSize = c.size();
            if (newPartSize == 0) {
                return false;
            }
            final int newSize = size() + newPartSize;
            ensureCapacity(newSize);
            try {
                System.arraycopy(((LowAllocArrayList<V>) c).dataField.get(c), 0, dataField.get(this), size(), newPartSize);
                sizeField.setInt(this, newSize); // NOSONAR
                modCount++;
                return true;
            } catch (IllegalArgumentException | IllegalAccessException e) {
                Log.e(DEBUG_TAG, "copy failed " + e.getMessage());
            }
        }
        return super.addAll(c);
    }
}
