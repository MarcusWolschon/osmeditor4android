package de.blau.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.XMLReader;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import ch.poole.poparser.ParseException;
import ch.poole.poparser.Po;
import ch.poole.poparser.TokenMgrError;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.exception.OsmServerException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.resources.TileLayerSource;

public final class Util {

    private static final String DEBUG_TAG = "Util";

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
    public static List<OsmElement> sortWays(@NonNull List<OsmElement> list) {
        List<OsmElement> result = new ArrayList<>();
        List<OsmElement> unconnected = new ArrayList<>(list);

        OsmElement e = unconnected.get(0);
        unconnected.remove(0);
        if (!e.getName().equals(Way.NAME)) {
            return null; // not all are ways
        }
        result.add(e);
        while (true) {
            boolean found = false;
            for (OsmElement w : unconnected) {
                if (!Way.NAME.equals(w.getName())) {
                    return null; // not all are ways
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
     * Sort a list of RelationMemberDescription in the order they are connected
     * 
     * Note: there is likely a far better algorithm than this, ignores way direction.
     * 
     * @param list List of relation members
     * @return fully or partially sorted List of RelationMembers, if partially sorted the unsorted elements will come
     *         first
     * @param <T> Class that extents RelationMember
     */
    @NonNull
    public static <T extends RelationMember> List<T> sortRelationMembers(@NonNull List<T> list) {
        List<T> result = new ArrayList<>();
        List<T> unconnected = new ArrayList<>(list);
        int nextWay = 0;
        while (true) {
            nextWay = nextWay(nextWay, unconnected);
            if (nextWay >= unconnected.size()) {
                break;
            }
            T currentRmd = unconnected.get(nextWay);
            unconnected.remove(currentRmd);
            result.add(currentRmd);
            int start = result.size() - 1;

            for (int i = nextWay; i < unconnected.size();) {
                T rmd = unconnected.get(i);
                if (!rmd.downloaded() || !Way.NAME.equals(rmd.getType())) {
                    i++;
                    continue;
                }

                Way startWay = (Way) result.get(start).getElement();
                Way endWay = (Way) result.get(result.size() - 1).getElement();

                Way currentWay = (Way) rmd.getElement();

                // the following works for all situation including closed ways but will be a bit slow
                if (haveCommonNode(endWay, currentWay)) {
                    result.add(rmd);
                    unconnected.remove(rmd);
                } else if (haveCommonNode(startWay, currentWay)) {
                    result.add(start, rmd);
                    unconnected.remove(rmd);
                } else {
                    i++;
                }
            }
        }
        unconnected.addAll(result); // return with unsorted elements at top
        return unconnected;
    }

    /**
     * Test if two ways have a common Node
     * 
     * Note should be moved to the Way class
     * 
     * @param way1 first Way
     * @param way2 second Way
     * @return true if the have a common Node
     */
    private static boolean haveCommonNode(@Nullable Way way1, @Nullable Way way2) {
        if (way1 != null && way2 != null) {
            List<Node> way1Nodes = way1.getNodes();
            int size1 = way1Nodes.size();
            List<Node> way2Nodes = way2.getNodes();
            int size2 = way2Nodes.size();
            // optimization: check start and end first, this should make partially sorted list reasonably fast
            if (way2Nodes.contains(way1Nodes.get(0)) || way2Nodes.contains(way1Nodes.get(size1 - 1)) || way1Nodes.contains(way2Nodes.get(0))
                    || way1Nodes.contains(way2Nodes.get(size2 - 1))) {
                return true;
            }
            // nope have to iterate
            List<Node> slice = way2Nodes.subList(1, size2 - 1);
            for (int i = 1; i < size1 - 2; i++) {
                if (slice.contains(way1Nodes.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return the next Way index in a list of RelationMemberDescriptions
     * 
     * @param start starting index
     * @param unconnected List of T
     * @param <T> Class that extents RelationMember
     * @return the index of the next Way, or that value of start
     */
    private static <T extends RelationMember> int nextWay(int start, @NonNull List<T> unconnected) {
        // find first way
        int firstWay = start;
        for (; firstWay < unconnected.size(); firstWay++) {
            T rm = unconnected.get(firstWay);
            if (rm.downloaded() && Way.NAME.equals(rm.getType())) {
                break;
            }
        }
        return firstWay;
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
        if (s != null && s.length() >= 1) {
            return s.charAt(0);
        } else {
            return 0;
        }
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
            if (coords != null) {
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
        } else {
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
    }

    /**
     * Set the background tint list for a FloatingActionButton in a version independent way
     * 
     * @param fab the FloatingActionButton
     * @param tint a ColorStateList
     */
    public static void setBackgroundTintList(@NonNull FloatingActionButton fab, @NonNull ColorStateList tint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fab.setBackgroundTintList(tint);
        } else {
            ViewCompat.setBackgroundTintList(fab, tint);
        }
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
    public static void sharePosition(@NonNull Activity activity, @Nullable double[] lonLat, @Nullable Integer z) {
        if (lonLat != null) {
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
     * @param maxStringLength maximum length the string is allowed to have
     */
    public static void sanitizeString(@Nullable Context context, @NonNull Editable s, int maxStringLength) {
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
                Snack.toastTopWarning(context, context.getString(R.string.toast_string_too_long, len));
            }
        }
    }

    /**
     * Replacement for Long.compare prior to Android 19
     * 
     * @param x first value to compare
     * @param y second value to compare
     * @return -1 if x is numerically smaller than y, 0 if equal and +1 if x is numerically larger than y
     */
    @SuppressLint("NewApi")
    public static int longCompare(long x, long y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return Long.compare(x, y);
        }
        return Long.valueOf(x).compareTo(y); // NOSONAR
    }

    private static class UlTagHandler implements Html.TagHandler {
        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if ("ul".equals(tag) && !opening) {
                output.append("\n");
            }
            if ("li".equals(tag) && opening) {
                output.append("\n\t•");
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
        Logic logic = App.getLogic();
        if (logic != null) {
            de.blau.android.Map map = logic.getMap();
            if (map != null) {
                de.blau.android.layer.data.MapOverlay dataLayer = map.getDataLayer();
                if (dataLayer != null) {
                    dataLayer.clearIconCaches();
                    dataLayer.invalidate();
                }
            }
        }
        TileLayerSource.clearLogos();
    }

    /**
     * If aspects of the configuration have changed clear icon caches
     * 
     * Side effect updates stored Configuration
     * 
     * @param context Android Context
     * @param newConfig new Configuration
     */
    public static void clearCaches(@NonNull Context context, @NonNull Configuration newConfig) {
        Configuration oldConfig = App.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (oldConfig == null || oldConfig.densityDpi != newConfig.densityDpi || oldConfig.fontScale != newConfig.fontScale) {
                // if the density has changed the icons will have wrong dimension remove them
                clearIconCaches(context);
                App.setConfiguration(newConfig);
            }
        }
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
                Snack.toastTopWarning(activity,
                        activity.getString(R.string.toast_download_failed, ((OsmServerException) iox).getErrorCode(), iox.getMessage()));
            } else {
                Snack.toastTopWarning(activity, activity.getString(R.string.toast_server_connection_failed, iox.getMessage()));
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
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH || ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WEBVIEW);
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
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
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
     * estimate number of 'usable' processors
     *
     * @return number of processors
     */
    public static int usableProcessors() {
        int val = Runtime.getRuntime().availableProcessors();
        if (val > 4) {
            // Typically has big+little architecture. Try to avoid the 'little' cores
            // as they would need a smaller chunk to finish work in similar time.
            // E.g. on Samsung A50 it's faster to use only the 4 fast cores instead of using
            // all 8 cores and wait for 4 slower cores to finish their equal sized chunk.
            val /= 2;
        }
        return val;
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
        return text != null && !"".equals(text);
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
}
