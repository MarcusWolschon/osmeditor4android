package de.blau.android.easyedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.layer.LayerType;
import de.blau.android.osm.ApiTest;
import de.blau.android.osm.Node;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.Task;

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
        TestUtils.removeImageryLayers(context);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        App.getTaskStorage().reset();
        TestUtils.stopEasyEdit(main);
        switchSimpleMode(false);
    }

    /**
     * Switch the simple mode checkbox/pref
     * 
     * @param on if true it should be turned on if it is not on, if false turned off
     */
    private void switchSimpleMode(boolean on) {
        if (TestUtils.clickOverflowButton(device)) {
            UiObject2 simpleMode = TestUtils.findObjectWithText(device, false, "Simple mode", 5000);
            if (simpleMode != null) {
                UiObject2 check = simpleMode.getParent().getParent().getChildren().get(1);
                assertTrue(check.isCheckable());
                if ((on && !check.isChecked()) || (!on && check.isChecked())) {
                    check.click();
                } else {
                    device.pressBack();
                }
            } else {
                Log.e("toggleSimpleMode", "Simple mode check not found");
                device.pressBack();
            }
        } else {
            Log.e("toggleSimpleMode", "no overflowbutton");
        }
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToLevel(device, main, 18);
        TestUtils.clickOverflowButton(device);
        switchSimpleMode(true);
        App.getTaskStorage().reset();
    }

    /**
     * Create a new Node by long click plus re-click
     */
    @SdkSuppress(minSdkVersion = 26)
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
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void newWay() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.longClickAtCoordinates(device, map, 8.3893454, 47.3901898, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.menu_add)));
        TestUtils.clickAtCoordinates(device, map, 8.3895763, 47.3901374, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_createpath)));
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
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void newBug() {
        TestUtils.addTaskLayer(main);
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
        assertEquals("test", ((Note) t).getComment());
        TestUtils.removeTaskLayer(main);
    }
}
