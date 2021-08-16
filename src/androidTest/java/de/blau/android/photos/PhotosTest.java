package de.blau.android.photos;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.TestUtils;
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
            JavaResources.copyFileFromResources(main, PHOTO_FILE, null, "Pictures");
            JavaResources.copyFileFromResources(main, PHOTO_FILE2, null, "Pictures");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        if (main.getMap().getPhotoLayer() == null) {
            de.blau.android.layer.Util.addLayer(main, LayerType.PHOTO);
        }
        main.getMap().setPrefs(main, prefs);
    }

    /**
     * Select a photograph from the photo layer and show it in the internal viewer
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void selectDisplayDelete() {
        TestUtils.findText(device, false, context.getString(R.string.toast_photo_indexing_finished), 10000);
        TestUtils.textGone(device, context.getString(R.string.toast_photo_indexing_finished), 10000);
        TestUtils.zoomToLevel(device, main, 20);
        TestUtils.unlock(device);
        Assert.assertEquals(2, App.getPhotoIndex().count());
        TestUtils.clickAtCoordinates(device, main.getMap(), 7.5886112, 47.5519448, true);
        // Assert.assertTrue(TestUtils.findText(mDevice, false, "Done", 1000));

        TestUtils.clickMenuButton(device, "delete", false, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Delete permamently", false, false));
        // Assert.assertTrue(TestUtils.clickText(device, false, "Done", true, false));
        // TestUtils.clickMenuButton(device, "Go to photo", false, true);
        // device.pressBack();
        Assert.assertEquals(1, App.getPhotoIndex().count());
    }
}
