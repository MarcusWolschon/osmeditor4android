package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
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
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.prefs.API;
import de.blau.android.ShadowWorkManager;
import de.blau.android.SignalUtils;
import de.blau.android.exception.OsmServerException;
import de.blau.android.osm.ApiTest.FailOnErrorHandler;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import okhttp3.HttpUrl;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class })
@LargeTest
public class ApiErrorTest {

    private static final String UPLOAD6_FIXTURE         = "upload6";
    private static final String UPLOAD5_FIXTURE         = "upload5";
    private static final String CLOSE_CHANGESET_FIXTURE = "close_changeset";
    private static final String CHANGESET1_FIXTURE      = "changeset1";;
    private static final String CAPABILITIES1_FIXTURE   = "capabilities1";
    private static final String TEST1_OSM_FIXTURE       = "test1.osm";

    public static final int TIMEOUT = 10;

    private MockWebServerPlus    mockServer = null;
    private AdvancedPrefDatabase prefDB     = null;
    private Main                 main       = null;
    private Preferences          prefs      = null;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        main = Robolectric.buildActivity(Main.class).create().resume().get();
        prefDB = new AdvancedPrefDatabase(main);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", API.Auth.BASIC);
        prefDB.selectAPI("Test");
        System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
        Logic logic = App.getLogic();
        prefs = new Preferences(main);
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

    /**
     * Upload changes (mock-)server and check behaviour when we receive an error
     */
    @Test
    public void dataUploadErrors() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        // we need something changes in memory or else we wont try to upload
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(TEST1_OSM_FIXTURE);
        logic.readOsmFile(ApplicationProvider.getApplicationContext(), is, false, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertTrue(App.getDelegator().getApiElementCount() > 0);
        uploadErrorTest(401);
        uploadErrorTest(403);
        uploadErrorTest(429);
        uploadErrorTest(999);
    }

    /**
     * Upload changes (mock-)server and check behaviour when we receive an error
     * 
     * @param code error code to return
     */
    private void uploadErrorTest(int code) {
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("" + code);

        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), ApiTest.GENERATOR_NAME);
        s.resetChangeset();
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
        } catch (OsmServerException e) {
            System.out.println(e.getMessage());
            assertEquals(code, e.getErrorCode());
            return;
        } catch (IOException e) {
            fail(e.getMessage());
            return;
        }
        fail("Expected error " + code);
    }

    /**
     * Upload changes (mock-)server and check behaviour when we receive a broken response
     */
    @Test
    public void dataUploadErrorInResult() {
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
        mockServer.enqueue(UPLOAD5_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);
        mockServer.enqueue(CHANGESET1_FIXTURE);
        mockServer.enqueue(UPLOAD6_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);

        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), ApiTest.GENERATOR_NAME);
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException e) {
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(1, App.getDelegator().getApiElementCount());
    }

    /**
     * Test the response to error code 400 on download
     */
    @Test
    public void dataDownloadError400() {
        downloadErrorTest(400);
    }

    /**
     * Test the response to error code 401 on download
     */
    @Test
    public void dataDownloadError401() {
        downloadErrorTest(401);
    }

    /**
     * Test the response to error code 403 on download
     */
    @Test
    public void dataDownloadError403() {
        downloadErrorTest(403);
    }

    /**
     * Test the response to error code 509 on download
     */
    @Test
    public void dataDownloadError509() {
        downloadErrorTest(509);
    }

    /**
     * Test the response to error code 999 on download
     */
    @Test
    public void dataDownloadError999() {
        downloadErrorTest(999);
    }

    /**
     * Test that receiving a specific error code doesn't break anything
     * 
     * @param code the error code
     */
    private void downloadErrorTest(int code) {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("" + code);
        Logic logic = App.getLogic();
        logic.downloadBox(ApplicationProvider.getApplicationContext(), new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false,
                new PostAsyncActionHandler() {
                    @Override
                    public void onSuccess() {
                        fail("Expected error");
                    }

                    @Override
                    public void onError(AsyncResult result) {
                        signal.countDown();
                    }
                });
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
    }
}
