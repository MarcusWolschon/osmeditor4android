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
import android.view.KeyEvent;
import androidx.annotation.NonNull;
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
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.MRUTags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.resources.DataStyle;
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
        monitor = instrumentation.addMonitor(PropertyEditor.class.getName(), null, false);
        main = (Main) mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        System.out.println("mock api url " + mockBaseUrl.toString());
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", false);
        prefDB.selectAPI("Test");
        prefDB.resetCurrentServer();
        prefs = new Preferences(context);
        App.getLogic().setPrefs(prefs);
        System.out.println(prefs.getServer().getReadWriteUrl());
        device = UiDevice.getInstance(instrumentation);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
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
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);
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
     * Add a tag and relation membership to a new node
     */
    @Test
    public void newNode() {
        Logic logic = App.getLogic();
        Map map = main.getMap();
        logic.setZoom(map, 20);
        float tolerance = DataStyle.getCurrent().getWayToleranceValue();
        System.out.println("Tolerance " + tolerance);

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
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);
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
        assertTrue(TestUtils.clickText(device, false, "Add to relation", true));
        assertTrue(TestUtils.clickText(device, false, "test", true));

        TestUtils.clickHome(device, true);
        assertTrue(n.hasTag("key", "value"));
        List<Relation> parents = n.getParentRelations();
        assertNotNull(parents);
        assertEquals(1, parents.size());
        assertEquals(r, parents.get(0));
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
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);

        if (!((PropertyEditor) propertyEditor).paneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName("Facilities"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName("Food+Drinks"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName("Restaurant"), true, false);
        assertTrue(found);
        UiObject2 cusine = null;
        try {
            cusine = getField(device, "Cuisine", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(cusine);
        cusine.click();
        TestUtils.scrollTo("Asian");
        assertTrue(TestUtils.clickText(device, true, "Asian", false, false));
        TestUtils.scrollTo("German");
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

        TestUtils.scrollTo("Contact");
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
        TestUtils.scrollTo("Toilets");
        UiObject2 toilets = null;
        try {
            toilets = getField(device, "Toilets", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        toilets.click();
        clickOK(); // click away tip

        switchToDetailsTab();
        TestUtils.scrollTo("toilets");
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
        TestUtils.scrollTo("toilets");
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
        TestUtils.scrollTo("Diaper changing (deprecated)");
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
        TestUtils.zoomToLevel(device, main, 22);
        TestUtils.clickAtCoordinates(device, main.getMap(), 8.3848461, 47.3899166, true);
        assertTrue(TestUtils.clickText(device, false, "Kindhauserstrasse", false, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Way w = App.getLogic().getSelectedWay();
        assertNotNull(w);

        assertTrue(TestUtils.clickMenuButton(device, "Properties", false, true));
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);
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
        assertTrue(TestUtils.findText(device, false, "Bus 305"));
        try {
            UiObject2 roleField = getField(device, "Bus 305", 1);
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
        TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, true);
        assertFalse(findRole("platform", w, parents));

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
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);

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
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);

        String tooLongText = "This is a very long text string to test the that the API limit of 255 characters is enforced by the PropertyEditor by truncating and showing a toast."
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
        TestUtils.scrollTo("from");
        
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
        TestUtils.scrollTo("stop");
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
     * @param name the group name
     * @return the translated name
     */
    String getTranslatedPresetGroupName(String name) {
        String result = null;
        Preset[] presets = App.getCurrentPresets(main);
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
     * @param name the item name
     * @return the translated name
     */
    String getTranslatedPresetItemName(String name) {
        String result = null;
        Preset[] presets = App.getCurrentPresets(main);
        for (Preset p : presets) {
            if (p != null) {
                PresetItem item = p.getItemByName(name);
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
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);

        if (!((PropertyEditor) propertyEditor).paneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName("Highways"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName("Streets"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName("Motorway"), true, false);
        assertTrue(found);
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
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);

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
        assertEquals("McDonald's", n.getTagWithKey(Tags.KEY_NAME));
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
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);

        if (!((PropertyEditor) propertyEditor).paneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName("Highways"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName("Ways"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName("Steps"), true, false);
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
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);

        if (!((PropertyEditor) propertyEditor).paneLayout()) {
            assertTrue(TestUtils.clickText(device, true, main.getString(R.string.tag_menu_preset), false, false));
        }
        boolean found = TestUtils.clickText(device, true, getTranslatedPresetGroupName("Highways"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetGroupName("Streets"), true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName("Residential"), true, false);
        assertTrue(found);
        assertTrue(TestUtils.findText(device, false, "Kindhauserstrasse"));
        found = TestUtils.clickText(device, true, getTranslatedPresetItemName("Cond. & direct. max speed"), true, false);
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
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/save", true);
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
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);

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

        if (!((PropertyEditor) propertyEditor).paneLayout()) {
            TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false);
        }
        PresetItem presetItem = ((PropertyEditor) propertyEditor).getBestPreset();
        assertNotNull(presetItem);
        assertEquals("Building", presetItem.getName());
        assertTrue(TestUtils.clickText(device, true, presetItem.getTranslatedName(), false, false)); // building
                                                                                                     // preset
        // should now be
        // added to MRU
    }

    /**
     * Test that keys with empty values get removed
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
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditor);
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
        TestUtils.scrollTo(text);
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
        TestUtils.scrollTo(text);
        BySelector bySelector = By.textStartsWith(text);
        UiObject2 keyField = device.wait(Until.findObject(bySelector), 500);
        UiObject2 linearLayout = keyField.getParent();
        if (!linearLayout.getClassName().equals("android.widget.LinearLayout")) {
            // some of the text fields are nested one level deeper
            linearLayout = linearLayout.getParent();
        }
        return linearLayout.getChildren().get(1).getChildren().get(fieldIndex);
    }
}
