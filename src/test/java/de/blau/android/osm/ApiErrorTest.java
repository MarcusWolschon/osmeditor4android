package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.ShadowWorkManager;
import de.blau.android.SignalUtils;
import de.blau.android.exception.OsmServerException;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.API.AuthParams;
import okhttp3.HttpUrl;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk = 33)
@LargeTest
public class ApiErrorTest {

    private static final String CLOSE_CHANGESET_FIXTURE = "close_changeset";
    private static final String UPLOAD1_FIXTURE         = "upload1";
    private static final String CHANGESET1_FIXTURE      = "changeset1";
    private static final String CAPABILITIES1_FIXTURE   = "capabilities1";
    private static final String TEST1_OSM_FIXTURE       = "test1.osm";

    static final String GENERATOR_NAME = "vesupucci test";

    public static final int TIMEOUT = 10;

    private MockWebServerPlus    mockServer = null;
    private AdvancedPrefDatabase prefDB     = null;
    private Main                 main       = null;
    private Preferences          prefs      = null;

    static class FailOnErrorHandler implements PostAsyncActionHandler {
        CountDownLatch signal;

        FailOnErrorHandler(@NonNull CountDownLatch signal) {
            this.signal = signal;
        }

        @Override
        public void onSuccess() {
            System.out.println("FailOnErrorHandler onSuccess");
            signal.countDown();
        }

        @Override
        public void onError(AsyncResult result) {
            fail("Expected success");
        }
    };

    static class FailOnSuccessHandler implements PostAsyncActionHandler {
        CountDownLatch signal;
        int            expectedError;

        FailOnSuccessHandler(@NonNull CountDownLatch signal, int expectedError) {
            this.signal = signal;
            this.expectedError = expectedError;
        }

        @Override
        public void onSuccess() {
            fail("Expected fail");

        }

        @Override
        public void onError(AsyncResult result) {
            System.out.println("FailOnSuccessHandler onError " + expectedError + " " + result.getCode());
            assertEquals(expectedError, result.getCode());
            signal.countDown();
        }
    };

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        App.getDelegator().reset(true);
        main = Robolectric.buildActivity(Main.class).create().resume().get();
        prefDB = new AdvancedPrefDatabase(main);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null));
        prefDB.selectAPI("Test");
        System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
        prefs = new Preferences(main);
        Logic logic = App.getLogic();
        logic.setPrefs(prefs);
        logic.getMap().setPrefs(main, prefs);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
        prefs.close();
    }

    /**
     * Simple bounding box data download error 500
     */
    @Test
    public void dataDownload500() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("500");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false,
                new FailOnSuccessHandler(signal, ErrorCodes.UNKNOWN_ERROR));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
    }

    /**
     * Simple bounding box data download error 509
     */
    @Test
    public void dataDownload509() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("509");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false,
                new FailOnSuccessHandler(signal, ErrorCodes.DOWNLOAD_LIMIT_EXCEEDED));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
    }

    /**
     * Simple bounding box data download error 400
     */
    @Test
    public void dataDownloadOther400() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("400-other");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false,
                new FailOnSuccessHandler(signal, ErrorCodes.BOUNDING_BOX_TOO_LARGE));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
    }

    /**
     * Simple bounding box data download error 400
     */
    @Test
    public void dataDownloadOAuth400() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("400");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false,
                new FailOnSuccessHandler(signal, ErrorCodes.INVALID_LOGIN));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
    }

    /**
     * Simple bounding box data download parse error
     */
    @Test
    public void dataDownloadParseError() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("invalid1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false,
                new FailOnSuccessHandler(signal, ErrorCodes.INVALID_DATA_RECEIVED));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
    }

    /**
     * Upload to changes (mock-)server that returns an 500 error
     */
    @Test
    public void dataUpload500() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(TEST1_OSM_FIXTURE);
        logic.readOsmFile(ApplicationProvider.getApplicationContext(), is, false, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertEquals(33, App.getDelegator().getApiElementCount());
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(CHANGESET1_FIXTURE);
        mockServer.enqueue("500");

        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
            fail("Should have failed");
        } catch (OsmServerException e) {
            assertEquals(500, e.getHttpErrorCode());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Super ugly hack to get the looper to run
     */
    private void runLooper() {
        try {
            Thread.sleep(3000); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            // Ignore
        }
        shadowOf(Looper.getMainLooper()).idle();
    }
}
