package de.blau.android.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.MockTileServer;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.layer.data.MapOverlay;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.views.layers.MapTilesLayer;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LayerDialogTest {

    private static final int VISIBLE_BUTTON = 0;
    public static final int  EXTENT_BUTTON  = 1;
    public static final int  MENU_BUTTON    = 3;

    AdvancedPrefDatabase prefDB          = null;
    Main                 main            = null;
    UiDevice             device          = null;
    Map                  map             = null;
    Logic                logic           = null;
    Instrumentation      instrumentation = null;
    MockWebServer        tileServer      = null;

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
        try (TileLayerDatabase db = new TileLayerDatabase(main)) {
            TileLayerSource.createOrUpdateFromAssetsSource(main, db.getWritableDatabase(), true, false);
        }
        tileServer = MockTileServer.setupTileServer(main, "ersatz_background.mbt", true);
        assertNotNull(tileServer);
        Preferences prefs = new Preferences(main);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.loadTestData(main, "test2.osm");
        map.getDataLayer().setVisible(true);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            if (tileServer != null) {
                tileServer.close();
            }
        } catch (IOException e) {
            // ignore
        }
        instrumentation.getTargetContext().deleteDatabase(TileLayerDatabase.DATABASE_NAME);
        instrumentation.waitForIdleSync();
    }

    /**
     * Show dialog, zoom to extent, hide layer, try to select object, show layer
     */
    @Test
    public void dataLayer() {
        TestUtils.zoomToLevel(device, main, 21);
        String dataLayerName = map.getDataLayer().getName();
        UiObject2 extentButton = TestUtils.getLayerButton(device, dataLayerName, EXTENT_BUTTON);
        extentButton.clickAndWait(Until.newWindow(), 2000);
        BoundingBox box = map.getViewBox();
        // <bounds minlat='47.3881338' minlon='8.3863771' maxlat='47.3908067' maxlon='8.3911514' origin='CGImap 0.6.0
        System.out.println("Viewbox extent " + box);
        System.out.println("Data layer extent " + map.getDataLayer().getExtent());
        // (6068 thorn-01.openstreetmap.org)' />
        assertEquals(8.3911514, box.getRight() / 1E7D, 0.003);
        assertEquals(8.3863771, box.getLeft() / 1E7D, 0.003);
        //
        UiObject2 visibleButton = TestUtils.getLayerButton(device, dataLayerName, VISIBLE_BUTTON);
        visibleButton.click();
        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);
        assertFalse(map.getDataLayer().isVisible());
        TestUtils.unlock(device);
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, false);
        assertFalse(TestUtils.clickText(device, false, "Toilets", false, false)); // nothing should happen
        visibleButton = TestUtils.getLayerButton(device, dataLayerName, VISIBLE_BUTTON);
        visibleButton.click();

        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);
        assertTrue(map.getDataLayer().isVisible());

        UiObject2 infoButton = TestUtils.getLayerButton(device, dataLayerName, MENU_BUTTON);
        infoButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_information), true, false));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.data_in_memory), 5000));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));
    }

    /**
     * Show dialog, zoom to extent, hide layer, try to select object, show layer
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void dataLayerPrune() {
        TestUtils.zoomToLevel(device, main, 22);
        String dataLayerName = map.getDataLayer().getName();
        StorageDelegator delegator = App.getDelegator();
        assertEquals(929, delegator.getCurrentStorage().getNodeCount());
        assertEquals(99, delegator.getCurrentStorage().getWayCount());
        assertEquals(5, delegator.getCurrentStorage().getRelationCount());
        TestUtils.unlock(device);
        TestUtils.clickAtCoordinates(device, map, 8.38782, 47.390339, true);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_nodeselect)));
        TestUtils.clickUp(device);
        UiObject2 menuButton = TestUtils.getLayerButton(device, dataLayerName, MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.prune), true, false));

        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);

        assertNotNull(delegator.getOsmElement(Node.NAME, 3465444349L));
        assertNull(delegator.getOsmElement(Way.NAME, 206010346L));

        assertEquals(387, delegator.getCurrentStorage().getNodeCount());
        assertEquals(15, delegator.getCurrentStorage().getWayCount());
        assertEquals(1, delegator.getCurrentStorage().getRelationCount());
    }

    /**
     * Show dialog, move data layer up one and then down
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void layerMove() {
        final MapOverlay dataLayer = map.getDataLayer();
        String dataLayerName = dataLayer.getName();
        UiObject2 menuButton = TestUtils.getLayerButton(device, dataLayerName, MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        int origPos = dataLayer.getIndex();
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.move_up), true, false));
        assertEquals(origPos + 1, dataLayer.getIndex());
        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);
        menuButton = TestUtils.getLayerButton(device, dataLayerName, MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.move_down), true, false));
        assertEquals(origPos, dataLayer.getIndex());
        TestUtils.clickText(device, true, main.getString(R.string.done), false, false);
    }

    /**
     * Find task layer and enable
     */
    @Test
    public void taskLayer() {
        LayerUtils.addTaskLayer(main);
        assertNotNull(main.getMap().getTaskLayer());
        Preferences prefs = App.getLogic().getPrefs();
        assertTrue(prefs.taskFilter().contains(main.getString(R.string.bugfilter_osmose_warning)));
        UiObject2 menuButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_tasks), MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_layers_configure), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.bugfilter_osmose_warning_entry), false, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));
        prefs = App.getLogic().getPrefs();
        assertFalse(prefs.taskFilter().contains(main.getString(R.string.bugfilter_osmose_warning)));

        menuButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_tasks), MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_information), true, false));
        assertTrue(TestUtils.findText(device, false, "MapRoulette"));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));

        menuButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_tasks), MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.discard), true, false));
        assertNull(main.getMap().getTaskLayer());
    }

    /**
     * Find grid layer, move up and switch to imperial
     */
    @Test
    public void gridLayer() {
        TestUtils.unlock(device); // so that grid gets displayed
        assertNotNull(main.getMap().getLayer(LayerType.SCALE));
        Preferences prefs = App.getLogic().getPrefs();
        assertEquals(main.getString(R.string.scale_metric), prefs.scaleLayer());
        // note grid has no extent button
        UiObject2 menuButton = TestUtils.getLayerButton(device, main.getString(R.string.layer_grid), MENU_BUTTON - 1);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_layers_configure), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.scale_imperial_entry), false, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));
        prefs = App.getLogic().getPrefs();
        assertEquals(main.getString(R.string.scale_imperial), prefs.scaleLayer());
    }

    /**
     * Load geojson file and check if we can zoom to extent, style, discard
     */
    @Test
    public void geoJsonLayer() {
        final String geoJsonFile = "featureCollection.geojson";
        try {
            JavaResources.copyFileFromResources(main, geoJsonFile, "geojson/", "/");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_layers_load_geojson), true, false));
        TestUtils.selectFile(device, main, null, geoJsonFile, true);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.Done), true, false));
        UiObject2 extentButton = TestUtils.getLayerButton(device, geoJsonFile, EXTENT_BUTTON);
        extentButton.click();

        UiObject2 menuButton = TestUtils.getLayerButton(device, geoJsonFile, MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        de.blau.android.layer.geojson.MapOverlay layer = map.getGeojsonLayer();
        assertEquals("", layer.getLabel("ignored"));
        assertEquals(Map.SHOW_LABEL_LIMIT, layer.getLabelMinZoom("ignored"));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_change_style), true, false));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.none), false));
        assertTrue(TestUtils.findText(device, false, "prop0", 1000));
        assertTrue(TestUtils.clickText(device, false, "prop0", false));
        assertTrue(TestUtils.findText(device, false, Integer.toString(Map.SHOW_LABEL_LIMIT), 1000));
        assertTrue(TestUtils.clickText(device, false, "-", false));
        assertTrue(TestUtils.findText(device, false, Integer.toString(Map.SHOW_LABEL_LIMIT - 1), 1000));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.okay), true, false));
        TestUtils.sleep();
        assertEquals("prop0", layer.getLabel("ignored"));
        assertEquals(Map.SHOW_LABEL_LIMIT - 1, layer.getLabelMinZoom("ignored"));
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_information), true, false));
        assertTrue(TestUtils.findText(device, false, "MultiLineString"));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true, false));
        TestUtils.sleep();
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.discard), true, false));
        assertNull(map.getGeojsonLayer());
    }

    /**
     * Set to "mapnik"
     */
    @Test
    public void backgroundLayer() {
        UiObject2 menuButton = TestUtils.getLayerButton(device, "Vespucci Test", MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.layer_select_imagery), true, false));
        TestUtils.clickText(device, true, main.getString(R.string.okay), true, false); // for the tip alert
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.layer_category_all), true, false));
        TestUtils.scrollTo("OpenStreetMap (Standard)", false);
        UiObject2 text = TestUtils.findObjectWithText(device, false, "OpenStreetMap (Standard)", 1000, false);
        List<UiObject2> children = text.getParent().getChildren();
        assertNotNull(children.get(1).clickAndWait(Until.newWindow(), 1000));
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.done), true, false));
        assertNotNull(text.clickAndWait(Until.newWindow(), 1000));
        TestUtils.sleep();
        main.getMap().invalidate();
        TestUtils.sleep();
        MapTilesLayer<?> layer = main.getMap().getBackgroundLayer();
        assertNotNull(layer);
        assertEquals(TileLayerSource.LAYER_MAPNIK, layer.getTileLayerConfiguration().getId());
    }

    /**
     * Display background properties dialog
     */
    @Test
    public void backgroundProperties() {
        Preferences prefs = App.getLogic().getPrefs();
        assertEquals(0.0, prefs.getContrastValue(), 0.01);
        UiObject2 menuButton = TestUtils.getLayerButton(device, "Vespucci Test", MENU_BUTTON);
        menuButton.clickAndWait(Until.newWindow(), 1000);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_tools_background_properties), true, false));

        UiObject seekbar = TestUtils.findObjectWithResourceId(device, false, device.getCurrentPackageName() + ":id/background_contrast_seeker");
        try {
            seekbar.swipeRight(10); // this should slide completely to the right
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.okay), true, false));
        TestUtils.sleep();
        prefs = App.getLogic().getPrefs();
        assertEquals(1.0, prefs.getContrastValue(), 0.01);
    }

    /**
     * Checks if filters are correct
     */
    @Test
    public void layerFilter() {
        try (TileLayerDatabase db = new TileLayerDatabase(ApplicationProvider.getApplicationContext())) {
            TileLayerSource.addOrUpdateCustomLayer(main, db.getWritableDatabase(), "TERRAINTEST", null, -1, -1, "Terrain Test", null,
                    TileLayerSource.Category.elevation, TileLayerSource.TYPE_TMS, null, 0, 19, TileLayerSource.DEFAULT_TILE_SIZE, false, "");
            TileLayerSource.getListsLocked(main, db.getReadableDatabase(), true);
        }
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
        assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/add", true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_layers_add_backgroundlayer), true, false));
        TestUtils.clickText(device, true, main.getString(R.string.okay), true, false);
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.layer_category_all), true, false));
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.layer_category_photo), true, false));
        assertTrue(TestUtils.clickText(device, true, main.getString(R.string.layer_category_elevation), true, false));
        assertTrue(TestUtils.clickText(device, true, "Terrain Test", false, false));
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
}
