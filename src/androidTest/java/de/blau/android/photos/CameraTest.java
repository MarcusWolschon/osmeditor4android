package de.blau.android.photos;

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

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.TestUtils;
import de.blau.android.contract.Paths;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.photos.MapOverlay;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.FileUtil;

@SuppressLint("NewApi")
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CameraTest {

    MockWebServerPlus    mockServer      = null;
    Context              context         = null;
    ActivityMonitor      monitor         = null;
    AdvancedPrefDatabase prefDB          = null;
    Instrumentation      instrumentation = null;
    UiDevice             device          = null;
    Main                 main            = null;
    Preferences          prefs           = null;
    Map                  map             = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext();
        main = mActivityRule.getActivity();
        prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        de.blau.android.layer.Util.addLayer(main, LayerType.PHOTO);
        map = main.getMap();
        map.setPrefs(main, prefs);

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Post test teardown
     */
    @After
    public void teardown() {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
            MapOverlay layer = map.getPhotoLayer();
            if (layer != null) {
                db.deleteLayer(layer.getIndex(), layer.getType());
                map.setUpLayers(context);
                map.invalidate();
                layer.discard(context);
                main.getMap().setPrefs(main, prefs);
            }
        }
    }

    /**
     * Click on the camera button, then take a photograph
     */
    @FlakyTest(detail="This requires a camera app to be present")
    @Test
    public void takePicture() {
        MapOverlay photoLayer = map.getPhotoLayer();
        assertNotNull(photoLayer);
        int origCount = photoCount();
        assertTrue(TestUtils.clickMenuButton(device, "Camera", false, true));
        TestUtils.grantPermissons(device);
        if (TestUtils.findText(device, false, "Next", 2000)) {
            TestUtils.clickText(device, false, "Next", true);
        }
        TestUtils.clickResource(device, false, "com.android.camera2:id/shutter_button", true);
        TestUtils.clickResource(device, false, "com.android.camera2:id/done_button", true);

        // should have a photo more now
        assertEquals(origCount + 1, photoCount());
    }

    /**
     * Count the photos in the photo dir
     * 
     * The index can't be used for this as adding the photo may fail because of missing exif information
     * 
     * @return number of photos
     */
    private int photoCount() {
        try {
            File photoDir = new File(FileUtil.getPublicDirectory(main), Paths.DIRECTORY_PATH_PICTURES);
            assertTrue(photoDir.exists());
            return photoDir.list().length;
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return 0;
    }
}
