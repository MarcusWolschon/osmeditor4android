package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.SignalUtils;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.Util;
import de.blau.android.validation.Validator;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LEGACY)
@LargeTest
public class ApiTest {
    private static final String DEBUG_TAG = ApiTest.class.getName();

    private static final String NOTES_DOWNLOAD1_FIXTURE = "notesDownload1";
    private static final String UPLOAD6_FIXTURE         = "upload6";
    private static final String UPLOAD5_FIXTURE         = "upload5";
    private static final String UPLOAD4_FIXTURE         = "upload4";
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

    private static final String GENERATOR_NAME = "vesupucci test";

    public static final int TIMEOUT = 10;

    MockWebServerPlus mockServer = null;

    private Context context;

    private API api;

    class FailOnErrorHandler implements PostAsyncActionHandler {
        CountDownLatch signal;

        FailOnErrorHandler(@NonNull CountDownLatch signal) {
            this.signal = signal;
        }

        @Override
        public void onSuccess() {
            signal.countDown();
        }

        @Override
        public void onError(AsyncResult result) {
            fail("Expected success");
        }
    }

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        Logic logic = App.newLogic();
        context = ApplicationProvider.getApplicationContext();
        AdvancedPrefDatabase prefDB = new AdvancedPrefDatabase(context);
        logic.setPrefs(new Preferences(context));
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", false);
        prefDB.selectAPI("Test");
        api = prefDB.getCurrentAPI();
        prefDB.close();
        Log.d(DEBUG_TAG, "mock api url " + mockBaseUrl.toString()); // NOSONAR
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
    }

    /**
     * Get API capabilities
     */
    @Test
    public void capabilities() {
        mockServer.enqueue(CAPABILITIES1_FIXTURE);

        final Server s = new Server(ApplicationProvider.getApplicationContext(), api, GENERATOR_NAME);
        Capabilities result = s.getCapabilities();

        assertNotNull(result);
        assertEquals("0.6", result.getMinVersion());
        assertEquals(Capabilities.Status.ONLINE, result.getGpxStatus());
        assertEquals(Capabilities.Status.ONLINE, result.getApiStatus());
        assertEquals(Capabilities.Status.ONLINE, result.getDbStatus());
        assertEquals(2001, result.getMaxWayNodes(), 2001);
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
        logic.downloadBox(ApplicationProvider.getApplicationContext(), new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false,
                new FailOnErrorHandler(signal));
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
     * Download then download again and merge
     */
    @Test
    public void dataDownloadMerge() {
        dataDownload();

        // modify this node
        Logic logic = App.getLogic();
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        Map<String, String> tags = new TreeMap<>(n.getTags());
        tags.put(Tags.KEY_NAME, "dietikonBerg");
        try {
            logic.setTags(null, Node.NAME, 101792984L, tags);
        } catch (OsmIllegalOperationException e1) {
            fail(e1.getMessage());
        }

        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(DOWNLOAD2_FIXTURE);
        logic.downloadBox(ApplicationProvider.getApplicationContext(), new BoundingBox(8.3838500D, 47.3883000D, 8.3865200D, 47.3898500D), true,
                new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984L));
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        n.hasTag(Tags.KEY_NAME, "dietikonBerg");

        // test timestamp related stuff, no point in making a separate test
        Node t = (Node) App.getDelegator().getOsmElement(Node.NAME, 3465444349L);
        assertNotNull(t);
        assertTrue(t.hasTag("amenity", "toilets"));
        assertEquals(1429452889, t.getTimestamp()); // 2015-04-19T14:14:49Z
        assertNotEquals(Validator.OK,
                t.hasProblem(ApplicationProvider.getApplicationContext(), App.getDefaultValidator(ApplicationProvider.getApplicationContext())));
    }

    /**
     * Fetch multiple elements in one call
     */
    @Test
    public void dataDownloadMultiFetch() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(MULTIFETCH1_FIXTURE);
        mockServer.enqueue(MULTIFETCH2_FIXTURE);
        mockServer.enqueue(MULTIFETCH3_FIXTURE);
        Logic logic = App.getLogic();

        List<Long> nodes = new ArrayList<>();
        nodes.add(Long.valueOf(416083528L));
        nodes.add(Long.valueOf(577098580L));
        nodes.add(Long.valueOf(577098578L));
        nodes.add(Long.valueOf(573380242L));
        nodes.add(Long.valueOf(577098597L));
        nodes.add(Long.valueOf(984783547L));
        nodes.add(Long.valueOf(984784083L));
        nodes.add(Long.valueOf(2190871496L));
        nodes.add(Long.valueOf(1623520413L));
        nodes.add(Long.valueOf(954564305L));
        nodes.add(Long.valueOf(990041213L));

        List<Long> ways = new ArrayList<>();
        ways.add(Long.valueOf(35479116L));
        ways.add(Long.valueOf(35479120L));

        logic.downloadElements(ApplicationProvider.getApplicationContext(), nodes, ways, null, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 573380242L));
        assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 35479116L));
    }

    /**
     * Down load a Relation with members
     */
    @Test
    public void dataDownloadElement() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(ELEMENTFETCH1_FIXTURE);
        Logic logic = App.getLogic();

        logic.downloadElement(ApplicationProvider.getApplicationContext(), Relation.NAME, 2807173L, true, false, new FailOnErrorHandler(signal));
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        assertNotNull(App.getDelegator().getOsmElement(Relation.NAME, 2807173L));
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 416426192L));
        assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 104364414L));
    }

    /**
     * Upload to changes (mock-)server
     */
    @Test
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

        final Server s = new Server(ApplicationProvider.getApplicationContext(), api, GENERATOR_NAME);
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

    /**
     * Upload a subset (just one) of changes (mock-)server
     */
    @Test
    public void dataUploadSelective() {
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
        mockServer.enqueue(PARTIALUPLOAD_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);

        final Server s = new Server(ApplicationProvider.getApplicationContext(), api, GENERATOR_NAME);
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, Util.wrapInList(n));
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(50000, s.getCachedCapabilities().getMaxElementsInChangeset());
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        assertEquals(7L, n.getOsmVersion());
        assertEquals(32, App.getDelegator().getApiElementCount());
    }

    /**
     * Upload unchanged data (mock-)server
     */
    @Test
    public void dataUploadUnchanged() {
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
        n.setState(OsmElement.STATE_UNCHANGED);

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(CHANGESET1_FIXTURE);
        mockServer.enqueue(UPLOAD7_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);

        final Server s = new Server(ApplicationProvider.getApplicationContext(), api, GENERATOR_NAME);
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Upload to changes (mock-)server with reduced number of elements per changeset
     */
    @Test
    public void dataUploadSplit() {
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
        assertEquals(6L, n.getOsmVersion());

        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604);
        assertNotNull(w);
        assertEquals(OsmElement.STATE_MODIFIED, w.getState());
        assertEquals(18L, w.getOsmVersion());

        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);
        assertEquals(OsmElement.STATE_MODIFIED, r.getState());
        assertEquals(3L, r.getOsmVersion());

        mockServer.enqueue(CAPABILITIES2_FIXTURE);
        mockServer.enqueue(CHANGESET2_FIXTURE);
        mockServer.enqueue(UPLOAD2_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);
        mockServer.enqueue(CHANGESET3_FIXTURE);
        mockServer.enqueue(UPLOAD3_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);
        mockServer.enqueue(CHANGESET4_FIXTURE);
        mockServer.enqueue(UPLOAD4_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);

        final Server s = new Server(ApplicationProvider.getApplicationContext(), api, GENERATOR_NAME);
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, false, null, null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(11, s.getCachedCapabilities().getMaxElementsInChangeset());
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        assertEquals(7L, n.getOsmVersion());
        w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604);
        assertNotNull(w);
        assertEquals(OsmElement.STATE_UNCHANGED, w.getState());
        assertEquals(19L, w.getOsmVersion());
        r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertNotNull(r);
        assertEquals(OsmElement.STATE_UNCHANGED, r.getState());
        assertEquals(4L, r.getOsmVersion());
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

        final Server s = new Server(ApplicationProvider.getApplicationContext(), api, GENERATOR_NAME);
        s.resetChangeset();
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
        } catch (OsmServerException e) {
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

        final Server s = new Server(ApplicationProvider.getApplicationContext(), api, GENERATOR_NAME);
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException e) {
            // expected
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
        Preferences prefs = new Preferences(context);
        logic.setPrefs(prefs);
        logic.downloadBox(context, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false, new PostAsyncActionHandler() {
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

    /**
     * Download Notes for a bounding box
     */
    @Test
    public void notesDownload() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(NOTES_DOWNLOAD1_FIXTURE);
        App.getTaskStorage().reset();
        try {
            final Server s = new Server(ApplicationProvider.getApplicationContext(), api, GENERATOR_NAME);
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext());
            Resources r = ApplicationProvider.getApplicationContext().getResources();
            String notesSelector = r.getString(R.string.bugfilter_notes);
            Set<String> set = new HashSet<>(Arrays.asList(notesSelector));
            p.edit().putStringSet(r.getString(R.string.config_bugFilter_key), set).commit();
            assertTrue(new Preferences(ApplicationProvider.getApplicationContext()).taskFilter().contains(notesSelector));
            TransferTasks.downloadBox(ApplicationProvider.getApplicationContext(), s, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false,
                    TransferTasks.MAX_PER_REQUEST, new FailOnErrorHandler(signal));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        List<Task> tasks = App.getTaskStorage().getTasks();
        // note the fixture contains 100 notes, however 41 of them are closed and expired
        assertEquals(59, tasks.size());
        try {
            tasks = App.getTaskStorage().getTasks(new BoundingBox(-0.0918, 51.532, -0.0917, 51.533));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertTrue(tasks.get(0) instanceof Note);
        assertEquals(458427, tasks.get(0).getId());
    }

    /**
     * Upload a single new Note
     */
    @Test
    public void noteUpload() {
        Main main = Robolectric.setupActivity(Main.class);
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("noteUpload1");
        App.getTaskStorage().reset();
        try {
            final Server s = new Server(ApplicationProvider.getApplicationContext(), api, GENERATOR_NAME);
            Note n = new Note((int) (51.0 * 1E7D), (int) (0.1 * 1E7D));
            assertTrue(n.isNew());
            assertTrue(TransferTasks.uploadNote(main, s, n, "ThisIsANote", false, new FailOnErrorHandler(signal)));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        try {
            assertFalse(App.getTaskStorage().isEmpty());
            List<Task> tasks = App.getTaskStorage().getTasks(new BoundingBox(0.099, 50.99, 0.111, 51.01));
            assertEquals(1, tasks.size());
            Note n = (Note) tasks.get(0);
            assertEquals("<p>ThisIsANote</p>", n.getLastComment().getText());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * get the user preferences, the set and delete one
     */
    @Test
    public void userpreferences() {
        mockServer.enqueue("userpreferences");
        mockServer.enqueue("200");
        mockServer.enqueue("200");
        try {
            final Server s = new Server(ApplicationProvider.getApplicationContext(), api, GENERATOR_NAME);
            Map<String, String> preferences = s.getUserPreferences();
            assertEquals(3, preferences.size());
            assertEquals("public", preferences.get("gps.trace.visibility"));
            RecordedRequest request1 = mockServer.takeRequest();
            assertEquals("GET", request1.getMethod().toUpperCase());
            assertEquals("/api/0.6/user/preferences", request1.getPath());
            s.setUserPreference("gps.trace.visibility", "private");
            RecordedRequest request2 = mockServer.takeRequest();
            assertEquals("PUT", request2.getMethod().toUpperCase());
            assertEquals("/api/0.6/user/preferences/gps.trace.visibility", request2.getPath());
            assertEquals("private", request2.getBody().readUtf8());
            s.deleteUserPreference("gps.trace.visibility");
            RecordedRequest request3 = mockServer.takeRequest();
            assertEquals("DELETE", request3.getMethod().toUpperCase());
            assertEquals("/api/0.6/user/preferences/gps.trace.visibility", request3.getPath());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
