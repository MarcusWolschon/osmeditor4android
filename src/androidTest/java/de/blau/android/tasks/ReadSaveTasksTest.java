package de.blau.android.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.osm.ApiTest;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReadSaveTasksTest {

    private static final String TEST_RESULT_OSN = "test-result.osn";
    private static final String TEST_JSON       = "test.json";
    private static final String TEST_OSN        = "test.osn";
    MockWebServerPlus           mockServer      = null;
    Context                     context         = null;
    AdvancedPrefDatabase        prefDB          = null;
    Main                        main            = null;
    TaskStorage                 ts              = null;
    UiDevice                    device          = null;

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
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        ts = App.getTaskStorage();
        ts.reset();
        prefDB = new AdvancedPrefDatabase(context);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
    }

    /**
     * Read and save custom tasks
     */
    @Test
    public void readAndSaveCustomBugs() {
        final CountDownLatch signal1 = new CountDownLatch(1);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("customBug.json");
        assertNotNull(is);
        TransferTasks.readCustomBugs(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        List<Task> tasks = ts.getTasks();
        assertEquals(2, tasks.size());
        assertTrue(tasks.get(0) instanceof CustomBug);
        CustomBug bug = (CustomBug) tasks.get(0);
        bug.close();
        try {
            assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_bugs), true, false));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_file), true, false));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_write_custom_bugs), true, false));
            TestUtils.selectFile(device, context, null, TEST_JSON, true, true);

            assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_bugs), true, false));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_file), true, false));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_read_custom_bugs), true, false));
            TestUtils.selectFile(device, main, null, TEST_JSON, true);
            TestUtils.findText(device, false, main.getString(R.string.toast_read_successfully), 1000);
            TestUtils.textGone(device, main.getString(R.string.toast_read_successfully), 1000);
            //
            tasks = ts.getTasks();
            assertEquals(1, tasks.size());
            assertTrue(tasks.get(0) instanceof CustomBug);
        } finally {
            TestUtils.deleteFile(main, TEST_JSON);
        }
    }

    /**
     * Save OSM Notes to an OSN file
     */
    @Test
    public void saveNotes() {
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        System.out.println("mock api url " + mockBaseUrl.toString());
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", false);
        prefDB.selectAPI("Test");
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("notesDownload1");
        try {
            final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            Resources r = context.getResources();
            String notesSelector = r.getString(R.string.bugfilter_notes);
            Set<String> set = new HashSet<String>(Arrays.asList(notesSelector));
            p.edit().putStringSet(r.getString(R.string.config_bugFilter_key), set).commit();
            assertTrue(new Preferences(context).taskFilter().contains(notesSelector));
            TransferTasks.downloadBox(context, s, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, Long.MAX_VALUE,
                    new SignalHandler(signal));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            signal.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        List<Task> tasks = ts.getTasks();
        // note the fixture contains 100 notes, however 41 of them are closed and expired
        assertEquals(59, tasks.size());
        try {
            assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_bugs), true, false));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_file), true, false));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_save_notes_all), true, false));
            TestUtils.selectFile(device, context, null, TEST_OSN, true, true);

            assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_bugs), true, false));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_file), true, false));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_read_notes), true, false));
            TestUtils.selectFile(device, main, null, TEST_OSN, true);

            tasks = ts.getTasks();
            assertEquals(59, tasks.size());
        } finally {
            TestUtils.deleteFile(main, TEST_OSN);
        }
    }

    /**
     * Read notes in JOSM format from a file
     */
    @Test
    public void readNotes() {
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
                List<Task> tasks = App.getTaskStorage().getTasks();
                // see previous test
                assertEquals(59, tasks.size());
                for (Task t : tasks) {
                    if (t instanceof Note && t.getId() == 893035) {
                        return;
                    }
                }
                fail("Note 893035 not found");
            } finally {
                TestUtils.deleteFile(main, TEST_RESULT_OSN);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
