package de.blau.android.propertyeditor;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.test.suitebuilder.annotation.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerServer;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PropertyEditorTest {
	
	MockWebServerPlus mockServer = null;
	Context context = null;
	ActivityMonitor monitor = null;
	AdvancedPrefDatabase prefDB = null;
	Instrumentation instrumentation = null;
	Main main = null;
	UiDevice mDevice = null;
	
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Before
    public void setup() {
    	instrumentation = InstrumentationRegistry.getInstrumentation();
		context = instrumentation.getTargetContext();
		monitor = instrumentation.addMonitor(PropertyEditor.class.getName(), null, false);
		main = (Main)mActivityRule.getActivity(); 
		Preferences prefs = new Preferences(context);
		prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
		main.getMap().setPrefs(main, prefs);
    	mockServer = new MockWebServerPlus();
 		HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
		System.out.println("mock api url " + mockBaseUrl.toString());
 		prefDB = new AdvancedPrefDatabase(context);
 		prefDB.deleteAPI("Test");
		prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", null, false);
 		prefDB.selectAPI("Test");
 		mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }
    
    @After
    public void teardown() {
		try {
			mockServer.server().shutdown();
			instrumentation.removeMonitor(monitor);
		} catch (IOException ioex) {
			System.out.println("Stopping mock webserver exception " + ioex);
		}
    }
    
    @Test
	public void existingNode() {
    	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("capabilities1");
    	mockServer.enqueue("download1");
    	Logic logic = App.getLogic();
    	try {
			logic.downloadBox(main, new BoundingBox(8.3879800D,47.3892400D,8.3844600D,47.3911300D), false, new SignalHandler(signal));
		} catch (OsmException e) {
			Assert.fail(e.getMessage());
		}
    	try {
			signal.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
    	Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
    	Assert.assertNotNull(n);

    	main.performTagEdit(n, null, false, false, false);
    	Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
    	Assert.assertTrue(propertyEditor instanceof PropertyEditor);
    	TestUtils.clickText(mDevice, true, main.getString(R.string.menu_tags), false);
    	final String original = "Bergdietikon";
    	final String edited = "dietikonBerg";
    	mDevice.wait(Until.findObject(By.clickable(true).textStartsWith(original)), 500);
		UiObject editText = mDevice.findObject(new UiSelector().clickable(true).textStartsWith(original));
		try {
			editText.click(); //NOTE this seems to be necessary
			editText.setText(edited);
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}
		TestUtils.clickUp(mDevice); 
    	Assert.assertEquals(edited, n.getTagWithKey(Tags.KEY_NAME));
    }
    
    @Test
	public void newNode() {
		Logic logic = App.getLogic();
		Map map = main.getMap();
		logic.setZoom(map, 20);
		float tolerance = DataStyle.getCurrent().wayToleranceValue;
		System.out.println("Tolerance " + tolerance);

		logic.setSelectedWay(null);
		logic.setSelectedNode(null);
		logic.setSelectedRelation(null);
		try {
			logic.performAdd(main, 1000.0f, 0.0f);
		} catch (OsmIllegalOperationException e1) {
			Assert.fail(e1.getMessage());
		}
		
		Node n = logic.getSelectedNode();
    	Assert.assertNotNull(n);

    	main.performTagEdit(n, null, false, false, false);
    	Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
    	Assert.assertTrue(propertyEditor instanceof PropertyEditor);
    	TestUtils.clickText(mDevice, true, main.getString(R.string.tag_details), false);
    	mDevice.wait(Until.findObject(By.clickable(true).res("de.blau.android:id/editKey")), 500);
		UiObject editText = mDevice.findObject(new UiSelector().clickable(true).resourceId("de.blau.android:id/editKey"));
		try {
			editText.setText("key");
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}
		editText = mDevice.findObject(new UiSelector().clickable(true).resourceId("de.blau.android:id/editValue"));
		try {
			editText.setText("value");
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}
		TestUtils.clickUp(mDevice);
    	Assert.assertTrue(n.hasTag("key", "value"));
    }
    
    @Test
	public void way() {
    	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("capabilities1");
    	mockServer.enqueue("download1");
    	Logic logic = App.getLogic();
    	try {
			logic.downloadBox(main, new BoundingBox(8.3879800D,47.3892400D,8.3844600D,47.3911300D), false, new SignalHandler(signal));
		} catch (OsmException e) {
			Assert.fail(e.getMessage());
		}
    	try {
			signal.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
    	Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604);
    	Assert.assertNotNull(w);

    	main.performTagEdit(w, null, false, false, false);
    	Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
    	Assert.assertTrue(propertyEditor instanceof PropertyEditor);
    	UiObject text = mDevice.findObject(new UiSelector().textStartsWith("Kindhauserstrasse"));
    	Assert.assertTrue(text.exists());
    }
    
    @Test
	public void relation() {
   	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("capabilities1");
    	mockServer.enqueue("download1");
    	Logic logic = App.getLogic();
    	try {
			logic.downloadBox(main, new BoundingBox(8.3879800D,47.3892400D,8.3844600D,47.3911300D), false, new SignalHandler(signal));
		} catch (OsmException e) {
			Assert.fail(e.getMessage());
		}
    	try {
			signal.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
    	Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
    	Assert.assertNotNull(r);

    	main.performTagEdit(r, null, false, false, false);
    	Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
    	Assert.assertTrue(propertyEditor instanceof PropertyEditor);
    	
    	TestUtils.clickText(mDevice, true, main.getString(R.string.tag_details), false);
    	TestUtils.clickText(mDevice, true, main.getString(R.string.members), false);
    	UiObject text = mDevice.findObject(new UiSelector().textStartsWith("Vorbühl"));
    	Assert.assertTrue(text.exists());
    }
    
    @Test
	public void maxTagLength() {
   	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("capabilities1");
    	mockServer.enqueue("download1");
    	Logic logic = App.getLogic();
    	try {
			logic.downloadBox(main, new BoundingBox(8.3879800D,47.3892400D,8.3844600D,47.3911300D), false, new SignalHandler(signal));
		} catch (OsmException e) {
			Assert.fail(e.getMessage());
		}
    	try {
			signal.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
    	Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
    	Assert.assertNotNull(r);

    	main.performTagEdit(r, null, false, false, false);
    	Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
    	Assert.assertTrue(propertyEditor instanceof PropertyEditor);
    	
       	String tooLongText = 
     			  "This is a very long text string to test the that the API limit of 255 characters is enforced by the PropertyEditor by truncating and showing a toast."
     			+ "This is some more text so that we can actually test the limit by entering a string that is too long to trigger the check";
    	
       	TestUtils.clickText(mDevice, true, main.getString(R.string.menu_tags), false);
       	
    	mDevice.wait(Until.findObject(By.clickable(true).textStartsWith("Dietikon Bahnhof")), 500);
		UiObject editText = mDevice.findObject(new UiSelector().clickable(true).textStartsWith("Dietikon Bahnhof"));
		try {
			editText.click(); //NOTE this seems to be necessary
			editText.setText(tooLongText);
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}
       	
       	TestUtils.clickText(mDevice, true, main.getString(R.string.tag_details), false);
    	
    	mDevice.wait(Until.findObject(By.clickable(true).textStartsWith("from")), 500);
		editText = mDevice.findObject(new UiSelector().clickable(true).textStartsWith("from"));
		try {
			editText.click(); //NOTE this seems to be necessary
			editText.setText(tooLongText);
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}
    	
    	editText = mDevice.findObject(new UiSelector().clickable(true).textStartsWith("Kindhausen AG"));
		try {
			editText.click(); //NOTE this seems to be necessary
			editText.setText(tooLongText);
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}
		
		TestUtils.clickText(mDevice, true, main.getString(R.string.members), false);
		
		mDevice.wait(Until.findObject(By.clickable(true).textStartsWith("stop")), 500);
    	editText = mDevice.findObject(new UiSelector().clickable(true).textStartsWith("stop")); // this should be node #416064528
		try {
			editText.click(); //NOTE this seems to be necessary
			editText.setText(tooLongText);
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}
		
		TestUtils.clickUp(mDevice); // close the PropertEditor and save
		
		Assert.assertEquals(255, r.getTagWithKey("to").length()); // value on the form
		
		String tooLongKey = tooLongText.substring(0, 255);
    	Assert.assertTrue(r.hasTagKey(tooLongKey)); // key on details
    	Assert.assertEquals(255, r.getTagWithKey(tooLongKey).length()); // value on details
    	
    	Assert.assertEquals(255,r.getMember(Node.NAME, 416064528).getRole().length()); // role of node #416064528
    }
    
    String getTranslatedPresetGroupName(String name) {
    	String result = null;
    	Preset[] presets = App.getCurrentPresets(main);
    	for (Preset p:presets) {
    		if (p != null) {
    			PresetGroup group = p.getGroupByName(name);
    			if (group != null) {
    				return group.getTranslatedName();
    			}
    		}
    	}
    	return result;
    }
    
    String getTranslatedPresetItemName(String name) {
    	String result = null;
    	Preset[] presets = App.getCurrentPresets(main);
    	for (Preset p:presets) {
    		if (p != null) {
    			PresetItem item = p.getItemByName(name);
    			if (item != null) {
    				return item.getTranslatedName();
    			}
    		}
    	}
    	return result;
    }
    
    @Test
	public void presets() {
   	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("capabilities1");
    	mockServer.enqueue("download1");
    	Logic logic = App.getLogic();
    	try {
			logic.downloadBox(main, new BoundingBox(8.3879800D,47.3892400D,8.3844600D,47.3911300D), false, new SignalHandler(signal));
		} catch (OsmException e) {
			Assert.fail(e.getMessage());
		}
    	try {
			signal.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
      	Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604L);
    	Assert.assertNotNull(w);

    	main.performTagEdit(w, null, false, false, false);
    	Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
    	Assert.assertTrue(propertyEditor instanceof PropertyEditor);
    	
    	if (!((PropertyEditor)propertyEditor).paneLayout()) {
    		Assert.assertTrue(TestUtils.clickText(mDevice, true, main.getString(R.string.tag_menu_preset), false));
    	}
    	boolean found = TestUtils.clickText(mDevice, true, getTranslatedPresetGroupName("Highways"), true);
    	Assert.assertTrue(found);
    	found = TestUtils.clickText(mDevice, true, getTranslatedPresetGroupName("Streets"), true);
    	Assert.assertTrue(found);
    	found = TestUtils.clickText(mDevice, true, getTranslatedPresetItemName("Motorway"), true);
    	Assert.assertTrue(found);
    }  
    
    @Test
	public void presetsViaManualTag() {
   	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("capabilities1");
    	mockServer.enqueue("download1");
    	Logic logic = App.getLogic();
    	try {
			logic.downloadBox(main, new BoundingBox(8.3879800D,47.3892400D,8.3844600D,47.3911300D), false, new SignalHandler(signal));
		} catch (OsmException e) {
			Assert.fail(e.getMessage());
		}
    	try {
			signal.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
      	Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 210468113L);
    	Assert.assertNotNull(w);
    	try {
			logic.setTags(main, w, null);
		} catch (OsmIllegalOperationException e) {
			Assert.fail(e.getMessage());
		}

    	main.performTagEdit(w, null, false, false, false);
    	Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
    	Assert.assertTrue(propertyEditor instanceof PropertyEditor);
    	
    	TestUtils.clickText(mDevice, true, main.getString(R.string.tag_details), false);
    	mDevice.wait(Until.findObject(By.clickable(true).res("de.blau.android:id/editValue")), 500);

		UiObject keyEditText = mDevice.findObject(new UiSelector().clickable(true).resourceId("de.blau.android:id/editKey"));
		UiObject valueEditText = mDevice.findObject(new UiSelector().clickable(true).resourceId("de.blau.android:id/editValue"));
		
		String key=Tags.KEY_BUILDING;
		String value = Tags.VALUE_YES;
		try {
			keyEditText.click(); 
			keyEditText.setText(key);
			valueEditText.click(); 
			valueEditText.setText(value);
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}
    	
    	if (!((PropertyEditor)propertyEditor).paneLayout()) {
    	   	TestUtils.clickText(mDevice, true, main.getString(R.string.menu_tags), false);
    	}
    	PresetItem presetItem = ((PropertyEditor)propertyEditor).getBestPreset();
    	Assert.assertEquals("Building", presetItem.getName());
    	Assert.assertTrue(TestUtils.clickText(mDevice, true, presetItem.getTranslatedName(), false)); // building preset should now be added to MRU
    }  
    
    @Test
	public void emptyKey() {
    	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("capabilities1");
    	mockServer.enqueue("download1");
    	Logic logic = App.getLogic();
    	try {
			logic.downloadBox(main, new BoundingBox(8.3879800D,47.3892400D,8.3844600D,47.3911300D), false, new SignalHandler(signal));
		} catch (OsmException e) {
			Assert.fail(e.getMessage());
		}
    	try {
			signal.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
    	Node n = (Node) logic.performAddNode(main, 1.0, 1.0);
    	Assert.assertNotNull(n);

    	main.performTagEdit(n, null, false, false, false);
    	Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
    	Assert.assertTrue(propertyEditor instanceof PropertyEditor);
    	TestUtils.clickText(mDevice, true, main.getString(R.string.tag_details), false);
    	mDevice.wait(Until.findObject(By.clickable(true).res("de.blau.android:id/editValue")), 500);
		UiObject editText = mDevice.findObject(new UiSelector().clickable(true).resourceId("de.blau.android:id/editValue"));
		String edited="edited";
		try {
			editText.click(); //NOTE this seems to be necessary
			editText.setText(edited);
		} catch (UiObjectNotFoundException e) {
			Assert.fail(e.getMessage());
		}
		TestUtils.clickUp(mDevice); 
    	Assert.assertFalse(n.hasTag("",edited));
    }
}
