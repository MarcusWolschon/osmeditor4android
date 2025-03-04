package de.blau.android;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.API.AuthParams;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FeedbackTest {

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
        KeyDatabaseHelper.replaceOrDeleteKey(new KeyDatabaseHelper(main).getWritableDatabase(), Feedback.VESPUCCI_REPORTER_ENTRY, EntryType.API_KEY, "123",
                false, false, null, null);
        ;
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        prefDB = new AdvancedPrefDatabase(context);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
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
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
    }

    /**
     * Start up the feedback activity
     */
    @Test
    public void startFeedback() {
        mockServer.enqueue("userdetails");
        TestUtils.clickOverflowButton(device);
        ActivityMonitor monitor = instrumentation.addMonitor(Feedback.class.getName(), null, false);
        TestUtils.clickText(device, false, "Provide feedback", true, false);
        instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        instrumentation.removeMonitor(monitor);
        Assert.assertTrue(TestUtils.findText(device, false, "Feedback for the developers", 10000));
        device.pressBack();
    }
}
