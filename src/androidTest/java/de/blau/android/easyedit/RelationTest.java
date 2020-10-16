package de.blau.android.easyedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.propertyeditor.PropertyEditor;
import de.blau.android.propertyeditor.PropertyEditorTest;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RelationTest {

    Context              context         = null;
    AdvancedPrefDatabase prefDB          = null;
    Main                 main            = null;
    UiDevice             device          = null;
    Map                  map             = null;
    Logic                logic           = null;
    Instrumentation      instrumentation = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        TestUtils.removeImageryLayers(context);
        prefs.enableSimpleActions(true);
        main.runOnUiThread(() -> main.showSimpleActionsButton());
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToLevel(device, main, 18);
    }

    /**
     * Select, show info dialog, delete, undelete
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void selectRelation() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        assertTrue(TestUtils.clickText(device, false, "Hiking", false, false));
        List<Relation> rels = App.getLogic().getSelectedRelations();
        assertNotNull(rels);
        assertEquals(1, rels.size());
        Relation relation = rels.get(0);
        List<RelationMember> origMembers = relation.getMembers();
        assertEquals(6490362L, relation.getOsmId());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_relationselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_information), true, false));
        assertTrue(TestUtils.findText(device, false, "hiking"));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.done), true, false));
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.delete), false, true));
        assertEquals(OsmElement.STATE_DELETED, relation.getState());
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, false));
        assertEquals(OsmElement.STATE_UNCHANGED, relation.getState());
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // for the tip alert
        assertNotNull(relation.getMember(Way.NAME, 104148456L));
        List<RelationMember> members = relation.getMembers();
        assertEquals(origMembers.size(), members.size());
        for (int i = 0; i < members.size(); i++) {
            assertEquals(origMembers.get(i), members.get(i));
        }
    }

    /**
     * Select way, create relation, set tag, delete
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void createRelation() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        assertTrue(TestUtils.findText(device, false, "Path", 2000));
        assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(104148456L, way.getOsmId());
        //
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(context.getString(R.string.menu_relation));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_relation), true, false));
        TestUtils.scrollToEnd();
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.select_relation_type_other), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_add_relation_member)));
        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditor.class.getName(), null, false);
        // finish
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertNotNull(propertyEditor);
        TestUtils.sleep(5000);

        assertTrue(TestUtils.findText(device, false, "Relation type"));
        UiObject2 relationType = null;
        try {
            relationType = PropertyEditorTest.getField(device, "Relation type", 1);
        } catch (UiObjectNotFoundException e) {
            fail();
        }
        assertNotNull(relationType);
        relationType.click(); // NOSONAR
        relationType.setText(Tags.VALUE_MULTIPOLYGON); // can't find text in drop downs
        TestUtils.clickHome(device, true);
        List<Relation> relations = App.getLogic().getSelectedRelations();
        assertEquals(1, relations.size());
        Relation relation = relations.get(0);
        assertTrue(relation.getOsmId() < 0);
        assertTrue(relation.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON));
        assertTrue(TestUtils.clickMenuButton(device, "Delete", false, true));
    }

    /**
     * Create way set it to platform, create stop_area relation, check role of way in relation
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void createRelationAssignRole() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_way), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.simple_add_way)));
        TestUtils.clickAtCoordinates(device, map, 8.3893454, 47.3901898, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_createpath), 1000));
        TestUtils.clickAtCoordinates(device, map, 8.3895763, 47.3901374, true);
        TestUtils.sleep();
        TestUtils.clickAtCoordinates(device, map, 8.3896274, 47.3902424, true);
        TestUtils.clickUp(device);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_form_untagged_element)));
        TestUtils.clickHome(device, true);
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        java.util.Map<String, String> tags = new TreeMap<>();
        tags.put("public_transport", "platform");
        logic.setTags(main, way, tags);
        //
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(context.getString(R.string.menu_relation));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_relation), true, false));
        TestUtils.scrollTo("Stop Area");
        assertTrue(TestUtils.clickText(device, false, "Stop Area", true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_add_relation_member)));
        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditor.class.getName(), null, false);
        // finish
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertNotNull(propertyEditor);
        TestUtils.sleep(5000);
        TestUtils.clickHome(device, true);
        List<Relation> relations = App.getLogic().getSelectedRelations();
        assertEquals(1, relations.size());
        Relation relation = relations.get(0);
        assertTrue(relation.getOsmId() < 0);
        assertTrue(relation.hasTag(Tags.KEY_TYPE, "public_transport"));
        assertTrue(relation.hasTag("public_transport", "stop_area"));
        List<RelationMember> members = relation.getMembers();
        assertNotNull(members);
        assertEquals(1, members.size());
        RelationMember member = members.get(0);
        assertEquals("platform", member.getRole());
        assertEquals(way, member.getElement());
    }
}
