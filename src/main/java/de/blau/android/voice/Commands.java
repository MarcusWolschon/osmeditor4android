package de.blau.android.voice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Tags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.Address;
import de.blau.android.tasks.Note;
import de.blau.android.util.ElementSearch;
import de.blau.android.util.GeoMath;
import de.blau.android.util.OptimalStringAlignment;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Util;

/**
 * Support for simple voice commands, format
 * <location> <number>
 * for an address
 * <location> <object> [<name>]
 * for a POI of some kind-
 * <location> can be one of left, here and right
 * @author simon
 *
 */
public class Commands {
	private static final String DEBUG_TAG = Commands.class.getSimpleName();
	private Main main;
	
	private Map<String,NameAndTags> namesSearchIndex;
	
	public Commands(Main main) {
		this.main = main;
		namesSearchIndex = App.getNameSearchIndex(main);
	}
	
	public void processIntentResult(Intent data, Location location) {

		// Fill the list view with the strings the recognizer thought it
		// could have heard
		ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
		final Logic logic = App.getLogic();
		// try to find a command it simply stops at the first string that is valid
		for (String v:matches) {
			Toast.makeText(main,">"+v+"<", Toast.LENGTH_LONG).show();
			String[] words = v.split("\\s+", 3);
			if (words.length > 1) {
				String loc = words[0].toLowerCase(Locale.getDefault()); 
				if (match(R.string.voice_left,loc) || match(R.string.voice_here,loc) || match(R.string.voice_right,loc) || match(R.string.voice_note,loc) ) {
					if (match(R.string.voice_note,loc)) {
						Note n = createNote(words, location);
						Toast.makeText(main,"Note: " + n.getDescription(), Toast.LENGTH_LONG).show();
						return;
					}
					if (!match(R.string.voice_here,loc)) {
						Toast.makeText(main,"Sorry currently only the command \"" + main.getString(R.string.voice_here) + "\" is supported", Toast.LENGTH_LONG).show();
					} 
					// 
					String first = words[1].toLowerCase(Locale.getDefault());
					try {
						int number = Integer.parseInt(first);
						// worked if there is a further word(s) simply add it/them
						Toast.makeText(main,loc + " "+ number  + (words.length == 3?words[2]:""), Toast.LENGTH_LONG).show();
						Node node = createNode(loc,location);
						if (node != null) {
							TreeMap<String, String> tags = new TreeMap<String, String>(node.getTags());
							tags.put(Tags.KEY_ADDR_HOUSENUMBER, "" + number  + (words.length == 3?words[2]:""));
							tags.put("source:original_text", v);
							LinkedHashMap<String, ArrayList<String>> map = Address.predictAddressTags(main, Node.NAME, node.getOsmId(), 
										new ElementSearch(new int[]{node.getLon(),node.getLat()}, true), 
										Util.getArrayListMap(tags), Address.NO_HYSTERESIS);
							tags = new TreeMap<String, String>();
							for (String key:map.keySet()) {
								tags.put(key, map.get(key).get(0));
							}
							logic.setTags(node, tags);
						}
						return;
					} catch (NumberFormatException ex) {
						// ok wasn't a number
					} catch (OsmIllegalOperationException e) {
						Log.e(DEBUG_TAG,e.getMessage());
						Toast.makeText(main,e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
						e.printStackTrace();
					}

					List<PresetItem> presetItems = SearchIndexUtils.searchInPresets(main, first,ElementType.NODE,2,1);
					if (presetItems != null && presetItems.size()==1) {
						addNode(createNode(loc,location), words.length == 3? words[2]:null, presetItems.get(0), logic, v);
						return;
					}

					if (namesSearchIndex == null) {
						return;
					}
					
					// search in names
					String input = "";
					for (int i=1;i<words.length;i++) {
						input = input + words[i] + (i<words.length?" ":"");
					}
					NameAndTags nt = SearchIndexUtils.searchInNames(main, input, 2);
					if (nt != null) {
						HashMap<String, String> map = new HashMap<String, String>();
						map.putAll(nt.getTags());
						PresetItem pi = Preset.findBestMatch(App.getCurrentPresets(main), map);
						if (pi != null) {
							addNode(createNode(loc,location), nt.getName(), pi, logic, v);
							return;
						}
					}
				}
			} else if (words.length == 1) {
				if (match(R.string.voice_follow,words[0])) {
					main.setFollowGPS(true);
					return;
				} else {
					Toast.makeText(main,main.getResources().getString(R.string.toast_unknown_voice_command,words[0]), Toast.LENGTH_LONG).show();
				} 
			}
		}

	}

	private boolean addNode(Node node, String name, PresetItem pi, Logic logic, String original) {
		if (node != null) {
			Toast.makeText(main, pi.getName()  + (name != null? " name: " + name:""), Toast.LENGTH_LONG).show();
			if (node != null) {
				try {
					TreeMap<String, String> tags = new TreeMap<String, String>(node.getTags());
					for (Entry<String, StringWithDescription> tag : pi.getFixedTags().entrySet()) {
						tags.put(tag.getKey(), tag.getValue().getValue());
					}
					if (name != null) {
						tags.put(Tags.KEY_NAME, name);
					}
					tags.put("source:original_text", original);
					logic.setTags(node, tags);
					return true;
				} catch (OsmIllegalOperationException e) {
					Log.e(DEBUG_TAG,e.getMessage());
					Toast.makeText(main,e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
		return false;
	}

	/**
	 * Create a new node at the current or at a provided  GPS pos 
	 * @return
	 */
	private Node createNode(String loc, Location location) {
		if (location == null) {
			location = getLocation();
		}
		if (location != null) {
			if (main.getString(R.string.voice_here).equals(loc)) {
				double lon = location.getLongitude();
				double lat = location.getLatitude();
				if (lon >= -180 && lon <= 180 && lat >= -GeoMath.MAX_LAT && lat <= GeoMath.MAX_LAT) {
					final Logic logic = App.getLogic();
					logic.setSelectedNode(null);
					Node node = logic.performAddNode(main, lon, lat);
					logic.setSelectedNode(null);
					return node;
				}
			}
		}
		return null;
	}
	
	private Note createNote(String[] words, Location location) {
		if (location == null) {
			location = getLocation();
		}
		if (location != null) {		
			double lon = location.getLongitude();
			double lat = location.getLatitude();
			if (lon >= -180 && lon <= 180 && lat >= -GeoMath.MAX_LAT && lat <= GeoMath.MAX_LAT) {
				Note n = new Note((int)(lat*1E7D),(int)(lon*1E7D));
				StringBuilder input = new StringBuilder();
				for (int i=1;i<words.length;i++) {
					input.append(words[i]);
					input.append(" ");
				}
				n.addComment(input.toString().trim());
				n.open();
				n.setChanged();
				App.getTaskStorage().add(n);
				return n;
			}
		}
		return null;
	}
	
	private Location getLocation() {
		LocationManager locationManager = (LocationManager)main.getSystemService(android.content.Context.LOCATION_SERVICE);
		if (locationManager != null) {
			try {
				return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			} catch (SecurityException sex) {
				// can be safely ignored
				return null;
			}
		}
		return null;
	}

	private boolean match(int resId, String input) {
		final int maxDistance = 1;
		int distance = OptimalStringAlignment.editDistance(main.getString(resId), input, maxDistance);
		return distance >= 0 && distance <= maxDistance;
	}
}
