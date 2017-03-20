package de.blau.android.osm;

import java.util.ArrayList;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.filter.TagFilter;
import de.blau.android.filter.TagFilterDatabaseHelper;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
/**
 * Note: these test currently only test the filter logic not the UI
 * @author simon
 *
 */
public class TagFilterTest {
	
	MockWebServerPlus mockServer = null;
	Context context = null;
	AdvancedPrefDatabase prefDB = null;
	SQLiteDatabase db;
	Main main = null;
	
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Before
    public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		Preferences prefs = new Preferences(context);
		prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
		db = new TagFilterDatabaseHelper(context).getWritableDatabase();
		main = mActivityRule.getActivity();
    }
    
    @After
    public void teardown() {
    	db.delete("filterentries",null,null);
    	db.close();
    }
    
    @Test
    public void tagFilterNode() {
    	try {
    		TreeMap<String,String> tags = new TreeMap<String,String>();
    		tags.put(Tags.KEY_BARRIER,Tags.VALUE_KERB);
    		Logic logic = App.getLogic();

    		logic.performAdd(main, 100.0f, 100.0f);

    		Node n1 = logic.getSelectedNode();
    		
    		logic.setSelectedNode(null);
    		logic.setSelectedWay(null);
    		n1.setTags(tags);

    		insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "node", Tags.KEY_BUILDING, null);
    			
    		TagFilter f = new TagFilter(context);
    		Assert.assertTrue(!f.include(n1, false));
    		insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "node", "b.*", null);
    		f = new TagFilter(context);
    		Assert.assertTrue(f.include(n1, false));
    		insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "node", null, ".*l");
    		f = new TagFilter(context);
    		Assert.assertTrue(f.include(n1, false));
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    }
    
    @Test
    public void tagFilterWay() {
    	try {
    		TreeMap<String,String> tags = new TreeMap<String,String>();
    		tags.put(Tags.KEY_BUILDING,"yes");
    		Logic logic = App.getLogic();

    		logic.performAdd(main, 100.0f, 100.0f);

    		Node n1 = logic.getSelectedNode();
    		logic.performAdd(main, 1000.0f, 1000.0f);
    		Node n2 = logic.getSelectedNode();
    		Way w = logic.getSelectedWay();
    		logic.setSelectedNode(null);
    		logic.setSelectedWay(null);
    		w.setTags(tags);

    		insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "way", Tags.KEY_BUILDING, null);
    			
    		TagFilter f = new TagFilter(context);
    		Assert.assertTrue(f.include(w, false));
    		Assert.assertTrue(!f.include(n1, false));
    		Assert.assertTrue(!f.include(n2, false));
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    }
    
    @Test
    public void tagFilterWayWithNodes() {
    	try {
    		TreeMap<String,String> tags = new TreeMap<String,String>();
    		tags.put(Tags.KEY_BUILDING,"yes");
    		Logic logic = App.getLogic();

    		logic.performAdd(main, 100.0f, 100.0f);

    		Node n1 = logic.getSelectedNode();
    		logic.performAdd(main, 1000.0f, 1000.0f);
    		Node n2 = logic.getSelectedNode();
    		Way w = logic.getSelectedWay();
    		logic.setSelectedNode(null);
    		logic.setSelectedWay(null);
    		w.setTags(tags);

    		insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "way+", Tags.KEY_BUILDING, null);
    			
    		TagFilter f = new TagFilter(context);
    		Assert.assertTrue(f.include(w, false));
    		Assert.assertTrue(f.include(n1, false));
    		Assert.assertTrue(f.include(n2, false));
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    }
    
    @Test
    public void tagFilterWayInRelation() {
    	try {
    		TreeMap<String,String> tags = new TreeMap<String,String>();
    		tags.put(Tags.KEY_BUILDING,"yes");
    		Logic logic = App.getLogic();

    		logic.performAdd(main, 100.0f, 100.0f);

    		Node n1 = logic.getSelectedNode();
    		logic.performAdd(main, 1000.0f, 1000.0f);
    		Node n2 = logic.getSelectedNode();
    		Way w = logic.getSelectedWay();
    		logic.setSelectedNode(null);
    		logic.setSelectedWay(null);
    		
       		ArrayList<OsmElement> members = new ArrayList<OsmElement>();
    		members.add(w);
    		Relation r = logic.createRelation(main, "", members);
    		r.setTags(tags);

    		insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "relation", Tags.KEY_BUILDING, null);
    			
    		TagFilter f = new TagFilter(context);
    		Assert.assertTrue(f.include(w, false));
    		Assert.assertTrue(!f.include(n1, false));
    		Assert.assertTrue(!f.include(n2, false));
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    }
    
    @Test
    public void tagFilterWayInRelationWithNodes() {
    	try {
    		TreeMap<String,String> tags = new TreeMap<String,String>();
    		tags.put(Tags.KEY_BUILDING,"yes");
    		Logic logic = App.getLogic();

    		logic.performAdd(main, 100.0f, 100.0f);

    		Node n1 = logic.getSelectedNode();
    		logic.performAdd(main, 1000.0f, 1000.0f);
    		Node n2 = logic.getSelectedNode();
    		Way w = logic.getSelectedWay();
    		logic.setSelectedNode(null);
    		logic.setSelectedWay(null);
    		
       		ArrayList<OsmElement> members = new ArrayList<OsmElement>();
    		members.add(w);
    		Relation r = logic.createRelation(main, "", members);
    		r.setTags(tags);

    		insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "relation+", Tags.KEY_BUILDING, null);
    			
    		TagFilter f = new TagFilter(context);
    		Assert.assertTrue(f.include(w, false));
    		Assert.assertTrue(f.include(n1, false));
    		Assert.assertTrue(f.include(n2, false));
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    }
    
	private void insertTagFilterRow(SQLiteDatabase db, String filter, boolean active, boolean include, String type, String key, String value) {
		ContentValues values = new ContentValues();
		values.put("filter", filter);
		values.put("active", active ? 1 : 0);
		values.put("include", include ? 1 : 0);
		values.put("type", type);
		values.put("key", key);
		values.put("value", value);
		db.insert("filterentries", null, values);
	}
}
