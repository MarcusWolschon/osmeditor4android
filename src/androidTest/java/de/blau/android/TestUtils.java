package de.blau.android;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.Assert;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Configurator;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import de.blau.android.filter.Filter;
import de.blau.android.gpx.TrackPoint;
import de.blau.android.imageryoffset.Offset;
import de.blau.android.osm.TransferMenuTest;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoMath;
import de.blau.android.views.layers.MapTilesLayer;
import okhttp3.mockwebserver.MockResponse;
import okio.Buffer;

/**
 * Various methods to support testing
 * 
 * @author Simon Poole
 *
 */
public final class TestUtils {
    private static final String DEBUG_TAG = TestUtils.class.getSimpleName().substring(0, Math.min(23, TestUtils.class.getSimpleName().length()));

    static final String VESPUCCI = "Vespucci";

    /**
     * Private constructor
     */
    private TestUtils() {
        // empty
    }

    /**
     * Grant permissions by clicking on the dialogs, currently only works for English and German
     * 
     * @param device the UiDevice
     */
    public static void grantPermissons(@NonNull UiDevice device) {
        clickText(device, false, "Wait", true, false, 5000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean notdone = true;
            while (notdone) {
                notdone = clickText(device, false, "While using the app", true, true) || clickText(device, false, "WHILE USING THE APP", true, true)
                        || clickText(device, true, "allow", true, false) || clickText(device, true, "zulassen", false, false);
            }
        }
    }

    /**
     * Dismiss initial welcome dialog
     * 
     * @param device the UiDevice
     * @param ctx Android context
     */
    public static void dismissStartUpDialogs(@NonNull UiDevice device, @NonNull Context ctx) {
        if (findText(device, false, "Vespucci crashed")) {
            clickText(device, true, ctx.getResources().getString(R.string.cancel), true, false);
        }
        if (findText(device, false, ctx.getResources().getString(R.string.welcome_title))) {
            clickText(device, true, ctx.getResources().getString(R.string.next), true, false);
            clickResource(device, false, device.getCurrentPackageName() + ":id/authorize", false);
            clickText(device, true, ctx.getResources().getString(R.string.welcome_start), true, false);
        }
    }

    /**
     * Zoom to null island
     * 
     * @param logic current logic instance
     * @param map current map
     */
    public static void zoomToNullIsland(@NonNull Logic logic, @NonNull Map map) {
        logic.setZoom(map, 18);
        map.getViewBox().moveTo(map, 0, 0);
        map.invalidate();
    }

    /**
     * Select the recipient of an intent
     * 
     * Currently flaky
     * 
     * @param device the UiDevice
     */
    public static void selectIntentRecipient(@NonNull UiDevice device) {
        device.waitForWindowUpdate(null, 1000);
        String shareWithOSM = "Share on OpenStreetMap";
        String justOnce = "Just once";
        if (device.wait(Until.findObject(By.text("^Open with$|^Open with Maps$")), 500) != null) {
            UiObject2 share = findObjectWithText(device, false, shareWithOSM, 500, false);
            List<UiObject2> l = share.getParent().getParent().getParent().getChildren(); // found experimentally
            for (UiObject2 o : l) {
                if (o.findObject(By.text(shareWithOSM)) == null) {
                    o.click();
                    clickText(device, false, justOnce, false, false);
                    return;
                }
            }
        } else {
            if (findText(device, false, "Open with Vespucci")) {
                if (findText(device, false, shareWithOSM)) {
                    clickText(device, false, justOnce, false, false);
                } else {
                    // Open with Vespucci was actually Share on OpenStreetMap
                    clickText(device, false, VESPUCCI, false, false);
                }
            } else {
                clickText(device, false, VESPUCCI, false, false);
                if (!clickText(device, false, justOnce, false, false)) {
                    clickText(device, false, "Nur diesmal", false, false);
                }
            }
        }
    }

    /**
     * Click the overflow button in a menu bar
     * 
     * @param device the UiDevice
     * @return true if successful
     */
    public static boolean clickOverflowButton(@NonNull UiDevice device) {
        return clickMenuButton(device, "More options", false, true);
    }

    /**
     * Click a menu bar button
     * 
     * @param device the UiDevice
     * @param description the description of the button
     * @param longClick if true perform a long click
     * @param waitForNewWindow if true wait for a new window
     * 
     * @return true if successful
     */
    public static boolean clickMenuButton(@NonNull UiDevice device, String description, boolean longClick, boolean waitForNewWindow) {
        return clickMenuButton(device, description, longClick, waitForNewWindow, 500);
    }

    /**
     * Click a menu bar button
     * 
     * @param device the UiDevice
     * @param description the description of the button
     * @param longClick if true perform a long click
     * @param waitForNewWindow if true wait for a new window
     * @param wait how long to wait for the test to appear
     * 
     * @return true if successful
     */
    public static boolean clickMenuButton(@NonNull UiDevice device, String description, boolean longClick, boolean waitForNewWindow, long wait) {
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = By.clickable(true).descStartsWith(description);
        UiSelector uiSelector = new UiSelector().clickable(true).descriptionStartsWith(description);
        device.wait(Until.findObject(bySelector), wait);
        UiObject button = device.findObject(uiSelector);
        if (button.exists()) {
            try {
                if (longClick) {
                    button.longClick();
                } else if (waitForNewWindow) {
                    button.clickAndWaitForNewWindow();
                } else {
                    button.click();
                }
                return true; // the button clicks don't seem to reliably return a true
            } catch (UiObjectNotFoundException e) {
                Log.e(DEBUG_TAG, "Object vanished.");
                return false;
            }
        } else {
            Log.e(DEBUG_TAG, "Object not found");
            return false;
        }
    }

    /**
     * Click on a button
     * 
     * @param device the UiDevice
     * @param resId resource id
     * @param waitForNewWindow if true wait for a new window after clicking
     * 
     * @return true if the button was found and clicked
     * @throws UiObjectNotFoundException if we couldn't find the button
     */
    public static boolean clickButton(@NonNull UiDevice device, @NonNull String resId, boolean waitForNewWindow) {
        try {
            UiSelector uiSelector = new UiSelector().clickable(true).resourceId(resId);
            UiObject button = device.findObject(uiSelector);
            if (waitForNewWindow) {
                return button.clickAndWaitForNewWindow();
            } else {
                return button.click();
            }
        } catch (UiObjectNotFoundException e) {
            System.out.println(e.getMessage() + " " + resId);
            return false;
        }
    }

    /**
     * Two finger zoom out simulation
     * 
     * @param device the UiDevice
     */
    public static void pinchOut(@NonNull UiDevice device) {
        UiSelector uiSelector = new UiSelector().resourceId(device.getCurrentPackageName() + ":id/map_view");
        try {
            device.findObject(uiSelector).pinchOut(75, 100);
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Two finger zoom in simulation
     * 
     * @param device the UiDevice
     */
    public static void pinchIn(@NonNull UiDevice device) {
        UiSelector uiSelector = new UiSelector().resourceId(device.getCurrentPackageName() + ":id/map_view");
        try {
            device.findObject(uiSelector).pinchIn(75, 100);
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Single click at a screen location
     * 
     * @param device the UiDevice
     * @param x screen X coordinate
     * @param y screen y coordinate
     */
    public static void clickAt(@NonNull UiDevice device, float x, float y) {
        System.out.println("clicking at " + x + " " + y);
        device.click((int) x, (int) y);
        sleep(100);
    }

    /**
     * Single click at a WGS84 location
     * 
     * Will center the screen on the location first
     * 
     * @param device the UiDevice
     * @param map the current Map object
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     */
    public static void clickAtCoordinates(@NonNull UiDevice device, @NonNull Map map, double lon, double lat) {
        clickAtCoordinates(device, map, lon, lat, true);
    }

    /**
     * Click at a coordinate and wait for a new window
     * 
     * @param device UiDevice instance
     * @param map the current Map object
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     * @return true if successful
     */
    public static boolean clickAtCoordinatesWaitNewWindow(@NonNull UiDevice device, @NonNull Map map, int lonE7, int latE7) {
        return device.performActionAndWait(new Runnable() {
            @Override
            public void run() {
                TestUtils.clickAtCoordinates(device, map, lonE7, latE7);
            }
        }, Until.newWindow(), 5000);
    }

    /**
     * Single click at a WGS84 location
     * 
     * @param device the UiDevice
     * @param map the current Map object
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void clickAtCoordinates(@NonNull UiDevice device, @NonNull Map map, double lon, double lat, boolean recenter) {
        clickAtCoordinates(device, map, (int) (lon * 1E7), (int) (lat * 1E7), recenter);
    }

    /**
     * Single click at a WGS84 location
     * 
     * Will center the screen on the location first
     * 
     * @param device the UiDevice
     * @param map the current Map object
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     */
    public static void clickAtCoordinates(@NonNull UiDevice device, @NonNull Map map, int lonE7, int latE7) {
        clickAtCoordinates(device, map, lonE7, latE7, true);
    }

    /**
     * Single click at a WGS84 location
     * 
     * @param device the UiDevice
     * @param map the current Map object
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void clickAtCoordinates(@NonNull UiDevice device, @NonNull Map map, int lonE7, int latE7, boolean recenter) {
        if (recenter) {
            if (map.getZoomLevel() < 15) {
                App.getLogic().setZoom(map, 15); // we want the coordinate to be somewhere in the middle of the screen
            }
            map.getViewBox().moveTo(map, lonE7, latE7);
            map.invalidate();
        }
        float x = GeoMath.lonE7ToX(map.getWidth(), map.getViewBox(), lonE7);
        float y = GeoMath.latE7ToY(map.getHeight(), map.getWidth(), map.getViewBox(), latE7);
        int[] outLocation = new int[2];
        map.getLocationOnScreen(outLocation);
        TestUtils.clickAt(device, Math.round(x) + outLocation[0], Math.round(y) + outLocation[1]);
    }

    /**
     * An attempt at getting reliable long clicks with swiping
     * 
     * @param device the current UiDevice
     * @param o the UiObject to long click on
     * @throws UiObjectNotFoundException if o is not found
     */
    public static void longClick(@NonNull UiDevice device, @NonNull UiObject o) throws UiObjectNotFoundException {
        Rect rect = o.getBounds();
        device.swipe(rect.centerX(), rect.centerY(), rect.centerX(), rect.centerY(), 200);
        sleep(2000);
    }

    /**
     * An attempt at getting reliable long clicks with swiping
     * 
     * @param device the current UiDevice
     * @param o the UiObject2 to long click on
     * @throws UiObjectNotFoundException if o is not found
     */
    public static void longClick(@NonNull UiDevice device, @NonNull UiObject2 o) throws UiObjectNotFoundException {
        Point p = o.getVisibleCenter();
        device.swipe(p.x, p.y, p.x, p.y, 200);
        sleep(2000);
    }

    /**
     * Long click at a screen location
     * 
     * @param device the UiDevice
     * @param x screen X coordinate
     * @param y screen y coordinate
     */
    public static void longClickAt(@NonNull UiDevice device, float x, float y) {
        device.swipe((int) x, (int) y, (int) x, (int) y, 200);
    }

    /**
     * Long click at a WGS84 location
     * 
     * @param device the UiDevice
     * @param map the current Map object
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void longClickAtCoordinates(@NonNull UiDevice device, @NonNull Map map, double lon, double lat, boolean recenter) {
        longClickAtCoordinates(device, map, (int) (lon * 1E7), (int) (lat * 1E7), recenter);
    }

    /**
     * Long click at a WGS84 location
     * 
     * @param device the UiDevice
     * @param map the current Map object
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void longClickAtCoordinates(@NonNull UiDevice device, @NonNull Map map, int lonE7, int latE7, boolean recenter) {
        if (recenter) {
            if (map.getZoomLevel() < 15) {
                App.getLogic().setZoom(map, 15); // we want the coordinate to be somewhere in the middle of the screen
            }
            map.getViewBox().moveTo(map, lonE7, latE7);
            map.invalidate();
        }
        float x = GeoMath.lonE7ToX(map.getWidth(), map.getViewBox(), lonE7);
        float y = GeoMath.latE7ToY(map.getHeight(), map.getWidth(), map.getViewBox(), latE7);
        int[] outLocation = new int[2];
        map.getLocationOnScreen(outLocation);
        TestUtils.longClickAt(device, x + outLocation[0], y + outLocation[1]);
    }

    /**
     * Double click at a screen location
     * 
     * @param device the UiDevice
     * @param x screen X coordinate
     * @param y screen y coordinate
     */
    public static void doubleClickAt(@NonNull UiDevice device, float x, float y) {
        Configurator cc = Configurator.getInstance();
        long defaultAckTimeout = cc.getActionAcknowledgmentTimeout();
        cc.setActionAcknowledgmentTimeout(0);
        device.click((int) x, (int) y);
        try {
            Thread.sleep(50); // NOSONAR
        } catch (InterruptedException e) {
        }
        device.click((int) x, (int) y);
        cc.setActionAcknowledgmentTimeout(defaultAckTimeout);
    }

    /**
     * Double click at a WGS84 location
     * 
     * @param device the UiDevice
     * @param map the current Map object
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void doubleClickAtCoordinates(@NonNull UiDevice device, Map map, double lon, double lat, boolean recenter) {
        doubleClickAtCoordinates(device, map, (int) (lon * 1E7), (int) (lat * 1E7), recenter);
    }

    /**
     * Double click at a WGS84 location
     * 
     * @param device the UiDevice
     * @param map the current Map object
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void doubleClickAtCoordinates(@NonNull UiDevice device, Map map, int lonE7, int latE7, boolean recenter) {
        if (recenter) {
            if (map.getZoomLevel() < 15) {
                App.getLogic().setZoom(map, 15); // we want the coordinate to be somewhere in the middle of the screen
            }
            map.getViewBox().moveTo(map, lonE7, latE7);
            map.invalidate();
        }
        float x = GeoMath.lonE7ToX(map.getWidth(), map.getViewBox(), lonE7);
        float y = GeoMath.latE7ToY(map.getHeight(), map.getWidth(), map.getViewBox(), latE7);
        System.out.println("double clicking at " + x + " / " + y);
        int[] outLocation = new int[2];
        map.getLocationOnScreen(outLocation);
        TestUtils.doubleClickAt(device, x + outLocation[0], y + outLocation[1]);
    }

    /**
     * Execute a drag
     * 
     * @param device the UiDevice
     * @param startX start screen X coordinate
     * @param startY start screen Y coordinate
     * @param endX end screen X coordinate
     * @param endY end screen Y coordinate
     * @param steps number of 5ms steps
     */
    public static void drag(@NonNull UiDevice device, float startX, float startY, float endX, float endY, int steps) {
        device.swipe((int) startX, (int) startY, (int) endX, (int) endY, steps);
    }

    /**
     * Execute a drag
     * 
     * @param device the UiDevice
     * @param map map the current Map object
     * @param startLon start WGS84 longitude
     * @param startLat start WGS84 latitude
     * @param endLon end lon WGS84 longitude
     * @param endLat end WGS84 latitude
     * @param recenter center the screen on the start location first if true
     * @param steps number of 5ms steps
     */
    public static void drag(UiDevice device, Map map, double startLon, double startLat, double endLon, double endLat, boolean recenter, int steps) {
        drag(device, map, (int) (startLon * 1E7), (int) (startLat * 1E7), (int) (endLon * 1E7), (int) (endLat * 1E7), recenter, steps);
    }

    /**
     * Execute a drag
     * 
     * @param device the UiDevice
     * @param map map the current Map object
     * @param startLonE7 start WGS84*1E7 longitude
     * @param startLatE7 start WGS84*1E7 latitude
     * @param endLonE7 end lon WGS84*1E7 longitude
     * @param endLatE7 end WGS84*1E7 latitude
     * @param recenter center the screen on the start location first if true
     * @param steps number of 5ms steps
     */
    public static void drag(@NonNull UiDevice device, @NonNull Map map, int startLonE7, int startLatE7, int endLonE7, int endLatE7, boolean recenter,
            int steps) {
        if (recenter) {
            if (map.getZoomLevel() < 15) {
                App.getLogic().setZoom(map, 15); // we want the coordinate to be somewhere in the middle of the screen
            }
            map.getViewBox().moveTo(map, startLonE7, startLatE7);
            map.invalidate();
        }
        float startX = GeoMath.lonE7ToX(map.getWidth(), map.getViewBox(), startLonE7);
        float startY = GeoMath.latE7ToY(map.getHeight(), map.getWidth(), map.getViewBox(), startLatE7);
        float endX = GeoMath.lonE7ToX(map.getWidth(), map.getViewBox(), endLonE7);
        float endY = GeoMath.latE7ToY(map.getHeight(), map.getWidth(), map.getViewBox(), endLatE7);
        int[] outLocation = new int[2];
        map.getLocationOnScreen(outLocation);
        TestUtils.drag(device, startX + outLocation[0], startY + outLocation[1], endX + outLocation[0], endY + outLocation[1], steps);
    }

    /**
     * Unlock the screen if locked
     * 
     * @param device the UiDevice
     */
    public static void unlock(@NonNull UiDevice device) {
        if (App.getLogic().isLocked()) {
            clickOnLock(device);
        }
    }

    /**
     * Click on the lock button
     * 
     * @param device the UiDevice
     */
    private static void clickOnLock(@NonNull UiDevice device) {
        UiObject lock = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/floatingLock"));
        try {
            lock.click();
            device.waitForWindowUpdate(null, 1000);
            clickText(device, false, "OK", false);
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Unlock the screen if locked
     * 
     * @param device the UiDevice
     */
    public static void lock(@NonNull UiDevice device) {
        if (!App.getLogic().isLocked()) {
            clickOnLock(device);
        }
    }

    /**
     * Zoom to a specific zoom level
     * 
     * @param device the UiDevice
     * @param main current Main object
     * @param level level to zoom to
     */
    public static void zoomToLevel(@NonNull UiDevice device, @NonNull Main main, int level) {
        Map map = main.getMap();
        int count = 0;
        int currentLevel = map.getZoomLevel();
        int prevLevel = -1;
        int noChange = 0;
        while (level != currentLevel && count < 20) {
            Log.d(DEBUG_TAG, "Zoom level " + currentLevel);
            if (currentLevel < level) {
                if (level - currentLevel > 3) {
                    pinchOut(device);
                } else {
                    clickButton(device, device.getCurrentPackageName() + ":id/zoom_in", false);
                }
            } else {
                if (currentLevel - level > 3) {
                    pinchIn(device);
                } else {
                    clickButton(device, device.getCurrentPackageName() + ":id/zoom_out", false);
                }
            }
            currentLevel = map.getZoomLevel();
            if (prevLevel == currentLevel) {
                noChange++;
                if (noChange > 3) {
                    Log.e(DEBUG_TAG, "No zoom level change");
                    return;
                }
            } else {
                noChange = 0;
            }
            prevLevel = currentLevel;
            count++;
        }
    }

    /**
     * Click a text on screen (case insensitive, start of a string)
     * 
     * @param device UiDevice object
     * @param clickable clickable if true the search will be restricted to clickable objects
     * @param text text to search (case insensitive, uses textStartsWith)
     * @param waitForNewWindow set the wait for new window flag if true
     * @return true if successful
     */
    public static boolean clickText(@NonNull UiDevice device, boolean clickable, @NonNull String text, boolean waitForNewWindow) {
        return clickText(device, clickable, text, waitForNewWindow, false);
    }

    /**
     * Click a text on screen (case insensitive, start of a string, if exact is false)
     * 
     * @param device UiDevice object
     * @param clickable clickable if true the search will be restricted to clickable objects
     * @param text text to search (case insensitive, uses textStartsWith)
     * @param waitForNewWindow set the wait for new window flag if true
     * @param exact if true use an exact text match
     * @return true if successful
     */
    public static boolean clickText(@NonNull UiDevice device, boolean clickable, @NonNull String text, boolean waitForNewWindow, boolean exact) {
        return clickText(device, clickable, text, waitForNewWindow, exact, 500);
    }

    /**
     * Click a text on screen (case insensitive, start of a string, if exact is false)
     * 
     * @param device UiDevice object
     * @param clickable clickable if true the search will be restricted to clickable objects
     * @param text text to search (case insensitive, uses textStartsWith)
     * @param waitForNewWindow set the wait for new window flag if true
     * @param exact if true use an exact text match
     * @param wait how long to wait for the object to appear in ms
     * @return true if successful
     */
    public static boolean clickText(@NonNull UiDevice device, boolean clickable, @NonNull String text, boolean waitForNewWindow, boolean exact, long wait) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = exact ? By.text(text) : By.textStartsWith(text);
        UiSelector uiSelector = exact ? new UiSelector().text(text) : new UiSelector().textStartsWith(text);
        // NOTE order of the selector terms is significant
        if (clickable) {
            bySelector = bySelector.clickable(true);
            uiSelector = uiSelector.clickable(true);
        }
        device.wait(Until.findObject(bySelector), wait);
        UiObject button = device.findObject(uiSelector);
        if (button.exists()) {
            try {
                if (waitForNewWindow) {
                    button.clickAndWaitForNewWindow();
                } else {
                    button.click();
                }
                Log.e(DEBUG_TAG, ".... clicked");
                return true;
            } catch (UiObjectNotFoundException e) {
                Log.e(DEBUG_TAG, "Object vanished.");
                return false;
            }
        } else {
            Log.e(DEBUG_TAG, "Object not found");
            return false;
        }
    }

    /**
     * Long click a text on screen (case insensitive, start of a string)
     * 
     * @param device UiDevice object
     * @param text text to search (case insensitive, uses textStartsWith)
     * @return true if successful
     */
    public static boolean longClickText(@NonNull UiDevice device, @NonNull String text) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = By.textStartsWith(text);
        UiSelector uiSelector = new UiSelector().textStartsWith(text);
        device.wait(Until.findObject(bySelector), 500);
        UiObject button = device.findObject(uiSelector);
        if (button.exists()) {
            try {
                longClick(device, button);
                Log.e(DEBUG_TAG, ".... clicked");
                return true;
            } catch (UiObjectNotFoundException e) {
                Log.e(DEBUG_TAG, "Object vanished.");
                return false;
            }
        } else {
            Log.e(DEBUG_TAG, "Object not found");
            return false;
        }
    }

    /**
     * Click a text on screen
     * 
     * @param device UiDevice object
     * @param text text to search (case insensitive, uses textStartsWith)
     * @param waitForNewWindow set the wait for new window flag if true
     * @param wait how long to wait for the object to appear in ms
     * @return true if successful
     */
    public static boolean clickTextContains(@NonNull UiDevice device, @NonNull String text, boolean waitForNewWindow, long wait) {
        return clickTextContains(device, false, text, waitForNewWindow, wait);
    }

    /**
     * Click a text on screen (case sensitive, any position in a string)
     * 
     * @param device UiDevice object
     * @param clickable clickable if true the search will be restricted to clickable objects
     * @param text text to search (case sensitive, uses textContains)
     * @param waitForNewWindow set the wait for new window flag if true
     * @return true if successful
     */
    public static boolean clickTextContains(@NonNull UiDevice device, boolean clickable, @NonNull String text, boolean waitForNewWindow) {
        return clickTextContains(device, clickable, text, waitForNewWindow, 500);
    }

    /**
     * Click a text on screen (case sensitive, any position in a string)
     * 
     * @param device UiDevice object
     * @param clickable clickable if true the search will be restricted to clickable objects
     * @param text text to search (case sensitive, uses textContains)
     * @param waitForNewWindow set the wait for new window flag if true
     * @param wait how long to wait for the object to appear in ms
     * @return true if successful
     */
    public static boolean clickTextContains(@NonNull UiDevice device, boolean clickable, @NonNull String text, boolean waitForNewWindow, long wait) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        //
        BySelector bySelector = null;
        UiSelector uiSelector = null;
        // NOTE order of the selector terms is significant
        if (clickable) {
            bySelector = By.clickable(true).textContains(text);
            uiSelector = new UiSelector().clickable(true).textContains(text);
        } else {
            bySelector = By.textContains(text);
            uiSelector = new UiSelector().textContains(text);
        }
        device.wait(Until.findObject(bySelector), wait);
        UiObject button = device.findObject(uiSelector);
        if (button.exists()) {
            try {
                if (waitForNewWindow) {
                    button.clickAndWaitForNewWindow();
                } else {
                    button.click();
                    Log.e(DEBUG_TAG, ".... clicked");
                }
                return true;
            } catch (UiObjectNotFoundException e) {
                Log.e(DEBUG_TAG, "Object vanished.");
                return false;
            }
        } else {
            Log.e(DEBUG_TAG, "Object not found");
            return false;
        }
    }

    /**
     * Find text on screen (case insensitive)
     * 
     * @param device UiDevice object
     * @param clickable if true the search will be restricted to clickable objects
     * @param text the text to find
     * @return true if successful
     */
    public static boolean findText(@NonNull UiDevice device, boolean clickable, @NonNull String text) {
        return findText(device, clickable, text, 500);
    }

    /**
     * Find text on screen (case insensitive)
     * 
     * @param device UiDevice object
     * @param clickable if true the search will be restricted to clickable objects
     * @param text the text to find
     * @param wait time to wait in ms before timing out
     * @return true if successful
     */
    public static boolean findText(@NonNull UiDevice device, boolean clickable, @NonNull String text, long wait) {
        Log.w(DEBUG_TAG, "Searching for  " + text);
        return findText(device, clickable, text, wait, false);
    }

    /**
     * Find text on screen
     * 
     * @param device UiDevice object
     * @param clickable if true the search will be restricted to clickable objects
     * @param text the text to find
     * @param wait time to wait in ms before timing out
     * @param contains if true check if text contains the value case sensitive, if false check if text starts with the
     *            text case insensitive
     * @return true if successful
     */
    public static boolean findText(@NonNull UiDevice device, boolean clickable, @NonNull String text, long wait, boolean contains) {
        Log.w(DEBUG_TAG, "Searching for  " + text);
        return findObjectWithText(device, clickable, text, wait, contains) != null;
    }

    /**
     * Find object with text on screen
     * 
     * @param device UiDevice object
     * @param clickable if true the search will be restricted to clickable objects
     * @param text the text to find
     * @param wait time to wait in ms before timing out
     * @param contains if true check if text contains the value case sensitive, if false check if text starts with the
     *            text case insensitive
     * @return a UiObject2 if successful or null
     */
    @Nullable
    public static UiObject2 findObjectWithText(@NonNull UiDevice device, boolean clickable, @NonNull String text, long wait, boolean contains) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = contains ? By.textContains(text) : By.textStartsWith(text);
        if (clickable) {
            bySelector = bySelector.clickable(true);
        }
        return device.wait(Until.findObject(bySelector), wait);
    }

    /**
     * Wait until text on screen is gone (case insensitive)
     * 
     * @param device UiDevice object
     * @param text the text to find
     * @param wait time to wait in ms before timing out
     * @return true if successful
     */
    public static boolean textGone(@NonNull UiDevice device, @NonNull String text, long wait) {
        Log.w(DEBUG_TAG, "Waiting for object with " + text + " to go away");
        return device.wait(Until.gone(By.textStartsWith(text)), wait) != null;
    }

    /**
     * Click on an object
     * 
     * @param device UiDevice object
     * @param clickable if true the search will be restricted to clickable objects
     * @param resourceId resource id of the object
     * @param waitForNewWindow set the wait for new window flag if true
     * @return true if successful
     */
    public static boolean clickResource(@NonNull UiDevice device, boolean clickable, @NonNull String resourceId, boolean waitForNewWindow) {
        Log.w(DEBUG_TAG, "Searching for object with " + resourceId);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        UiObject button = findObjectWithResourceId(device, clickable, resourceId);
        if (button.exists()) {
            try {
                if (waitForNewWindow) {
                    button.clickAndWaitForNewWindow();
                } else {
                    button.click();
                    Log.e(DEBUG_TAG, ".... clicked");
                }
                return true;
            } catch (UiObjectNotFoundException e) {
                Log.e(DEBUG_TAG, "Object vanished.");
                return false;
            }
        } else {
            Log.e(DEBUG_TAG, "Object not found");
            return false;
        }
    }

    /**
     * Find an object with its resource id
     * 
     * @param device the UiDevice object
     * @param clickable true if object is clickable
     * @param resourceId the resource id
     * @return an UiObject
     */
    @NonNull
    public static UiObject findObjectWithResourceId(@NonNull UiDevice device, boolean clickable, @NonNull String resourceId) {
        BySelector bySelector = null;
        UiSelector uiSelector = null;
        // NOTE order of the selector terms is significant
        if (clickable) {
            bySelector = By.clickable(true).res(resourceId);
            uiSelector = new UiSelector().clickable(true).resourceId(resourceId);
        } else {
            bySelector = By.res(resourceId);
            uiSelector = new UiSelector().resourceId(resourceId);
        }
        device.wait(Until.findObject(bySelector), 5000);
        return device.findObject(uiSelector);
    }

    /**
     * Find the nth instance of an object with a specific resource id
     * 
     * @param device the UiDevice object
     * @param resourceId the resource id
     * @param instance the instance (0 based)
     * @return an UiObject
     */
    @NonNull
    public static UiObject findObjectWithResourceId(@NonNull UiDevice device, @NonNull String resourceId, int instance) {
        UiSelector uiSelector = null;
        uiSelector = new UiSelector().resourceId(resourceId).instance(instance);
        return device.findObject(uiSelector);
    }

    /**
     * Click "Home" button in Activity app bars
     * 
     * @param device UiDevice object
     * @param fail if true fail if object is not found
     * @return true if the button was clicked
     */
    public static boolean clickHome(@NonNull UiDevice device, boolean fail) {
        UiObject homeButton = device.findObject(new UiSelector().clickable(true).descriptionStartsWith("Navigate up"));
        if (!homeButton.exists()) {
            homeButton = device.findObject(new UiSelector().clickable(true).descriptionStartsWith("Nach oben"));
        }
        try {
            return homeButton.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e) {
            if (fail) {
                Assert.fail(e.getMessage());
            }
            return false; // can't actually be reached
        }
    }

    /**
     * Click "Up" button in Action modes
     * 
     * @param device UiDevice object
     */
    public static void clickUp(@NonNull UiDevice device) {
        clickResource(device, true, device.getCurrentPackageName() + ":id/action_mode_close_button", true);
    }

    /**
     * Buffered read an InputStream into a byte array
     * 
     * @param is the InputStream to read
     * @return a byte array
     * @throws IOException ir reading goes wrong
     */
    public static byte[] readInputStream(@NonNull InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readBytes = -1;
        try {
            while ((readBytes = is.read(buffer)) > -1) {
                baos.write(buffer, 0, readBytes);
            }
        } finally {
            is.close();
        }
        return baos.toByteArray();
    }

    /**
     * Setup a mock location provider
     * 
     * @param context an Android context
     * @param providerCriteria if coarse use network, if fine use gps
     */
    public static void setupMockLocation(@NonNull final Context context, final int providerCriteria) {
        String provider = "none";
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        } catch (IllegalArgumentException iaex) {
            // according to googles doc this shouldn't happen
            Log.e(DEBUG_TAG, "injectLocation " + iaex.getMessage());
        }
        locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, // requiresNetwork
                false, // requiresSatellite
                false, // requiresCell
                false, // hasMonetaryCost
                true, // supportsAltitude
                true, // supportsSpeed
                true, // supportsBearing
                1, // powerRequirement see
                   // https://developer.android.com/reference/android/location/provider/ProviderProperties
                1 // accuracy
        );
        try {
            locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
        } catch (IllegalArgumentException iaex) {
            // ignore
            Log.e(DEBUG_TAG, "injectLocation " + iaex.getMessage());
        }
        locationManager.addTestProvider(LocationManager.NETWORK_PROVIDER, false, // requiresNetwork
                false, // requiresSatellite
                false, // requiresCell
                false, // hasMonetaryCost
                true, // supportsAltitude
                true, // supportsSpeed
                true, // supportsBearing
                1, // powerRequirement
                1 // accuracy
        );

        if (providerCriteria == Criteria.ACCURACY_FINE) {
            provider = LocationManager.GPS_PROVIDER;
        } else {
            provider = LocationManager.NETWORK_PROVIDER;
        }

        Log.d(DEBUG_TAG, "injectLocation Provider " + provider);
        locationManager.setTestProviderEnabled(provider, true);
        for (String p : locationManager.getAllProviders()) {
            Log.d(DEBUG_TAG, "injectLocation available " + p);
        }
    }

    /**
     * Inject a Location to the app in testing
     * 
     * @param context Android context
     * @param lat Latitude
     * @param lon Longitude
     * @param interval interval between values
     * @param handler handler to call when we are finished
     */
    public static void injectLocation(@NonNull final Context context, final double lat, final double lon, final int interval,
            @Nullable final SignalHandler handler) {
        List<TrackPoint> track = new ArrayList<>();
        TrackPoint tp = new TrackPoint((byte) 0, lat, lon, 0, System.currentTimeMillis());
        track.add(tp);
        injectLocation(context, track, Criteria.ACCURACY_FINE, interval, handler);
    }

    /**
     * Inject a Location to the app in testing
     * 
     * @param context Android context
     * @param track List of TrackPoints
     * @param providerCriteria selection criteria for the provider
     * @param interval interval between values
     * @param handler handler to call when we are finished
     */
    public static void injectLocation(@NonNull final Context context, @NonNull final List<TrackPoint> track, final int providerCriteria, final int interval,
            @Nullable final SignalHandler handler) {

        new ExecutorTask<Void, Void, Void>() {
            String          provider        = "none";
            LocationManager locationManager = null;

            @Override
            protected void onPreExecute() {
                Log.d(DEBUG_TAG, "injectLocation injecting " + track.size() + " Locations");
                locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                if (providerCriteria == Criteria.ACCURACY_FINE) {
                    provider = LocationManager.GPS_PROVIDER;
                } else {
                    provider = LocationManager.NETWORK_PROVIDER;
                }
            }

            @Override
            protected Void doInBackground(Void arg) {
                Location loc = new Location(provider);
                loc.setAccuracy(5.0f);
                for (TrackPoint tp : track) {
                    loc.setLatitude(tp.getLatitude());
                    loc.setLongitude(tp.getLongitude());
                    if (tp.hasAltitude()) {
                        loc.setAltitude(tp.getAltitude());
                    }
                    loc.setTime(System.currentTimeMillis());
                    loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    locationManager.setTestProviderLocation(provider, loc);
                    TestUtils.sleep(interval);
                }
                TestUtils.sleep(interval); // let location drain
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (handler != null) {
                    handler.onSuccess();
                }
            }
        }.execute();
    }

    /**
     * Finish any currently running EasyEdit modes, switch to simple mode, and turn off any filters
     * 
     * @param main the current Main instance
     */
    public static void stopEasyEdit(@NonNull final Main main) {
        Logic logic = App.getLogic();
        logic.deselectAll();
        Preferences prefs = logic.getPrefs();
        prefs.enableSimpleActions(true);
        prefs.enablePresetFilter(false);
        prefs.enableTagFilter(false);
        Filter filter = logic.getFilter();
        if (filter != null) {
            main.runOnUiThread(() -> {
                filter.hideControls();
                filter.removeControls();
                logic.setFilter(null);
            });
        }
        logic.setMode(main, Mode.MODE_EASYEDIT);
        main.runOnUiThread(() -> main.getEasyEditManager().finish());

    }

    /**
     * Get one of the buttons for a specific layer
     * 
     * @param device the current UIDevice
     * @param layer the name of the layer
     * @param buttonIndex the index of the button in the TableRow
     * 
     * @return an UiObject2 for the button in question
     */
    public static UiObject2 getLayerButton(@NonNull UiDevice device, @NonNull String layer, int buttonIndex) {
        Assert.assertTrue(TestUtils.clickResource(device, true, device.getCurrentPackageName() + ":id/layers", true));
        BySelector bySelector = By.textStartsWith(layer);
        UiObject2 layerName = device.wait(Until.findObject(bySelector), 500);
        assertNotNull(layerName);
        UiObject2 tableRow = layerName.getParent();
        List<UiObject2> tableCells = tableRow.getChildren();
        return tableCells.get(buttonIndex);
    }

    /**
     * Select a file from the file picker
     * 
     * @param device the current UiDevice
     * @param context Android Context
     * @param directory optional sub-directory
     * @param fileName the name of the file
     * @param useVespucciDir if true use the Vespucci directory on external storage
     */
    public static void selectFile(@NonNull UiDevice device, @NonNull Context context, @Nullable String directory, @NonNull String fileName,
            boolean useVespucciDir) {
        selectFile(device, context, directory, fileName, useVespucciDir, false);
    }

    /**
     * Select a file from the file picker
     * 
     * @param device the current UiDevice
     * @param context Android Context
     * @param directory optional sub-directory
     * @param fileName the name of the file
     * @param useVespucciDir if true use the Vespucci directory on external storage
     * @param create if true create a new file with the name
     */
    public static void selectFile(@NonNull UiDevice device, @NonNull Context context, @Nullable String directory, @NonNull String fileName,
            boolean useVespucciDir, boolean create) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            // we should see the file picker selector here
            assertTrue(findText(device, false, context.getString(R.string.select_file_picker_title)));
            assertTrue(clickText(device, false, "Files", true));
        }
        // if the fileName we are looking for is on screen click and be done with it
        if (!create && TestUtils.findText(device, false, fileName) && TestUtils.clickText(device, false, fileName, true, true)) {
            return;
        }
        UiSelector scrollableSelector = Build.VERSION.SDK_INT > Build.VERSION_CODES.P ? new UiSelector().className("android.widget.FrameLayout")
                : Build.VERSION.SDK_INT > Build.VERSION_CODES.N ? new UiSelector().scrollable(true).className("android.support.v7.widget.RecyclerView")
                        : new UiSelector().scrollable(true).className("android.widget.ListView");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TestUtils.clickOverflowButton(device);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && !TestUtils.findText(device, false, "SDCARD", 2000)) {
                // old stuff
                TestUtils.clickText(device, false, "Settings", true, false);
                UiObject cb = device.findObject(new UiSelector().resourceId("android:id/checkbox"));
                try {
                    if (!cb.isChecked()) {
                        TestUtils.clickText(device, false, "Display advanced devices", false, false);
                    }
                } catch (UiObjectNotFoundException e) {
                    Assert.fail("Coudn't turn on SDCARD view");
                }
                TestUtils.clickText(device, false, "Settings", true, false);
                TestUtils.clickResource(device, false, "android:id/up", true);
                TestUtils.clickText(device, false, "SDCARD", true, false);
            } else {
                if (!TestUtils.clickText(device, false, "Show", false, false)) {
                    TestUtils.clickAt(device, device.getDisplayWidth() / 2, device.getDisplayHeight() / 2);
                }
                TestUtils.clickMenuButton(device, "List view", false, false);
                if (!TestUtils.clickMenuButton(device, "Show roots", false, true)) {
                    // TestUtils.clickResource(device, false, "android:id/roots_toolbar", true);
                    UiObject drawerButton = device.findObject(new UiSelector().classNameMatches("^.*.ImageButton$"));
                    try {
                        drawerButton.clickAndWaitForNewWindow();
                    } catch (UiObjectNotFoundException e) {

                    }
                }

                UiSelector android = new UiSelector().resourceIdMatches(".*:id/title")
                        .textMatches("(^Android SDK.*)|(^AOSP.*)|(^Internal.*)|(^Samsung.*)|(^sdk_.*)");
                UiObject androidButton = device.findObject(android);
                try {
                    androidButton.clickAndWaitForNewWindow();
                } catch (UiObjectNotFoundException e1) {
                    Assert.fail("Link to internal storage not found in drawer");
                }
            }
            String storagePath = "Android/data/" + context.getPackageName() + "/files";
            if (useVespucciDir) {
                storagePath = "Download/Vespucci"; // FIXME use FileUtil...
            }
            selectDirectory(device, storagePath, scrollableSelector);
        }
        if (directory != null) {
            scrollToAndSelect(device, directory, scrollableSelector);
        }
        if (create) {
            UiObject editText = device.findObject(new UiSelector().classNameMatches("^.*.EditText$"));
            try {
                editText.setText(fileName);
                findText(device, false, fileName, 2000);
                clickText(device, false, context.getString(R.string.save), true);
            } catch (UiObjectNotFoundException e) {
                Assert.fail(e.getMessage());
            }
        } else {
            scrollToAndSelect(device, fileName, scrollableSelector);
        }
    }

    /**
     * Delete a file in the Vespucci directory
     * 
     * Very hackish for Android 30 up
     * 
     * @param ctx Android context
     * @param fileName the relative path to the file
     */
    public static void deleteFile(@NonNull Context ctx, @NonNull String fileName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 and up
                ContentResolver cr = ctx.getContentResolver();
                String uriString = "content://com.android.externalstorage.documents/document/primary%3ADownload%2FVespucci%2F" + Uri.encode(fileName);
                if (!DocumentsContract.deleteDocument(cr, Uri.parse(uriString))) {
                    throw new IOException(uriString);
                }
            } else {
                File testFile = new File(FileUtil.getPublicDirectory(), fileName);
                Files.delete(testFile.toPath());
            }
        } catch (IOException e) {
            fail("Deleting file failed " + e.getMessage());
        }
    }

    /**
     * Determine if soft keyboard is visible
     * 
     * @param device the UiDevice
     * @return true is the keyboard is visible
     */
    private static boolean softKeyboardIsVisible(@NonNull UiDevice device) {
        UiObject editText = device.findObject(new UiSelector().classNameMatches("^.*.EditText$"));
        try {
            Rect bounds = editText.getVisibleBounds();
            int height = device.getDisplayHeight();
            return height - bounds.bottom > height / 4; // view has been pushed up
        } catch (UiObjectNotFoundException e) {
            return false;
        }
    }

    /**
     * Iterate over the specified path selecting the entry in the SAF file selector one after the other
     * 
     * @param device the UiDevice
     * @param path the path
     * @param scrollableSelector what Android widget is used for scrolling
     */
    private static void selectDirectory(@NonNull UiDevice device, @NonNull String path, @NonNull UiSelector scrollableSelector) {
        System.out.println("Path: " + path);
        String[] dirs = path.split("/");
        String prev = null;
        if (dirs.length > 0) {
            for (String dir : dirs) {
                if (!"".equals(dir)) {
                    if (!scrollToAndSelect(device, dir, scrollableSelector)) {
                        if (prev != null) {
                            // retry to get around suspected flaky android resend back button pressed
                            scrollToAndSelect(device, prev, scrollableSelector);
                            if (!scrollToAndSelect(device, dir, scrollableSelector)) {
                                Assert.fail("selectDirectory failed click on " + dir);
                            }
                        } else {
                            Assert.fail("selectDirectory failed click on " + dir + " no prev dir");
                        }
                    }
                    prev = dir;
                }
            }
        }
    }

    /**
     * Scroll to an entry in the SAF file selector and select it
     * 
     * 
     * @param device the UiDevice
     * @param entry the text of the entry to select
     * @param scrollableSelector what Android widget is used for scrolling
     * @return true if successful
     */
    public static boolean scrollToAndSelect(@NonNull UiDevice device, @NonNull String entry, @NonNull UiSelector scrollableSelector) {
        hideSoftKeyboard(device);

        if (!findText(device, false, entry, 1000)) { // only scroll if text isn't already visible
            UiScrollable appView;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                appView = new UiScrollable(scrollableSelector);
            } else {
                appView = new UiScrollable(new UiSelector().scrollable(true));
            }
            appView.setSwipeDeadZonePercentage(0.2);
            try {
                appView.scrollIntoView(new UiSelector().text(entry));
            } catch (UiObjectNotFoundException e) {
                // if there is no scrollable then this will fail
            }
        }

        if (!clickText(device, false, entry, true, true, 1000)) {
            Log.i(DEBUG_TAG, "scrollToAndSelect failed click on " + entry);
            return false;
        }
        return true;
    }

    /**
     * Try to hide any visible softkeyboard
     * 
     * @param device the UiDevice
     */
    private static void hideSoftKeyboard(@NonNull UiDevice device) {
        if (softKeyboardIsVisible(device)) {
            device.pressBack();
            device.waitForWindowUpdate(null, 10000);
        }
    }

    /**
     * Scroll to a specific text
     * 
     * @param text the text
     * @param fail fail if scrollable not found
     */
    public static void scrollTo(@NonNull String text, boolean fail) {
        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
        try {
            appView.setSwipeDeadZonePercentage(0.4);
            appView.scrollIntoView(new UiSelector().text(text));
        } catch (UiObjectNotFoundException e) {
            if (fail) {
                Assert.fail(text + " not found");
            }
        }
    }

    /**
     * Scroll to a specific text
     * 
     * @param text the text
     * @param fail fail if scrollable not found
     */
    public static void scrollToStartsWith(@NonNull String text, boolean fail) {
        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
        try {
            appView.setSwipeDeadZonePercentage(0.4);
            appView.scrollIntoView(new UiSelector().textStartsWith(text));
        } catch (UiObjectNotFoundException e) {
            if (fail) {
                Assert.fail(text + " not found");
            }
        }
    }

    /**
     * Scroll to a specific text
     * 
     * @param text the text
     * @param fail fail if scrollable not found
     */
    public static void scrollToStartsWith(@NonNull UiDevice device, @NonNull String text, int maxSwipes, boolean fail) {
        BySelector bySelector = By.textStartsWith(text);
        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
        appView.setSwipeDeadZonePercentage(0.4);
        try {
            for (int swipes = 0; swipes < maxSwipes; maxSwipes++) {
                appView.scrollForward(10);
                if (device.wait(Until.findObject(bySelector), 500) != null) {
                    return;
                }
            }
        } catch (UiObjectNotFoundException e) {
            Assert.fail("UiScrollable not found");
        }
        if (fail) {
            Assert.fail(text + " not found");
        }
    }

    /**
     * Scroll to end
     * 
     * @param fail fail if scrollable not found
     * 
     */
    public static void scrollToEnd(boolean fail) {
        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
        try {
            appView.scrollToEnd(100);
        } catch (UiObjectNotFoundException e) {
            if (fail) {
                Assert.fail("UiScrollable not found");
            }
        }
    }

    /**
     * Zap all offsets for the background layer
     * 
     * @param map the current Map object
     */
    public static void resetOffsets(@NonNull Map map) {
        MapTilesLayer<?> layer = map.getBackgroundLayer();
        if (layer != null) {
            TileLayerSource osmts = layer.getTileLayerConfiguration();
            if (osmts != null) {
                osmts.setOffsets(new Offset[osmts.getMaxZoom() - osmts.getMinZoom() + 1]);
            } else {
                Log.e(DEBUG_TAG, "resetOffsets osmts is null");
            }
        } else {
            Log.e(DEBUG_TAG, "resetOffsets layer is null");
        }
    }

    /**
     * Wait a second
     */
    public static void sleep() {
        sleep(1000);
    }

    /**
     * Wait
     * 
     * Unluckily while this is bad because it can make testing load dependent there doesn't seem to be a way around this
     * 
     * @param snore number of ms to sleep
     */
    public static void sleep(long snore) {
        try {
            Thread.sleep(snore); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            // do nothing
        }
    }

    /**
     * Create a MockResponse for a binary file
     * 
     * MockWebServerPlus currently doesn't handle non-text bodies properly so we do this manually
     *
     * @param contentType the content type we are returning
     * @param fixture the filename of the fixture
     * @return a MockResponse
     */
    public static MockResponse createBinaryReponse(@NonNull String contentType, @NonNull String fixture) {
        MockResponse response = new MockResponse();
        response.setHeader("Content-type", contentType);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream(fixture);
        Buffer buffer = new Buffer();
        try {
            buffer.readFrom(inputStream);
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        }
        response.setBody(buffer);
        return response;
    }

    /**
     * Load test data
     * 
     * @param activity the calling activity
     * @param fileName file name for the test data
     */
    public static void loadTestData(@NonNull FragmentActivity activity, @NonNull String fileName) {
        final CountDownLatch signal1 = new CountDownLatch(1);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(fileName);
        App.getLogic().readOsmFile(activity, is, false, new SignalHandler(signal1));
        try {
            signal1.await(TransferMenuTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
    }

    /**
     * Click the "simple" button
     * 
     * @param device the current UiDevice
     */
    public static void clickSimpleButton(@NonNull UiDevice device) {
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
    }

    /**
     * Click the "simple" button
     * 
     * @param device the current UiDevice
     * @param wait time till timeout
     */
    public static void clickSimpleButton(@NonNull UiDevice device, long wait) {
        waitForSimpleButton(device, wait);
        TestUtils.clickButton(device, device.getCurrentPackageName() + ":id/simpleButton", true);
    }

    /**
     * Wait for the "simple" button
     * 
     * @param device the current UiDevice
     * @param wait time till timeout
     */
    public static void waitForSimpleButton(@NonNull UiDevice device, long wait) {
        device.wait(Until.findObject(By.res(device.getCurrentPackageName() + ":id/simpleButton")), wait);
    }

    /**
     * Get the lock button
     * 
     * @param device the current UiDevice
     * @return the lock button
     */
    @Nullable
    public static UiObject getLock(@NonNull UiDevice device) {
        return device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/floatingLock"));
    }

    /**
     * Find a specific notification
     * 
     * @param device the current UiDevice
     * @param message the message to find
     * @return true if found
     */
    public static boolean findNotification(@NonNull UiDevice device, @NonNull String message) {
        return findNotification(device, VESPUCCI, message);
    }

    /**
     * Find a specific notification
     * 
     * @param device the current UiDevice
     * @param app the app that generated the notification
     * @param message the message to find
     * @return true if found
     */
    public static boolean findNotification(@NonNull UiDevice device, @NonNull String app, @NonNull String message) {
        if (device.openNotification()) {
            boolean found = device.wait(Until.hasObject(By.textContains(message)), 5000);
            if (!found) {
                UiObject2 notification = device.findObject(By.textContains(app));
                if (notification != null) {
                    notification.click();
                    found = device.wait(Until.hasObject(By.textContains(message)), 5000);
                }
            }
            scrollToStartsWith("CLEAR ALL", false);
            UiObject2 clearAll = device.findObject(By.text(Pattern.compile("CLEAR ALL", Pattern.CASE_INSENSITIVE)));
            if (clearAll != null) {
                clearAll.click();
            } else {
                UiObject2 notification = device.findObject(By.textContains(app));
                if (notification != null) {
                    Rect rect = notification.getVisibleBounds();
                    device.swipe(rect.left, rect.centerY(), rect.right + 500, rect.centerY(), 5);
                } else {
                    device.pressBack();
                }
            }
            return found;
        }
        return false;
    }

    /**
     * Click on a specific notification
     * 
     * @param device the current UiDevice
     * @param message the message to find
     * @return true if found
     */
    public static boolean clickNotification(@NonNull UiDevice device, @NonNull String message) {
        return clickNotification(device, VESPUCCI, message);
    }

    /**
     * Click on a specific notification
     * 
     * @param device the current UiDevice
     * @param app the app that generated the notification
     * @param message the message to find
     * @return true if found
     */
    public static boolean clickNotification(@NonNull UiDevice device, @NonNull String app, @NonNull String message) {
        if (device.openNotification()) {
            boolean found = device.wait(Until.hasObject(By.textContains(message)), 5000);
            if (!found) {
                UiObject2 notification = device.findObject(By.textContains(app));
                if (notification != null) {
                    notification.click();
                    found = device.wait(Until.hasObject(By.textContains(message)), 5000);
                }
            }
            if (found) {
                UiObject2 notification = device.findObject(By.textContains(message));
                if (notification != null) {
                    notification.click();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Disable a tip
     * 
     * @param ctx an Android Context
     * @param res the tip key
     */
    public static void disableTip(@NonNull Context ctx, int res) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(ctx.getString(res), false).commit();
    }

    /**
     * Click away a tip dialog
     * 
     * @param device the current UiDevice
     * @param ctx an Android Context
     */
    public static void clickAwayTip(@NonNull UiDevice device, @NonNull Context ctx) {
        clickAwayTip(device, ctx, 500);
    }

    /**
     * Click away a tip dialog
     * 
     * @param device the current UiDevice
     * @param ctx an Android Context
     * @param wait time mS to wait for the tip
     */
    public static void clickAwayTip(@NonNull UiDevice device, @NonNull Context ctx, long wait) {
        if (TestUtils.findText(device, false, ctx.getString(R.string.tip_title), wait)) {
            TestUtils.clickText(device, false, ctx.getString(R.string.okay), true, false); // TIP
        }
    }

    /**
     * Switch the simple mode checkbox/pref
     * 
     * @param on if true it should be turned on if it is not on, if false turned off
     */
    public static void switchSimpleMode(@NonNull UiDevice device, @NonNull Activity activity, boolean on) {
        if (TestUtils.clickOverflowButton(device)) {
            UiObject2 modesMenu = TestUtils.findObjectWithText(device, false, activity.getString(R.string.menu_modes), 5000, false);
            modesMenu.click();
            UiObject2 simpleMode = TestUtils.findObjectWithText(device, false, activity.getString(R.string.menu_simple_actions), 5000, false);
            if (simpleMode != null) {
                UiObject2 check = simpleMode.getParent().getParent().getChildren().get(1);
                assertTrue(check.isCheckable());
                if ((on && !check.isChecked()) || (!on && check.isChecked())) {
                    check.click();
                } else {
                    device.pressBack();
                }
            } else {
                Log.e("toggleSimpleMode", "Simple mode check not found");
                device.pressBack();
            }
        } else {
            Log.e("toggleSimpleMode", "no overflowbutton");
        }
    }
}