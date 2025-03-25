package io.vespucci.layer.streetlevel.panoramax;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.MockTileServer;
import io.vespucci.TestUtils;
import io.vespucci.layer.LayerType;
import io.vespucci.layer.streetlevel.ImageViewerActivity;
import io.vespucci.layer.streetlevel.panoramax.PanoramaxOverlay;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.TileLayerDatabase;
import io.vespucci.resources.TileLayerSource.TileType;
import io.vespucci.util.FileUtil;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PanoramaxTest {

    AdvancedPrefDatabase prefDB          = null;
    Main                 main            = null;
    UiDevice             device          = null;
    Map                  map             = null;
    Logic                logic           = null;
    Instrumentation      instrumentation = null;
    MockWebServerPlus    mockApiServer   = null;
    MockWebServer        tileServer      = null;
    HttpUrl              mockApiBaseUrl  = null;

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
        LayerUtils.removeLayer(main, LayerType.PANORAMAX);
        tileServer = MockTileServer.setupTileServer(main, "panoramax.mbtiles", true, LayerType.PANORAMAX, TileType.MVT,
                io.vespucci.layer.streetlevel.panoramax.PanoramaxOverlay.PANORAMAX_TILES_ID);

        mockApiServer = new MockWebServerPlus();
        mockApiBaseUrl = mockApiServer.server().url("/");
        Preferences prefs = new Preferences(main);
        prefs.setPanoramaxApiUrl(mockApiBaseUrl.toString());

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
        } catch (IOException e) {
            // Ignore
        }
        try {
            tileServer.close();
        } catch (IOException | NullPointerException e) {
            // ignore
        }
        LayerUtils.removeLayer(main, LayerType.PANORAMAX);
        try (TileLayerDatabase tlDb = new TileLayerDatabase(main); SQLiteDatabase db = tlDb.getWritableDatabase()) {
            TileLayerDatabase.deleteLayerWithId(db, io.vespucci.layer.streetlevel.panoramax.PanoramaxOverlay.PANORAMAX_TILES_ID);
        }
        instrumentation.waitForIdleSync();
    }

    /**
     * Add Panoramax layer and click on one image
     */
    @Test
    public void panoramaxLayer() {
        MockResponse response = new MockResponse();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = loader.getResourceAsStream("fixtures/panoramax_sequences.json");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String sequenceJson = FileUtil.readToString(reader);
            sequenceJson = sequenceJson.replaceAll("https\\://panoramax.openstreetmap.fr/", mockApiBaseUrl.toString());
            Buffer buffer = new Buffer();
            buffer.write(sequenceJson.getBytes());
            response.setBody(buffer);
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        }
        mockApiServer.server().enqueue(response);
        io.vespucci.layer.streetlevel.panoramax.PanoramaxOverlay layer = (PanoramaxOverlay) map.getLayer(LayerType.PANORAMAX);
        layer.setVisible(true);
        assertNotNull(layer);
        layer.flushCaches(main); // forces the layer to retrieve everything

        MockResponse image = TestUtils.createBinaryReponse("image/jpeg", "fixtures/mapillary_image_v4.jpg");
        mockApiServer.server().enqueue(image);
        mockApiServer.server().enqueue(image);
        mockApiServer.server().enqueue(image);
        mockApiServer.server().enqueue(image);
        mockApiServer.server().enqueue(image);

        TestUtils.unlock(device);
        TestUtils.sleep();

        ActivityMonitor monitor = instrumentation.addMonitor(ImageViewerActivity.class.getName(), null, false);
        // hack around slow rendering on some emulators
        map.getViewBox().moveTo(map, (int) (2.3285747 * 1E7), (int) (48.8588878 * 1E7));
        map.invalidate();
        TestUtils.zoomToLevel(device, main, 22);
        TestUtils.clickAtCoordinates(device, map, 2.3285747, 48.8588878, true);
        if (TestUtils.clickText(device, false, "OK", true)) {
            TestUtils.clickAtCoordinates(device, map, 2.3285747, 48.8588878, true);
        }
        ImageViewerActivity viewer = null;
        try {
            viewer = (ImageViewerActivity) instrumentation.waitForMonitorWithTimeout(monitor, 30000);
            assertNotNull(viewer);
            try {
                RecordedRequest recorded = mockApiServer.server().takeRequest(10, TimeUnit.SECONDS);
                assertNotNull(recorded);
                System.out.println(recorded.getPath());
                mockApiServer.server().takeRequest(10, TimeUnit.SECONDS);
                mockApiServer.server().takeRequest(10, TimeUnit.SECONDS);
                mockApiServer.server().takeRequest(10, TimeUnit.SECONDS);
                mockApiServer.server().takeRequest(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.share), false, true));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertTrue(TestUtils.findText(device, false, "f4fd371a-1203-4aa7-95ca-24026fa956b1"));
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

}
