package de.blau.android.osm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.FileUtil;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReadSaveData {

    private static final String TEST_OSM        = "test.osm";
    private static final String TEST_MODIFY_OSM = "test_modify.osm";
    MockWebServerPlus           mockServer      = null;
    Context                     context         = null;
    AdvancedPrefDatabase        prefDB          = null;
    Main                        main            = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        UiDevice device = UiDevice.getInstance(instrumentation);
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
    }

    /**
     * Read a file in OSM/JOSM XML format, then write it and check if the contents are the same
     */
    @Test
    public void dataReadSave() {
        final CountDownLatch signal1 = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test2.osm");
        Assert.assertNotNull(is);
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }

        final CountDownLatch signal2 = new CountDownLatch(1);
        logic.writeOsmFile(main, TEST_OSM, new SignalHandler(signal2));
        try {
            signal2.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        try {
            byte[] testContent = TestUtils.readInputStream(new FileInputStream(new File(FileUtil.getPublicDirectory(), TEST_OSM)));
            is = loader.getResourceAsStream("test-result.osm");
            byte[] correctContent = TestUtils.readInputStream(is);
            Assert.assertTrue(dataIsSame(correctContent, testContent));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Read a file in OSM/JOSM XML format, then write it and check if the contents are the same
     */
    @SdkSuppress(minSdkVersion=26)
    @Test
    public void dataReadModifySave() {
        final CountDownLatch signal1 = new CountDownLatch(1);
        Logic logic = App.getLogic();
        StorageDelegator delegator = App.getDelegator();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test2.osm");
        Assert.assertNotNull(is);
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }

        // modify, for now just deletions
        Node node = delegator.getCurrentStorage().getNode(2522882577L);
        logic.performEraseNode(main, node, true);
        Way way = delegator.getCurrentStorage().getWay(49855526L);
        logic.performEraseWay(main, way, true, true);
        Relation rel = delegator.getCurrentStorage().getRelation(6490362L);
        logic.performEraseRelation(main, rel, true);

        // check
        Assert.assertNull(delegator.getCurrentStorage().getNode(2522882577L));
        Assert.assertNotNull(delegator.getApiStorage().getNode(2522882577L));
        Assert.assertNull(delegator.getCurrentStorage().getWay(49855526L));
        Assert.assertNotNull(delegator.getApiStorage().getWay(49855526L));
        Assert.assertNull(delegator.getCurrentStorage().getRelation(6490362L));
        Assert.assertNotNull(delegator.getApiStorage().getRelation(6490362L));

        // write out
        final CountDownLatch signal2 = new CountDownLatch(1);
        logic.writeOsmFile(main, TEST_MODIFY_OSM, new SignalHandler(signal2));
        try {
            signal2.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        // read back
        try {
            is = new FileInputStream(new File(FileUtil.getPublicDirectory(), TEST_OSM));
            Assert.assertNotNull(is);
            logic.readOsmFile(main, is, false, new SignalHandler(signal1));
            try {
                signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Assert.fail(e.getMessage());
            }
            is.close();
        } catch (IOException e1) {
        }
        // check that modifications are present
        Assert.assertNull(delegator.getCurrentStorage().getNode(2522882577L));
        Assert.assertNotNull(delegator.getApiStorage().getNode(2522882577L));
        Assert.assertNull(delegator.getCurrentStorage().getWay(49855526L));
        Assert.assertNotNull(delegator.getApiStorage().getWay(49855526L));
        Assert.assertNull(delegator.getCurrentStorage().getRelation(6490362L));
        Assert.assertNotNull(delegator.getApiStorage().getRelation(6490362L));
    }

    /**
     * Compare skipping build number (roughly)
     * 
     * @param correctContent the known good content
     * @param testContent the generated content
     * @return true if "the same"
     */
    private boolean dataIsSame(byte[] correctContent, byte[] testContent) {
        int oldVersionLength = 8;
        int offset = context.getString(R.string.app_version).length() - oldVersionLength;
        if (correctContent.length == testContent.length - offset) { // this will fail if more than the build changes
            for (int i = 77 + offset; i < correctContent.length + offset; i++) {
                if (correctContent[i - offset] != testContent[i]) {
                    System.out.println("Files differ at position " + i + " offset " + offset);
                    return false;
                }
            }
            return true;
        }
        System.out.println("Files lengths differ by " + (correctContent.length - (testContent.length - offset)));
        return false;
    }

    /**
     * Read a file in Overpass (slightly non-standard) OSM XML format
     */
    @Test
    public void overpassRead() {
        final CountDownLatch signal1 = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("overpass.osm");
        Assert.assertNotNull(is);
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        Assert.assertEquals(57, App.getDelegator().getCurrentStorage().getNodes().size());
        Assert.assertEquals(1, App.getDelegator().getBoundingBoxes().size());
        Assert.assertEquals(new BoundingBox(124827727, 418829156, 125010324, 418968428), App.getDelegator().getLastBox());
    }
}
