package de.blau.android.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.osm.ApiTest;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.FileUtil;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReadSaveTasks {

    private static final String TEST_OSN   = "test.osn";
    MockWebServerPlus           mockServer = null;
    Context                     context    = null;
    AdvancedPrefDatabase        prefDB     = null;
    Main                        main       = null;
    TaskStorage                 ts         = null;

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
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
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
        Assert.assertNotNull(is);
        TransferTasks.readCustomBugs(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        List<Task> tasks = ts.getTasks();
        Assert.assertEquals(2, tasks.size());
        Assert.assertTrue(tasks.get(0) instanceof CustomBug);
        CustomBug bug = (CustomBug) tasks.get(0);
        bug.close();
        final CountDownLatch signal2 = new CountDownLatch(1);
        TransferTasks.writeCustomBugFile(main, "test.json", new SignalHandler(signal2));
        try {
            signal2.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is = new FileInputStream(new File(FileUtil.getPublicDirectory(), "test.json"));
        } catch (FileNotFoundException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(is);
        final CountDownLatch signal3 = new CountDownLatch(1);
        TransferTasks.readCustomBugs(main, is, false, new SignalHandler(signal3));
        try {
            signal3.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        tasks = ts.getTasks();
        Assert.assertEquals(1, tasks.size());
        Assert.assertTrue(tasks.get(0) instanceof CustomBug);
    }

    /**
     * Save OSM Nostes to an OSN file
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
            TransferTasks.downloadBox(context, s, new BoundingBox(8.3879800D, 47.3892400D, 8.3844600D, 47.3911300D), false, Long.MAX_VALUE,
                    new SignalHandler(signal));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        try {
            signal.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        List<Task> tasks = App.getTaskStorage().getTasks();
        // note the fixture contains 100 notes, however 41 of them are closed and expired
        Assert.assertEquals(59, tasks.size());
        final CountDownLatch signal1 = new CountDownLatch(1);
        TransferTasks.writeOsnFile(main, true, TEST_OSN, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            byte[] testContent = TestUtils.readInputStream(new FileInputStream(new File(FileUtil.getPublicDirectory(), TEST_OSN)));
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream is = loader.getResourceAsStream("test-result.osn");
            byte[] correctContent = TestUtils.readInputStream(is);
            Assert.assertArrayEquals(correctContent, testContent);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
