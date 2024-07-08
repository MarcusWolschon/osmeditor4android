package de.blau.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import de.blau.android.contract.Files;
import de.blau.android.dialogs.NewVersion;
import de.blau.android.net.OAuthHelper.OAuthConfiguration;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.API.Auth;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NewVersionTest {

    Context              context         = null;
    ActivityMonitor      monitor         = null;
    AdvancedPrefDatabase prefDB          = null;
    Instrumentation      instrumentation = null;
    UiDevice             device          = null;
    Main                 main            = null;
    MockWebServerPlus    mockServer      = null;

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
        mockServer = new MockWebServerPlus();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
    }

    @After
    public void teardown() {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
            API api = db.getCurrentAPI();
            db.setAPIDescriptors(api.id, api.name, api.url, api.readonlyurl, api.notesurl, Auth.OAUTH2);
        }
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }
    }

    /**
     * No migration test
     */
    @Test
    public void noMigration() {
        NewVersion.showDialog(main);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.upgrade_title)));
        assertFalse(TestUtils.findText(device, false, main.getString(R.string.migrate_now)));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.read_upgrade), true));

        // Waiting with a monitor doesn't work in this case
        assertTrue(TestUtils.findText(device, false, "Release", 10000, true));
        assertTrue(TestUtils.clickMenuButton(device, "Back", false, true));
        assertFalse(TestUtils.findText(device, false, main.getString(R.string.upgrade_title)));
    }
}
