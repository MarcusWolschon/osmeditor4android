package de.blau.android.osm;

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
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
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
import de.blau.android.resources.TileLayerServer;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.validation.Validator;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApiTest {

    public static final int TIMEOUT    = 90;
    MockWebServerPlus       mockServer = null;
    Context                 context    = null;
    AdvancedPrefDatabase    prefDB     = null;
    Main                    main       = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        System.out.println("mock api url " + mockBaseUrl.toString());
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", null, false);
        prefDB.selectAPI("Test");
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
    }

    /**
     * Get API capabilities
     */
    @Test
    public void capabilities() {
        mockServer.enqueue("capabilities1");

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        Capabilities result = s.getCapabilities();

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getMinVersion(), "0.6");
        Assert.assertEquals(result.getGpxStatus(), Capabilities.Status.ONLINE);
        Assert.assertEquals(result.getApiStatus(), Capabilities.Status.ONLINE);
        Assert.assertEquals(result.getDbStatus(), Capabilities.Status.ONLINE);
        Assert.assertEquals(result.getMaxWayNodes(), 2001);
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

        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984));

        // check that we have parsed and post processed relations correctly
        Relation r1 = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 1638705);
        Relation r2 = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        Relation parent = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2078158);
        Assert.assertTrue(r1.hasParentRelation(2078158));
        Assert.assertTrue(r2.hasParentRelation(2078158));
        Assert.assertNotNull(parent.getMember(r1));
        Assert.assertNotNull(parent.getMember(r2));
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
        Map<String, String> tags = new TreeMap<String, String>(n.getTags());
        tags.put(Tags.KEY_NAME, "dietikonBerg");
        try {
            logic.setTags(main, Node.NAME, 101792984L, tags);
        } catch (OsmIllegalOperationException e1) {
            Assert.fail(e1.getMessage());
        }

        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download2");
        logic.downloadBox(main, new BoundingBox(8.3838500D, 47.3883000D, 8.3865200D, 47.3898500D), true, new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984L));
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        n.hasTag(Tags.KEY_NAME, "dietikonBerg");

        // test timestamp related stuff, no point in making a separate test
        Node t = (Node) App.getDelegator().getOsmElement(Node.NAME, 3465444349L);
        Assert.assertNotNull(t);
        Assert.assertTrue(t.hasTag("amenity", "toilets"));
        Assert.assertEquals(1429452889, t.getTimestamp()); // 2015-04-19T14:14:49Z
        Assert.assertTrue(t.hasProblem(main, App.getDefaultValidator(main)) != Validator.OK);
    }

    /**
     * Fetch multiple elements in one call
     */
    @Test
    public void dataDownloadMultiFetch() {
        final CountDownLatch signal = new CountDownLatch(1);
        // mockServer.enqueue("capabilities1");
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

        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 573380242L));
        Assert.assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 35479116L));
    }

    /**
     * Down load a Relation with members
     */
    @Test
    public void dataDownloadElement() {
        final CountDownLatch signal = new CountDownLatch(1);
        // mockServer.enqueue("capabilities1");
        mockServer.enqueue("elementfetch1");
        Logic logic = App.getLogic();

        logic.downloadElement(main, Relation.NAME, 2807173L, true, false, new SignalHandler(signal));

        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(App.getDelegator().getOsmElement(Relation.NAME, 2807173L));
        Assert.assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 416426192L));
        Assert.assertNotNull(App.getDelegator().getOsmElement(Way.NAME, 104364414L));
    }

    @Test
    public void dataUpload() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertEquals(App.getDelegator().getApiElementCount(), 32);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        Assert.assertNotNull(n);
        Assert.assertEquals(n.getState(), OsmElement.STATE_MODIFIED);

        mockServer.enqueue("capabilities1");
        mockServer.enqueue("changeset1");
        mockServer.enqueue("upload1");
        mockServer.enqueue("close_changeset");

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", true);
        } catch (OsmServerException e) {
            Assert.fail(e.getMessage());
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        } catch (ProtocolException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertEquals(50000, s.getCachedCapabilities().getMaxElementsInChangeset());
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        Assert.assertNotNull(n);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        Assert.assertEquals(7L, n.getOsmVersion());
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, r.getState());
        Assert.assertEquals(4L, r.getOsmVersion());
    }

    @Test
    public void dataUploadUnchanged() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertEquals(App.getDelegator().getApiElementCount(), 32);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        Assert.assertNotNull(n);
        Assert.assertEquals(n.getState(), OsmElement.STATE_MODIFIED);
        n.setState(OsmElement.STATE_UNCHANGED);

        mockServer.enqueue("capabilities1");
        mockServer.enqueue("changeset1");
        mockServer.enqueue("upload7");
        mockServer.enqueue("close_changeset");

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", true);
        } catch (OsmServerException e) {
            Assert.fail(e.getMessage());
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        } catch (ProtocolException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void dataUploadSplit() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals(App.getDelegator().getApiElementCount(), 32);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        Assert.assertNotNull(n);
        Assert.assertEquals(OsmElement.STATE_MODIFIED, n.getState());
        Assert.assertEquals(6L, n.getOsmVersion());

        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604);
        Assert.assertNotNull(w);
        Assert.assertEquals(OsmElement.STATE_MODIFIED, w.getState());
        Assert.assertEquals(18L, w.getOsmVersion());

        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        Assert.assertNotNull(r);
        Assert.assertEquals(OsmElement.STATE_MODIFIED, r.getState());
        Assert.assertEquals(3L, r.getOsmVersion());

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
            App.getDelegator().uploadToServer(s, "TEST", "none", false);
        } catch (OsmServerException e) {
            Assert.fail(e.getMessage());
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        } catch (ProtocolException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertEquals(11, s.getCachedCapabilities().getMaxElementsInChangeset());
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        Assert.assertNotNull(n);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        Assert.assertEquals(7L, n.getOsmVersion());
        w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604);
        Assert.assertNotNull(w);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, w.getState());
        Assert.assertEquals(19L, w.getOsmVersion());
        r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        Assert.assertNotNull(r);
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, r.getState());
        Assert.assertEquals(4L, r.getOsmVersion());
    }

    @Test
    public void dataUploadErrors() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        // we need something changes in memory or else we wont try to upload
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(App.getDelegator().getApiElementCount() > 0);
        uploadErrorTest(401);
        uploadErrorTest(403);
        uploadErrorTest(999);
    }

    private void uploadErrorTest(int code) {
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("" + code);

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        s.resetChangeset();
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", true);
        } catch (OsmServerException e) {
            System.out.println(e.getMessage());
            Assert.assertEquals(code, e.getErrorCode());
            return;
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
            return;
        } catch (ProtocolException e) {
            Assert.fail(e.getMessage());
            return;
        } catch (IOException e) {
            Assert.fail(e.getMessage());
            return;
        }
        Assert.fail("Expected error " + code);
    }

    @Test
    public void dataUploadErrorInResult() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertEquals(App.getDelegator().getApiElementCount(), 32);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        Assert.assertNotNull(n);
        Assert.assertEquals(OsmElement.STATE_MODIFIED, n.getState());

        mockServer.enqueue("capabilities1");
        mockServer.enqueue("changeset1");
        mockServer.enqueue("upload5");
        mockServer.enqueue("close_changeset");
        mockServer.enqueue("changeset1");
        mockServer.enqueue("upload6");
        mockServer.enqueue("close_changeset");

        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        try {
            App.getDelegator().uploadToServer(s, "TEST", "none", true);
            Assert.fail("Expected ProtocolException");
        } catch (ProtocolException e) {
        } catch (OsmServerException e) {
            Assert.fail(e.getMessage());
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertEquals(1, App.getDelegator().getApiElementCount());
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
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Doenload Notes for a bounding box
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
            Assert.assertTrue(new Preferences(context).taskFilter().contains(notesSelector));
            TransferTasks.downloadBox(context, s, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false, new SignalHandler(signal));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        List<Task> tasks = App.getTaskStorage().getTasks();
        // note the fixture contains 100 notes, however 41 of them are closed and expired
        Assert.assertEquals(59, tasks.size());
        try {
            tasks = App.getTaskStorage().getTasks(new BoundingBox(-0.0918, 51.532, -0.0917, 51.533));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(tasks.get(0) instanceof Note);
        Assert.assertEquals(458427, tasks.get(0).getId());
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
            Assert.assertTrue(n.isNew());
            Assert.assertTrue(TransferTasks.uploadNote(main, s, n, "ThisIsANote", false, false, new SignalHandler(signal)));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            List<Task> tasks = App.getTaskStorage().getTasks(new BoundingBox(0.099, 50.99, 0.111, 51.01));
            Assert.assertEquals(1, tasks.size());
            Note n = (Note) tasks.get(0);
            Assert.assertEquals("<p>ThisIsANote</p>", n.getLastComment().getText());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
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
        UiObject snackbarTextView = mDevice.findObject(new UiSelector().resourceId("de.blau.android:id/snackbar_text"));
        logic.checkForMail(main);
        Assert.assertTrue(snackbarTextView.waitForExists(5000));
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
            Assert.assertEquals(2,preferences.size());
            Assert.assertEquals("public", preferences.get("gps.trace.visibility"));
            RecordedRequest request1 = mockServer.takeRequest();
            Assert.assertEquals("GET", request1.getMethod().toUpperCase());
            Assert.assertEquals("/api/0.6/user/preferences", request1.getPath());
            s.setUserPreference("gps.trace.visibility", "private");
            RecordedRequest request2 = mockServer.takeRequest();
            Assert.assertEquals("PUT", request2.getMethod().toUpperCase());
            Assert.assertEquals("/api/0.6/user/preferences/gps.trace.visibility", request2.getPath());
            Assert.assertEquals("private", request2.getBody().readUtf8());
            s.deleteUserPreference("gps.trace.visibility");
            RecordedRequest request3 = mockServer.takeRequest();
            Assert.assertEquals("DELETE", request3.getMethod().toUpperCase());
            Assert.assertEquals("/api/0.6/user/preferences/gps.trace.visibility", request3.getPath());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }           
    } 
}
