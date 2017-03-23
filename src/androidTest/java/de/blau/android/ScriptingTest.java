package de.blau.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.javascript.EvaluatorException;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import de.blau.android.javascript.Utils;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ScriptingTest {
	
	Context context = null;
	Main main = null;
	
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);
    
    @Before
    public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		Preferences prefs = new Preferences(context);
		prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
		main = mActivityRule.getActivity();
		main.getMap().setPrefs(main, prefs);
    	App.getDelegator().reset(false);
		App.getDelegator().setOriginalBox(BoundingBox.getMaxMercatorExtent());
    }
    
    @After
    public void teardown() {
    }
 
    @Test
	public void sandbox() {
    	// normal scope
    	String r = Utils.evalString(context, "sandbox1", "b = new BoundingBox();");
    	Assert.assertEquals("(0,0,0,0)", r);
    	r = Utils.evalString(context, "sandbox2", "b = GeoMath.createBoundingBoxForCoordinates(0,0,10,false);");
    	Assert.assertEquals("(-899,-899,899,899)", r);
    	try {
    		r = Utils.evalString(context, "sandbox3", "importClass(Packages.de.blau.android.App);");
    		Assert.fail("Sandbox should stop further importing");
    	} catch (EvaluatorException ex) {
    		// carry on
    	}
    	// scope for presets
    	Map<String, ArrayList<String>> tags = new HashMap<String, ArrayList<String>>();
    	ArrayList<String>v = new ArrayList<String>();
    	v.add("value");
    	tags.put("key", v);
    	r = Utils.evalString(context, "sandbox4", "a = new java.util.ArrayList(); a.add('value1'); tags.put('key1',a);tags", tags, "test");
    	Assert.assertEquals("{key=[value], key1=[value1]}", r);
    	try {
    		r = Utils.evalString(context, "sandbox4", "importClass(Packages.de.blau.android.App);", App.getLogic());
    		Assert.fail("Sandbox should stop further importing");
    	} catch (EvaluatorException ex) {
    		// carry on
    	}
    	// scope for general scripting
    	r = Utils.evalString(context, "sandbox5", "b = new BoundingBox();", App.getLogic());
    	Assert.assertEquals("(0,0,0,0)", r);
    	r = Utils.evalString(context, "sandbox6", "b = GeoMath.createBoundingBoxForCoordinates(0,0,10,false);", App.getLogic());
    	Assert.assertEquals("(-899,-899,899,899)", r);
    	r = Utils.evalString(context, "sandbox7", "logic.getModifiedNodes().size() + logic.getNodes().size()", App.getLogic());
    	Assert.assertEquals("0", r);
    	try {
    		r = Utils.evalString(context, "sandbox8", "importClass(Packages.de.blau.android.App);", App.getLogic());
    		Assert.fail("Sandbox should stop further importing");
    	} catch (EvaluatorException ex) {
    		// carry on
    	}
    }
}
