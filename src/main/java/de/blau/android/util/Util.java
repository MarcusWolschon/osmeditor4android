package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.xml.sax.XMLReader;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import ch.poole.poparser.ParseException;
import ch.poole.poparser.Po;
import ch.poole.poparser.TokenMgrError;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.contract.Schemes;
import de.blau.android.exception.OsmServerException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.resources.TileLayerSource;

public final class Util {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Util.class.getSimpleName().length());
    private static final String DEBUG_TAG = Util.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Private constructor
     */
    private Util() {
        // don't allow instantiating of this class
    }

    /**
     * Wrap an Object in an ArrayList
     * 
     * @param <T> the Object type
     * @param o the input String
     * @return an ArrayList containing only s
     */
    public static <T> List<T> wrapInList(@NonNull T o) {
        List<T> v = new ArrayList<>(1);
        v.add(o);
        return v;
    }

    /**
     * Convert a &lt;String, String&gt; Map to a Map of &lt;String, ArrayList&lt;String&gt;&gt;
     * 
     * @param map the input Map
     * @return the converted Map
     */
    @NonNull
    public static LinkedHashMap<String, List<String>> getListMap(@NonNull Map<String, String> map) {
        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        for (Entry<String, String> e : map.entrySet()) {
            result.put(e.getKey(), wrapInList(e.getValue()));
        }
        return result;
    }

    /**
     * Sort a list of ways in the order they are connected
     * 
     * Note: there is likely a far better algorithm than this, assumes that ways could be reversed. Further this will in
     * general not work for closed ways and assumes that the ways can actually be arranged as a single sorted sequence.
     * 
     * @param list List of ways
     * @return null if not connected or not all ways connected or the sorted list of ways
     */
    @Nullable
    public static List<OsmElement> sortWays(@NonNull List<OsmElement> list) {
        List<OsmElement> result = new ArrayList<>();
        List<OsmElement> unconnected = new ArrayList<>(list);

        OsmElement e = unconnected.get(0);
        unconnected.remove(0);
        if (!Way.NAME.equals(e.getName())) {
            return null; // not all are ways
        }
        result.add(e);
        while (true) {
            boolean found = false;
            for (OsmElement w : unconnected) {
                if (!(w instanceof Way) || ((Way)w).getNodes().isEmpty()) {
                    return null; // not all are proper ways
                }
                // this is a bit complicated because we don't want to reverse ways just yet
                Node firstNode1 = ((Way) result.get(0)).getFirstNode();
                Node firstNode2 = ((Way) result.get(0)).getLastNode();
                Node lastNode1 = ((Way) result.get(result.size() - 1)).getFirstNode();
                Node lastNode2 = ((Way) result.get(result.size() - 1)).getLastNode();

                Node wFirstNode = ((Way) w).getFirstNode();
                Node wLastNode = ((Way) w).getLastNode();
                if (wFirstNode.equals(firstNode1) || wFirstNode.equals(firstNode2) || wLastNode.equals(firstNode1) || wLastNode.equals(firstNode2)) {
                    result.add(0, w);
                    unconnected.remove(w);
                    found = true;
                    break;
                } else if (wFirstNode.equals(lastNode1) || wFirstNode.equals(lastNode2) || wLastNode.equals(lastNode1) || wLastNode.equals(lastNode2)) {
                    result.add(w);
                    unconnected.remove(w);
                    found = true;
                    break;
                }
            }
            if (!found && !unconnected.isEmpty()) {
                return null;
            } else if (unconnected.isEmpty()) {
                return result;
            }
        }
    }

    /**
     * Safely return a short cut (aka one character) from the string resources
     * 
     * @param ctx Android context
     * @param res the id of a string resource
     * @return character or 0 if no short cut can be found
     */
    public static char getShortCut(@NonNull Context ctx, int res) {
        String s = ctx.getString(res);
        if (s.length() >= 1) {
            return s.charAt(0);
        }
        return 0;
    }

    /**
     * Get the location of the center of the given osm-element
     * 
     * @param delegator the StorageDelegator instance
     * @param osmElementType the typoe of OSM element as a String (NODE, WAY, RELATION)
     * @param osmId the id of the object
     * @return {lat, lon} or null
     */
    public static IntCoordinates getCenter(@NonNull final StorageDelegator delegator, @NonNull final String osmElementType, long osmId) {
        OsmElement osmElement = delegator.getOsmElement(osmElementType, osmId);
        if (osmElement instanceof Node) {
            Node n = (Node) osmElement;
            return new IntCoordinates(n.getLon(), n.getLat());
        }
        if (osmElement instanceof Way) {
            double[] coords = Geometry.centroidLonLat((Way) osmElement);
            if (coords.length == 2) {
                return new IntCoordinates((int) (coords[0] * 1E7), (int) (coords[1] * 1E7));
            }
        }
        if (osmElement instanceof Relation) { // the center of the bounding box is naturally just a rough estimate
            BoundingBox bbox = osmElement.getBounds();
            if (bbox != null) {
                ViewBox vbox = new ViewBox(bbox);
                return new IntCoordinates(vbox.getLeft() + (vbox.getRight() - vbox.getLeft()) / 2, ((int) (vbox.getCenterLat() * 1E7)));
            }
        }
        return null;
    }

    private static final int[] bearingStrings = { R.string.bearing_ne, R.string.bearing_e, R.string.bearing_se, R.string.bearing_s, R.string.bearing_sw,
            R.string.bearing_w, R.string.bearing_nw, R.string.bearing_n };

    /**
     * Get a String indicating the bearing from (sLon/sLat) to (eLon/eLat)
     * 
     * @param context an Android Context
     * @param sLon start lon
     * @param sLat start lat
     * @param eLon end lon
     * @param eLat end lat
     * @return a String indicating the bearing
     */
    @NonNull
    public static String getBearingString(@NonNull Context context, double sLon, double sLat, double eLon, double eLat) {
        return context.getString(bearingStrings[getBearingIndex(sLon, sLat, eLon, eLat)]);
    }

    private static final char[] bearingArrows = { '↗', '→', '↘', '↓', '↙', '←', '↖', '↑' };

    /**
     * Get a char indicating the bearing from (sLon/sLat) to (eLon/eLat)
     * 
     * @param sLon start lon
     * @param sLat start lat
     * @param eLon end lon
     * @param eLat end lat
     * @return a char indicating the bearing
     */
    @NonNull
    public static char getBearingArrow(double sLon, double sLat, double eLon, double eLat) {
        return bearingArrows[getBearingIndex(sLon, sLat, eLon, eLat)];
    }

    /**
     * Calculate the bearing and return an index in to an array holding display values
     * 
     * @param sLon start lon
     * @param sLat start lat
     * @param eLon end lon
     * @param eLat end lat
     * @return the index
     */
    private static int getBearingIndex(double sLon, double sLat, double eLon, double eLat) {
        long bearing = GeoMath.bearing(sLon, sLat, eLon, eLat);

        int index = (int) (bearing - 22.5);
        if (index < 0) {
            index += 360;
        }
        index = index / 45;
        return index;
    }

    /**
     * Convert a collection to a semicolon separated string
     * 
     * @param collection the Collection to convert
     * @return string containing the individual list values separated by ; or the empty string if list is null or empty
     */
    @NonNull
    public static String toOsmList(@Nullable Collection<String> collection) {
        StringBuilder osmList = new StringBuilder();
        if (collection != null) {
            for (String s : collection) {
                if (osmList.length() > 0) {
                    osmList.append(Tags.OSM_VALUE_SEPARATOR);
                }
                osmList.append(s);
            }
        }
        return osmList.toString();
    }

    /**
     * Scroll to the supplied view
     * 
     * @param sv the ScrollView or NestedScrollView to scroll
     * @param row the row to display, if null scroll to top or bottom of sv
     * @param up if true scroll to top if row is null, otherwise scroll to bottom
     * @param force if true always try to scroll even if row is already on screen
     */
    public static void scrollToRow(@NonNull final View sv, @Nullable final View row, final boolean up, boolean force) {
        Rect scrollBounds = new Rect();
        sv.getHitRect(scrollBounds);
        Log.d(DEBUG_TAG, "scrollToRow bounds " + scrollBounds);
        if (row != null && row.getLocalVisibleRect(scrollBounds) && !force) {
            return; // already on screen
        }
        if (row == null) {
            Log.d(DEBUG_TAG, "scrollToRow scrolling to top or bottom");
            sv.post(() -> {
                if (sv instanceof ScrollView) {
                    ((ScrollView) sv).fullScroll(up ? View.FOCUS_UP : View.FOCUS_DOWN);
                } else if (sv instanceof NestedScrollView) {
                    ((NestedScrollView) sv).fullScroll(up ? View.FOCUS_UP : View.FOCUS_DOWN);
                } else {
                    Log.e(DEBUG_TAG, "scrollToRow unexpected view " + sv);
                }
            });
            return;
        }
        Log.d(DEBUG_TAG, "scrollToRow scrolling to row");
        sv.post(() -> {
            final int target = up ? row.getTop() : row.getBottom();
            if (sv instanceof ScrollView) {
                ((ScrollView) sv).smoothScrollTo(0, target);
            } else if (sv instanceof NestedScrollView) {
                ((NestedScrollView) sv).smoothScrollTo(0, target);
            } else {
                Log.e(DEBUG_TAG, "scrollToRow unexpected view " + sv);
            }
        });
    }

    /**
     * Set the background tint list for a FloatingActionButton in a version independent way
     * 
     * @param fab the FloatingActionButton
     * @param tint a ColorStateList
     */
    public static void setBackgroundTintList(@NonNull FloatingActionButton fab, @NonNull ColorStateList tint) {
        fab.setBackgroundTintList(tint);
    }

    /**
     * Convert first letter of v to upper case using English
     * 
     * @param v the input String
     * @return a String with its first letter as a capital (or null if v was null)
     */
    @Nullable
    public static String capitalize(@Nullable String v) {
        if (v != null && v.length() > 0) {
            char[] a = v.toCharArray();
            a[0] = Character.toUpperCase(a[0]);
            return String.valueOf(a);
        }
        return v;
    }

    /**
     * Share the supplied position with other apps
     * 
     * @param activity this activity
     * @param lonLat coordinates to share
     * @param z the zoom level or null
     */
    public static void sharePosition(@NonNull Activity activity, @NonNull double[] lonLat, @Nullable Integer z) {
        if (lonLat.length == 2) {
            Uri geo = Uri.parse("geo:" + String.format(Locale.US, "%.7f", lonLat[1]) + "," + String.format(Locale.US, "%.7f", lonLat[0])
                    + (z != null ? "?z=" + z.toString() : ""));
            Log.d(DEBUG_TAG, "sharing " + geo);
            Intent geoIntent = new Intent(Intent.ACTION_VIEW, geo);
            activity.startActivity(geoIntent);
        }
    }

    /**
     * Check that a double is not zero
     * 
     * @param a the double to test
     * @return true if not zero
     */
    public static boolean notZero(double a) {
        return a < -Double.MIN_VALUE || a > Double.MIN_VALUE;
    }

    /**
     * Check if two floating point values are the same within a tolereance range
     * 
     * @param d1 value 1
     * @param d2 value 2
     * @param tolerance the tolerance
     * @return true if the difference between the values is smaller than the tolerance
     */
    public static boolean equals(double d1, double d2, double tolerance) {
        return Math.abs(d1 - d2) < tolerance;
    }

    /**
     * Remove formating from s and truncate it if necessary, typically used in a TextWatcher
     * 
     * @param context if non-null and the string has been truncated display a toast with this context
     * @param s Editable that needs to be sanitized
     * @param maxStringLength maximum length the string is allowed to have, has to be >= 0
     */
    public static void sanitizeString(@Nullable Context context, @NonNull Editable s, int maxStringLength) {
        if (maxStringLength < 0) {
            Log.e(DEBUG_TAG, "sanitizeString maxStringLength " + maxStringLength);
            maxStringLength = 0;
        }
        // remove formating from pastes etc
        CharacterStyle[] toBeRemovedSpans = s.getSpans(0, s.length(), MetricAffectingSpan.class);
        for (CharacterStyle toBeRemovedSpan : toBeRemovedSpans) {
            s.removeSpan(toBeRemovedSpan);
        }

        // truncate if longer than max supported string length
        int len = s.length();
        if (len > maxStringLength) {
            s.delete(maxStringLength, len);
            if (context != null) {
                ScreenMessage.toastTopWarning(context, context.getString(R.string.toast_string_too_long, len));
            }
        }
    }

    private static final class UlTagHandler implements Html.TagHandler {
        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if ("ul".equals(tag) && !opening) {
                output.append("\n");
            }
            if ("li".equals(tag) && opening) {
                output.append("\n\t<b>&bull;</b>");
            }
        }
    }

    /**
     * Compatibility wrapper for Html.fromHtml
     * 
     * @param html string with HTML markup to convert
     * @return a Spanned formated as the markup required
     */
    @NonNull
    public static Spanned fromHtml(@NonNull String html) {
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY, null, new UlTagHandler());
    }

    /**
     * Wrap string in a HTML font element with a themed color
     * 
     * @param context an Android COntext
     * @param attrColorRes the attr color resource
     * @param input the input string
     * @return the wrapped string
     */
    public static String withHtmlColor(@NonNull Context context, int attrColorRes, @NonNull String input) {
        int labelColor = ThemeUtils.getStyleAttribColorValue(context, attrColorRes, R.color.material_red);
        return String.format("<font color=\"#%s\">%s</font>", String.format("%X", labelColor).substring(2), input);
    }

    /**
     * Convert a Drawable to a Bitmap See
     * https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap/9390776
     * 
     * @param drawable input Drawable
     * @return a Bitmap
     */
    public static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        // We ask for the bounds if they have been set as they would be most
        // correct, then we check we are > 0
        final int width = !drawable.getBounds().isEmpty() ? drawable.getBounds().width() : drawable.getIntrinsicWidth();

        final int height = !drawable.getBounds().isEmpty() ? drawable.getBounds().height() : drawable.getIntrinsicHeight();

        // Now we check we are > 0
        final Bitmap bitmap = Bitmap.createBitmap(width <= 0 ? 1 : width, height <= 0 ? 1 : height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Get the size of a bundle in bytes
     * 
     * @param bundle the Bundle
     * @return the size in bytes
     */
    public static int getBundleSize(@NonNull Bundle bundle) {
        Parcel parcel = Parcel.obtain();
        parcel.writeBundle(bundle);
        int size = parcel.dataSize();
        parcel.recycle();
        return size;
    }

    /**
     * Clear all the places where we've cached icons
     * 
     * @param context Android Context
     */
    public static void clearIconCaches(@NonNull Context context) {
        Preset[] presets = App.getCurrentPresets(context);
        for (Preset p : presets) {
            if (p != null) {
                p.clearIcons();
            }
        }
        clearDataLayerIconCaches(true);
        TileLayerSource.clearLogos();
    }

    /**
     * Clear the icon caches in the data layer if it exists
     * 
     * @param invalidate invalidate the layer after clearing the caches
     */
    public static void clearDataLayerIconCaches(boolean invalidate) {
        Logic logic = App.getLogic();
        if (logic != null) {
            de.blau.android.Map map = logic.getMap();
            if (map != null) {
                de.blau.android.layer.data.MapOverlay<OsmElement> dataLayer = map.getDataLayer();
                if (dataLayer != null) {
                    dataLayer.clearCaches();
                    if (invalidate) {
                        dataLayer.invalidate();
                    }
                }
            }
        }
    }

    /**
     * If aspects of the configuration have changed clear icon caches
     * 
     * Side effect updates stored Configuration
     * 
     * @param context Android Context
     * @param oldConfig old Configuration
     * @param newConfig new Configuration
     */
    public static void clearCaches(@NonNull Context context, Configuration oldConfig, @NonNull Configuration newConfig) {
        if (oldConfig == null || oldConfig.densityDpi != newConfig.densityDpi || oldConfig.fontScale != newConfig.fontScale) {
            // if the density has changed the icons will have wrong dimension remove them
            clearIconCaches(context);
        }
    }

    /**
     * Check if the theme has changed in the system and we are following those changes
     * 
     * @param oldConfig the old configuration
     * @param newConfig the new configuration
     * 
     * @return true if the theme has changed
     */
    public static boolean themeChanged(@NonNull Preferences prefs, Configuration oldConfig, @NonNull Configuration newConfig) {
        return prefs.followingSystemTheme() && (oldConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) != (newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK);
    }

    /**
     * Determine if we have less than 32MB of heap
     * 
     * @return true if the heap is small
     */
    public static boolean smallHeap() {
        return Runtime.getRuntime().maxMemory() <= 32L * 1024L * 1024L; // less than 32MB
    }

    /**
     * Display a toast if we got an IOException downloading
     * 
     * @param activity the calling Activity
     * @param iox the IOException
     */
    public static void toastDowloadError(@NonNull final FragmentActivity activity, @NonNull final IOException iox) {
        activity.runOnUiThread(() -> {
            if (iox instanceof OsmServerException) {
                ScreenMessage.toastTopWarning(activity,
                        activity.getString(R.string.toast_download_failed, ((OsmServerException) iox).getHttpErrorCode(), iox.getMessage()));
            } else {
                ScreenMessage.toastTopWarning(activity, activity.getString(R.string.toast_server_connection_failed, iox.getMessage()));
            }
        });
    }

    /**
     * Check if the device supports WebView
     * 
     * Unluckily it is totally unclear if this helps with disabled packages.
     * 
     * @param ctx an Android Context
     * @return true if the system has a WebView implementation
     */
    public static boolean supportsWebView(@NonNull Context ctx) {
        return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WEBVIEW);
    }

    /**
     * Check if a specific package is installed
     * 
     * @param packageName the name of the package
     * @param packageManager a PackageManager instance
     * @return true if the package is installed
     */

    public static boolean isPackageInstalled(@NonNull String packageName, @NonNull PackageManager packageManager) {
        try {
            getPackageInfo(packageName, packageManager);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Get the PackageInfo for a specific package
     * 
     * @param packageName the name of the package
     * @param packageManager a PackageManager instance
     * @return a PackageInfo object if the package is installed
     * @throws NameNotFoundException
     */
    @SuppressWarnings("deprecation")
    @NonNull
    public static PackageInfo getPackageInfo(@NonNull String packageName, @NonNull PackageManager packageManager) throws NameNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0));
        }
        return packageManager.getPackageInfo(packageName, 0);
    }

    /**
     * Create a Po class from an InputStream
     * 
     * @param poFileStream the InputStream
     * @return an Po object or null
     */
    @Nullable
    public static Po parsePoFile(@Nullable InputStream poFileStream) {
        if (poFileStream != null) {
            try {
                return new Po(poFileStream);
            } catch (ParseException | TokenMgrError ignored) {
                Log.e(DEBUG_TAG, "Parsing translation file for " + Locale.getDefault() + " or " + Locale.getDefault().getLanguage() + " failed");
            }
        }
        return null;
    }

    /**
     * Append further query arguments to an url
     * 
     * @param url the original url
     * @param query the query
     * @return the extended url
     */
    @NonNull
    public static String appendQuery(@NonNull String url, @NonNull String query) {
        url += (url.contains("?") ? (url.endsWith("?") ? "" : "&") : "?") + query;
        return url;
    }

    /**
     * Naive counting of how many times a char is present in a string
     * 
     * @param string the String
     * @param c the char
     * @return the count
     */
    public static int countChar(@NonNull String string, char c) {
        int count = 0;
        for (int i = 0; i < string.length(); i++) {
            if (c == string.charAt(i)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get a translated string with element type and id
     * 
     * @param ctx an Android COntext
     * @param type the element type
     * @param id the id
     * @return a transalted String
     */
    public static String elementTypeId(@NonNull Context ctx, @NonNull String type, long id) {
        switch (type) {
        case Node.NAME:
            return ctx.getString(R.string.node_id, id);
        case Way.NAME:
            return ctx.getString(R.string.way_id, id);
        case Relation.NAME:
            return ctx.getString(R.string.relation_id, id);
        default:
            throw new IllegalArgumentException("Unknoen element " + type);
        }
    }

    /**
     * Check if a String is neither null nor the empty String
     * 
     * @param text the input String
     * @return true if text is neither null nor the empty String
     */
    public static boolean notEmpty(@Nullable final String text) {
        return text != null && text.length() > 0;
    }

    /**
     * Check if a String is either null or the empty String
     * 
     * @param text the input String
     * @return true if text is either null or the empty String
     */
    public static boolean isEmpty(@Nullable final String text) {
        return text == null || text.length() == 0;
    }

    /**
     * Check if a Collection is neither null nor empty
     * 
     * @param collection the input Collection
     * @return true if text is neither null nor empty
     */
    public static <T extends Object> boolean notEmpty(@Nullable final Collection<T> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * Check if a Collection is empty
     * 
     * @param collection the input Collection
     * @return true if text is either null or empty
     */
    public static <T extends Object> boolean isEmpty(@Nullable final Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Backwards compatible way of getting current Locale
     * 
     * @param r the Resources
     * @return the current primary Locale
     */
    @SuppressWarnings("deprecation")
    @NonNull
    public static Locale getPrimaryLocale(@NonNull Resources r) {
        Configuration c = r.getConfiguration();
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? c.getLocales().get(0) : c.locale;
    }

    /**
     * Test if an Object implements a list of interfaces, and if not throw an exception
     * 
     * Note: as the object will typically be cast to the interface, it would latest fail there, however this improves
     * logging of the issue a lot
     * 
     * @param toTest the Object being tested
     * @param interfaces the interfaces it should implement
     */
    public static void implementsInterface(@Nullable Object toTest, @NonNull Class<?>... interfaces) {
        for (Class<?> c : interfaces) {
            if (!(c.isInterface() && c.isInstance(toTest))) {
                throw new ClassCastException(
                        toTest != null ? toTest.getClass().getCanonicalName() + " must implement " + c.getCanonicalName() : "class is null");
            }
        }
    }

    /**
     * Get the parent fragment that implements the specified interfaces
     * 
     * @param f the Fragment
     * @param interfaces the interfaces
     * @return the parent fragment
     */
    @NonNull
    public static Fragment getParentFragmentWithInterface(@NonNull Fragment f, @NonNull Class<?>... interfaces) {
        Fragment parent = f.getParentFragment();
        for (Class<?> c : interfaces) {
            while (parent != null && c.isInterface() && !c.isInstance(parent)) {
                parent = parent.getParentFragment();
            }
        }
        implementsInterface(parent, interfaces);
        return parent; // NOSONAR null will cause a ClassCastException to be thrown
    }

    /**
     * Check if a permission is granted
     * 
     * @param ctx an Android Context
     * @param permission the permission to check
     * @return true if the permission has been granted
     */
    public static boolean permissionGranted(@NonNull Context ctx, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Determine if we are displaying in an RTL script
     * 
     * @param context an Android Context
     * @return true if RTL
     */
    public static boolean isRtlScript(@NonNull Context context) {
        Configuration config = context.getResources().getConfiguration();
        return config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Set the TextView CompoundDrawable according to if we are using RTL rendering or not
     * 
     * @param rtl if true RTL
     * @param textView the TextView
     * @param drawable the Drawable to set
     */
    public static void setCompoundDrawableWithIntrinsicBounds(boolean rtl, @NonNull final TextView textView, @Nullable final Drawable drawable) {
        textView.setCompoundDrawablesWithIntrinsicBounds(!rtl ? drawable : null, null, rtl ? drawable : null, null);
    }

    /**
     * Wrapper for getSerializableExtra
     * 
     * @param <T> the Serializable type
     * @param intent the Intent
     * @param key the key
     * @param clazz the class we want to retrieve
     * @return an instance of clazz or null
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Nullable
    public static <T extends Serializable> T getSerializableExtra(@NonNull Intent intent, @NonNull String key, @NonNull Class<T> clazz) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? intent.getSerializableExtra(key, clazz) : (T) intent.getSerializableExtra(key);
    }

    /**
     * Wrapper for getParcelable
     * 
     * @param <T> the Parceable type
     * @param bundle the Bundle
     * @param key the key
     * @param clazz the class we want to retrieve
     * @return an instance of clazz or null
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Nullable
    public static <T extends Parcelable> T getParcelable(@NonNull Bundle bundle, @NonNull String key, @NonNull Class<T> clazz) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? bundle.getParcelable(key, clazz) : (T) bundle.getParcelable(key);
    }

    /**
     * Wrapper for getSerializeable
     * 
     * @param <T> the Serializable type
     * @param bundle the Bundle
     * @param key the key
     * @param clazz the class we want to retrieve
     * @return an instance of clazz or null
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Nullable
    public static <T extends Serializable> T getSerializeable(@NonNull Bundle bundle, @NonNull String key, @NonNull Class<T> clazz) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? bundle.getSerializable(key, clazz) : (T) bundle.getSerializable(key);
    }

    /**
     * Get an ArrayList of T
     * 
     * @param <T> the Serializable type
     * @param bundle the Bundle
     * @param key the key
     * @param clazz the class we want to retrieve
     * @return an instance of ArrayList<T> or null
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Nullable
    public static <T extends Serializable> ArrayList<T> getSerializeableArrayList(@NonNull Bundle bundle, @NonNull String key, @NonNull Class<T> clazz) { // NOSONAR
        return (ArrayList<T>) (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? bundle.getSerializable(key, ArrayList.class)
                : bundle.getSerializable(key));
    }

    /**
     * Try to cleanly cancel any queued for execution jobs
     * 
     * @param executor the ThreadPoolExecutor
     */
    public static void shutDownThreadPool(@NonNull ThreadPoolExecutor executor) {
        executor.shutdownNow();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) { // NOSONAR
            // nothing we can really do here
        }
    }

    /**
     * Filter a list of OsmELement ids for download
     * 
     * @param delegator the current StorageDelegator instance
     * @param type the element type
     * @param elements the list
     * @return a filtered list that only contains id that are not already loaded
     */
    public static List<Long> filterForDownload(@NonNull StorageDelegator delegator, @NonNull String type, @NonNull List<Long> elements) {
        List<Long> result = new ArrayList<>();
        for (long id : elements) {
            if (delegator.getOsmElement(type, id) == null) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * Check if we are in multi window mode
     * 
     * @param the calling Activity
     * @return true if we are in multi window mode, false otherwise
     */
    public static boolean isInMultiWindowModeCompat(@NonNull FragmentActivity activity) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode();
    }

    /**
     * Run action on the UI thread
     * 
     * @param ctx an Android Context
     * @param action the runnable
     */
    public static void runOnUiThread(@NonNull Context ctx, @NonNull final Runnable action) {
        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(action);
        }
    }

    /**
     * Add a tag incrementing the numeric suffix of key if there is a collision
     * 
     * @param key the key
     * @param value the value
     * @param tags the exiting tags
     */
    public static void addTagWithNumericSuffix(@NonNull String key, @NonNull String value, @NonNull Map<String, String> tags) {
        String existing = tags.get(key);
        if (existing == null) {
            tags.put(key, value);
            return;
        }
        int index = 0;
        for (String tag : tags.keySet()) {
            if (!tag.startsWith(key)) {
                continue;
            }
            String[] parts = tag.split("\\:");
            if (parts.length == 2) {
                try {
                    int temp = Integer.parseInt(parts[1]);
                    if (temp > index) {
                        index = temp;
                    }
                } catch (NumberFormatException nfex) {
                    // ignore
                }
            }
        }
        tags.put(key + Tags.NS_SEP + Integer.toString(index + 1), value);
    }

    /**
     * Check if desktop mode is enabled, currently only for DeX
     * 
     * @param context an Andrpid context
     * @return true if desktop mode is enabled.
     */
    public static boolean isDesktopModeEnabled(@NonNull Context context){
        Configuration config = context.getResources().getConfiguration();
        try {
            Class<? extends Configuration> configClass = config.getClass();
            return configClass.getField("SEM_DESKTOP_MODE_ENABLED").getInt(configClass)
                    == configClass.getField("semDesktopModeEnabled").getInt(config);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException  e) {
           Log.e(DEBUG_TAG, "isDesktopModeEnabled " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Check for an url
     * 
     * @param url the url
     * @return true if the check passes
     */
    public static boolean isUrl(@Nullable String url) {
        return url != null && (url.startsWith(Schemes.HTTP + "://") || url.startsWith(Schemes.HTTPS + "://"));
    }
}
