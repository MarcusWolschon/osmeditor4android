package io.vespucci.easyedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.osm.Node;
import io.vespucci.osm.Relation;
import io.vespucci.osm.Way;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.propertyeditor.PropertyEditorActivity;

/**
 * 1st attempts at testing lifecycle related aspects in easyedit modes
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SaveResumeTest {

    Context                 context  = null;
    AdvancedPrefDatabase    prefDB   = null;
    Main                    main     = null;
    UiDevice                device   = null;
    Map                     map      = null;
    Logic                   logic    = null;
    private Instrumentation instrumentation;
    ActivityScenario<Main>  scenario = null;

    @Rule
    public ActivityScenarioRule<Main> activityScenarioRule = new ActivityScenarioRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        scenario = ActivityScenario.launch(Main.class);
        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        instrumentation.removeMonitor(monitor);
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        prefs.enableSimpleActions(true);
        main.runOnUiThread(() -> main.showSimpleActionsButton());

        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        App.getTaskStorage().reset();
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
        App.getTaskStorage().reset();
        scenario.moveToState(State.DESTROYED);
    }

    /**
     * Select way, create route, add a further segment - restart app
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void createRoute() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickText(device, false, "↓ Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(104148456L, way.getOsmId());
        //
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_create_route), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_add_segment)));

        // add 50059937
        Way way2 = (Way) App.getDelegator().getOsmElement(Way.NAME, 50059937L);
        assertNotNull(way2);
        Node way2Node = way2.getNodes().get(2);
        TestUtils.clickAtCoordinates(device, map, way2Node.getLon(), way2Node.getLat(), true);
        TestUtils.sleep();
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_add_segment)));

        scenario.recreate();

        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_add_segment), 10000));

        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        // finish
        clickSimpleButton(device);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertNotNull(propertyEditor);
        TestUtils.sleep(2000);
        TestUtils.clickText(device, false, context.getString(R.string.cancel), true, false);
        assertTrue(TestUtils.clickHome(device, true));
        instrumentation.removeMonitor(monitor);

        List<Relation> rels = logic.getSelectedRelations();
        assertNotNull(rels);
        assertEquals(1, rels.size());
        final Relation route = rels.get(0);
        assertEquals(2, route.getMembers().size());
        assertNotNull(route.getMember(Way.NAME, 104148456L));
        assertNotNull(route.getMember(Way.NAME, 50059937L));
    }

    /**
     * Click the simple button
     * 
     * @param device the UiDevice
     */
    public void clickSimpleButton(@NonNull UiDevice device) {
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
    }

    /**
     * Select way, create reation - restart app
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void createRelation() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        TestUtils.clickText(device, false, "↓ Path", false, false);
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertEquals(104148456L, way.getOsmId());
        //
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(context.getString(R.string.menu_relation), false);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_relation), true, false));
        TestUtils.scrollToEnd(true);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.select_relation_type_other), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_add_relation_member)));
        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);

        scenario.recreate();

        clickSimpleButton(device);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertNotNull(propertyEditor);
        TestUtils.sleep(2000);
        TestUtils.clickText(device, false, context.getString(R.string.cancel), true, false);
        TestUtils.clickHome(device, true);
        List<Relation> relations = App.getLogic().getSelectedRelations();
        assertEquals(1, relations.size());
        Relation relation = relations.get(0);
        assertTrue(relation.getOsmId() < 0);
    }

    /**
     * Create a new way from menu
     */
    @Test
    public void createWay() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.clickSimpleButton(device);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_way), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_way_start_instruction)));
        TestUtils.clickAtCoordinates(device, map, 8.3893454, 47.3901898, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_way_node_instruction), 1000));
        TestUtils.clickAtCoordinates(device, map, 8.3895763, 47.3901374, true);
        TestUtils.sleep();
        TestUtils.clickAtCoordinates(device, map, 8.3896274, 47.3902424, true);
        TestUtils.sleep();
        TestUtils.clickAtCoordinates(device, map, 8.3897000, 47.3903500, true);
        TestUtils.sleep();

        scenario.recreate();

        clickSimpleButton(device);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_form_untagged_element)));
        TestUtils.clickHome(device, true);
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertTrue(way.getOsmId() < 0);
        assertEquals(4, way.nodeCount());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        TestUtils.clickUp(device);
    }

}
