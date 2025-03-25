package io.vespucci.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
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
import io.vespucci.Main;
import io.vespucci.TestUtils;
import io.vespucci.osm.Relation;
import io.vespucci.osm.Storage;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Way;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReadPbfTest {

    private static final String PBF_FILE = "vaduz.pbf";
    Context                     context  = null;
    AdvancedPrefDatabase        prefDB   = null;
    Main                        main     = null;
    UiDevice                    device   = null;
    File                        pbfFile  = null;

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
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        try {
            pbfFile = JavaResources.copyFileFromResources(main, PBF_FILE, null, ".");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        App.getDelegator().reset(false);
    }

    /**
     * Post test clean up
     */
    @After
    public void teardown() {
        if (pbfFile != null) {
            pbfFile.delete();
        }
    }

    /**
     * Read a PBF file and do some superficial checks that it was successfully read
     */
    @Test
    public void pbfRead() {
        StorageDelegator delegator = App.getDelegator();
        App.getLogic().performAddNode(main, 8.3874640 * 1E7D, 47.3906515 * 1E7D); // force menu
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_file), false, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_read_pbf_file), true, false));
        // warning dialog
        TestUtils.clickText(device, false, main.getString(R.string.unsaved_data_proceed), false, false);
        //
        TestUtils.selectFile(device, main, null, PBF_FILE, true);
        TestUtils.findText(device, false, "Loading", 2000); // spinner appears
        TestUtils.textGone(device, "Loading", 60000);// spinner goes away

        assertNotNull(delegator.getOsmElement(Relation.NAME, 1252853L));
        assertNotNull(delegator.getOsmElement(Way.NAME, 243055643L));
        Storage current = delegator.getCurrentStorage();
        assertEquals(3404, current.getNodes().size());
        assertEquals(391, current.getWays().size());
        assertEquals(76, current.getRelations().size());
    }
}
