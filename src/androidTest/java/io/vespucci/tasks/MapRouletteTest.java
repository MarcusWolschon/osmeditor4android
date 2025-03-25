package io.vespucci.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
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
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.SignalHandler;
import io.vespucci.TestUtils;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Node;
import io.vespucci.osm.Server;
import io.vespucci.prefs.API;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.prefs.API.AuthParams;
import io.vespucci.tasks.MapRouletteTask;
import io.vespucci.tasks.Task;
import io.vespucci.tasks.TransferTasks;
import io.vespucci.util.Util;
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
    private Preferences  prefs;

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
        prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        LayerUtils.addTaskLayer(main);
        Set<String> filter = new HashSet<>();
        filter.add(MapRouletteTask.FILTER_KEY);
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
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
        prefDB.selectAPI("Test");
        prefDB.resetCurrentServer();
        prefs = new Preferences(context);
        App.getLogic().setPrefs(prefs);
        System.out.println(prefs.getServer().getReadWriteUrl());
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
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
        prefDB.close();
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
        mockServerMapRoulette.enqueue("challenge8021");
        mockServerMapRoulette.enqueue("challenge27589");
        mockServerMapRoulette.enqueue("challenge15221");
        mockServerMapRoulette.enqueue("challenge27158");
        mockServerMapRoulette.enqueue("challenge9400");
        mockServerMapRoulette.enqueue("challenge17368");
        mockServerMapRoulette.enqueue("challenge7385");
        mockServerMapRoulette.enqueue("challenge13209");
        mockServerMapRoulette.enqueue("challenge7386");
        mockServerMapRoulette.enqueue("challenge26955");
        mockServerMapRoulette.enqueue("challenge17807");
        mockServerMapRoulette.enqueue("challenge13087");

        BoundingBox boundingBox = new BoundingBox(8.3735696D, 47.3473070D, 8.4726104D, 47.4459883D);
        final Server s = new Server(context, prefDB.getCurrentAPI(), "vesupucci test");
        try {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            Resources r = context.getResources();
            String mapRouletteSelector = r.getString(R.string.bugfilter_maproulette);
            Set<String> set = new HashSet<>(Arrays.asList(mapRouletteSelector));
            p.edit().putStringSet(r.getString(R.string.config_bugFilter_key), set).commit();
            prefs = new Preferences(context);
            App.getLogic().setPrefs(prefs);
            main.getMap().setPrefs(main, prefs);
            assertTrue(prefs.taskFilter().contains(mapRouletteSelector));
            TransferTasks.downloadBox(context, s, boundingBox, false, TransferTasks.MAX_PER_REQUEST, new SignalHandler(signal));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            signal.await(40, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        List<Task> tasks = App.getTaskStorage().getTasks();
        //
        assertEquals(291, tasks.size());
        try {
            final BoundingBox searchBox = new BoundingBox(8.3887505, 47.4005831, 8.3887505, 47.4005831);

            tasks = App.getTaskStorage().getTasks(searchBox);
            assertFalse(tasks.isEmpty());
            t = tasks.get(0);
            assertTrue(t instanceof MapRouletteTask);
            assertEquals(129071316L, ((MapRouletteTask) t).getId());
            // re-download the same bounding box
            mockServerMapRoulette.enqueue("maprouletteDownload");
            final CountDownLatch signal2 = new CountDownLatch(1);
            try {
                TransferTasks.downloadBox(context, s, boundingBox, true, TransferTasks.MAX_PER_REQUEST, new SignalHandler(signal2));
            } catch (Exception e) {
                fail(e.getMessage());
            }
            try {
                signal2.await(40, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            tasks = App.getTaskStorage().getTasks();
            //
            assertEquals(291, tasks.size());
            tasks = App.getTaskStorage().getTasks(searchBox);
            assertEquals(1, tasks.size());
            t = tasks.get(0);
            assertTrue(t instanceof MapRouletteTask);
            assertEquals(129071316L, ((MapRouletteTask) t).getId());
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Map map = main.getMap();
        Logic logic = App.getLogic();
        logic.getViewBox().setBorders(map, boundingBox);
        map.setViewBox(logic.getViewBox());
        map.invalidate();
        TestUtils.sleep(5000);
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
        assertTrue(b.hasBeenChanged());
        final CountDownLatch signal = new CountDownLatch(1);
        mockServerApi.enqueue("userpreferences");
        mockServerMapRoulette.enqueue("200");
        try {
            assertTrue(TransferTasks.updateMapRouletteTask(main, new Server(context, prefDB.getCurrentAPI(), "vesupucci test"), prefs.getMapRouletteServer(), b,
                    false, new SignalHandler(signal)));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            signal.await(40, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertFalse(b.hasBeenChanged());
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
        assertTrue(b.hasBeenChanged());
        final CountDownLatch signal = new CountDownLatch(1);
        mockServerApi.enqueue("userpreferences");
        mockServerMapRoulette.enqueue("500");
        try {
            assertFalse(TransferTasks.updateMapRouletteTask(main, new Server(context, prefDB.getCurrentAPI(), "vesupucci test"), prefs.getMapRouletteServer(),
                    b, false, new SignalHandler(signal)));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            signal.await(40, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(b.hasBeenChanged());
    }

    /**
     * Close a MapRoulette task via dialog
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void mapRouletteDialog() {
        mapRouletteDownload();
        TestUtils.zoomToLevel(device, main, 20);
        MapRouletteTask b = (MapRouletteTask) t; // ugly but removes code duplication
        TestUtils.unlock(device);
        assertTrue(TestUtils.clickAtCoordinatesWaitNewWindow(device, main.getMap(), b.getLon(), b.getLat()));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.maproulette_task_explanations), true));
        assertTrue(TestUtils.findText(device, false, "crossing", 1000, true));
        assertTrue(TestUtils.findText(device, false, Util.elementTypeId(context, Node.NAME, 2548954034L)));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.dismiss), true));
        UiObject saveButton = device.findObject(new UiSelector().resourceId("android:id/button1"));
        try {
            assertTrue(saveButton.exists());
            TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/openstreetbug_state", true);
            TestUtils.clickText(device, false, "Deleted", true, false);
            assertTrue(saveButton.isEnabled());
            TestUtils.clickText(device, false, "Save", true, false);
            assertTrue(b.isClosed());
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
    }
}
