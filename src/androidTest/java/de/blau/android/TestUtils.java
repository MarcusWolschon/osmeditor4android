package de.blau.android;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import android.content.Context;
import android.graphics.Rect;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.util.Log;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;

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
     */
    public static void grantPermissons() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            boolean notdone = true;
            while (notdone) {
                notdone = clickText(mDevice, true, "allow", true) || clickText(mDevice, true, "zulassen", false);
            }
        }
    }

    /**
     * Dismiss initial welcome dialog and download
     * 
     * @param ctx Android context
     */
    public static void dismissStartUpDialogs(@NonNull Context ctx) {
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        clickText(mDevice, true, ctx.getResources().getString(R.string.okay), false);
        clickText(mDevice, true, ctx.getResources().getString(R.string.location_load_dismiss), false);
    }

    /**
     * Select the recipient of an intent
     * 
     * Currently flaky
     */
    public static void selectIntentRecipient() {
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.waitForWindowUpdate(null, 1000);
        clickText(mDevice, false, "Vespucci", false);
        if (!clickText(mDevice, false, "Just once", false)) {
            clickText(mDevice, false, "Nur diesmal", false);
        }
    }

    /**
     * Click the overflow button in a menu bar
     * 
     * @return true if successful
     */
    public static boolean clickOverflowButton() {
        return clickMenuButton("More options", false, true);
    }

    /**
     * Click a menu bar button
     * 
     * @param description the description of the button
     * @param longClick if true perform a long click
     * @param waitForNewWindow if true wait for a new window
     * @return true if successful
     */
    public static boolean clickMenuButton(String description, boolean longClick, boolean waitForNewWindow) {
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = By.clickable(true).descStartsWith(description);
        UiSelector uiSelector = new UiSelector().clickable(true).descriptionStartsWith(description);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
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
     * @param resId resource id
     * @param waitForNewWindow if true wait for a new window after clicking
     * @return true if the button was found and clicked
     * @throws UiObjectNotFoundException
     */
    public static boolean clickButton(String resId, boolean waitForNewWindow) {
        try {
            UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
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
     */
    public static void pinchOut() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiSelector uiSelector = new UiSelector().resourceId("de.blau.android:id/map_view");
        try {
            device.findObject(uiSelector).pinchOut(75, 100);
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Two finger zoom in simulation
     */
    public static void pinchIn() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiSelector uiSelector = new UiSelector().resourceId("de.blau.android:id/map_view");
        try {
            device.findObject(uiSelector).pinchIn(75, 100);
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Single click at a screen location
     * 
     * @param x screen X coordinate
     * @param y screen y coordinate
     */
    public static void clickAt(float x, float y) {
        System.out.println("clicking at " + x + " " + y);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.click((int) x, (int) y);
    }

    /**
     * Single click at a WGS84 location
     * 
     * Will center the screen on the location first
     * 
     * @param map the current Map object
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     */
    public static void clickAtCoordinates(Map map, double lon, double lat) {
        clickAtCoordinates(map, lon, lat, true);
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
                TestUtils.clickAtCoordinates(map, lonE7, latE7);
            }
        }, Until.newWindow(), 5000);
    }

    /**
     * Single click at a WGS84 location
     * 
     * @param map the current Map object
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void clickAtCoordinates(Map map, double lon, double lat, boolean recenter) {
        clickAtCoordinates(map, (int) (lon * 1E7), (int) (lat * 1E7), recenter);
    }

    /**
     * Single click at a WGS84 location
     * 
     * Will center the screen on the location first
     * 
     * @param map the current Map object
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     */
    public static void clickAtCoordinates(Map map, int lonE7, int latE7) {
        clickAtCoordinates(map, lonE7, latE7, true);
    }

    /**
     * Single click at a WGS84 location
     * 
     * @param map the current Map object
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void clickAtCoordinates(Map map, int lonE7, int latE7, boolean recenter) {
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
        TestUtils.clickAt(Math.round(x) + outLocation[0], Math.round(y) + outLocation[1]);
    }

    /**
     * An attempt at getting reliable long clicks with swiping
     * 
     * @param mDevice the current UiDevice
     * @param o the UiObject to long click on
     * @throws UiObjectNotFoundException if o is not found
     */
    public static void longClick(UiDevice mDevice, UiObject o) throws UiObjectNotFoundException {
        Rect rect = o.getBounds();
        mDevice.swipe(rect.centerX(), rect.centerY(), rect.centerX(), rect.centerY(), 200);
        try {
            Thread.sleep(2000); // NOSONAR
        } catch (InterruptedException e1) {
        }
    }

    /**
     * Long click at a screen location
     * 
     * @param x screen X coordinate
     * @param y screen y coordinate
     */
    public static void longClickAt(float x, float y) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.swipe((int) x, (int) y, (int) x, (int) y, 200);
    }

    /**
     * Long click at a WGS84 location
     * 
     * @param map the current Map object
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void longClickAtCoordinates(Map map, double lon, double lat, boolean recenter) {
        longClickAtCoordinates(map, (int) (lon * 1E7), (int) (lat * 1E7), recenter);
    }

    /**
     * Long click at a WGS84 location
     * 
     * @param map the current Map object
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void longClickAtCoordinates(Map map, int lonE7, int latE7, boolean recenter) {
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
        TestUtils.longClickAt(x + outLocation[0], y + outLocation[1]);
    }

    /**
     * Double click at a screen location
     * 
     * @param x screen X coordinate
     * @param y screen y coordinate
     */
    public static void doubleClickAt(float x, float y) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.click((int) x, (int) y);
        try {
            Thread.sleep(100); // NOSONAR
        } catch (InterruptedException e) {
        }
        device.click((int) x, (int) y);
    }

    /**
     * Double click at a WGS84 location
     * 
     * @param map the current Map object
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void doubleClickAtCoordinates(Map map, double lon, double lat, boolean recenter) {
        doubleClickAtCoordinates(map, (int) (lon * 1E7), (int) (lat * 1E7), recenter);
    }

    /**
     * Double click at a WGS84 location
     * 
     * @param map the current Map object
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     * @param recenter center the screen on the location first if true
     */
    public static void doubleClickAtCoordinates(Map map, int lonE7, int latE7, boolean recenter) {
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
        TestUtils.doubleClickAt(x + outLocation[0], y + outLocation[1]);
    }

    /**
     * Execute a drag
     * 
     * @param startX start screen X coordinate
     * @param startY start screen Y coordinate
     * @param endX end screen X coordinate
     * @param endY end screen Y coordinate
     * @param steps number of 5ms steps
     */
    public static void drag(float startX, float startY, float endX, float endY, int steps) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.swipe((int) startX, (int) startY, (int) endX, (int) endY, steps);
    }

    /**
     * Execute a drag
     * 
     * @param map map the current Map object
     * @param startLon start WGS84 longitude
     * @param startLat start WGS84 latitude
     * @param endLon end lon WGS84 longitude
     * @param endLat end WGS84 latitude
     * @param recenter center the screen on the start location first if true
     * @param steps number of 5ms steps
     */
    public static void drag(Map map, double startLon, double startLat, double endLon, double endLat, boolean recenter, int steps) {
        drag(map, (int) (startLon * 1E7), (int) (startLat * 1E7), (int) (endLon * 1E7), (int) (endLat * 1E7), recenter, steps);
    }

    /**
     * Execute a drag
     * 
     * @param map map the current Map object
     * @param startLonE7 start WGS84*1E7 longitude
     * @param startLatE7 start WGS84*1E7 latitude
     * @param endLonE7 end lon WGS84*1E7 longitude
     * @param endLatE7 end WGS84*1E7 latitude
     * @param recenter center the screen on the start location first if true
     * @param steps number of 5ms steps
     */
    public static void drag(Map map, int startLonE7, int startLatE7, int endLonE7, int endLatE7, boolean recenter, int steps) {
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
        TestUtils.drag(startX + outLocation[0], startY + outLocation[1], endX + outLocation[0], endY + outLocation[1], steps);
    }

    /**
     * Unlock the screen if locked
     */
    public static void unlock() {
        if (App.getLogic().isLocked()) {
            UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            UiObject lock = device.findObject(new UiSelector().resourceId("de.blau.android:id/floatingLock"));
            try {
                lock.click();
            } catch (UiObjectNotFoundException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    /**
     * Zoom to a specific zoom level
     * 
     * @param main current Main object
     * @param level level to zoom to
     */
    public static void zoomToLevel(Main main, int level) {
        Map map = main.getMap();
        int count = 0;
        int currentLevel = map.getZoomLevel();
        int prevLevel = -1;
        int noChange = 0;
        while (level != currentLevel && count < 20) {
            Log.d(DEBUG_TAG, "Zoom level " + currentLevel);
            if (currentLevel < level) {
                if (level - currentLevel > 3) {
                    pinchOut();
                } else {
                    clickButton("de.blau.android:id/zoom_in", false);
                }
            } else {
                if (currentLevel - level > 3) {
                    pinchIn();
                } else {
                    clickButton("de.blau.android:id/zoom_out", false);
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
    public static boolean clickText(UiDevice device, boolean clickable, String text, boolean waitForNewWindow) {
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
    public static boolean longClickText(UiDevice device, String text) {
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
    public static boolean clickTextContains(UiDevice device, boolean clickable, String text, boolean waitForNewWindow) {
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
    public static boolean findText(UiDevice device, boolean clickable, String text) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = null;
        if (clickable) {
            bySelector = By.clickable(true).textStartsWith(text);
        } else {
            bySelector = By.textStartsWith(text);
        }
        UiObject2 ob = device.wait(Until.findObject(bySelector), 500);
        return ob != null;
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
    public static boolean clickResource(UiDevice device, boolean clickable, String resourceId, boolean waitForNewWindow) {
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
     * Click "Up" button in action modes
     * 
     * @param mDevice UiDevice object
     * @return true if the button was clicked
     */
    public static boolean clickUp(@NonNull UiDevice mDevice) {
        UiObject homeButton = mDevice.findObject(new UiSelector().clickable(true).descriptionStartsWith("Navigate up"));
        if (!homeButton.exists()) {
            homeButton = mDevice.findObject(new UiSelector().clickable(true).descriptionStartsWith("Nach oben"));
        }
        try {
            return homeButton.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
            return false; // can't actually be reached
        }
    }

    /**
     * Click "Home" button in Activity app bars
     * 
     * @param mDevice UiDevice object
     */
    public static void clickHome(UiDevice mDevice) {
        clickResource(mDevice, true, "de.blau.android:id/action_mode_close_button", true);
    }

    /**
     * Buffered read an InputStream into a byte array
     * 
     * @param is the InputStream to read
     * @return a byte array
     * @throws IOException
     */
    public static byte[] readInputStream(InputStream is) throws IOException {
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
                if (providerCriteria == Criteria.ACCURACY_FINE) {
                    locationManager.addTestProvider(LocationManager.GPS_PROVIDER, // name
                            false, // requiresNetwork
                            false, // requiresSatellite
                            false, // requiresCell
                            false, // hasMonetaryCost
                            true, // supportsAltitude
                            true, // supportsSpeed
                            true, // supportsBearing
                            0, // powerRequirement
                            5 // accuracy
                    );
                } else if (providerCriteria == Criteria.ACCURACY_COARSE) {
                    locationManager.addTestProvider(LocationManager.NETWORK_PROVIDER, // name
                            false, // requiresNetwork
                            false, // requiresSatellite
                            false, // requiresCell
                            false, // hasMonetaryCost
                            true, // supportsAltitude
                            true, // supportsSpeed
                            true, // supportsBearing
                            0, // powerRequirement
                            500 // accuracy
                    );
                } else {
                    return;
                }
                Criteria criteria = new Criteria();
                criteria.setAccuracy(providerCriteria);
                provider = locationManager.getBestProvider(criteria, true);
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
                        ;
                    }
                    loc.setTime(System.currentTimeMillis());
                    loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
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
     * @param destination the destiantion sub-directory
     * @throws IOException
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
}
