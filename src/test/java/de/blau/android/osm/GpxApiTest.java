package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import okhttp3.HttpUrl;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class GpxApiTest {

    private static final String GPX_FILES_FIXTURE = "gpx_files";
    private static final String GPX_DATA_FIXTURE  = "gpx_data";

    private static final String GENERATOR_NAME = "vesupucci test";

    public static final int TIMEOUT = 10;

    MockWebServerPlus    mockServer = null;
    AdvancedPrefDatabase prefDB     = null;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        prefDB = new AdvancedPrefDatabase(ApplicationProvider.getApplicationContext());
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", API.Auth.BASIC);
        prefDB.selectAPI("Test");
        System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
        Logic logic = App.newLogic();
        logic.setPrefs(new Preferences(ApplicationProvider.getApplicationContext()));
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
    }

    /**
     * Get all user GPX files
     */
    @Test
    public void allUserGpxFiles() {
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        // from fixture
        mockServer.enqueue(GPX_FILES_FIXTURE);
        List<GpxFile> result = OsmGpxApi.getUserGpxFiles(s, null);
        assertNotNull(result);
        assertEquals(308, result.size());
        boolean found = false;
        for (GpxFile g : result) {
            if (2631740L == g.getId() && g.getTags().contains("trrrrrr") && "test".equals(g.getDescription()) && "2018_03_10T152010.gpx".equals(g.getName())) {
                found = true;
            }
        }
        if (!found) {
            fail("file 2631740 not found");
        }
    }

    /**
     * Get user GPX files with BB filter
     */
    @Test
    public void userGpxFiles() {
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        // from fixture
        mockServer.enqueue(GPX_FILES_FIXTURE);
        List<GpxFile> result = OsmGpxApi.getUserGpxFiles(s, new BoundingBox(8.37578, 47.4100, 8.37580, 47.4110));
        assertNotNull(result);
        assertEquals(1, result.size());
        boolean found = false;
        for (GpxFile g : result) {
            if (2631740L == g.getId() && g.getTags().contains("trrrrrr") && "test".equals(g.getDescription()) && "2018_03_10T152010.gpx".equals(g.getName())) {
                found = true;
            }
        }
        if (!found) {
            fail("file 2631740 not found");
        }
    }

    /**
     * Get a GPX file
     */
    @Test
    public void downloadGpxFile() {
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), GENERATOR_NAME);
        // from fixture
        mockServer.enqueue(GPX_DATA_FIXTURE);
        Uri result = OsmGpxApi.downloadTrack(s, 111, ".", "test.gpx");
        assertNotNull(result);
        assertTrue(result.getLastPathSegment().endsWith("test.gpx"));
    }
}
