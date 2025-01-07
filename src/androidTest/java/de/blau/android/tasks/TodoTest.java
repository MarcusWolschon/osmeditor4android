package de.blau.android.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Set;

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
import de.blau.android.osm.Node;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TodoTest {

    private static final String TEST_TO_DO_LIST = "Test ToDo list";
    Context                     context         = null;
    AdvancedPrefDatabase        prefDB          = null;
    Main                        main            = null;
    UiDevice                    device          = null;
    Map                         map             = null;
    Logic                       logic           = null;

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
        App.getTaskStorage().reset();
        Preferences prefs = new Preferences(context);
        prefs.setAutolockDelay(300000L);
        Set<String> filter = prefs.taskFilter();
        filter.add(Todo.FILTER_KEY);
        prefs.setTaskFilter(filter);
        LayerUtils.removeImageryLayers(context);
        LayerUtils.addTaskLayer(main);
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
        App.getTaskStorage().reset();
    }

    /**
     * Select, add to Todo, display todo, then close
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void addAndCloseTodo() {
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickTextContains(device, " Toilets", true, 5000));
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
            todoList.setText(TEST_TO_DO_LIST);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        // assertTrue(TestUtils.clickText(device, false, context.getString(R.string.add), false, false));
        assertTrue(TestUtils.clickResource(device, false, "android:id/button1", true));
        List<Todo> todos = App.getTaskStorage().getTodosForElement(node);
        assertEquals(1, todos.size());
        dismissKeyboard();
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, true);
        TestUtils.sleep();
        assertTrue(TestUtils.clickText(device, false, "Todo", true, false, 5000));
        TestUtils.sleep();
        assertTrue(TestUtils.clickResource(device, false, "android:id/button3", false));
        assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.menu_todo), false, false));
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_todo_close_and_next), true, false));
        assertTrue(todos.get(0).isClosed());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.all_todos_done_title)));
        // assertTrue(TestUtils.clickText(device, false, context.getString(R.string.delete), true, false));
        assertTrue(TestUtils.clickResource(device, false, "android:id/button1", false));
        assertTrue(App.getTaskStorage().getTodosForElement(node).isEmpty());
    }

    /**
     * Select, add two nodes to Todo, display todo, then close and next in fragment
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void addAndCloseTodo2() {
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, true);
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // Tip
        assertTrue(TestUtils.clickTextContains(device, " Toilets", true, 5000));
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
            todoList.setText(TEST_TO_DO_LIST);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickResource(device, false, "android:id/button1", true));
        List<Todo> todos = App.getTaskStorage().getTodosForElement(node);
        assertEquals(1, todos.size());
        dismissKeyboard();

        TestUtils.clickAtCoordinates(device, map, 8.3865262, 47.3898401, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(101792984L, node.getOsmId());
        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(context.getString(R.string.menu_add_to_todo), false);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_to_todo), true, false));
        assertTrue(TestUtils.findText(device, false, context.getResources().getQuantityString(R.plurals.add_todo_title, 1)));
        todoList = TestUtils.findObjectWithResourceId(device, device.getCurrentPackageName() + ":id/todoList", 0);
        try {
            todoList.click();
            todoList.setText(TEST_TO_DO_LIST);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickResource(device, false, "android:id/button1", true));
        todos = App.getTaskStorage().getTodos(TEST_TO_DO_LIST, true);
        assertEquals(2, todos.size());
        dismissKeyboard();

        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, true);
        TestUtils.sleep();
        assertTrue(TestUtils.clickText(device, false, "Todo", true, false, 5000));
        TestUtils.sleep();
        assertTrue(TestUtils.clickResource(device, false, "android:id/button1", false));
        assertTrue(todos.get(0).isClosed());
        todos = App.getTaskStorage().getTodos(TEST_TO_DO_LIST, false);
        assertEquals(1, todos.size());
    }

    /**
     * Hack to get rid of keyboard if it is showing
     */
    private void dismissKeyboard() {
        device.pressBack();
        if (TestUtils.findText(device, false, context.getString(R.string.exit_title))) {
            TestUtils.clickResource(device, false, "android:id/button2", true);
            TestUtils.sleep();
        }
    }
}
