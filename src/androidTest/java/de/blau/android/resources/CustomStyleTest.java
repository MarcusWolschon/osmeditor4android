package de.blau.android.resources;

import static org.junit.Assert.assertEquals;
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
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.StyleConfigurationEditorActivity;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CustomStyleTest {

    private static final String TEST_STYLE_NAME = "Test Style";
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
            ActivityMonitor monitor2 = instrumentation.addMonitor(StyleConfigurationEditorActivity.class.getName(), null, false);
            try {
                TestUtils.zoomToLevel(device, main, 18);
                assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
                instrumentation.waitForMonitorWithTimeout(monitor, 40000); //
                
                TestUtils.clickText(device, false, main.getString(R.string.config_mapProfile_title), true, false);
                instrumentation.waitForMonitorWithTimeout(monitor2, 40000); //
                
                TestUtils.clickMenuButton(device, main.getString(R.string.urldialog_add_style), false, true);
                device.wait(Until.findObject(By.clickable(true).res(device.getCurrentPackageName() + ":id/listedit_editName")), 500);
                UiObject name = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/listedit_editName"));
                try {
                    name.setText(TEST_STYLE_NAME);
                } catch (UiObjectNotFoundException e) {
                    fail(e.getMessage());
                }
                
                UiObject fileButton = device
                        .findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/listedit_file_button"));
                try {
                    fileButton.click();
                } catch (UiObjectNotFoundException e) {
                    fail(e.getMessage());
                }
                TestUtils.selectFile(device, main, null, "test-style.zip", true);
                assertTrue(TestUtils.clickButton(device, "android:id/button1", true));
                assertTrue(TestUtils.clickText(device, false, TEST_STYLE_NAME, false, false));
                assertTrue(TestUtils.clickHome(device, true));
                assertTrue(TestUtils.clickHome(device, true));
                
                assertEquals(TEST_STYLE_NAME, App.getDataStyleManager(main).getCurrent().getName());
                
            } finally {
                styleFile.delete();
                instrumentation.removeMonitor(monitor);
                instrumentation.removeMonitor(monitor2);
            }
        } catch (IOException iex) {
            fail(iex.getMessage());
        }
    }

    /**
     * Import invalid empty style
     */
    @Test
    public void importEmpty() {
        try {
            File styleFile = JavaResources.copyFileFromResources(main, "empty.xml", null, "/");
            ActivityMonitor monitor = instrumentation.addMonitor(PrefEditor.class.getName(), null, false);
            ActivityMonitor monitor2 = instrumentation.addMonitor(StyleConfigurationEditorActivity.class.getName(), null, false);
            try {
                TestUtils.zoomToLevel(device, main, 18);
                assertTrue(TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/menu_config", true));
                instrumentation.waitForMonitorWithTimeout(monitor, 40000); //
                
                TestUtils.clickText(device, false, main.getString(R.string.config_mapProfile_title), true, false);
                instrumentation.waitForMonitorWithTimeout(monitor2, 40000); //
                
                TestUtils.clickMenuButton(device, main.getString(R.string.urldialog_add_style), false, true);
                
                TestUtils.clickMenuButton(device, main.getString(R.string.urldialog_add_style), false, true);
                device.wait(Until.findObject(By.clickable(true).res(device.getCurrentPackageName() + ":id/listedit_editName")), 500);
                UiObject name = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/listedit_editName"));
                try {
                    name.setText(TEST_STYLE_NAME);
                } catch (UiObjectNotFoundException e) {
                    fail(e.getMessage());
                }
                   
                UiObject fileButton = device
                        .findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/listedit_file_button"));
                try {
                    fileButton.click();
                } catch (UiObjectNotFoundException e) {
                    fail(e.getMessage());
                }
                TestUtils.selectFile(device, main, null, "empty.xml", true);
                assertTrue(TestUtils.clickButton(device, "android:id/button1", true));
                assertTrue(TestUtils.findNotification(device, "Error"));
            } finally {
                styleFile.delete();
            }
        } catch (IOException iex) {
            fail(iex.getMessage());
        }
    }
}
