package de.blau.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import android.content.Context;
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

/**
 * 
 * @author simon
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

    public static void dismissStartUpDialogs(Context ctx) {
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        clickText(mDevice, true, ctx.getResources().getString(R.string.okay), false);
        clickText(mDevice, true, ctx.getResources().getString(R.string.location_load_dismiss), false);
    }

    public static void selectIntentRecipient(Context ctx) {
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.waitForWindowUpdate(null, 1000);
        clickText(mDevice, true, "Vespucci", true);
        if (!clickText(mDevice, true, "Just once", false)) {
            clickText(mDevice, true, "Nur diesmal", false);
        }
    }

    public static void clickButton(String resId, boolean waitForNewWindow) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiSelector uiSelector = new UiSelector().clickable(true).resourceId(resId);
        UiObject button = device.findObject(uiSelector);
        try {
            if (waitForNewWindow) {
                button.clickAndWaitForNewWindow();
            } else {
                button.click();
            }
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void pinchOut() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiSelector uiSelector = new UiSelector().resourceId("de.blau.android:id/map_view");
        try {
            device.findObject(uiSelector).pinchOut(75, 100);
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void pinchIn() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiSelector uiSelector = new UiSelector().resourceId("de.blau.android:id/map_view");
        try {
            device.findObject(uiSelector).pinchIn(75, 100);
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public static void clickAt(float x, float y) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.click((int)x, (int)y);
    }

    public static void zoomToLevel(Main main, int level) {
        Map map = main.getMap();
        while (level != map.getZoomLevel()) {
            int currentLevel = map.getZoomLevel();
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
        }
    }

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
    
    public static boolean findText(UiDevice device, boolean clickable, String text) {
        Log.w(DEBUG_TAG, "Searching for object with " + text);
        // Note: contrary to "text", "textStartsWith" is case insensitive
        BySelector bySelector = null;
        
        // NOTE order of the selector terms is significant
        if (clickable) {
            bySelector = By.clickable(true).textStartsWith(text);
            
        } else {
            bySelector = By.textStartsWith(text);
            
        }
        UiObject2 ob = device.wait(Until.findObject(bySelector), 500);
        return ob != null;
    }


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

    public static void clickUp(UiDevice mDevice) {
        UiObject homeButton = mDevice.findObject(new UiSelector().clickable(true).descriptionStartsWith("Navigate up"));
        if (!homeButton.exists()) {
            homeButton = mDevice.findObject(new UiSelector().clickable(true).descriptionStartsWith("Nach oben"));
        }
        try {
            homeButton.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }

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
     * @param context   Android context
     * @param lat       Latitude
     * @param lon       Longitude
     * @param interval  interval between values
     * @param handler   handler to call when we are finished
     */
    public static void injectLocation(@NonNull final Context context, final double lat, final double lon, final int interval, @Nullable final SignalHandler handler) {
        List<TrackPoint> track = new ArrayList<>();
        TrackPoint tp = new TrackPoint((byte) 0, lat, lon, 0, System.currentTimeMillis());
        track.add(tp);
        injectLocation(context, track, interval, handler);
    }

    /**
     * Inject a Location to the app in testing
     * 
     * @param context   Android context
     * @param track     List of TrackPoints
     * @param interval  interval between values
     * @param handler   handler to call when we are finished
     */
    public static void injectLocation(@NonNull final Context context, @NonNull final List<TrackPoint> track, final int interval, @Nullable final SignalHandler handler) {

        new AsyncTask<Void, Void, Void>() {
            String          provider        = "none";
            LocationManager locationManager = null;

            @Override
            protected void onPreExecute() {
                System.out.println("Injecting " + track.size() + " Locations");
                locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
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
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
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
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                if (handler != null) {
                    handler.onSuccess();
                }
            }
        }.execute();
    }

}
