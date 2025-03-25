package io.vespucci.imageryoffset;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.MockTileServer;
import io.vespucci.TestUtils;
import io.vespucci.exception.OsmException;
import io.vespucci.layer.LayerDialogTest;
import io.vespucci.osm.BoundingBox;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.util.GeoMath;
import okhttp3.mockwebserver.MockWebServer;

/**
 * 1st attempts at testing lifecycle related aspects
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SaveResumeTest {

    Context                context    = null;
    AdvancedPrefDatabase   prefDB     = null;
    Main                   main       = null;
    UiDevice               device     = null;
    Map                    map        = null;
    Logic                  logic      = null;
    MockWebServer          tileServer = null;
    ActivityScenario<Main> scenario   = null;

    @Rule
    public ActivityScenarioRule<Main> activityScenarioRule = new ActivityScenarioRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        scenario = ActivityScenario.launch(Main.class);
        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        instrumentation.removeMonitor(monitor);
        Preferences prefs = new Preferences(context);

        tileServer = MockTileServer.setupTileServer(main, "ersatz_background.mbt", true);

        map = main.getMap();
        map.setPrefs(main, prefs);
        logic = App.getLogic();
        logic.setPrefs(prefs);
        TestUtils.resetOffsets(main.getMap());
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
        main.invalidateOptionsMenu(); // to be sure that the menu entry is actually shown
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            tileServer.close();
        } catch (IOException e) {
            // ignore
        }

        scenario.moveToState(State.DESTROYED);
    }

    /**
     * Start the alignment mode then recreate
     */
    @Test
    public void startResumeMode() {
        TestUtils.zoomToLevel(device, main, 18);
        try {
            BoundingBox bbox = GeoMath.createBoundingBoxForCoordinates(47.390339D, 8.38782D, 50D);
            App.getLogic().getViewBox().setBorders(map, bbox);
            map.setViewBox(App.getLogic().getViewBox());
            map.invalidate();
            try {
                Thread.sleep(5000); // NOSONAR
            } catch (InterruptedException e) {
            }
            main.invalidateOptionsMenu();
        } catch (OsmException e) {
            fail(e.getMessage());
        }
        UiObject2 menuButton = TestUtils.getLayerButton(device, "Vespucci Test", LayerDialogTest.MENU_BUTTON);
        menuButton.click();
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_layers_background_align), true, false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.menu_tools_background_align)));
        scenario.recreate();
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.menu_tools_background_align), 10000));
    }
}
