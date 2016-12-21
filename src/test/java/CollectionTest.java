
import org.junit.Test;

import android.util.Log;
import de.blau.android.util.collections.LongHashSet;
import de.blau.android.util.collections.LongOsmElementMap;
import de.blau.android.util.rtree.BoundedObject;
import de.blau.android.util.rtree.RTree;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertTrue;

public class CollectionTest {
    
    @Test
	public void hashmap() {
		LongOsmElementMap<Node> map = new LongOsmElementMap<Node>(100000); 
		
		ArrayList<Node>elements = new ArrayList<Node>(100000);
		for (int i=0;i<100000;i++) { 
			elements.add(OsmElementFactory.createNode((long) (Math.random()*Long.MAX_VALUE), 1L, OsmElement.STATE_CREATED, 0, 0));
		}
		for (int i=0;i<100000;i++) { 
			Node n = elements.get(i);
			map.put(n.getOsmId(), n);
		}
	
		for (int i=0;i<100000;i++) { 
			assertTrue(map.containsKey(elements.get(i).getOsmId()));
		}
	}
	
    @Test
	public void hashset() {
		LongHashSet set = new LongHashSet(100000); 
		
		long[] l =  new long[100000];
		for (int i=0;i<100000;i++) { 
			l[i] = (long) ((Math.random()-0.5D)*2*Long.MAX_VALUE);
		}
		for (int i=0;i<100000;i++) { 
			set.put(l[i]);
		}
	
		for (int i=0;i<100000;i++) { 
			assertTrue(set.contains(l[i]));
		}
	}
    
    @Test
	public void rtree() {
		final double MAX = BoundingBox.MAX_LAT_E7;
		RTree tree = new RTree(2,100);
		final int NODES = 10000;
		Node[] temp = new Node[NODES];
		for (long i=0;i<NODES;i++) {
			temp[(int) i]= OsmElementFactory.createNode(i, 1L, OsmElement.STATE_CREATED,(int)(Math.random()*MAX), (int)(Math.random()*MAX));
		}
		long start = System.currentTimeMillis();	
		for (long i=0;i<NODES;i++) { 
			tree.insert(temp[(int) i]);
		}
		System.out.println("Node insertion " + (System.currentTimeMillis()-start));
		for (int i=0;i<NODES;i++) { 
			Collection<BoundedObject>result = new ArrayList<BoundedObject>();
			BoundingBox b = null;
			// create a small bounding box around the Node and query that, since contains doesn't seem to work for Nodes
			try {
				b = new BoundingBox(temp[i].getLon()-1,temp[i].getLat()-1,temp[i].getLon()+1,temp[i].getLat()+1);
			} catch (OsmException e) {
			}
			tree.query(result, b);
			assertTrue(result.contains(temp[i]));
		}
	}
}