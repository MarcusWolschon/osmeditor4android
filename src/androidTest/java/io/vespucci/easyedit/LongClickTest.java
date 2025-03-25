package io.vespucci.easyedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.osm.Node;
import io.vespucci.osm.Way;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.tasks.Note;
import io.vespucci.tasks.Task;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LongClickTest {

    Context              context = null;
    AdvancedPrefDatabase prefDB  = null;
    Main                 main    = null;
    UiDevice             device  = null;
    Map                  map     = null;
    Logic                logic   = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        App.getTaskStorage().reset();
        TestUtils.stopEasyEdit(main);
        TestUtils.switchSimpleMode(device, main, false);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
        TestUtils.clickOverflowButton(device);
        TestUtils.switchSimpleMode(device, main, true);
        App.getTaskStorage().reset();
    }

    /**
     * Create a new Node by long click plus re-click
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void newNode() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.longClickAtCoordinates(device, map, 8.3893454, 47.3901898, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_add)));
        TestUtils.clickAtCoordinates(device, map, 8.3893454, 47.3901898, false);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_form_untagged_element)));
        TestUtils.clickHome(device, true);
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertTrue(node.getOsmId() < 0);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        TestUtils.clickUp(device);
    }

    /**
     * Create a new way from long click and clicks at two more locations and finishing via home button
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void newWay() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.longClickAtCoordinates(device, map, 8.3893454, 47.3901898, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_add)));
        TestUtils.clickAtCoordinates(device, map, 8.3895763, 47.3901374, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_way_node_instruction)));
        TestUtils.clickAtCoordinates(device, map, 8.3896274, 47.3902424, true);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_form_untagged_element)));
        TestUtils.clickHome(device, true);
        Way way = App.getLogic().getSelectedWay();
        assertNotNull(way);
        assertTrue(way.getOsmId() < 0);
        assertEquals(3, way.nodeCount());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        TestUtils.clickUp(device);
    }

    /**
     * Create a new Note from long click
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void newBug() {
        LayerUtils.addTaskLayer(main);
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.longClickAtCoordinates(device, map, 8.3890736, 47.3896628, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_add)));
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.openstreetbug_new_bug), false, true));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.openstreetbug_new_title)));
        UiObject editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/openstreetbug_comment"));
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText("test");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickText(device, true, context.getString(R.string.Save), true, false));
        List<Task> tasks = App.getTaskStorage().getTasks();
        assertEquals(1, tasks.size());
        Task t = tasks.get(0);
        assertTrue(t instanceof Note);
        assertEquals("test", ((Note) t).getLastComment().getText());
        LayerUtils.removeTaskLayer(main);
    }
}
