
import org.junit.Test;

import de.blau.android.util.collections.LongHashSet;
import de.blau.android.util.collections.LongOsmElementMap;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollectionTest {
    
    @Test
	public void hashmap() {
		LongOsmElementMap<Node> map = new LongOsmElementMap<Node>(100000); 
		
		ArrayList<Node>elements = new ArrayList<Node>(100000);
		for (int i=0;i<100000;i++) { // worst case from a key pov
			elements.add(OsmElementFactory.createNode((long) (Math.random()*Long.MAX_VALUE), 1L, OsmElement.STATE_CREATED, 0, 0));
		}
		for (int i=0;i<100000;i++) { // worst case from a key pov
			Node n = elements.get(i);
			map.put(n.getOsmId(), n);
		}
	
		for (int i=0;i<100000;i++) { // worst case from a key pov
			assertTrue(map.containsKey(elements.get(i).getOsmId()));
		}
	}
	
    @Test
	public void hashset() {
		LongHashSet set = new LongHashSet(100000); 
		
		long[] l =  new long[100000];
		for (int i=0;i<100000;i++) { // worst case from a key pov
			l[i] = (long) ((Math.random()-0.5D)*2*Long.MAX_VALUE);
		}
		for (int i=0;i<100000;i++) { // worst case from a key pov
			set.put(l[i]);
		}
	
		for (int i=0;i<100000;i++) { // worst case from a key pov
			assertTrue(set.contains(l[i]));
		}
	}
}