package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.Application;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.PresetKeyType;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Util;
import de.blau.android.views.CustomAutoCompleteTextView;


	
public class TagFormFragment extends SherlockFragment implements FormUpdate {

	private static final String DEBUG_TAG = TagFormFragment.class.getSimpleName();
	
	
	LayoutInflater inflater = null;

	private Names names = null;

	private Preferences prefs = null;

	private EditorUpdate tagListener = null;

	private NameAdapters nameAdapters = null;
	
	private boolean focusOnAddress = false;

	
	/**
	 * @param applyLastAddressTags 
	 * @param focusOnKey 
	 * @param displayMRUpresets 
     */
    static public TagFormFragment newInstance(boolean displayMRUpresets, boolean focusOnAddress) {
    	TagFormFragment f = new TagFormFragment();
    	
        Bundle args = new Bundle();
   
        args.putSerializable("displayMRUpresets", Boolean.valueOf(displayMRUpresets));
        args.putSerializable("focusOnAddress", Boolean.valueOf(focusOnAddress));

        f.setArguments(args);
        // f.setShowsDialog(true);
        
        return f;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
        try {
        	tagListener = (EditorUpdate) activity;
            nameAdapters = (NameAdapters) activity;
        } catch (ClassCastException e) {
        	throw new ClassCastException(activity.toString() + " must implement OnPresetSelectedListener and NameAdapters");
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
		}
    	
    	
     	this.inflater = inflater;
     	rowLayout = (ScrollView) inflater.inflate(R.layout.tag_form_view, null);
           	
     	boolean displayMRUpresets = ((Boolean) getArguments().getSerializable("displayMRUpresets")).booleanValue();
     	focusOnAddress = ((Boolean) getArguments().getSerializable("focusOnAddress")).booleanValue();
     	
       	// Log.d(DEBUG_TAG,"element " + element + " tags " + tags);
		
	
		if (getUserVisibleHint()) { // don't request focus if we are not visible 
			Log.d(DEBUG_TAG,"is visible");
		}	
		// 
    	prefs = new Preferences(getActivity());
		
		if (prefs.getEnableNameSuggestions()) {
			names = Application.getNames(getActivity());
		}

		if (displayMRUpresets) {
			Log.d(DEBUG_TAG,"Adding MRU prests");
			FragmentManager fm = getChildFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
			if (recentPresetsFragment != null) {
				ft.remove(recentPresetsFragment);
			}
			
			recentPresetsFragment = RecentPresetsFragment.newInstance(((PropertyEditor)getActivity()).getElement()); // FIXME
			ft.add(R.id.form_mru_layout,recentPresetsFragment,"recentpresets_fragment");
			ft.commit();
		}
		
		Log.d(DEBUG_TAG,"onCreateView returning");
		return rowLayout;
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
	 * Simpilified version for non-multi-select and preset only situation
	 * @param key
	 * @param value
	 * @param allTags
	 * @return
	 */
	protected ArrayAdapter<?> getValueAutocompleteAdapter(String key, String value, PresetItem preset, LinkedHashMap<String, String> allTags) {
		ArrayAdapter<?> adapter = null;
	
		if (key != null && key.length() > 0) {
			Set<String> usedKeys = allTags.keySet();
			PresetKeyType presetType = preset.getKeyType(key);
			if (TagEditorFragment.isStreetName(key, usedKeys)) {
				adapter = nameAdapters.getStreetNameAutocompleteAdapter(value!=null?Util.getArrayList(value):null);
			} else if (TagEditorFragment.isPlaceName(key, usedKeys)) {
				adapter = nameAdapters.getPlaceNameAutocompleteAdapter(value!=null?Util.getArrayList(value):null);
			} else if (key.equals(Tags.KEY_NAME) && (names != null) && TagEditorFragment.useNameSuggestions(usedKeys)) {
				Log.d(DEBUG_TAG,"generate suggestions for name from name suggestion index");
				ArrayList<NameAndTags> values = (ArrayList<NameAndTags>) names.getNames(new TreeMap<String,String>(allTags)); 
				if (values != null && !values.isEmpty()) {
					ArrayList<NameAndTags> result = values;
					Collections.sort(result);
					adapter = new ArrayAdapter<NameAndTags>(getActivity(), R.layout.autocomplete_row, result);
				}
			} else {
				HashMap<String, Integer> counter = new HashMap<String, Integer>();
				ArrayAdapter<ValueWithCount> adapter2 = new ArrayAdapter<ValueWithCount>(getActivity(), R.layout.autocomplete_row);
	
				Collection<StringWithDescription> values = preset.getAutocompleteValues(key);
				Log.d(DEBUG_TAG,"setting autocomplete adapter for values " + values);
				if (values != null && !values.isEmpty()) {
					ArrayList<StringWithDescription> result = new ArrayList<StringWithDescription>(values);
					if (preset.sortIt(key)) {
						Collections.sort(result);
					}
					for (StringWithDescription s:result) {
						if (counter != null && counter.containsKey(s.getValue())) {
							continue; // skip stuff that is already listed
						}
						counter.put(s.getValue(),Integer.valueOf(1));
						
						adapter2.add(new ValueWithCount(s.getValue(), s.getDescription(), true));
					}
					Log.d(DEBUG_TAG,"key " + key + " type " + preset.getKeyType(key));
				} 
				if (!counter.containsKey("") && !counter.containsKey(null)) { // add empty value so that we can remove tag
					adapter2.insert(new ValueWithCount("", getString(R.string.tag_not_set), true),0); // FIXME allow unset value depending on preset
				}
				if (value != null && !"".equals(value) && !counter.containsKey(value)) { // add in any non-standard non-empty values
					ValueWithCount v = new ValueWithCount(value,1); // FIXME determine description in some way
					adapter2.insert(v,0);
				}	
				if (adapter2.getCount() > 1) {
					return adapter2;
				}
			}
		}
		return adapter;
	}
	
	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		// final MenuInflater inflater = getSupportMenuInflater();
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.tag_form_menu, menu);
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
			updateEditorFromText();
			((PropertyEditor)getActivity()).sendResultAndFinish();
			return true;
		case R.id.tag_menu_address:
			updateEditorFromText();
			tagListener.predictAddressTags(true);
			update();
			if (!focusOnValue(Tags.KEY_ADDR_HOUSENUMBER)) {
				focusOnValue(Tags.KEY_ADDR_STREET);
			} 
			return true;
		case R.id.tag_menu_apply_preset:
			PresetItem pi = tagListener.getBestPreset();
			if (pi!=null) {
				((PropertyEditor)getActivity()).onPresetSelected(pi, true);
			}
			return true;
		case R.id.tag_menu_revert:
			doRevert();
			return true;
		case R.id.tag_menu_mapfeatures:
			startActivity(Preset.getMapFeaturesIntent(getActivity(),tagListener.getBestPreset()));
			return true;
		case R.id.tag_menu_delete_unassociated_tags:
			// remove tags that don't belong to an identified preset
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
	 * reload original arguments
	 */
	private void doRevert() {
		tagListener.revertTags();
		update();
	}
	
	/**
	 * update editor with any potential text changes that haven't been saved yet
	 */
	private void updateEditorFromText() {
		// check for focus on text field
		LinearLayout l = (LinearLayout) getView().findViewById(R.id.form_container_layout);
		if (l != null) { // FIXME this might need an alert
			View v = l.findFocus();
			if (v != null && v instanceof CustomAutoCompleteTextView){
				View row = v;
				do {
					row = (View) row.getParent();
				} while (row != null && !(row instanceof TagTextRow));
				if (row != null) {
					tagListener.updateSingleValue(((TagTextRow) row).getKey(), ((TagTextRow) row).getValue());
				}
			}
		}
	}
	
	/**
	 * Return the view we have our rows in and work around some android craziness
	 * @return
	 */
	public View getImmutableView() {
		// android.support.v4.app.NoSaveStateFrameLayout
		View v =  getView();	
		if (v != null) {
			if ( v.getId() == R.id.form_immutable_row_layout) {
				Log.d(DEBUG_TAG,"got correct view in getView");
				return v;
			} else {
				v = v.findViewById(R.id.form_immutable_row_layout);
				if (v == null) {
					Log.d(DEBUG_TAG,"didn't find R.id.form_immutable_row_layout");
				}  else {
					Log.d(DEBUG_TAG,"Found R.id.form_immutable_row_layout");
				}
				return v;
			}
		} else {
			Log.d(DEBUG_TAG,"got null view in getView");
		}
		return null;
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
	
	public void update() {
		Log.d(DEBUG_TAG,"update");
		// remove all editable stuff
		View sv = getView();
		LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
		if (ll != null) {
			while (ll.getChildAt(0) instanceof EditableLayout) {
				ll.removeViewAt(0);
			}
		} else {
			Log.d(DEBUG_TAG,"update container layout null");
			return;
		}		
		final EditableLayout editableView  = (EditableLayout)inflater.inflate(R.layout.tag_form_editable, null);
		editableView.setSaveEnabled(false); 
		int pos = 0;
		ll.addView(editableView, pos++);
		
		LinearLayout nonEditableView = (LinearLayout) getImmutableView();
		if (nonEditableView != null && nonEditableView.getChildCount() > 0) {
			nonEditableView.removeAllViews(); 
		}
		
    	PresetItem mainPreset = tagListener.getBestPreset();
    	editableView.setTitle(mainPreset);
//    	currently it is not clear how to get the view to re-layout
//    	editableView.headerIconView.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Log.d(DEBUG_TAG,"onClick called");
//				if (editableView.rowLayout.getVisibility()==View.GONE) {
//					editableView.open();
//				} else {
//					editableView.close();
//				}
//				getView().requestLayout();
//				getView().invalidate();
//			}});
//    	
    	LinkedHashMap<String, String> allTags = tagListener.getKeyValueMapSingle(true);
    	Map<String, String> nonEditable;
    	if (mainPreset != null) {
    		nonEditable = addTagsToViews(editableView, mainPreset, allTags);
    		for (PresetItem preset:tagListener.getSecondaryPresets()) {
    			final EditableLayout editableView1  = (EditableLayout)inflater.inflate(R.layout.tag_form_editable, null);
    			editableView1.setSaveEnabled(false);
    			editableView1.setTitle(preset);
    			ll.addView(editableView1, pos++);
    			nonEditable = addTagsToViews(editableView1, preset, (LinkedHashMap<String, String>) nonEditable);
    		}
    	} else {
    		nonEditable = allTags;
    	}
    	
    	LinearLayout nel = (LinearLayout) getView().findViewById(R.id.form_immutable_header_layout);
    	if (nel != null) {
    		nel.setVisibility(View.GONE);
    	}
    	if (nonEditable.size() > 0) {
    		nel.setVisibility(View.VISIBLE);
    		for (String key:nonEditable.keySet()) {
    			addRow(nonEditableView,key, nonEditable.get(key),null, null);
    		}
    	}
    	
    	if (focusOnAddress) {
    		focusOnAddress = false; // only do it once
    		if (!focusOnValue(Tags.KEY_ADDR_HOUSENUMBER)) {
    			focusOnValue(Tags.KEY_ADDR_STREET);
    		} 
    	}
	}
	
	Map<String,String> addTagsToViews(LinearLayout editableView, PresetItem preset, LinkedHashMap<String, String> tags) {
		LinkedHashMap<String,String> recommendedEditable = new LinkedHashMap<String,String>();
		LinkedHashMap<String,String> optionalEditable = new LinkedHashMap<String,String>();
		LinkedHashMap<String,String> linkedTags = new LinkedHashMap<String,String>();
		LinkedHashMap<String,String> nonEditable = new LinkedHashMap<String,String>();
		HashMap<String,PresetItem> keyToLinkedPreset = new HashMap<String,PresetItem>();
		
		if (preset != null) {
			List<PresetItem> linkedPresets = preset.getLinkedPresets();
			for (String key:tags.keySet()) {
				if (preset.hasKeyValue(key, tags.get(key))) {
					if (preset.isFixedTag(key)) {
						// skip
					} else if (preset.isRecommendedTag(key)) {
						recommendedEditable.put(key, tags.get(key));
					} else {
						optionalEditable.put(key, tags.get(key));
					}
				} else {
					boolean found = false;
					if (linkedPresets != null) { // check if tag is in a linked preset
						for (PresetItem l:linkedPresets) {
							if (l.hasKeyValue(key, tags.get(key))) {
								linkedTags.put(key, tags.get(key));
								keyToLinkedPreset.put(key, l);
								found = true;
								break;
							}
						}
					}
					if (!found) {
						nonEditable.put(key, tags.get(key));
					}
				}
			}
		} else {
			Log.e(DEBUG_TAG,"addTagsToViews called with null preset");
		}
		for (String key:recommendedEditable.keySet()) {
			addRow(editableView,key, recommendedEditable.get(key),preset, tags);
		}
		for (String key:optionalEditable.keySet()) {
			addRow(editableView,key, optionalEditable.get(key),preset, tags);
		}
		for (String key: linkedTags.keySet()) {
			addRow(editableView,key, linkedTags.get(key), keyToLinkedPreset.get(key), tags);
		}

		return nonEditable;
	}
	


	
	private void addRow(LinearLayout rowLayout, final String key, final String value, PresetItem preset, LinkedHashMap<String, String> allTags) {
		if (rowLayout != null) {
			if (preset != null) {
				if (!preset.isFixedTag(key)) {
					ArrayAdapter<?> adapter = getValueAutocompleteAdapter(key, value, preset, allTags);
					int count = 0;
					if (adapter!=null) {
						count = adapter.getCount();
					} else {
						Log.d(DEBUG_TAG,"adapter null " + key + " " + value + " " + preset);
					}
					String hint = preset.getHint(key);
					//
					PresetKeyType keyType = preset.getKeyType(key);
					String defaultValue = preset.getDefault(key);
					
					if (keyType == PresetKeyType.TEXT 
						|| keyType == PresetKeyType.MULTISELECT 
						|| key.startsWith(Tags.KEY_ADDR_BASE)
						|| count > 5) {
						rowLayout.addView(addTextRow(keyType, hint, key, value, defaultValue, adapter));
					} else if (preset.getKeyType(key) == PresetKeyType.COMBO || (keyType == PresetKeyType.CHECK && count > 2)) {
						final TagComboRow row = (TagComboRow)inflater.inflate(R.layout.tag_form_combo_row, null);
						row.keyView.setText(hint != null?hint:key);
						row.keyView.setTag(key);
						for (int i=0;i< count;i++) {
							Object o = adapter.getItem(i);
							String v = "";
							String description = "";
							if (o instanceof ValueWithCount) {
								v = ((ValueWithCount)o).getValue();
								description = ((ValueWithCount)o).getDescription();
							} else if (o instanceof StringWithDescription) {
								v = ((StringWithDescription)o).getValue();
								description = ((StringWithDescription)o).getDescription();
							} else if (o instanceof String) {
								v = (String)o;
								description = v;
							}
							if (description==null) {
								description=v;
							}
							if ((value == null || "".equals(value)) && (defaultValue != null && !"".equals(defaultValue))) {
								row.addButton(description, v, v.equals(defaultValue));
							} else {
								row.addButton(description, v, v.equals(value));
							}
						}
						rowLayout.addView(row);
						row.getRadioGroup().setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(RadioGroup group, int checkedId) {
								RadioButton button = (RadioButton) group.findViewById(checkedId);
								tagListener.updateSingleValue(key, (String)button.getTag());
							}
						});
					} else if (preset.getKeyType(key) == PresetKeyType.CHECK) {
						final TagCheckRow row = (TagCheckRow)inflater.inflate(R.layout.tag_form_check_row, null);
						row.keyView.setText(hint != null?hint:key);
						row.keyView.setTag(key);
						
						String v = "";
						String description = "";
						final String valueOn = preset.getOnValue(key);
						String tempValueOff = "";;
						
						// this is a bit of a roundabout way of determining the non-checked value;
						for (int i=0;i< adapter.getCount();i++) {
							Object o = adapter.getItem(i);
							if (o instanceof ValueWithCount) {
								v = ((ValueWithCount)o).getValue();
								description = ((ValueWithCount)o).getDescription();
							} else if (o instanceof StringWithDescription) {
								v = ((StringWithDescription)o).getValue();
								description = ((StringWithDescription)o).getDescription();
							} else if (o instanceof String) {
								v = (String)o;
								description = v;
							} 
							if (!v.equals(valueOn)) {
								tempValueOff = v;
							}
						}
						
						final String valueOff = tempValueOff;
						
						Log.d(DEBUG_TAG,"adapter size " + adapter.getCount() + " checked value >" + valueOn + "< not checked value >" + valueOff + "<");
						if (description==null) {
							description=v;
						}
						
						row.getCheckBox().setChecked(valueOn.equals(value));
						
						rowLayout.addView(row);
						row.getCheckBox().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(
									CompoundButton buttonView, boolean isChecked) {
								tagListener.updateSingleValue(key, isChecked?valueOn:valueOff);
							} 
						});
					}
				}
//			} else if (key.startsWith(Tags.KEY_ADDR_BASE)) { // make address tags always editable
//				Set<String> usedKeys = allTags.keySet();
//				ArrayAdapter<?> adapter = null;
//				if (TagEditorFragment.isStreetName(key, usedKeys)) {
//					adapter = nameAdapters.getStreetNameAutocompleteAdapter(Util.getArrayList(value));
//				} else if (TagEditorFragment.isPlaceName(key, usedKeys)) {
//					adapter = nameAdapters.getPlaceNameAutocompleteAdapter(Util.getArrayList(value));
//				}
//				// String hint = preset.getHint(key);
//				rowLayout.addView(addTextRow(null, null, key, value, adapter));
			} else {
				final TagStaticTextRow row = (TagStaticTextRow)inflater.inflate(R.layout.tag_form_static_text_row, null);
				row.keyView.setText(key);
				row.valueView.setText(value);
				rowLayout.addView(row);
			}
		} else {
 			Log.d(DEBUG_TAG, "addRow rowLayout null");
 		}	
	}
	
	TagTextRow addTextRow(PresetKeyType keyType, final String hint, final String key, final String value, final String defaultValue, final ArrayAdapter<?> adapter) {
		final TagTextRow row = (TagTextRow)inflater.inflate(R.layout.tag_form_text_row, null);
		row.keyView.setText(hint != null?hint:key);
		row.keyView.setTag(key);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) { // stop Hint from wrapping
			row.valueView.setEllipsize(TruncateAt.END);
		}
		if ((value == null || "".equals(value)) && (defaultValue != null && !"".equals(defaultValue))) {
			row.valueView.setText(defaultValue);
		} else {
			row.valueView.setText(value);
		}
		if (adapter != null && adapter.getCount() > 0) {
			row.valueView.setAdapter(adapter);
		}
		if (keyType==PresetKeyType.MULTISELECT) { 
			// FIXME this should be somewhere better obvious since it creates a non obvious side effect
			row.valueView.setTokenizer(new CustomAutoCompleteTextView.SingleCharTokenizer(TagEditorFragment.LIST_SEPARATOR));
		}
		if (keyType==PresetKeyType.TEXT && (adapter==null || adapter.getCount() < 2)) {
			row.valueView.setHint(R.string.tag_value_hint);
		} else {
			row.valueView.setHint(R.string.tag_autocomplete_value_hint);
		}
		OnClickListener autocompleteOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.hasFocus()) {
					((AutoCompleteTextView)v).showDropDown();
				}
			}
		};
		row.valueView.setOnClickListener(autocompleteOnClick);
		row.valueView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus && !row.getValue().equals(value)) {
					tagListener.updateSingleValue(key, row.getValue());
				}
			}
		});
		row.valueView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.d("TagEdit","onItemClicked value");
				Object o = parent.getItemAtPosition(position);
				if (o instanceof Names.NameAndTags) {
					row.valueView.setText2(((NameAndTags)o).getName());
					// applyTagSuggestions(((NameAndTags)o).getTags());
				} else if (o instanceof ValueWithCount) {
					row.valueView.setText2(((ValueWithCount)o).getValue());
				} else if (o instanceof StringWithDescription) {
					row.valueView.setText2(((StringWithDescription)o).getValue());
				} else if (o instanceof String) {
					row.valueView.setText2((String)o);
				}
				tagListener.updateSingleValue(key, row.getValue());
			}
		});
		
		return row;
	}
	
	/**
	 * Focus on the value field of a tag with key "key" 
	 * @param key
	 * @return
	 */
	private boolean focusOnValue( String key) {
		boolean found = false;
		View sv = getView();
		LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
		if (ll != null) {
			int pos = 0;
			while (ll.getChildAt(pos) instanceof EditableLayout && pos < ll.getChildCount() && !found) {
				EditableLayout ll2 = (EditableLayout) ll.getChildAt(pos);
				for (int i = ll2.getChildCount() - 1; i >= 0; --i) {
					View v = ll2.getChildAt(i);
					if (v instanceof TagTextRow && ((TagTextRow)v).getKey().equals(key)) {
						((TagTextRow)v).getValueView().requestFocus();
						found = true;
						break;
					}
				}
				pos++;
			}
		} else {
			Log.d(DEBUG_TAG,"update container layout null");
			return false;
		}	
		return found;
	}
	
	public static class TagTextRow extends LinearLayout {

		private TextView keyView;
		private CustomAutoCompleteTextView valueView;
		
		public TagTextRow(Context context) {
			super(context);
		}
		
		public TagTextRow(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueView = (CustomAutoCompleteTextView)findViewById(R.id.textValue);
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public String getValue() { 
			return valueView.getText().toString();
		}
		
		public CustomAutoCompleteTextView getValueView() {
			return valueView;
		}
	}
	
	public static class TagStaticTextRow extends LinearLayout {

		private TextView keyView;
		private TextView valueView;
		
		public TagStaticTextRow(Context context) {
			super(context);
		}
		
		public TagStaticTextRow(Context context, AttributeSet attrs) {
			super(context, attrs);
		}		

		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueView = (TextView)findViewById(R.id.textValue);
		}	
	}
	
	public static class TagComboRow extends LinearLayout {

		private TextView keyView;
		private RadioGroup valueGroup;
		private Context context;
		private int idCounter = 0;
		
		public TagComboRow(Context context) {
			super(context);
			this.context = context;
		}
		
		public TagComboRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			this.context = context;
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueGroup = (RadioGroup)findViewById(R.id.valueGroup);
			
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public RadioGroup getRadioGroup() { 
			return valueGroup;
		}
		
		public void addButton(String description, String value, boolean selected) {
			RadioButton button = new RadioButton(context);
			button.setText(description);
			button.setTag(value);
			button.setChecked(selected);
			button.setId(idCounter++);
			valueGroup.addView(button);
		}
	}
	
	public static class TagCheckRow extends LinearLayout {

		private TextView keyView;
		private CheckBox valueCheck;
		
		public TagCheckRow(Context context) {
			super(context);
		}
		
		public TagCheckRow(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueCheck = (CheckBox)findViewById(R.id.valueSelected);
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public CheckBox getCheckBox() {
			return valueCheck;
		}
		
		public boolean isChecked() { 
			return valueCheck.isChecked();
		}
	}
	
	public static class EditableLayout extends LinearLayout {

		private TextView headerIconView;
		private TextView headerTitleView;
		private LinearLayout rowLayout;
		
		public  EditableLayout(Context context) {
			super(context);
		}
		
		public  EditableLayout(Context context, AttributeSet attrs) {
			super(context, attrs);
		}		

		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			headerIconView = (TextView)findViewById(R.id.form_header_icon_view);
			headerTitleView = (TextView)findViewById(R.id.form_header_title);
			rowLayout = (LinearLayout) findViewById(R.id.form_editable_row_layout);
		}	
		
		private void setMyVisibility(int visibility) {
			rowLayout.setVisibility(visibility);
			for (int i=0;i < rowLayout.getChildCount();i++) {
				rowLayout.getChildAt(i).setVisibility(visibility);
			}
		}
		
		public void close() {
			Log.d(DEBUG_TAG,"close");
			setMyVisibility(View.GONE);
		}
		
		public void open() {
			Log.d(DEBUG_TAG,"open");
			setMyVisibility(View.VISIBLE);
		}
		
		public void setTitle(PresetItem preset) {

			if (preset != null) {
				Drawable icon = preset.getIcon();
				if (icon != null) {
					headerIconView.setVisibility(View.VISIBLE);
					headerIconView.setCompoundDrawables(null, icon, null, null);
				} else {
					headerIconView.setVisibility(View.GONE);
				}
				headerTitleView.setText(preset.getTranslatedName());
			} else {
				headerTitleView.setText("Unknown element (no preset)");
			}
		}
	}

	@Override
	public void tagsUpdated() {
		update();	
	}
}
