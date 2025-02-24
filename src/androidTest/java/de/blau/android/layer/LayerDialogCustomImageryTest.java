package de.blau.android.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.MockTileServer;
import de.blau.android.PMTilesDispatcher;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.TileDispatcher;
import de.blau.android.gpx.GpxTest;
import de.blau.android.layer.data.MapOverlay;
import de.blau.android.layer.tiles.MapTilesLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LayerDialogCustomImageryTest {

    public static final int EXTENT_BUTTON = 1;
    public static final int MENU_BUTTON   = 3;

    AdvancedPrefDatabase prefDB          = null;
    Main                 main            = null;
    UiDevice             device          = null;
    Map                  map             = null;
    Instrumentation      instrumentation = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.getTargetContext().deleteDatabase(TileLayerDatabase.DATABASE_NAME);
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        main = mActivityRule.getActivity();
        TestUtils.grantPermissons(device);
        LayerUtils.removeImageryLayers(main);
        Preferences prefs = new Preferences(main);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Test adding and then modifiy custom imagery
     */
    @Test
    public void customImagery() {
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/add", true));
        TestUtils.scrollTo(main.getString(R.string.layer_add_custom_imagery), false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_add_custom_imagery), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.add_layer_title)));
        UiObject name = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/name"));
        try {
            name.setText("Custom imagery");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        UiObject url = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/url"));
        try {
            url.setText("https://test/");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save_and_set), true));
        try (TileLayerDatabase db = new TileLayerDatabase(ApplicationProvider.getApplicationContext())) {
            TileLayerSource tls = TileLayerDatabase.getLayerWithUrl(main, db.getReadableDatabase(), "https://test/");
            assertNotNull(tls);
            assertEquals("Custom imagery", tls.getName());
        }
        TestUtils.clickText(device, true, main.getString(R.string.done), true, false);
        UiObject2 menuButton = TestUtils.getLayerButton(device, "Custom imagery", MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 2000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_edit_custom_imagery_configuration), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.edit_layer_title)));
        url = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/url"));
        try {
            url.setText("https://test2/");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save), true));
        try (TileLayerDatabase db = new TileLayerDatabase(ApplicationProvider.getApplicationContext())) {
            TileLayerSource tls = TileLayerDatabase.getLayerWithUrl(main, db.getReadableDatabase(), "https://test2/");
            assertNotNull(tls);
        }
    }
    
    /**
     * Test adding custom imagery with an invalid URL
     */
    @Test
    public void customImageryBadUrl() {
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/add", true));
        TestUtils.scrollTo(main.getString(R.string.layer_add_custom_imagery), false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_add_custom_imagery), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.add_layer_title)));
        UiObject name = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/name"));
        try {
            name.setText("Custom imagery");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        UiObject url = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/url"));
        try {
            url.setText("https://test/wkid={wkid}");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save), true));
        assertTrue(TestUtils.findNotification(device, main.getString(R.string.toast_url_config_file_placeholders)));
    }

    /**
     * Test adding custom imagery with an invalid bounding box
     */
    @Test
    public void customImageryBadBox() {
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/add", true));
        TestUtils.scrollTo(main.getString(R.string.layer_add_custom_imagery), false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_add_custom_imagery), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.add_layer_title)));
        UiObject name = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/name"));
        try {
            name.setText("Custom imagery");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        UiObject url = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/url"));
        try {
            url.setText("https://test/");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        
        UiObject left = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/left"));
        try {
            left.setText("1");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save), true));
        assertTrue(TestUtils.findNotification(device, main.getString(R.string.toast_invalid_box)));
    }
    
    /**
     * Test adding a MBT source
     */
    @Test
    public void customImageryMBT() {
        final String fileName = "ersatz_background.mbt";
        try {
            JavaResources.copyFileFromResources(main, fileName, null, "/");
        } catch (IOException e) {
            fail("copying " + fileName + " failed");
        }
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/add", true));
        TestUtils.scrollTo(main.getString(R.string.layer_add_custom_imagery), false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_add_custom_imagery), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.add_layer_title)));
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/file_button", true));
        TestUtils.selectFile(device, main, null, fileName, true);
        TestUtils.clickAwayTip(device, main, 5000); // only used when the file is imported
        assertTrue(TestUtils.findText(device, false, "Vespucci Test"));
        TestUtils.findText(device, false, main.getString(R.string.save_and_set), 2000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save_and_set), true));
        assertTrue(TestUtils.textGone(device, main.getString(R.string.layer_add_custom_imagery), 2000));
        assertTrue(TestUtils.findText(device, false, "Vespucci Test")); // layer dialog
        try (TileLayerDatabase db = new TileLayerDatabase(ApplicationProvider.getApplicationContext())) {
            TileLayerSource tls = TileLayerDatabase.getLayer(main, db.getReadableDatabase(), MockTileServer.MOCK_TILE_SOURCE);
            assertNotNull(tls);
            assertEquals(TileLayerSource.TYPE_TMS, tls.getType());
            assertEquals(TileLayerSource.TileType.BITMAP, tls.getTileType());
        }
    }

    /**
     * Test adding a (local) PMTiles source
     */
    @Test
    public void customImageryLocalPMTiles() {
        final String fileName = "protomaps(vector)ODbL_firenze.pmtiles";
        try {
            JavaResources.copyFileFromResources(main, fileName, null, "/");
        } catch (IOException e) {
            fail("copying " + fileName + " failed");
        }
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/add", true));
        TestUtils.scrollTo(main.getString(R.string.layer_add_custom_imagery), false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_add_custom_imagery), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.add_layer_title)));
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/file_button", true));
        TestUtils.selectFile(device, main, null, fileName, true);
        TestUtils.clickAwayTip(device, main, 5000); // only used when the file is imported
        assertTrue(TestUtils.findText(device, false, "protomaps 2023-01"));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save_and_set), true));
        assertTrue(TestUtils.textGone(device, main.getString(R.string.layer_add_custom_imagery), 2000));
        assertTrue(TestUtils.findText(device, false, "protomaps 2023-01")); // layer dialog
        try (TileLayerDatabase db = new TileLayerDatabase(ApplicationProvider.getApplicationContext())) {
            TileLayerSource tls = TileLayerDatabase.getLayer(main, db.getReadableDatabase(), "protomaps20230118T074939Z");
            assertNotNull(tls);
            assertEquals(TileLayerSource.TYPE_PMT_3, tls.getType());
            assertEquals(TileLayerSource.TileType.MVT, tls.getTileType());
        }
    }

    /**
     * Test adding a (remote) PMTiles source
     */
    @Test
    public void customImageryRemotePMTiles() {
        final String fileName = "protomaps(vector)ODbL_firenze.pmtiles";
        try (MockWebServer tileServer = new MockWebServer()) {

            PMTilesDispatcher tileDispatcher = new PMTilesDispatcher(main, fileName);
            tileServer.setDispatcher(tileDispatcher);

            String tileUrl = tileServer.url("/").toString() + "firenze.pmtiles";

            assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
            assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/add", true));
            TestUtils.scrollTo(main.getString(R.string.layer_add_custom_imagery), false);
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_add_custom_imagery), true));
            assertTrue(TestUtils.findText(device, false, main.getString(R.string.add_layer_title)));
            UiObject url = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/url"));
            try {
                url.setText(tileUrl);
            } catch (UiObjectNotFoundException e) {
                fail(e.getMessage());
            }
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save), true));

            assertTrue(TestUtils.findText(device, false, "protomaps 2023-01"));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save_and_set), true));
            assertTrue(TestUtils.textGone(device, main.getString(R.string.layer_add_custom_imagery), 2000));
            assertTrue(TestUtils.findText(device, false, "protomaps 2023-01")); // layer dialog
            try (TileLayerDatabase db = new TileLayerDatabase(ApplicationProvider.getApplicationContext())) {
                TileLayerSource tls = TileLayerDatabase.getLayer(main, db.getReadableDatabase(), "protomaps20230118T074939Z");
                assertNotNull(tls);
                assertEquals(TileLayerSource.TYPE_PMT_3, tls.getType());
                assertEquals(TileLayerSource.TileType.MVT, tls.getTileType());
            }
        } catch (IOException e) {
            fail("setting up tiles server with " + fileName + " failed");
        }
    }
}
