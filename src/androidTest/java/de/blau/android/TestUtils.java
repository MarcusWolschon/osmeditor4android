package de.blau.android;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import de.blau.android.imageryoffset.Offset;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.resources.TileLayerServer.Category;
import de.blau.android.resources.TileLayerServer.Provider;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.views.layers.MapTilesLayer;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Various methods to support testing
 * 
 * @author Simon Poole
 *
 */
public class TestUtils {
    private static final String DEBUG_TAG = "TestUtils";

    /**
     * Grant permissions by clicking on the dialogs, currently only works for English and German
     * 
     * @param device the UiDevice
     */
    public static void grantPermissons(@NonNull UiDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean notdone = true;
            while (notdone) {
                notdone = clickText(device, true, "allow", true) || clickText(device, true, "zulassen", false);
            }
        }
    }

    /**
     * Dismiss initial welcome dialog and download
     * 
     * @param device the UiDevice
     * @param ctx Android context
     */
    public static void dismissStartUpDialogs(@NonNull UiDevice device, @NonNull Context ctx) {
        clickText(device, true, ctx.getResources().getString(R.string.okay), false);
        if (findText(device, false, "Download", 2000)) {
            clickHome(device);
        }
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
        if (findText(device, false, "Open with Vespucci")) {
            if (findText(device, false, "Share on OpenStreetMap")) {
                clickText(device, false, "Just once", false);
            } else {
                // Open with Vespucci was actually Share on OpenStreetMap
                clickText(device, false, "Vespucci", false);
            }
        } else {
            clickText(device, false, "Vespucci", false);
            if (!clickText(device, false, "Just once", false)) {
                clickText(device, false, "Nur diesmal", false);
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
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = By.clickable(true).descStartsWith(description);
        UiSelector uiSelector = new UiSelector().clickable(true).descriptionStartsWith(description);
        device.wait(Until.findObject(bySelector), 500);
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
        try {
            Thread.sleep(2000); // NOSONAR
        } catch (InterruptedException e1) {
        }
    }

    /**
     * Long click at a screen location
     * 
     * @param devie the UiDevice
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
        cc.setActionAcknowledgmentTimeout(40);
        device.click((int) x, (int) y);
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
            UiObject lock = device.findObject(new UiSelector().resourceId(device.getCurrentPackageName() + ":id/floatingLock"));
            try {
                lock.click();
                device.waitForWindowUpdate(null, 1000);
            } catch (UiObjectNotFoundException e) {
                Assert.fail(e.getMessage());
            }
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
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = null;
        UiSelector uiSelector = null;
        // NOTE order of the selector terms is significant
        if (clickable) {
            bySelector = By.clickable(true).textStartsWith(text);
            uiSelector = new UiSelector().clickable(true).textStartsWith(text);
        } else {
            bySelector = By.textStartsWith(text);
            uiSelector = new UiSelector().textStartsWith(text);
        }
        device.wait(Until.findObject(bySelector), 500);
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
     * Click a text on screen (case sensitive, any position in a string)
     * 
     * @param device UiDevice object
     * @param clickable clickable if true the search will be restricted to clickable objects
     * @param text text to search (case sensitive, uses textContains)
     * @param waitForNewWindow set the wait for new window flag if true
     * @return true if successful
     */
    public static boolean clickTextContains(@NonNull UiDevice device, boolean clickable, @NonNull String text, boolean waitForNewWindow) {
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
        device.wait(Until.findObject(bySelector), 500);
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
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = By.textStartsWith(text);
        if (clickable) {
            bySelector = bySelector.clickable(true);
        }
        return device.wait(Until.findObject(bySelector), wait) != null;
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
     * Click "Home" button in Activity app bars
     * 
     * @param device UiDevice object
     * @return true if the button was clicked
     */
    public static boolean clickHome(@NonNull UiDevice device) {
        UiObject homeButton = device.findObject(new UiSelector().clickable(true).descriptionStartsWith("Navigate up"));
        if (!homeButton.exists()) {
            homeButton = device.findObject(new UiSelector().clickable(true).descriptionStartsWith("Nach oben"));
        }
        try {
            return homeButton.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
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

        new AsyncTask<Void, Void, Void>() {
            String          provider        = "none";
            LocationManager locationManager = null;

            @Override
            protected void onPreExecute() {
                System.out.println("Injecting " + track.size() + " Locations");
                locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                try {
                    locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
                } catch (IllegalArgumentException iaex) {
                    // according to googles doc this shouldn't happen
                }
                locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, // requiresNetwork
                        false, // requiresSatellite
                        false, // requiresCell
                        false, // hasMonetaryCost
                        true, // supportsAltitude
                        true, // supportsSpeed
                        true, // supportsBearing
                        0, // powerRequirement
                        5 // accuracy
                );
                try {
                    locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
                } catch (IllegalArgumentException iaex) {
                    // ignore
                }
                locationManager.addTestProvider(LocationManager.NETWORK_PROVIDER, false, // requiresNetwork
                        false, // requiresSatellite
                        false, // requiresCell
                        false, // hasMonetaryCost
                        true, // supportsAltitude
                        true, // supportsSpeed
                        true, // supportsBearing
                        0, // powerRequirement
                        500 // accuracy
                );

                if (providerCriteria == Criteria.ACCURACY_FINE) {
                    provider = LocationManager.GPS_PROVIDER;
                } else {
                    provider = LocationManager.NETWORK_PROVIDER;
                }

                System.out.println("Provider " + provider);
                locationManager.setTestProviderEnabled(provider, true);
            }

            @Override
            protected Void doInBackground(Void... arg) {
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
                    System.out.println("injecting " + loc.toString());
                    locationManager.setTestProviderLocation(provider, loc);
                    try {
                        Thread.sleep(interval); // NOSONAR
                    } catch (InterruptedException e) {
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                try {
                    Thread.sleep(1000); // NOSONAR
                } catch (InterruptedException e) {
                }
                if (handler != null) {
                    handler.onSuccess();
                }
            }
        }.execute();
    }

    /**
     * Finish any currently running EasyEdit modes
     * 
     * @param main the current Main instance
     */
    public static void stopEasyEdit(@NonNull final Main main) {
        App.getLogic().deselectAll();
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                main.getEasyEditManager().finish();
            }
        });
    }

    /**
     * Copy a file from resources to a sub-directory of the public Vespucci directory
     * 
     * @param fileName the name of the file to copy
     * @param destination the destination sub-directory
     * @throws IOException if copying goes wrong
     */
    public static void copyFileFromResources(@NonNull String fileName, @NonNull String destination) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream(fileName);

        File destinationDir = FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), destination);
        File destinationFile = new File(destinationDir, fileName);
        OutputStream os = new FileOutputStream(destinationFile);

        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.flush();
        SavingHelper.close(is);
        SavingHelper.close(os);
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
        BySelector bySelector = By.text(layer);
        UiObject2 layerName = device.wait(Until.findObject(bySelector), 500);
        UiObject2 tableRow = layerName.getParent();
        List<UiObject2> tableCells = tableRow.getChildren();
        UiObject2 extentButton = tableCells.get(buttonIndex);
        return extentButton;
    }

    /**
     * Select a file from the file picker
     * 
     * @param device the current UiDevice
     * @param directory optional sub-directory
     * @param fileName the name of the file
     */
    public static void selectFile(@NonNull UiDevice device, @Nullable String directory, @NonNull String fileName) {
        UiSelector scrollableSelector = Build.VERSION.SDK_INT > Build.VERSION_CODES.P ? new UiSelector().className("android.widget.FrameLayout")
                : Build.VERSION.SDK_INT > Build.VERSION_CODES.N ? new UiSelector().scrollable(true).className("android.support.v7.widget.RecyclerView")
                        : new UiSelector().scrollable(true).className("android.widget.ListView");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TestUtils.clickOverflowButton(device);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && !TestUtils.findText(device, false, "SDCARD", 2000)) {
                // old stuff
                TestUtils.clickText(device, false, "Settings", true);
                UiObject cb = device.findObject(new UiSelector().resourceId("android:id/checkbox"));
                try {
                    if (!cb.isChecked()) {
                        TestUtils.clickText(device, false, "Display advanced devices", false);
                    }
                } catch (UiObjectNotFoundException e) {
                    Assert.fail("Coudn't turn on SDCARD view");
                }
                TestUtils.clickText(device, false, "Settings", true);
                TestUtils.clickResource(device, false, "android:id/up", true);
                TestUtils.clickText(device, false, "SDCARD", true);
            } else {
                if (!TestUtils.clickText(device, false, "Show", false)) {
                    TestUtils.clickAt(device, device.getDisplayWidth() / 2, device.getDisplayHeight() / 2);
                }
                TestUtils.clickMenuButton(device, "List view", false, false);
                TestUtils.clickMenuButton(device, "Show roots", false, true);

                UiSelector android = new UiSelector().resourceIdMatches(".*:id/title").textMatches("(^Android SDK.*)|(^AOSP.*)|(^Internal.*)|(^Samsung.*)");
                UiObject androidButton = device.findObject(android);
                try {
                    androidButton.clickAndWaitForNewWindow();
                } catch (UiObjectNotFoundException e1) {
                    Assert.fail("Link to internal storage not found in drawer");
                }
            }
            UiScrollable appView = new UiScrollable(scrollableSelector);
            try {
                System.out.println("selectFile " + appView.getClassName());
            } catch (UiObjectNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            try {
                appView.scrollIntoView(new UiSelector().text("Vespucci"));
            } catch (UiObjectNotFoundException e) {
                // if there is no scrollable then this will fail
            }
            TestUtils.clickText(device, false, "Vespucci", true);
        }
        UiScrollable appView;
        if (directory != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                appView = new UiScrollable(scrollableSelector);
            } else {
                appView = new UiScrollable(new UiSelector().scrollable(true));
            }
            try {
                appView.scrollIntoView(new UiSelector().text(directory));
            } catch (UiObjectNotFoundException e) {
                // if there is no scrollable then this will fail
            }
            TestUtils.clickText(device, false, directory, true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            appView = new UiScrollable(scrollableSelector);
        } else {
            appView = new UiScrollable(new UiSelector().scrollable(true));
        }
        try {
            appView.scrollIntoView(new UiSelector().text(fileName));
        } catch (UiObjectNotFoundException e) {
            // if there is no scrollable then this will fail
        }
        TestUtils.clickText(device, false, fileName, true);
    }

    /**
     * Scroll to a specific text
     * 
     * @param text the text
     * @throws UiObjectNotFoundException if the UiScrollable couldn't be found
     */
    public static void scrollTo(@NonNull String text) {
        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));
        try {
            appView.scrollIntoView(new UiSelector().text(text));
        } catch (UiObjectNotFoundException e) {
            Assert.fail(text + " not found");
        }
    }

    /**
     * Zap all offsets for the background layer
     * 
     * @param map the current Map object
     */
    public static void resetOffsets(@NonNull Map map) {
        MapTilesLayer layer = map.getBackgroundLayer();
        if (layer != null) {
            TileLayerServer osmts = layer.getTileLayerConfiguration();
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
     * Setup a mock web server that serves tiles from a MBT source and set it to the current source
     * 
     * @param context an Android Context
     * @param prefs the current Preferences
     * @param mbtSource the MBT file name
     * @return a MockWebServer
     */
    public static MockWebServer setupTileServer(@NonNull Context context, @NonNull Preferences prefs, @NonNull String mbtSource) {
        MockWebServer tileServer = new MockWebServer();
        try {
            tileServer.setDispatcher(new TileDispatcher(context, "ersatz_background.mbt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String tileUrl = tileServer.url("/").toString() + "{zoom}/{x}/{y}";
        System.out.println(tileUrl);
        TileLayerDatabase db = new TileLayerDatabase(context);
        TileLayerServer.addOrUpdateCustomLayer(context, db.getWritableDatabase(), "VESPUCCITEST", null, -1, -1, "Vespucci Test", new Provider(), Category.other,
                0, 19, false, tileUrl);

        // allow downloading tiles here
        prefs.setBackGroundLayer("VESPUCCITEST");
        return tileServer;
    }

    /**
     * Try to hide any visible soft keyboard
     * 
     * FIXME doesn't seem to work
     * 
     * @param activity the current Activity
     */
    public static void hideSoftKeyboard(@NonNull Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = activity.getWindow().getCurrentFocus();
        if (view != null) {
            final CountDownLatch signal = new CountDownLatch(1);
            ResultReceiver receiver = new ResultReceiver(null) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    signal.countDown();
                }
            };
            imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0, receiver);
            try {
                signal.await(10, TimeUnit.SECONDS); // NOSONAR
            } catch (InterruptedException e) { // NOSONAR
                Log.w(DEBUG_TAG, "Keyboed hiding failed");
            }
        }
    }
}
