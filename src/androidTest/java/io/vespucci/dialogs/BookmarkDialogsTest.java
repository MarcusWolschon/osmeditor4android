package io.vespucci.dialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import io.vespucci.R;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.bookmarks.Bookmark;
import io.vespucci.bookmarks.BookmarkStorage;
import io.vespucci.exception.OsmException;
import io.vespucci.osm.ViewBox;

@RunWith(AndroidJUnit4.class)
public class BookmarkDialogsTest {

    Instrumentation     instrumentation   = null;
    ArrayList<Bookmark> bookmarksStorages = null;
    ViewBox             viewBoxtest       = null;
    ActivityMonitor     monitor           = null;
    UiDevice            device            = null;
    Main                main              = null;
    Map                 map               = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class, false, false);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        main = mActivityRule.launchActivity(intent);
        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 90000);// NOSONAR wait for main
        assertNotNull(main);
        TestUtils.grantPermissons(device);
        map = main.getMap();
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        // zap current contents
        (new BookmarkStorage()).writeList(main, new ArrayList<>());
        TestUtils.sleep();
        bookmarksStorages = new ArrayList<>();
        try {
            // India
            bookmarksStorages.add(new Bookmark("TestLocation0", new ViewBox(68.1766451354, 7.96553477623, 97.4025614766, 35.4940095078)));
            // Netherlands
            bookmarksStorages.add(new Bookmark("TestLocation1", new ViewBox(3.31497114423, 50.803721015, 7.09205325687, 53.5104033474)));
            // Serbia
            bookmarksStorages.add(new Bookmark("TestLocation2", new ViewBox(18.82982, 42.2452243971, 22.9860185076, 46.1717298447)));
        } catch (OsmException osmex) {
            osmex.printStackTrace();
        }
    }

    /**
     * Checks if the the viewboxes are same across dialogs
     */
    @Test
    public void addRemoveTest() {
        // Add Dialog
        for (int i = 0; i < 3; i++) {
            map.getViewBox().fitToBoundingBox(map, bookmarksStorages.get(i).getViewBox());
            map.invalidate();
            assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
            assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_add_bookmark), true, false));
            UiObject comments = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
            try {
                comments.click();
                comments.setText("TestLocation" + i);
            } catch (UiObjectNotFoundException e) {
                Assert.fail(e.getMessage());
            }
            TestUtils.clickButton(device, "android:id/button1", true);
        }

        List<Bookmark> rereadStorages = new BookmarkStorage().readList(main);
        // Show Dialog
        for (int i = 0; i < 3; i++) {
            TestUtils.clickMenuButton(device, main.getString(R.string.menu_gps), false, true);
            TestUtils.clickText(device, false, main.getString(R.string.show_bookmarks), true, false);
            assertTrue(TestUtils.clickText(device, false, "TestLocation" + i, true, false));
            TestUtils.sleep(5000);
            ViewBox bookMarkBox = rereadStorages.get(i).getViewBox();
            ViewBox viewBoxtest = map.getViewBox();
            assertEquals(bookMarkBox.getLeft() / 1E7D, viewBoxtest.getLeft() / 1E7D, 0.01);
            assertEquals(bookMarkBox.getRight() / 1E7D, viewBoxtest.getRight() / 1E7D, 0.01);
            TestUtils.clickMenuButton(device, main.getString(R.string.menu_gps), false, true);
            TestUtils.clickText(device, false, main.getString(R.string.show_bookmarks), true, false);
            TestUtils.clickText(device, false, "⋮", true, false);
            TestUtils.clickText(device, false, main.getString(R.string.discard), true, false);
        }
        TestUtils.clickText(device, false, main.getString(R.string.done), true, false);
    }

    /**
     * Change the name of a bookmark
     */
    @Test
    public void editTest() {
        // Add Dialog
        map.getViewBox().fitToBoundingBox(map, bookmarksStorages.get(0).getViewBox());
        map.invalidate();
        assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_add_bookmark), true, false));
        UiObject comments = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
        try {
            comments.click();
            comments.setText("TestLocation0");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);

        TestUtils.clickMenuButton(device, main.getString(R.string.menu_gps), false, true);
        TestUtils.clickText(device, false, main.getString(R.string.show_bookmarks), true, false);
        TestUtils.clickText(device, false, "⋮", true, false);
        TestUtils.clickText(device, false, main.getString(R.string.edit), true, false);
        assertTrue(TestUtils.findText(device, false, "TestLocation0"));
        comments = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
        try {
            comments.clearTextField();
            ;
            comments.setText("TestLocation007");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        TestUtils.clickButton(device, "android:id/button1", true);
        assertTrue(TestUtils.findText(device, false, "TestLocation007"));
        TestUtils.clickText(device, false, main.getString(R.string.done), true, false);
    }
}