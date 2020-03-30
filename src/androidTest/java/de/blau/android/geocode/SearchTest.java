package de.blau.android.geocode;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.exception.IllegalOperationException;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.AdvancedPrefDatabase.Geocoder;
import de.blau.android.prefs.AdvancedPrefDatabase.GeocoderType;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SearchTest {

    public static final int TIMEOUT    = 90;
    MockWebServerPlus       mockServer = null;
    Context                 context    = null;
    AdvancedPrefDatabase    prefDB     = null;
    Main                    main       = null;
    UiDevice                device     = null;

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
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        mockServer = new MockWebServerPlus();

        prefDB = new AdvancedPrefDatabase(context);
        Geocoder[] geocoders = prefDB.getActiveGeocoders();
        for (Geocoder g : geocoders) {
            if (g.type == GeocoderType.PHOTON) {
                prefDB.deleteGeocoder(g.id);
            } else if (g.type == GeocoderType.NOMINATIM) {
                try {
                    prefDB.deleteGeocoder(g.id);
                    Assert.fail("Shouldn't be able to delete default");
                } catch (IllegalOperationException e) {
                }
            }
        }

        HttpUrl mockBaseUrl = mockServer.server().url("/");
        System.out.println("mock api url " + mockBaseUrl.toString());
        prefDB.addGeocoder("Nominatim2", "Nominatim2", GeocoderType.NOMINATIM, 0, mockBaseUrl.toString(), true);
        prefDB.addGeocoder("Photon", "Photon", GeocoderType.PHOTON, 0, mockBaseUrl.toString(), true);

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
        prefDB.deleteGeocoder("Nominatim2");
    }

    /**
     * Query Nominatim
     */
    @Test
    public void nominatim() {
        // http://nominatim.openstreetmap.org/search?q=bergdietikon&viewboxlbrt=-8.6723573%2C24.892276%2C34.6636399%2C66.2221988&format=jsonv2
        mockServer.enqueue("nominatim");
        if (!TestUtils.clickMenuButton(device, "Find", false, true)) {
            TestUtils.clickOverflowButton(device);
            TestUtils.clickText(device, false, "Find", true);
        }
        UiObject searchEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/location_search_edit"));
        try {
            searchEditText.click();
            searchEditText.setText("bergdietikon");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/location_search_geocoder", true);
        Assert.assertTrue(TestUtils.clickText(device, true, "Nominatim2", true));
        Assert.assertTrue(TestUtils.clickText(device, true, "SEARCH", true));
        Assert.assertTrue(TestUtils.findText(device, false, "Search results"));
        Assert.assertTrue(TestUtils.findText(device, false, "Bergdietikon"));
        Assert.assertTrue(TestUtils.clickText(device, false, "Bergdietikon", true));
        ViewBox vb = ((Main) main).getMap().getViewBox();
        Assert.assertEquals(47.3898401D, vb.getCenterLat(), 0.001D);
        Assert.assertEquals(8.3865262D, ((vb.getRight() - vb.getLeft()) / 2 + vb.getLeft()) / 1E7D, 0.001D);
    }

    /**
     * Query photon
     */
    @Test
    public void photon() {
        // http://photon.komoot.de/api?q=bergdietikon&lat=49.7333397512672&lon=12.9956413&limit=10
        mockServer.enqueue("photon");
        if (!TestUtils.clickMenuButton(device, "Find", false, true)) {
            TestUtils.clickOverflowButton(device);
            TestUtils.clickText(device, false, "Find", true);
        }
        UiObject searchEditText = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/location_search_edit"));
        try {
            searchEditText.click();
            searchEditText.setText("bergdietikon");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/location_search_geocoder", true);
        Assert.assertTrue(TestUtils.clickText(device, true, "Photon", true));
        Assert.assertTrue(TestUtils.clickText(device, true, "SEARCH", true));
        Assert.assertTrue(TestUtils.findText(device, false, "Search results"));
        Assert.assertTrue(TestUtils.clickTextContains(device, false, "Bergdietikon", true));
        ViewBox vb = ((Main) main).getMap().getViewBox();
        Assert.assertEquals(47.391823D, vb.getCenterLat(), 0.001D);
        Assert.assertEquals(8.3717616, ((vb.getRight() - vb.getLeft()) / 2 + vb.getLeft()) / 1E7D, 0.001D);
    }
}
