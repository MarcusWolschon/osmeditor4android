package io.vespucci.imageryoffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.MockTileServer;
import io.vespucci.Mode;
import io.vespucci.TestUtils;
import io.vespucci.exception.OsmException;
import io.vespucci.imageryoffset.Offset;
import io.vespucci.layer.LayerDialogTest;
import io.vespucci.osm.BoundingBox;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.TileLayerDatabase;
import io.vespucci.resources.TileLayerSource;
import io.vespucci.util.GeoMath;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OffsetModeTest {

    public static final int TIMEOUT         = 90;
    Main                    main            = null;
    UiDevice                device          = null;
    Instrumentation         instrumentation = null;
    MockWebServerPlus       mockServer      = null;
    MockWebServer           tileServer      = null;
    Preferences             prefs           = null;
    Logic                   logic           = null;
    Map                     map             = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();

        device = UiDevice.getInstance(instrumentation);
        main = mActivityRule.getActivity();

        TestUtils.grantPermissons(device);

        prefs = new Preferences(main);

        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/");
        prefs.setOffsetServer(mockBaseUrl.toString());

        tileServer = MockTileServer.setupTileServer(main, "ersatz_background.mbt", true);

        map = main.getMap();
        map.setPrefs(main, prefs);
        logic = App.getLogic();
        logic.setPrefs(prefs);
        TestUtils.resetOffsets(main.getMap());
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
        if (main != null) {
            TestUtils.zoomToNullIsland(logic, map);
            TestUtils.resetOffsets(main.getMap());
            logic.setMode(main, Mode.MODE_EASYEDIT);
            main.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
            main.finish();
        } else {
            System.out.println("main is null");
        }
        try {
            tileServer.close();
        } catch (IOException e) {
            // ignore
        }
        instrumentation.waitForIdleSync();
    }

    /**
     * Start offset mode and drag the screen
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void createOffset() {
        startMode();
        TileLayerSource tileLayerConfiguration = map.getBackgroundLayer().getTileLayerConfiguration();
        tileLayerConfiguration.setOffset(0, 0);
        TestUtils.zoomToLevel(device, main, tileLayerConfiguration.getMaxZoom());
        int zoomLevel = map.getZoomLevel();
        Offset offset = tileLayerConfiguration.getOffset(zoomLevel);
        assertEquals(0D, offset.getDeltaLat(), 0.1E-4);
        assertEquals(0D, offset.getDeltaLon(), 0.1E-4);
        TestUtils.drag(device, map, 8.38782, 47.390339, 8.388, 47.391, true, 50);
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, main.getString(R.string.menu_tools_background_align_save_db), true, false);
        // 74.22 m
        TestUtils.clickText(device, false, main.getString(R.string.cancel), true, false);
        TestUtils.clickOverflowButton(device);
        TestUtils.clickText(device, false, main.getString(R.string.apply), true, false);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        try {
            Thread.sleep(5000); // NOSONAR
        } catch (InterruptedException e) {
        }
        zoomLevel = map.getZoomLevel();
        offset = tileLayerConfiguration.getOffset(zoomLevel);
        assertNotNull(offset);
        assertEquals(6.462E-4, offset.getDeltaLat(), 0.1E-4);
        assertEquals(1.773E-4, offset.getDeltaLon(), 0.1E-4);
    }

    /**
     * Zoom in and start the alignment mode
     */
    private void startMode() {
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
    }

    /**
     * Start offset mode and download a offset
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void downloadOffset() {
        mockServer.enqueue("imagery_offset");
        startMode();
        TileLayerSource tileLayerConfiguration = map.getBackgroundLayer().getTileLayerConfiguration();
        TestUtils.zoomToLevel(device, main, tileLayerConfiguration.getMaxZoom());
        int zoomLevel = map.getZoomLevel();
        TestUtils.clickMenuButton(device, main.getString(R.string.menu_tools_background_align_retrieve_from_db), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.apply), true, false);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
        try {
            Thread.sleep(5000); // NOSONAR
        } catch (InterruptedException e) {
        }
        zoomLevel = map.getZoomLevel();
        Offset offset = tileLayerConfiguration.getOffset(zoomLevel);
        assertNotNull(offset);
        assertEquals(8.7E-6, offset.getDeltaLat(), 0.1E-6);
        assertEquals(-1.056E-5, offset.getDeltaLon(), 0.01E-5);
    }

    /**
     * Start offset mode and drag the screen, abort
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void abortOffset() {
        startMode();
        TileLayerSource tileLayerConfiguration = map.getBackgroundLayer().getTileLayerConfiguration();
        tileLayerConfiguration.setOffset(0, 0);
        TestUtils.zoomToLevel(device, main, tileLayerConfiguration.getMaxZoom());
        int zoomLevel = map.getZoomLevel();
        Offset offset = tileLayerConfiguration.getOffset(zoomLevel);
        assertEquals(0D, offset.getDeltaLat(), 0.1E-4);
        assertEquals(0D, offset.getDeltaLon(), 0.1E-4);
        TestUtils.drag(device, map, 8.38782, 47.390339, 8.388, 47.391, true, 50);
        TestUtils.clickUp(device);
        TestUtils.clickText(device, false, main.getString(R.string.yes), true);
        try {
            Thread.sleep(5000); // NOSONAR
        } catch (InterruptedException e) {
        }
        zoomLevel = map.getZoomLevel();
        offset = tileLayerConfiguration.getOffset(zoomLevel);
        assertEquals(0D, offset.getDeltaLat(), 0.1E-4);
        assertEquals(0D, offset.getDeltaLon(), 0.1E-4);
    }
}
