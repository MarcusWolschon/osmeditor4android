
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;

public class BoundingBoxTest {
    
    @Test
	public void boundingBox(){
		try {
			BoundingBox existingBox = new BoundingBox(-10.0,-10.0,10.0,10.0);
			
			ArrayList<BoundingBox> existing = new ArrayList<BoundingBox>();
			existing.add(existingBox);
			
			BoundingBox newBox = new BoundingBox(0.0,0.0,20.0,20.0);
			ArrayList<BoundingBox>result = BoundingBox.newBoxes(existing, newBox);
			assertEquals(2, result.size());
			
			newBox = new BoundingBox(0.0,0.0,5.0,20.0);
			result = BoundingBox.newBoxes(existing, newBox);
			assertEquals(1, result.size());
			
			newBox = new BoundingBox(0.0,0.0,5.0,10.0);
			result = BoundingBox.newBoxes(existing, newBox);
			assertEquals(0, result.size());
			
			newBox = new BoundingBox(-15.0,-15.0,15.0,15.0);
			result = BoundingBox.newBoxes(existing, newBox);
			assertEquals(4, result.size());
			
			newBox = new BoundingBox(20.0,20.0,25.0,25.0);
			result = BoundingBox.newBoxes(existing, newBox);
			assertEquals(1, result.size());
			
			// new bounding box ops
			newBox = new BoundingBox(20.0,20.0,25.0,25.0);
			BoundingBox node = new BoundingBox((int)(30*1E7),(int)(30*1E7));
			newBox.union(node);
			assertEquals((int)(20*1E7),newBox.getLeft());
			assertEquals((int)(20*1E7),newBox.getBottom());
			assertEquals((int)(30*1E7),newBox.getTop());
			assertEquals((int)(30*1E7),newBox.getRight());
			node = new BoundingBox((int)(15*1E7),(int)(15*1E7));
			newBox.union(node);
			assertEquals((int)(15*1E7),newBox.getLeft());
			assertEquals((int)(15*1E7),newBox.getBottom());
			assertEquals((int)(30*1E7),newBox.getTop());
			assertEquals((int)(30*1E7),newBox.getRight());
			BoundingBox newBox2 = new BoundingBox(10.0,10.0,25.0,25.0);
			assertTrue(newBox2.intersects(newBox));
			newBox2 = new BoundingBox(25.0,25.0,40.0,40.0);
			assertTrue(newBox2.intersects(newBox));
			newBox2 = new BoundingBox(10.0,25.0,20.0,40.0);
			assertTrue(newBox2.intersects(newBox));
			
			assertTrue(BoundingBox.intersects(newBox2,newBox));
			newBox2 = new BoundingBox(10.0,10.0,12.0,12.0);
			assertTrue(!newBox2.intersects(newBox));
			assertTrue(!BoundingBox.intersects(newBox2,newBox));
			// assertTrue(BoundingBox.intersects(node,newBox));
		} catch (OsmException e) {
			Assert.fail(e.getMessage());
		}
	}
}