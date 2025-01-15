package de.blau.android.layer.streetlevel.mapillary;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.MockTileServer;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.dialogs.DateRangeDialog;
import de.blau.android.layer.LayerDialogTest;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.streetlevel.ImageViewerActivity;
import de.blau.android.layer.streetlevel.mapillary.MapillaryOverlay;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource.TileType;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MapillaryTest {

    AdvancedPrefDatabase prefDB            = null;
    Main                 main              = null;
    UiDevice             device            = null;
    Map                  map               = null;
    Logic                logic             = null;
    Instrumentation      instrumentation   = null;
    MockWebServerPlus    mockApiServer     = null;
    MockWebServerPlus    mockImagesServer  = null;
    MockWebServer        tileServer        = null;
    HttpUrl              mockImagesBaseUrl = null;

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

        assertNotNull(main);
        TestUtils.grantPermissons(device);
        LayerUtils.removeLayer(main, LayerType.MAPILLARY);
        tileServer = MockTileServer.setupTileServer(main, "mapillary.mbt", true, LayerType.MAPILLARY, TileType.MVT,
                de.blau.android.layer.streetlevel.mapillary.MapillaryOverlay.MAPILLARY_TILES_ID);

        mockApiServer = new MockWebServerPlus();
        HttpUrl mockApiBaseUrl = mockApiServer.server().url("/");
        Preferences prefs = new Preferences(main);
        prefs.setMapillarySequencseUrlV4(mockApiBaseUrl.toString());
        prefs.setMapillaryImagesUrlV4(mockApiBaseUrl.toString());

        mockImagesServer = new MockWebServerPlus();
        mockImagesBaseUrl = mockImagesServer.server().url("/");

        App.getLogic().setPrefs(prefs);
        map = main.getMap();
        map.setPrefs(main, prefs);

        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.loadTestData(main, "test2.osm");
        map.getDataLayer().setVisible(true);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            mockApiServer.server().shutdown();
            mockImagesServer.server().shutdown();
        } catch (IOException e) {
            // Ignore
        }
        try {
            tileServer.close();
        } catch (IOException | NullPointerException e) {
            // ignore
        }
        LayerUtils.removeLayer(main, LayerType.MAPILLARY);
        try (TileLayerDatabase tlDb = new TileLayerDatabase(main); SQLiteDatabase db = tlDb.getWritableDatabase()) {
            TileLayerDatabase.deleteLayerWithId(db, de.blau.android.layer.streetlevel.mapillary.MapillaryOverlay.MAPILLARY_TILES_ID);
        }
        instrumentation.waitForIdleSync();
    }

    /**
     * Add mapillary layer and click on one image
     */
    @Test
    public void mapillaryLayer() {
        mockApiServer.enqueue("mapillary_sequences");
        MockResponse imageResponse = new MockResponse();
        imageResponse.setResponseCode(200);
        imageResponse.setBody("{\"thumb_2048_url\": \"" + mockImagesBaseUrl.toString() + "\",\"computed_geometry\": {\"type\": \"Point\",\"coordinates\": ["
                + "8.407748800863,47.412813485744]" + "},\"id\": \"178993950747668\"}");
        de.blau.android.layer.streetlevel.mapillary.MapillaryOverlay layer = (MapillaryOverlay) map.getLayer(LayerType.MAPILLARY);
        assertNotNull(layer);
        layer.flushCaches(main); // forces the layer to retrieve everything

        mockApiServer.enqueue(imageResponse);
        mockApiServer.enqueue(imageResponse);
        mockApiServer.enqueue(imageResponse);
        mockApiServer.enqueue(imageResponse);
        mockApiServer.enqueue(imageResponse);
        MockResponse image = TestUtils.createBinaryReponse("image/jpeg", "fixtures/mapillary_image_v4.jpg");
        mockImagesServer.server().enqueue(image);
        mockImagesServer.server().enqueue(image);
        mockImagesServer.server().enqueue(image);
        mockImagesServer.server().enqueue(image);
        mockImagesServer.server().enqueue(image);

        TestUtils.unlock(device);
        TestUtils.sleep();

        ActivityMonitor monitor = instrumentation.addMonitor(ImageViewerActivity.class.getName(), null, false);
        // hack around slow rendering on some emulators
        map.getViewBox().moveTo(map, (int) (8.407748800863 * 1E7), (int) (47.412813485744 * 1E7));
        map.invalidate();
        TestUtils.zoomToLevel(device, main, 22);
        TestUtils.clickAtCoordinates(device, map, 8.407748800863, 47.412813485744, true);
        if (TestUtils.clickText(device, false, "OK", true)) {
            TestUtils.clickAtCoordinates(device, map, 8.407748800863, 47.412813485744, true);
        }
        ImageViewerActivity viewer = null;
        try {
            viewer = (ImageViewerActivity) instrumentation.waitForMonitorWithTimeout(monitor, 30000);
            assertNotNull(viewer);
            try {
                RecordedRequest recorded = mockImagesServer.server().takeRequest(10, TimeUnit.SECONDS);
                assertNotNull(recorded);
                System.out.println(recorded.getPath());
                mockImagesServer.server().takeRequest(10, TimeUnit.SECONDS);
                mockImagesServer.server().takeRequest(10, TimeUnit.SECONDS);
                mockImagesServer.server().takeRequest(10, TimeUnit.SECONDS);
                mockImagesServer.server().takeRequest(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.share), false, true));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertTrue(TestUtils.findText(device, false, "178993950747668"));
            } else { // currently can't test this properly on Android before 10
                assertTrue(TestUtils.findText(device, false, "Share with"));
            }
            device.pressBack();
        } finally {
            instrumentation.removeMonitor(monitor);
            if (viewer != null) {
                viewer.finish();
            }
        }
    }

    /**
     * Add mapillary layer and click on one image that should be filtered away
     * 
     * Unluckily there doesn't seem to be an easy way to drag the sliders
     */
    @Test
    public void mapillaryLayerFilter() {
        TestUtils.unlock(device);
        TestUtils.sleep();

        UiObject2 menuButton = TestUtils.getLayerButton(device, "Mapillary", LayerDialogTest.MENU_BUTTON);
        menuButton.click();
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_set_date_range), true, false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.date_range_title)));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.Done), true, false));
        MapillaryOverlay layer = (MapillaryOverlay) map.getLayer(LayerType.MAPILLARY);
        layer.setDateRange(0L, 0L);
        layer.invalidate();
        //
        map.getViewBox().moveTo(map, (int) (8.407748800863 * 1E7), (int) (47.412813485744 * 1E7));
        map.invalidate();
        TestUtils.zoomToLevel(device, main, 22);
        TestUtils.clickAtCoordinates(device, map, 8.407748800863, 47.412813485744, true); // nothing should happen
        if (TestUtils.clickText(device, false, "OK", true)) {
            TestUtils.clickAtCoordinates(device, map, 8.407748800863, 47.412813485744, true);
        }
        assertFalse(TestUtils.clickMenuButton(device, main.getString(R.string.share), false, true));
    }

}
