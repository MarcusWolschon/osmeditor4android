package de.blau.android.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;

public class Util {
	
	static int progressBarShown = 0;
	
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
	public static int[] getCenter(final StorageDelegator delegator,
			final String osmElementType, long osmId) {
		OsmElement osmElement = delegator.getOsmElement(osmElementType, osmId);
		if (osmElement instanceof Node) {
			Node n = (Node) osmElement;
			return new int[] {n.getLat(), n.getLon()};
		}
		if (osmElement instanceof Way) {
			double[] coords = Logic.centroidLonLat((Way)osmElement);
			return new int[] {(int) (coords[1]*1E7), (int) (coords[0]*1E7)};
		}
		return null;
	}
	
	/**
	 * Wrapper with a counter so that we keep the progress bar on for as long as necessary
	 * @param mainActivity
	 * @param show
	 */
	public synchronized static void setSupportProgressBarIndeterminateVisibility(Main mainActivity, boolean show) {
		if (show) {
			if (progressBarShown <= 0) {
				mainActivity.setSupportProgressBarIndeterminateVisibility(true);
			}
			progressBarShown++;	
		} else {
			progressBarShown--;
			if (progressBarShown <= 0) {
				mainActivity.setSupportProgressBarIndeterminateVisibility(false);
			}
		}
	}

	/**
	 * Reset the progressbar counter to zero
	 */
	public synchronized static void resetProgressBarShown() {
		progressBarShown = 0;
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
    /***
     * From http://blog.android-develop.com/2014/10/android-l-api-21-javalangillegalargumen.html
     * 
     * Android L (lollipop, API 21) introduced a new problem when trying to invoke implicit intent,
     * "java.lang.IllegalArgumentException: Service Intent must be explicit"
     *
     * If you are using an implicit intent, and know only 1 target would answer this intent,
     * This method will help you turn the implicit intent into the explicit form.
     *
     * Inspired from SO answer: http://stackoverflow.com/a/26318757/1446466
     * @param context
     * @param implicitIntent - The original implicit intent
     * @return Explicit Intent created from the implicit original intent
     */
    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }
}
