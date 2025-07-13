package de.blau.android.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
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
import de.blau.android.JavaResources;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.layer.LayerType;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.API.AuthParams;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NotesTest {

    private static final String TEST_RESULT_OSN = "test-result.osn";

    Context                   context = null;
    AdvancedPrefDatabase      prefDB  = null;
    Main                      main    = null;
    TaskStorage               ts      = null;
    UiDevice                  device  = null;
    private Map               map     = null;
    private Preferences       prefs   = null;
    private MockWebServerPlus mockServer;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        device = UiDevice.getInstance(instrumentation);
        main = mActivityRule.getActivity();
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
        prefDB.selectAPI("Test");
        prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        ts = App.getTaskStorage();
        ts.reset();
        prefDB = new AdvancedPrefDatabase(context);
        TestUtils.stopEasyEdit(main);
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        Resources r = context.getResources();
        String notesSelector = r.getString(R.string.bugfilter_notes);
        Set<String> set = new HashSet<>(Arrays.asList(notesSelector));
        p.edit().putStringSet(r.getString(R.string.config_bugFilter_key), set).commit();
        if (map.getTaskLayer() == null) {
            de.blau.android.layer.Util.addLayer(main, LayerType.TASKS);
            main.getMap().setPrefs(context, prefs);
            map.invalidate();
        }
        map.getDataLayer().setVisible(true);
        de.blau.android.layer.tasks.MapOverlay taskLayer = map.getTaskLayer();
        assertNotNull(taskLayer);
        taskLayer.setVisible(true);
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
        ts = App.getTaskStorage();
        ts.reset();
    }

    /**
     * Read notes in JOSM format from a file
     */
    public static void readNotes(@NonNull UiDevice device, @NonNull Main main) {
        try {
            File notes = JavaResources.copyFileFromResources(main, TEST_RESULT_OSN, null, "/");
            try {
                assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true));
                assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_bugs), true, false));
                assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_file), true, false));
                assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_read_notes), true, false));
                TestUtils.selectFile(device, main, null, TEST_RESULT_OSN, true);
                TestUtils.findText(device, false, main.getString(R.string.toast_read_successfully), 1000);
                TestUtils.textGone(device, main.getString(R.string.toast_read_successfully), 1000);
            } finally {
                TestUtils.deleteFile(main, TEST_RESULT_OSN);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Add a comment to an existing note and then remove it
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void addCommentAndRemove() {
        readNotes(device, main);
        List<Task> tasks = App.getTaskStorage().getTasks();
        // see previous test
        assertEquals(59, tasks.size());
        Task t = findTask(tasks, 893035L);
        if (t == null) { // just to avoid annoying sonar messages
            fail("t is null");
            return;
        }
        assertTrue(t.isOpen());
        assertFalse(((Note) t).hasBeenChanged());
        String lastComment = ((Note) t).getLastComment().getText();

        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);

        TestUtils.clickAtCoordinates(device, map, t.getLon(), t.getLat(), true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.openstreetbug_edit_title)));
        UiObject editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/openstreetbug_comment"));
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText("test");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickText(device, true, context.getString(R.string.Save), true, false));
        TestUtils.sleep();
        assertEquals("test", ((Note) t).getLastComment().getText());
        assertTrue(((Note) t).hasBeenChanged());

        TestUtils.clickAtCoordinates(device, map, t.getLon(), t.getLat(), true); // NOSONAR
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.openstreetbug_edit_title)));
        editText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/openstreetbug_comment"));
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText("");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickText(device, true, context.getString(R.string.Save), true, false));
        TestUtils.sleep();
        assertEquals(lastComment, ((Note) t).getLastComment().getText());
        assertFalse(((Note) t).hasBeenChanged());
    }

    /**
     * Close an exiting note and try to upload it, which fails because it is hidden
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void closeAndUploadNote() {
        readNotes(device, main);
        List<Task> tasks = App.getTaskStorage().getTasks();
        // see previous test
        assertEquals(59, tasks.size());
        Task t = findTask(tasks, 893035L);
        if (t == null) { // just to avoid annoying sonar messages
            fail("t is null");
            return;
        }
        assertTrue(t.isOpen());
        assertFalse(((Note) t).hasBeenChanged());

        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.sleep(2000);

        TestUtils.clickAtCoordinates(device, map, t.getLon(), t.getLat(), true);
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.openstreetbug_edit_title), 5000));

        assertTrue(TestUtils.clickResource(device, false, device.getCurrentPackageName() + ":id/openstreetbug_state", true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.closed), true));

        mockServer.enqueue("410"); // hidden note
        assertTrue(TestUtils.clickText(device, true, context.getString(R.string.transfer_download_current_upload), true, false));
        TestUtils.findText(device, false, context.getString(R.string.openstreetbug_commit_ok), 5000);
        assertFalse(t.isOpen());
        assertFalse(((Note) t).hasBeenChanged());
        TestUtils.sleep(5000);
        assertFalse(App.getTaskStorage().getTasks().contains(t));
    }

    /**
     * Get a specific note from the tasks
     * 
     * @param tasks task container
     * @param id note id
     * @return the Note or null
     */
    private Task findTask(@NonNull List<Task> tasks, long id) {
        Task t = null;
        for (int i = 0; i < tasks.size(); i++) {
            t = tasks.get(i);
            if (t instanceof Note && ((Note) t).getId() == id) {
                break;
            }
        }
        return t;
    }
}
