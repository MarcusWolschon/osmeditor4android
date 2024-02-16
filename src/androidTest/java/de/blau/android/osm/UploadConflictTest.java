package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.SignalUtils;
import de.blau.android.TestUtils;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UploadConflictTest {

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
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", API.Auth.BASIC);
        prefDB.selectAPI("Test");
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
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
    }

    /**
     * Version conflict use the local element
     */
    @Test
    public void versionConflictUseLocal() {
        versionConflict("conflict1", new String[] { "conflictdownload1" }, false, R.string.upload_conflict_message_version);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.resolve), true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.use_local_version), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.confirm_upload_title), 5000));
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        assertEquals(7, n.getOsmVersion()); // version should now be the same as the server
    }

    /**
     * Version conflict use the server element
     */
    @Test
    public void versionConflictUseServer() {
        versionConflict("conflict1", new String[] { "conflictdownload1" }, false, R.string.upload_conflict_message_version);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        assertNotNull(n);
        assertEquals(6, n.getOsmVersion()); // version should now be server and not in the API
        assertTrue(n.hasTagKey(Tags.KEY_IS_IN));
        assertEquals(OsmElement.STATE_MODIFIED, n.getState());
        assertNotNull(App.getDelegator().getApiStorage().getNode(101792984L));
        mockServer.enqueue("conflictdownload1");
        mockServer.enqueue("empty");
        mockServer.enqueue("empty");
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.resolve), true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.use_server_version), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.confirm_upload_title), 20000));
        n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        assertEquals(7, n.getOsmVersion()); // version should now be server and not in the API
        assertFalse(n.hasTagKey(Tags.KEY_IS_IN));
        assertEquals(OsmElement.STATE_UNCHANGED, n.getState());
        assertNull(App.getDelegator().getApiStorage().getNode(101792984L));
    }

    /**
     * Server side element is already deleted
     * 
     * New code retries automatically
     */
    @Test
    public void severElementAlreadyDeleted() {
        versionConflict("conflict2", new String[] { "410" }, false, -1);
        mockServer.enqueue("200");
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.progress_uploading_message), 10000));
        TestUtils.sleep(5000);
        assertNull(App.getDelegator().getApiStorage().getWay(210461100L));
        assertNull(App.getDelegator().getOsmElement(Way.NAME, 210461100L));
    }

    /**
     * Server side element is still in use
     */
    @Test
    public void severElementInUse() {
        versionConflict("conflict3", new String[] { "way-210461100", "way-210461100-nodes", "relation-12345", "relation-12345", "empty" }, false, -1);
        Way w = App.getDelegator().getApiStorage().getWay(210461100L);
        assertNotNull(w);
        assertEquals(OsmElement.STATE_DELETED, w.getState());

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.resolve), true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.deleting_references_on_server), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.confirm_upload_title), 20000));

        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 12345L);
        assertNotNull(r);
        assertEquals(OsmElement.STATE_MODIFIED, r.getState());
        assertNull(r.getMember(Way.NAME, 210461100L));
    }

    /**
     * References to server side elements that are deleted
     */
    @Test
    public void referencesMissing() {
        versionConflict("conflict4", new String[] { "way-27009604", "way-27009604-nodes", "nodes-deleted" }, false,
                R.string.upload_conflict_message_missing_references);
        Way w = App.getDelegator().getApiStorage().getWay(27009604L);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.cancel), true));
    }

    /**
     * Upload to changes (mock-)server and wait for version conflict dialog
     * 
     * @param conflictReponse the response
     * @param fixtures name of additional fixtures with the response to the upload
     * @param userDetails if true enqueue user details
     * @param waitForDialog wait for the conflict dialog if true
     */
    private void versionConflict(@NonNull String conflictReponse, @NonNull String[] fixtures, boolean userDetails, int titleRes) {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        SignalUtils.signalAwait(signal, TIMEOUT);

        mockServer.enqueue("capabilities1"); // for whatever reason this gets asked for twice
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("changeset1");
        mockServer.enqueue(conflictReponse);
        if (userDetails) {
            mockServer.enqueue("userdetails");
        }
        for (String fixture : fixtures) {
            mockServer.enqueue(fixture);
        }

        TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_upload), true, false); // menu item

        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(1); // dialog upload button
        UiObject button = device.findObject(uiSelector);
        try {
            button.click();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        fillCommentAndSource(instrumentation, device);
        try {
            button.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
        if (titleRes > 0) {
            assertTrue(TestUtils.findText(device, false, main.getString(titleRes), 10000));
        }
    }

    /**
     * Fill our comment and source fields
     * 
     * @param instrumentation Instrumentation
     * @param device Device
     */
    public static void fillCommentAndSource(@NonNull Instrumentation instrumentation, @NonNull UiDevice device) {
        device.waitForIdle();
        TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/upload_comment_clear", false);
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/upload_comment", true));
        instrumentation.sendStringSync(COMMENT_1);
        instrumentation.waitForIdleSync();
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_ENTER); // this makes the dropdown disappear
        TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/upload_source_clear", false);
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/upload_source", false));
        instrumentation.sendStringSync(SOURCE_1);
        instrumentation.waitForIdleSync();
        instrumentation.sendCharacterSync(KeyEvent.KEYCODE_ENTER);
        instrumentation.waitForIdleSync();
        device.pressBack();
    }
}
