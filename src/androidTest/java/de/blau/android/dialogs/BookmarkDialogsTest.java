package de.blau.android.dialogs;


import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

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
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.bookmarks.BookmarksStorage;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.ViewBox;

@RunWith(AndroidJUnit4.class)
public class BookmarkDialogsTest {

    Instrumentation instrumentation = null;
    ArrayList<BookmarksStorage> bookmarksStorages = null;
    ViewBox viewBoxtest = null;
    ActivityMonitor monitor = null;
    UiDevice device = null;
    Main main = null;
    Map map = null;

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
        bookmarksStorages = new ArrayList<>();
        try {
            //India
            bookmarksStorages.add(new BookmarksStorage("TestLocation0", new ViewBox(68.1766451354, 7.96553477623, 97.4025614766, 35.4940095078)));
            //Netherlands
            bookmarksStorages.add(new BookmarksStorage("TestLocation1", new ViewBox(3.31497114423, 50.803721015, 7.09205325687, 53.5104033474)));
            //Serbia
            bookmarksStorages.add(new BookmarksStorage("TestLocation2", new ViewBox(18.82982, 42.2452243971, 22.9860185076, 46.1717298447)));
        } catch (OsmException osmex) {
            osmex.printStackTrace();
        }
    }

    /**
     * Checks if the the viewboxes are same across dialogs
     */
    @Test
    public void addRemoveTest() {
        //Add Dialog
        for (int i = 0; i < 3; i++) {
            map.getViewBox().fitToBoundingBox(map, bookmarksStorages.get(i).getViewBox());
            map.invalidate();
            Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/menu_gps", true));
            Assert.assertTrue(TestUtils.clickText(device, false, main.getString(R.string.add_bookmark), true, false));
            UiObject comments = device.findObject(new UiSelector().clickable(true).resourceId(device.getCurrentPackageName() + ":id/text_line_edit"));
            try {
                comments.click();
                comments.setText("TestLocation" + i);
            } catch (UiObjectNotFoundException e) {
                Assert.fail(e.getMessage());
            }
            TestUtils.clickButton(device, "android:id/button1", true);
        }
        //Show Dialog
        for (int i = 0; i < 3; i++) {
            TestUtils.clickMenuButton(device, "GPS/GNSS…", false, true);
            TestUtils.clickText(device, false, "Show Bookmarks..", true, false);
            TestUtils.clickResource(device, true, (device.getCurrentPackageName() + ":id/adapterlayout"), true);
            viewBoxtest = map.getViewBox();
            // dividing by 100 to accomodate slight change in map.getviewbox()
            Assert.assertEquals((bookmarksStorages.get(i).getViewBox().getLeft()) / 100, viewBoxtest.getLeft() / 100);
            Assert.assertEquals((bookmarksStorages.get(i).getViewBox().getRight()) / 100, viewBoxtest.getRight() / 100);
            TestUtils.clickMenuButton(device, main.getString(R.string.menu_gps), false, true);
            TestUtils.clickText(device, false, main.getString(R.string.show_bookmarks), true, false);
            TestUtils.clickText(device, false, "⋮", true, false);
            TestUtils.clickText(device, false, main.getString(R.string.discard), true, false);
        }
        TestUtils.clickText(device, false, main.getString(R.string.done), true, false);
    }
}