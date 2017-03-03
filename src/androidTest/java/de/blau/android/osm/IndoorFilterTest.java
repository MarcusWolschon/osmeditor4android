package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.filter.IndoorFilter;
import de.blau.android.filter.TagFilter;
import de.blau.android.filter.TagFilterDatabaseHelper;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TransferTasks;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
/**
 * Note: these test currently only test the filter logic not the UI
 * @author simon
 *
 */
public class IndoorFilterTest {
	
	MockWebServerPlus mockServer = null;
	Context context = null;
	AdvancedPrefDatabase prefDB = null;
	
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Before
    public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		Preferences prefs = new Preferences(context);
		prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
    }
    
    @After
    public void teardown() {
    }
    
    @Test
	public void indoorFilterNode() {
    	TreeMap<String,String> tags = new TreeMap<String,String>();
    	tags.put(Tags.KEY_LEVEL,"" + 1);
    	tags.put(Tags.KEY_REPEAT_ON,"" + 11);
    	Logic logic = App.getLogic();
    	Node n = logic.performAddNode(1.0D, 1.0D);
    	IndoorFilter f = new IndoorFilter();
    	Assert.assertTrue(!f.include(n, false));
    	f.clear();
    	n.setTags(tags);
    	f.setLevel(9); 	
    	Assert.assertTrue(f.include(n, true));
    	logic.setSelectedNode(null);
    	f.clear();
    	n.setTags(tags);
    	f.setLevel(9); 	
    	Assert.assertTrue(!f.include(n, false));
    	f.clear();
    	f.setLevel(1);
    	Assert.assertTrue(f.include(n, false));
    	f.clear();
    	f.setLevel(11);
    	Assert.assertTrue(f.include(n, false));
    	ArrayList<OsmElement> members = new ArrayList<OsmElement>();
    	members.add(n);
    	Relation r = logic.createRelation("", members);
    	f.clear();
    	tags.clear();
    	tags.put(Tags.KEY_MIN_LEVEL,"" + 8);
    	tags.put(Tags.KEY_MAX_LEVEL,"" + 10);
    	tags.put(Tags.KEY_BUILDING,"yes");
    	r.addTags(tags);
    	f.setLevel(9);
    	Assert.assertTrue(f.include(n, false));
	}
    
    @Test
	public void indoorFilterNodeInverted() {
    	TreeMap<String,String> tags = new TreeMap<String,String>();
    	tags.put(Tags.KEY_ENTRANCE,"yes");
    	Logic logic = App.getLogic();
    	Node n = logic.performAddNode(1.0D, 1.0D);
    	n.setTags(tags);
    	
    	IndoorFilter f = new IndoorFilter();
    	f.setInverted(true);
    	f.setLevel(9); 	
    	Assert.assertTrue(f.include(n, false)); 	
	}
    
    @Test
    public void indoorFilterWay() {
    	try {
    		TreeMap<String,String> tags = new TreeMap<String,String>();
    		tags.put(Tags.KEY_LEVEL,"" + 1);
    		tags.put(Tags.KEY_REPEAT_ON,"" + 11);
    		Logic logic = App.getLogic();

    		logic.performAdd(100.0f, 100.0f);

    		Node n1 = logic.getSelectedNode();
    		logic.performAdd(1000.0f, 1000.0f);
    		Node n2 = logic.getSelectedNode();
    		Way w = logic.getSelectedWay();
    		logic.setSelectedNode(null);
    		logic.setSelectedWay(null);
    		w.setTags(tags);

    		IndoorFilter f = new IndoorFilter();
    		f.setLevel(9); 	
    		Assert.assertTrue(!f.include(w, false));
    		f.clear();
    		f.setLevel(1);
    		Assert.assertTrue(f.include(w, false));
    		f.clear();
    		f.setLevel(11);
    		Assert.assertTrue(f.include(w, false));
    		ArrayList<OsmElement> members = new ArrayList<OsmElement>();
    		members.add(w);
    		Relation r = logic.createRelation("", members);
    		f.clear();
    		tags.clear();
    		tags.put(Tags.KEY_MIN_LEVEL,"" + 8);
    		tags.put(Tags.KEY_MAX_LEVEL,"" + 10);
    		tags.put(Tags.KEY_BUILDING,"yes");
    		r.addTags(tags);
    		f.setLevel(9);
    		Assert.assertTrue(f.include(w, false));
    		// check way nodes
    		Assert.assertTrue(f.include(n1, false));
    		Assert.assertTrue(f.include(n2, false));
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    }
    
    @Test
    public void indoorFilterWayInverted() {
    	try {
    		TreeMap<String,String> tags = new TreeMap<String,String>();
    		tags.put(Tags.KEY_ENTRANCE,"yes");
    		Logic logic = App.getLogic();
    		logic.performAdd(100.0f, 100.0f);

    		Node n1 = logic.getSelectedNode();
    		logic.performAdd(1000.0f, 1000.0f);
    		Node n2 = logic.getSelectedNode();
    		Way w = logic.getSelectedWay();
    		logic.setSelectedNode(null);
    		logic.setSelectedWay(null);
    		w.setTags(tags);

    		IndoorFilter f = new IndoorFilter();
    		f.setInverted(true);
    		f.setLevel(9); 	
    		Assert.assertTrue(f.include(w, false));
    		// check way nodes
    		Assert.assertTrue(f.include(n1, false));
    		Assert.assertTrue(f.include(n2, false));	
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    }
    
    @Test
    public void indoorFilterRelation() {
    	try {
    		TreeMap<String,String> tags = new TreeMap<String,String>();
    		Logic logic = App.getLogic();

    		logic.performAdd(100.0f, 100.0f);
    		logic.performAdd(1000.0f, 1000.0f);
    		Way w = logic.getSelectedWay();
    		logic.setSelectedNode(null);
    		logic.setSelectedWay(null);

    		IndoorFilter f = new IndoorFilter();
    		ArrayList<OsmElement> members = new ArrayList<OsmElement>();
    		members.add(w);
    		Relation r = logic.createRelation("", members);
    		f.clear();
    		tags.clear();
    		tags.put(Tags.KEY_MIN_LEVEL,"" + 8);
    		tags.put(Tags.KEY_MAX_LEVEL,"" + 10);
    		tags.put(Tags.KEY_BUILDING,"yes");
    		r.addTags(tags);
    		f.setLevel(9);
    		Assert.assertTrue(f.include(r, false));
    		Assert.assertTrue(f.include(w, false));
    	} catch (OsmIllegalOperationException e) {
    		Assert.fail(e.getMessage());
    	}
    }
}
