package de.blau.android.osm;

import java.io.IOException;

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
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReadPbfTest {

    private static final String PBF_FILE = "vaduz.pbf";
    Context                     context  = null;
    AdvancedPrefDatabase        prefDB   = null;
    Main                        main     = null;
    UiDevice                    device   = null;

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
        try {
            JavaResources.copyFileFromResources(main, PBF_FILE, null, ".");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        App.getDelegator().reset(false);
    }

    /**
     * Read a PBF file and do some superficial checks that it was successfully read
     */
    @Test
    public void pbfRead() {
        TestUtils.clickMenuButton(device, "Transfer", false, false);
        TestUtils.clickText(device, false, "File", false, false);
        TestUtils.clickText(device, false, "Read from PBF file", false, false);
        //
        TestUtils.selectFile(device, main, null, PBF_FILE, false);
        TestUtils.findText(device, false, "Loading", 2000); // spinner appears
        TestUtils.textGone(device, "Loading", 60000);// spinner goes away
        StorageDelegator delegator = App.getDelegator();
        Assert.assertNotNull(delegator.getOsmElement(Relation.NAME, 1252853L));
        Assert.assertNotNull(delegator.getOsmElement(Way.NAME, 243055643L));
        Storage current = delegator.getCurrentStorage();
        Assert.assertEquals(3404, current.getNodes().size());
        Assert.assertEquals(391, current.getWays().size());
        Assert.assertEquals(76, current.getRelations().size());
    }
}
