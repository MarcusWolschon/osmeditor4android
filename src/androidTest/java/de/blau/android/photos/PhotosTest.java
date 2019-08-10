package de.blau.android.photos;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.rtree.RTree;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PhotosTest {

    private static final String PHOTO_FILE  = "test.jpg";
    private static final String PHOTO_FILE2 = "test2.jpg";
    Context                     context     = null;
    AdvancedPrefDatabase        prefDB      = null;
    Main                        main        = null;
    UiDevice                    mDevice     = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
        prefs.setPhotoLayerEnabled(true);
        main.getMap().setPrefs(main, prefs);
        try {
            TestUtils.copyFileFromResources(PHOTO_FILE, "Pictures");
            TestUtils.copyFileFromResources(PHOTO_FILE2, "Pictures");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {

    }

    /**
     * Select a photograph from the photo layer and show it in the internal viewer
     */
    @Test
    public void selectDisplayDelete() {
        TestUtils.zoomToLevel(main, 20);
        TestUtils.unlock();
        Assert.assertEquals(2, App.getPhotoIndex().count());
        TestUtils.clickAtCoordinates(main.getMap(), 7.5886112, 47.5519448, true);
        // Assert.assertTrue(TestUtils.findText(mDevice, false, "Done", 1000));

        TestUtils.clickMenuButton("delete", false, true);
        Assert.assertTrue(TestUtils.clickText(mDevice, false, "Delete permamently", false));
        Assert.assertTrue(TestUtils.clickText(mDevice, false, "Done", true));
        TestUtils.clickMenuButton("Go to photo", false, true);
        Assert.assertEquals(1, App.getPhotoIndex().count());
    }
}
