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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
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

import de.blau.android.HelpViewer;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.PlaceTagValueAutocompletionAdapter;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.StreetTagValueAutocompletionAdapter;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.util.ClipboardUtils;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.KeyValue;
import de.blau.android.util.Util;
import de.blau.android.views.OffsettedAutoCompleteTextView;


public class TagEditorFragment extends SherlockFragment {
	
	private static final String DEBUG_TAG = TagEditorFragment.class.getName();
	 
	private SavingHelper<LinkedHashMap<String,String>> savingHelper
				= new SavingHelper<LinkedHashMap<String,String>>();
	
	private StreetTagValueAutocompletionAdapter streetNameAutocompleteAdapter = null;
	
	private PlaceTagValueAutocompletionAdapter placeNameAutocompleteAdapter = null;
	
	static TagSelectedActionModeCallback tagSelectedActionModeCallback = null;
	
	PresetItem autocompletePresetItem = null;
	
	private static Names names = null;
	
	private boolean loaded = false;
	private String[] types;
	private long[] osmIds;
	private Preferences prefs = null;
	private OsmElement[] elements = null;
	
	LayoutInflater inflater = null;

	/**
	 * saves any changed fields on onPause
	 */
	protected LinkedHashMap<String,ArrayList<String>> savedTags = null;
	
	/**
	 * selective copy of tags
	 */
	protected Map<String, String> copiedTags = null;
	
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
//        try {
//            mListener = (OnPresetSelectedListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString() + " must implement OnPresetSelectedListener");
//        }
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
    	ArrayList<LinkedHashMap<String, String>> originalTags = null;

		if (savedInstanceState == null) {
			// No previous state to restore - get the state from the intent
			Log.d(DEBUG_TAG, "Initializing from original arguments");
		} else {
			// Restore activity from saved state
			Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
		}
    	
    	prefs = new Preferences(getActivity());
    	
		if (prefs.getEnableNameSuggestions()) {
			if (names == null) {
				// this should be done async if it takes too long
				names = new Names(getActivity());
				// names.dump2Log();
			}
		} else {
			names = null; // might have been on before, zap now
		}
    	
    	
     	this.inflater = inflater;
     	rowLayout = (ScrollView) inflater.inflate(R.layout.taglist_view, null);
       
     	LinearLayout editRowLayout = (LinearLayout) rowLayout.findViewById(R.id.edit_row_layout);
    		
     	elements = (OsmElement[]) getArguments().getSerializable("elements");
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
			loadEdits(editRowLayout,Address.predictAddressTags(this, getKeyValueMap(editRowLayout,false)));
		}

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
		
		return rowLayout;
	}
    
    /**
     * Build the data structure we use to build the edit display
     * @return
     */
    LinkedHashMap<String,ArrayList<String>> buildEdits() {
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
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	Log.d(DEBUG_TAG, "onSaveInstanceState");
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
	
	/**
	 * Gets an adapter for the autocompletion of street names based on the neighborhood of the edited item.
	 * @param tagValues 
	 * @return
	 */
	protected ArrayAdapter<ValueWithCount> getStreetNameAutocompleteAdapter(ArrayList<String> tagValues) {
		if (Main.getLogic() == null || Main.getLogic().getDelegator() == null) {
			return null;
		}
		if (streetNameAutocompleteAdapter == null) {
			streetNameAutocompleteAdapter =	new StreetTagValueAutocompletionAdapter(getActivity(),
					R.layout.autocomplete_row, Main.getLogic().getDelegator(),
					types[0], osmIds[0], tagValues); // FIXME
		}
		return streetNameAutocompleteAdapter;
	}
	
	/**
	 * Gets an adapter for the autocompletion of place names based on the neighborhood of the edited item.
	 * @return
	 */
	protected ArrayAdapter<ValueWithCount> getPlaceNameAutocompleteAdapter(ArrayList<String> tagValues) {
		if (Main.getLogic() == null || Main.getLogic().getDelegator() == null) {
			return null;
		}
		if (placeNameAutocompleteAdapter == null) {
			placeNameAutocompleteAdapter =	new PlaceTagValueAutocompletionAdapter(getActivity(),
					R.layout.autocomplete_row, Main.getLogic().getDelegator(),
					types[0], osmIds[0], tagValues); // FIXME
		}
		return placeNameAutocompleteAdapter;
	}

	protected ArrayAdapter<String> getKeyAutocompleteAdapter(LinearLayout rowLayout, AutoCompleteTextView keyEdit) {
		// Use a set to prevent duplicate keys appearing
		Set<String> keys = new HashSet<String>();
		
		if (autocompletePresetItem == null && ((PropertyEditor)getActivity()).presets != null) {
			autocompletePresetItem = Preset.findBestMatch(((PropertyEditor)getActivity()).presets, getKeyValueMapSingle(rowLayout,false)); // FIXME
		}
		
		if (autocompletePresetItem != null) {
			keys.addAll(autocompletePresetItem.getTags().keySet());
			keys.addAll(autocompletePresetItem.getRecommendedTags().keySet());
			keys.addAll(autocompletePresetItem.getOptionalTags().keySet());
		}
		
		if (((PropertyEditor)getActivity()).presets != null && elements[0] != null) { // FIXME
			keys.addAll(Preset.getAutocompleteKeys(((PropertyEditor)getActivity()).presets, elements[0].getType())); // FIXME
		}
		
		keys.removeAll(getUsedKeys(rowLayout,keyEdit));
		
		List<String> result = new ArrayList<String>(keys);
		Collections.sort(result);
		return new ArrayAdapter<String>(getActivity(), R.layout.autocomplete_row, result);
	}
	
	protected ArrayAdapter<?> getValueAutocompleteAdapter(LinearLayout rowLayout, TagEditRow row) {
		ArrayAdapter<?> adapter = null;
		String key = row.keyEdit.getText().toString();
		if (key != null && key.length() > 0) {
			HashSet<String> usedKeys = (HashSet<String>) getUsedKeys(rowLayout,null);
			boolean isStreetName = (Tags.KEY_ADDR_STREET.equalsIgnoreCase(key) ||
					(Tags.KEY_NAME.equalsIgnoreCase(key) && usedKeys.contains(Tags.KEY_HIGHWAY)));
			boolean isPlaceName = (Tags.KEY_ADDR_PLACE.equalsIgnoreCase(key) ||
					(Tags.KEY_NAME.equalsIgnoreCase(key) && usedKeys.contains(Tags.KEY_PLACE)));
			boolean noNameSuggestions = usedKeys.contains(Tags.KEY_HIGHWAY) || usedKeys.contains(Tags.KEY_WATERWAY) 
					|| usedKeys.contains(Tags.KEY_LANDUSE) || usedKeys.contains(Tags.KEY_NATURAL) || usedKeys.contains(Tags.KEY_RAILWAY);
			if (isStreetName) {
				adapter = getStreetNameAutocompleteAdapter(row.tagValues != null && row.tagValues.size() > 1 ? row.tagValues : null);
			} else if (isPlaceName) {
				adapter = getPlaceNameAutocompleteAdapter(row.tagValues != null && row.tagValues.size() > 1 ? row.tagValues : null);
			} else if (key.equals(Tags.KEY_NAME) && (names != null) && !noNameSuggestions) {
				ArrayList<NameAndTags> values = (ArrayList<NameAndTags>) names.getNames(new TreeMap<String,String>(getKeyValueMapSingle(rowLayout,true))); // FIXME
				if (values != null && !values.isEmpty()) {
					ArrayList<NameAndTags> result = values;
					Collections.sort(result);
					adapter = new ArrayAdapter<NameAndTags>(getActivity(), R.layout.autocomplete_row, result);
				}
			} else {
				HashMap<String, Integer> counter = new HashMap<String, Integer>();
				ArrayAdapter<ValueWithCount> adapter2 = new ArrayAdapter<ValueWithCount>(getActivity(), R.layout.autocomplete_row);
				if (row.tagValues != null && row.tagValues.size() > 1) {
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
						ValueWithCount v = new ValueWithCount(t,counter.get(t).intValue());
						adapter2.add(v);
					}
				}
				Collection<String> values = null;
				if (autocompletePresetItem != null) { // note this will use the last applied preset which may be wrong FIXME
					values = autocompletePresetItem.getAutocompleteValues(key);
				} 
				if (values == null && ((PropertyEditor)getActivity()).presets != null && elements[0] != null) { // FIXME
					values = Preset.getAutocompleteValues(((PropertyEditor)getActivity()).presets,elements[0].getType(), key);
				}
				if (values != null && !values.isEmpty()) {
					ArrayList<String> result = new ArrayList<String>(values);
					Collections.sort(result);
					for (String s:result) {
						if (counter != null && counter.containsKey(s)) {
							continue; // skip stuff that is already listed
						}
						adapter2.add(new ValueWithCount(s));
					}
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
		if (autocompletePresetItem != null && aTagKey != null && !aTagKey.equals("")) { // set hints even if value isen't empty
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
			if (!same) {
				row.valueEdit.setHint(R.string.tag_multi_value_hint); // overwrite the above
			}
		}
		// If the user selects addr:street from the menu, auto-fill a suggestion
		row.keyEdit.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (Tags.KEY_ADDR_STREET.equals(parent.getItemAtPosition(position)) &&
						row.valueEdit.getText().toString().length() == 0) {
					ArrayAdapter<ValueWithCount> adapter = getStreetNameAutocompleteAdapter(tagValues);
					if (adapter != null && adapter.getCount() > 0) {
						row.valueEdit.setText(adapter.getItem(0).getValue());
					}
				} else if (Tags.KEY_ADDR_PLACE.equals(parent.getItemAtPosition(position)) &&
						row.valueEdit.getText().toString().length() == 0) {
					ArrayAdapter<ValueWithCount> adapter = getPlaceNameAutocompleteAdapter(tagValues);
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
						if (row.valueEdit.getText().toString().length() == 0) {
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
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					row.keyEdit.setAdapter(getKeyAutocompleteAdapter(rowLayout, row.keyEdit));
					if (PropertyEditor.running && row.keyEdit.getText().length() == 0) row.keyEdit.showDropDown();
				}
			}
		});
		row.valueEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					row.valueEdit.setAdapter(getValueAutocompleteAdapter(rowLayout, row));
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
					row.valueEdit.setText(((NameAndTags)o).getName());
					applyTagSuggestions(((NameAndTags)o).getTags());
				} else if (o instanceof ValueWithCount) {
					row.valueEdit.setText(((ValueWithCount)o).getValue());
				} else if (o instanceof String) {
					row.valueEdit.setText((String)o);
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
						tagDeselected();
					}
				}
				if (row.isEmpty()) {
					row.deSelect();
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
	public static class TagEditRow extends LinearLayout {
		
		private PropertyEditor owner;
		private AutoCompleteTextView keyEdit;
		private OffsettedAutoCompleteTextView valueEdit;
		private CheckBox selected;
		private ArrayList<String> tagValues;
				
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
			
			valueEdit = (OffsettedAutoCompleteTextView)findViewById(R.id.editValue);
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
		public void deleteRow() { //FIXME the references to owner.tagEditorFragemnt are likely suspect
			View cf = owner.getCurrentFocus();
			if (cf == keyEdit || cf == valueEdit) {
				// about to delete the row that has focus!
				// try to move the focus to the next row or failing that to the previous row
				int current = owner.tagEditorFragment.rowIndex(this);
				if (!owner.tagEditorFragment.focusRow(current + 1)) owner.tagEditorFragment.focusRow(current - 1);
			}
			((LinearLayout)owner.tagEditorFragment.getOurView()).removeView(this);
			if (isEmpty()) {
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
		
		public void deSelect() {
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
				applyPreset(p, false); 
			}
		}
	}
	
	protected void tagSelected() {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		if (tagSelectedActionModeCallback == null) {
			tagSelectedActionModeCallback = new TagSelectedActionModeCallback(this, rowLayout);
			((SherlockFragmentActivity)getActivity()).startActionMode(tagSelectedActionModeCallback);
		}	
	}
	
	protected void tagDeselected() {
		if (tagSelectedActionModeCallback != null) {
			if (tagSelectedActionModeCallback.tagDeselected()) {
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
				if (ret == null) ret = isEmpty ? row : insertNewEdit(rowLayout,"", new ArrayList(), -1);
				else if (isEmpty) row.deleteRow();
			}
			if (ret == null) ret = insertNewEdit(rowLayout,"", new ArrayList(), -1);
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
		applyPreset(item, true);
	}
	
	/**
	 * Applies a preset (e.g. selected from the dialog or MRU), i.e. adds the tags from the preset to the current tag set
	 * @param item the preset to apply
	 */
	private void applyPreset(PresetItem item, boolean addToMRU) {
		autocompletePresetItem = item;
		LinkedHashMap<String, ArrayList<String>> currentValues = getKeyValueMap(true);
		
		boolean replacedValue = false;	
		
		// Fixed tags, always have a value. We overwrite mercilessly.
		for (Entry<String, String> tag : item.getTags().entrySet()) {
			ArrayList<String> oldValue = currentValues.put(tag.getKey(), Util.getArrayList(tag.getValue()));
			if (oldValue != null && oldValue.size() > 0 && !oldValue.contains(tag.getValue())) {
				replacedValue = true;
			}
		}
		
		// Recommended tags, no fixed value is given. We add only those that do not already exist.
		for (Entry<String, String[]> tag : item.getRecommendedTags().entrySet()) {
			if (!currentValues.containsKey(tag.getKey())) {
				currentValues.put(tag.getKey(), Util.getArrayList(""));
			}
		}
		
		loadEdits(currentValues);
		if (replacedValue) Toast.makeText(getActivity(), R.string.toast_preset_overwrote_tags, Toast.LENGTH_LONG).show();
		
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
		HashMap<String,KeyValue> keyIndex = new HashMap(); // needed for de-duping
		
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
			loadEdits(Address.predictAddressTags(this, getKeyValueMap(false)));
			return true;
		case R.id.tag_menu_sourcesurvey:
			doSourceSurvey();
			return true;
		case R.id.tag_menu_apply_preset:
			PresetItem pi = Preset.findBestMatch(((PropertyEditor)getActivity()).presets,getKeyValueMapSingle(false)); // FIXME
			if (pi!=null) {
				applyPreset(pi, false); 
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
			Uri uri = null;
			LinkedHashMap<String, String> map = getKeyValueMapSingle(false); // FIXME
			if (map !=null) {
				PresetItem p =  Preset.findBestMatch(((PropertyEditor)getActivity()).presets,map);
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
			Intent startHelpViewer = new Intent(getActivity(), HelpViewer.class);
			startHelpViewer.putExtra(HelpViewer.TOPIC, R.string.help_propertyeditor);
			startActivity(startHelpViewer);
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
							tags.put(key, tagValues);
						} else {
							tags.put(key, Util.getArrayList(value));
						}
					}
				}
			}
		});
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
	LinkedHashMap<String,String> getKeyValueMapSingle(final boolean allowBlanks) {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		return getKeyValueMapSingle(rowLayout, allowBlanks);
	}	
		
	LinkedHashMap<String,String> getKeyValueMapSingle(LinearLayout rowLayout, final boolean allowBlanks) {
		
		final LinkedHashMap<String,String> tags = new LinkedHashMap<String, String>();
		if (rowLayout == null && savedTags != null) {
			// we've been stopped and the view hasn't been recreated
			for (String key:savedTags.keySet()) {
				ArrayList<String> values = savedTags.get(key);
				String value = (values != null && values.size() > 0 ? values.get(0):"");
				if (!("".equals(value) && !allowBlanks)) {
					tags.put(key, value);
				}
			}
			return tags;
		}
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
						if (valueBlank && hasValues) {
							tags.put(key, tagValues.get(0)); // FIXME
						} else {
							tags.put(key, value);
						}
					}
				}
			}
		});
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
			Map<String, String> copied = savingHelper.load(((PropertyEditor)getActivity()).COPIED_TAGS_FILE, false);
			if (copied != null) {
				mergeTags(copied, replace);
			}
		}
	}
		
	/**
	 * reload original arguments
	 */
	private void doRevert() {
		loadEdits(buildEdits());
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
	
	void deselectHeaderCheckBox() {
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
		ArrayList<LinkedHashMap<String,String>> oldTags = (ArrayList<LinkedHashMap<String,String>>)getArguments().getSerializable("tags");
		// make a (nearly) full copy
		ArrayList<LinkedHashMap<String,String>> newTags = new ArrayList<LinkedHashMap<String,String>>();
		for (LinkedHashMap<String,String> map:oldTags) {
			newTags.add((LinkedHashMap<String, String>) map.clone());
		}
		
		LinkedHashMap<String,ArrayList<String>> edits = getKeyValueMap(true);
		if (edits == null) {
			return oldTags;
		}
		
		for (LinkedHashMap<String,String> map:newTags) {
			for (String key:new TreeSet<String>(map.keySet())) {
				if (edits.containsKey(key)) {
					if (edits.get(key).size()==1) {
						if (!edits.get(key).get(0).trim().equals("")) {
							map.put(key, edits.get(key).get(0));
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
				if (!map.containsKey(editsKey) && edits.get(editsKey).size()==1 && !edits.get(editsKey).get(0).trim().equals("")) { // zap empty stuff
					map.put(editsKey,edits.get(editsKey).get(0));
				}
			}
		}
		return newTags;
	}
}
