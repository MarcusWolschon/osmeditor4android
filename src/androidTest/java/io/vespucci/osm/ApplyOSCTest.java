package io.vespucci.osm;

import java.io.File;
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

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.JavaResources;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.OscTestCommon;
import io.vespucci.SignalHandler;
import io.vespucci.TestUtils;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.prefs.API;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApplyOSCTest {

    Context              context = null;
    AdvancedPrefDatabase prefDB  = null;
    Main                 main    = null;
    UiDevice             device  = null;
    Logic                logic   = null;
    File                 oscFile = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        prefDB = new AdvancedPrefDatabase(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        try {
            oscFile = JavaResources.copyFileFromResources(main, OscTestCommon.OSC_FILE, null, ".");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        App.getDelegator().reset(false);
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        // load some base data
        final CountDownLatch signal1 = new CountDownLatch(1);
        logic = App.getLogic();
        logic.deselectAll();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(OscTestCommon.OSM_FILE);
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
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        API api = prefDB.getCurrentAPI();
        prefDB.setAPIDescriptors(api.id, api.name, api.url, null, api.notesurl, api.auth);
        prefDB.close();
        if (oscFile != null) {
            oscFile.delete();
        }
    }

    /**
     * Read an OSC file on existing data and do some superficial checks that that was successful, further we undo the
     * changes and check if everything reverted to the original state.
     */
    @Test
    public void readAndApply() {
        StorageDelegator delegator = App.getDelegator();

        OscTestCommon.checkInitialState(delegator);

        // apply OSC file
        TestUtils.clickMenuButton(device, "Transfer", false, false);
        TestUtils.clickText(device, false, "File", false, false);
        TestUtils.clickText(device, false, "Apply changes from OSC file", false, false);
        //
        TestUtils.selectFile(device, main, null, OscTestCommon.OSC_FILE, true);

        TestUtils.findText(device, false, "Loading", 2000); // spinner appears
        TestUtils.textGone(device, "Loading", 10000);// spinner goes away

        OscTestCommon.checkNewState(delegator);

        TestUtils.unlock(device);
        Assert.assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, false));
        TestUtils.clickText(device, false, "OK", true, false);
        TestUtils.sleep(5000);
        OscTestCommon.checkInitialState(delegator);
    }
}
