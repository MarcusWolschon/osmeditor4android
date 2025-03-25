package io.vespucci.osm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.SignalHandler;
import io.vespucci.TestUtils;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Node;
import io.vespucci.osm.Relation;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Way;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.util.FileUtil;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReadSaveDataTest {

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
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
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
            signal1.await(TransferMenuTest.TIMEOUT, TimeUnit.SECONDS); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
            // Ignored
        }

        final CountDownLatch signal2 = new CountDownLatch(1);
        File output = new File(context.getExternalCacheDir(), TEST_OSM);
        try {
            logic.writeOsmFile(main, output.getAbsolutePath(), new SignalHandler(signal2));
            try {
                signal2.await(TransferMenuTest.TIMEOUT, TimeUnit.SECONDS); // NOSONAR
            } catch (InterruptedException e) { // NOSONAR
                Assert.fail(e.getMessage());
            }

            try {
                byte[] testContent = TestUtils.readInputStream(new FileInputStream(output));
                is = loader.getResourceAsStream("test-result.osm");
                byte[] correctContent = TestUtils.readInputStream(is);
                Assert.assertTrue(dataIsSame(correctContent, testContent));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        } finally {
           // output.delete();
        }
    }

    /**
     * Read a file in OSM/JOSM XML format, then write it and check if the contents are the same
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void dataReadModifySave() {
        Logic logic = App.getLogic();
        StorageDelegator delegator = App.getDelegator();

        TestUtils.loadTestData(main, "test2.osm");

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
        final CountDownLatch signal1 = new CountDownLatch(1);
        logic.writeOsmFile(main, TEST_MODIFY_OSM, new SignalHandler(signal1));
        try {
            signal1.await(TransferMenuTest.TIMEOUT, TimeUnit.SECONDS); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            Assert.fail(e.getMessage());
        }

        // read back
        try {
            InputStream is = new FileInputStream(new File(FileUtil.getPublicDirectory(), TEST_OSM));
            Assert.assertNotNull(is);
            logic.readOsmFile(main, is, false, new SignalHandler(signal1));
            try { // NOSONAR
                signal1.await(TransferMenuTest.TIMEOUT, TimeUnit.SECONDS); // NOSONAR
            } catch (InterruptedException e) { // NOSONAR
                Assert.fail(e.getMessage());
            }
            is.close();
        } catch (IOException e1) {
            // Ignored
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
                    System.out.println("Files differ at position " + i + " offset " + offset); // NOSONAR
                    return false;
                }
            }
            return true;
        }
        System.out.println("Files lengths differ by " + (correctContent.length - (testContent.length - offset))); // NOSONAR
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
            signal1.await(TransferMenuTest.TIMEOUT, TimeUnit.SECONDS); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
            // Ignored
        }
        Assert.assertEquals(57, App.getDelegator().getCurrentStorage().getNodes().size());
        Assert.assertEquals(1, App.getDelegator().getBoundingBoxes().size());
        Assert.assertEquals(new BoundingBox(124827727, 418829156, 125010324, 418968428), App.getDelegator().getLastBox());
    }
}
