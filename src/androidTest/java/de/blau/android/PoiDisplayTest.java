package de.blau.android;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.GeoMath;
import de.blau.android.views.SplitPaneLayout;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PoiDisplayTest {

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
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToLevel(device, main, 19);
        TestUtils.loadTestData(main, "test1.osm");
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(App.getLogic(), map);
        SplitPaneLayout paneLayout = main.findViewById(R.id.pane_layout);
        paneLayout.setSplitterPositionPercent(100);
    }

    /**
     * Open the POI display while locked and click on POI
     */
    @Test
    public void locked() {
        map.getDataLayer().setVisible(true);
        TestUtils.lock(device);

        View mainMap = main.findViewById(R.id.mainMap);
        int[] pos = new int[2];
        mainMap.getLocationOnScreen(pos);
        int h = mainMap.getHeight();
        int w = mainMap.getWidth();
        final int startX = pos[0] + w / 2;
        final int startY = pos[1] + h;
        TestUtils.longClickAt(device, startX, startY);
        TestUtils.drag(device, startX, startY, startX, startY - 200, 100);
        assertTrue(TestUtils.clickText(device, false, "Excrement bags", true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.element_information)));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true));
    }

    /**
     * Open the POI display while unlocked and click on POI
     */
    @Test
    public void unlocked() {
        TestUtils.unlock(device);
        map.getDataLayer().setVisible(true);

        View mainMap = main.findViewById(R.id.mainMap);
        int[] pos = new int[2];
        mainMap.getLocationOnScreen(pos);
        int h = mainMap.getHeight();
        int w = mainMap.getWidth();
        final int startX = pos[0] + w / 2;
        final int startY = pos[1] + h;
        TestUtils.longClickAt(device, startX, startY);
        TestUtils.drag(device, startX, startY, startX, startY - 200, 100);
        TestUtils.unlock(device);
        assertTrue(TestUtils.clickText(device, false, "Excrement bags", true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_nodeselect)));
    }
}
