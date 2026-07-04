package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.SignalUtils;
import de.blau.android.TestUtils;
import de.blau.android.layer.LayerType;
import de.blau.android.prefs.API;
import de.blau.android.prefs.API.AuthParams;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DownloadAlongTest {

    private static final String DEBUG_TAG = DownloadAlongTest.class.getSimpleName();

    private static final String CAPABILITIES1_FIXTURE = "capabilities1";

    public static final int TIMEOUT = 90;

    MockWebServerPlus       mockServer = null;
    Context                 context    = null;
    AdvancedPrefDatabase    prefDB     = null;
    Main                    main       = null;
    private Instrumentation instrumentation;
    UiDevice                device     = null;
    private Preferences     prefs      = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext();
        main = mActivityRule.getActivity();
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false, false);
        prefDB.selectAPI("Test");
        prefDB.deleteLayer(LayerType.TASKS, null);
        prefs = new Preferences(context);
        prefs.setPanAndZoomAutoDownload(false);
        prefs.enableAcra(false);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
        App.getDelegator().reset(true);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        Log.d(DEBUG_TAG, "teardown");
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
    }

    /**
     * Select a way and then download along
     */
    @Test
    public void wayTest() {
        final CountDownLatch signal = new CountDownLatch(1);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("schulstrasse.osm");
        final Logic logic = App.getLogic();
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        SignalUtils.signalAwait(signal, TIMEOUT);

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("downloadalong1");
        mockServer.enqueue("downloadalong2");
        mockServer.enqueue("downloadalong3");
        TestUtils.zoomToLevel(device, main, 22);

        TestUtils.unlock(device);
        TestUtils.clickAwayTip(device, context);

        // w49855552
        TestUtils.clickAtCoordinates(device, main.getMap(), 8.385856, 47.3895648, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));

        assertEquals(49855552L, logic.getSelectedWay().getOsmId());
        TestUtils.clickOverflowButton(device);
        TestUtils.scrollTo(main.getString(R.string.menu_download_along_way), false);
        TestUtils.clickText(device, false, main.getString(R.string.menu_download_along_way), true, false);
        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.download_along_way_title), 5000));
        TestUtils.clickText(device, false, main.getString(R.string.submit), true, false);

        try {
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        TestUtils.sleep(2000);
        assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 210468096L));
    }

    /**
     * Select a way and then download along
     */
    @Test
    public void wayTestApiNotAvailable() {
        final CountDownLatch signal = new CountDownLatch(1);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("schulstrasse.osm");
        final Logic logic = App.getLogic();
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        SignalUtils.signalAwait(signal, TIMEOUT);

        mockServer.enqueue("capabilities-api-offline");
        TestUtils.zoomToLevel(device, main, 22);

        TestUtils.unlock(device);
        TestUtils.clickAwayTip(device, context);

        // w49855552
        TestUtils.clickAtCoordinates(device, main.getMap(), 8.385856, 47.3895648, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));

        assertEquals(49855552L, logic.getSelectedWay().getOsmId());
        TestUtils.clickOverflowButton(device);
        TestUtils.scrollTo(main.getString(R.string.menu_download_along_way), false);
        TestUtils.clickText(device, false, main.getString(R.string.menu_download_along_way), true, false);
        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.download_along_way_title), 5000));
        TestUtils.clickText(device, false, main.getString(R.string.submit), true, false);

        try {
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        TestUtils.sleep(2000);
        assertNull(App.getDelegator().getOsmElement(Way.NAME, 210468096L));
    }

    /**
     * Select a way and then download along
     */
    @Test
    public void wayTestErrorPause() {
        final CountDownLatch signal = new CountDownLatch(1);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("schulstrasse.osm");
        final Logic logic = App.getLogic();
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        SignalUtils.signalAwait(signal, TIMEOUT);

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("downloadalong1");
        mockServer.enqueue("500");

        TestUtils.zoomToLevel(device, main, 22);

        TestUtils.unlock(device);
        TestUtils.clickAwayTip(device, context);

        // w49855552
        TestUtils.clickAtCoordinates(device, main.getMap(), 8.385856, 47.3895648, true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));

        assertEquals(49855552L, logic.getSelectedWay().getOsmId());
        TestUtils.clickOverflowButton(device);
        TestUtils.scrollTo(main.getString(R.string.menu_download_along_way), false);
        TestUtils.clickText(device, false, main.getString(R.string.menu_download_along_way), true, false);
        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.download_along_way_title), 5000));
        TestUtils.clickText(device, false, main.getString(R.string.submit), true, false);

        try {
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        TestUtils.sleep(2000);
        assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 210456655L));
        assertNull(App.getDelegator().getOsmElement(Way.NAME, 210468096L));
    }
}
