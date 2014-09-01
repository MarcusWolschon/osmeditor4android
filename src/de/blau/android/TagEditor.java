package de.blau.android;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import de.blau.android.Address.Side;
import de.blau.android.exception.OsmException;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.PlaceTagValueAutocompletionAdapter;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetDialog;
import de.blau.android.presets.StreetTagValueAutocompletionAdapter;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;

/**
 * An Activity to edit OSM-Tags. Sends the edited Tags as Result to its caller-Activity (normally {@link Main}).
 * 
 * @author mb
 */
public class TagEditor extends SherlockActivity implements OnDismissListener, OnItemSelectedListener {
	public static final String TAGEDIT_DATA = "dataClass";
	public static final String TAGEDIT_LAST_ADDRESS_TAGS = "applyLastTags";
	
	/** The layout containing the edit rows */
	private LinearLayout rowLayout = null;
	
	/** The layout containing the presets */
	private LinearLayout presetsLayout = null;
	
	/** The layout containing the parent relations */
	private LinearLayout parentRelationsLayout = null;
	
	/** The layout containing the members of a relation */
	private LinearLayout relationMembersLayout = null;
	
	/**
	 * The tag we use for Android-logging.
	 */
	private static final String DEBUG_TAG = TagEditor.class.getName();
	
	private long osmId;
	
	private String type;
	
	/**
	 * The OSM element for reference.
	 * DO NOT ATTEMPT TO MODIFY IT.
	 */
	private OsmElement element;
	
	private TagEditorData loadData;
	
	private boolean applyLastAddressTags = false;
	
	/**
	 * Handles "enter" key presses.
	 */
	private final OnKeyListener myKeyListener = new MyKeyListener();
	
	/** Set to true once values are loaded. used to suppress adding of empty rows while loading. */
	private boolean loaded;
	
	/**
	 * True while the activity is between onResume and onPause.
	 * Used to suppress autocomplete dropdowns while the activity is not running (showing them can lead to crashes).
	 * Needs to be static to be accessible in TagEditRow.
	 */
	private static boolean running = false;
	
	/** the Preset selection dialog used by this editor */
	private PresetDialog presetDialog;
	
	/**
	 * The tags present when this editor was created (for undoing changes)
	 */
	private Map<String, String> originalTags;
	
	/**
	 * the same for relations
	 */
	private HashMap<Long,String> originalParents;
	private ArrayList<RelationMemberDescription> originalMembers;
	
	private PresetItem autocompletePresetItem = null;
	Preset[] presets = null;
	
	private static final String LAST_TAGS_FILE = "lasttags.dat";
	
	private static final String ADDRESS_TAGS_FILE = "addresstags.dat";
	private static final int MAX_SAVED_ADDRESSES = 100;
 
	private SavingHelper<LinkedHashMap<String,String>> savingHelper
				= new SavingHelper<LinkedHashMap<String,String>>();
	
	private SavingHelper<LinkedList<Address>> savingHelperAddress
				= new SavingHelper<LinkedList<Address>>();
	
	
	private static Names names = null;
	
	private Preferences prefs = null;
	
	private StreetTagValueAutocompletionAdapter streetNameAutocompleteAdapter = null;
	
	private PlaceTagValueAutocompletionAdapter placeNameAutocompleteAdapter = null;
	
	private LinkedList<Address> lastAddresses = null;
	
	/**
	 * Interface for handling the key:value pairs in the TagEditor.
	 * @author Andrew Gregory
	 */
	private interface KeyValueHandler {
		abstract void handleKeyValue(final EditText keyEdit, final EditText valueEdit);
	}
	
	/**
	 * Perform some processing for each key:value pair in the TagEditor.
	 * @param handler The handler that will be called for each key:value pair.
	 */
	private void processKeyValues(final KeyValueHandler handler) {
		final int size = rowLayout.getChildCount();
		for (int i = 0; i < size; ++i) {
			View view = rowLayout.getChildAt(i);
			TagEditRow row = (TagEditRow)view;
			handler.handleKeyValue(row.keyEdit, row.valueEdit);
		}
	}
	
	/**
	 * Ensures that at least one empty row exists (creating one if needed)
	 * @return the first empty row found (or the one created), or null if loading was not finished (loaded == false)
	 */
	private TagEditRow ensureEmptyRow() {
		TagEditRow ret = null;
		if (loaded) {
			int i = rowLayout.getChildCount();
			while (--i >= 0) {
				TagEditRow row = (TagEditRow)rowLayout.getChildAt(i);
				boolean isEmpty = row.isEmpty();
				if (ret == null) ret = isEmpty ? row : insertNewEdit("", "", -1);
				else if (isEmpty) row.deleteRow();
			}
			if (ret == null) ret = insertNewEdit("", "", -1);
		}
		return ret;
		
	}
	
	/**
	 * Given a tag edit row, calculate its position.
	 * @param row The tag edit row to find.
	 * @return The position counting from 0 of the given row, or -1 if it couldn't be found.
	 */
	private int rowIndex(TagEditRow row) {
		for (int i = rowLayout.getChildCount() - 1; i >= 0; --i) {
			if (rowLayout.getChildAt(i) == row) return i;
		}
		return -1;
	}
	
	/**
	 * Focus on the value field of a tag with key "key" 
	 * @param key
	 * @return
	 */
	private boolean focusOnValue(String key) {
		boolean found = false;
		for (int i = rowLayout.getChildCount() - 1; i >= 0; --i) {
			TagEditRow ter = (TagEditRow)rowLayout.getChildAt(i);
			if (ter.getKey().equals(key)) {
				focusRowValue(rowIndex(ter));
				found = true;
				break;
			}
		}
		return found;
	}
	
	/**
	 * Focus on the value field of the first tag with non empty key and empty value 
	 * @param key
	 * @return
	 */
	private boolean focusOnEmptyValue() {
		boolean found = false;
		for (int i = rowLayout.getChildCount() - 1; i >= 0; --i) {
			TagEditRow ter = (TagEditRow)rowLayout.getChildAt(i);
			if (ter.getKey() != null && !ter.getKey().equals("") && ter.getValue().equals("")) {
				focusRowValue(rowIndex(ter));
				found = true;
				break;
			}
		}
		return found;
	}
	
	/**
	 * Move the focus to the key field of the specified row.
	 * @param index The index of the row to move to, counting from 0.
	 * @return true if the row was successfully focussed, false otherwise.
	 */
	private boolean focusRow(int index) {
		TagEditRow row = (TagEditRow)rowLayout.getChildAt(index);
		return row != null && row.keyEdit.requestFocus();
	}
	
	/**
	 * Move the focus to the value field of the specified row.
	 * @param index The index of the row to move to, counting from 0.
	 * @return true if the row was successfully focussed, false otherwise.
	 */
	private boolean focusRowValue(int index) {
		TagEditRow row = (TagEditRow)rowLayout.getChildAt(index);
		return row != null && row.valueEdit.requestFocus();
	}
	
	
	/**
	 */
	private interface ParentRelationHandler {
		abstract void handleParentRelation(final EditText roleEdit, final long relationId);
	}
	
	/**
	 * Perform some processing for each row in the parent relation view.
	 * @param handler The handler that will be called for each row.
	 */
	private void processParentRelations(final ParentRelationHandler handler) {
		final int size = parentRelationsLayout.getChildCount();
		for (int i = 0; i < size; ++i) { 
			View view = parentRelationsLayout.getChildAt(i);
			RelationMembershipRow row = (RelationMembershipRow)view;
			handler.handleParentRelation(row.roleEdit, row.relationId);
		}
	}
	
	/**
	 */
	private interface RelationMemberHandler {
		abstract void handleRelationMember(final TextView typeView, final long elementId, final EditText roleEdit, final TextView descView);
	}
	
	/**
	 * Perform some processing for each row in the relation members view.
	 * @param handler The handler that will be called for each rowr.
	 */
	private void processRelationMembers(final RelationMemberHandler handler) {
		final int size = relationMembersLayout.getChildCount();
		for (int i = 0; i < size; ++i) { 
			View view = relationMembersLayout.getChildAt(i);
			RelationMemberRow row = (RelationMemberRow)view;
			handler.handleRelationMember(row.typeView, row.elementId, row.roleEdit, row.elementView);
		}
	}
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Not yet implemented by Google
		//getWindow().requestFeature(Window.FEATURE_CUSTOM_TITLE);
		//getWindow().setTitle(getString(R.string.tag_title) + " " + type + " " + osmId);
		
		// Disabled because it slows down the Motorola Milestone/Droid
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		prefs = new Preferences(this);
		if (prefs.splitActionBarEnabled()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW); // this might need to be set with bit ops
			}
			// besides hacking ABS, there is no equivalent method to enable this for ABS
		} 
		
		if (prefs.getEnableNameSuggestions()) {
			if (names == null) {
				// this should be done async if it takes too long
				names = new Names(this);
				// names.dump2Log();
			}
		} else {
			names = null; // might have been on before, zap now
		}
		
		setContentView(R.layout.tag_view);
		
		rowLayout = (LinearLayout) findViewById(R.id.edit_row_layout);
		presetsLayout = (LinearLayout) findViewById(R.id.presets_layout);
		parentRelationsLayout = (LinearLayout) findViewById(R.id.relation_membership_layout);
		relationMembersLayout = (LinearLayout) findViewById(R.id.relation_members_layout);
		loaded = false;
		
		// tags

		if (savedInstanceState == null) {
			// No previous state to restore - get the state from the intent
			Log.d(DEBUG_TAG, "Initializing from intent");
			loadData = (TagEditorData)getIntent().getSerializableExtra(TAGEDIT_DATA);
			applyLastAddressTags = (Boolean)getIntent().getSerializableExtra(TAGEDIT_LAST_ADDRESS_TAGS); 
		} else {
			// Restore activity from saved state
			Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
			loadData = (TagEditorData)savedInstanceState.getSerializable(TAGEDIT_DATA);
			// applyLastTags = (Boolean)savedInstanceState.getSerializable(TAGEDIT_LASTTAGS); not saved 
		}

		Log.d(DEBUG_TAG, "... done.");
		osmId = loadData.osmId;
		type = loadData.type;
		loadEdits(loadData.tags);
		originalTags = loadData.originalTags != null ? loadData.originalTags : loadData.tags;
		element = Main.logic.delegator.getOsmElement(type, osmId);
		presets = Main.getCurrentPresets();

		// presets
		createRecentPresetView();
		
		// parent relations
		originalParents = loadData.originalParents != null ? loadData.originalParents : loadData.parents;
		createParentRelationView(loadData.parents);
		
		// members of this relation
		originalMembers = loadData.originalMembers != null ? loadData.originalMembers : loadData.members;
		createMembersView(loadData.members);
		
		loaded = true;
		TagEditRow row = ensureEmptyRow();
		row.keyEdit.requestFocus();
		row.keyEdit.dismissDropDown();
		
		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayShowTitleEnabled(false);
		actionbar.setDisplayHomeAsUpEnabled(true);
		if (loadData.focusOnKey != null) {
			focusOnValue(loadData.focusOnKey);
		} else {
			focusOnEmptyValue(); // probably never actually works
		}
		
		// 
		if (applyLastAddressTags) doAddressTags();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		running = true;
		if (lastAddresses == null) {
			try {
				lastAddresses = savingHelperAddress.load(ADDRESS_TAGS_FILE, false);
				Log.d("TagEditor","onResume read " + lastAddresses.size() + " addresses");
			} catch (Exception e) {
				//TODO be more specific
			}
		}
	}
	
	/**
	 * Given an edit field of an OSM key value, determine it's corresponding source key.
	 * For example, the source of "name" is "source:name". The source of "source" is
	 * "source". The source of "mf:name" is "mf.source:name".
	 * @param keyEdit The edit field of the key to be sourced.
	 * @return The source key for the given key.
	 */
	private static String sourceForKey(final String key) {
		String result = "source";
		if (key != null && !key.equals("") && !key.equals("source")) {
			// key is neither blank nor "source"
			// check if it's namespaced
			int i = key.indexOf(':');
			if (i == -1) {
				result = "source:" + key;
			} else {
				// handle already namespaced keys as per
				// http://wiki.openstreetmap.org/wiki/Key:source
				result = key.substring(0, i) + ".source" + key.substring(i);
			}
		}
		return result;
	}
	
	private void doSourceSurvey() {
		// determine the key (if any) that has the current focus in the key or its value
		final String[] focusedKey = new String[]{null}; // array to work around unsettable final
		processKeyValues(new KeyValueHandler() {
			@Override
			public void handleKeyValue(final EditText keyEdit, final EditText valueEdit) {
				if (keyEdit.isFocused() || valueEdit.isFocused()) {
					focusedKey[0] = keyEdit.getText().toString().trim();
				}
			}
		});
		// ensure source(:key)=survey is tagged
		final String sourceKey = sourceForKey(focusedKey[0]);
		final boolean[] sourceSet = new boolean[]{false}; // array to work around unsettable final
		processKeyValues(new KeyValueHandler() {
			@Override
			public void handleKeyValue(final EditText keyEdit, final EditText valueEdit) {
				if (!sourceSet[0]) {
					String key = keyEdit.getText().toString().trim();
					String value = valueEdit.getText().toString().trim();
					// if there's a blank row - use them
					if (key.equals("") && value.equals("")) {
						key = sourceKey;
						keyEdit.setText(key);
					}
					if (key.equals(sourceKey)) {
						valueEdit.setText(Tags.VALUE_SURVEY);
						sourceSet[0] = true;
					}
				}
			}
		});
		if (!sourceSet[0]) {
			// source wasn't set above - add a new pair
			insertNewEdit(sourceKey, Tags.VALUE_SURVEY, -1);
		}
	}
	
	private void doPresets() {
		if (Main.getCurrentPresets() != null) {
			showPresetDialog();
		}
	}
	
	private void doRepeatLast(boolean merge) {
		Map<String, String> last = savingHelper.load(LAST_TAGS_FILE, false);
		if (last != null) {
			if (merge) {
				final Map<String, String> current = getKeyValueMap(false);
				for (String k: current.keySet()) {
					if (!last.containsKey(k)) {
						last.put(k, current.get(k));
					}
				}
			}
			loadEdits(last);
		}
	}
	
	/**
	 * Add an address from OSM data to the address cache
	 * @param street
	 * @param streetId
	 * @param e
	 * @param addresses
	 */
	private void seedAddressList(String street,long streetId, OsmElement e,LinkedList<Address> addresses) {
		if (e.hasTag(Tags.KEY_ADDR_STREET, street) && e.hasTagKey(Tags.KEY_ADDR_HOUSENUMBER)) {
			Address seed = new Address(e.getName(), e.getOsmId(),getAddressTags(e.getTags()));
			if (streetId > 0) {
				seed.setSide(streetId);
			}
			if (addresses.size() >= MAX_SAVED_ADDRESSES) { //arbitrary limit for now
				addresses.removeLast();
			}
			addresses.addFirst(seed);
			Log.d("TagEditor","seedAddressList added " + seed.tags.toString());
		}
	}
	
	/**
	 * REturn a sorted list of house numbers and the associated address objects
	 * @param street
	 * @param side
	 * @param lastAddresses
	 * @return
	 */
	private TreeMap<Integer,Address> getHouseNumbers(String street, Address.Side side, LinkedList<Address> lastAddresses ) {
		TreeMap<Integer,Address> result = new TreeMap<Integer,Address>(); //list sorted by house numbers
		for (Address a:lastAddresses) {
			if (a != null && a.tags != null 
					&& ((a.tags.get(Tags.KEY_ADDR_STREET) != null && a.tags.get(Tags.KEY_ADDR_STREET).equals(street)) 
							|| (a.tags.get(Tags.KEY_ADDR_PLACE) != null && a.tags.get(Tags.KEY_ADDR_PLACE).equals(street)))
					&& a.tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER)
					&& a.getSide() == side) {
				Log.d("TagEditor","Number " + a.tags.get(Tags.KEY_ADDR_HOUSENUMBER));
				String[] numbers = a.tags.get(Tags.KEY_ADDR_HOUSENUMBER).split("\\,");
				for (String n:numbers) {
					Log.d("TagEditor","add number  " + n);
					try {
						result.put(Integer.valueOf(getNumber(n)),a);
					} catch (NumberFormatException nfe){
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Add address tags
	 * This uses a file to cache/save the address information over invocations of the TagEditor, if the cache doesn't have entries for a specific street/place 
	 * an attempt tp extract the information from the downloaded data is made
	 */
	private void doAddressTags() {
		Address newAddress = null;
		
		final Map<String, String> current = getKeyValueMap(false);
		try {
			lastAddresses = savingHelperAddress.load(ADDRESS_TAGS_FILE, false);
			Log.d("TagEditor","doAddressTags read " + lastAddresses.size() + " addresses");
		} catch (Exception e) {
			//TODO be more specific
		}
		StreetTagValueAutocompletionAdapter streetAdapter = (StreetTagValueAutocompletionAdapter) getStreetNameAutocompleteAdapter();
		// PlaceTagValueAutocompletionAdapter placeAdapter = (PlaceTagValueAutocompletionAdapter) getPlaceNameAutocompleteAdapter();
		if (lastAddresses != null && lastAddresses.size() > 0) {
			newAddress = new Address(getType(), getOsmId(),lastAddresses.get(0).tags); // last address we added
		} 

		if (newAddress == null) { // make sure we have the address object
			newAddress = new Address(getType(), getOsmId(), new LinkedHashMap<String, String>()); 
		}
		// merge in any existing tags
		for (String k: current.keySet()) {
			newAddress.tags.put(k, current.get(k));
		}
		boolean hasPlace = newAddress.tags.containsKey(Tags.KEY_ADDR_PLACE);
		if (streetAdapter != null || hasPlace) {
			// the auto completion arrays should now be calculated, retrieve street names if any
			ArrayList<String> streetNames = new ArrayList<String>(Arrays.asList(streetAdapter.getNames()));
			// ArrayList<String> placeNames = new ArrayList<String>(Arrays.asList(placeAdapter.getNames()));		
			if ((streetNames != null && streetNames.size() > 0) || hasPlace) {
				LinkedHashMap<String, String> tags = newAddress.tags;
				Log.d("TagEditor","tags.get(Tags.KEY_ADDR_STREET)) " + tags.get(Tags.KEY_ADDR_STREET));
				Log.d("TagEditor","Rank of " + tags.get(Tags.KEY_ADDR_STREET) + " " + streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)));
				String street;		
				if (!hasPlace) {
					if (!newAddress.tags.containsKey(Tags.KEY_ADDR_STREET) 
							|| newAddress.tags.get(Tags.KEY_ADDR_STREET).equals("") 
							|| streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)) > 2 
							|| streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)) < 0)  { // check if has street and still in the top 3
						Log.d("TagEditor","names.indexOf(tags.get(Tags.KEY_ADDR_STREET)) " + streetNames.indexOf(tags.get(Tags.KEY_ADDR_STREET)));
						// nope -> zap
						tags.put(Tags.KEY_ADDR_STREET, streetNames.get(0));
						if (tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER)) {
							tags.put(Tags.KEY_ADDR_HOUSENUMBER, "");
						}
					}
					street = tags.get(Tags.KEY_ADDR_STREET); // should now have the final suggestion for a street
					try {
						newAddress.setSide(streetAdapter.getId(street));
					} catch (OsmException e) { // street not in adapter
						newAddress.side = Side.UNKNOWN;
					}
				} else { // ADDR_PLACE minimal support, don't overwrite with street
					street = tags.get(Tags.KEY_ADDR_PLACE);
					newAddress.side = Side.UNKNOWN;
				}
				Log.d("TagEditor","side " + newAddress.getSide());
				Side side = newAddress.getSide();
				// find the addresses corresponding to the current street
				if (street != null && lastAddresses != null) {
					TreeMap<Integer,Address> list = getHouseNumbers(street, side, lastAddresses);
					if (list.size() == 0) { // try to seed lastAddresses from OSM data
						try {
							Log.d("TagEditor","street " + street);
							long streetId = -1;
							if  (!hasPlace) {
								streetId = streetAdapter.getId(street);
							}
							// nodes
							for (Node n:Main.logic.delegator.getCurrentStorage().getNodes()) {
								seedAddressList(street,streetId,(OsmElement)n,lastAddresses);
							}
							// ways
							for (Way w:Main.logic.delegator.getCurrentStorage().getWays()) {
								seedAddressList(street,streetId,(OsmElement)w,lastAddresses);
							}
							// and try again
							list = getHouseNumbers(street, side, lastAddresses);

						} catch (OsmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					// try to predict the next number
					//
					// - get all existing numbers for the side of the street we are on
					// - determine if the increment per number is 1 or 2 (for now everything else is ignored)
					// - determine the nearest address node
					// - if it is the last or first node and we are at one side use that and add or subtract the increment
					// - if the nearest node is somewhere in the middle determine on which side of it we are, 
					// - inc/dec in that direction
					// If everything works out correctly even if a prediction is wrong, entering the correct number should improve the next prediction
					//TODO the following assumes that the road is not doubling back or similar, aka that the addresses are more or less in a straight line, 
					//     use the length along the way defined by the addresses instead
					//
					if (list.size() >= 2) {
						try {
							int firstNumber = list.firstKey();
							int lastNumber = list.lastKey();
							// 
							int inc = 1;
							float incTotal = 0;
							float incCount = 0;
							ArrayList<Integer> numbers = new ArrayList<Integer>(list.keySet());
							for (int i=0;i<numbers.size()-1;i++) {
								int diff = numbers.get(i+1).intValue()-numbers.get(i).intValue();
								if (diff > 0 && diff <= 2) {
									incTotal = incTotal + diff;
									incCount++;
								}
							}
							inc = Math.round(incTotal/incCount);
							int nearest = -1; 
							int prev = -1;
							int post = -1;
							double distanceFirst = 0;
							double distanceLast = 0;
							double distance = Double.MAX_VALUE;
							for (int i=0;i<numbers.size();i++) {
								// determine the nearest existing address
								int number = Integer.valueOf(numbers.get(i));
								Address a = list.get(number);
								double newDistance = GeoMath.haversineDistance(newAddress.lon, newAddress.lat, a.lon, a.lat);
								if (newDistance < distance) {
									distance = newDistance;
									nearest = number;
									prev = numbers.get(Math.max(0, i-1));
									post = numbers.get(Math.min(numbers.size()-1, i+1));
								}
								if (i==0) {
									distanceFirst = newDistance;
								} else if (i==numbers.size()-1) {
									distanceLast = newDistance;
								}
							}
							//
							double distanceTotal = GeoMath.haversineDistance(list.get(firstNumber).lon, list.get(firstNumber).lat, list.get(lastNumber).lon, list.get(lastNumber).lat);
							if (nearest == firstNumber) { 
								if (distanceLast > distanceTotal) {
									inc = -inc;
								}
							} else if (nearest == lastNumber) { 
								if (distanceFirst < distanceTotal) {
									inc = -inc;
								}
							} else {
								double distanceNearestFirst = GeoMath.haversineDistance(list.get(firstNumber).lon, list.get(firstNumber).lat, list.get(nearest).lon, list.get(nearest).lat);
								if (distanceFirst < distanceNearestFirst) {
									inc = -inc;
								} // else already correct
							} 
							// Toast.makeText(this, "First " + firstNumber + " last " + lastNumber + " nearest " + nearest + "inc " + inc + " prev " + prev + " post " + post + " side " + side, Toast.LENGTH_LONG).show();
							Log.d("TagEditor","First " + firstNumber + " last " + lastNumber + " nearest " + nearest + " inc " + inc + " prev " + prev + " post " + post + " side " + side);
							// first apply tags from nearest address if they don't already exist
							for (String key:list.get(nearest).tags.keySet()) {
								if (!tags.containsKey(key)) {
									tags.put(key,list.get(nearest).tags.get(key));
								}
							}
							tags.put(Tags.KEY_ADDR_HOUSENUMBER, "" + Math.max(1, nearest+inc));
						} catch (NumberFormatException nfe){
							tags.put(Tags.KEY_ADDR_HOUSENUMBER, "");
						}
					} else if (list.size() == 1) {
						// can't do prediction with only one value 
						// apply tags from sole existing address if they don't already exist
						for (String key:list.get(list.firstKey()).tags.keySet()) {
							if (!tags.containsKey(key)) {
								tags.put(key,list.get(list.firstKey()).tags.get(key));
							}
						}
					}
				}
			} else { // last ditch attemot
				LinkedHashMap<String, String> tags = new LinkedHashMap<String, String>();
				// fill with Karlsruher schema
				tags.put(Tags.KEY_ADDR_HOUSENUMBER, "");
				tags.put(Tags.KEY_ADDR_STREET, "");
				tags.put(Tags.KEY_ADDR_POSTCODE, "");
				tags.put(Tags.KEY_ADDR_CITY, "");
				tags.put(Tags.KEY_ADDR_COUNTRY, "");
				tags.put(Tags.KEY_ADDR_FULL, "");
				// the following are less used but may be necessary
				tags.put(Tags.KEY_ADDR_HOUSENAME, "");
				tags.put(Tags.KEY_ADDR_PLACE, "");
				tags.put(Tags.KEY_ADDR_HAMLET, "");
				tags.put(Tags.KEY_ADDR_SUBURB, "");
				tags.put(Tags.KEY_ADDR_SUBDISTRICT, "");
				tags.put(Tags.KEY_ADDR_DISTRICT, "");
				tags.put(Tags.KEY_ADDR_PROVINCE, "");
				tags.put(Tags.KEY_ADDR_STATE, "");
				tags.put(Tags.KEY_ADDR_FLATS, "");
				tags.put(Tags.KEY_ADDR_DOOR, "");
				tags.put(Tags.KEY_ADDR_UNIT, "");
				newAddress.tags.putAll(tags); 
			}
		}
		
		// is this a node on a building outline, if yes add entrance=yes if it doesn't already exist
		if (getType().equals(Node.NAME)) {
			boolean isOnBuilding = false;
			for (Way w:Main.logic.getWaysForNode((Node)Main.logic.delegator.getOsmElement(Node.NAME, getOsmId()))) {
				if (w.hasTagKey(Tags.KEY_BUILDING)) {
					isOnBuilding = true;
				} else if (w.getParentRelations() != null) { // need to check relations too
					for (Relation r:w.getParentRelations()) {
						if (r.hasTagKey(Tags.KEY_BUILDING) || r.hasTag(Tags.KEY_TYPE, Tags.VALUE_BUILDING)) {
							isOnBuilding = true;
							break;
						}
					}
				}
				if (isOnBuilding) {
					break;
				}
			}
			if (isOnBuilding && !newAddress.tags.containsKey(Tags.KEY_ENTRANCE)) {
				newAddress.tags.put(Tags.KEY_ENTRANCE, "yes");
			}
		}
		loadEdits(newAddress.tags);
	}
	
	private int getNumber(String hn) throws NumberFormatException {
		StringBuffer sb = new StringBuffer();
		for (Character c:hn.toCharArray()) {
			if (Character.isDigit(c)) {
				sb.append(c);
			}
		}
		if (sb.toString().equals("")) {
			return 0;
		} else {
			return Integer.parseInt(sb.toString());
		}
	}
	
	private void doRevert() {
		loadEdits(originalTags);
		createParentRelationView(originalParents);
		createMembersView(originalMembers);
	}
	
	private void createRecentPresetView() {
		Preset[] presets = Main.getCurrentPresets();
	
		if (presets != null && presets.length >= 1 && element != null) {
			// check if any of the presets has a MRU
			boolean mruFound = false;
			for (Preset p:presets) {
				if (p!=null) {
					if (p.hasMRU()) {
						mruFound = true;
						break;
					}
				}
			}
			if (mruFound) {
				ElementType filterType = element.getType();
				View v = presets[0].getRecentPresetView(this, presets, new PresetClickHandler() { //TODO this should really be a call of a static method, all MRUs get added to this view
					@Override
					public void onItemClick(PresetItem item) {
						Log.d(DEBUG_TAG, "normal click");
						applyPreset(item);
					}

					@Override
					public boolean onItemLongClick(PresetItem item) {
						Log.d(DEBUG_TAG, "long click");
						removePresetFromMRU(item);
						return true;
					}

					@Override
					public void onGroupClick(PresetGroup group) {
						// should not have groups
					}
				}, filterType);

				v.setBackgroundColor(getResources().getColor(R.color.tagedit_field_bg));
				v.setPadding(Preset.SPACING, Preset.SPACING, Preset.SPACING, Preset.SPACING);
				v.setId(R.id.recentPresets);
				presetsLayout.addView(v);
				presetsLayout.setVisibility(View.VISIBLE);
			}
		}
	}
	
	
	/**
	 * Removes an old RecentPresetView and replaces it by a new one (to update it)
	 */
	private void recreateRecentPresetView() {
		View currentView = presetsLayout.findViewById(R.id.recentPresets);
		if (currentView != null) presetsLayout.removeView(currentView);
		createRecentPresetView();
	}
	
	/** 
	 * display relation membership if any
	 * @param parents 
	 */
	private void createParentRelationView(HashMap<Long,String> parents) {
		
		if (parents != null && parents.size() > 0) {
			LinearLayout l = (LinearLayout) findViewById(R.id.membership_heading_view);
			l.setVisibility(View.VISIBLE);
			parentRelationsLayout.removeAllViews();
			for (Long id :  parents.keySet()) {
				Relation r = (Relation) Main.logic.delegator.getOsmElement(Relation.NAME, id.longValue());
				insertNewMembership(parents.get(id),r,0);
			}
		}
	}
		
	private void addToRelation() {
		if ((loadData.parents == null) || (loadData.parents.size() == 0)) { // show heading if none there
			LinearLayout l = (LinearLayout) findViewById(R.id.membership_heading_view);
			l.setVisibility(View.VISIBLE);
		}
		insertNewMembership(null,null,-1);
	}
	
	/** 
	 * display member elements of the relation if any
	 * @param members 
	 */
	private void createMembersView(ArrayList<RelationMemberDescription> members) {
		// if this is a relation get members

		if (members != null && members.size() > 0) {
			LinearLayout l = (LinearLayout) findViewById(R.id.member_heading_view);
			l.setVisibility(View.VISIBLE);
			relationMembersLayout.removeAllViews();
			for (RelationMemberDescription rmd :  members) {
				insertNewMember( members.indexOf(rmd) +"", rmd, -1);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.tag_menu, menu);

		return true;
	}
	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		// disable address tagging for stuff that won't have an address
		menu.findItem(R.id.tag_menu_address).setVisible(!type.equals(Way.NAME) || element.hasTagKey(Tags.KEY_BUILDING));
		
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			sendResultAndFinish();
			return true;
		case R.id.tag_menu_address:
			doAddressTags();
			return true;
		case R.id.tag_menu_sourcesurvey:
			doSourceSurvey();
			return true;
		case R.id.tag_menu_preset:
			doPresets();
			return true;
		case R.id.tag_menu_apply_preset:
			PresetItem pi = Preset.findBestMatch(presets,getKeyValueMap(false));
			if (pi!=null) {
				applyPreset(pi, false); 
			}
			return true;
		case R.id.tag_menu_repeat:
			doRepeatLast(true);
			return true;
		case R.id.tag_menu_revert:
			doRevert();
			return true;
		case R.id.tag_menu_mapfeatures:
			Uri uri = null;
			LinkedHashMap<String, String> map = getKeyValueMap(false);
			if (map !=null) {
				PresetItem p =  Preset.findBestMatch(presets,map);
				if (p != null) {
					uri = p.getMapFeatures();
				}
			}
			if (uri == null) {
				uri = Uri.parse(getString(R.string.link_mapfeatures));
			}
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
			return true;
		case R.id.tag_menu_addtorelation:
			addToRelation();
			return true;
		case R.id.tag_menu_resetMRU:
			for (Preset p:presets)
				p.resetRecentlyUsed();
			recreateRecentPresetView();
			return true;
		case R.id.tag_menu_reset_address_prediction:
			// simply overwrite with an empty file
			savingHelperAddress.save(ADDRESS_TAGS_FILE, new LinkedList<Address>(), false);
			lastAddresses = null;
			return true;
		case R.id.tag_menu_help:
			Intent startHelpViewer = new Intent(this, HelpViewer.class);
			startHelpViewer.putExtra(HelpViewer.TOPIC, "TagEditor");
			startActivity(startHelpViewer);
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onBackPressed() {
		// sendResultAndFinish();
		Map<String, String> currentTags = getKeyValueMap(false);
		HashMap<Long,String> currentParents = getParentRelationMap();
		ArrayList<RelationMemberDescription> currentMembers = getMembersList();
		// if we haven't edited just exit
		if (!currentTags.equals(originalTags) || !currentParents.equals(originalParents) || (element != null && element.getName().equals(Relation.NAME) && !currentMembers.equals(originalMembers))) {
		    new AlertDialog.Builder(this)
	        .setNeutralButton(R.string.cancel, null)
	        .setNegativeButton(R.string.tag_menu_revert,        	
	        		new DialogInterface.OnClickListener() {
	            	@Override
					public void onClick(DialogInterface arg0, int arg1) {
	            		doRevert();
	            }})
	        .setPositiveButton(R.string.tag_menu_exit_no_save, 
	        	new DialogInterface.OnClickListener() {
		            @Override
					public void onClick(DialogInterface arg0, int arg1) {
		                TagEditor.super.onBackPressed();
		            }
	        }).create().show();
		} else {
			TagEditor.super.onBackPressed();
		}
	}
	
	protected LinkedHashMap<String,String> getAddressTags(Map<String,String> sortedMap) {
		LinkedHashMap<String,String> result = new LinkedHashMap<String,String>();
		for (String key:sortedMap.keySet()) {
			if (key.startsWith("addr:")) {
				result.put(key, sortedMap.get(key));
			}
		}
		return result;
	}
	
	/**
	 * 
	 */
	protected void sendResultAndFinish() {
		// Save current tags for "repeat last" button
		LinkedHashMap<String,String> tags = getKeyValueMap(false);
		savingHelper.save(LAST_TAGS_FILE, tags, false);
		// save any address tags for "last address tags"
		LinkedHashMap<String,String> addressTags = getAddressTags(tags);
		// this needs to be done after the edit again in case the street name of what ever has changes 
		if (addressTags.size() > 0) {
			if (lastAddresses == null) {
				lastAddresses = new LinkedList<Address>();
			}
			if (lastAddresses.size() >= MAX_SAVED_ADDRESSES) { //arbitrary limit for now
				lastAddresses.removeLast();
			}
			Address current = new Address(getType(), getOsmId(), addressTags);
			if (streetNameAutocompleteAdapter!= null) {
				String streetName = tags.get(Tags.KEY_ADDR_STREET);
				if (streetName != null) {
					try {
						current.setSide(streetNameAutocompleteAdapter.getId(streetName));
					} catch (OsmException e) {
						current.side = Side.UNKNOWN;
					}
				}
			}
			lastAddresses.addFirst(current);
			savingHelperAddress.save(ADDRESS_TAGS_FILE, lastAddresses, false);
		}
		
		Intent intent = new Intent();
		Map<String, String> currentTags = getKeyValueMap(false);
		HashMap<Long,String> currentParents = getParentRelationMap();
		ArrayList<RelationMemberDescription> currentMembers = getMembersList();
		
		if (!currentTags.equals(originalTags) || !(originalParents==null && currentParents.size()==0) && !currentParents.equals(originalParents) 
				|| (element != null && element.getName().equals(Relation.NAME) && !currentMembers.equals(originalMembers))) {
			// changes were made
			intent.putExtra(TAGEDIT_DATA, new TagEditorData(osmId, type, 
					currentTags.equals(originalTags)? null : currentTags,  null, 
					(originalParents==null && currentParents.size()==0) || currentParents.equals(originalParents)?null:currentParents, null, 
					currentMembers.equals(originalMembers)?null:currentMembers, null));
		}
		
		setResult(RESULT_OK, intent);
		finish();
	}
	
	/**
	 * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
	 */
	protected void loadEdits(final Map<String,String> tags) {
		loaded = false;
		rowLayout.removeAllViews();
		for (Entry<String, String> pair : tags.entrySet()) {
			insertNewEdit(pair.getKey(), pair.getValue(), -1);
		}
		loaded = true;
		ensureEmptyRow();
	}
	
	/** Save the state of this activity instance for future restoration.
	 * @param outState The object to receive the saved state.
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		// no call through. We restore our state from scratch, auto-restore messes up the already loaded edit fields.
		outState.putSerializable(TAGEDIT_DATA, new TagEditorData(osmId, type, getKeyValueMap(true), originalTags, getParentRelationMap(), originalParents, getMembersList(), originalMembers));
	}
	
	/** When the Activity is interrupted, save MRUs and address cache*/
	@Override
	protected void onPause() {
		running = false;
		if (Main.getCurrentPresets() != null)  {
			for (Preset p:Main.getCurrentPresets()) {
				if (p!=null) {
					p.saveMRU();
				}
			}
		}
		if (lastAddresses != null) {
			savingHelperAddress.save(ADDRESS_TAGS_FILE, lastAddresses, false);
		}
		super.onPause();
	}
	
	/**
	 * Gets an adapter for the autocompletion of street names based on the neighborhood of the edited item.
	 * @return
	 */
	private ArrayAdapter<String> getStreetNameAutocompleteAdapter() {
		if (Main.logic == null || Main.logic.delegator == null) {
			return null;
		}
		if (streetNameAutocompleteAdapter == null) {
			streetNameAutocompleteAdapter =	new StreetTagValueAutocompletionAdapter(this,
					R.layout.autocomplete_row, Main.logic.delegator,
					type, osmId);
		}
		return streetNameAutocompleteAdapter;
	}
	
	/**
	 * Gets an adapter for the autocompletion of place names based on the neighborhood of the edited item.
	 * @return
	 */
	private ArrayAdapter<String> getPlaceNameAutocompleteAdapter() {
		if (Main.logic == null || Main.logic.delegator == null) {
			return null;
		}
		if (placeNameAutocompleteAdapter == null) {
			placeNameAutocompleteAdapter =	new PlaceTagValueAutocompletionAdapter(this,
					R.layout.autocomplete_row, Main.logic.delegator,
					type, osmId);
		}
		return placeNameAutocompleteAdapter;
	}

	/**
	 * Insert a new row with one key and one value to edit.
	 * @param aTagKey the key-value to start with
	 * @param aTagValue the value to start with.
	 * @param position the position where this should be inserted. set to -1 to insert at end, or 0 to insert at beginning.
	 * @returns The new TagEditRow.
	 */
	protected TagEditRow insertNewEdit(final String aTagKey, final String aTagValue, final int position) {
		TagEditRow row = (TagEditRow)View.inflate(this, R.layout.tag_edit_row, null);
		row.setValues(aTagKey, aTagValue);
		if (autocompletePresetItem != null) { // set hints even if value isen't empty
			String hint = autocompletePresetItem.getHint(aTagKey);
			if (hint != null) { 
				row.valueEdit.setHint(hint);
			} else if (autocompletePresetItem.getRecommendedTags().keySet().size() > 0 || autocompletePresetItem.getOptionalTags().keySet().size() > 0) {
				row.valueEdit.setHint(R.string.tag_value_hint);
			}
			if (row.valueEdit.getText().toString().length() == 0) {
				String defaultValue = autocompletePresetItem.getDefault(aTagKey);
				if (defaultValue != null) { //
					row.valueEdit.setText(defaultValue);
				} 
			}
		}
		rowLayout.addView(row, (position == -1) ? rowLayout.getChildCount() : position);
		return row;
	}
	
	/**
	 * A row representing an editable tag, consisting of edits for key and value, labels and a delete button.
	 * Needs to be static, otherwise the inflater will not find it.
	 * @author Jan
	 */
	public static class TagEditRow extends LinearLayout {
		
		private TagEditor owner;
		private AutoCompleteTextView keyEdit;
		private AutoCompleteTextView valueEdit;
		
		public TagEditRow(Context context) {
			super(context);
			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
		public TagEditRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
//		public TagEditRow(Context context, AttributeSet attrs, int defStyle) {
//			super(context, attrs, defStyle);
//			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
//		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyEdit = (AutoCompleteTextView)findViewById(R.id.editKey);
			keyEdit.setOnKeyListener(owner.myKeyListener);
			//lastEditKey.setSingleLine(true);
			
			valueEdit = (AutoCompleteTextView)findViewById(R.id.editValue);
			valueEdit.setOnKeyListener(owner.myKeyListener);
			
			// If the user selects addr:street from the menu, auto-fill a suggestion
			keyEdit.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if (Tags.KEY_ADDR_STREET.equals(parent.getItemAtPosition(position)) &&
							valueEdit.getText().toString().length() == 0) {
						ArrayAdapter<String> adapter = owner.getStreetNameAutocompleteAdapter();
						if (adapter != null && adapter.getCount() > 0) {
							valueEdit.setText(adapter.getItem(0));
						}
					} else if (Tags.KEY_ADDR_PLACE.equals(parent.getItemAtPosition(position)) &&
							valueEdit.getText().toString().length() == 0) {
						ArrayAdapter<String> adapter = owner.getPlaceNameAutocompleteAdapter();
						if (adapter != null && adapter.getCount() > 0) {
							valueEdit.setText(adapter.getItem(0));
						}
					} else{
						if (owner.autocompletePresetItem != null) {
							String hint = owner.autocompletePresetItem.getHint(parent.getItemAtPosition(position).toString());
							if (hint != null) { //
								valueEdit.setHint(hint);
							} else if (owner.autocompletePresetItem.getRecommendedTags().keySet().size() > 0 || owner.autocompletePresetItem.getOptionalTags().keySet().size() > 0) {
								valueEdit.setHint(R.string.tag_value_hint);
							}
							if (valueEdit.getText().toString().length() == 0) {
								String defaultValue = owner.autocompletePresetItem.getDefault(parent.getItemAtPosition(position).toString());
								if (defaultValue != null) { //
									valueEdit.setText(defaultValue);
								} 
							}
						}
						// set focus on value
						valueEdit.requestFocus();
					}
				}
			});
			
			keyEdit.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						keyEdit.setAdapter(getKeyAutocompleteAdapter());
						if (running && keyEdit.getText().length() == 0) keyEdit.showDropDown();
					}
				}
			});
			
			valueEdit.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						valueEdit.setAdapter(getValueAutocompleteAdapter());
						if (running) {
							if (valueEdit.getText().length() == 0) valueEdit.showDropDown();
//							try { // hack to display numeric keyboard for numeric tag values
//								  // unluckily there is then no way to get an alpha-numeric keyboard
//								int number = Integer.parseInt(valueEdit.getText().toString());
//								valueEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
//							} catch (NumberFormatException nfe) {
//								// do nothing
//							}
						}
						
					}
				}
			});
			
			valueEdit.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Log.d("TagEdit","onItemClicked value");
					Object o = parent.getItemAtPosition(position);
					if (o instanceof Names.NameAndTags) {
						valueEdit.setText(((NameAndTags)o).getName());
						owner.applyTagSuggestions(((NameAndTags)o).getTags());
					}
				}
			});
			
			View deleteIcon = findViewById(R.id.iconDelete);
			deleteIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!isEmpty()) {
						// can't delete the empty row; TagEditor.ensureEmptyRow() will
						// ensure there is exactly one empty row at the bottom
						deleteRow();
					}
				}
			});
			
			
			OnClickListener autocompleteOnClick = new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (v.hasFocus()) {
						((AutoCompleteTextView)v).showDropDown();
					}
				}
			};
			
			keyEdit.setOnClickListener(autocompleteOnClick);
			valueEdit.setOnClickListener(autocompleteOnClick);
			
			// This TextWatcher reacts to previously empty cells being filled to add additional rows where needed
			TextWatcher emptyWatcher = new TextWatcher() {
				private boolean wasEmpty;
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// nop
				}
				
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					wasEmpty = TagEditRow.this.isEmpty();
				}
				
				@Override
				public void afterTextChanged(Editable s) {
					if (wasEmpty == (s.length() > 0)) {
						// changed from empty to not-empty or vice versa
						owner.ensureEmptyRow();
					}
				}
			};
			keyEdit.addTextChangedListener(emptyWatcher);
			valueEdit.addTextChangedListener(emptyWatcher);
		}
	
		protected ArrayAdapter<String> getKeyAutocompleteAdapter() {
			// Use a set to prevent duplicate keys appearing
			Set<String> keys = new HashSet<String>();
			
			if (owner.autocompletePresetItem == null && owner.presets != null) {
				owner.autocompletePresetItem = Preset.findBestMatch(owner.presets,owner.getKeyValueMap(false));
			}
			
			if (owner.autocompletePresetItem != null) {
				keys.addAll(owner.autocompletePresetItem.getTags().keySet());
				keys.addAll(owner.autocompletePresetItem.getRecommendedTags().keySet());
				keys.addAll(owner.autocompletePresetItem.getOptionalTags().keySet());
			}
			
			if (owner.presets != null && owner.element != null) {
				keys.addAll(Preset.getAutocompleteKeys(owner.presets, owner.element.getType()));
			}
			
			keys.removeAll(owner.getUsedKeys(keyEdit));
			
			List<String> result = new ArrayList<String>(keys);
			Collections.sort(result);
			return new ArrayAdapter<String>(owner, R.layout.autocomplete_row, result);
		}
		
		protected ArrayAdapter<?> getValueAutocompleteAdapter() {
			ArrayAdapter<?> adapter = null;
			String key = keyEdit.getText().toString();
			if (key != null && key.length() > 0) {
				HashSet<String> usedKeys = (HashSet<String>) owner.getUsedKeys(null);
				boolean isStreetName = (Tags.KEY_ADDR_STREET.equalsIgnoreCase(key) ||
						(Tags.KEY_NAME.equalsIgnoreCase(key) && usedKeys.contains(Tags.KEY_HIGHWAY)));
				boolean isPlaceName = (Tags.KEY_ADDR_PLACE.equalsIgnoreCase(key) ||
						(Tags.KEY_NAME.equalsIgnoreCase(key) && usedKeys.contains(Tags.KEY_PLACE)));
				boolean noNameSuggestions = usedKeys.contains(Tags.KEY_HIGHWAY) || usedKeys.contains(Tags.KEY_WATERWAY) 
						|| usedKeys.contains(Tags.KEY_LANDUSE) || usedKeys.contains(Tags.KEY_NATURAL) || usedKeys.contains(Tags.KEY_RAILWAY);
				if (isStreetName) {
					adapter = owner.getStreetNameAutocompleteAdapter();
				} else if (isPlaceName) {
					adapter = owner.getPlaceNameAutocompleteAdapter();
				} else if (key.equals(Tags.KEY_NAME) && (names != null) && !noNameSuggestions) {
					ArrayList<NameAndTags> values = (ArrayList<NameAndTags>) names.getNames(new TreeMap<String,String>(owner.getKeyValueMap(true)));
					if (values != null && !values.isEmpty()) {
						ArrayList<NameAndTags> result = values;
						Collections.sort(result);
						adapter = new ArrayAdapter<NameAndTags>(owner, R.layout.autocomplete_row, result);
					}
				} else {
					if (owner.presets != null && owner.element != null) {
						Collection<String> values = Preset.getAutocompleteValues(owner.presets,owner.element.getType(), key);
						if (values != null && !values.isEmpty()) {
							ArrayList<String> result = new ArrayList<String>(values);
							Collections.sort(result);
							adapter = new ArrayAdapter<String>(owner, R.layout.autocomplete_row, result);
						}
					}
				}
			}
			return adapter;
		}
				
		/**
		 * Sets key and value values
		 * @param aTagKey the key value to set
		 * @param aTagValue the value value to set
		 * @return the TagEditRow object for convenience
		 */
		public TagEditRow setValues(String aTagKey, String aTagValue) {
			keyEdit.setText(aTagKey);
			valueEdit.setText(aTagValue);
			return this;
		}
		
		public String getKey() {
			return keyEdit.getText().toString();
		}
		
		public String getValue() {
			return valueEdit.getText().toString();
		}
		
		/**
		 * Deletes this row
		 */
		public void deleteRow() {
			View cf = owner.getCurrentFocus();
			if (cf == keyEdit || cf == valueEdit) {
				// about to delete the row that has focus!
				// try to move the focus to the next row or failing that to the previous row
				int current = owner.rowIndex(this);
				if (!owner.focusRow(current + 1)) owner.focusRow(current - 1);
			}
			owner.rowLayout.removeView(this);
			if (isEmpty()) {
				owner.ensureEmptyRow();
			}
		}
		
		/**
		 * Checks if the fields in this row are empty
		 * @return true if both fields are empty, false if at least one is filled
		 */
		public boolean isEmpty() {
			return keyEdit.getText().toString().trim().equals("")
				&& valueEdit.getText().toString().trim().equals("");
		}
		
	}
	
	/**
	 * Appy tags from name suggestion list, ask if overwriting
	 * @param tags
	 */
	private void applyTagSuggestions(Names.TagMap tags) {
		final LinkedHashMap<String, String> currentValues = getKeyValueMap(true);
		
		boolean replacedValue = false;	
		
		// Fixed tags, always have a value. We overwrite mercilessly.
		for (Entry<String, String> tag : tags.entrySet()) {
			String oldValue = currentValues.put(tag.getKey(), tag.getValue());
			if (oldValue != null && oldValue.length() > 0 && !oldValue.equals(tag.getValue())) replacedValue = true;
		}
		if (replacedValue) {
			Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle(R.string.tag_editor_name_suggestion);
			dialog.setMessage(R.string.tag_editor_name_suggestion_overwrite_message);
			dialog.setPositiveButton(R.string.replace, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					loadEdits(currentValues);
				}
			});
			dialog.setNegativeButton(R.string.cancel, null);
			dialog.create().show();
		} else
			loadEdits(currentValues);
		
// TODO while applying presets automatically seems like a good idea, it needs some further thought
		if (prefs.enableAutoPreset()) {
			PresetItem p = Preset.findBestMatch(presets,getKeyValueMap(false));
			if (p!=null) {
				applyPreset(p, false); 
			}
		}
	}
	
	/**
	 * Collect all key-value pairs into a LinkedHashMap<String,String>
	 * 
	 * @param allowBlanks If true, includes key-value pairs where one or the other is blank.
	 * @return The LinkedHashMap<String,String> of key-value pairs.
	 */
	private LinkedHashMap<String,String> getKeyValueMap(final boolean allowBlanks) {
		final LinkedHashMap<String,String> tags = new LinkedHashMap<String, String>();
		processKeyValues(new KeyValueHandler() {
			@Override
			public void handleKeyValue(final EditText keyEdit, final EditText valueEdit) {
				String key = keyEdit.getText().toString().trim();
				String value = valueEdit.getText().toString().trim();
				boolean bothBlank = "".equals(key) && "".equals(value);
				boolean neitherBlank = !"".equals(key) && !"".equals(value);
				if (!bothBlank) {
					// both blank is never acceptable
					if (neitherBlank || allowBlanks) {
						tags.put(key, value);
					}
				}
			}
		});
		return tags;
	}	
	
	/**
	 * Get all key values currently in the editor, optionally skipping one field.
	 * @param ignoreEdit optional - if not null, this key field will be skipped,
	 *                              i.e. the key  in it will not be included in the output
	 * @return the set of all (or all but one) keys currently entered in the edit boxes
	 */
	private Set<String> getUsedKeys(final EditText ignoreEdit) {
		final HashSet<String> keys = new HashSet<String>();
		processKeyValues(new KeyValueHandler() {
			@Override
			public void handleKeyValue(final EditText keyEdit, final EditText valueEdit) {
				if (!keyEdit.equals(ignoreEdit)) {
					String key = keyEdit.getText().toString().trim();
					if (key.length() > 0) {
						keys.add(key);
					}
				}
			}
		});
		return keys;
	}
	
	/**
	 * Insert a new row of key+value -edit-widgets if some text is entered into the current one.
	 * 
	 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
	 */
	private class MyKeyListener implements OnKeyListener {
		@Override
		public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
			if (keyEvent.getAction() == KeyEvent.ACTION_UP || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) {
				if (view instanceof EditText) {
					//on Enter -> goto next EditText
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						View nextView = view.focusSearch(View.FOCUS_RIGHT);
						if (!(nextView instanceof EditText)) {
							nextView = view.focusSearch(View.FOCUS_LEFT);
							if (nextView != null) {
								nextView = nextView.focusSearch(View.FOCUS_DOWN);
							}
						}
						if (nextView != null && nextView instanceof EditText) {
							nextView.requestFocus();
							return true;
						}
					}
				}
			}
			return false;
		}
	}
	
	/**
	 * Collect all interesting values from the parent relation view HashMap<String,String>, currently only the role value
	 * 
	 * @return The HashMap<Long,String> of relation and role in that relation pairs.
	 */
	private HashMap<Long,String> getParentRelationMap() {
		final HashMap<Long,String> parents = new HashMap<Long,String>();
		processParentRelations(new ParentRelationHandler() {
			@Override
			public void handleParentRelation(final EditText roleEdit, final long relationId) {
				String role = roleEdit.getText().toString().trim();
				parents.put(Long.valueOf(relationId), role);
			}
		});
		return parents;
	}	
	
	/**
	 * Collect all interesting values from the relation member view 
	 * RelationMemberDescritption is an extended version of RelationMember that holds a textual description of the element 
	 * instead of the element itself
	 * 
	 * @return ArrayList<RelationMemberDescription>.
	 */
	private ArrayList<RelationMemberDescription> getMembersList() {
		final ArrayList<RelationMemberDescription> members = new ArrayList<RelationMemberDescription>();
		processRelationMembers(new RelationMemberHandler() {
			@Override
			public void handleRelationMember(final TextView typeView, final long elementId, final EditText roleEdit, final TextView descView) {
				String type = typeView.getText().toString().trim();
				String role = roleEdit.getText().toString().trim();
				String desc = descView.getText().toString().trim();
				RelationMemberDescription rmd = new RelationMemberDescription(type,elementId,role,desc);
				members.add(rmd);
			}
		});
		return members;
	}	
	
	/**
	 * Insert a new row with a relation member
	 * @param pos (currently unused)
	 * @param rmd information on the relation member
	 * @param position the position where this should be inserted. set to -1 to insert at end, or 0 to insert at beginning.
	 * @returns The new RelationMemberRow.
	 */
	protected RelationMemberRow insertNewMember(final String pos, final RelationMemberDescription rmd, final int position) {
		RelationMemberRow row = (RelationMemberRow)View.inflate(this, R.layout.relation_member_row, null);
		row.setValues(pos, rmd);
		relationMembersLayout.addView(row, (position == -1) ? relationMembersLayout.getChildCount() : position);
		return row;
	}
	
	/**
	 * A row representing an editable member of a relation, consisting of edits for role and display of other values and a delete button.
	 */
	public static class RelationMemberRow extends LinearLayout {
		
		private TagEditor owner;
		private long elementId;
		private AutoCompleteTextView roleEdit;
		private TextView typeView;
		private TextView elementView;
		
		public RelationMemberRow(Context context) {
			super(context);
			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
		public RelationMemberRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
//		public RelationMemberRow(Context context, AttributeSet attrs, int defStyle) {
//			super(context, attrs, defStyle);
//			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
//		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			roleEdit = (AutoCompleteTextView)findViewById(R.id.editMemberRole);
			roleEdit.setOnKeyListener(owner.myKeyListener);
			//lastEditKey.setSingleLine(true);
			
			typeView = (TextView)findViewById(R.id.memberType);
			
			elementView = (TextView)findViewById(R.id.memberObject);
			
			roleEdit.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						roleEdit.setAdapter(getRoleAutocompleteAdapter());
						if (running && roleEdit.getText().length() == 0) roleEdit.showDropDown();
					}
				}
			});
			
			
			View deleteIcon = findViewById(R.id.iconDelete);
			deleteIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					deleteRow();
				}
			});
			
			
			OnClickListener autocompleteOnClick = new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (v.hasFocus()) {
						((AutoCompleteTextView)v).showDropDown();
					}
				}
			};

			roleEdit.setOnClickListener(autocompleteOnClick);
		}
		
		/**
		 * 
		 * @return
		 */
		protected ArrayAdapter<String> getRoleAutocompleteAdapter() {
			// Use a set to prevent duplicate keys appearing
			Set<String> roles = new HashSet<String>();
					
			if ( owner.presets != null && owner.autocompletePresetItem != null) {
				PresetItem relationPreset = Preset.findBestMatch(owner.presets,owner.getKeyValueMap(false));
				if (relationPreset != null) {
					roles.addAll(owner.autocompletePresetItem.getRoles());
				}
			}

			List<String> result = new ArrayList<String>(roles);
			Collections.sort(result);
			return new ArrayAdapter<String>(owner, R.layout.autocomplete_row, result);
		}
		
		
		/**
		 * Sets the per row values for a relation member
		 * @param pos not used
		 * @param rmd the information on the relation member
		 * @return elationMemberRow object for convenience
		 */
		public RelationMemberRow setValues(String pos, RelationMemberDescription rmd) {
			
			String desc = rmd.getDescription();
			String objectType = rmd.getType() == null ? "--" : rmd.getType();
			elementId = rmd.getRef();
			roleEdit.setText(rmd.getRole());
			typeView.setText(objectType);
			elementView.setText(desc);
			return this;
		}
		
		public long getOsmId() {
			return elementId;
		}
		
		public String getRole() {
			return roleEdit.getText().toString();
		}
		
		/**
		 * Deletes this row
		 */
		public void deleteRow() {
			View cf = owner.getCurrentFocus();
			if (cf == roleEdit) {
				 owner.focusRow(0); // focus on first row of tag editing for now
			}
			owner.relationMembersLayout.removeView(this);
		}
		
		/**
		 * Checks if the fields in this row are empty
		 * @return true if both fields are empty, false if at least one is filled
		 */
		public boolean isEmpty() {
			return  roleEdit.getText().toString().trim().equals("");
		}
	}
	
	/**
	 * Insert a new row with a parent relation 
	 * 
	 * @param role		role of this element in the relation
	 * @param r			the relation
	 * @param position the position where this should be inserted. set to -1 to insert at end, or 0 to insert at beginning.
	 * @return the new RelationMembershipRow
	 */
	protected RelationMembershipRow insertNewMembership(final String role, final Relation r, final int position) {
		RelationMembershipRow row = (RelationMembershipRow)View.inflate(this, R.layout.relation_membership_row, null);
		if (r != null) row.setValues(role, r);
		parentRelationsLayout.addView(row, (position == -1) ? parentRelationsLayout.getChildCount() : position);
		return row;
	}
	
	/**
	 * A row representing a parent relation with an edits for role and further values and a delete button.
	 */
	public static class RelationMembershipRow extends LinearLayout {
		
		private TagEditor owner;
		private long relationId =-1; // flag value for new relation memberships
		private AutoCompleteTextView roleEdit;
		private Spinner parentEdit;
		private ArrayAdapter<String> roleAdapter; 
		
		public RelationMembershipRow(Context context) {
			super(context);
			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
		public RelationMembershipRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
//		public RelationMembershipRow(Context context, AttributeSet attrs, int defStyle) {
//			super(context, attrs, defStyle);
//			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
//		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			roleEdit = (AutoCompleteTextView)findViewById(R.id.editRole);
			roleEdit.setOnKeyListener(owner.myKeyListener);
			
			parentEdit = (Spinner)findViewById(R.id.editParent);
			ArrayAdapter<Relation> a = getRelationSpinnerAdapter();
			a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			parentEdit.setAdapter(a);
			parentEdit.setOnItemSelectedListener(owner);

			

			roleEdit.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						roleEdit.setAdapter(getRoleAutocompleteAdapter());
						if (running && roleEdit.getText().length() == 0) roleEdit.showDropDown();
					}
				}
			});
			
						
			View deleteIcon = findViewById(R.id.iconDelete);
			deleteIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					new AlertDialog.Builder(owner)
					.setTitle(R.string.delete)
					.setMessage(R.string.delete_from_relation_description)
					.setPositiveButton(R.string.delete,
						new DialogInterface.OnClickListener() {	
							@Override
							public void onClick(DialogInterface dialog, int which) {
								deleteRow();
							}
						})
					.show();					
				}
			});
			
			
			OnClickListener autocompleteOnClick = new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (v.hasFocus()) {
						((AutoCompleteTextView)v).showDropDown();
					}
				}
			};
			
			roleEdit.setOnClickListener(autocompleteOnClick);
		}
		
		protected ArrayAdapter<String> getRoleAutocompleteAdapter() {
			// Use a set to prevent duplicate keys appearing
			Set<String> roles = new HashSet<String>();
			Relation r = (Relation) Main.logic.delegator.getOsmElement(Relation.NAME, relationId);
			if ( r!= null) {			
				if ( owner.presets != null) {
					PresetItem relationPreset = Preset.findBestMatch(owner.presets,r.getTags());
					if (relationPreset != null) {
						roles.addAll(relationPreset.getRoles());
					}
				}
			}
			
			List<String> result = new ArrayList<String>(roles);
			Collections.sort(result);
			roleAdapter = new ArrayAdapter<String>(owner, R.layout.autocomplete_row, result);
			
			return roleAdapter;
		}
		
		protected ArrayAdapter<Relation> getRelationSpinnerAdapter() {
			//
			
			List<Relation> result = Main.logic.delegator.getCurrentStorage().getRelations();;
			// Collections.sort(result);
			return new ArrayAdapter<Relation>(owner, R.layout.autocomplete_row, result);
		}
		
		
		/**
		 * Sets key and value values
		 * @param aTagKey the key value to set
		 * @param aTagValue the value value to set
		 * @return the TagEditRow object for convenience
		 */
		public RelationMembershipRow setValues(String role, Relation r) {
			relationId = r.getOsmId();
			roleEdit.setText(role);
			parentEdit.setSelection(Main.logic.delegator.getCurrentStorage().getRelations().indexOf(r));
			return this;
		}
		
		/**
		 * Sets key and value values
		 * @param aTagKey the key value to set
		 * @param aTagValue the value value to set
		 * @return the TagEditRow object for convenience
		 */
		public RelationMembershipRow setRelation(Relation r) {
			relationId = r.getOsmId();
			parentEdit.setSelection(Main.logic.delegator.getCurrentStorage().getRelations().indexOf(r));
			Log.d("TagEditor", "Set parent relation to " + relationId + " " + r.getDescription());
			roleEdit.setAdapter(getRoleAutocompleteAdapter()); // update 
			return this;
		}
		
		public long getOsmId() {
			return relationId;
		}
	
		
		public String getRole() {
			return roleEdit.getText().toString();
		}
		
		
		/**
		 * Deletes this row
		 */
		public void deleteRow() {
			View cf = owner.getCurrentFocus();
			if (cf == roleEdit) {
				owner.focusRow(0); // focus on first row of tag editing for now
			}
			owner.parentRelationsLayout.removeView(this);
		}
	}
	    
    @Override
	public void onItemSelected(AdapterView<?> parent, View view, 
            int pos, long id) {
    	
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
    	Log.d("TagEditor", ((Relation)parent.getItemAtPosition(pos)).getDescription());
    	ViewParent pv = view.getParent();
    	while (!(pv instanceof RelationMembershipRow))
    		pv = pv.getParent();
    	((RelationMembershipRow)pv).setRelation((Relation)parent.getItemAtPosition(pos));	
    }

    @Override
	public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
	
	
	/**
	 * @return the OSM ID of the element currently edited by the editor
	 */
	public long getOsmId() {
		return osmId;
	}
	
	/**
	 * Set the OSM ID currently edited by the editor
	 */
	public void setOsmId(final long osmId) {
		this.osmId = osmId;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(final String type) {
		this.type = type;
	}
	
	protected OnKeyListener getKeyListener() {
		return myKeyListener;
	}
	
	/**
	 * Shows the preset dialog for choosing which preset to apply
	 */
	private void showPresetDialog() {
		if (Main.getCurrentPresets() != null && element != null) {
			presetDialog = new PresetDialog(this, Main.getCurrentPresets(), element);
			presetDialog.setOnDismissListener(this);
			presetDialog.show();
		}
	}
	
	/**
	 * Handles the result from the preset dialog
	 * @param dialog
	 */
	@Override
	public void onDismiss(DialogInterface dialog) {
		PresetItem result = presetDialog.getDialogResult();
		if (result != null) {
			applyPreset(result);
		}
	}
	
	/**
	 * Applies a preset (e.g. selected from the dialog or MRU), i.e. adds the tags from the preset to the current tag set
	 * @param item the preset to apply
	 */
	private void applyPreset(PresetItem item) {
		applyPreset(item, true);
	}
	
	/**
	 * Applies a preset (e.g. selected from the dialog or MRU), i.e. adds the tags from the preset to the current tag set
	 * @param item the preset to apply
	 */
	private void applyPreset(PresetItem item, boolean addToMRU) {
		autocompletePresetItem = item;
		LinkedHashMap<String, String> currentValues = getKeyValueMap(true);
		
		boolean replacedValue = false;	
		
		// Fixed tags, always have a value. We overwrite mercilessly.
		for (Entry<String, String> tag : item.getTags().entrySet()) {
			String oldValue = currentValues.put(tag.getKey(), tag.getValue());
			if (oldValue != null && oldValue.length() > 0 && !oldValue.equals(tag.getValue())) {
				replacedValue = true;
			}
		}
		
		// Recommended tags, no fixed value is given. We add only those that do not already exist.
		for (Entry<String, String[]> tag : item.getRecommendedTags().entrySet()) {
			if (!currentValues.containsKey(tag.getKey())) {
				currentValues.put(tag.getKey(), "");
			}
		}
		
		loadEdits(currentValues);
		if (replacedValue) Toast.makeText(this, R.string.toast_preset_overwrote_tags, Toast.LENGTH_LONG).show();
		
		//
		if (addToMRU) {
			Preset[] presets = Main.getCurrentPresets();
			if (presets != null) {
				for (Preset p:presets) {
					if (p.contains(item)) {
						p.putRecentlyUsed(item);
						break;
					}
				}
			}
			recreateRecentPresetView();
		}
		focusOnEmptyValue();
	}
	
	/**
	 * Removes a preset from the MRU
	 * @param item the preset to apply
	 */
	private void removePresetFromMRU(PresetItem item) {
		
		//
		Preset[] presets = Main.getCurrentPresets();
		if (presets != null) {
			for (Preset p:presets) {
				if (p.contains(item)) {
					p.removeRecentlyUsed(item);
					break;
				}
			}
		}
		recreateRecentPresetView();
	}
	
	/**
	 * Holds data sent in intents.
	 * Directly serializing a TreeMap in an intent does not work, as it comes out as a HashMap (?!?) 
	 * @author Jan
	 */
	public static class TagEditorData implements Serializable {
		private static final long serialVersionUID = 2L;
		
		public final long osmId;
		public final String type;
		public final Map<String,String> tags;
		public final Map<String,String> originalTags;
		public final HashMap<Long,String> parents; // just store the ids and role
		public final HashMap<Long,String> originalParents; // just store the ids and role
		public final ArrayList<RelationMemberDescription> members;
		public final ArrayList<RelationMemberDescription> originalMembers;
		
		public String focusOnKey = null;
		
		public TagEditorData(long osmId, String type, Map<String, String> tags, Map<String, String> originalTags, HashMap<Long,String> parents, HashMap<Long,String> originalParents, ArrayList<RelationMemberDescription> members, ArrayList<RelationMemberDescription> originalMembers) {
			this(osmId, type, tags, originalTags, parents, originalParents, members, originalMembers, null);
		}
		
		public TagEditorData(long osmId, String type, Map<String, String> tags, Map<String, String> originalTags, HashMap<Long,String> parents, HashMap<Long,String> originalParents, ArrayList<RelationMemberDescription> members, ArrayList<RelationMemberDescription> originalMembers, String focusOnKey) {
			this.osmId = osmId;
			this.type = type;
			this.tags = tags;
			this.originalTags = originalTags;
			this.parents = parents;
			this.originalParents = originalParents;
			this.members = members;
			this.originalMembers = originalMembers;
			this.focusOnKey = focusOnKey;
		}
		
		public TagEditorData(OsmElement selectedElement) {
			this(selectedElement, null);
		}
		
		public TagEditorData(OsmElement selectedElement, String focusOnKey) {
			osmId = selectedElement.getOsmId();
			type = selectedElement.getName();
			tags = new LinkedHashMap<String, String>(selectedElement.getTags());
			originalTags = tags;
			HashMap<Long,String> tempParents = new HashMap<Long,String>();
			if (selectedElement.getParentRelations() != null) {
				for (Relation r:selectedElement.getParentRelations()) {
					RelationMember rm = r.getMember(selectedElement);
					if (rm != null)
						tempParents.put(Long.valueOf(r.getOsmId()), rm.getRole());
					else
						Log.e("TagEditor","inconsistency in relation membership");
				}
				parents = tempParents;
				originalParents = parents;
			}
			else {
				parents = null;
				originalParents = null;
			}
			ArrayList<RelationMemberDescription> tempMembers = new ArrayList<RelationMemberDescription>();
			if (selectedElement.getName().equals(Relation.NAME)) {
				for (RelationMember rm:((Relation)selectedElement).getMembers()) {
					RelationMemberDescription newRm = new RelationMemberDescription(rm);
					tempMembers.add(newRm);
				}
				members = tempMembers;
				originalMembers = members;
			}
			else {
				members = null;
				originalMembers = null;
			}
			
			this.focusOnKey = focusOnKey;
		}
	}
}
