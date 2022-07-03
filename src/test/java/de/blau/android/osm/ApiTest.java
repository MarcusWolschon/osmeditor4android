package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.Util;
import de.blau.android.validation.Validator;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class })
@LargeTest
public class ApiTest {

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
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", false);
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
        logic.downloadBox(main, new BoundingBox(8.3838500D, 47.3883000D, 8.3865200D, 47.3898500D), true, new FailOnErrorHandler(signal));
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

        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
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

        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
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

        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
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
     * Retrieve a changeset by id
     */
    @Test
    public void getChangeset() {
        mockServer.enqueue(CHANGESET5_FIXTURE);
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        Changeset cs = s.getChangeset(1234567);
        assertNotNull(cs);
        assertEquals(120631739L, cs.osmId);
        assertNotNull(cs.tags);
        assertEquals("swisstopo SWISSIMAGE;Mapillary Images;KartaView Images", cs.tags.get("imagery_used"));
    }

    /**
     * Update a changeset
     */
    @Test
    public void updateChangeset() {
        mockServer.enqueue(CHANGESET5_FIXTURE);
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        try {
            Changeset cs = s.updateChangeset(1234567, "ignored", "ignored", "ignored", null);
            assertNotNull(cs);
            assertEquals(120631739L, cs.osmId);
            assertNotNull(cs.tags);
            assertEquals("swisstopo SWISSIMAGE;Mapillary Images;KartaView Images", cs.tags.get("imagery_used"));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * open an existing changeset
     */
    @Test
    public void openExistingChangeset() {
        mockServer.enqueue(CHANGESET5_FIXTURE);
        mockServer.enqueue(CHANGESET5_FIXTURE);
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        s.setOpenChangeset(123456789);
        try {
            s.openChangeset(false, "ignored", "ignored", "ignored", null);
            assertEquals(123456789, s.getOpenChangeset()); // still open
        } catch (IOException e) {
            fail(e.getMessage());
        }
        try {
            mockServer.takeRequest();
            RecordedRequest request = mockServer.takeRequest();
            assertEquals("/api/0.6/changeset/123456789", request.getPath());
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    /**
     * replace an existing changeset
     */
    @Test
    public void replaceExistingChangeset() {
        mockServer.enqueue(CHANGESET5_FIXTURE);
        mockServer.enqueue(CLOSE_CHANGESET_FIXTURE);
        mockServer.enqueue(CHANGESET1_FIXTURE);
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        s.setOpenChangeset(123456789);
        try {
            s.openChangeset(true, "ignored", "ignored", "ignored", null);
            assertEquals(1234567, s.getOpenChangeset()); // new id
        } catch (IOException e) {
            fail(e.getMessage());
        }
        try {
            mockServer.takeRequest();
            mockServer.takeRequest();
            RecordedRequest request = mockServer.takeRequest();
            assertEquals("/api/0.6/changeset/create", request.getPath());
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}
