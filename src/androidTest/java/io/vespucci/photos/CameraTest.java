package io.vespucci.photos;

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
import io.vespucci.R;
import io.vespucci.LayerUtils;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.contract.Paths;
import io.vespucci.layer.LayerType;
import io.vespucci.layer.photos.MapOverlay;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.util.FileUtil;

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
        io.vespucci.layer.Util.addLayer(main, LayerType.PHOTO);
        map = main.getMap();
        map.setPrefs(main, prefs);

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
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
    @FlakyTest(detail = "This requires a camera app to be present")
    @Test
    public void takePicture() {
        MapOverlay photoLayer = map.getPhotoLayer();
        assertNotNull(photoLayer);
        int origCount = photoCount();
        TestUtils.findText(device, false, main.getString(R.string.toast_photo_indexing_started), 2000);
        TestUtils.textGone(device, main.getString(R.string.toast_photo_indexing_finished), 5000);
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_camera), false, false, 10000));
        TestUtils.clickText(device, false, "Camera", false);
        TestUtils.clickText(device, false, "Just Once", false); // FIXME needs more work
        device.waitForWindowUpdate(null, 5000);
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
            File photoDir = new File(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_PICTURES);
            photoDir.mkdirs();
            assertTrue(photoDir.exists());
            return photoDir.list().length;
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return 0;
    }
}
