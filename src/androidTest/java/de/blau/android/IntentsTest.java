package de.blau.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import de.blau.android.contract.MimeTypes;
import de.blau.android.layer.LayerDialogTest;
import de.blau.android.osm.Node;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.API.AuthParams;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.OsmoseBug;
import de.blau.android.tasks.Task;
import de.blau.android.util.FileUtil;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class IntentsTest {

    MockWebServerPlus    mockServer       = null;
    MockWebServerPlus    mockServerNotes  = null;
    MockWebServerPlus    mockServerOsmose = null;
    Context              context          = null;
    ActivityMonitor      geoMonitor       = null;
    ActivityMonitor      rcMonitor        = null;
    ActivityMonitor      mainMonitor      = null;
    AdvancedPrefDatabase prefDB           = null;
    Instrumentation      instrumentation  = null;
    UiDevice             device           = null;
    Main                 main             = null;

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
        App.getDelegator().reset(false);
        App.getTaskStorage().reset();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        prefs.setTaskFilter(null);
        final Map map = main.getMap();
        map.setPrefs(main, prefs);
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        System.out.println("mock api url " + mockBaseUrl.toString());
        mockServerNotes = new MockWebServerPlus();
        HttpUrl mockNotesUrl = mockServerNotes.server().url("/api/0.6/");
        System.out.println("mock notes api url " + mockNotesUrl.toString());
        //
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, mockNotesUrl.toString(), new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
        prefDB.selectAPI("Test");
        mockServerOsmose = new MockWebServerPlus();
        mockBaseUrl = mockServerOsmose.server().url("/en/api/0.2/");
        prefs.putString(R.string.config_osmoseServer_key, mockBaseUrl.scheme() + "://" + mockBaseUrl.host() + ":" + mockBaseUrl.port() + "/");
        App.getLogic().setPrefs(prefs);
        LayerUtils.addTaskLayer(main);
        map.setUpLayers(main);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        App.getDelegator().reset(false);
        App.getTaskStorage().reset();
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        LayerUtils.removeTaskLayer(main);
        if (geoMonitor != null) {
            instrumentation.removeMonitor(geoMonitor);
        }
        if (rcMonitor != null) {
            instrumentation.removeMonitor(rcMonitor);
        }
        try {
            mockServer.server().shutdown();
            mockServerNotes.server().shutdown();
            mockServerOsmose.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Test that geo intents work
     */
    @Test
    public void geo() {
        geoMonitor = instrumentation.addMonitor(GeoUrlActivity.class.getName(), null, false);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        mockServerNotes.enqueue("notesDownload1");
        mockServerOsmose.enqueue("osmoseDownload");
        Preferences prefs = new Preferences(main);
        prefDB.selectAPI("Test"); // this seems to be necessary to force reload of server object
        App.getLogic().setPrefs(prefs);
        System.out.println("Server " + prefs.getServer().toString());
        // <bounds minlat="47.3892400" minlon="8.3844600" maxlat="47.3911300" maxlon="8.3879800"/
        Uri uri = Uri.parse("geo:47.3905,8.385");
        main.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        TestUtils.selectIntentRecipient(device);
        GeoUrlActivity geo = (GeoUrlActivity) instrumentation.waitForMonitorWithTimeout(geoMonitor, 20000);
        assertNotNull(geo);
        takeRequests();
        // a bit of time is needed to load the results
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984L));
        List<Task> tasks = App.getTaskStorage().getTasks();
        //
        int osmose = 0;
        int notes = 0;
        for (Task t : tasks) {
            if (t instanceof Note) {
                notes++;
            } else if (t instanceof OsmoseBug) {
                osmose++;
            }
        }
        System.out.println("Counts " + osmose + " " + notes);
        assertEquals(166, tasks.size()); // combined count of OSMOSE bugs and notes
    }

    /**
     * Test that geo intents with zoom work
     */
    @Test
    public void geoWithZoom() {
        geoMonitor = instrumentation.addMonitor(GeoUrlActivity.class.getName(), null, false);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        mockServerNotes.enqueue("notesDownload1");
        mockServerOsmose.enqueue("osmoseDownload");
        Preferences prefs = new Preferences(main);
        prefDB.selectAPI("Test"); // this seems to be necessary to force reload of server object
        System.out.println("Server " + prefs.getServer().toString());
        // <bounds minlat="47.3892400" minlon="8.3844600" maxlat="47.3911300" maxlon="8.3879800"/
        Uri uri = Uri.parse("geo:47.3905,8.385?z=18");
        main.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        TestUtils.selectIntentRecipient(device);
        GeoUrlActivity geo = (GeoUrlActivity) instrumentation.waitForMonitorWithTimeout(geoMonitor, 60000);
        assertNotNull(geo);
        takeRequests();
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984L));
        assertEquals(19, main.getMap().getZoomLevel());
        assertEquals(8.385D, main.getMap().getViewBox().getCenter()[0], 0.0001D);
        assertEquals(47.3905D, main.getMap().getViewBox().getCenter()[1], 0.0001D);
    }

    /**
     * Test that JOSM rc intents with zoom work
     */
    @Test
    public void rcWithZoom() {
        rcMonitor = instrumentation.addMonitor(RemoteControlUrlActivity.class.getName(), null, false);
        mockServer.enqueue("capabilities1");
        mockServer.enqueue("download1");
        mockServerNotes.enqueue("notesDownload1");
        mockServerOsmose.enqueue("osmoseDownload");
        Preferences prefs = new Preferences(main);
        prefDB.selectAPI("Test"); // this seems to be necessary to force reload of server object
        System.out.println("Server " + prefs.getServer().toString());
        // <bounds minlat="47.3892400" minlon="8.3844600" maxlat="47.3911300" maxlon="8.3879800"/
        Uri uri = Uri.parse(
                "josm:/load_and_zoom?left=8.3844600&right=8.3879800&top=47.3911300&bottom=47.3892400&changeset_comment=thisisatest&select=node101792984&new_layer=true");
        main.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        TestUtils.selectIntentRecipient(device);
        RemoteControlUrlActivity rc = (RemoteControlUrlActivity) instrumentation.waitForMonitorWithTimeout(rcMonitor, 60000);
        assertNotNull(rc);
        takeRequests();
        TestUtils.clickAwayTip(device, main);
        assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984L));
        assertTrue(18 <= main.getMap().getZoomLevel());
        assertEquals(101792984L, App.getLogic().getSelectedNode().getOsmId());
        assertEquals("thisisatest", App.getLogic().getDraftComment());
    }

    /**
     * Wait till all api requests have been answered
     */
    private void takeRequests() {
        try {
            mockServer.server().takeRequest(10L, TimeUnit.SECONDS);
            mockServer.server().takeRequest(10L, TimeUnit.SECONDS);
            mockServerNotes.server().takeRequest(10L, TimeUnit.SECONDS);
            mockServerOsmose.server().takeRequest(10L, TimeUnit.SECONDS);
            // processing the downloads takes time
            TestUtils.sleep(10000);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test that JOSM rc intent for imagery config work
     */
    @Test
    public void rcImagery() {
        rcMonitor = instrumentation.addMonitor(RemoteControlUrlActivity.class.getName(), null, false);
        Uri uri = Uri.parse("josm:/imagery?title=osmtest&type=tms&min_zoom=2&max_zoom=19&url=https://a.tile.openstreetmap.org/%7Bzoom%7D/%7Bx%7D/%7By%7D.png");
        main.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        TestUtils.selectIntentRecipient(device);
        RemoteControlUrlActivity rc = (RemoteControlUrlActivity) instrumentation.waitForMonitorWithTimeout(rcMonitor, 60000);
        assertNotNull(rc);

        // there currently doesn't seem to be a reasonable way to wait
        TestUtils.sleep(5000);
        TileLayerSource tileServer = TileLayerSource.get(context, TileLayerSource.nameToId("osmtest"), false);
        assertEquals("osmtest", tileServer.getName());
        assertEquals(2, tileServer.getMinZoomLevel());
        assertEquals(19, tileServer.getMaxZoomLevel());
    }

    /**
     * Test that viewing a GPX file via an ACTION_VIEW works
     */
    @Test
    public void gpxView() {
        startWithFile(Intent.ACTION_VIEW, null, "short.gpx", MimeTypes.GPX);
    }

    /**
     * Test that viewing a GPX file via an ACTION_SEND works
     */
    @Test
    public void gpxSend() {
        startWithFile(Intent.ACTION_SEND, null, "short.gpx", MimeTypes.GPX);
    }

    /**
     * Test that viewing a GeoJSON file via an ACTION_VIEW works
     */
    @Test
    public void geojsonView() {
        startWithFile(Intent.ACTION_VIEW, "geojson/", "featureCollection.geojson", MimeTypes.GEOJSON);
    }

    /**
     * Test that viewing a GeoJSON file via an ACTION_SEND works
     */
    @Test
    public void geojsonSend() {
        startWithFile(Intent.ACTION_SEND, "geojson/", "featureCollection.geojson", MimeTypes.GEOJSON);
    }

    /**
     * Construct an intent with a file and then try to start us with it
     * 
     * @param action the Intent action
     */
    private void startWithFile(@NonNull String action, @Nullable String sourceDir, @NonNull String fileName, @NonNull String mimeType) {
        try {
            File destinationDir = FileUtil.getApplicationDirectory(main, "/");
            File gpxFile = new File(destinationDir, fileName);
            JavaResources.copyFileFromResources(fileName, sourceDir, gpxFile);
            try {
                ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
                Intent intent = new Intent(action);
                Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", gpxFile);
                intent.setDataAndType(uri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                main.startActivity(intent);
                TestUtils.selectIntentRecipient(device);
                Main m = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 60000);
                assertNotNull(m);
                UiObject2 extentButton = TestUtils.getLayerButton(device, fileName, LayerDialogTest.EXTENT_BUTTON);
                extentButton.click();
            } finally {
                gpxFile.delete();
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
