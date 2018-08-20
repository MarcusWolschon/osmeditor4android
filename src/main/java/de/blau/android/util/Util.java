package de.blau.android.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
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
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ScrollView;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMemberDescription;
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
    public static ArrayList<String> getArrayList(String s) {
        ArrayList<String> v = new ArrayList<>();
        v.add(s);
        return v;
    }

    public static LinkedHashMap<String, ArrayList<String>> getArrayListMap(Map<String, String> map) {
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
    public static List<OsmElement> sortWays(List<OsmElement> list) {
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
     */
    public static List<RelationMemberDescription> sortRelationMembers(List<RelationMemberDescription> list) {
        List<RelationMemberDescription> result = new ArrayList<>();
        List<RelationMemberDescription> unconnected = new ArrayList<>(list);
        int nextWay = 0;
        while (true) {
            nextWay = nextWay(nextWay, unconnected);
            if (nextWay >= unconnected.size()) {
                break;
            }
            RelationMemberDescription currentRmd = unconnected.get(nextWay);
            unconnected.remove(currentRmd);
            result.add(currentRmd);
            int start = result.size() - 1;

            for (int i = nextWay; i < unconnected.size();) {
                RelationMemberDescription rmd = unconnected.get(i);
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

    private static boolean haveCommonNode(Way way1, Way way2) {
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

    private static int nextWay(int start, List<RelationMemberDescription> unconnected) {
        // find first way
        int firstWay = start;
        for (; firstWay < unconnected.size(); firstWay++) {
            RelationMemberDescription rmd = unconnected.get(firstWay);
            if (rmd.downloaded() && Way.NAME.equals(rmd.getType())) {
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
    public static char getShortCut(Context ctx, int res) {
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
     * @param delegator
     * @param osmElementType
     * @param osmId
     * @return {lat, lon} or null
     */
    public static int[] getCenter(final StorageDelegator delegator, final String osmElementType, long osmId) {
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
    public static String listToOsmList(List<String> list) {
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

    @SuppressLint("NewApi")
    public static boolean isLandscape(Activity activity) {
        // reliable determine if we are in landscape mode
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(size);
        } else {
            // noinspection deprecation
            size.x = display.getWidth();
            // noinspection deprecation
            size.y = display.getHeight();
        }

        return isLarge(activity) && size.x > size.y;
    }

    public static boolean isLarge(Activity activity) {
        int screenSize = activity.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE);
    }

    /**
     * Scroll to the supplied view
     * 
     * @param sv the ScrollView or NestedScrollView to scroll
     * @param row the row to display, if null scroll to top or bottom of sv
     * @param up if true scroll to top if row is null, otherwise scroll to bottom
     * @param force if true always try to scroll even if row is already on screen
     */
    public static void scrollToRow(final View sv, final View row, final boolean up, boolean force) {
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

    public static void setBackgroundTintList(FloatingActionButton fab, ColorStateList tint) {
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
     * @return a String with its first letter as a capital
     */
    public static String capitalize(String v) {
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
    public static void setAlpha(FloatingActionButton fab, float fabalpha) {
        ViewCompat.setAlpha(fab, fabalpha);
    }

    /**
     * Share the supplied position with other apps
     * 
     * @param activity this activity
     * @param lonLat coordinates to sahre
     */
    public static void sharePosition(Activity activity, double[] lonLat) {
        if (lonLat != null) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            Uri geo = Uri.parse("geo:" + lonLat[1] + "," + lonLat[0]);
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

    /**
     * Set the summary of a list preference from its current value and add an OnPreferenceChangeListener
     * 
     * @param prefFragment PreferenceFragmentCompat this is being called from
     * @param key key of the ListPreference
     */
    public static void setListPreferenceSummary(@NonNull PreferenceFragmentCompat prefFragment, @NonNull String key) {
        ListPreference listPref = (ListPreference) prefFragment.getPreferenceScreen().findPreference(key);
        if (listPref != null) {
            CharSequence currentEntry = listPref.getEntry();
            if (currentEntry != null) {
                listPref.setSummary(currentEntry);
            }
            OnPreferenceChangeListener p = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    try {
                        int i = ((ListPreference) preference).findIndexOfValue((String) newValue);
                        CharSequence currentEntry = ((ListPreference) preference).getEntries()[i];
                        if (currentEntry != null) {
                            preference.setSummary(currentEntry);
                        }
                    } catch (Exception ex) {
                        Log.d(DEBUG_TAG, "onPreferenceChange " + ex);
                    }
                    return true;
                }
            };
            listPref.setOnPreferenceChangeListener(p);
        }
    }

    /**
     * Backwards compatible version of Html.fromHtml
     * 
     * @param html string with HTML markup to convert
     * @return a Spanned formated as the markup required
     */
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }

    /**
     * Convert a Drawable to a Bitmap See
     * https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap/9390776
     * 
     * @param drawable input Drawable
     * @return a Bitmap
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
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
    public static int getBundleSize(Bundle bundle) {
        Parcel parcel = Parcel.obtain();
        parcel.writeBundle(bundle);
        int size = parcel.dataSize();
        parcel.recycle();
        return size;
    }

    /**
     * Get the size of the smaller side of the screen
     * 
     * @param activity the calling Activity
     * @return the smaller side in px
     */
    public static int getScreenSmallDimemsion(Activity activity) {
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(size);
        if (size.x < size.y) {
            return size.x;
        }
        return size.y;
    }

    /**
     * Clear all the places where we've cached icons
     * 
     * @param context Android Context
     */
    public static void clearIconCaches(Context context) {
        Preset[] presets = App.getCurrentPresets(context);
        for (Preset p : presets) {
            if (p != null) {
                p.clearIcons();
            }
        }
        de.blau.android.Map map = App.getLogic().getMap();
        if (map != null) {
            de.blau.android.layer.data.MapOverlay dataLayer = map.getDataLayer();
            if (dataLayer != null) {
                dataLayer.clearIconCaches();
                dataLayer.invalidate();
            }
        }
        TileLayerServer.clearLogos();
    }
}
