package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.SignalUtils;
import de.blau.android.TestUtils;
import de.blau.android.contract.MimeTypes;
import de.blau.android.layer.LayerType;
import de.blau.android.listener.UploadListener;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.API.AuthParams;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TransferMenuTest {

    private static final String DEBUG_TAG = TransferMenuTest.class.getSimpleName();

    private static final String DOWNLOAD1_FIXTURE     = "download1";
    private static final String CAPABILITIES1_FIXTURE = "capabilities1";

    public static final int TIMEOUT = 90;

    private static final String TRANSIENT_VALUE  = "transient value";
    private static final String TRANSIENT        = "transient";
    private static final String PERSISTENT_VALUE = "persistent value";
    private static final String PERSISTENT       = "persistent";

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
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
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
        prefs.setPanAndZoomAutoDownload(false);
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
    }

    /**
     * Upload to changes (mock-)server
     */
    @Test
    public void dataUpload() {

        loadTestData();

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("changeset1");
        mockServer.enqueue("upload1");
        mockServer.enqueue("close_changeset");
        mockServer.enqueue("userdetails");

        clickUploadMenuItem();
        UploadConflictTest.fillCommentAndSource(instrumentation, device);
        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(1); // dialog upload button
        UiObject button = device.findObject(uiSelector);
        try {
            button.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        TestUtils.sleep(10000);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);
        assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        assertEquals(7L, n.getOsmVersion());
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 2807173);
        assertEquals(OsmElement.STATE_UNCHANGED, r.getState());
        assertEquals(4L, r.getOsmVersion());
        // check that we actually sent what was expected
        try {
            mockServer.takeRequest(); // capabilities query
            RecordedRequest recordedRequest = mockServer.takeRequest(); // changeset
            // this currently doesn't work
            byte[] request = recordedRequest.getBody().readByteArray();
            Changeset changeset = Changeset.parse(XmlPullParserFactory.newInstance().newPullParser(), new ByteArrayInputStream(request));
            assertEquals(UploadConflictTest.COMMENT_1, changeset.getTags().get(Tags.KEY_COMMENT));
            assertEquals("1 Tertiary", changeset.getTags().get(UploadListener.V_CREATED));
            assertEquals("1 untagged way", changeset.getTags().get(UploadListener.V_DELETED));
            assertTrue(changeset.getTags().get(UploadListener.V_MODIFIED).contains("2 unknown object to Public transport route (Legacy)"));
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

            Way w = (Way) storage.getOsmElement(Way.NAME, 210461100);
            assertNotNull(w);
            assertEquals(OsmElement.STATE_DELETED, w.getState());
        } catch (InterruptedException | SAXException | IOException | ParserConfigurationException | XmlPullParserException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Upload to changes (mock-)server
     */
    @Test
    public void dataUploadCustomTags() {

        loadTestData();

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("changeset1");
        mockServer.enqueue("upload1");
        mockServer.enqueue("close_changeset");
        mockServer.enqueue("userdetails");

        clickUploadMenuItem();
        UploadConflictTest.fillCommentAndSource(instrumentation, device);

        assertTrue(TestUtils.clickText(device, false, "Custom", true, false));

        device.wait(Until.findObject(By.clickable(true).res(device.getCurrentPackageName() + ":id/editKey")), 500);
        setCustomTag(PERSISTENT, PERSISTENT_VALUE, 0);
        TestUtils.sleep(1000);
        setCustomTag(TRANSIENT, TRANSIENT_VALUE, 2);

        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(1); // dialog upload button
        UiObject button = device.findObject(uiSelector);
        try {
            button.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        TestUtils.sleep(10000);

        // check that we actually sent what was expected
        try {
            mockServer.takeRequest(); // capabilities query
            RecordedRequest recordedRequest = mockServer.takeRequest(); // changeset
            byte[] request = recordedRequest.getBody().readByteArray();
            Changeset changeset = Changeset.parse(XmlPullParserFactory.newInstance().newPullParser(), new ByteArrayInputStream(request));
            assertEquals(PERSISTENT_VALUE, changeset.getTags().get(PERSISTENT));
            assertEquals(TRANSIENT_VALUE, changeset.getTags().get(TRANSIENT));
        } catch (InterruptedException | IOException | XmlPullParserException e) {
            fail(e.getMessage());
        }

        // now check that the tags behave as expected

        loadTestData();

        clickUploadMenuItem();
        UploadConflictTest.fillCommentAndSource(instrumentation, device);

        assertTrue(TestUtils.clickText(device, false, "Custom", true, false));
        assertTrue(TestUtils.findText(device, false, PERSISTENT, 2000));
        assertTrue(TestUtils.findText(device, false, PERSISTENT_VALUE));
        assertTrue(TestUtils.textGone(device, TRANSIENT, 1000));
    }

    /**
     * Click the upload menu item
     */
    private void clickUploadMenuItem() {
        TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_upload), true, false); // menu item

        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(1); // dialog upload button
        UiObject button = device.findObject(uiSelector);
        try {
            button.click();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
    }

    /**
     * Set tag
     */
    private void setCustomTag(String key, String value, int instance) {
        UiObject editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editKey").instance(instance));
        try {
            editText.setText(key);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/editValue").instance(instance));
        try {
            editText.setText(value);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Upload to changes (mock-)server get a 429 response
     */
    @Test
    public void dataUploadError() {

        loadTestData();

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("changeset1");
        mockServer.enqueue("429");

        TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_upload), true, false); // menu item

        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(1); // dialog upload button
        UiObject button = device.findObject(uiSelector);
        try {
            button.click();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        UploadConflictTest.fillCommentAndSource(instrumentation, device);
        try {
            button.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.upload_limit_title)));
        assertTrue(TestUtils.clickText(device, false, main.getString(android.R.string.ok), true));
    }

    /**
     * Upload to changes (mock-)server get a 412 response
     */
    @Test
    public void degenWayUploadError() {

        StorageDelegator delegator = App.getDelegator();
        App.getLogic().createCheckpoint(main, R.string.undo_action_add);
        Way way = delegator.getFactory().createWayWithNewId();
        delegator.insertElementSafe(way);
        main.invalidateOptionsMenu();
        TestUtils.sleep();

        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("changeset1");
        MockResponse response = new MockResponse();
        response.setHeader("Content-type", MimeTypes.TEXTXML);
        response.setResponseCode(412);
        response.setBody("Precondition failed: Way " + way.getOsmId() + " must have at least one node");
        mockServer.enqueue(response);

        TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_upload), true, false); // menu item

        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(1); // dialog upload button
        UiObject button = device.findObject(uiSelector);
        try {
            button.click();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        UploadConflictTest.fillCommentAndSource(instrumentation, device);
        try {
            button.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.upload_way_needs_one_node_title)));
        assertNull(delegator.getOsmElement(Way.NAME, way.getOsmId())); // node is deleted
    }

    final QueueDispatcher dispatcher = new QueueDispatcher() {

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

            if (request.getPath().contains("upload")) {
                return TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/upload1.xml").throttleBody(10, 1000, TimeUnit.MILLISECONDS);
            }
            return super.dispatch(request);
        }
    };

    /**
     * Upload to changes (mock-)server and simulate a timeout, no changes uploaded
     */
    @Test
    public void dataUploadTimeout() {
        // change timeout for the current API
        prefDB.setCurrentAPITimeout(1000);
        prefs = new Preferences(context);
        prefs.setPanAndZoomAutoDownload(false);
        App.getLogic().setPrefs(prefs);
        main.getMap().setPrefs(main, prefs);

        loadTestData();

        mockServer.setDispatcher(dispatcher);

        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/capabilities1.xml"));
        // fixture here provided by dispatcher
        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/changeset1.txt"));
        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/changeset-no-changes.xml"));

        clickUploadMenuItem();
        TestUtils.sleep();
        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(1); // dialog upload button
        UiObject button = device.findObject(uiSelector);
        try {
            button.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.upload_retry_title), 50000));
        assertTrue(TestUtils.findText(device, false, "No changes", 1000, true));
    }

    /**
     * Upload to changes (mock-)server and simulate a timeout, all changes uploaded
     */
    @Test
    public void dataUploadTimeout2() {
        // change timeout for the current API
        prefDB.setCurrentAPITimeout(1000);
        prefs = new Preferences(context);
        prefs.setPanAndZoomAutoDownload(false);
        main.getMap().setPrefs(main, prefs);

        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("update-test-data-2.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        SignalUtils.signalAwait(signal, TIMEOUT);

        mockServer.setDispatcher(dispatcher);

        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/capabilities1.xml"));
        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/changeset1.txt"));
        // fixture here provided by dispatcher
        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/changeset-12-changes.xml"));
        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "update-test-changes-2.xml"));

        clickUploadMenuItem();
        TestUtils.sleep();
        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(1); // dialog upload button
        UiObject button = device.findObject(uiSelector);
        try {
            button.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.upload_retry_title), 20000));
        assertTrue(TestUtils.findText(device, false, "However it seems as if all changes", 1000, true));
    }

    /**
     * Upload to changes (mock-)server and simulate a timeout, all changes uploaded
     */
    @Test
    public void dataUploadTimeout3() {
        // change timeout for the current API
        prefDB.setCurrentAPITimeout(1000);
        prefs = new Preferences(context);
        prefs.setPanAndZoomAutoDownload(false);
        main.getMap().setPrefs(main, prefs);

        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("update-test-data-2.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        SignalUtils.signalAwait(signal, TIMEOUT);

        // add a random node
        logic.performAdd(main, 100.0f, 100.0f);
        assertNotNull(logic.getSelectedNode());
        logic.deselectAll();

        mockServer.setDispatcher(dispatcher);

        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/capabilities1.xml"));
        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/changeset1.txt"));
        // fixture here provided by dispatcher
        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/changeset-12-changes.xml"));
        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "update-test-changes-2.xml"));

        clickUploadMenuItem();
        TestUtils.sleep();
        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(1); // dialog upload button
        UiObject button = device.findObject(uiSelector);
        try {
            button.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.upload_retry_title), 20000));
        assertTrue(TestUtils.findText(device, false, "but result was not received.", 1000, true));

        // apply changes
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.update_data), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.progress_updating_message), 5000));
        assertTrue(TestUtils.textGone(device, main.getString(R.string.progress_updating_message), 20000));
        assertEquals(1, App.getDelegator().getApiStorage().getElementCount());
    }

    /**
     * Upload to changes (mock-)server and simulate a timeout during opening a changeset
     */
    @Test
    public void dataUploadTimeout4() {
        // change timeout for the current API
        prefDB.setCurrentAPITimeout(500);
        prefs = new Preferences(context);
        prefs.setPanAndZoomAutoDownload(false);
        App.getLogic().setPrefs(prefs);
        main.getMap().setPrefs(main, prefs);

        loadTestData();

        mockServer.setDispatcher(new QueueDispatcher() {
            boolean seen;

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

                if (!seen && request.getPath().contains("changeset/create")) {
                    seen = true;
                    return TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/changeset1.txt").throttleBody(1, 1000, TimeUnit.MILLISECONDS);
                }
                return super.dispatch(request);
            }
        });

        dispatcher.enqueueResponse(TestUtils.createBinaryReponse(MimeTypes.TEXTXML, "fixtures/capabilities1.xml"));

        clickUploadMenuItem();
        TestUtils.sleep();
        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(1); // dialog upload button
        UiObject button = device.findObject(uiSelector);
        try {
            button.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.upload_retry_title), 50000));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.upload_retry_message_no_open_changeset), 1000, true));
    }

    /**
     * Clear data
     */
    @Test
    public void clearData() {
        loadTestData();
        assertFalse(App.getDelegator().getCurrentStorage().isEmpty());
        assertFalse(App.getDelegator().getApiStorage().isEmpty());
        TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_data_clear), true, false);
        TestUtils.clickText(device, false, main.getString(R.string.unsaved_data_proceed), true, false);
        assertTrue(App.getDelegator().getCurrentStorage().isEmpty());
        assertTrue(App.getDelegator().getApiStorage().isEmpty());
    }

    /**
     * Turn on pan and zoom download and see if it actually retrieves something
     */
    @Test
    public void panAndZoomDownload() {
        TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_data_clear), true, false);
        TestUtils.clickText(device, false, main.getString(R.string.unsaved_data_proceed), true, false);
        assertTrue(App.getDelegator().getCurrentStorage().isEmpty());
        assertTrue(App.getDelegator().getApiStorage().isEmpty());
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(DOWNLOAD1_FIXTURE);

        TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.menu_enable_pan_and_zoom_auto_download), true, false);

        TestUtils.clickAwayTip(device, context);
        ViewBox viewBox = main.getMap().getViewBox();
        viewBox.moveTo(main.getMap(), 0, 0);
        main.getMap().invalidate();
        TestUtils.drag(device, main.getMap(), viewBox.getCenter()[0] / 1E7D, viewBox.getCenter()[1] / 1E7D, viewBox.getCenter()[0] / 1E7D + 0.001,
                viewBox.getCenter()[1] / 1E7D + 0.0001, false, 10);

        try {
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        TestUtils.sleep(2000);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984L));
    }

    /**
     * Turn on pan and zoom download and see if it pauses when it gets an 429 message
     */
    @Test
    public void panAndZoomDownload2() {
        TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_data_clear), true, false);
        TestUtils.clickText(device, false, main.getString(R.string.unsaved_data_proceed), true, false);
        assertTrue(App.getDelegator().getCurrentStorage().isEmpty());
        assertTrue(App.getDelegator().getApiStorage().isEmpty());
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("429");

        TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.menu_enable_pan_and_zoom_auto_download), true, false);

        TestUtils.clickAwayTip(device, context);

        ViewBox viewBox = main.getMap().getViewBox();
        viewBox.moveTo(main.getMap(), 0, 0);
        main.getMap().invalidate();
        TestUtils.drag(device, main.getMap(), viewBox.getCenter()[0] / 1E7D, viewBox.getCenter()[1] / 1E7D, viewBox.getCenter()[0] / 1E7D + 0.0001,
                viewBox.getCenter()[1] / 1E7D + 0.0001, true, 10);

        try {
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.download_limit_title), 10000));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true));
        assertFalse(App.getPreferences(main).getAutoDownload());
    }

    /**
     * Download something
     */
    @Test
    public void download() {
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue(DOWNLOAD1_FIXTURE);
        App.getDelegator().reset(true);
        main.getMap().invalidate();
        TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_download_current), true, false);
        TestUtils.textGone(device, main.getString(R.string.progress_download_message), 20000);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984L));
    }

    /**
     * Download something with unknown error
     */
    @Test
    public void downloadUnknownError() {
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        mockServer.enqueue("999");
        App.getDelegator().reset(true);
        main.getMap().invalidate();
        TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_download_current), true, false);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.unknown_error_title), 10000));
    }

    /**
     * Load the test data
     */
    private void loadTestData() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        SignalUtils.signalAwait(signal, TIMEOUT);
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
    }
}
