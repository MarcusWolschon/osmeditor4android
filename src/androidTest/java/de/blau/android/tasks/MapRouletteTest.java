package de.blau.android.tasks;

import java.io.IOException;
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
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MapRouletteTest {

    MockWebServerPlus    mockServer = null;
    Context              context    = null;
    AdvancedPrefDatabase prefDB     = null;
    Main                 main       = null;
    Task                 t          = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-teset setup
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
        HttpUrl mockBaseUrl = mockServer.server().url("/api/v2/");
        System.out.println("mock api url " + mockBaseUrl.toString());
        prefs.putString(R.string.config_maprouletteServer_key, mockBaseUrl.scheme() + "://" + mockBaseUrl.host() + ":" + mockBaseUrl.port() + "/");
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
     * Download some MapRoulette tasks and check that a certain one exists, then re-download and check that it got correctly merged
     */
    @Test
    public void mapRouletteDownload() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("maprouletteDownload");
        mockServer.enqueue("challenge3241");
        mockServer.enqueue("challenge2523");
        mockServer.enqueue("challenge2611");
        mockServer.enqueue("challenge249");
        App.getTaskStorage().reset();
        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        try {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            Resources r = context.getResources();
            String mapRouletteSelector = r.getString(R.string.bugfilter_maproulette);
            Set<String> set = new HashSet<String>(Arrays.asList(mapRouletteSelector));
            p.edit().putStringSet(r.getString(R.string.config_bugFilter_key), set).commit();
            Assert.assertTrue(new Preferences(context).taskFilter().contains(mapRouletteSelector));
            TransferTasks.downloadBox(context, s, new BoundingBox(8.3733566D, 47.3468982D, 8.4748442D, 47.4476552D), false, new SignalHandler(signal));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        try {
            signal.await(40, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        List<Task> tasks = App.getTaskStorage().getTasks();
        //
        Assert.assertEquals(11, tasks.size());
        try {
            tasks = App.getTaskStorage().getTasks(new BoundingBox(8.4470272, 47.3960161, 8.4470274, 47.3960163));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        t = tasks.get(0);
        Assert.assertTrue(t instanceof MapRouletteTask);
        Assert.assertEquals(2237667L, t.getId());
        // re-download the same bounding box
        mockServer.enqueue("maprouletteDownload");
        mockServer.enqueue("challenge3241");
        mockServer.enqueue("challenge2523");
        mockServer.enqueue("challenge2611");
        mockServer.enqueue("challenge249");
        final CountDownLatch signal2 = new CountDownLatch(1);
        try {
            TransferTasks.downloadBox(context, s, new BoundingBox(8.3733566D, 47.3468982D, 8.4748442D, 47.4476552D), true, new SignalHandler(signal2));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        try {
            signal2.await(40, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        tasks = App.getTaskStorage().getTasks();
        //
        Assert.assertEquals(11, tasks.size());
        try {
            tasks = App.getTaskStorage().getTasks(new BoundingBox(8.4470272, 47.3960161, 8.4470274, 47.3960163));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        t = tasks.get(0);
        Assert.assertTrue(t instanceof MapRouletteTask);
        Assert.assertEquals(2237667L, t.getId());
    }

    /**
     * Update a MapRoulette task
     */
    @Test
    public void mapRouletteUpdate() {
        mapRouletteDownload();
        MapRouletteTask b = (MapRouletteTask) t; // ugly but removes code duplication
        b.setFalse();
        b.setChanged(true);
        Assert.assertTrue(b.hasBeenChanged());
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("200");
        try {
            Assert.assertTrue(TransferTasks.updateMapRouletteTask(context, b, false, new SignalHandler(signal)));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        try {
            signal.await(40, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertFalse(b.hasBeenChanged());
    }

    /**
     * Check that we handle error messages from the MapRoulette server correctly
     */
    @Test
    public void osmoseUploadFail() {
        mapRouletteDownload();
        OsmoseBug b = (OsmoseBug) t; // ugly but removes code duplication
        b.setFalse();
        b.setChanged(true);
        Assert.assertTrue(b.hasBeenChanged());
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("500");
        try {
            Assert.assertFalse(TransferTasks.updateOsmoseBug(context, b, false, new SignalHandler(signal)));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        try {
            signal.await(40, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(b.hasBeenChanged());
    }
}
