package de.blau.android.voice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

import de.blau.android.Application;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.GeoMath;

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
public class Commands extends SherlockActivity implements OnClickListener {
	private static final String DEBUG_TAG = Commands.class.getSimpleName();

	private ListView mList;
	private ImageButton speakButton;
	private Preset[] presets;
	
	public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);
		
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_customMain_Light);
		}
		super.onCreate(savedInstanceState);
		ActionBar actionbar = getSupportActionBar();
		if (actionbar == null) {
			Log.d(DEBUG_TAG, "No actionbar"); // fail?
			return;
		}
		actionbar.setDisplayShowHomeEnabled(true);
		actionbar.setTitle("Voice Input");
		actionbar.setDisplayShowTitleEnabled(true);
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.show();
		setContentView(R.layout.voice_commands);

		speakButton = (ImageButton) findViewById(R.id.btn_speak);
		speakButton.setOnClickListener(this);
		mList = (ListView) findViewById(R.id.list);
		presets = Main.getCurrentPresets();
	}

	public void startVoiceRecognitionActivity() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Map!");
		try {
			startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
		} catch (Exception ex) {
			Log.d(DEBUG_TAG,"Caught exception " + ex);
			Toast.makeText(this,"No voice recognition facility present", Toast.LENGTH_LONG).show();
		}
	}

	public void onClick(View v) {
		startVoiceRecognitionActivity();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
			// Fill the list view with the strings the recognizer thought it
			// could have heard
			ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			mList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, matches));
			Logic logic = Application.mainActivity.getLogic();
			// try to find a command it simply stops at the first string that is valid
			for (String v:matches) {
				String[] words = v.split("\\s+", 3);
				if (words.length > 1) {
					String loc = words[0].toLowerCase(); 
					if (getString(R.string.voice_left).equals(loc) || getString(R.string.voice_here).equals(loc) || getString(R.string.voice_right).equals(loc) ) {
						// 
						String first = words[1].toLowerCase();
						try {
							int number = Integer.parseInt(first);
							// worked if there is a further word(s) simply add it/them
							Toast.makeText(this,loc + " "+ number  + (words.length == 3?words[2]:""), Toast.LENGTH_LONG).show();
							Node node = createNode(loc);
							if (node != null) {
								TreeMap<String, String> tags = new TreeMap<String, String>(node.getTags());
								tags.put(Tags.KEY_ADDR_HOUSENUMBER, "" + number  + (words.length == 3?words[2]:""));
								tags.put("source:original_text", v);
								logic.setTags(Node.NAME, node.getOsmId(), tags);
							}
							return;
						} catch (Exception ex) {
							// ok wasn't a number
						}
						// basic idea is to take the input and search in the presets, this is a hackish implementation for demo only
						String[] majorKeys = {"amenity","shop","man_made","highway","natural"};
						for (String major:majorKeys) {
							HashMap<String, String> map = new HashMap<String, String>();
							map.put(major, first);
							PresetItem pi = Preset.findBestMatch(presets, map); 
							if (pi != null) {
								Toast.makeText(this,loc + " "+ pi.getName()  + (words.length == 3? " name: " + words[2]:""), Toast.LENGTH_LONG).show();
								Node node = createNode(loc);
								if (node != null) {
									TreeMap<String, String> tags = new TreeMap<String, String>(node.getTags());
									for (Entry<String, String> tag : pi.getTags().entrySet()) {
										tags.put(tag.getKey(), tag.getValue());
									}
									if (words.length == 3) {
										tags.put(Tags.KEY_NAME, words[2]);
									}
									tags.put("source:original_text", v);
									logic.setTags(Node.NAME, node.getOsmId(), tags);
								}
							}
							return;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Create a new node at the current GPS pos
	 * @return
	 */
	Node createNode(String loc) {
		LocationManager locationManager = (LocationManager)Application.mainActivity.getSystemService(android.content.Context.LOCATION_SERVICE);
		if (locationManager != null) {
			Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (location != null) {
				if (getString(R.string.voice_here).equals(loc)) {
					double lon = location.getLongitude();
					double lat = location.getLatitude();
					if (lon >= -180 && lon <= 180 && lat >= -GeoMath.MAX_LAT && lat <= GeoMath.MAX_LAT) {
						Logic logic = Application.mainActivity.getLogic();
						logic.setSelectedNode(null);
						Node node = logic.performAddNode(lon, lat);
						logic.setSelectedNode(null);
						return node;
					}
				}
			}
		}
		return null;
	}
}
