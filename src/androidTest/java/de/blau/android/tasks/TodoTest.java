package de.blau.android.tasks;

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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TodoTest {

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
        TestUtils.stopEasyEdit(main);
        TestUtils.unlock(device);
        map.getViewBox().fitToBoundingBox(map, map.getDataLayer().getExtent());
        logic.updateStyle();
        map.getDataLayer().setVisible(true);
        map.invalidate();
        device.waitForWindowUpdate(null, 2000);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
    }

    /**
     * Select, add to Todo, then close
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void addAndCloseTodo() {
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, true);
        assertTrue(TestUtils.clickText(device, false, "Toilets", true, false, 5000));
        Node node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(3465444349L, node.getOsmId());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(context.getString(R.string.menu_add_to_todo), false);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_to_todo), true, false));
        assertTrue(TestUtils.findText(device, false, context.getResources().getQuantityString(R.plurals.add_todo_title, 1)));
        UiObject todoList = TestUtils.findObjectWithResourceId(device, device.getCurrentPackageName() + ":id/todoList", 0);
        try {
            todoList.click();
            todoList.setText("Test ToDo list");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        // assertTrue(TestUtils.clickText(device, false, context.getString(R.string.add), false, false));
        assertTrue(TestUtils.clickResource(device, false, "android:id/button1", false));
        List<Todo> todos = App.getTaskStorage().getTodosForElement(node);
        assertEquals(1, todos.size());
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_todo), false, false));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_todo_close_and_next), true, false));
        assertTrue(todos.get(0).isClosed());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.all_todos_done_title)));
        // assertTrue(TestUtils.clickText(device, false, context.getString(R.string.delete), true, false));
        assertTrue(TestUtils.clickResource(device, false, "android:id/button1", false));
        assertTrue(App.getTaskStorage().getTodosForElement(node).isEmpty());
    }
}
