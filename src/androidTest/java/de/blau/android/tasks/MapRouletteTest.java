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
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MapRouletteTest {

    MockWebServerPlus    mockServerApi         = null;
    MockWebServerPlus    mockServerMapRoulette = null;
    Context              context               = null;
    AdvancedPrefDatabase prefDB                = null;
    Main                 main                  = null;
    Task                 t                     = null;
    UiDevice             device                = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-teset setup
     */
    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        LayerUtils.addTaskLayer(main);
        Set<String> filter = new HashSet<>();
        filter.add("MAPROULETTE");
        prefs.setTaskFilter(filter);
        main.getMap().setPrefs(main, prefs);

        mockServerMapRoulette = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServerMapRoulette.server().url("/api/v2/");
        System.out.println("mock maproulette url " + mockBaseUrl.toString());
        prefs.putString(R.string.config_maprouletteServer_key, mockBaseUrl.scheme() + "://" + mockBaseUrl.host() + ":" + mockBaseUrl.port() + "/");
        mockServerApi = new MockWebServerPlus();
        mockBaseUrl = mockServerApi.server().url("/api/0.6/");

        System.out.println("mock api url " + mockBaseUrl.toString());
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", false);
        prefDB.selectAPI("Test");
        prefDB.resetCurrentServer();
        prefs = new Preferences(context);
        App.getLogic().setPrefs(prefs);
        System.out.println(prefs.getServer().getReadWriteUrl());
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        LayerUtils.removeTaskLayer(main);
        App.getTaskStorage().reset();
        try {
            mockServerMapRoulette.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
    }

    /**
     * Download some MapRoulette tasks and check that a certain one exists, then re-download and check that it got
     * correctly merged
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void mapRouletteDownload() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServerMapRoulette.enqueue("maprouletteDownload");
        mockServerMapRoulette.enqueue("challenge3241");
        mockServerMapRoulette.enqueue("challenge2523");
        mockServerMapRoulette.enqueue("challenge2611");
        mockServerMapRoulette.enqueue("challenge249");
        App.getTaskStorage().reset();
        BoundingBox boundingBox = new BoundingBox(8.3733566D, 47.3468982D, 8.4748442D, 47.4476552D);
        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        try {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            Resources r = context.getResources();
            String mapRouletteSelector = r.getString(R.string.bugfilter_maproulette);
            Set<String> set = new HashSet<String>(Arrays.asList(mapRouletteSelector));
            p.edit().putStringSet(r.getString(R.string.config_bugFilter_key), set).commit();
            Assert.assertTrue(new Preferences(context).taskFilter().contains(mapRouletteSelector));
            TransferTasks.downloadBox(context, s, boundingBox, false, new SignalHandler(signal));
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
        Assert.assertEquals(10, tasks.size());
        try {
            tasks = App.getTaskStorage().getTasks(new BoundingBox(8.4470272, 47.3960161, 8.4470274, 47.3960163));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        t = tasks.get(0);
        Assert.assertTrue(t instanceof MapRouletteTask);
        Assert.assertEquals(2237667L, t.getId());
        // re-download the same bounding box
        mockServerMapRoulette.enqueue("maprouletteDownload");
        final CountDownLatch signal2 = new CountDownLatch(1);
        try {
            TransferTasks.downloadBox(context, s, boundingBox, true, new SignalHandler(signal2));
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
        Assert.assertEquals(10, tasks.size());
        try {
            tasks = App.getTaskStorage().getTasks(new BoundingBox(8.4470272, 47.3960161, 8.4470274, 47.3960163));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        t = tasks.get(0);
        Assert.assertTrue(t instanceof MapRouletteTask);
        Assert.assertEquals(2237667L, t.getId());

        Map map = main.getMap();
        Logic logic = App.getLogic();
        logic.getViewBox().setBorders(map, boundingBox);
        map.setViewBox(logic.getViewBox());
        map.invalidate();
        TestUtils.zoomToNullIsland(logic, map);
        try {
            Thread.sleep(5000); // NOSONAR
        } catch (InterruptedException e) {
        }
    }

    /**
     * Update a MapRoulette task
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void mapRouletteUpdate() {
        mapRouletteDownload();
        MapRouletteTask b = (MapRouletteTask) t; // ugly but removes code duplication
        b.setFalse();
        b.setChanged(true);
        Assert.assertTrue(b.hasBeenChanged());
        final CountDownLatch signal = new CountDownLatch(1);
        mockServerApi.enqueue("userpreferences");
        mockServerMapRoulette.enqueue("200");
        try {
            Assert.assertTrue(TransferTasks.updateMapRouletteTask(main, new Server(context, prefDB.getCurrentAPI(), "vesupucci test"), b, false,
                    new SignalHandler(signal)));
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
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void mapRouletteUpdateFail() {
        mapRouletteDownload();
        MapRouletteTask b = (MapRouletteTask) t; // ugly but removes code duplication
        b.setFalse();
        b.setChanged(true);
        Assert.assertTrue(b.hasBeenChanged());
        final CountDownLatch signal = new CountDownLatch(1);
        mockServerApi.enqueue("userpreferences");
        mockServerMapRoulette.enqueue("500");
        try {
            Assert.assertFalse(TransferTasks.updateMapRouletteTask(main, new Server(context, prefDB.getCurrentAPI(), "vesupucci test"), b, false,
                    new SignalHandler(signal)));
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

    /**
     * Close a MapRoulette task via dialog
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void mapRouletteDialog() {
        mapRouletteDownload();
        MapRouletteTask b = (MapRouletteTask) t; // ugly but removes code duplication
        TestUtils.unlock(device);
        Assert.assertTrue(TestUtils.clickAtCoordinatesWaitNewWindow(device, main.getMap(), b.getLon(), b.getLat()));
        UiObject saveButton = device.findObject(new UiSelector().resourceId("android:id/button1"));
        try {
            Assert.assertTrue(saveButton.exists());
            TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/openstreetbug_state", true);
            TestUtils.clickText(device, false, "Deleted", true, false);
            Assert.assertTrue(saveButton.isEnabled());
            TestUtils.clickText(device, false, "Save", true, false);
            Assert.assertTrue(b.isClosed());
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }
}
