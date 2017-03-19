package de.blau.android.osm;

import java.util.HashMap;
import java.util.SortedMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BasicStuffTest {
	
	Context context = null;
	    
    @Rule
    public UiThreadTestRule uiThreadTestRule = new UiThreadTestRule();

    @Before
    public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		Preferences prefs = new Preferences(context);
		prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
    	App.getDelegator().reset(false);
		App.getDelegator().setOriginalBox(BoundingBox.getMaxMercatorExtent());
    }
    
    @After
    public void teardown() {
    }

    @Test
	public void setTags() {
    	// 
    	Logic logic = App.getLogic();
    	logic.setSelectedWay(null);
    	logic.setSelectedNode(null);
    	logic.setSelectedRelation(null);
    	// nodes
    	try {
    		logic.performAdd(null, 100.0f, 100.0f);
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    	Node n = logic.getSelectedNode();
    	Assert.assertNotNull(n);
    	System.out.println(n);
    	Assert.assertEquals(1, App.getDelegator().getApiNodeCount());
       	OsmElementFactory factory = App.getDelegator().getFactory();
    	Node n2 = factory.createNodeWithNewId(0, 0);
       	setTagsElement(logic, n, n2); 
       	// ways
		try {
			logic.performAdd(null,150.0f, 150.0f);
		} catch (OsmIllegalOperationException e) {
			Assert.fail(e.getMessage());
		}
       	Way w = logic.getSelectedWay();
       	Assert.assertNotNull(w);
    	System.out.println(w);
    	Assert.assertEquals(1, App.getDelegator().getApiWayCount());
    	Way w2 = factory.createWayWithNewId(); // node-less way!
       	setTagsElement(logic, w, w2); 
       	// FIXME do the same for relations
    }

	private void setTagsElement(Logic logic, OsmElement eInStorage, OsmElement eNotInStorage) {
		String key1 = "key1";
    	String value1 = "value1";
    	String key2 = "key2";
    	String value2 = "value2";
    	Assert.assertFalse(eInStorage.hasTags());
    	HashMap<String,String>tags = new HashMap<String,String>();

    	tags.put(key1, value1);
    	// new form
    	try {
    		logic.setTags(eInStorage, tags); 
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    	Assert.assertTrue(eInStorage.hasTags());
    	Assert.assertTrue(eInStorage.hasTagKey(key1));
    	Assert.assertTrue(eInStorage.hasTag(key1,value1));
    	Assert.assertEquals(value1, eInStorage.getTagWithKey(key1));
    	SortedMap<String, String> m = eInStorage.getTags();
    	try {
    		m.put(key2, value2);
    		Assert.fail("Map returned from getTags should be immutable");
    	} catch (UnsupportedOperationException ex) {
    		// just carry on
    	}
    	tags.clear();
    	tags.putAll(m);
    	tags.put(key2,  value2);
    	try {
    		logic.setTags(eInStorage, tags); 
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    	Assert.assertTrue(eInStorage.hasTags());
    	Assert.assertTrue(eInStorage.hasTagKey(key1));
    	Assert.assertTrue(eInStorage.hasTag(key1,value1));
    	Assert.assertEquals(value1, eInStorage.getTagWithKey(key1));
    	Assert.assertTrue(eInStorage.hasTagKey(key2));
    	Assert.assertTrue(eInStorage.hasTag(key2,value2));
    	Assert.assertEquals(value2, eInStorage.getTagWithKey(key2));
    	try {
    		logic.setTags(eInStorage, null);
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    	Assert.assertFalse(eInStorage.hasTags());
    	// old form
    	Assert.assertTrue(logic.setTags(eInStorage.getName(),eInStorage.getOsmId(), tags)); 
    	Assert.assertTrue(eInStorage.hasTags());
    	Assert.assertTrue(eInStorage.hasTagKey(key1));
    	Assert.assertTrue(eInStorage.hasTag(key1,value1));
    	Assert.assertEquals(value1, eInStorage.getTagWithKey(key1));
    	Assert.assertTrue(logic.setTags(eInStorage.getName(),eInStorage.getOsmId(), null));
    	Assert.assertFalse(eInStorage.hasTags());
    	//
 
       	try {
    		logic.setTags(eNotInStorage, tags);
    		Assert.fail("Element not in storage should fail");
    	} catch (OsmIllegalOperationException e) {
    		// carry on
    	}
       	Assert.assertFalse(logic.setTags(eNotInStorage.getName(),eNotInStorage.getOsmId(), tags));
	}
}
