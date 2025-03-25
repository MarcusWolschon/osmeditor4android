package io.vespucci.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.ShadowWorkManager;
import io.vespucci.osm.Capabilities;
import io.vespucci.osm.Server;
import io.vespucci.prefs.API;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.prefs.API.AuthParams;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk = 33)
@LargeTest
public class MiscApiTest {

    private static final String CAPABILITIES1_FIXTURE = "capabilities1";

    public static final int TIMEOUT = 10;

    MockWebServerPlus    mockServer = null;
    AdvancedPrefDatabase prefDB     = null;
    Main                 main       = null;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        main = Robolectric.buildActivity(Main.class).create().resume().get();
        prefDB = new AdvancedPrefDatabase(main);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
        prefDB.selectAPI("Test");
        System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
        Logic logic = App.getLogic();
        Preferences prefs = new Preferences(main);
        logic.setPrefs(prefs);
        logic.getMap().setPrefs(main, prefs);
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
     * Get API capabilities
     */
    @Test
    public void capabilities() {
        final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), ApiTest.GENERATOR_NAME);
        // from default
        Capabilities result = s.getCachedCapabilities();
        assertNotNull(result);
        assertEquals("0.6", result.getMinVersion());
        assertEquals("0.6", result.getMaxVersion());
        assertEquals(Capabilities.Status.ONLINE, result.getGpxStatus());
        assertEquals(Capabilities.Status.ONLINE, result.getApiStatus());
        assertEquals(Capabilities.Status.ONLINE, result.getDbStatus());
        assertEquals(2000, result.getMaxWayNodes());
        assertEquals(5000, result.getMaxTracepointsPerPage());
        assertEquals(10000, result.getMaxElementsInChangeset());
        assertEquals(300, result.getTimeout());
        assertEquals(0.25, result.getMaxArea(), 0.001);
        assertEquals(25.0, result.getMaxNoteArea(), 0.001);

        // from fixture
        mockServer.enqueue(CAPABILITIES1_FIXTURE);
        result = s.getCapabilities();
        assertNotNull(result);
        assertEquals("0.6", result.getMinVersion());
        assertEquals("0.6", result.getMaxVersion());
        assertEquals(Capabilities.Status.ONLINE, result.getGpxStatus());
        assertEquals(Capabilities.Status.ONLINE, result.getApiStatus());
        assertEquals(Capabilities.Status.ONLINE, result.getDbStatus());
        assertEquals(2001, result.getMaxWayNodes());
        assertEquals(4999, result.getMaxTracepointsPerPage());
        assertEquals(50000, result.getMaxElementsInChangeset());
        assertEquals(301, result.getTimeout());
        assertEquals(0.24, result.getMaxArea(), 0.001);
    }

    /**
     * get the user preferences, the set and delete one
     */
    @Test
    public void userpreferences() {
        mockServer.enqueue("userpreferences");
        mockServer.enqueue("200");
        mockServer.enqueue("200");
        try {
            final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), ApiTest.GENERATOR_NAME);
            Map<String, String> preferences = s.getUserPreferences();
            assertEquals(3, preferences.size());
            assertEquals("public", preferences.get("gps.trace.visibility"));
            RecordedRequest request1 = mockServer.takeRequest();
            assertEquals("GET", request1.getMethod().toUpperCase());
            assertEquals("/api/0.6/user/preferences", request1.getPath());
            s.setUserPreference("gps.trace.visibility", "private");
            RecordedRequest request2 = mockServer.takeRequest();
            assertEquals("PUT", request2.getMethod().toUpperCase());
            assertEquals("/api/0.6/user/preferences/gps.trace.visibility", request2.getPath());
            assertEquals("private", request2.getBody().readUtf8());
            s.deleteUserPreference("gps.trace.visibility");
            RecordedRequest request3 = mockServer.takeRequest();
            assertEquals("DELETE", request3.getMethod().toUpperCase());
            assertEquals("/api/0.6/user/preferences/gps.trace.visibility", request3.getPath());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
