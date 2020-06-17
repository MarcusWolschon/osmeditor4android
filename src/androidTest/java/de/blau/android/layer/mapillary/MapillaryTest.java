package de.blau.android.layer.mapillary;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.layer.LayerDialogTest;
import de.blau.android.layer.LayerType;
import de.blau.android.osm.ApiTest;
import de.blau.android.photos.MapillaryViewerActivity;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MapillaryTest {

    AdvancedPrefDatabase prefDB           = null;
    Main                 main             = null;
    UiDevice             device           = null;
    Map                  map              = null;
    Logic                logic            = null;
    Instrumentation      instrumentation  = null;
    MockWebServerPlus    mockApiServer    = null;
    MockWebServerPlus    mockImagesServer = null;

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

        Assert.assertNotNull(main);
        TestUtils.grantPermissons(device);
        Preferences prefs = new Preferences(main);
        TestUtils.removeImageryLayers(main);
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
            db.deleteLayer(LayerType.MAPILLARY, null);
        }
        map = main.getMap();

        mockApiServer = new MockWebServerPlus();
        HttpUrl mockApiBaseUrl = mockApiServer.server().url("/");
        prefs.setMapillaryApiUrl(mockApiBaseUrl.toString());

        mockImagesServer = new MockWebServerPlus();
        HttpUrl mockImagesBaseUrl = mockImagesServer.server().url("/");
        prefs.setMapillaryImagesUrl(mockImagesBaseUrl.toString());

        map.setPrefs(main, prefs);

        TestUtils.dismissStartUpDialogs(device, main);
        final CountDownLatch signal1 = new CountDownLatch(1);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test2.osm");
        logic = App.getLogic();
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
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
        // instrumentation.removeMonitor(monitor);
        instrumentation.waitForIdleSync();
    }

    /**
     * Add mapillary layer
     */
    @Test
    public void mapillaryLayer() {
        mockApiServer.enqueue("mapillary_sequences");
        MockResponse image = TestUtils.createBinaryReponse("image/jpeg", "fixtures/THi1maFChJ6A6-6cRaVHuQ.jpg");
        mockImagesServer.server().enqueue(image);
        mockImagesServer.server().enqueue(image);
        mockImagesServer.server().enqueue(image);
        mockImagesServer.server().enqueue(image);
        mockImagesServer.server().enqueue(image);
        String dataLayerName = map.getDataLayer().getName();
        UiObject2 extentButton = TestUtils.getLayerButton(device, dataLayerName, LayerDialogTest.EXTENT_BUTTON);
        extentButton.clickAndWait(Until.newWindow(), 2000);
        TestUtils.zoomToLevel(device, main, 22);

        // this isn't possible via UI (PopupWindow issue)
        de.blau.android.layer.Util.addLayer(main, LayerType.MAPILLARY);
        map.setUpLayers(main);

        de.blau.android.layer.mapillary.MapOverlay layer = (MapOverlay) map.getLayer(LayerType.MAPILLARY);

        Assert.assertNotNull(layer);
        final CountDownLatch signal1 = new CountDownLatch(1);
        layer.downloadBox(main, map.getViewBox(), new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            Assert.fail(e.getMessage());
        }
        Assert.assertEquals(17, layer.getFeatures().size());
        TestUtils.unlock(device);
        TestUtils.sleep();
        ActivityMonitor monitor = instrumentation.addMonitor(MapillaryViewerActivity.class.getName(), null, false);
        TestUtils.clickAtCoordinates(device, map, 8.3886805, 47.3893802, true);
        Assert.assertNotNull(instrumentation.waitForMonitorWithTimeout(monitor, 30000));
        instrumentation.removeMonitor(monitor);
        try {
            RecordedRequest recorded = mockImagesServer.server().takeRequest(10000, TimeUnit.SECONDS);
            System.out.println(recorded.getPath());
            mockImagesServer.server().takeRequest(10000, TimeUnit.SECONDS);
            mockImagesServer.server().takeRequest(10000, TimeUnit.SECONDS);
            mockImagesServer.server().takeRequest(10000, TimeUnit.SECONDS);
            mockImagesServer.server().takeRequest(10000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        device.pressBack();
    }
}
