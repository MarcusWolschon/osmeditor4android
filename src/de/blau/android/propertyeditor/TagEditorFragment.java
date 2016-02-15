package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.acra.ACRA;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.Application;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.PresetKeyType;
import de.blau.android.presets.StreetTagValueAutocompletionAdapter;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.util.ClipboardUtils;
import de.blau.android.util.KeyValue;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Util;
import de.blau.android.views.CustomAutoCompleteTextView;


	
public class TagEditorFragment extends SherlockFragment implements
		PropertyRows, EditorUpdate {

	private static final String DEBUG_TAG = TagEditorFragment.class.getSimpleName();

	static final char LIST_SEPARATOR = ';';
	 
	private SavingHelper<LinkedHashMap<String,String>> savingHelper
				= new SavingHelper<LinkedHashMap<String,String>>();
		
	static SelectedRowsActionModeCallback tagSelectedActionModeCallback = null;
	
	PresetItem autocompletePresetItem = null;
	
	private Names names = null;

	private boolean loaded = false;
	private String[] types;
	private long[] osmIds;
	private Preferences prefs = null;
	private OsmElement[] elements = null;
	
	LayoutInflater inflater = null;
	
	private NameAdapters nameAdapters = null;

	/**
	 * saves any changed fields on onPause
	 */
	protected LinkedHashMap<String,ArrayList<String>> savedTags = null;
	
	/**
	 * per tag preset association
	 */
	protected HashMap<String,PresetItem> tags2Preset = new HashMap<String,PresetItem>();
	
	/**
	 * per tag preset association
	 */
	protected ArrayList<PresetItem> secondaryPresets = new ArrayList<PresetItem>();
	
	/**
	 * selective copy of tags
	 */
	protected Map<String, String> copiedTags = null;

	private FormUpdate formUpdate;
	
	/**
	 * Interface for handling the key:value pairs in the TagEditor.
	 * @author Andrew Gregory
	 */
	protected interface KeyValueHandler {
		abstract void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final ArrayList<String>tagValues);
	}
	
	/**
	 * Perform some processing for each key:value pair in the TagEditor.
	 * @param handler The handler that will be called for each key:value pair.
	 */
	private void processKeyValues(final KeyValueHandler handler) {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		processKeyValues(rowLayout, handler);
	}
	
	/**
	 * Perform some processing for each key:value pair in the TagEditor.
	 * @param handler The handler that will be called for each key:value pair.
	 */
	private void processKeyValues(LinearLayout rowLayout, final KeyValueHandler handler) {
		final int size = rowLayout.getChildCount();
		for (int i = 0; i < size; ++i) {
			View view = rowLayout.getChildAt(i);
			TagEditRow row = (TagEditRow)view;
			handler.handleKeyValue(row.keyEdit, row.valueEdit, row.tagValues);
		}
	}
	
	/**
	 * @param applyLastAddressTags 
	 * @param focusOnKey 
	 * @param displayMRUpresets 
     */
    static public TagEditorFragment newInstance(OsmElement[] elements, ArrayList<LinkedHashMap<String,String>> tags, boolean applyLastAddressTags, 
    											String focusOnKey, boolean displayMRUpresets) {
    	TagEditorFragment f = new TagEditorFragment();
    	
        Bundle args = new Bundle();
   
        args.putSerializable("elements", elements);
        args.putSerializable("tags", tags);
        args.putSerializable("applyLastAddressTags", Boolean.valueOf(applyLastAddressTags));
        args.putSerializable("focusOnKey", focusOnKey);
        args.putSerializable("displayMRUpresets", Boolean.valueOf(displayMRUpresets));

        f.setArguments(args);
        // f.setShowsDialog(true);
        
        return f;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
        try {
            nameAdapters = (NameAdapters) activity;
            formUpdate = (FormUpdate) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement NameAdapters");
        }
        setHasOptionsMenu(true);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       	Log.d(DEBUG_TAG, "onCreate");
    }
	
	/** 
	 * display member elements of the relation if any
	 * @param members 
	 */
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	ScrollView rowLayout = null;

		if (savedInstanceState == null) {
			// No previous state to restore - get the state from the intent
			Log.d(DEBUG_TAG, "Initializing from original arguments");
		} else {
			// Restore activity from saved state
			Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
			@SuppressWarnings("unchecked")
			Map<String, ArrayList<String>> temp = (Map<String, ArrayList<String>>) savedInstanceState.getSerializable("SAVEDTAGS");
			savedTags = new LinkedHashMap<String, ArrayList<String>>();
			savedTags.putAll(temp);
		}
    	
    	prefs = new Preferences(getActivity());
    	
		if (prefs.getEnableNameSuggestions()) {
			names = Application.getNames(getActivity());
		}
    	
     	this.inflater = inflater;
     	rowLayout = (ScrollView) inflater.inflate(R.layout.taglist_view, null);
       
     	LinearLayout editRowLayout = (LinearLayout) rowLayout.findViewById(R.id.edit_row_layout);
     	// editRowLayout.setSaveFromParentEnabled(false);
     	editRowLayout.setSaveEnabled(false); 
     	
     	try {
			elements = (OsmElement[]) getArguments().getSerializable("elements");
		} catch (ClassCastException cce) {
			Log.d(DEBUG_TAG,"onCreateView called in funny state");
			ACRA.getErrorReporter().handleException(null);
			return null;
		}
     	types = new String[elements.length];
     	osmIds =  new long[elements.length];
     	for (int i=0;i< elements.length;i++) {
     		types[i] = elements[i].getName();
     		osmIds[i] = elements[i].getOsmId();
     	}
     	
     	LinkedHashMap<String,ArrayList<String>> tags;
     	if (savedTags != null) { // view was destroyed and needs to be recreated with current state
     		Log.d(DEBUG_TAG,"Restoring from instance variable");
     		tags = savedTags;
     	} else {
     		tags = buildEdits();
     	}
     	boolean applyLastAddressTags = ((Boolean) getArguments().getSerializable("applyLastAddressTags")).booleanValue();
     	String focusOnKey = (String)  getArguments().getSerializable("focusOnKey");
     	boolean displayMRUpresets = ((Boolean) getArguments().getSerializable("displayMRUpresets")).booleanValue();
     	
       	// Log.d(DEBUG_TAG,"element " + element + " tags " + tags);
		
       	loaded = false;
		// rowLayout.removeAllViews();
		for (Entry<String, ArrayList<String>> pair : tags.entrySet()) {
			insertNewEdit(editRowLayout, pair.getKey(), pair.getValue(), -1);
		}
	
		loaded = true;
		TagEditRow row = ensureEmptyRow(editRowLayout);
	
		if (getUserVisibleHint()) { // don't request focus if we are not visible 
			Log.d(DEBUG_TAG,"is visible");
			row.keyEdit.requestFocus();
			row.keyEdit.dismissDropDown();
		
			if (focusOnKey != null) {
				focusOnValue(editRowLayout,focusOnKey);
			} else {
				focusOnEmptyValue(editRowLayout); // probably never actually works
			}
		}	
		// 
		if (applyLastAddressTags) {
			loadEdits(editRowLayout,Address.predictAddressTags(getType(),getOsmId(),
					((StreetTagValueAutocompletionAdapter)nameAdapters.getStreetNameAutocompleteAdapter(null)).getElementSearch(), 
					getKeyValueMap(editRowLayout,false), Address.DEFAULT_HYSTERESIS));
			if (getUserVisibleHint()) {
				if (!focusOnValue(editRowLayout,Tags.KEY_ADDR_HOUSENUMBER)) {
					focusOnValue(editRowLayout,Tags.KEY_ADDR_STREET);
				} // this could be a bit more refined
			}
		}

		updateAutocompletePresetItem(editRowLayout); // set preset from initial tags
		
		if (displayMRUpresets) {
			Log.d(DEBUG_TAG,"Adding MRU prests");
			FragmentManager fm = getChildFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
			if (recentPresetsFragment != null) {
				ft.remove(recentPresetsFragment);
			}
			
			recentPresetsFragment = RecentPresetsFragment.newInstance(elements[0]); // FIXME
			ft.add(R.id.tag_mru_layout,recentPresetsFragment,"recentpresets_fragment");
			ft.commit();
		}
		
		CheckBox headerCheckBox = (CheckBox) rowLayout.findViewById(R.id.header_tag_selected);
		headerCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					selectAllTags();
				} else {
					deselectAllTags();
				}
			}
		});
		Log.d(DEBUG_TAG,"onCreateView returning");
		return rowLayout;
	}
    
    /**
     * Build the data structure we use to build the edit display
     * @return
     */
    LinkedHashMap<String,ArrayList<String>> buildEdits() {
		@SuppressWarnings("unchecked")
    	ArrayList<LinkedHashMap<String,String>> originalTags = (ArrayList<LinkedHashMap<String,String>>)getArguments().getSerializable("tags");
 		// 
    	LinkedHashMap<String,ArrayList<String>> tags = new LinkedHashMap<String,ArrayList<String>>();
 		for (LinkedHashMap<String,String>map:originalTags) {
 			for (String key:map.keySet()) {
 				String value = map.get(key);
 				if (!tags.containsKey(key)) {
 					tags.put(key,new ArrayList<String>());
 				}
 				tags.get(key).add(value);
 			}
 		}
 		// for those keys that don't have a value for each element add an empty string
 		int l = originalTags.size();
 		for (ArrayList<String>v:tags.values()) {
 			if (v.size() != l) {
 				v.add("");
 			}
 		}
 		return tags;
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	Log.d(DEBUG_TAG, "onStart");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.d(DEBUG_TAG, "onResume");
    	formUpdate.tagsUpdated(); // kick the form
    }
	
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	Log.d(DEBUG_TAG, "onSaveInstanceState");
    	outState.putSerializable("SAVEDTAGS", savedTags);
    }  
    
    @Override
    public void onPause() {
    	super.onPause();
    	Log.d(DEBUG_TAG, "onPause");
    	savedTags  = getKeyValueMap(true);
    }
       
    @Override
    public void onStop() {
    	super.onStop();
    	Log.d(DEBUG_TAG, "onStop");
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(DEBUG_TAG, "onDestroy");
    }
    
    @Override
    public void onDestroyView() {
    	super.onDestroyView();
    	Log.d(DEBUG_TAG, "onDestroyView");
    }
    
    /**
 	 * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
 	 * Backwards compatible version
 	 */
 	protected void loadEditsSingle(final Map<String,String> tags) {
 		LinearLayout rowLayout = (LinearLayout) getOurView();
 		LinkedHashMap<String,ArrayList<String>> convertedTags = new LinkedHashMap<String,ArrayList<String>>();
 		for (String key:tags.keySet()) {
 			ArrayList<String> v = new ArrayList<String>();
 			v.add(tags.get(key));
 			convertedTags.put(key, v);
 		}
 		loadEdits(rowLayout, convertedTags);
 	}
    
    /**
	 * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
	 */
	protected void loadEdits(final Map<String,ArrayList<String>> tags) {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		loadEdits(rowLayout, tags);
	}
	
	/**
	 * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
	 */
	protected void loadEdits(LinearLayout rowLayout, final Map<String,ArrayList<String>> tags) {
	
		loaded = false;
		rowLayout.removeAllViews();
		for (Entry<String, ArrayList<String>> pair : tags.entrySet()) {
			insertNewEdit(rowLayout, pair.getKey(), pair.getValue(), -1);
		}
		loaded = true;
		ensureEmptyRow(rowLayout);
	}

	private void updateAutocompletePresetItem(LinearLayout rowLayout) {
		Log.d(DEBUG_TAG,"setting new autocompletePresetItem");
		clearPresets();
		clearSecondaryPresets();
		LinkedHashMap<String, String> allTags = getKeyValueMapSingle(rowLayout,true);
		autocompletePresetItem = Preset.findBestMatch(((PropertyEditor)getActivity()).presets, allTags); // FIXME multiselect
    	
    	Map<String, String> nonAssigned = addPresetsToTags(autocompletePresetItem, allTags);
    	int nonAssignedCount = nonAssigned.size();
    	while (nonAssignedCount > 0) {
    		PresetItem nonAssignedPreset = Preset.findBestMatch(((PropertyEditor)getActivity()).presets, nonAssigned, true);
    		if (nonAssignedPreset==null) {
    			// no point in continuing
    			break;
    		}
    		addSecondaryPreset(nonAssignedPreset);
    		nonAssigned = addPresetsToTags(nonAssignedPreset, (LinkedHashMap<String, String>) nonAssigned);
    		nonAssignedCount = nonAssigned.size();
    	}
    	
    	// update hints
    	for (int i = 0; i < rowLayout.getChildCount()-1; i++) { // don't update empty row at end
    		setHint((TagEditRow) rowLayout.getChildAt(i));
    	}
	}
	
	private void setHint(TagEditRow row) {
		String aTagKey = row.getKey();
		PresetItem preset = getPreset(aTagKey);
		if (preset != null && aTagKey != null && !aTagKey.equals("")) { // set hints even if value isen't empty
			String hint = preset.getHint(aTagKey);
			if (hint != null) { 
				row.valueEdit.setHint(hint);
			} else if (preset.getKeyType(aTagKey) != PresetKeyType.TEXT) {
				row.valueEdit.setHint(R.string.tag_autocomplete_value_hint);
			} else {
				row.valueEdit.setHint(R.string.tag_value_hint);
			}
			if (row.getValue().length() == 0) {
				String defaultValue = preset.getDefault(aTagKey);
				if (defaultValue != null) { //
					row.valueEdit.setText(defaultValue);
				} 
			}
			if (!row.same) {
				row.valueEdit.setHint(R.string.tag_multi_value_hint); // overwrite the above
			}
		}
	}
	
	/**
	 * Don't call with null preset
	 * @param preset
	 * @param tags
	 * @return
	 */
	Map<String, String> addPresetsToTags(PresetItem preset, LinkedHashMap<String, String> tags) {
		LinkedHashMap<String,String> leftOvers = new LinkedHashMap<String,String>();
		if (preset!=null) {
			List<PresetItem> linkedPresetList = preset.getLinkedPresets();
			for (String key:tags.keySet()) {
				if ( preset.hasKeyValue(key, tags.get(key))) {
					storePreset(key, preset);
				} else {
					boolean found = false;
					if (linkedPresetList != null){
						for (PresetItem linkedPreset:linkedPresetList) {
							if (linkedPreset.hasKeyValue(key, tags.get(key))) {
								storePreset(key, linkedPreset);
								found = true;
								break;
							}
						}
					}
					if (!found) {
						leftOvers.put(key, tags.get(key));
					}
				}
			}
		} else {
			Log.e(DEBUG_TAG,"addPresetsToTags called with null preset");
		}
		return leftOvers;
	}
	
	PresetItem getPreset(String key) {
		return tags2Preset.get(key);
	}
	
	void storePreset(String key, PresetItem preset) {
		tags2Preset.put(key,preset);
	}
	
	void clearPresets() {
		tags2Preset.clear();
	}
	
	@Override
	public Map<String,PresetItem> getAllPresets() {
		return tags2Preset;
	}
	
	void addSecondaryPreset(PresetItem nonAssignedPreset) {
		secondaryPresets.add(nonAssignedPreset);
	}
	
	void clearSecondaryPresets() {
		secondaryPresets.clear();
	}
	
	@Override
	public List getSecondaryPresets() {
		return secondaryPresets;
	}
	
	/**
	 * Edits may change the best fitting preset
	 */
	void updateAutocompletePresetItem() {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		if (rowLayout != null) {
			updateAutocompletePresetItem(rowLayout);
		} else {
			Log.d(DEBUG_TAG,"updateAutocompletePresetItem rowLayout null");
		}
	}
	
	@Override
	public PresetItem getBestPreset() {
		return autocompletePresetItem;
	}
	
	@Override
	public void predictAddressTags(boolean allowBlanks) {
		loadEdits(Address.predictAddressTags(getType(),getOsmId(),
				((StreetTagValueAutocompletionAdapter)nameAdapters.getStreetNameAutocompleteAdapter(null)).getElementSearch(), 
				getKeyValueMap(allowBlanks), Address.DEFAULT_HYSTERESIS));
		updateAutocompletePresetItem();
	}
	
	protected ArrayAdapter<String> getKeyAutocompleteAdapter(PresetItem preset, LinearLayout rowLayout, AutoCompleteTextView keyEdit) {
		// Use a set to prevent duplicate keys appearing
		Set<String> keys = new HashSet<String>();
		
		if (preset == null && ((PropertyEditor)getActivity()).presets != null) {
			updateAutocompletePresetItem(rowLayout);
		}
		
		if (preset != null) {
			keys.addAll(preset.getFixedTags().keySet());
			keys.addAll(preset.getRecommendedTags().keySet());
			keys.addAll(preset.getOptionalTags().keySet());
		}
		
		if (((PropertyEditor)getActivity()).presets != null && elements[0] != null) { // FIXME multiselect
			keys.addAll(Preset.getAutocompleteKeys(((PropertyEditor)getActivity()).presets, elements[0].getType())); // FIXME multiselect
		}
		
		keys.removeAll(getUsedKeys(rowLayout,keyEdit));
		
		List<String> result = new ArrayList<String>(keys);
		Collections.sort(result);
		return new ArrayAdapter<String>(getActivity(), R.layout.autocomplete_row, result);
	}
	
	/**
	 * Return true if the edited object has an address or is a "highway"
	 * @param key
	 * @param usedKeys
	 * @return
	 */
	public static boolean isStreetName(String key, Set<String> usedKeys) {
		return (Tags.KEY_ADDR_STREET.equalsIgnoreCase(key) ||
				(Tags.KEY_NAME.equalsIgnoreCase(key) && usedKeys.contains(Tags.KEY_HIGHWAY)));
	}
	
	/**
	 * Return true if the edited object has an address or is a "place"
	 * @param key
	 * @param usedKeys
	 * @return
	 */
	public static boolean isPlaceName(String key, Set<String> usedKeys) {
		return (Tags.KEY_ADDR_PLACE.equalsIgnoreCase(key) ||
			(Tags.KEY_NAME.equalsIgnoreCase(key) && usedKeys.contains(Tags.KEY_PLACE)));
	}
	
	/**
	 * Return true if the edited object could have a name in the name index
	 * @param usedKeys
	 * @return
	 */
	public static boolean useNameSuggestions(Set<String> usedKeys) {
		return !(usedKeys.contains(Tags.KEY_HIGHWAY) || usedKeys.contains(Tags.KEY_WATERWAY) 
			|| usedKeys.contains(Tags.KEY_LANDUSE) || usedKeys.contains(Tags.KEY_NATURAL) || usedKeys.contains(Tags.KEY_RAILWAY));
	}
	
	protected ArrayAdapter<?> getValueAutocompleteAdapter(PresetItem preset, LinearLayout rowLayout, TagEditRow row) {
		ArrayAdapter<?> adapter = null;
		String key = row.getKey();
		if (key != null && key.length() > 0) {
			HashSet<String> usedKeys = (HashSet<String>) getUsedKeys(rowLayout,null);

			boolean hasTagValues = row.tagValues != null && row.tagValues.size() > 1;
			if (isStreetName(key, usedKeys)) {
				adapter = nameAdapters.getStreetNameAutocompleteAdapter(hasTagValues ? row.tagValues : null);
			} else if (isPlaceName(key, usedKeys)) {
				adapter = nameAdapters.getPlaceNameAutocompleteAdapter(hasTagValues ? row.tagValues : null);
			} else if (key.equals(Tags.KEY_NAME) && (names != null) && useNameSuggestions(usedKeys)) {
				Log.d(DEBUG_TAG,"generate suggestions for name from name suggestion index");
				ArrayList<NameAndTags> values = (ArrayList<NameAndTags>) names.getNames(new TreeMap<String,String>(getKeyValueMapSingle(rowLayout,true))); // FIXME
				if (values != null && !values.isEmpty()) {
					ArrayList<NameAndTags> result = values;
					Collections.sort(result);
					adapter = new ArrayAdapter<NameAndTags>(getActivity(), R.layout.autocomplete_row, result);
				}
			} else {
				HashMap<String, Integer> counter = new HashMap<String, Integer>();
				ArrayAdapter<ValueWithCount> adapter2 = new ArrayAdapter<ValueWithCount>(getActivity(), R.layout.autocomplete_row);
				if (hasTagValues) {
					for(String t:row.tagValues) {
						if (t.equals("")) {
		        			continue;
		        		}
						if (counter.containsKey(t)) {
							counter.put(t, Integer.valueOf(counter.get(t).intValue()+1));
						} else {
							counter.put(t, Integer.valueOf(1));
						}
					}
					ArrayList<String> keys = new ArrayList<String>(counter.keySet());
					Collections.sort(keys);
					for (String t:keys) {
						ValueWithCount v = new ValueWithCount(t,counter.get(t).intValue()); // FIXME determine description in some way
						adapter2.add(v);
					}
				}
				if (preset != null) { // note this will use the last applied preset which may be wrong FIXME
					Collection<StringWithDescription> values = preset.getAutocompleteValues(key);
					Log.d(DEBUG_TAG,"setting autocomplete adapter for values " + values + " based on " + preset.getName());
					if (values != null && !values.isEmpty()) {
						ArrayList<StringWithDescription> result = new ArrayList<StringWithDescription>(values);
						if (preset.sortIt(key)) {
							Collections.sort(result);
						}
						for (StringWithDescription s:result) {
							if (counter != null && counter.containsKey(s.getValue())) {
								continue; // skip stuff that is already listed
							}
							adapter2.add(new ValueWithCount(s.getValue(), s.getDescription()));
						}
						Log.d(DEBUG_TAG,"key " + key + " type " + preset.getKeyType(key));
					} else if (preset.isFixedTag(key)) {
						for (StringWithDescription s:Preset.getAutocompleteValues(((PropertyEditor)getActivity()).presets,elements[0].getType(), key)) {
							adapter2.add(new ValueWithCount(s.getValue(), s.getDescription()));
						}	
					}
				} else if (((PropertyEditor)getActivity()).presets != null && elements[0] != null) { 
					Log.d(DEBUG_TAG,"generate suggestions for >" + key + "< from presets"); // only do this if there is no other source of suggestions
					for (StringWithDescription s:Preset.getAutocompleteValues(((PropertyEditor)getActivity()).presets,elements[0].getType(), key)) {
						adapter2.add(new ValueWithCount(s.getValue(), s.getDescription()));
					}		
				} else if (adapter2.getCount() == 0) {
					// FIXME shouldn't happen but seems to
					Log.d(DEBUG_TAG,"no suggestions for values for >" + key + "<");
				}
				if (adapter2.getCount() > 0) {
					return adapter2;
				}
			}
		}
		return adapter;
	}
	
	/**
	 * Insert a new row with one key and one value to edit.
	 * @param aTagKey the key-value to start with
	 * @param aTagValue the value to start with.
	 * @param position the position where this should be inserted. set to -1 to insert at end, or 0 to insert at beginning.
	 * @returns The new TagEditRow.
	 */
	protected TagEditRow insertNewEdit(final LinearLayout rowLayout, final String aTagKey, final ArrayList<String> tagValues, final int position) {
		final TagEditRow row = (TagEditRow)inflater.inflate(R.layout.tag_edit_row, null);
	
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) { // stop Hint from wrapping
			row.valueEdit.setEllipsize(TruncateAt.END);
		}
		
		boolean same = true;
		if (tagValues.size() > 1) {
			for (int i=1;i<tagValues.size();i++) {
				if (!tagValues.get(i-1).equals(tagValues.get(i))) {
					same = false;
					break;
				}
			}
		}
		row.setValues(aTagKey, tagValues, same);

		// If the user selects addr:street from the menu, auto-fill a suggestion
		row.keyEdit.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (Tags.KEY_ADDR_STREET.equals(parent.getItemAtPosition(position)) &&
						row.getValue().length() == 0) {
					ArrayAdapter<ValueWithCount> adapter = nameAdapters.getStreetNameAutocompleteAdapter(tagValues);
					if (adapter != null && adapter.getCount() > 0) {
						row.valueEdit.setText(adapter.getItem(0).getValue());
					}
				} else if (Tags.KEY_ADDR_PLACE.equals(parent.getItemAtPosition(position)) &&
						row.getValue().length() == 0) {
					ArrayAdapter<ValueWithCount> adapter = nameAdapters.getPlaceNameAutocompleteAdapter(tagValues);
					if (adapter != null && adapter.getCount() > 0) {
						row.valueEdit.setText(adapter.getItem(0).getValue());
					}
				} else{
					if (autocompletePresetItem != null) {
						String hint = autocompletePresetItem.getHint(parent.getItemAtPosition(position).toString());
						if (hint != null) { //
							row.valueEdit.setHint(hint);
						} else if (autocompletePresetItem.getRecommendedTags().keySet().size() > 0 || autocompletePresetItem.getOptionalTags().keySet().size() > 0) {
							row.valueEdit.setHint(R.string.tag_value_hint);
						}
						if (row.getValue().length() == 0) {
							String defaultValue = autocompletePresetItem.getDefault(parent.getItemAtPosition(position).toString());
							if (defaultValue != null) { //
								row.valueEdit.setText(defaultValue);
							} 
						}
					}
					// set focus on value
					row.valueEdit.requestFocus();
				}
			}
		});
		row.keyEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			String originalKey;
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				PresetItem preset = getPreset(aTagKey);
				if (hasFocus) {
					// Log.d(DEBUG_TAG,"got focus");
					originalKey = row.getKey();
					row.keyEdit.setAdapter(getKeyAutocompleteAdapter(preset, rowLayout, row.keyEdit));
					if (PropertyEditor.running && row.getKey().length() == 0) row.keyEdit.showDropDown();
				} else {
					String newKey = row.getValue();
					if (!newKey.equals(originalKey)) { // our preset may have changed re-calc
						updateAutocompletePresetItem(rowLayout);
					} 
				}
			}
		});
		row.valueEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			String originalValue;
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					originalValue = row.getValue();
					PresetItem preset = getPreset(row.getKey());
					row.valueEdit.setAdapter(getValueAutocompleteAdapter(preset, rowLayout, row));
					if (preset != null && preset.getKeyType(row.getKey())==PresetKeyType.MULTISELECT) { 
						// FIXME this should be somewhere better obvious since it creates a non obvious side effect
						row.valueEdit.setTokenizer(new CustomAutoCompleteTextView.SingleCharTokenizer(LIST_SEPARATOR));
					}
					if (PropertyEditor.running) {
						if (row.valueEdit.getText().length() == 0) row.valueEdit.showDropDown();
//						try { // hack to display numeric keyboard for numeric tag values
//							  // unluckily there is then no way to get an alpha-numeric keyboard
//							int number = Integer.parseInt(valueEdit.getText().toString());
//							valueEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
//						} catch (NumberFormatException nfe) {
//							// do nothing
//						}
					}
				} else {
					// our preset may have changed re-calc
					String newValue = row.getValue();
					if (!newValue.equals(originalValue)) {
						// Log.d(DEBUG_TAG,"lost focus");
						// potentially we should update tagValues here
						updateAutocompletePresetItem(rowLayout);
					} 
				}
			}
		});
		
		// This TextWatcher reacts to previously empty cells being filled to add additional rows where needed
		TextWatcher emptyWatcher = new TextWatcher() {
			private boolean wasEmpty;
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// nop
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				wasEmpty = row.isEmpty();
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				if (wasEmpty == (s.length() > 0)) {
					// changed from empty to not-empty or vice versa
					row.enableCheckBox();
					ensureEmptyRow(rowLayout);
				}
			}
		};
		row.keyEdit.addTextChangedListener(emptyWatcher);
		row.valueEdit.addTextChangedListener(emptyWatcher);
		
		row.valueEdit.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.d("TagEdit","onItemClicked value");
				Object o = parent.getItemAtPosition(position);
				if (o instanceof Names.NameAndTags) {
					row.valueEdit.setText2(((NameAndTags)o).getName());
					applyTagSuggestions(((NameAndTags)o).getTags());
				} else if (o instanceof ValueWithCount) {
					row.valueEdit.setText2(((ValueWithCount)o).getValue());
				} else if (o instanceof StringWithDescription) {
					row.valueEdit.setText2(((StringWithDescription)o).getValue());
				} else if (o instanceof String) {
					row.valueEdit.setText2((String)o);
				}
			}
		});
		
		row.selected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (!row.isEmpty()) {
					if (isChecked) {
						tagSelected();
					} else {
						deselectRow();
					}
				}
				if (row.isEmpty()) {
					row.deselect();
				}
			}
		});
		
		if (row.isEmpty()) {
			row.disableCheckBox();
		}
		rowLayout.addView(row, (position == -1) ? rowLayout.getChildCount() : position);
		//

		return row;
	}
	
	/**
	 * A row representing an editable tag, consisting of edits for key and value, labels and a delete button.
	 * Needs to be static, otherwise the inflater will not find it.
	 * @author Jan
	 */
	public static class TagEditRow extends LinearLayout implements
			SelectedRowsActionModeCallback.Row {
		
		private PropertyEditor owner;
		private AutoCompleteTextView keyEdit;
		private CustomAutoCompleteTextView valueEdit;
		private CheckBox selected;
		private ArrayList<String> tagValues;
		private boolean same = true;
				
		public TagEditRow(Context context) {
			super(context);
			owner = (PropertyEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
		public TagEditRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			owner = (PropertyEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
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
			
			valueEdit = (CustomAutoCompleteTextView)findViewById(R.id.editValue);
			valueEdit.setOnKeyListener(owner.myKeyListener);
			
			selected = (CheckBox) findViewById(R.id.tagSelected);

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
		}
	
		@Override
		public void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			// Log.d(DEBUG_TAG, "onSizeChanged");

			if (w == 0 && h == 0) {
				return;
			}
			// Log.d(DEBUG_TAG,"w=" + w +" h="+h);
			// this is not really satisfactory
			keyEdit.setDropDownAnchor(valueEdit.getId());
//			keyEdit.setDropDownVerticalOffset(-h);
//			valueEdit.setDropDownVerticalOffset(-h);
			valueEdit.setParentWidth(w);
			//			
		}

		/**
		 * Sets key and value values
		 * @param aTagKey the key value to set
		 * @param aTagValue the value value to set
		 * @return the TagEditRow object for convenience
		 */
		public TagEditRow setValues(String aTagKey, ArrayList<String> tagValues, boolean same) {
			Log.d(DEBUG_TAG, "key " + aTagKey + " value " + tagValues);
			keyEdit.setText(aTagKey);
			this.tagValues = tagValues;
			this.same = same;
			if (same) {
				if (tagValues != null && tagValues.size() > 0) {
					valueEdit.setText(tagValues.get(0));
				} else {
					valueEdit.setText("");
				}
			} else {
				valueEdit.setHint(R.string.tag_multi_value_hint);
			}
			return this;
		}
		
		public String getKey() {
			return keyEdit.getText().toString();
		}
		
		public String getValue() { // FIXME check if returning the textedit value is actually ok
			return valueEdit.getText().toString();
		}
		
		/**
		 * Deletes this row
		 */
		@Override
		public void delete() { //FIXME the references to owner.tagEditorFragemnt are likely suspect
			deleteRow((LinearLayout)owner.tagEditorFragment.getOurView());
		}
		
		/**
		 * Deletes this row
		 */
		public void deleteRow(LinearLayout rowLayout) { //FIXME the references to owner.tagEditorFragemnt are likely suspect
			View cf = owner.getCurrentFocus();
			if (cf == keyEdit || cf == valueEdit) {
				// about to delete the row that has focus!
				// try to move the focus to the next row or failing that to the previous row
				int current = owner.tagEditorFragment.rowIndex(this);
				if (!owner.tagEditorFragment.focusRow(current + 1)) owner.tagEditorFragment.focusRow(current - 1);
			}
			rowLayout.removeView(this);
			if (isEmpty() && owner.tagEditorFragment != null) {
				owner.tagEditorFragment.ensureEmptyRow();
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
		
		// return the status of the checkbox
		@Override
		public boolean isSelected() {
			return selected.isChecked();
		}

		@Override
		public void deselect() {
			selected.setChecked(false);
		}
		
		public void disableCheckBox() {
			selected.setEnabled(false);
		}
		
		protected void enableCheckBox() {
			selected.setEnabled(true);
		}
	}
	
	/**
	 * Appy tags from name suggestion list, ask if overwriting
	 * @param tags
	 */
	private void applyTagSuggestions(Names.TagMap tags) {
		final LinkedHashMap<String, ArrayList<String>> currentValues = getKeyValueMap(true);
		
		boolean replacedValue = false;	
		
		// Fixed tags, always have a value. We overwrite mercilessly.
		for (Entry<String, String> tag : tags.entrySet()) {
			ArrayList<String> oldValue = currentValues.put(tag.getKey(), Util.getArrayList(tag.getValue()));
			if (oldValue != null && oldValue.size() > 0 && !oldValue.contains(tag.getValue())) replacedValue = true;
		}
		if (replacedValue) {
			Builder dialog = new AlertDialog.Builder(getActivity());
			dialog.setTitle(R.string.tag_editor_name_suggestion);
			dialog.setMessage(R.string.tag_editor_name_suggestion_overwrite_message);
			dialog.setPositiveButton(R.string.replace, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					loadEdits(currentValues);// FIXME
				}
			});
			dialog.setNegativeButton(R.string.cancel, null);
			dialog.create().show();
		} else
			loadEdits(currentValues);// FIXME
		
// TODO while applying presets automatically seems like a good idea, it needs some further thought
		if (prefs.enableAutoPreset()) {
			PresetItem p = Preset.findBestMatch(((PropertyEditor)getActivity()).presets,getKeyValueMapSingle(false)); // FIXME
			if (p!=null) {
				applyPreset(p, false, false); 
			}
		}
	}
	
	protected synchronized void tagSelected() {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		if (tagSelectedActionModeCallback == null) {
			tagSelectedActionModeCallback = new TagSelectedActionModeCallback(this, rowLayout);
			((SherlockFragmentActivity)getActivity()).startActionMode(tagSelectedActionModeCallback);
		}	
	}
	
	@Override
	public synchronized void deselectRow() {
		if (tagSelectedActionModeCallback != null) {
			if (tagSelectedActionModeCallback.rowsDeselected(false)) {
				tagSelectedActionModeCallback = null;
			}
		}	
	}
	
	
	protected void selectAllTags() {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		if (loaded) {
			int i = rowLayout.getChildCount();
			while (--i >= 0) { 
				TagEditRow row = (TagEditRow)rowLayout.getChildAt(i);
				if (row.selected.isEnabled()) {
					row.selected.setChecked(true);
				}
			}
		}
	}
	
	protected void deselectAllTags() {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		if (loaded) {
			int i = rowLayout.getChildCount();
			while (--i >= 0) { 
				TagEditRow row = (TagEditRow)rowLayout.getChildAt(i);
				if (row.selected.isEnabled()) {
					row.selected.setChecked(false);
				}
			}	
		}	
	}
	
	/**
	 * Ensures that at least one empty row exists (creating one if needed)
	 * @return the first empty row found (or the one created), or null if loading was not finished (loaded == false)
	 */
	private TagEditRow ensureEmptyRow() {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		return ensureEmptyRow(rowLayout);
	}
	
	/**
	 * Ensures that at least one empty row exists (creating one if needed)
	 * @return the first empty row found (or the one created), or null if loading was not finished (loaded == false)
	 */
	private TagEditRow ensureEmptyRow(LinearLayout rowLayout) {
		TagEditRow ret = null;
		if (loaded) {
			int i = rowLayout.getChildCount();
			while (--i >= 0) { 
				TagEditRow row = (TagEditRow)rowLayout.getChildAt(i);
				boolean isEmpty = row.isEmpty();
				if (ret == null) ret = isEmpty ? row : insertNewEdit(rowLayout,"", new ArrayList<String>(), -1);
				else if (isEmpty) row.deleteRow(rowLayout);
			}
			if (ret == null) ret = insertNewEdit(rowLayout,"", new ArrayList<String>(), -1);
		}
		return ret;
	}
	
	/**
	 * Focus on the value field of the first tag with non empty key and empty value 
	 * @param key
	 * @return
	 */
	private boolean focusOnEmptyValue() {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		return focusOnEmptyValue(rowLayout);
	}
	
	private boolean focusOnEmptyValue(LinearLayout rowLayout) {		
		boolean found = false;
		for (int i = 0; i < rowLayout.getChildCount(); i++) {
			TagEditRow ter = (TagEditRow)rowLayout.getChildAt(i);
			if (ter.getKey() != null && !ter.getKey().equals("") && ter.getValue().equals("")) {
				focusRowValue(rowLayout, rowIndex(rowLayout,ter));
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
		LinearLayout rowLayout = (LinearLayout) getOurView();
		TagEditRow row = (TagEditRow)rowLayout.getChildAt(index);
		return row != null && row.keyEdit.requestFocus();
	}
	

	/**
	 * Move the focus to the value field of the specified row.
	 * @param index The index of the row to move to, counting from 0.
	 * @return true if the row was successfully focussed, false otherwise.
	 */
	private boolean focusRowValue(LinearLayout rowLayout, int index) {
		TagEditRow row = (TagEditRow)rowLayout.getChildAt(index);
		return row != null && row.valueEdit.requestFocus();
	}


	/**
	 * Given a tag edit row, calculate its position.
	 * @param row The tag edit row to find.
	 * @return The position counting from 0 of the given row, or -1 if it couldn't be found.
	 */
	private int rowIndex(TagEditRow row) {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		return rowIndex(rowLayout, row);
	}

	private int rowIndex(LinearLayout rowLayout,TagEditRow row) {		
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
	private boolean focusOnValue(LinearLayout rowLayout, String key) {
		boolean found = false;
		for (int i = rowLayout.getChildCount() - 1; i >= 0; --i) {
			TagEditRow ter = (TagEditRow)rowLayout.getChildAt(i);
			if (ter.getKey().equals(key)) {
				focusRowValue(rowLayout, rowIndex(rowLayout, ter));
				found = true;
				break;
			}
		}
		return found;
	}
		
	/**
	 * Applies a preset (e.g. selected from the dialog or MRU), i.e. adds the tags from the preset to the current tag set
	 * @param item the preset to apply
	 */
	void applyPreset(PresetItem item) {
		applyPreset(item, false, true);
	}
	
	/**
	 * Applies a preset (e.g. selected from the dialog or MRU), i.e. adds the tags from the preset to the current tag set
	 * @param item the preset to apply
	 * @param addOptional TODO
	 */
	void applyPreset(PresetItem item, boolean addOptional, boolean addToMRU) {
		LinkedHashMap<String, ArrayList<String>> currentValues = getKeyValueMap(true);
		
		boolean replacedValue = false;	
		
		// remove everything that doesn't have a value
		// given that these are likely leftovers from a previous preset
		Set<String> keySet = new HashSet<String>(currentValues.keySet()); // shallow copy
		for (String key:keySet) {
			ArrayList<String>list = currentValues.get(key);
			if (list == null || list.size() == 0) {
				currentValues.remove(key);
			}
		}
		
		// Fixed tags, always have a value. We overwrite mercilessly.
		for (Entry<String, StringWithDescription> tag : item.getFixedTags().entrySet()) {
			String v = tag.getValue().getValue();
			ArrayList<String> oldValue = currentValues.put(tag.getKey(), Util.getArrayList(v));
			if (oldValue != null && oldValue.size() > 0 && !oldValue.contains(v)) {
				replacedValue = true;
			}
		}
		
		// Recommended tags, no fixed value is given. We add only those that do not already exist.
		for (Entry<String, StringWithDescription[]> tag : item.getRecommendedTags().entrySet()) {
			if (!currentValues.containsKey(tag.getKey())) {
				currentValues.put(tag.getKey(), Util.getArrayList(""));
			}
		}
		
		// Optional tags, no fixed value is given. We add only those that do not already exist.
		if (addOptional) {
			for (Entry<String, StringWithDescription[]> tag : item.getOptionalTags().entrySet()) {
				if (!currentValues.containsKey(tag.getKey())) {
					currentValues.put(tag.getKey(), Util.getArrayList(""));
				}
			}
		}

		loadEdits(currentValues);
		if (replacedValue) Toast.makeText(getActivity(), R.string.toast_preset_overwrote_tags, Toast.LENGTH_LONG).show();
		
		// redeterming best preset
		updateAutocompletePresetItem();
		
		//
		if (addToMRU) {
			Preset[] presets = Application.getCurrentPresets(getActivity());
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
	
	protected void recreateRecentPresetView() {
		Log.d(DEBUG_TAG,"Updating MRU prests");
		FragmentManager fm = getChildFragmentManager();
		Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
		if (recentPresetsFragment != null) {
			((RecentPresetsFragment)recentPresetsFragment).recreateRecentPresetView();
		}
	}
	
	
	/**
	 * Merge a set of tags in to the current ones
	 * @param newTags
	 * @param replace // FIXME
	 */
	private void mergeTags(Map<String, String> newTags, boolean replace) {
		LinkedHashMap<String, ArrayList<String>> currentValues = getKeyValueMap(true);
		
		boolean replacedValue = false;	
		
		// Fixed tags, always have a value. We overwrite mercilessly.
		for (Entry<String, String> tag : newTags.entrySet()) {
			ArrayList<String> oldValue = currentValues.put(tag.getKey(), Util.getArrayList(tag.getValue()));
			if (oldValue != null && oldValue.size() > 0 && !oldValue.contains(tag.getValue())) {
				replacedValue = true;
			}
		}
		
		loadEdits(currentValues);
		// FIXME text if (replacedValue) Toast.makeText(getActivity(), R.string.toast_preset_overwrote_tags, Toast.LENGTH_LONG).show();
		focusOnEmptyValue();
	}
	
	/**
	 * Merge a set of tags in to the current ones, with potentially empty keys
	 * @param newTags
	 * @param replace // FIXME
	 */
	private void mergeTags(ArrayList<KeyValue> newTags, boolean replace) {
		LinkedHashMap<String, ArrayList<String>> currentValues = getKeyValueMap(true);
		HashMap<String,KeyValue> keyIndex = new HashMap<String, KeyValue>(); // needed for de-duping
		
		ArrayList<KeyValue> keysAndValues = new ArrayList<KeyValue>();
		for (String key:currentValues.keySet()) {
			KeyValue keyValue = new KeyValue(key, currentValues.get(key));
			keysAndValues.add(keyValue);
			keyIndex.put(key, keyValue);
		}
		
		boolean replacedValue = false;	
		
		// 
		for (KeyValue tag : newTags) {
			KeyValue keyValue = keyIndex.get(tag.getKey());
			if (keyValue != null) { // exists
				keyValue.setValue(tag.getValue());
				replacedValue = true;
			} else {
				keysAndValues.add(new KeyValue(tag.getKey(),tag.getValue()));
			}
		}
		
		// this code needs to be duplicated because we can't use a map here
		LinearLayout rowLayout = (LinearLayout) getOurView();
		loaded = false;
		rowLayout.removeAllViews();
		for (KeyValue keyValue:keysAndValues) {
			insertNewEdit(rowLayout, keyValue.getKey(), keyValue.getValues(), -1);
		}
		loaded = true;
		ensureEmptyRow(rowLayout);
		
		// FIXME text if (replacedValue) Toast.makeText(getActivity(), R.string.toast_preset_overwrote_tags, Toast.LENGTH_LONG).show();
		focusOnEmptyValue();
	}
	
	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		// final MenuInflater inflater = getSupportMenuInflater();
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.tag_menu, menu);
		menu.findItem(R.id.tag_menu_mapfeatures).setEnabled(NetworkStatus.isConnected(getActivity()));
	}
	
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		// disable address tagging for stuff that won't have an address
		// menu.findItem(R.id.tag_menu_address).setVisible(!type.equals(Way.NAME) || element.hasTagKey(Tags.KEY_BUILDING));
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			((PropertyEditor)getActivity()).sendResultAndFinish();
			return true;
		case R.id.tag_menu_address:
			predictAddressTags(false);
			return true;
		case R.id.tag_menu_sourcesurvey:
			doSourceSurvey();
			return true;
		case R.id.tag_menu_apply_preset:
			PresetItem pi = Preset.findBestMatch(((PropertyEditor)getActivity()).presets,getKeyValueMapSingle(false)); // FIXME
			if (pi!=null) {
				applyPreset(pi, true, false); 
			}
			return true;
		case R.id.tag_menu_paste:
			doPaste(true);
			return true;
		case R.id.tag_menu_paste_from_clipboard:
			ArrayList<KeyValue> paste = ClipboardUtils.getKeyValues(getActivity());
			if (paste != null) {
				mergeTags(paste, false);
			}
			return true;
		case R.id.tag_menu_revert:
			doRevert();
			return true;
		case R.id.tag_menu_mapfeatures:
			startActivity(Preset.getMapFeaturesIntent(getActivity(),getBestPreset()));
			return true;
		case R.id.tag_menu_resetMRU:
			for (Preset p:((PropertyEditor)getActivity()).presets)
				p.resetRecentlyUsed();
			((PropertyEditor)getActivity()).recreateRecentPresetView();
			return true;
		case R.id.tag_menu_reset_address_prediction:
			// simply overwrite with an empty file
			Address.resetLastAddresses();
			return true;
		case R.id.tag_menu_help:
			HelpViewer.start(getActivity(), R.string.help_propertyeditor);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Collect all key-value pairs into a LinkedHashMap<String,String>
	 * 
	 * @param allowBlanks If true, includes key-value pairs where one or the other is blank.
	 * @return The LinkedHashMap<String,String> of key-value pairs.
	 */
	LinkedHashMap<String,ArrayList<String>> getKeyValueMap(final boolean allowBlanks) {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		return getKeyValueMap(rowLayout, allowBlanks);
	}	
		
	LinkedHashMap<String,ArrayList<String>> getKeyValueMap(LinearLayout rowLayout, final boolean allowBlanks) {
		
		final LinkedHashMap<String,ArrayList<String>> tags = new LinkedHashMap<String, ArrayList<String>>();
		
		if (rowLayout == null && savedTags != null) {		
			return savedTags;
		}
		
		if (rowLayout != null) {
			processKeyValues(rowLayout, new KeyValueHandler() {
				@Override
				public void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final ArrayList<String> tagValues) {
					String key = keyEdit.getText().toString().trim();
					String value = valueEdit.getText().toString().trim();
					boolean valueBlank = "".equals(value);
					boolean bothBlank = "".equals(key) && valueBlank;
					boolean neitherBlank = !"".equals(key) && !valueBlank;
					if (!bothBlank) {
						// both blank is never acceptable
						if (neitherBlank || allowBlanks || (valueBlank && tagValues != null && tagValues.size()>0)) {
							if (valueBlank) {
								tags.put(key, tagValues.size()==1?Util.getArrayList(""):tagValues);
							} else {
								tags.put(key, Util.getArrayList(value));
							}
						}
					}
				}
			});
		} else {
			Log.e(DEBUG_TAG,"rowLayout null in getKeyValueMapSingle");
		}
//		for (String key:tags.keySet()) {
//			Log.d(DEBUG_TAG,"getKeyValueMap Key " + key + " " + tags.get(key));
//		}
		return tags;
	}	
	
	/**
	 * Version of above that ignores multiple values
	 * @param allowBlanks
	 * @return
	 */
	public LinkedHashMap<String,String> getKeyValueMapSingle(final boolean allowBlanks) {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		return getKeyValueMapSingle(rowLayout, allowBlanks);
	}	
		
	LinkedHashMap<String,String> getKeyValueMapSingle(LinearLayout rowLayout, final boolean allowBlanks) {
		
		final LinkedHashMap<String,String> tags = new LinkedHashMap<String, String>();
		if (rowLayout == null && savedTags != null) {
			for (Entry<String,ArrayList<String>>entry:savedTags.entrySet()) {
				String key = entry.getKey().trim();
				ArrayList<String> tagValues = entry.getValue();
				String value = tagValues != null && tagValues.size() > 0 ? (tagValues.get(0)!=null?tagValues.get(0):""):"";			
				boolean valueBlank = "".equals(value);
				boolean bothBlank = "".equals(key) && valueBlank;
				boolean neitherBlank = !"".equals(key) && !valueBlank;
				if (!bothBlank) {
					// both blank is never acceptable
					if (neitherBlank || allowBlanks || valueBlank) {
						if (valueBlank) {
							tags.put(key, tagValues==null || tagValues.size()==1?"":tagValues.get(0)); // FIXME if multi-select
						} else {
							tags.put(key, value);
						}
					}
				}
			}
		}
		if (rowLayout != null) {
			processKeyValues(rowLayout, new KeyValueHandler() {
				@Override
				public void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final ArrayList<String> tagValues) {
					String key = keyEdit.getText().toString().trim();
					String value = valueEdit.getText().toString().trim();
					boolean valueBlank = "".equals(value);
					boolean bothBlank = "".equals(key) && valueBlank;
					boolean neitherBlank = !"".equals(key) && !valueBlank;
					if (!bothBlank) {
						// both blank is never acceptable
						boolean hasValues =  tagValues != null && tagValues.size()>0;
						if (neitherBlank || allowBlanks || (valueBlank && hasValues)) {
							if (valueBlank) {
								tags.put(key, "");
							} else {
								tags.put(key, value);
							}
						}
					}
				}
			});
		} else {
			Log.e(DEBUG_TAG,"rowLayout null in getKeyValueMapSingle");
		}
		return tags;
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
	
	private void doSourceSurvey() { // FIXME
		// determine the key (if any) that has the current focus in the key or its value
		final String[] focusedKey = new String[]{null}; // array to work around unsettable final
		processKeyValues(new KeyValueHandler() {
			@Override
			public void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final ArrayList<String> tagValues) {
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
			public void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final ArrayList<String> tagValues) {
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
			ArrayList<String> v = new ArrayList<String>();
			v.add(Tags.VALUE_SURVEY);
			insertNewEdit((LinearLayout) getOurView(),sourceKey, v, -1);
		}
	}
	
	private void doPaste(boolean replace) {
		if (copiedTags != null) {
			mergeTags(copiedTags, replace);
		} else {
			Map<String, String> copied = savingHelper.load(PropertyEditor.COPIED_TAGS_FILE, false);
			if (copied != null) {
				mergeTags(copied, replace);
			}
		}
		updateAutocompletePresetItem();
	}
		
	/**
	 * reload original arguments
	 */
	void doRevert() {
		loadEdits(buildEdits());
		updateAutocompletePresetItem();
	}
	
	/**
	 * @return the OSM ID of the element currently edited by the editor
	 */
	public long getOsmId() { // FIXME
		return osmIds[0];
	}
	
	
	public String getType() {// FIXME
		return types[0];
	}
	
	/**
	 * Get all key values currently in the editor, optionally skipping one field.
	 * @param ignoreEdit optional - if not null, this key field will be skipped,
	 *                              i.e. the key  in it will not be included in the output
	 * @return the set of all (or all but one) keys currently entered in the edit boxes
	 */
	private Set<String> getUsedKeys(LinearLayout rowLayout, final EditText ignoreEdit) {
		final HashSet<String> keys = new HashSet<String>();
		processKeyValues(rowLayout,new KeyValueHandler() {
			@Override
			public void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final ArrayList<String> tagValues) {
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
	
	@Override
	public void deselectHeaderCheckBox() {
		CheckBox headerCheckBox = (CheckBox) getView().findViewById(R.id.header_tag_selected);
		headerCheckBox.setChecked(false);
	}
	
	/**
	 * Return the view we have our rows in and work around some android craziness
	 * @return
	 */
	public View getOurView() {
		// android.support.v4.app.NoSaveStateFrameLayout
		View v =  getView();	
		if (v != null) {
			if ( v.getId() == R.id.edit_row_layout) {
				Log.d(DEBUG_TAG,"got correct view in getView");
				return v;
			} else {
				v = v.findViewById(R.id.edit_row_layout);
				if (v == null) {
					Log.d(DEBUG_TAG,"didn't find R.id.edit_row_layout");
				}  else {
					Log.d(DEBUG_TAG,"Found R.id.edit_row_layout");
				}
				return v;
			}
		} else {
			Log.d(DEBUG_TAG,"got null view in getView");
		}
		return null;
	}
	
	/**
	 * Return tags copied or cut
	 * @return
	 */
	public LinkedHashMap<String,String> getCopiedTags() {
		return (LinkedHashMap<String, String>) copiedTags;
	}

	public void enableRecentPresets() {
		FragmentManager fm = getChildFragmentManager();
		Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
		if (recentPresetsFragment != null) {
			((RecentPresetsFragment)recentPresetsFragment).enable();
		}
	}
	
	public void disableRecentPresets() {
		FragmentManager fm = getChildFragmentManager();
		Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
		if (recentPresetsFragment != null) {
			((RecentPresetsFragment)recentPresetsFragment).disable();
		}
	}

	/**
	 * update the original list of tags to reflect edits
	 * @return
	 */
	public ArrayList<LinkedHashMap<String, String>> getUpdatedTags() {
		@SuppressWarnings("unchecked")
		ArrayList<LinkedHashMap<String,String>> oldTags = (ArrayList<LinkedHashMap<String,String>>)getArguments().getSerializable("tags");
		// make a (nearly) full copy
		ArrayList<LinkedHashMap<String,String>> newTags = new ArrayList<LinkedHashMap<String,String>>();
		for (LinkedHashMap<String,String> map:oldTags) {
			newTags.add(new LinkedHashMap<String, String>(map));
		}
		
		LinkedHashMap<String,ArrayList<String>> edits = getKeyValueMap(true);
		if (edits == null) {
			return oldTags;
		}
		
		for (LinkedHashMap<String,String> map:newTags) {
			for (String key:new TreeSet<String>(map.keySet())) {
				if (edits.containsKey(key)) {
					if (edits.get(key).size()==1) {
						String value = edits.get(key).get(0).trim();
						if (!"".equals(value)) {
							addTagToMap(map, key, value);
						} else {
							map.remove(key); // zap stuff with empty values
						}
					} 
				} else { // key deleted
					map.remove(key);
				}
			}
			// check for new tags
			for (String editsKey:edits.keySet()) {
				if (!map.containsKey(editsKey) && edits.get(editsKey).size()==1) { // zap empty stuff
					String value = edits.get(editsKey).get(0).trim();
					if (!"".equals(value)) {
						addTagToMap(map, editsKey, value);
					}
				}
			}
		}
		return newTags;
	}
	
	@Override
	public void updateSingleValue(String key, String value) {
		LinkedHashMap<String, ArrayList<String>> currentValues = getKeyValueMap(true);
		// Fixed tags, always have a value. We overwrite mercilessly.
		for (Entry<String, ArrayList<String>> tag : currentValues.entrySet()) {
			if (tag.getKey().equals(key)) {
				currentValues.put(tag.getKey(), Util.getArrayList(value));
			}
		}
		loadEdits(currentValues);
		updateAutocompletePresetItem();
	}

	@Override
	public void updateTags(Map<String, String> tags, boolean flush) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void revertTags() {
		doRevert();
	}
	
	@Override
	public void deleteTag(final String key) {
		LinearLayout l = (LinearLayout) getOurView();
		if (l!=null) {
			for (int i = l.getChildCount() - 1; i >= 0; --i) {
				TagEditRow ter = (TagEditRow)l.getChildAt(i);
				if (ter.getKey().equals(key)) {
					ter.delete();
					break;
				}
			}	
		}
	}
	
	/**
	 * Added key-value to a map stripping training list separator
	 * @param map
	 * @param key
	 * @param value
	 */
	private void addTagToMap(Map<String,String>map, String key, String value) {
		if (autocompletePresetItem != null && autocompletePresetItem.getKeyType(key)==PresetKeyType.MULTISELECT) {
			// trim potential trailing separators 
			if (value.endsWith(String.valueOf(LIST_SEPARATOR))) {
				value = value.substring(0, value.length()-1);
			}
		}
		map.put(key, value);
	}
}
