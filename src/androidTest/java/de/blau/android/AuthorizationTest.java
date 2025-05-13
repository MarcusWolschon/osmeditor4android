package de.blau.android;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.prefs.API;
import de.blau.android.prefs.API.AuthParams;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AuthorizationTest {

    private static final String DEBUG_TAG = AuthorizationTest.class.getSimpleName();

    MockWebServerPlus    mockApiServer      = null;
    MockWebServerPlus    mockServer      = null;
    Context              context         = null;
    AdvancedPrefDatabase prefDB          = null;
    Instrumentation      instrumentation = null;
    UiDevice             device          = null;
    Main                 main            = null;

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
        KeyDatabaseHelper.readKeysFromAssets(main);
        mockServer = new MockWebServerPlus();
        HttpUrl mockUrl = mockServer.server().url("/");
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(main)) {
            KeyDatabaseHelper.replaceOrDeleteKey(keyDatabase.getWritableDatabase(), "OpenStreetMap sandbox", EntryType.API_OAUTH2_KEY, "1111111111", false, true, "empty", mockUrl.toString());
        }
        mockApiServer = new MockWebServerPlus();
        HttpUrl mockApiBaseUrl = mockApiServer.server().url("/api/0.6/");
        Log.d(DEBUG_TAG, "Mock url " + mockApiBaseUrl);

        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockApiBaseUrl.toString(), null, null, new AuthParams(API.Auth.OAUTH2, "user", "pass", null, null), false);
        prefDB.selectAPI("Test");
        prefDB.resetCurrentServer();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            mockApiServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
    }

    /**
     * Start up the authorization activity and check that we get the dialog to select a client key
     */
    @Test
    public void startAuthorization() {
        ActivityMonitor monitor = instrumentation.addMonitor(Authorize.class.getName(), null, false);

        if (!TestUtils.clickMenuButton(device, main.getString(R.string.menu_tools), false, true)) {
            TestUtils.clickOverflowButton(device);
            TestUtils.clickText(device, false, main.getString(R.string.menu_tools), true, false);
        }
        TestUtils.scrollTo(main.getString(R.string.menu_tools_oauth_authorisation), false);
        TestUtils.clickText(device, false, main.getString(R.string.menu_tools_oauth_authorisation), true, false);
        instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        instrumentation.removeMonitor(monitor);
        mockServer.enqueue("loaded");
        assertTrue(TestUtils.findText(device, false, "OpenStreetMap sandbox", 10000));
        assertTrue(TestUtils.clickText(device, false, "OpenStreetMap sandbox", true));
        assertTrue(TestUtils.findText(device, false, "Loaded", 10000));
        try {
            assertTrue(mockServer.takeRequest().toString().contains("1111111111"));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}
