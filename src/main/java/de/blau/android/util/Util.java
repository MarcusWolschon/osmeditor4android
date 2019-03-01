package de.blau.android.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.XMLReader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.resources.TileLayerServer;

public final class Util {

    private static final String DEBUG_TAG = "Util";

    /**
     * Private constructor
     */
    private Util() {
        // don't allow instantiating of this class
    }

    /**
     * Wrap a string in an ArrayList
     * 
     * @param s the input String
     * @return an ArrayList containing only s
     */
    public static ArrayList<String> getArrayList(@NonNull String s) {
        ArrayList<String> v = new ArrayList<>();
        v.add(s);
        return v;
    }

    /**
     * Convert a <String, String> Map to a Map of <String, ArrayList<String>>
     * 
     * @param map the input Map
     * @return the converted Map
     */
    @NonNull
    public static LinkedHashMap<String, ArrayList<String>> getArrayListMap(@NonNull Map<String, String> map) {
        LinkedHashMap<String, ArrayList<String>> result = new LinkedHashMap<>();
        for (Entry<String, String> e : map.entrySet()) {
            result.put(e.getKey(), getArrayList(e.getValue()));
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
    private static boolean haveCommonNode(@NonNull Way way1, @NonNull Way way2) {
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
    public static int[] getCenter(@NonNull final StorageDelegator delegator, @NonNull final String osmElementType, long osmId) {
        OsmElement osmElement = delegator.getOsmElement(osmElementType, osmId);
        if (osmElement instanceof Node) {
            Node n = (Node) osmElement;
            return new int[] { n.getLat(), n.getLon() };
        }
        if (osmElement instanceof Way) {
            double[] coords = Logic.centroidLonLat((Way) osmElement);
            if (coords != null) {
                return new int[] { (int) (coords[1] * 1E7), (int) (coords[0] * 1E7) };
            }
        }
        if (osmElement instanceof Relation) { // the center of the bounding box is naturally just a rough estimate
            BoundingBox bbox = osmElement.getBounds();
            if (bbox != null) {
                ViewBox vbox = new ViewBox(bbox);
                return new int[] { (int) (vbox.getCenterLat() * 1E7), vbox.getLeft() + (vbox.getRight() - vbox.getLeft()) / 2 };
            }
        }
        return null;
    }

    /**
     * Convert a list to a semicolon separated string
     * 
     * @param list the List to convert
     * @return string containing the individual list values separated by ; or the empty string if list is null or empty
     */
    @NonNull
    public static String listToOsmList(@Nullable List<String> list) {
        StringBuilder osmList = new StringBuilder("");
        if (list != null) {
            for (String s : list) {
                if (osmList.length() > 0) {
                    osmList.append(";");
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
            sv.post(new Runnable() {
                @Override
                public void run() {
                    if (sv instanceof ScrollView) {
                        ((ScrollView) sv).fullScroll(up ? ScrollView.FOCUS_UP : ScrollView.FOCUS_DOWN);
                    } else if (sv instanceof NestedScrollView) {
                        ((NestedScrollView) sv).fullScroll(up ? ScrollView.FOCUS_UP : ScrollView.FOCUS_DOWN);
                    } else {
                        Log.e(DEBUG_TAG, "scrollToRow unexpected view " + sv);
                    }
                }
            });
        } else {
            Log.d(DEBUG_TAG, "scrollToRow scrolling to row");
            sv.post(new Runnable() {
                @SuppressLint("NewApi")
                @Override
                public void run() {
                    final int target = up ? row.getTop() : row.getBottom();
                    if (sv instanceof ScrollView) {
                        ((ScrollView) sv).smoothScrollTo(0, target);
                    } else if (sv instanceof NestedScrollView) {
                        ((NestedScrollView) sv).smoothScrollTo(0, target);
                    } else {
                        Log.e(DEBUG_TAG, "scrollToRow unexpected view " + sv);
                    }
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
     * Set the alpha value for a FAB, just a workaround of support lib brokenness
     * 
     * @param fab the floating action button
     * @param fabalpha the alpha value
     */
    public static void setAlpha(@NonNull FloatingActionButton fab, float fabalpha) {
        ViewCompat.setAlpha(fab, fabalpha);
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
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
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
     * Remove formating from s and truncate it if necessary, typically used in a TextWatcher
     * 
     * @param activity if non-null and the string has been truncated display a toast with this activity
     * @param s Editable that needs to be sanitized
     * @param maxStringLength maximum length the string is allowed to have
     */
    public static void sanitizeString(@Nullable Activity activity, @NonNull Editable s, int maxStringLength) {
        // remove formating from pastes etc
        CharacterStyle[] toBeRemovedSpans = s.getSpans(0, s.length(), MetricAffectingSpan.class);
        for (CharacterStyle toBeRemovedSpan : toBeRemovedSpans) {
            s.removeSpan(toBeRemovedSpan);
        }

        // truncate if longer than max supported string length
        int len = s.length();
        if (len > maxStringLength) {
            s.delete(maxStringLength, len);
            if (activity != null) {
                Snack.toastTopWarning(activity, activity.getString(R.string.toast_string_too_long, len));
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
        return Long.valueOf(x).compareTo(y);
    }

    private static class UlTagHandler implements Html.TagHandler {
        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if (tag.equals("ul") && !opening) {
                output.append("\n");
            }
            if (tag.equals("li") && opening) {
                output.append("\n\t•");
            }
        }
    }

    /**
     * Backwards compatible version of Html.fromHtml
     * 
     * @param html string with HTML markup to convert
     * @return a Spanned formated as the markup required
     */
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(@NonNull String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, null, new UlTagHandler());
        } else {
            return Html.fromHtml(html, null, new UlTagHandler());
        }
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
        TileLayerServer.clearLogos();
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
            if (oldConfig == null || oldConfig.densityDpi != newConfig.densityDpi) {
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
}
