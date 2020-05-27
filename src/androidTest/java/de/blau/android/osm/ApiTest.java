package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import android.view.KeyEvent;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.Util;
import de.blau.android.validation.Validator;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApiTest {

    public static final int TIMEOUT = 90;

    private static final String SOURCE_1  = "source 1";
    private static final String COMMENT_1 = "comment 1";

    MockWebServerPlus       mockServer = null;
    Context                 context    = null;
    AdvancedPrefDatabase    prefDB     = null;
    Main                    main       = null;
    private Instrumentation instrumentation;
    UiDevice                device     = null;

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
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", false);
        prefDB.selectAPI("Test");
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerSource.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerSource.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
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
    }

    /**
     * Get API capabilities
     */
    @Test
    public void capabilities() {
        mockServer.enqueue("capabilities1");

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
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
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false, new SignalHandler(signal));

        TestUtils.signalAwait(signal, TIMEOUT);
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
     * Downlad then download again and merge
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
            logic.setTags(main, Node.NAME, 101792984L, tags);
        } catch (OsmIllegalOperationException e1) {
            fail(e1.getMessage());
        }

        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download2");
        logic.downloadBox(main, new BoundingBox(8.3838500D, 47.3883000D, 8.3865200D, 47.3898500D), true, new SignalHandler(signal));
        TestUtils.signalAwait(signal, TIMEOUT);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984L));
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        n.hasTag(Tags.KEY_NAME, "dietikonBerg");

        // test timestamp related stuff, no point in making a separate test
        Node t = (Node) App.getDelegator().getOsmElement(Node.NAME, 3465444349L);
        assertNotNull(t);
        assertTrue(t.hasTag("amenity", "toilets"));
        assertEquals(1429452889, t.getTimestamp()); // 2015-04-19T14:14:49Z
        assertNotEquals(Validator.OK, t.hasProblem(main, App.getDefaultValidator(main)));
    }

    /**
     * Fetch multiple elements in one call
     */
    @Test
    public void dataDownloadMultiFetch() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("multifetch1");
        mockServer.enqueue("multifetch2");
        mockServer.enqueue("multifetch3");
        Logic logic = App.getLogic();

        List<Long> nodes = new ArrayList<Long>();
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

        List<Long> ways = new ArrayList<Long>();
        ways.add(Long.valueOf(35479116L));
        ways.add(Long.valueOf(35479120L));

        logic.downloadElements(main, nodes, ways, null, new SignalHandler(signal));

        TestUtils.signalAwait(signal, TIMEOUT);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 573380242L));
        assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 35479116L));
    }

    /**
     * Down load a Relation with members
     */
    @Test
    public void dataDownloadElement() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("elementfetch1");
        Logic logic = App.getLogic();

        logic.downloadElement(main, Relation.NAME, 2807173L, true, false, new SignalHandler(signal));

        TestUtils.signalAwait(signal, TIMEOUT);
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
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        TestUtils.signalAwait(signal, TIMEOUT);
        assertEquals(33, App.getDelegator().getApiElementCount());
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());

        mockServer.enqueue("capabilities1");
        mockServer.enqueue("changeset1");
        mockServer.enqueue("upload1");
        mockServer.enqueue("close_changeset");

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
        } catch (OsmServerException e) {
            fail(e.getMessage());
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        } catch (ProtocolException e) {
            fail(e.getMessage());
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
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        TestUtils.signalAwait(signal, TIMEOUT);
        assertEquals(33, App.getDelegator().getApiElementCount());
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());

        mockServer.enqueue("capabilities1");
        mockServer.enqueue("changeset1");
        mockServer.enqueue("partialupload");
        mockServer.enqueue("close_changeset");

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, Util.wrapInList(n));
        } catch (OsmServerException e) {
            fail(e.getMessage());
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        } catch (ProtocolException e) {
            fail(e.getMessage());
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
     * Upload to changes (mock-)server
     */
    @Test
    public void dataUploadViaDialog() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        TestUtils.signalAwait(signal, TIMEOUT);
        assertEquals(33, App.getDelegator().getApiElementCount());
        assertEquals(27, App.getDelegator().getApiNodeCount());
        assertEquals(4, App.getDelegator().getApiWayCount());
        assertEquals(2, App.getDelegator().getApiRelationCount());

        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());

        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 210461100);
        assertNotNull(w);
        assertEquals(OsmElement.STATE_DELETED, w.getState());

        mockServer.enqueue("capabilities1"); // for whatever reason this gets asked for twice
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("changeset1");
        mockServer.enqueue("upload1");
        mockServer.enqueue("close_changeset");
        mockServer.enqueue("userdetails");

        TestUtils.clickMenuButton(device, "Transfer", false, true);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        TestUtils.clickText(device, false, "Upload", true, false); // menu item

        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(1); // dialog upload button
        UiObject button = device.findObject(uiSelector);
        try {
            button.click();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        device.waitForIdle();
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/upload_comment", true));
        instrumentation.sendStringSync(COMMENT_1);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_ENTER);
        // assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/upload_source",
        // true));
        instrumentation.sendStringSync(SOURCE_1);
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_BACK);
        try {
            button.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        try {
            Thread.sleep(10000); // NOSONAR
        } catch (InterruptedException e) {
        }
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        assertEquals(7L, n.getOsmVersion());
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertEquals(OsmElement.STATE_UNCHANGED, r.getState());
        assertEquals(4L, r.getOsmVersion());
        // check that we actually sent what was expected
        try {
            mockServer.takeRequest(); // capabilities query
            mockServer.takeRequest(); // capabilities query
            RecordedRequest recordedRequest = mockServer.takeRequest(); // changeset
            // this currently doesn't work
            // Changeset changeset = Changeset.parse(XmlPullParserFactory.newInstance().newPullParser(),
            // new ByteArrayInputStream(recordedRequest.getBody().readByteArray()));
            // assertEquals(COMMENT_1, changeset.tags.get("comment"));
            // assertEquals(SOURCE_1, changeset.tags.get("source"));
            recordedRequest = mockServer.takeRequest(); // diff upload
            OsmChangeParser ocParser = new OsmChangeParser();
            ocParser.start(new ByteArrayInputStream(recordedRequest.getBody().readByteArray()));
            Storage storage = ocParser.getStorage();
            assertEquals(27, storage.getNodeCount());
            assertEquals(4, storage.getWayCount());
            assertEquals(2, storage.getRelationCount());

            n = (Node) storage.getOsmElement(Node.NAME, 101792984);
            assertNotNull(n);
            assertEquals(OsmElement.STATE_MODIFIED, n.getState());

            w = (Way) storage.getOsmElement(Way.NAME, 210461100);
            assertNotNull(w);
            assertEquals(OsmElement.STATE_DELETED, w.getState());
        } catch (InterruptedException | SAXException | IOException | ParserConfigurationException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Upload unchanged data (mock-)server
     */
    @Test
    public void dataUploadUnchanged() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        TestUtils.signalAwait(signal, TIMEOUT);
        assertEquals(33, App.getDelegator().getApiElementCount());
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());
        n.setState(OsmElement.STATE_UNCHANGED);

        mockServer.enqueue("capabilities1");
        mockServer.enqueue("changeset1");
        mockServer.enqueue("upload7");
        mockServer.enqueue("close_changeset");

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
        } catch (OsmServerException e) {
            fail(e.getMessage());
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        } catch (ProtocolException e) {
            fail(e.getMessage());
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
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        TestUtils.signalAwait(signal, TIMEOUT);

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

        mockServer.enqueue("capabilities2");
        mockServer.enqueue("changeset2");
        mockServer.enqueue("upload2");
        mockServer.enqueue("close_changeset");
        mockServer.enqueue("changeset3");
        mockServer.enqueue("upload3");
        mockServer.enqueue("close_changeset");
        mockServer.enqueue("changeset4");
        mockServer.enqueue("upload4");
        mockServer.enqueue("close_changeset");

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, false, null, null);
        } catch (OsmServerException e) {
            fail(e.getMessage());
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        } catch (ProtocolException e) {
            fail(e.getMessage());
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
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        TestUtils.signalAwait(signal, TIMEOUT);
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
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("" + code);

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        s.resetChangeset();
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
        } catch (OsmServerException e) {
            System.out.println(e.getMessage());
            assertEquals(code, e.getErrorCode());
            return;
        } catch (MalformedURLException e) {
            fail(e.getMessage());
            return;
        } catch (ProtocolException e) {
            fail(e.getMessage());
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
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        TestUtils.signalAwait(signal, TIMEOUT);
        assertEquals(33, App.getDelegator().getApiElementCount());
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());

        mockServer.enqueue("capabilities1");
        mockServer.enqueue("changeset1");
        mockServer.enqueue("upload5");
        mockServer.enqueue("close_changeset");
        mockServer.enqueue("changeset1");
        mockServer.enqueue("upload6");
        mockServer.enqueue("close_changeset");

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", false, true, null, null);
            fail("Expected ProtocolException");
        } catch (ProtocolException e) {
        } catch (OsmServerException e) {
            fail(e.getMessage());
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(1, App.getDelegator().getApiElementCount());
    }

    /**
     * Test the response to various error codes on download
     */
    @Test
    public void dataDownloadErrors() {
        downloadErrorTest(400);
        downloadErrorTest(401);
        downloadErrorTest(403);
        downloadErrorTest(999);
    }

    /**
     * Test that receiving a specific error code doesn't break anything
     * 
     * @param code the error code
     */
    private void downloadErrorTest(int code) {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("" + code);
        Logic logic = App.getLogic();
        logic.downloadBox(main, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false, new SignalHandler(signal));
        TestUtils.signalAwait(signal, TIMEOUT);
    }

    /**
     * Download Notes for a bounding box
     */
    @Test
    public void notesDownload() {
        final CountDownLatch signal = new CountDownLatch(1);
        // mockServer.enqueue("capabilities1");
        mockServer.enqueue("notesDownload1");
        App.getTaskStorage().reset();
        try {
            final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            Resources r = context.getResources();
            String notesSelector = r.getString(R.string.bugfilter_notes);
            Set<String> set = new HashSet<String>(Arrays.asList(notesSelector));
            p.edit().putStringSet(r.getString(R.string.config_bugFilter_key), set).commit();
            assertTrue(new Preferences(context).taskFilter().contains(notesSelector));
            TransferTasks.downloadBox(context, s, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false, new SignalHandler(signal));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        TestUtils.signalAwait(signal, TIMEOUT);
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
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("noteUpload1");
        App.getTaskStorage().reset();
        try {
            final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
            Note n = new Note((int) (51.0 * 1E7D), (int) (0.1 * 1E7D));
            assertTrue(n.isNew());
            assertTrue(TransferTasks.uploadNote(main, s, n, "ThisIsANote", false, false, new SignalHandler(signal)));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        TestUtils.signalAwait(signal, TIMEOUT);
        try {
            List<Task> tasks = App.getTaskStorage().getTasks(new BoundingBox(0.099, 50.99, 0.111, 51.01));
            assertEquals(1, tasks.size());
            Note n = (Note) tasks.get(0);
            assertEquals("<p>ThisIsANote</p>", n.getLastComment().getText());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * get the user details
     */
    @Test
    public void userdetails() {
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mockServer.enqueue("userdetails");
        Logic logic = App.getLogic();
        UiObject snackbarTextView = mDevice.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/snackbar_text"));
        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        logic.checkForMail(main, s);
        assertTrue(snackbarTextView.waitForExists(5000));
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
            final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
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
