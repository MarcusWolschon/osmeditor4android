package de.blau.android.propertyeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.graphics.Rect;
import android.os.RemoteException;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.RelationMemberPosition;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.MRUTags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;
import de.blau.android.resources.DataStyle;
import de.blau.android.util.ClipboardUtils;
import de.blau.android.util.KeyValue;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PropertyEditorTest {

    MockWebServerPlus    mockServer      = null;
    Context              context         = null;
    ActivityMonitor      monitor         = null;
    AdvancedPrefDatabase prefDB          = null;
    Instrumentation      instrumentation = null;
    Main                 main            = null;
    UiDevice             device          = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        File mruTags = new File("/sdcard/Vespucci/mrutags.xml");
        mruTags.delete();
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        main = (Main) mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        System.out.println("mock api url " + mockBaseUrl.toString());
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", API.Auth.BASIC);
        prefDB.selectAPI("Test");
        prefDB.resetCurrentServer();
        prefs = new Preferences(context);
        App.getLogic().setPrefs(prefs);
        System.out.println(prefs.getServer().getReadWriteUrl());
        device = UiDevice.getInstance(instrumentation);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.unlock(device);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        try {
            mockServer.server().shutdown();
            instrumentation.removeMonitor(monitor);
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
    }

    /**
     * Change tags on an existing node
     */
    @Test
    public void existingNode() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false);
        final String original = "Bergdietikon";
        final String edited = "dietikonBerg";
        device.wait(Until.findObject(By.clickable(true).textStartsWith(original)), 500);
        UiObject editText = device.findObject(new UiSelector().clickable(true).textStartsWith(original));
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText(edited);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        TestUtils.clickHome(device, true);
        assertEquals(edited, n.getTagWithKey(Tags.KEY_NAME));
    }

    /**
     * Test that pressing back does the correct thing(s)
     */
    @Test
    public void backPressed() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        java.util.Map<String,String> oTags = new HashMap<>(n.getTags());
        oTags.remove("openGeoDB:name");
        oTags.remove("openGeoDB:sort_name");
        logic.setTags(main, n, oTags);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false);
        final String original = "Bergdietikon";
        final String edited = "dietikonBerg";
        device.wait(Until.findObject(By.clickable(true).textStartsWith(original)), 500);
        UiObject editText = device.findObject(new UiSelector().clickable(true).textStartsWith(original));
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText(edited);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        device.pressBack(); // get rid of keyboard
        device.pressBack();

        // click cancel
        TestUtils.clickButton(device, "android:id/button3", true);
        assertTrue(TestUtils.findText(device, false, edited));

        device.pressBack();
       
        // click revert
        TestUtils.clickButton(device, "android:id/button2", true);
        assertTrue(TestUtils.findText(device, false, original));

        TestUtils.clickHome(device, true);
        assertEquals(original, n.getTagWithKey(Tags.KEY_NAME));
    }

    /**
     * Add a tag and relation membership to a new node
     */
    @Test
    public void newNode() {
        Logic logic = App.getLogic();
        Map map = main.getMap();
        logic.setZoom(map, 20);

        // create a relation for testing
        Relation r = logic.createRelation(main, Tags.VALUE_ROUTE, new ArrayList<>());
        java.util.Map<String, String> tags = new TreeMap<>(r.getTags());
        tags.put(Tags.KEY_NAME, "test");
        logic.setTags(main, r, tags);

        logic.setSelectedWay(null);
        logic.setSelectedNode(null);
        logic.setSelectedRelation(null);
        try {
            logic.performAdd(main, 1000.0f, 0.0f);
        } catch (OsmIllegalOperationException e1) {
            fail(e1.getMessage());
        }

        Node n = logic.getSelectedNode();
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        device.wait(Until.findObject(By.clickable(true).res(device.getCurrentPackageName() + ":id/editKey")), 500);
        UiObject editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editKey"));
        try {
            editText.setText("key");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editValue"));
        try {
            editText.setText("value");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.relations), false, false));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.tag_menu_addtorelation), true));
        assertTrue(TestUtils.clickText(device, false, "test", true));

        TestUtils.clickHome(device, true);
        assertTrue(n.hasTag("key", "value"));
        List<Relation> parents = n.getParentRelations();
        assertNotNull(parents);
        assertEquals(1, parents.size());
        assertEquals(r, parents.get(0));
    }

    /**
     * Add set a direction value on a new node
     */
    @Test
    public void nodeWithDirection1() {
        Logic logic = App.getLogic();
        Map map = main.getMap();
        logic.setZoom(map, 20);

        logic.setSelectedWay(null);
        logic.setSelectedNode(null);
        logic.setSelectedRelation(null);
        try {
            logic.performAdd(main, 1000.0f, 0.0f);
        } catch (OsmIllegalOperationException e1) {
            fail(e1.getMessage());
        }

        Node n = logic.getSelectedNode();
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Highways"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Waypoints"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Traffic sign"), true, true);
        assertTrue(found);
        UiObject2 direction = null;
        try {
            direction = getField(device, "For traffic direction", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(direction);
        assertEquals(main.getString(R.string.tag_dialog_value_hint), direction.getText());
        direction.clickAndWait(Until.newWindow(), 2000);
        TestUtils.clickText(device, true, main.getString(R.string.save), true, false);
        TestUtils.clickHome(device, true);
        TestUtils.sleep(5000);
        try {
            assertTrue(Integer.parseInt(n.getTagWithKey("direction")) >= 0);
        } catch (NumberFormatException nfex) {
            fail(nfex.getMessage());
        }
    }

    /**
     * Add set a direction value on a new node
     */
    @Test
    public void nodeWithDirection2() {
        Logic logic = App.getLogic();
        Map map = main.getMap();
        logic.setZoom(map, 20);

        logic.setSelectedWay(null);
        logic.setSelectedNode(null);
        logic.setSelectedRelation(null);
        try {
            logic.performAdd(main, 1000.0f, 0.0f);
        } catch (OsmIllegalOperationException e1) {
            fail(e1.getMessage());
        }

        Node n = logic.getSelectedNode();
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Highways"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Waypoints"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Traffic sign"), true, true);
        assertTrue(found);
        UiObject2 direction = null;
        try {
            direction = getField(device, "For traffic direction", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(direction);
        assertEquals(main.getString(R.string.tag_dialog_value_hint), direction.getText());
        direction.clickAndWait(Until.newWindow(), 2000);
        TestUtils.clickText(device, true, "Forward", false, false);
        TestUtils.clickText(device, true, main.getString(R.string.save), true, false);
        TestUtils.clickHome(device, true);
        assertEquals("forward", n.getTagWithKey("direction").toLowerCase());
    }

    @Test
    public void nodeWithDirection3() {
        Logic logic = App.getLogic();
        Map map = main.getMap();
        logic.setZoom(map, 20);

        logic.setSelectedWay(null);
        logic.setSelectedNode(null);
        logic.setSelectedRelation(null);
        try {
            logic.performAdd(main, 1000.0f, 0.0f);
        } catch (OsmIllegalOperationException e1) {
            fail(e1.getMessage());
        }

        Node n = logic.getSelectedNode();
        assertNotNull(n);
        java.util.Map<String, String> tags = new HashMap<>();
        tags.put("traffic_sign", "stop");
        tags.put("direction", "forward");
        logic.setTags(main, n, tags);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        UiObject2 direction = null;
        try {
            direction = getField(device, "For traffic direction", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(direction);
        assertEquals("forward", direction.getText().toLowerCase());
        direction.clickAndWait(Until.newWindow(), 2000);
        TestUtils.clickText(device, true, "Forward", false, false);
        TestUtils.clickText(device, true, main.getString(R.string.save), true, false);
        TestUtils.clickHome(device, true);
        try {
            assertTrue(Integer.parseInt(n.getTagWithKey(Tags.KEY_DIRECTION)) >= 0);
        } catch (NumberFormatException nfex) {
            fail(nfex.getMessage());
        }
    }

    /**
     * Check that we moan if a duplicate key is added
     */
    // @Test this currently can't happen at least in the way the test tries to add a duplicate key
    public void duplicateKey() {
        Logic logic = App.getLogic();
        Map map = main.getMap();
        logic.setZoom(map, 20);

        logic.setSelectedWay(null);
        logic.setSelectedNode(null);
        logic.setSelectedRelation(null);
        try {
            logic.performAdd(main, 1000.0f, 0.0f);
        } catch (OsmIllegalOperationException e1) {
            fail(e1.getMessage());
        }

        Node n = logic.getSelectedNode();
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        device.wait(Until.findObject(By.clickable(true).res(device.getCurrentPackageName() + ":id/editKey")), 500);
        UiObject editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editKey"));
        try {
            editText.setText("key");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editValue"));
        try {
            editText.setText("value");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editKey").instance(1));
        try {
            editText.setText("key");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editValue").instance(1));
        try {
            editText.setText("value2");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.duplicate_tag_key_title)));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true));
        try {
            editText.setText("");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        TestUtils.clickHome(device, true);
        assertTrue(n.hasTag("key", "value"));
    }

    /**
     * Select an untagged node, then - apply restaurant preset - set cuisine and opening_hours
     */
    @Test
    public void node() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        // trying to get node click work properly is frustrating
        // TestUtils.clickAtCoordinates(main.getMap(), 8.3856255, 47.3894333, true);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 599672192L);
        assertNotNull(n);
        final CountDownLatch signal2 = new CountDownLatch(1);
        main.runOnUiThread(() -> {
            main.getEasyEditManager().editElement(n);
            (new SignalHandler(signal2)).onSuccess();
        });
        try {
            signal2.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        // Node n = App.getLogic().getSelectedNode();
        // assertNotNull(n);

        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_tags), false, true));
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        if (!((PropertyEditorActivity) propertyEditor).usingPaneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Facilities"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Food+Drinks"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Restaurant"), true, false);
        assertTrue(found);
        UiObject2 cusine = null;
        try {
            cusine = getField(device, "Cuisine", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(cusine);
        cusine.click();
        TestUtils.scrollTo("Asian", false);
        assertTrue(TestUtils.clickText(device, true, "Asian", false, false));
        TestUtils.scrollTo("German", false);
        assertTrue(TestUtils.clickText(device, true, "German", false, false));
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.save), true, false));
        UiObject2 openingHours = null;
        try {
            openingHours = getField(device, "Opening Hours", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(openingHours);
        openingHours.click();
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.load_templates_title)));
        assertTrue(TestUtils.clickText(device, false, "24 Hours", true, false));
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/save", true);

        TestUtils.scrollTo("Contact", false);
        assertTrue(TestUtils.clickText(device, false, "Contact", false, false));
        UiObject2 phone = null;
        try {
            phone = getField2("Phone number", 0);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        phone.click();

        instrumentation.sendStringSync("444400160");

        assertTrue(TestUtils.findText(device, false, "+41 44 440 01 60", 5000));

        // check that tristate checkboxes work
        TestUtils.scrollTo("Toilets", false);
        UiObject2 toilets = null;
        try {
            toilets = getField(device, "Toilets", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        toilets.click();
        clickOK(); // click away tip

        switchToDetailsTab();
        TestUtils.scrollTo("toilets", false);
        UiObject2 toiletValue = null;
        try {
            toiletValue = getField(device, "toilets", 2);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertEquals(Tags.VALUE_YES, toiletValue.getText());
        switchtoTagsTab();
        try {
            toilets = getField(device, "Toilets", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        toilets.click();
        switchToDetailsTab();
        TestUtils.scrollTo("toilets", false);
        try {
            toiletValue = getField(device, "toilets", 2);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertEquals(Tags.VALUE_NO, toiletValue.getText());
        switchtoTagsTab();
        try {
            toilets = getField(device, "Toilets", 1);
            TestUtils.longClick(device, toilets);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        // apply optional tags and check that diaper tag isn't present
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.tag_menu_apply_preset_with_optional), false, false));
        TestUtils.scrollTo("Diaper changing (deprecated)", false);
        assertFalse(TestUtils.findText(device, false, "Diaper changing (deprecated)"));

        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        device.waitForIdle();
        assertTrue(n.hasTag("cuisine", "asian;german") || n.hasTag("cuisine", "german;asian"));
        assertTrue(n.hasTag("opening_hours", "24/7"));
        assertTrue(n.hasTag("phone", "+41 44 440 01 60"));
        assertFalse(n.hasTagKey("toilets"));
    }

    /**
     * Select an untagged node, then - apply restaurant preset - edit wheelchair access details
     */
    @Test
    public void longText() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);

        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 599672192L);
        assertNotNull(n);
        final CountDownLatch signal2 = new CountDownLatch(1);
        main.runOnUiThread(() -> {
            main.getEasyEditManager().editElement(n);
            (new SignalHandler(signal2)).onSuccess();
        });
        try {
            signal2.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));

        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_tags), false, true));
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        if (!((PropertyEditorActivity) propertyEditor).usingPaneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Facilities"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Food+Drinks"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Restaurant"), true, false);
        assertTrue(found);

        // apply optional tags
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.tag_menu_apply_preset_with_optional), false, false));
        TestUtils.scrollTo("Wheelchair access details", false);

        try {
            UiObject2 details = getField(device, "Wheelchair access details", 1);
            assertNotNull(details);
            details.click();
            UiObject editText = TestUtils.findObjectWithResourceId(device, false, device.getCurrentPackageName() + ":id/editText");
            editText.setText("1234567890");
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save), true));
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertTrue(TestUtils.findText(device, false, "1234567890"));
        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        device.waitForIdle();
        assertTrue(n.hasTag("wheelchair:description", "1234567890"));
    }

    /**
     * Select an untagged node, then - apply charging station preset- set vehicle type
     */
    @Test
    public void checkGroupDialog() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        // trying to get node click work properly is frustrating
        // TestUtils.clickAtCoordinates(main.getMap(), 8.3856255, 47.3894333, true);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 599672192L);
        assertNotNull(n);
        final CountDownLatch signal2 = new CountDownLatch(1);
        main.runOnUiThread(() -> {
            main.getEasyEditManager().editElement(n);
            (new SignalHandler(signal2)).onSuccess();
        });
        try {
            signal2.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));

        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_tags), false, true));
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        if (!((PropertyEditorActivity) propertyEditor).usingPaneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Transport"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Car"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Charging Station"), true, false);
        assertTrue(found);
        UiObject2 vehicles = null;
        try {
            vehicles = getField(device, "Types of vehicles which can be charged", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(vehicles);
        vehicles.click();
        assertTrue(TestUtils.clickText(device, true, "Car", false, false));
        assertTrue(TestUtils.clickText(device, true, "Truck", false, false));
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.save), true, false));
        assertTrue(TestUtils.findText(device, false, "Car"));
        assertTrue(TestUtils.findText(device, false, "Truck"));

        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        device.waitForIdle();
        assertTrue(n.hasTag("motorcar", Tags.VALUE_YES));
        assertTrue(n.hasTag("truck", Tags.VALUE_YES));
    }

    /**
     * What the name says
     */
    public void clickOK() {
        TestUtils.clickText(device, false, main.getString(R.string.okay), true);
    }

    /**
     * Switch to the tag form tab
     */
    public void switchtoTagsTab() {
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false));
    }

    /**
     * switch to the details tab
     */
    public void switchToDetailsTab() {
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false));
    }

    /**
     * Select a way and check if expected street name is there, then - check for max speed dropdown - check for bridge
     * and sidewalk:left checkboxes, change role in relation check that changed keys end up in the MRU tags, undo the
     * role change.
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void way() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 23);
        TestUtils.clickAtCoordinates(device, main.getMap(), 8.3848461, 47.3899166, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickText(device, false, "↖ Kindhauserstrasse", false, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way w = App.getLogic().getSelectedWay();
        assertNotNull(w);

        // determine position in relation
        int pos = determinePosition(w, "Bus 305: Kind");

        assertTrue(TestUtils.clickMenuButton(device, "Properties", false, true));
        waitForPropertyEditor();
        assertTrue(TestUtils.findText(device, false, "Kindhauserstrasse"));
        try {
            UiObject2 valueField = getField(device, "50", 1);
            // clicking doesn't work see https://issuetracker.google.com/issues/37017411
            valueField.click();
            valueField.setText("100");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        //
        // Apply best preset
        assertTrue(TestUtils.clickMenuButton(device, "Apply preset with optional", false, false));
        UiObject2 bridge = null;
        try {
            bridge = getField(device, "Bridge", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(bridge);
        bridge.click();
        clickOK(); // click away tip
        UiObject2 sidewalk = null;
        try {
            sidewalk = getField(device, "Sidewalk", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(sidewalk);
        sidewalk.click();
        assertTrue(TestUtils.clickText(device, true, "Only left side", true, false));

        switchToDetailsTab();
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.relations), false, false));
        assertTrue(TestUtils.findText(device, false, "Bus 305: Kind"));
        try {
            UiObject2 roleField = getField(device, "Bus 305: Kind", 1);
            // clicking doesn't work see https://issuetracker.google.com/issues/37017411
            roleField.setText("platform");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        // exit and test that everything has been set correctly
        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(w.hasTag("maxspeed", "100"));
        assertTrue(w.hasTag("bridge", "yes"));
        assertTrue(w.hasTag("sidewalk", "left"));

        List<Relation> parents = w.getParentRelations();
        assertNotNull(parents);
        assertTrue(findRole("platform", w, parents));

        // find the parent relation we modifed
        Relation found = null;
        for (Relation p : parents) {
            if (p.getTagWithKey(Tags.KEY_NAME).startsWith("Bus 305: Kind")) {
                found = p;
                break;
            }
        }
        assertNotNull(found);
        assertTrue(App.getDelegator().getApiStorage().contains(found));

        TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, true);
        assertFalse(findRole("platform", w, parents));
        assertFalse(App.getDelegator().getApiStorage().contains(found));

        assertEquals(pos, determinePosition(w, "Bus 305: Kind"));

        //
        MRUTags mruTags = App.getMruTags();
        List<String> path = Arrays.asList(new String[] { "Highways", "Streets", "Tertiary" });
        PresetItem item = (PresetItem) Preset.getElementByPath(App.getCurrentRootPreset(context).getRootGroup(), new PresetElementPath(path));
        assertNotNull(item);
        assertTrue(mruTags.getValues(item, "bridge").contains("yes"));
        assertTrue(mruTags.getValues(item, "sidewalk").contains("left"));
        assertTrue(mruTags.getValues(item, "maxspeed").contains("100"));
        assertTrue(mruTags.getKeys(ElementType.WAY).contains("bridge"));
        assertTrue(mruTags.getKeys(ElementType.WAY).contains("sidewalk"));
        path = Arrays.asList(new String[] { "Transport", "Public Transport (Legacy)", "Public transport route (Legacy)" });
        item = (PresetItem) Preset.getElementByPath(App.getCurrentRootPreset(context).getRootGroup(), new PresetElementPath(path));
        assertNotNull(item);
        if (mruTags.getRoles(item) == null) { // hack: we matched the non-legacy version of the preset
            path = Arrays.asList(new String[] { "Transport", "Public Transport", "Public Transport Route (Bus)" });
            item = (PresetItem) Preset.getElementByPath(App.getCurrentRootPreset(context).getRootGroup(), new PresetElementPath(path));
            assertNotNull(item);
        }
        assertNotNull(mruTags.getRoles(item));
        assertTrue(mruTags.getRoles(item).contains("platform"));
    }

    /**
     * Determine the position of a way in a relation
     * 
     * @param w the way
     * @param name the name of the relation
     * @return the position
     */
    private int determinePosition(@NonNull Way w, @NonNull String name) {
        int pos = -1;
        List<Relation> parents = w.getParentRelations();
        assertNotNull(parents);
        for (Relation r : parents) {
            if (r.getTagWithKey(Tags.KEY_NAME).startsWith(name)) {
                final List<RelationMember> members = r.getMembers();
                for (RelationMember rm : members) {
                    if (Way.NAME.equals(rm.getType()) && w.getOsmId() == rm.getRef()) {
                        pos = members.indexOf(rm);
                        break;
                    }
                }
            }
        }
        assertTrue(pos != -1);
        return pos;
    }

    /**
     * Select a way, apply optional, set lane count
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void way2() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 23);
        TestUtils.clickAtCoordinates(device, main.getMap(), 8.3848461, 47.3899166, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickText(device, false, "↖ Kindhauserstrasse", false, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way w = App.getLogic().getSelectedWay();
        assertNotNull(w);
        assertTrue(TestUtils.clickMenuButton(device, "Properties", false, true));
        waitForPropertyEditor();
        assertTrue(TestUtils.findText(device, false, "Kindhauserstrasse"));
        //
        // Apply best preset
        assertTrue(TestUtils.clickMenuButton(device, "Apply preset with optional", false, false));
        UiObject2 lanes = null;
        try {
            lanes = getField(device, "Lanes", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        lanes.click();
        // currently there doesn't seem to be a reliable way to scroll to a specific value
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save), true, false));
        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(w.hasTag("lanes", "0"));
    }

    /**
     * Select a way and check if expected street name is there, change orientation re-check, change back re-check
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void orientationChange() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 22);
        TestUtils.clickAtCoordinates(device, main.getMap(), 8.3848461, 47.3899166, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickText(device, false, "↖ Kindhauserstrasse", false, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way w = App.getLogic().getSelectedWay();
        assertNotNull(w);

        try {
            device.unfreezeRotation();
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        assertTrue(device.isNaturalOrientation());
        try {
            device.setOrientationRight();
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        TestUtils.sleep(2000);
        assertFalse(device.isNaturalOrientation());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        w = App.getLogic().getSelectedWay();
        assertNotNull(w);
        try {
            device.setOrientationNatural();
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        TestUtils.sleep(2000);
        assertTrue(device.isNaturalOrientation());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        w = App.getLogic().getSelectedWay();
        assertNotNull(w);

        assertTrue(TestUtils.clickMenuButton(device, "Properties", false, true));
        waitForPropertyEditor();
        assertTrue(TestUtils.findText(device, false, "Kindhauserstrasse"));

        try {
            device.unfreezeRotation();
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        try {
            device.setOrientationRight();
        } catch (RemoteException e) {
            fail(e.getMessage());
        }

        assertTrue(TestUtils.findText(device, false, "Kindhauserstrasse", 5000));
        try {
            device.setOrientationNatural();
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.findText(device, false, "Kindhauserstrasse", 5000));
    }

    /**
     * Check if the OsmELement e has a specific role in one of its parent relations
     * 
     * @param role the role
     * @param e the OsmElement
     * @param parents the parent Relations
     * @return true if the role was found
     */
    private boolean findRole(@NonNull String role, @NonNull OsmElement e, @NonNull List<Relation> parents) {
        for (Relation parent : parents) {
            List<RelationMember> members = parent.getAllMembers(e);
            for (RelationMember member : members) {
                if (role.equals(member.getRole())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Select a relation and check for a specific member
     */
    @Test
    public void relation() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);

        main.performTagEdit(r, null, false, false);
        waitForPropertyEditor();

        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        TestUtils.clickText(device, true, main.getString(R.string.members), false, false);
        UiObject text = device.findObject(new UiSelector().textStartsWith("Vorbühl"));
        assertTrue(text.exists());

        // select all members
        assertTrue(TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/header_member_selected", false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.tag_action_members_title), 20000));

        // delete all members
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.delete), false, false));
        assertTrue(TestUtils.textGone(device, "Vorbühl", 10000));

        // exit property editor
        TestUtils.clickHome(device, false);

        // empty relation dialog
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.empty_relation_title), 5000));
        assertTrue(r.getMembers().isEmpty());
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.delete), true));
        assertEquals(OsmElement.STATE_DELETED, r.getState());
    }

    /**
     * Select a relation move member up 2 positions and then down one
     */
    @Test
    public void relationMemberMove() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 577098580);
        assertNotNull(n);
        List<RelationMemberPosition> members = r.getAllMembersWithPosition(n);
        assertEquals(1, members.size());
        int oldPos = members.get(0).getPosition();

        main.performTagEdit(r, null, false, false);
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();
        PropertyEditorFragment fragment = getPropertyEditorFragment(propertyEditor);

        assertNotNull(fragment);
        assertTrue(fragment.isPagingEnabled());

        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        TestUtils.clickText(device, true, main.getString(R.string.members), false, false);

        selectMember("Vorbühl");

        assertFalse(fragment.isPagingEnabled());

        clickButtonOrOverflowMenu(main.getString(R.string.tag_menu_move_up));
        clickButtonOrOverflowMenu(main.getString(R.string.tag_menu_move_up));
        clickButtonOrOverflowMenu(main.getString(R.string.tag_menu_move_down));
        // exit property editor
        TestUtils.clickUp(device);
        TestUtils.clickHome(device, false);

        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_relationselect), 5000));
        assertEquals(OsmElement.STATE_MODIFIED, r.getState());
        members = r.getAllMembersWithPosition(n);
        assertEquals(1, members.size());
        assertEquals(oldPos - 1, members.get(0).getPosition());
    }

    /**
     * Get the PropertyEditorFragment
     * 
     * @param propertyEditor
     * @return a propertyeditorfragment or null
     */
    @Nullable
    private PropertyEditorFragment getPropertyEditorFragment(@NonNull PropertyEditorActivity propertyEditor) {
        FragmentManager fm = propertyEditor.getSupportFragmentManager();
        for (Fragment f : fm.getFragments()) {
            if (f instanceof PropertyEditorFragment) {
                return (PropertyEditorFragment) f;
            }
        }
        return null;
    }

    /**
     * Select a relation move member to bottom then down one
     */
    @Test
    public void relationMemberMove2() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 577098580);
        assertNotNull(n);

        main.performTagEdit(r, null, false, false);
        waitForPropertyEditor();

        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        TestUtils.clickText(device, true, main.getString(R.string.members), false, false);

        selectMember("Vorbühl");

        clickButtonOrOverflowMenu(main.getString(R.string.tag_menu_move_bottom));
        clickButtonOrOverflowMenu(main.getString(R.string.tag_menu_move_down));
        // exit property editor
        TestUtils.clickUp(device);
        TestUtils.clickHome(device, false);

        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_relationselect), 5000));
        assertEquals(OsmElement.STATE_MODIFIED, r.getState());
        List<RelationMemberPosition> members = r.getAllMembersWithPosition(n);
        assertEquals(1, members.size());
        assertEquals(0, members.get(0).getPosition()); // should now be at top
    }

    /**
     * Select a relation move member to top then up one
     */
    @Test
    public void relationMemberMove3() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 577098580);
        assertNotNull(n);

        main.performTagEdit(r, null, false, false);
        waitForPropertyEditor();

        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        TestUtils.clickText(device, true, main.getString(R.string.members), false, false);

        selectMember("Vorbühl");

        clickButtonOrOverflowMenu(main.getString(R.string.tag_menu_move_top));
        clickButtonOrOverflowMenu(main.getString(R.string.tag_menu_move_up));
        // exit property editor
        TestUtils.clickUp(device);
        if (!TestUtils.clickHome(device, false)) {
            TestUtils.clickHome(device, false); // try again on android 9
        }

        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_relationselect), 5000));
        assertEquals(OsmElement.STATE_MODIFIED, r.getState());
        List<RelationMemberPosition> members = r.getAllMembersWithPosition(n);
        assertEquals(1, members.size());
        assertEquals(r.getMembers().size() - 1, members.get(0).getPosition());
    }

    /**
     * Select a relation select two members reverse position
     */
    @Test
    public void relationMembersReverse() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);
        RelationMember m1 = r.getMember(Way.NAME, 35479120L);
        RelationMember m2 = r.getMember(Way.NAME, 35479116L);

        List<RelationMember> members = r.getMembers();
        int oldPos1 = members.indexOf(m1);
        int oldPos2 = members.indexOf(m2);

        main.performTagEdit(r, null, false, false);
        waitForPropertyEditor();

        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        TestUtils.clickText(device, true, main.getString(R.string.members), false, false);

        selectMember("#35479120");
        selectMember("#35479116");
        clickButtonOrOverflowMenu(main.getString(R.string.tag_menu_reverse_order));

        // exit property editor
        TestUtils.clickUp(device);
        TestUtils.clickHome(device, false);

        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_relationselect), 5000));
        assertEquals(OsmElement.STATE_MODIFIED, r.getState());

        assertEquals(oldPos1, members.indexOf(m2));
        assertEquals(oldPos2, members.indexOf(m1));
    }

    /**
     * Select a relation "unsort", then sort again
     */
    @Test
    public void relationMembersSort() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);
        RelationMember m1 = r.getMember(Way.NAME, 119104098L);
        assertTrue(m1.downloaded());
        RelationMember m2 = r.getMember(Way.NAME, 27009604L);
        assertTrue(m2.downloaded());
        java.util.Map<String, String> tags2 = new TreeMap<>(m2.getElement().getTags());
        tags2.put(Tags.KEY_NAME, tags2.get(Tags.KEY_NAME) + "2");
        logic.setTags(main, m2.getElement(), tags2);

        main.performTagEdit(r, null, false, false);
        waitForPropertyEditor();

        // 1st unsort
        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        TestUtils.clickText(device, true, main.getString(R.string.members), false, false);
        String name1 = m1.getElement().getTagWithKey(Tags.KEY_NAME);
        TestUtils.scrollToStartsWith(device, name1, r.getMemberCount(), true);

        selectMember(name1);
        clickButtonOrOverflowMenu(main.getString(R.string.tag_menu_move_up));
        // exit property editor
        TestUtils.clickUp(device);
        TestUtils.clickHome(device, false);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_relationselect), 5000));
        assertEquals(OsmElement.STATE_MODIFIED, r.getState());

        List<RelationMember> members = r.getMembers();
        int oldPos1 = members.indexOf(m1);
        int oldPos2 = members.indexOf(m2);
        assertEquals(2, oldPos2 - oldPos1);

        // now sort
        monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        main.performTagEdit(r, null, false, false);
        waitForPropertyEditor();

        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        TestUtils.clickText(device, true, main.getString(R.string.members), false, false);

        String name2 = m2.getElement().getTagWithKey(Tags.KEY_NAME);
        TestUtils.scrollToStartsWith(device, name2, r.getMemberCount(), true);
        selectMember(name1);
        selectMember("#119104097");
        selectMember(name2);

        clickButtonOrOverflowMenu(main.getString(R.string.tag_menu_sort));

        // exit property editor
        TestUtils.clickUp(device);
        TestUtils.clickHome(device, false);

        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_relationselect), 5000));

        oldPos1 = members.indexOf(m1);
        oldPos2 = members.indexOf(m2);
        assertEquals(1, oldPos2 - oldPos1);
    }

    /**
     * Select a relation member
     * 
     * @param description description of the member to select
     */
    private void selectMember(@NonNull String description) {
        UiObject text = device.findObject(new UiSelector().textStartsWith(description));
        assertTrue(text.exists());
        try {
            UiObject checkbox = text.getFromParent(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/member_selected"));
            assertTrue(checkbox.click());
        } catch (UiObjectNotFoundException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Select a relation move member up 2 positions and then down one
     */
    @Test
    public void relationMemberDownload() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);
        RelationMember member = r.getMember(Way.NAME, 35479120L);
        assertNotNull(member);
        assertFalse(member.downloaded());

        main.performTagEdit(r, null, false, false);
        waitForPropertyEditor();

        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        TestUtils.clickText(device, true, main.getString(R.string.members), false, false);

        mockServer.enqueue("way-35479120");
        mockServer.enqueue("way-35479120-nodes");

        selectMember("#35479120");
        clickButtonOrOverflowMenu(main.getString(R.string.download));

        // exit property editor
        TestUtils.clickUp(device);
        TestUtils.clickHome(device, false);

        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_relationselect), 5000));
        assertEquals(OsmElement.STATE_UNCHANGED, r.getState());
        assertTrue(member.downloaded());
    }

    /**
     * Click either the button or the text in the overflow menu
     * 
     * @param text the text to click
     */
    private void clickButtonOrOverflowMenu(@NonNull String text) {
        if (!TestUtils.clickMenuButton(device, text, false, false)) {
            TestUtils.clickOverflowButton(device);
            assertTrue(TestUtils.clickText(device, false, text, false));
        }
    }

    /**
     * Select a relation and check that the membership tab doesn't allow adding to itself
     */
    @Test
    public void relationLoopAvoidance() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);

        main.performTagEdit(r, null, false, false);
        waitForPropertyEditor();

        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        TestUtils.clickText(device, true, main.getString(R.string.members), false, false);
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.relations), false, false));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.tag_menu_addtorelation), true));
        assertFalse(TestUtils.findText(device, false, r.getDescription(main)));
    }

    /**
     * Select a relation and drilldown a specific member
     */
    @Test
    public void relationDrilldown() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);

        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 577098580L);
        assertNotNull(n);
        assertTrue(n.hasTagWithValue("shelter", Tags.VALUE_YES));

        main.performTagEdit(r, null, false, false);
        waitForPropertyEditor();

        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        TestUtils.clickText(device, true, main.getString(R.string.members), false, false);
        UiObject text = device.findObject(new UiSelector().textStartsWith("Vorbühl"));
        assertTrue(text.exists());
        try {
            assertTrue(text.click());
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        TestUtils.clickAwayTip(device, main);
        assertTrue(TestUtils.findText(device, false, "8590205"));
        try {
            UiObject2 shelter = getField(device, "Shelter", 1);
            assertNotNull(shelter);
            shelter.click();
            TestUtils.clickText(device, true, main.getString(R.string.okay), false, false);
        } catch (UiObjectNotFoundException e) {
            fail();
        }

        // exit property editor
        TestUtils.clickHome(device, false);
        TestUtils.clickHome(device, false);
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());
        assertTrue(n.hasTagWithValue("shelter", Tags.VALUE_NO));
    }

    /**
     * Test for max tag length
     */
    // @SdkSuppress(minSdkVersion = 24)
    @Test
    public void maxTagLength() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);

        main.performTagEdit(r, null, false, false);
        waitForPropertyEditor();

        String tooLongText = "This is a very long text string to test the that the API limit of 255 characters is enforced by the PropertyEditorActivity by truncating and showing a toast."
                + "This is some more text so that we can actually test the limit by entering a string that is too long to trigger the check";

        TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false);

        device.wait(Until.findObject(By.clickable(true).textStartsWith("Dietikon Bahnhof")), 500);
        UiObject editText = device.findObject(new UiSelector().clickable(true).textStartsWith("Dietikon Bahnhof"));
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText(tooLongText);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        TestUtils.clickText(device, true, main.getString(R.string.tag_details), true, false);
        device.wait(Until.findObject(By.clickable(true).textStartsWith("from")), 500);
        TestUtils.scrollTo("from", false);

        editText = device.findObject(new UiSelector().clickable(true).textStartsWith("from"));
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText(tooLongText);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        editText = device.findObject(new UiSelector().clickable(true).textStartsWith("Kindhausen AG"));
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText(tooLongText);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        TestUtils.clickText(device, true, main.getString(R.string.members), false, false);

        device.wait(Until.findObject(By.clickable(true).textStartsWith("stop")), 500);
        TestUtils.scrollTo("stop", false);
        editText = device.findObject(new UiSelector().clickable(true).textStartsWith("stop")); // this should be node
                                                                                               // #416064528
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText(tooLongText);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        TestUtils.clickHome(device, true); // close the PropertEditor and save

        assertEquals(255, r.getTagWithKey("to").length()); // value on the form

        String tooLongKey = tooLongText.substring(0, 255);
        assertTrue(r.hasTagKey(tooLongKey)); // key on details
        assertEquals(255, r.getTagWithKey(tooLongKey).length()); // value on details

        assertEquals(255, r.getMember(Node.NAME, 416064528).getRole().length()); // role of node #416064528
    }

    /**
     * Get the translated name of a preset group
     * 
     * @param context an Android Context
     * @param name the group name
     * 
     * @return the translated name
     */
    static String getTranslatedPresetGroupName(Context context, String name) {
        String result = null;
        Preset[] presets = App.getCurrentPresets(context);
        for (Preset p : presets) {
            if (p != null) {
                PresetGroup group = p.getGroupByName(name);
                if (group != null) {
                    return group.getTranslatedName();
                }
            }
        }
        return result;
    }

    /**
     * Get the translated name of a preset item
     * 
     * @param context an Android Context
     * @param name the item name
     * 
     * @return the translated name
     */
    static String getTranslatedPresetItemName(Context context, String name) {
        String result = null;
        Preset[] presets = App.getCurrentPresets(context);
        for (Preset p : presets) {
            if (p != null) {
                PresetItem item = p.getItemByName(name, null);
                if (item != null) {
                    return item.getTranslatedName();
                }
            }
        }
        return result;
    }

    /**
     * Navigate to a specific preset item
     */
    @Test
    public void presets() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604L);
        assertNotNull(w);

        main.performTagEdit(w, null, false, false);
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        if (!((PropertyEditorActivity) propertyEditor).usingPaneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Highways"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Streets"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Motorway"), true, false);
        assertTrue(found);
    }

    /**
     * Apply a preset with alternatives
     */
    @Test
    public void alternativePresets() {
        TestUtils.disableTip(main, R.string.tip_alternative_tagging_key);
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 289987513L);
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        if (!((PropertyEditorActivity) propertyEditor).usingPaneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Highways"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Waypoints"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Mini-Roundabout"), true, false);
        assertTrue(found);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.menu_tags)));
        assertTrue(TestUtils.findText(device, false, "Circular"));
    }

    /**
     * Search for McDo
     */
    @Test
    public void presetSearch() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 577098580L);
        assertNotNull(n);

        main.performTagEdit(n, null, false, true);
        waitForPropertyEditor();

        UiSelector uiSelector = new UiSelector().resourceId(device.getCurrentPackageName() + ":id/preset_search_edit");
        UiObject field = device.findObject(uiSelector);
        try {
            field.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_M);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_C);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_D);
        assertTrue(TestUtils.findText(device, false, "MCM", 5000));
        assertTrue(TestUtils.findText(device, false, "McDonald's", 1000));
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_O);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_N);
        assertTrue(TestUtils.textGone(device, "MCM", 5000));
        assertTrue(TestUtils.findText(device, false, "McDonald's", 1000));
        assertTrue(TestUtils.clickText(device, false, "McDonald's", true, true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.menu_tags), 5000));
        TestUtils.clickHome(device, true); // close the PropertEditor and save
        // NSI unluckily messes things up
        assertTrue("McDonald's".equals(n.getTagWithKey(Tags.KEY_NAME)) || "McDonald's".equals(n.getTagWithKey(Tags.KEY_OPERATOR)));
    }

    /**
     * Search for McDo then delete chars
     */
    @Test
    public void presetSearch2() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 577098580L);
        assertNotNull(n);
        main.performTagEdit(n, null, false, true);
        waitForPropertyEditor();

        UiSelector uiSelector = new UiSelector().resourceId(device.getCurrentPackageName() + ":id/preset_search_edit");
        UiObject field = device.findObject(uiSelector);
        try {
            field.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_M);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_C);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_D);
        assertTrue(TestUtils.findText(device, false, "MCM", 5000));
        assertTrue(TestUtils.findText(device, false, "McDonald's", 1000));
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_DEL);
        assertTrue(TestUtils.textGone(device, "McDonald's", 5000));
    }

    /**
     * Navigate to a specific preset item
     */
    @Test
    public void applyOptional() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604L);
        assertNotNull(w);

        main.performTagEdit(w, null, false, false);
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        if (!((PropertyEditorActivity) propertyEditor).usingPaneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Highways"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Ways"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Steps"), true, false);
        assertTrue(found);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/tag_menu_apply_preset_with_optional", false);
        device.waitForIdle(1000);
        UiObject2 handrail = null;
        try {
            handrail = getField(device, "Handrail", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(handrail);
        handrail.click();
        clickOK(); // click away tip
        UiObject2 overtaking = null;
        try {
            overtaking = getField(device, "Overtaking", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(overtaking);
        overtaking.click();
        device.waitForIdle(1000);
        TestUtils.clickText(device, true, "In way direction", false, false);
        TestUtils.clickText(device, true, "Save", true, false);
        assertTrue(TestUtils.findText(device, false, "In way direction"));
    }

    /**
     * Add a conditional restriction, this is just a rough test without using the actual UI elements of the editor
     */
    @Test
    public void conditionalRestrictionEditor() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604L);
        assertNotNull(w);

        main.performTagEdit(w, null, false, false);
        PropertyEditorActivity<?, ?, ?> propertyEditor = waitForPropertyEditor();

        if (!((PropertyEditorActivity<?, ?, ?>) propertyEditor).usingPaneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Highways"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Streets"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Residential"), true, false);
        assertTrue(found);
        assertTrue(TestUtils.findText(device, false, "Kindhauserstrasse"));
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Cond. & direct. max speed"), true, false);
        assertTrue(found);
        UiObject2 conditionalMaxSpeed = null;
        try {
            conditionalMaxSpeed = getField(device, "Max speed @", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(conditionalMaxSpeed);
        conditionalMaxSpeed.click();
        assertTrue(TestUtils.findText(device, false, "50 @"));

        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.tag_restriction_add_restriction), false));
        UiObject editValue = TestUtils.findObjectWithResourceId(device, device.getCurrentPackageName() + ":id/editValue", 1);
        try {
            editValue.setText("30");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        UiObject condition0 = TestUtils.findObjectWithResourceId(device, device.getCurrentPackageName() + ":id/editCondition", 0);
        try {
            condition0.setText("maxweight>40");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.refresh), false));
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/save", true));
        try {
            conditionalMaxSpeed = getField(device, "Max speed @", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertEquals("50 @ Mo-Fr 19:00-07:00,Sa,Su; 30 @ maxweight>40", conditionalMaxSpeed.getText());
    }

    /**
     * Add a tag and check if that has added a preset item to the MRU display
     */
    @Test
    public void presetsViaManualTag() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 210468113L);
        assertNotNull(w);
        try {
            logic.setTags(main, w, null);
        } catch (OsmIllegalOperationException e) {
            fail(e.getMessage());
        }

        main.performTagEdit(w, null, false, false);
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        device.wait(Until.findObject(By.clickable(true).res(device.getCurrentPackageName() + ":id/editValue")), 500);

        UiObject keyEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editKey"));
        UiObject valueEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editValue"));

        String key = Tags.KEY_BUILDING;
        String value = Tags.VALUE_YES;
        try {
            keyEditText.click();
            keyEditText.setText(key);
            valueEditText.click();
            valueEditText.setText(value);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        if (!((PropertyEditorActivity) propertyEditor).usingPaneLayout()) {
            TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false);
        }
        PropertyEditorFragment<?, ?, ?> f = ((PropertyEditorActivity<?, ?, ?>) propertyEditor)
                .peekBackStack(((PropertyEditorActivity) propertyEditor).getSupportFragmentManager());
        assertNotNull(f);
        PresetItem presetItem = f.getBestPreset();
        assertNotNull(presetItem);
        assertEquals("Building", presetItem.getName());
        assertTrue(TestUtils.clickText(device, true, presetItem.getTranslatedName(), false, false)); // building
                                                                                                     // preset
        // should now be
        // added to MRU
    }

    /**
     * Test that tags with empty keys get removed
     */
    @Test
    public void emptyKey() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) logic.performAddNode(main, 1.0, 1.0);
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        device.wait(Until.findObject(By.clickable(true).res(device.getCurrentPackageName() + ":id/editValue")), 500);
        UiObject editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editValue"));
        String edited = "edited";
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText(edited);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        TestUtils.clickHome(device, true);
        assertFalse(n.hasTag("", edited));
    }

    /**
     * Test that tags with empty values get removed
     */
    @Test
    public void emptyValue() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) logic.performAddNode(main, 1.0, 1.0);
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditorActivity);
        TestUtils.clickText(device, true, main.getString(R.string.tag_details), false, false);
        device.wait(Until.findObject(By.clickable(true).res(device.getCurrentPackageName() + ":id/editKey")), 500);
        UiObject editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editKey"));
        String edited = "edited";
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText(edited);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        TestUtils.clickHome(device, true);
        assertFalse(n.hasTagKey(edited));
    }

    /**
     * Copy tags paste to untagged node
     */
    @Test
    public void tagCopyPaste1() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        instrumentation.removeMonitor(monitor);
        TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false);
        assertTrue(TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/form_header_copy", false));
        assertTrue(TestUtils.clickHome(device, true));
        TestUtils.zoomToLevel(device, main, 21);
        Node n2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 2205498723L);
        assertNotNull(n2);
        TestUtils.clickAtCoordinates(device, main.getMap(), n2.getLon(), n2.getLat());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(2205498723L, node.getOsmId());
        assertTrue(TestUtils.clickOverflowButton(device));
        monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_paste_tags), false));
        waitForPropertyEditor();
        assertTrue(TestUtils.findText(device, false, "Bergdietikon"));
    }

    /**
     * Copy tags paste to untagged node
     */
    @Test
    public void tagCopyPaste2() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        instrumentation.removeMonitor(monitor);
        TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false);
        assertTrue(TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/form_header_copy", false));
        assertTrue(TestUtils.clickHome(device, true));
        TestUtils.zoomToLevel(device, main, 21);
        Node n2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 2205498723L);
        assertNotNull(n2);
        TestUtils.clickAtCoordinates(device, main.getMap(), n2.getLon(), n2.getLat());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(2205498723L, node.getOsmId());
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_tags), false, false));
        monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.tag_details), false));
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_paste), false, false));
        assertTrue(TestUtils.clickHome(device, true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        assertTrue(node.hasTag(Tags.KEY_NAME, "Bergdietikon"));
    }

    /**
     * Copy tags paste to untagged node
     */
    @Test
    public void tagCopyPaste3() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        instrumentation.removeMonitor(monitor);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.tag_details), false));

        TestUtils.scrollTo(Tags.KEY_NAME, false);
        BySelector bySelector = By.textStartsWith("Bergdietikon");
        UiObject2 valueField = device.wait(Until.findObject(bySelector), 500);
        UiObject2 linearLayout = valueField.getParent();
        UiObject2 checkBox = linearLayout.getChildren().get(0);
        checkBox.click();
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_action_tag_title)));
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_copy), false, false));
        assertTrue(TestUtils.textGone(device, context.getString(R.string.tag_action_tag_title), 2000));
        assertTrue(TestUtils.clickHome(device, true));
        TestUtils.zoomToLevel(device, main, 21);
        Node n2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 2205498723L);
        assertNotNull(n2);
        TestUtils.clickAtCoordinates(device, main.getMap(), n2.getLon(), n2.getLat());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(2205498723L, node.getOsmId());
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_tags), false, false));
        monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_paste_from_clipboard), false));
        assertTrue(TestUtils.clickHome(device, true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        assertTrue(node.hasTag(Tags.KEY_NAME, "Bergdietikon"));
        List<KeyValue> tags = ClipboardUtils.getKeyValues(main);
        assertNotNull(tags);
        assertEquals(1, tags.size());
        KeyValue name = tags.get(0);
        assertEquals(name.getKey(), Tags.KEY_NAME);
        assertEquals(name.getValue(), "Bergdietikon");
    }

    /**
     * Cut tags paste to untagged node
     */
    @Test
    public void tagCutPaste() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);

        main.performTagEdit(n, null, false, false);
        waitForPropertyEditor();
        instrumentation.removeMonitor(monitor);
        TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false);
        assertTrue(TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/form_header_cut", false));
        assertTrue(TestUtils.clickHome(device, true));
        TestUtils.zoomToLevel(device, main, 21);
        Node n2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 2205498723L);
        assertNotNull(n2);
        TestUtils.clickAtCoordinates(device, main.getMap(), n2.getLon(), n2.getLat());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(2205498723L, node.getOsmId());
        assertTrue(TestUtils.clickOverflowButton(device));
        monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_paste_tags), false));
        waitForPropertyEditor();
        assertTrue(TestUtils.findText(device, false, "Bergdietikon"));
        assertFalse(n.hasTag(Tags.KEY_NAME, "BergDietikon"));
    }

    /**
     * Select an untagged node, then - apply fastfood preset - select name (McDonalds)
     */
    @Test
    public void nameSuggestion() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        // trying to get node click work properly is frustrating
        // TestUtils.clickAtCoordinates(main.getMap(), 8.3856255, 47.3894333, true);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 599672192L);
        assertNotNull(n);
        final CountDownLatch signal2 = new CountDownLatch(1);
        main.runOnUiThread(() -> {
            main.getEasyEditManager().editElement(n);
            (new SignalHandler(signal2)).onSuccess();
        });
        try {
            signal2.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));

        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_tags), false, true));
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        if (!((PropertyEditorActivity) propertyEditor).usingPaneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Facilities"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Food+Drinks"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Fast Food"), true, false);
        assertTrue(found);
        // set cuisine to something so that we get the dialog
        UiObject2 cusine = null;
        try {
            cusine = getField(device, "Cuisine", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(cusine);
        cusine.click();
        TestUtils.scrollTo("Asian", false);
        assertTrue(TestUtils.clickText(device, true, "Asian", false, false));
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.save), true, false));
        UiObject2 name = null;
        try {
            name = getField(device, "Name", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(name);
        name.click();
        instrumentation.sendStringSync("McD");
        Rect rect = name.getVisibleBounds();
        device.waitForWindowUpdate(null, 1000);
        device.click(rect.left + 64, rect.bottom + 72); // hack alert

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.replace), true));

        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        device.waitForIdle();
        assertTrue(n.hasTag("cuisine", "burger"));
    }

    /**
     * Select two nodes
     */
    @Test
    public void multiSelect() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);

        Node n1 = (Node) App.getDelegator().getOsmElement(Node.NAME, 3465444349L);
        assertNotNull(n1);
        Node n2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 3190098961L);
        assertNotNull(n1);
        final CountDownLatch signal2 = new CountDownLatch(1);
        main.runOnUiThread(() -> {
            logic.addSelectedNode(n1);
            logic.addSelectedNode(n2);
            main.getEasyEditManager().editElements();
            (new SignalHandler(signal2)).onSuccess();
        });
        try {
            signal2.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_multiselect)));

        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_tags), false, true));
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        assertFalse(TestUtils.findText(device, false, context.getString(R.string.tag_details)));

        if (!((PropertyEditorActivity) propertyEditor).usingPaneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        TestUtils.scrollToEnd(false);
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Lifecycle"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Set to disused"), true, false);
        assertTrue(found);

        UiObject2 level = null;
        try {
            level = getField(device, "level", 2);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(level);
        level.setText("1234");

        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_multiselect)));
        device.waitForIdle();

        assertTrue(n1.hasTag("disused:amenity", "toilets"));
        assertTrue(n1.hasTag("level", "1234"));
        assertTrue(n2.hasTag("disused:amenity", "fountain"));
        assertTrue(n2.hasTag("level", "1234"));
    }

    /**
     * Select two nodes
     */
    @Test
    public void multiSelectClearValue() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);

        Node n1 = (Node) App.getDelegator().getOsmElement(Node.NAME, 3465444349L);
        assertNotNull(n1);
        Node n2 = (Node) App.getDelegator().getOsmElement(Node.NAME, 3190098961L);
        assertNotNull(n1);
        final CountDownLatch signal2 = new CountDownLatch(1);
        main.runOnUiThread(() -> {
            logic.addSelectedNode(n1);
            logic.addSelectedNode(n2);
            main.getEasyEditManager().editElements();
            (new SignalHandler(signal2)).onSuccess();
        });
        try {
            signal2.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_multiselect)));

        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_tags), false, true));
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        assertFalse(TestUtils.findText(device, false, context.getString(R.string.tag_details)));

        UiObject2 access = null;
        try {
            access = getField(device, Tags.KEY_ACCESS, 2);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(access);
        access.setText("");

        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_multiselect)));
        device.waitForIdle();

        assertFalse(n1.hasTagKey(Tags.KEY_ACCESS));
        assertFalse(n2.hasTagKey(Tags.KEY_ACCESS));
    }

    /**
     * Clear value in details editor
     */
    @Test
    public void clearValue() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);

        Node n1 = (Node) App.getDelegator().getOsmElement(Node.NAME, 3465444349L);
        assertNotNull(n1);

        final CountDownLatch signal2 = new CountDownLatch(1);
        main.runOnUiThread(() -> {
            logic.addSelectedNode(n1);
            main.getEasyEditManager().editElements();
            (new SignalHandler(signal2)).onSuccess();
        });
        try {
            signal2.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));

        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_tags), false, true));
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        switchToDetailsTab();

        UiObject2 access = null;
        try {
            access = getField(device, Tags.KEY_ACCESS, 2);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(access);
        access.setText("");

        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        device.waitForIdle();

        assertFalse(n1.hasTagKey(Tags.KEY_ACCESS));
    }

    /**
     * Select an untagged node, then - apply restaurant preset
     */
    @Test
    public void mruPreset() {
        PresetItem restaurant = App.getCurrentPresets(main)[0].getItemByName("Restaurant", null);
        MRUTags mruTags = App.getMruTags();
        mruTags.put(restaurant, "cuisine", "greek");
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, new SignalHandler(signal));
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        main.getMap().getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        // trying to get node click work properly is frustrating
        // TestUtils.clickAtCoordinates(main.getMap(), 8.3856255, 47.3894333, true);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 599672192L);
        assertNotNull(n);
        final CountDownLatch signal2 = new CountDownLatch(1);
        main.runOnUiThread(() -> {
            main.getEasyEditManager().editElement(n);
            (new SignalHandler(signal2)).onSuccess();
        });
        try {
            signal2.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        // Node n = App.getLogic().getSelectedNode();
        // assertNotNull(n);

        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_tags), false, true));
        PropertyEditorActivity propertyEditor = waitForPropertyEditor();

        if (!((PropertyEditorActivity) propertyEditor).usingPaneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }

        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Facilities"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName(main, "Food+Drinks"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName(main, "Restaurant"), true, false);
        assertTrue(found);

        TestUtils.scrollToEnd(false);

        assertTrue(TestUtils.findText(device, false, "Restaurant"));
        assertTrue(TestUtils.longClickText(device, "Restaurant"));

        assertTrue(TestUtils.findText(device, false, main.getString(R.string.apply_with_last_values)));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.apply_with_last_values), true));
        assertTrue(TestUtils.clickText(device, false, "Restaurant", false)); // this applies the preset which should set
                                                                             // cuisine to greek

        UiObject2 cusine = null;
        try {
            cusine = getField(device, "Cuisine", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(cusine);

        TestUtils.clickHome(device, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        device.waitForIdle();
        assertTrue(n.hasTag("cuisine", "greek"));
    }

    /**
     * Get the value field for a specific key
     * 
     * @param mDevice the current UiDevice
     * @param text the text display for the key
     * @param fieldIndex the index in to the linear layout holding the row
     * 
     * @return an UiObject2 for the value field
     * @throws UiObjectNotFoundException if we couldn't find the object with text
     */
    public static UiObject2 getField(UiDevice mDevice, @NonNull String text, int fieldIndex) throws UiObjectNotFoundException {
        TestUtils.scrollTo(text, false);
        BySelector bySelector = By.textStartsWith(text);
        UiObject2 keyField = mDevice.wait(Until.findObject(bySelector), 500);
        UiObject2 linearLayout = keyField.getParent();
        if (!linearLayout.getClassName().equals("android.widget.LinearLayout")) {
            // some of the text fields are nested one level deeper
            linearLayout = linearLayout.getParent();
        }
        return linearLayout.getChildren().get(fieldIndex);
    }

    /**
     * Get the value field for a specific key, assuming the values are one level deep in a layout
     * 
     * @param text the text display for the key
     * @param fieldIndex the index of the field
     * @return an UiObject2 for the value field
     * @throws UiObjectNotFoundException if we couldn't find the object with text
     */
    private UiObject2 getField2(@NonNull String text, int fieldIndex) throws UiObjectNotFoundException {
        TestUtils.scrollTo(text, false);
        BySelector bySelector = By.textStartsWith(text);
        UiObject2 keyField = device.wait(Until.findObject(bySelector), 500);
        UiObject2 linearLayout = keyField.getParent();
        if (!linearLayout.getClassName().equals("android.widget.LinearLayout")) {
            // some of the text fields are nested one level deeper
            linearLayout = linearLayout.getParent();
        }
        return linearLayout.getChildren().get(1).getChildren().get(fieldIndex);
    }

    /**
     * Wait for the property editor to start
     * 
     * @return the PropertyEditorActivity instance
     */
    private PropertyEditorActivity waitForPropertyEditor() {
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditorActivity);
        instrumentation.waitForIdleSync();
        return (PropertyEditorActivity) propertyEditor;
    }
}
