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
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ScrollView;
import de.blau.android.Logic;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;

public class Util {
	
	private static final String DEBUG_TAG = "Util";

	static public ArrayList<String> getArrayList(String s) {
		ArrayList<String> v = new ArrayList<String>();
		v.add(s);
		return v;
	}

	static public LinkedHashMap<String,ArrayList<String>> getArrayListMap(Map<String,String>map) {
		LinkedHashMap<String,ArrayList<String>> result = new LinkedHashMap<String,ArrayList<String>>();
		for (Entry<String, String> e:map.entrySet()) {
			result.put(e.getKey(), getArrayList(e.getValue()));
		}
		return result;
	}
	
	/**
	 * Sort a list of ways in the order they are connected, there is likely a far better algorithm than this
	 * Note: assumes that ways could be reversed
	 * @param list
	 * @return null if not connected or not all ways
	 */
	static public List<OsmElement> sortWays(List<OsmElement>list) {
		List<OsmElement> result = new ArrayList<OsmElement>();
		List<OsmElement> unconnected = new ArrayList<OsmElement>(list);


		OsmElement e = unconnected.get(0);
		unconnected.remove(0);
		if (!e.getName().equals(Way.NAME)) {
			return null; // not all are ways
		}
		result.add(e);
		while (true) {
			boolean found = false;
			for (OsmElement w:unconnected) {
				if (!w.getName().equals(Way.NAME)) {
					return null; // not all are ways
				}
				// this is a bit complicated because we don't want to reverse ways just yet
				Node firstNode1 = ((Way) result.get(0)).getFirstNode();
				Node firstNode2 = ((Way) result.get(0)).getLastNode();
				Node lastNode1 = ((Way) result.get(result.size()-1)).getFirstNode();
				Node lastNode2 = ((Way) result.get(result.size()-1)).getLastNode();

				Node wFirstNode = ((Way)w).getFirstNode();
				Node wLastNode = ((Way)w).getLastNode();
				if (wFirstNode.equals(firstNode1) || wFirstNode.equals(firstNode2) || wLastNode.equals(firstNode1)  || wLastNode.equals(firstNode2)) {
					result.add(0,w);
					unconnected.remove(w);
					found = true;
					break;
				} else if (wFirstNode.equals(lastNode1) || wFirstNode.equals(lastNode2) || wLastNode.equals(lastNode1)  || wLastNode.equals(lastNode2)) {
					result.add(w);
					unconnected.remove(w);
					found = true;
					break;
				}
			}
			if (!found && unconnected.size() > 0) {
				return null;
			} else if (unconnected.size() == 0) {
				return result;
			}
		}
	}

	/**
	 * Safely return a short cut (aka one character) from the string resources
	 * @param ctx
	 * @param id
	 * @return character or 0 if no short cut can be found
	 */
	static public char getShortCut(Context ctx,int id) {
		String s = ctx.getString(id);
		if (s != null && s.length() >= 1) {
			return s.charAt(0);
		} else {
			return 0;
		}
	}
	
	/**
     * Get the location of the center of the given osm-element
     * @param delegator
     * @param osmElementType
     * @param osmId
     * @return {lat, lon} or null
     */
	public static int[] getCenter(final StorageDelegator delegator, final String osmElementType, long osmId) {
		OsmElement osmElement = delegator.getOsmElement(osmElementType, osmId);
		if (osmElement instanceof Node) {
			Node n = (Node) osmElement;
			return new int[] {n.getLat(), n.getLon()};
		}
		if (osmElement instanceof Way) {
			double[] coords = Logic.centroidLonLat((Way)osmElement);
			if (coords != null) {
				return new int[] {(int) (coords[1]*1E7), (int) (coords[0]*1E7)};
			}
		}
		if (osmElement instanceof Relation) { // the center of the bounding box is naturally just a rough estimate
			BoundingBox bbox = osmElement.getBounds();
			if (bbox != null) {
				return new int[] {(int)(bbox.getCenterLat()*1E7), bbox.getLeft()+(bbox.getRight()-bbox.getLeft())/2};
			}
		}
		return null;
	}
	
	/**
	 * Convert a list to a semicolon separated string
	 * @param list
	 * @return string containing the individual list values separated by ; or the empty string if list is null or empty
	 */
	public static String listToOsmList(List<String> list) {
		String osmList = "";
		if (list != null) {
			for (String i:list) {
				if (!"".equals(osmList)) {
					osmList = osmList + ";";
				}
				osmList = osmList + i;
			}
		}
		return osmList;
	}

    @SuppressLint("NewApi")
	public static boolean isLandscape(Activity activity) {
		// reliable determine if we are in landscape mode
		Display display = activity.getWindowManager().getDefaultDisplay();
		Point size = new Point();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			display.getSize(size);
		} else {
			//noinspection deprecation
			size.x = display.getWidth();
			//noinspection deprecation
			size.y = display.getHeight();
		}

		return isLarge(activity) && size.x > size.y;
    }
    
    @SuppressLint("NewApi")
    public static boolean isLarge(Activity activity) {
    	int screenSize = activity.getResources().getConfiguration().screenLayout &
		        Configuration.SCREENLAYOUT_SIZE_MASK;
    	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
    		return screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE;
    	}
    	return (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE);
    }
    
	/**
	 * Scroll to the supplied view 
	 * 
	 * @param sv 	the ScrollView or NestedScrollView to scroll
	 * @param row 	the row to display, if null scroll to top or bottom of sv
	 * @param up	if true scroll to top if row is null, otherwise scroll to bottom
	 * @param force	if true always try to scroll even if row is already on screen
	 */
	public static void scrollToRow(final View sv, final View row,final boolean up, boolean force) {	
		Rect scrollBounds = new Rect();
		sv.getHitRect(scrollBounds);
		Log.d(DEBUG_TAG, "scrollToRow bounds " + scrollBounds);
		if (row != null && row.getLocalVisibleRect(scrollBounds)&& !force) {
			return; // already on screen
		} 
		if (row==null) {
			Log.d(DEBUG_TAG, "scrollToRow scrolling to top or bottom");
			sv.post(new Runnable() {
				@Override
				public void run() {
					if (sv != null && sv instanceof ScrollView) { 
						((ScrollView)sv).fullScroll(up ? ScrollView.FOCUS_UP : ScrollView.FOCUS_DOWN);
					} else if (sv != null && sv instanceof NestedScrollView) { 
						((NestedScrollView)sv).fullScroll(up ? ScrollView.FOCUS_UP : ScrollView.FOCUS_DOWN);
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
					final int target = up ? row.getTop(): row.getBottom();
					if (sv != null && sv instanceof ScrollView) { 
						((ScrollView)sv).smoothScrollTo(0, target);
					} else if (sv != null && sv instanceof NestedScrollView) {
						((NestedScrollView)sv).smoothScrollTo(0, target);
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
	 * Group keys so that i18n follow the base key (which should always come first in map)
	 * @param <V>
	 * @param map
	 */
	public static <V> void groupI18nKeys(Map<String,V> map) {
		LinkedHashMap<String,V> temp = new  LinkedHashMap<String,V>();
		ArrayList<String> keys = new ArrayList<String>(map.keySet());
		while (keys.size()>0) {
			String key = keys.get(0);
			keys.remove(0);
			if (Tags.I18N_NAME_KEYS.contains(key)) {
				temp.put(key, map.get(key));
				int i = 0;
				while (keys.size()>0 && i < keys.size()) {
					String i18nKey = keys.get(i);
					if (i18nKey.startsWith(key+":")) {
						temp.put(i18nKey, map.get(i18nKey));
						keys.remove(i);
					} else {
						i++;
					}
				}
			} else {
				temp.put(key, map.get(key));
			}
		}
		map.clear();
		map.putAll(temp);
	}

	/**
	 * Convert first letter of v to upper case using English
	 * @param v
	 * @return
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
	 * Set the alpha value for a FAB, just a workaround of support lib  brokenness
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
			Uri geo = Uri.parse("geo:"+lonLat[1]+","+lonLat[0]);
			Log.d(DEBUG_TAG,"sharing " + geo);
			Intent geoIntent = new Intent(Intent.ACTION_VIEW, geo);
			activity.startActivity(geoIntent);
		}
	}

	/**
	 * Check that a double is not zero
	 * @param a the double to test
	 * @return true if not zero
	 */
	public static boolean notZero(double a) {
		return a < -Double.MIN_VALUE || a > Double.MIN_VALUE;
	}
}
