package de.blau.android.photos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.preference.PreferenceManager;
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
import de.blau.android.SignalUtils;
import de.blau.android.TestUtils;
import de.blau.android.contract.MimeTypes;
import de.blau.android.contract.Paths;
import de.blau.android.layer.LayerType;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ContentResolverUtil;
import de.blau.android.util.rtree.RTree;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PhotosTest {

    private static final String PHOTO_FILE  = "test.jpg";
    private static final String PHOTO_FILE2 = "test2.jpg";
    private static final String PHOTO_FILE3 = "test3.jpg";
    private static final String PHOTO_FILE4 = "test4.heic";
    private Context             context     = null;
    private Main                main        = null;
    private Preferences         prefs;
    private Map                 map;
    private UiDevice            device      = null;
    private File                photo1      = null;
    private File                photo2      = null;
    private File                photo3      = null;
    private File                photo4      = null;

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
        prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(App.getLogic(), map);
        try {
            photo1 = JavaResources.copyFileFromResources(main, PHOTO_FILE, null, Paths.DIRECTORY_PATH_PICTURES);
            photo2 = JavaResources.copyFileFromResources(main, PHOTO_FILE2, null, Paths.DIRECTORY_PATH_PICTURES);
            photo3 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), PHOTO_FILE3);
            JavaResources.copyFileFromResources(PHOTO_FILE3, null, photo3);
            final CountDownLatch signal = new CountDownLatch(1);
            MediaScannerConnection.scanFile(context, new String[] { photo3.getAbsolutePath() }, new String[] { MimeTypes.JPEG },
                    (String path, Uri uri) -> signal.countDown());
            SignalUtils.signalAwait(signal, 10);
            photo4 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), PHOTO_FILE4);
            JavaResources.copyFileFromResources(PHOTO_FILE4, null, photo4);
            final CountDownLatch signal2 = new CountDownLatch(1);
            MediaScannerConnection.scanFile(context, new String[] { photo4.getAbsolutePath() }, new String[] { MimeTypes.HEIC },
                    (String path, Uri uri) -> signal2.countDown());
            SignalUtils.signalAwait(signal2, 10);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Post test clean up
     */
    @After
    public void teardown() {
        LayerUtils.removeLayer(main, LayerType.PHOTO);
        map.setUpLayers(main);
        map.invalidate();
        PreferenceManager.getDefaultSharedPreferences(main).edit().putBoolean(main.getString(R.string.config_indexMediaStore_key), false).commit();
        try (PhotoIndex index = new PhotoIndex(main)) {
            RTree<Photo> tree = new RTree<>(2, 5);
            index.fill(tree);
            List<Photo> photos = new ArrayList<>();
            tree.query(photos);
            for (Photo p : photos) {
                try {
                    main.getContentResolver().delete(p.getRefUri(main), null, null);
                } catch (SecurityException ex) {
                    //
                }
            }
        }
        if (photo1 != null) {
            photo1.delete();
        }
        if (photo2 != null) {
            photo2.delete();
        }
        if (photo3 != null) {
            photo3.delete();
        }
    }

    /**
     * Select a photograph from the photo layer and show it in the internal viewer
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void selectDisplayInfoDelete() {
        addLayerAndIndex();
        TestUtils.unlock(device);
        assertEquals(2, App.getPhotoIndex().count());
        TestUtils.clickAtCoordinates(device, main.getMap(), 7.5886112, 47.5519448, true);

        TestUtils.clickMenuButton(device, context.getString(R.string.menu_information), false, true);
        TestUtils.sleep(5000); // seems to be needed
        
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.image_information_title)));
        // TestUtils.sleep(20000);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.done),true, false));
        
        TestUtils.clickMenuButton(device, main.getString(R.string.abc_action_menu_overflow_description), false, true, 10000);
        
        TestUtils.sleep(5000); // seems to be needed

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.delete), false));

        // TestUtils.clickMenuButton(device, context.getString(R.string.delete), false, true);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.photo_viewer_delete_button), true, false));

        assertEquals(1, App.getPhotoIndex().count());
    }

    /**
     * Turn on indexing of MediaStore
     */
    @Test
    public void indexWithMediaStore() {
        PreferenceManager.getDefaultSharedPreferences(main).edit().putBoolean(main.getString(R.string.config_indexMediaStore_key), true).commit();
        prefs = new Preferences(context);
        App.getLogic().setPrefs(prefs);
        addLayerAndIndex();
        try (PhotoIndex index = new PhotoIndex(main)) {
            RTree<Photo> tree = new RTree<>(2, 5);
            index.fill(tree);
            List<Photo> photos = new ArrayList<>();
            tree.query(photos);
            assertEquals(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? 3 : 4, photos.size());
            boolean got3 = false;
            boolean got4 = false;
            for (Photo p : photos) {
                if (PHOTO_FILE3.equals(ContentResolverUtil.getDisplaynameColumn(context, Uri.parse(p.getRef())))) {
                    got3 = true;
                }
                if (PHOTO_FILE4.equals(ContentResolverUtil.getDisplaynameColumn(context, Uri.parse(p.getRef())))) {
                    got4 = true;
                }
            }
            assertTrue(got3);
            assertTrue(got4 || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q);
        }
    }

    /**
     * Add the photo layer and wait until indexing is finished
     */
    private void addLayerAndIndex() {
        prefs = new Preferences(context);
        App.getLogic().setPrefs(prefs);
        TestUtils.zoomToLevel(device, main, 20);
        if (map.getPhotoLayer() == null) {
            de.blau.android.layer.Util.addLayer(main, LayerType.PHOTO);
        }
        map.setPrefs(main, prefs);
        map.invalidate();
        TestUtils.findText(device, false, context.getString(R.string.toast_photo_indexing_finished), 10000);
        TestUtils.textGone(device, context.getString(R.string.toast_photo_indexing_finished), 10000);
    }
}
