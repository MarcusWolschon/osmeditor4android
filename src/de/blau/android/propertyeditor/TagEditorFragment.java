package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

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
import de.blau.android.util.SavingHelper;


public class TagEditorFragment extends SherlockFragment {
	
	private static final String DEBUG_TAG = TagEditorFragment.class.getName();
	
	private static final String LAST_TAGS_FILE = "lasttags.dat";
	 
	private SavingHelper<LinkedHashMap<String,String>> savingHelper
				= new SavingHelper<LinkedHashMap<String,String>>();
	
	private StreetTagValueAutocompletionAdapter streetNameAutocompleteAdapter = null;
	
	private PlaceTagValueAutocompletionAdapter placeNameAutocompleteAdapter = null;
	
	static TagSelectedActionModeCallback tagSelectedActionModeCallback = null;
	
	PresetItem autocompletePresetItem = null;
	
	private static Names names = null;
	
	private boolean loaded = false;
	private String type;
	private long osmId;
	private Preferences prefs = null;
	private OsmElement element = null;
	
	LayoutInflater inflater = null;

	/**
	 * saves any changed fields on onPause
	 */
	private Map<String, String> savedTags = null;
	

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
			handler.handleKeyValue(row.keyEdit, row.valueEdit);
		}
	}
	
	/**
	 * @param applyLastAddressTags 
	 * @param focusOnKey 
	 * @param displayMRUpresets 
     */
    static public TagEditorFragment newInstance(OsmElement element, LinkedHashMap<String,String> tags, boolean applyLastAddressTags, String focusOnKey, boolean displayMRUpresets) {
    	TagEditorFragment f = new TagEditorFragment();
    	
        Bundle args = new Bundle();
   
        args.putSerializable("element", element);
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
    		
     	element = (OsmElement) getArguments().getSerializable("element");
     	type = element.getName();
     	osmId = element.getOsmId();
     	Map<String,String> tags;
     	if (savedTags != null) { // view was destroyed and needs to be recreated with current state
     		Log.d(DEBUG_TAG,"Restoring from instance variable");
     		tags = savedTags;
     	} else {
     		tags = (Map<String,String>)getArguments().getSerializable("tags");
     	}
     	boolean applyLastAddressTags = ((Boolean) getArguments().getSerializable("applyLastAddressTags")).booleanValue();
     	String focusOnKey = (String)  getArguments().getSerializable("focusOnKey");
     	boolean displayMRUpresets = ((Boolean) getArguments().getSerializable("displayMRUpresets")).booleanValue();
     	
       	Log.d(DEBUG_TAG,"element " + element + " tags " + tags);
		
       	loaded = false;
		// rowLayout.removeAllViews();
		for (Entry<String, String> pair : tags.entrySet()) {
			insertNewEdit(editRowLayout, pair.getKey(), pair.getValue(), -1);
		}
		
		loaded = true;
		TagEditRow row = ensureEmptyRow(editRowLayout);
		row.keyEdit.requestFocus();
		row.keyEdit.dismissDropDown();
		
		if (focusOnKey != null) {
			focusOnValue(editRowLayout,focusOnKey);
		} else {
			focusOnEmptyValue(editRowLayout); // probably never actually works
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
			
			recentPresetsFragment = RecentPresetsFragment.newInstance(element);
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
	 */
	protected void loadEdits(final Map<String,String> tags) {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		loadEdits(rowLayout, tags);
	}
	
	/**
	 * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
	 */
	protected void loadEdits(LinearLayout rowLayout, final Map<String,String> tags) {
	
		loaded = false;
		rowLayout.removeAllViews();
		for (Entry<String, String> pair : tags.entrySet()) {
			insertNewEdit(rowLayout, pair.getKey(), pair.getValue(), -1);
		}
		loaded = true;
		ensureEmptyRow(rowLayout);
	}
	
	/**
	 * Gets an adapter for the autocompletion of street names based on the neighborhood of the edited item.
	 * @return
	 */
	protected ArrayAdapter<String> getStreetNameAutocompleteAdapter() {
		if (Main.getLogic() == null || Main.getLogic().getDelegator() == null) {
			return null;
		}
		if (streetNameAutocompleteAdapter == null) {
			streetNameAutocompleteAdapter =	new StreetTagValueAutocompletionAdapter(getActivity(),
					R.layout.autocomplete_row, Main.getLogic().getDelegator(),
					type, osmId);
		}
		return streetNameAutocompleteAdapter;
	}
	
	/**
	 * Gets an adapter for the autocompletion of place names based on the neighborhood of the edited item.
	 * @return
	 */
	protected ArrayAdapter<String> getPlaceNameAutocompleteAdapter() {
		if (Main.getLogic() == null || Main.getLogic().getDelegator() == null) {
			return null;
		}
		if (placeNameAutocompleteAdapter == null) {
			placeNameAutocompleteAdapter =	new PlaceTagValueAutocompletionAdapter(getActivity(),
					R.layout.autocomplete_row, Main.getLogic().getDelegator(),
					type, osmId);
		}
		return placeNameAutocompleteAdapter;
	}

	protected ArrayAdapter<String> getKeyAutocompleteAdapter(LinearLayout rowLayout, AutoCompleteTextView keyEdit) {
		// Use a set to prevent duplicate keys appearing
		Set<String> keys = new HashSet<String>();
		
		if (autocompletePresetItem == null && ((PropertyEditor)getActivity()).presets != null) {
			autocompletePresetItem = Preset.findBestMatch(((PropertyEditor)getActivity()).presets, getKeyValueMap(rowLayout,false));
		}
		
		if (autocompletePresetItem != null) {
			keys.addAll(autocompletePresetItem.getTags().keySet());
			keys.addAll(autocompletePresetItem.getRecommendedTags().keySet());
			keys.addAll(autocompletePresetItem.getOptionalTags().keySet());
		}
		
		if (((PropertyEditor)getActivity()).presets != null && element != null) {
			keys.addAll(Preset.getAutocompleteKeys(((PropertyEditor)getActivity()).presets, element.getType()));
		}
		
		keys.removeAll(getUsedKeys(rowLayout,keyEdit));
		
		List<String> result = new ArrayList<String>(keys);
		Collections.sort(result);
		return new ArrayAdapter<String>(getActivity(), R.layout.autocomplete_row, result);
	}
	
	protected ArrayAdapter<?> getValueAutocompleteAdapter(LinearLayout rowLayout, AutoCompleteTextView keyEdit) {
		ArrayAdapter<?> adapter = null;
		String key = keyEdit.getText().toString();
		if (key != null && key.length() > 0) {
			HashSet<String> usedKeys = (HashSet<String>) getUsedKeys(rowLayout,null);
			boolean isStreetName = (Tags.KEY_ADDR_STREET.equalsIgnoreCase(key) ||
					(Tags.KEY_NAME.equalsIgnoreCase(key) && usedKeys.contains(Tags.KEY_HIGHWAY)));
			boolean isPlaceName = (Tags.KEY_ADDR_PLACE.equalsIgnoreCase(key) ||
					(Tags.KEY_NAME.equalsIgnoreCase(key) && usedKeys.contains(Tags.KEY_PLACE)));
			boolean noNameSuggestions = usedKeys.contains(Tags.KEY_HIGHWAY) || usedKeys.contains(Tags.KEY_WATERWAY) 
					|| usedKeys.contains(Tags.KEY_LANDUSE) || usedKeys.contains(Tags.KEY_NATURAL) || usedKeys.contains(Tags.KEY_RAILWAY);
			if (isStreetName) {
				adapter = getStreetNameAutocompleteAdapter();
			} else if (isPlaceName) {
				adapter = getPlaceNameAutocompleteAdapter();
			} else if (key.equals(Tags.KEY_NAME) && (names != null) && !noNameSuggestions) {
				ArrayList<NameAndTags> values = (ArrayList<NameAndTags>) names.getNames(new TreeMap<String,String>(getKeyValueMap(rowLayout,true)));
				if (values != null && !values.isEmpty()) {
					ArrayList<NameAndTags> result = values;
					Collections.sort(result);
					adapter = new ArrayAdapter<NameAndTags>(getActivity(), R.layout.autocomplete_row, result);
				}
			} else {
				Collection<String> values = null;
				if (autocompletePresetItem != null) { // note this will use the last applied preset which may be wrong FIXME
					values = autocompletePresetItem.getAutocompleteValues(key);
				} 
				if (values == null && ((PropertyEditor)getActivity()).presets != null && element != null) {
					values = Preset.getAutocompleteValues(((PropertyEditor)getActivity()).presets,element.getType(), key);
				}
				if (values != null && !values.isEmpty()) {
					ArrayList<String> result = new ArrayList<String>(values);
					Collections.sort(result);
					adapter = new ArrayAdapter<String>(getActivity(), R.layout.autocomplete_row, result);
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
	protected TagEditRow insertNewEdit(final LinearLayout rowLayout, final String aTagKey, final String aTagValue, final int position) {
		final TagEditRow row = (TagEditRow)inflater.inflate(R.layout.tag_edit_row, null);
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
		// If the user selects addr:street from the menu, auto-fill a suggestion
		row.keyEdit.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (Tags.KEY_ADDR_STREET.equals(parent.getItemAtPosition(position)) &&
						row.valueEdit.getText().toString().length() == 0) {
					ArrayAdapter<String> adapter = getStreetNameAutocompleteAdapter();
					if (adapter != null && adapter.getCount() > 0) {
						row.valueEdit.setText(adapter.getItem(0));
					}
				} else if (Tags.KEY_ADDR_PLACE.equals(parent.getItemAtPosition(position)) &&
						row.valueEdit.getText().toString().length() == 0) {
					ArrayAdapter<String> adapter = getPlaceNameAutocompleteAdapter();
					if (adapter != null && adapter.getCount() > 0) {
						row.valueEdit.setText(adapter.getItem(0));
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
					row.valueEdit.setAdapter(getValueAutocompleteAdapter(rowLayout, row.keyEdit));
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
		private AutoCompleteTextView valueEdit;
		private CheckBox selected;
		
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
			
			valueEdit = (AutoCompleteTextView)findViewById(R.id.editValue);
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
	

				
		/**
		 * Sets key and value values
		 * @param aTagKey the key value to set
		 * @param aTagValue the value value to set
		 * @return the TagEditRow object for convenience
		 */
		public TagEditRow setValues(String aTagKey, String aTagValue) {
			Log.d(DEBUG_TAG, "key " + aTagKey + " value " + aTagValue);
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
		final LinkedHashMap<String, String> currentValues = getKeyValueMap(true);
		
		boolean replacedValue = false;	
		
		// Fixed tags, always have a value. We overwrite mercilessly.
		for (Entry<String, String> tag : tags.entrySet()) {
			String oldValue = currentValues.put(tag.getKey(), tag.getValue());
			if (oldValue != null && oldValue.length() > 0 && !oldValue.equals(tag.getValue())) replacedValue = true;
		}
		if (replacedValue) {
			Builder dialog = new AlertDialog.Builder(getActivity());
			dialog.setTitle(R.string.tag_editor_name_suggestion);
			dialog.setMessage(R.string.tag_editor_name_suggestion_overwrite_message);
			dialog.setPositiveButton(R.string.replace, new DialogInterface.OnClickListener() {
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
			PresetItem p = Preset.findBestMatch(((PropertyEditor)getActivity()).presets,getKeyValueMap(false));
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
//		if (tagSelectedActionModeCallback == null) {
//			tagSelectedActionModeCallback = new TagSelectedActionModeCallback(this, rowLayout);
//			((SherlockFragmentActivity)getActivity()).startActionMode(tagSelectedActionModeCallback);
//		}	
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
//		if (tagSelectedActionModeCallback == null) {
//			tagSelectedActionModeCallback = new TagSelectedActionModeCallback(this, rowLayout);
//			((SherlockFragmentActivity)getActivity()).startActionMode(tagSelectedActionModeCallback);
//		}	
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
				if (ret == null) ret = isEmpty ? row : insertNewEdit(rowLayout,"", "", -1);
				else if (isEmpty) row.deleteRow();
			}
			if (ret == null) ret = insertNewEdit(rowLayout,"", "", -1);
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
				focusRowValue(rowLayout, rowIndex(ter));
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
				focusRowValue(rowLayout, rowIndex(ter));
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
			Log.d(DEBUG_TAG,"Updating MRU prests");
			FragmentManager fm = getChildFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
			if (recentPresetsFragment != null) {
				((RecentPresetsFragment)recentPresetsFragment).recreateRecentPresetView();
			}
		}
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
			PresetItem pi = Preset.findBestMatch(((PropertyEditor)getActivity()).presets,getKeyValueMap(false));
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
			startHelpViewer.putExtra(HelpViewer.TOPIC, "TagEditor");
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
	LinkedHashMap<String,String> getKeyValueMap(final boolean allowBlanks) {
		LinearLayout rowLayout = (LinearLayout) getOurView();
		return getKeyValueMap(rowLayout, allowBlanks);
	}	
		
	LinkedHashMap<String,String> getKeyValueMap(LinearLayout rowLayout, final boolean allowBlanks) {
		
		final LinkedHashMap<String,String> tags = new LinkedHashMap<String, String>();
		processKeyValues(rowLayout, new KeyValueHandler() {
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
			insertNewEdit((LinearLayout) getOurView(),sourceKey, Tags.VALUE_SURVEY, -1);
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
	 * reload original arguments
	 */
	private void doRevert() {
		loadEdits((Map<String,String>)getArguments().getSerializable("tags"));
	}
	
	/**
	 * @return the OSM ID of the element currently edited by the editor
	 */
	public long getOsmId() {
		return osmId;
	}
	
	
	public String getType() {
		return type;
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
}
