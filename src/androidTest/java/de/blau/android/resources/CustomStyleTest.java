package de.blau.android.resources;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.prefs.PrefEditor;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CustomStyleTest {

    Main            main            = null;
    UiDevice        device          = null;
    Instrumentation instrumentation = null;
    Logic           logic           = null;
    Map             map             = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();

        device = UiDevice.getInstance(instrumentation);
        main = mActivityRule.getActivity();
        map = main.getMap();

        TestUtils.grantPermissons(device);

        TestUtils.dismissStartUpDialogs(device, main);

        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(App.getLogic(), map);
        main.invalidateOptionsMenu(); // to be sure that the menu entry is actually shown
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {

    }

    /**
     * Import zipped style
     */
    @Test
    public void importZipped() {
        try {
            File styleFile = JavaResources.copyFileFromResources(main, "test-style.zip", null, "/");
            ActivityMonitor monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
            try {
                TestUtils.zoomToLevel(device, main, 18);
                if (!TestUtils.clickMenuButton(device, main.getString(R.string.menu_tools), false, true)) {
                    TestUtils.clickOverflowButton(device);
                    TestUtils.clickText(device, false, main.getString(R.string.menu_tools), true, false);
                }
                TestUtils.scrollTo(main.getString(R.string.menu_tools_import_data_style), false);
                TestUtils.clickText(device, false, main.getString(R.string.menu_tools_import_data_style), true, false);
                TestUtils.selectFile(device, main, null, "test-style.zip", true);
                assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
                instrumentation.waitForMonitorWithTimeout(monitor, 40000); // wait for prefs
                assertTrue(TestUtils.clickText(device, false, main.getString(R.string.config_mapProfile_title), true, false));
                assertTrue(TestUtils.clickText(device, false, "Test style", true, false));
                assertTrue(TestUtils.clickHome(device, true));
            } finally {
                styleFile.delete();
                instrumentation.removeMonitor(monitor);
            }
        } catch (IOException iex) {
            fail(iex.getMessage());
        }
    }
}
