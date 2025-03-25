package io.vespucci.propertyeditor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.propertyeditor.PropertyEditorActivity;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UrlCheckTest {

    private Context         context         = null;
    private Instrumentation instrumentation = null;
    private Main            main            = null;
    private UiDevice        device          = null;
    private Map             map;
    private Logic           logic;
    private String          presetId        = null;

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
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        prefs.enableSimpleActions(true);
        main.runOnUiThread(() -> main.showSimpleActionsButton());
        map = main.getMap();
        map.setPrefs(main, prefs);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        App.getTaskStorage().reset();
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
        App.getTaskStorage().reset();
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(main)) {
            if (presetId != null) {
                db.deletePreset(presetId);
            }
        }
    }

    /**
     * Create a new Node set it to a shop=supermarket and then set the website
     * 
     * THis hads the side effect of checking if notifications for errors get generated correctly
     */
    // @SdkSuppress(minSdkVersion = 26)
    @Test
    public void urlCheck() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.unlock(device);
        TestUtils.clickSimpleButton(device);
        assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_node_tags), true, false));
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.add_node_instruction)));
        TestUtils.clickAtCoordinates(device, map, 8.3893454, 47.3901898, true);

        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);

        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditorActivity);
        boolean found = TestUtils.clickText(device, true, "Shops", true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, "Food", true, false);
        assertTrue(found);
        found = TestUtils.clickText(device, true, "Supermarket", true, false);
        assertTrue(found);
        assertTrue(TestUtils.findText(device, false, "Supermarket"));
        TestUtils.scrollTo("Contact", false);
        assertTrue(TestUtils.clickText(device, false, "Contact", false, true));
        try {
            UiObject2 website = PropertyEditorTest.getField(device, "Website", 1);
            assertNotNull(website);
            website.click();
        } catch (UiObjectNotFoundException e) {
            fail();
        }

        UiObject urlText = TestUtils.findObjectWithResourceId(device, false, device.getCurrentPackageName() + ":id/text_line_edit");
        try {
            urlText.setText("localhost");
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.check), false));
        String[] statusStrings = main.getResources().getStringArray(R.array.checkstatus_entries);
        assertTrue(TestUtils.findNotification(device, statusStrings[3]));

        MockWebServerPlus mockServer = new MockWebServerPlus();
        try {
            HttpUrl mockBaseUrl = mockServer.server().url("/");
            try {
                urlText.setText(mockBaseUrl.toString());
            } catch (UiObjectNotFoundException e) {
                fail(e.getMessage());
            }
            mockServer.enqueue("403");
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.check), false));
            try {
                mockServer.server().takeRequest(10L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // ignore
            }
            assertTrue(TestUtils.findNotification(device, main.getString(R.string.toast_url_check, mockBaseUrl.toString(), 403)));
        } finally {
            try {
                mockServer.server().shutdown();
            } catch (IOException e) {
                // Ignore
            }
        }

        mockServer = new MockWebServerPlus();
        try {
            HttpUrl mockBaseUrl = mockServer.server().url("/");
            try {
                urlText.setText(mockBaseUrl.toString());
            } catch (UiObjectNotFoundException e) {
                fail(e.getMessage());
            }
            mockServer.enqueue("200");
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.check), false));
            try {
                mockServer.server().takeRequest(10L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // ignore
            }
            assertFalse(TestUtils.findNotification(device, "Vespucci"));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.save), true));
            assertTrue(TestUtils.findText(device, false, mockBaseUrl.toString()));
        } finally {
            try {
                mockServer.server().shutdown();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
