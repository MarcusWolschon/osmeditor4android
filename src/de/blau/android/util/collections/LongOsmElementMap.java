package de.blau.android.util.collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import android.annotation.SuppressLint;
import android.os.Build;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;

/**
 * long to OsmElement HashMap
 * 
 * This only implements the, roughly equivalent to Map, interfaces that we current (might) need and is rather Vespucci specific,
 * however could easily be adopted to different implementations of an OSM element as long as you can easily get the element id. As 
 * implemented null is used as a marker for free space and cannot be stored in the map, however that is trivial to change.
 * 
 * This implementation will only require at most 8*(capacity-size) more space than a plain array, and roughly the same as an 
 * ArrayList (depending on how extra capacity is provided).
 * 
 * This is based on public domain code see http://unlicense.org from Mikhail Vorontsov, see https://github.com/mikvor
 * 
 * @version 0.1
 * @author simon
 */
@SuppressLint("UseSparseArrays")
public class LongOsmElementMap<V extends OsmElement> implements Iterable<V>,Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L; // NOTE if you change the hashing algorithm you need to increment this
	
	private static final OsmElement FREE_KEY = null; // NOTE change this if you want to be able to store null 
    private static final OsmElement REMOVED_KEY = OsmElementFactory.createNode(Long.MIN_VALUE, 1, OsmElement.STATE_CREATED, 0, 0);;
    private static final float DEFAULT_FILLFACTOR = 0.75f;
    private static final int DEFAULT_CAPACITY = 16;
    
    /** Keys and values */
    private OsmElement[] m_data;

    /** Fill factor, must be between (0 and 1) */
    private final float m_fillFactor;
    /** We will resize a map once it reaches this size */
    private int m_threshold;
    /** Current map size */
    private int m_size;
    /** Mask to calculate the original position */
    private long m_mask;

    public LongOsmElementMap() {
    	this(DEFAULT_CAPACITY, DEFAULT_FILLFACTOR);
    }
    
    public LongOsmElementMap( final int size) {
    	this(size, DEFAULT_FILLFACTOR);
    }
    
    public LongOsmElementMap( final int size, final float fillFactor )
    {
        if ( fillFactor <= 0 || fillFactor >= 1 ) {
            throw new IllegalArgumentException( "FillFactor must be in (0, 1)" );
        }
        if ( size <= 0 ) {
            throw new IllegalArgumentException( "Size must be positive!" );
        }
        final int capacity = Tools.arraySize(size, fillFactor);
        m_mask = capacity - 1;
        m_fillFactor = fillFactor;

        m_data = new OsmElement[capacity];
        if (FREE_KEY != null){
        	Arrays.fill( m_data, FREE_KEY ); // not necessary if FREE_KEY==null
        }

        m_threshold = (int) (capacity * fillFactor);
    }
    
    @SuppressLint("NewApi")
	public LongOsmElementMap(LongOsmElementMap<? extends V>map) {
    	m_mask = map.m_mask;
    	m_fillFactor = map.m_fillFactor;
    	m_threshold = map.m_threshold;
    	m_size = map.m_size;
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
    		m_data = Arrays.copyOf(map.m_data, map.m_data.length);
    	} else { // sigh
    		m_data = new OsmElement[map.m_data.length];
    		for (int i=0;i < m_data.length;i++) {
    			m_data[i] = map.m_data[i];
    		}
    	}
    }
    

    @SuppressWarnings("unchecked")
	public V get( final long key )
    {
        int ptr = (int) ((Tools.phiMix(key) & m_mask));
        // Log.d("Hash","get key " + Long.toHexString(key) + " ptr " + Integer.toHexString(ptr));
        OsmElement e = m_data[ ptr ];
        if ( e == FREE_KEY ) {
            return null;
        }
        if ( e.getOsmId() == key ) { 
            return (V) e;
        }
        while ( true )
        {
            ptr = (int) ((ptr + 1) & m_mask); //that's next index
            e = m_data[ ptr ];
            if ( e == FREE_KEY ) {
                return null;
            }            
            if ( e.getOsmId() == key ) {
                return (V) e;
            }
        }
    }

    @SuppressWarnings("unchecked")
	public V put( final long key, final V value )
    {
    	int ptr = (int) ((Tools.phiMix(key) & m_mask));
    	// Log.d("Hash","put key " + Long.toHexString(key) + " ptr " + Integer.toHexString(ptr));
        OsmElement e = m_data[ptr];

        if ( e == FREE_KEY ) //end of chain already
        {
            m_data[ ptr ] = (OsmElement) value;
            if ( m_size >= m_threshold ) {
                rehash( m_data.length * 2 ); //size is set inside
            } else {
                ++m_size;
            }
            return null;
        }
        else if ( e.getOsmId() == key ) //we check FREE and REMOVED prior to this call
        {
            m_data[ ptr ] = (OsmElement) value;
            return (V) e;
        }

        // Log.d("Hash","put collision");
        // collisions++;
        int firstRemoved = -1;
        if ( e == REMOVED_KEY ) {
            firstRemoved = ptr; //we may find a key later
        }
  
        while ( true ) {
            ptr = (int) (( ptr + 1 ) & m_mask); //the next index calculation
            e = m_data[ ptr ];
            if ( e == FREE_KEY ) {
                if ( firstRemoved != -1 ) {
                    ptr = firstRemoved;
                }
                m_data[ ptr ] = (OsmElement) value;
                if ( m_size >= m_threshold ) {
                    rehash( m_data.length * 2 ); //size is set inside
                } else {
                    ++m_size;
                }
                return null;
            }
            else if ( e.getOsmId() == key ) {
                m_data[ ptr ] = (OsmElement) value;
                return (V) e ;
            }
            else if ( e == REMOVED_KEY ) {
                if ( firstRemoved == -1 ) {
                    firstRemoved = ptr;
                }
            }
         // Log.d("Hash","put collision");
         //   collisions++;
        }
    }
    
    public void putAll(LongOsmElementMap<V> map) {
    	ensureCapacity(m_data.length + map.size());
    	for (V e:map) { // trivial implementation for now
    		put(e.getOsmId(),e); 
    	}
    }
    
    public void putAll(Collection<V> c) {
    	ensureCapacity(m_data.length + c.size());
    	for (V e:c) { // trivial implementation for now
    		put(e.getOsmId(),e); 
    	}
    }

    public OsmElement remove( final long key )
    {
        int ptr =  (int) (Tools.phiMix(key) & m_mask);
        OsmElement e = m_data[ ptr ];
        if ( e == FREE_KEY ) {
            return null;  //end of chain already
        }
        else if ( e.getOsmId() == key ) //we check FREE and REMOVED prior to this call
        {
            --m_size;
            if ( m_data[ (int) (( ptr + 1 ) & m_mask) ] == FREE_KEY ) { // this shortens the chain
            	m_data[ ptr ] = FREE_KEY;
            } else {
                m_data[ ptr ] = REMOVED_KEY;
            }
            return e;
        }
        while ( true )
        {
            ptr = (int) (( ptr + 1 ) & m_mask); //that's next index calculation
            e = m_data[ ptr ];
            if ( e == FREE_KEY ) {
                return null;
            }
            else if ( e.getOsmId() == key )
            {
                --m_size;
                if ( m_data[ (int) (( ptr + 1 ) & m_mask) ] == FREE_KEY ) { // this shortens the chain
                    m_data[ ptr ] = FREE_KEY;
                } else {
                    m_data[ ptr ] = REMOVED_KEY;
                }
                return e;
            }
        }
    }
    
    public boolean containsKey(long key) {
        int ptr = (int) ((Tools.phiMix(key) & m_mask));
        OsmElement e = m_data[ ptr ];
        if ( e == FREE_KEY) {
            return false;
        }
        if ( e.getOsmId() == key ) { // note this assumes REMOVED_KEY doesn't match
            return true;
        }
        while ( true )
        {
            ptr = (int) ((ptr + 1) & m_mask); //the next index
            e = m_data[ ptr ];
            if ( e == FREE_KEY ) {
                return false;
            }
            if ( e.getOsmId() == key ) {
                return true;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
	public List<V> values() {
    	int found = 0;
    	ArrayList<V> result = new ArrayList<V>(m_size);
    	for (OsmElement v:m_data) {
    		if (v != FREE_KEY && v != REMOVED_KEY) {
    			result.add((V) v);
    			found++;
    			if (found>=m_size) { // found all
    				break;
    			}
    		}
    	}
    	return result;
    }

    public int size()
    {
        return m_size;
    }
    
    public boolean isEmpty() {
    	return m_size == 0;
    }
    
    public void ensureCapacity(int minimumCapacity) {
    	int newCapacity = Tools.arraySize(minimumCapacity, m_fillFactor);
    	if (newCapacity > m_data.length) {
    		rehash(newCapacity);
    	}
    }

    @SuppressWarnings("unchecked")
	private void rehash( final int newCapacity )
    {
        m_threshold = (int) (newCapacity * m_fillFactor);
        m_mask = newCapacity - 1;

        final int oldCapacity = m_data.length;
        final OsmElement[] oldData = m_data;

        m_data = new OsmElement[ newCapacity ];
        if (FREE_KEY != null){
        	Arrays.fill( m_data, FREE_KEY );
        }
        
        m_size = 0;

        for ( int i = 0; i < oldCapacity; i++ ) {
            final OsmElement e = oldData[ i ];
            if( e != FREE_KEY && e != REMOVED_KEY ) {
                put(e.getOsmId(), (V) e);
            }
        }
    }

    /**
     * Iterator that skips FREE_KEY and REMOVED_KEY values
     */
	@Override
	public Iterator<V> iterator() {
		Iterator<V> it = new Iterator<V>() {
			int index = 0;
			int found = 0;
			
			@Override
			public boolean hasNext() {
				while (true) {
					if (found >= m_size || index >= m_data.length) { // all ready returned all elements					
						return false;
					} else {
						OsmElement e = m_data[index];
						if (e != FREE_KEY && e != REMOVED_KEY) {
							found++;
							return true;
						} else {
							index++;
						}
					}
				}
			}

			@SuppressWarnings("unchecked")
			@Override
			public V next() {
				while (true) {
					if (index >= m_data.length) { // all ready returned all elements
						throw new NoSuchElementException() ;
					} else {
						OsmElement e = m_data[index];
						if (e != FREE_KEY && e != REMOVED_KEY) {
							index++;
							return (V) e;
						} else {
							index++;
						}
					}
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException(); // could be implemented
			}
		
		};
		return it;
	}
	
	/**
	 * for stats and debugging
	 * @return
	 */
	public HashMap<Integer,Integer> getChainStats() {
		HashMap<Integer,Integer> result = new HashMap<Integer,Integer>();
		for (V v:values()) {
			int len = 0;
			long key = v.getOsmId();
	        int ptr = (int) ((Tools.phiMix(key) & m_mask));
	        OsmElement e = m_data[ ptr ];
	        if ( e.getOsmId() == key ) { // note this assumes REMOVED_KEY doesn't match
	        	if (result.containsKey(len)) {
	        		result.put(len, result.get(len)+1);
	        	} else {
	        		result.put(len, 1);
	        	}
	            continue;
	        }
	        while ( true )
	        {
	        	len++;
	            ptr = (int) ((ptr + 1) & m_mask); //the next index
	            e = m_data[ ptr ];
	            if ( e.getOsmId() == key ) {
		        	if (result.containsKey(len)) {
		        		result.put(len, result.get(len)+1);
		        	} else {
		        		result.put(len, 1);
		        	}
	                break;
	            }
	        }	
		}
		return result;
	}
}
