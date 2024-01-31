package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.ShadowWorkManager;
import de.blau.android.SignalUtils;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.Util;
import de.blau.android.validation.Validator;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class })
@LargeTest
public class ApiErrorTest {

    private static final String UPLOAD4_FIXTURE         = "upload4";
    private static final String CHANGESET5_FIXTURE      = "changeset5";
    private static final String CHANGESET4_FIXTURE      = "changeset4";
    private static final String UPLOAD3_FIXTURE         = "upload3";
    private static final String CHANGESET3_FIXTURE      = "changeset3";
    private static final String UPLOAD2_FIXTURE         = "upload2";
    private static final String CHANGESET2_FIXTURE      = "changeset2";
    private static final String CAPABILITIES2_FIXTURE   = "capabilities2";
    private static final String UPLOAD7_FIXTURE         = "upload7";
    private static final String PARTIALUPLOAD_FIXTURE   = "partialupload";
    private static final String CLOSE_CHANGESET_FIXTURE = "close_changeset";
    private static final String UPLOAD1_FIXTURE         = "upload1";
    private static final String CHANGESET1_FIXTURE      = "changeset1";
    private static final String ELEMENTFETCH1_FIXTURE   = "elementfetch1";
    private static final String MULTIFETCH3_FIXTURE     = "multifetch3";
    private static final String MULTIFETCH2_FIXTURE     = "multifetch2";
    private static final String MULTIFETCH1_FIXTURE     = "multifetch1";
    private static final String DOWNLOAD2_FIXTURE       = "download2";
    private static final String DOWNLOAD1_FIXTURE       = "download1";
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
     * Simple bounding box data download
     */
    @Test
    public void dataDownload() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(DOWNLOAD1_FIXTURE);
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984));

        // check that we have parsed and post processed relations correctly
        Relation r1 = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 1638705);
        Relation r2 = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        Relation parent = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2078158);
        assertTrue(r1.hasParentRelation(2078158));
        assertTrue(r2.hasParentRelation(2078158));
        assertNotNull(parent.getMember(r1));
        assertNotNull(parent.getMember(r2));
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
     * Upload to changes (mock-)server
     */
    //@Test
    public void dataUpload() {
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
        mockServer.enqueue(UPLOAD1_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);

        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(50000, s.getCachedCapabilities().getMaxElementsInChangeset());
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        assertEquals(7L, n.getOsmVersion());
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertEquals(OsmElement.STATE_UNCHANGED, r.getState());
        assertEquals(4L, r.getOsmVersion());
    }

 
}
