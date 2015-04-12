package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;
import de.blau.android.Application;
import de.blau.android.util.jsonreader.JsonReader;
import de.blau.android.views.util.OpenStreetMapTileServer;

/**
 * Tags that we want to remove before saving to server. List is in discarded.json from the iD repository
 * @author simon
 *
 */
public class DiscardedTags {

	private HashSet<String> redundantTags = new HashSet<String>();
	
	SortedMap<String, String> newTags = new TreeMap<String, String>();

	/**
	 * Implicit assumption that the list will be short and that it is OK to read in synchronously
	 */
	DiscardedTags() {	
		Resources r = Application.mainActivity.getResources();

		Log.d("DiscardedTags","Parsing configuration file");

		AssetManager assetManager = Application.mainActivity.getAssets();

		try {
			InputStream is = assetManager.open("discarded.json");
			JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
			try {

				try {
					reader.beginArray();
					while (reader.hasNext()) {
						redundantTags.add(reader.nextString());
					}
					reader.endArray();
					Log.d("DiscardedTags","Found " + redundantTags.size() + " tags.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
			finally {
				reader.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove the redundant tags from element.
	 * Notes:
	 *  - element already has the modified flag set if not, something went wrong and we skip 
	 *  - this does not create a checkpoint and assumes that we will never want to undo this
	 * @param element
	 */
	void remove(OsmElement element) {
		if (element.isUnchanged()) {
			Log.e("DicardedTags","Presented with unmodified element");
			return;
		}
		boolean modified = false;
		for (String key:element.getTags().keySet()) {
			Log.d("DicardedTags","Checking " + key);
			if (!redundantTags.contains(key)) {
				newTags.put(key, element.getTags().get(key));
			} else {
				Log.d("DicardedTags"," delete");
				modified = true;
			}
		}
		if (modified) {
			element.setTags(newTags);
		}
	}
}

