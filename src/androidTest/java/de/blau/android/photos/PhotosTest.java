package de.blau.android.photos;

import java.io.File;
import java.io.IOException;

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
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.contract.Paths;
import de.blau.android.layer.LayerType;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PhotosTest {

    private static final String PHOTO_FILE  = "test.jpg";
    private static final String PHOTO_FILE2 = "test2.jpg";
    Context                     context     = null;
    AdvancedPrefDatabase        prefDB      = null;
    Main                        main        = null;
    UiDevice                    device      = null;
    File                        photo1      = null;
    File                        photo2      = null;

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
        Map map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(App.getLogic(), map);
        try {
            photo1 = JavaResources.copyFileFromResources(main, PHOTO_FILE, null, Paths.DIRECTORY_PATH_PICTURES);
            photo2 = JavaResources.copyFileFromResources(main, PHOTO_FILE2, null, Paths.DIRECTORY_PATH_PICTURES);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        if (map.getPhotoLayer() == null) {
            de.blau.android.layer.Util.addLayer(main, LayerType.PHOTO);
        }
        map.setPrefs(main, prefs);
    }

    /**
     * Post test clean up
     */
    @After
    public void teardown() {
        if (photo1 != null) {
            photo1.delete();
        }
        if (photo2 != null) {
            photo2.delete();
        }
    }

    /**
     * Select a photograph from the photo layer and show it in the internal viewer
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void selectDisplayDelete() {
        TestUtils.findText(device, false, context.getString(R.string.toast_photo_indexing_finished), 10000);
        TestUtils.textGone(device, context.getString(R.string.toast_photo_indexing_finished), 10000);
        TestUtils.zoomToLevel(device, main, 20);
        TestUtils.unlock(device);
        Assert.assertEquals(2, App.getPhotoIndex().count());
        TestUtils.clickAtCoordinates(device, main.getMap(), 7.5886112, 47.5519448, true);
        // Assert.assertTrue(TestUtils.findText(mDevice, false, "Done", 1000));

        TestUtils.clickMenuButton(device, context.getString(R.string.delete), false, true);
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.photo_viewer_delete_button), false, false));
        // Assert.assertTrue(TestUtils.clickText(device, false, "Done", true, false));
        // TestUtils.clickMenuButton(device, "Go to photo", false, true);
        // device.pressBack();
        Assert.assertEquals(1, App.getPhotoIndex().count());
    }
}
