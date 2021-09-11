package de.blau.android;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.propertyeditor.PropertyEditor;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class KeyboardTest {

    private static final String WAY_SELECTED = "Way selected";
    Context                     context      = null;
    AdvancedPrefDatabase        prefDB       = null;
    Main                        main         = null;
    UiDevice                    device       = null;
    Map                         map          = null;
    Logic                       logic        = null;
    private Instrumentation     instrumentation;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        main = mActivityRule.getActivity();
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", "", null, null, "user", "pass", false);
        prefDB.selectAPI("Test");
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
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToLevel(device, main, 18);
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
    }

    /**
     * Select, show info dialog, delete (check that nodes are deleted), undelete
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void zoomAndPan() {
        map.getDataLayer().setVisible(true);

        TestUtils.zoomToLevel(device, main, 20);

        ViewBox viewBox = new ViewBox(App.getLogic().getViewBox());

        device.pressKeyCode(KeyEvent.KEYCODE_PLUS);
        TestUtils.sleep();
        Assert.assertNotEquals(viewBox, App.getLogic().getViewBox());

        device.pressKeyCode(KeyEvent.KEYCODE_MINUS);
        TestUtils.sleep();
        boxIsEqual(viewBox, App.getLogic().getViewBox());

        device.pressKeyCode(KeyEvent.KEYCODE_VOLUME_DOWN);
        TestUtils.sleep();
        Assert.assertNotEquals(viewBox, App.getLogic().getViewBox());

        device.pressKeyCode(KeyEvent.KEYCODE_VOLUME_UP);
        TestUtils.sleep();
        boxIsEqual(viewBox, App.getLogic().getViewBox());

        device.pressDPadRight();
        TestUtils.sleep();
        Assert.assertNotEquals(viewBox, App.getLogic().getViewBox());
        device.pressDPadLeft();
        TestUtils.sleep();
        boxIsEqual(viewBox, App.getLogic().getViewBox());

        device.pressDPadLeft();
        TestUtils.sleep();
        Assert.assertNotEquals(viewBox, App.getLogic().getViewBox());
        device.pressDPadRight();
        TestUtils.sleep();
        boxIsEqual(viewBox, App.getLogic().getViewBox());

        device.pressDPadUp();
        TestUtils.sleep();
        Assert.assertNotEquals(viewBox, App.getLogic().getViewBox());
        device.pressDPadDown();
        TestUtils.sleep();
        boxIsEqual(viewBox, App.getLogic().getViewBox());

        device.pressDPadDown();
        TestUtils.sleep();
        Assert.assertNotEquals(viewBox, App.getLogic().getViewBox());
        device.pressDPadUp();
        TestUtils.sleep();
        boxIsEqual(viewBox, App.getLogic().getViewBox());
    }

    /**
     * Select, start property editor, delete, undelete, re-select, copy, paste, start help
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void way() {

        map.getDataLayer().setVisible(true);

        TestUtils.zoomToLevel(device, main, 21);

        TestUtils.unlock(device);

        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        Assert.assertNotNull(way);
        Assert.assertEquals(104148456L, way.getOsmId());
        Assert.assertTrue(TestUtils.findText(device, false, WAY_SELECTED));

        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditor.class.getName(), null, false);

        device.pressKeyCode(KeyEvent.KEYCODE_E, KeyEvent.META_CTRL_ON);

        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        Assert.assertTrue(propertyEditor instanceof PropertyEditor);
        instrumentation.removeMonitor(monitor);

        Assert.assertTrue(TestUtils.clickHome(device, true));
        Assert.assertTrue(TestUtils.findText(device, false, WAY_SELECTED));

        device.pressKeyCode(KeyEvent.KEYCODE_R, KeyEvent.META_CTRL_ON);
        Assert.assertTrue(TestUtils.textGone(device, WAY_SELECTED, 1000));

        Assert.assertTrue(TestUtils.clickText(device, false, "Delete Way", true));

        Assert.assertEquals(OsmElement.STATE_DELETED, way.getState());
        device.pressKeyCode(KeyEvent.KEYCODE_U, KeyEvent.META_CTRL_ON);
        TestUtils.sleep();
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, way.getState());
        TestUtils.clickText(device, false, "Ok", false); // in case we get a tip

        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        way = App.getLogic().getSelectedWay();
        Assert.assertNotNull(way);
        Assert.assertEquals(104148456L, way.getOsmId());
        Assert.assertTrue(TestUtils.findText(device, false, WAY_SELECTED));

        device.pressKeyCode(KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON);
        TestUtils.sleep();
        device.pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON);
        way = App.getLogic().getSelectedWay();
        Assert.assertNotNull(way);
        Assert.assertTrue(way.getOsmId() < 0);

        monitor = instrumentation.addMonitor(HelpViewer.class.getName(), null, false);
        device.pressKeyCode(KeyEvent.KEYCODE_H, KeyEvent.META_CTRL_ON);
        Activity helpViewer = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        Assert.assertTrue(helpViewer instanceof HelpViewer);
        instrumentation.removeMonitor(monitor);
        Assert.assertTrue(TestUtils.findText(device, false, WAY_SELECTED));
        device.pressBack();
        Assert.assertTrue(TestUtils.findText(device, false, WAY_SELECTED));
        TestUtils.clickUp(device);
    }

    /**
     * Start help on the map display, beep
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void help() {
        TestUtils.zoomToLevel(device, main, 21); // delays things a bit
        ActivityMonitor monitor = instrumentation.addMonitor(HelpViewer.class.getName(), null, false);
        device.pressKeyCode(KeyEvent.KEYCODE_H, KeyEvent.META_CTRL_ON);
        Activity helpViewer = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        Assert.assertTrue(helpViewer instanceof HelpViewer);
        instrumentation.removeMonitor(monitor);
        Assert.assertTrue(TestUtils.findText(device, false, "Main Vespucci Screen", 2000));
        device.pressBack();
        device.pressKeyCode(KeyEvent.KEYCODE_X, KeyEvent.META_CTRL_ON);
    }

    /**
     * Check that two ViewBoxes have the same coordinates with a bit of tolerance
     * 
     * @param box first box
     * @param box2 second box
     */
    private void boxIsEqual(@NonNull ViewBox box, @NonNull ViewBox box2) {
        Assert.assertEquals(box.getLeft(), box2.getLeft(), 10);
        Assert.assertEquals(box.getRight(), box2.getRight(), 10);
        Assert.assertEquals(box.getBottom(), box2.getBottom(), 10);
        Assert.assertEquals(box.getTop(), box2.getTop(), 10);
    }
}
