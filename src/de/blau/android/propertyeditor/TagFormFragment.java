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
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
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
import de.blau.android.presets.PlaceTagValueAutocompletionAdapter;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.PresetKeyType;
import de.blau.android.presets.StreetTagValueAutocompletionAdapter;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.propertyeditor.TagEditorFragment.TagEditRow;
import de.blau.android.util.ClipboardUtils;
import de.blau.android.util.KeyValue;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Util;
import de.blau.android.views.CustomAutoCompleteTextView;


	
public class TagFormFragment extends SherlockFragment {

	private static final String DEBUG_TAG = TagFormFragment.class.getSimpleName();
	
	
	LayoutInflater inflater = null;

	private Names names = null;

	private Preferences prefs = null;

	private TagUpdate tagListener = null;

	private NameAdapters nameAdapters = null;

	
	/**
	 * @param applyLastAddressTags 
	 * @param focusOnKey 
	 * @param displayMRUpresets 
     */
    static public TagFormFragment newInstance(boolean displayMRUpresets) {
    	TagFormFragment f = new TagFormFragment();
    	
        Bundle args = new Bundle();
   
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
        	tagListener = (TagUpdate) activity;
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
     	rowLayout = (ScrollView) inflater.inflate(R.layout.tagform_view, null);
       
     	LinearLayout formRowLayout = (LinearLayout) rowLayout.findViewById(R.id.form_editable_row_layout);
     	// editRowLayout.setSaveFromParentEnabled(false);
     	formRowLayout.setSaveEnabled(false); 
     	
     	boolean displayMRUpresets = ((Boolean) getArguments().getSerializable("displayMRUpresets")).booleanValue();
     	
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
    	update();
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

    private void setTitle(PresetItem preset) {
        View view = getView();
     	if (view != null) {
     		TextView iconView = (TextView) view.findViewById(R.id.form_header_icon_view);
     		if (iconView != null && preset != null) { 
     			Drawable icon = preset.getIcon();
     			if (icon != null) {
     				iconView.setCompoundDrawables(null, icon, null, null);
     			} else {
     				iconView.setCompoundDrawables(null, null, null, null);
     			}
     		} else {
     			Log.d(DEBUG_TAG, "setTitle iconLayout null");
     			iconView.setCompoundDrawables(null, null, null, null);
     		}
     		TextView title = (TextView) view.findViewById(R.id.form_header_title);
     		if (title != null) {
     			if (preset != null) {
     				title.setText(preset.getTranslatedName());
     			} else {
     				title.setText("Unknown element (no preset)");
     			}
     		} else {
     			Log.d(DEBUG_TAG, "setTitle title null");
     		}
     	} else {
 			Log.d(DEBUG_TAG, "setTitle getView null");
 		}
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
					if (preset.getKeyType(key) == PresetKeyType.TEXT || preset.getKeyType(key) == PresetKeyType.MULTISELECT 
						|| (preset.getKeyType(key) == PresetKeyType.CHECK && count > 1)	|| count > 5) {
						final TagTextRow row = (TagTextRow)inflater.inflate(R.layout.tag_form_text_row, null);
						row.keyView.setText(hint != null?hint:key);
						row.valueView.setText(value);
						row.valueView.setAdapter(adapter);
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
						rowLayout.addView(row);
					} else if (preset.getKeyType(key) == PresetKeyType.COMBO) {
						final TagComboRow row = (TagComboRow)inflater.inflate(R.layout.tag_form_combo_row, null);
						row.keyView.setText(hint != null?hint:key);
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
							row.addButton(description, v, v.equals(value));
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
						Object o = adapter.getItem(0);
						final String v;
						String description = "";
						if (count==1) {
							if (o instanceof ValueWithCount) {
								v = ((ValueWithCount)o).getValue();
								description = ((ValueWithCount)o).getDescription();
							} else if (o instanceof StringWithDescription) {
								v = ((StringWithDescription)o).getValue();
								description = ((StringWithDescription)o).getDescription();
							} else if (o instanceof String) {
								v = (String)o;
								description = v;
							} else {
								v = "";
							}
						} else {
							v = "";
						}
						if (description==null) {
							description=v;
						}
						row.getCheckBox().setChecked(v.equals(value));
						rowLayout.addView(row);
						row.getCheckBox().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(
									CompoundButton buttonView, boolean isChecked) {
								tagListener.updateSingleValue(key, isChecked?v:"");
							} 
						});
					}
				}
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
				adapter = nameAdapters.getStreetNameAutocompleteAdapter(Util.getArrayList(value));
			} else if (TagEditorFragment.isPlaceName(key, usedKeys)) {
				adapter = nameAdapters.getPlaceNameAutocompleteAdapter(Util.getArrayList(value));
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
					Collections.sort(result);
					for (StringWithDescription s:result) {
						if (counter != null && counter.containsKey(s.getValue())) {
							continue; // skip stuff that is already listed
						}
						counter.put(s.getValue(),Integer.valueOf(1));
						adapter2.add(new ValueWithCount(s.getValue(), s.getDescription(), true));
					}
					Log.d(DEBUG_TAG,"key " + key + " type " + preset.getKeyType(key));
				} 
				if (!counter.containsKey("") && !counter.containsKey(null) && presetType != PresetKeyType.CHECK) {
					adapter2.insert(new ValueWithCount("", "Not set", true),0); // FIXME allow unset value depending on preset
				}
				if (value != null && !counter.containsKey(value) && (presetType != PresetKeyType.CHECK && !"".equals(value))) {
					ValueWithCount v = new ValueWithCount(value,1); // FIXME determine description in some way
					adapter2.insert(v,0);
				}	
				if (adapter2.getCount() > 0) {
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
		case R.id.tag_menu_revert:
			doRevert();
			return true;
		case R.id.tag_menu_mapfeatures:
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
	void doRevert() {
	}
	
	
	/**
	 * Return the view we have our rows in and work around some android craziness
	 * @return
	 */
	public View getEditableView() {
		// android.support.v4.app.NoSaveStateFrameLayout
		View v =  getView();	
		if (v != null) {
			if ( v.getId() == R.id.form_editable_row_layout) {
				Log.d(DEBUG_TAG,"got correct view in getView");
				return v;
			} else {
				v = v.findViewById(R.id.form_editable_row_layout);
				if (v == null) {
					Log.d(DEBUG_TAG,"didn't find R.id.form_editable_row_layout");
				}  else {
					Log.d(DEBUG_TAG,"Found R.id.form_editable_row_layout");
				}
				return v;
			}
		} else {
			Log.d(DEBUG_TAG,"got null view in getEditableView");
		}
		return null;
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
		LinearLayout editableView = (LinearLayout) getEditableView();
		if (editableView != null && editableView.getChildCount() > 0) {
			editableView.removeAllViews(); 
		}
		LinearLayout nonEditableView = (LinearLayout) getImmutableView();
		if (nonEditableView != null && nonEditableView.getChildCount() > 0) {
			nonEditableView.removeAllViews(); 
		}
    	PresetItem preset = tagListener.getBestPreset();
    	setTitle(preset);
    	LinkedHashMap<String, String> allTags = tagListener.getKeyValueMapSingle(true);
    	if (allTags != null) {
    		LinkedHashMap<String,String> recommendedEditable = new LinkedHashMap<String,String>();
    		LinkedHashMap<String,String> optionalEditable = new LinkedHashMap<String,String>();
    		LinkedHashMap<String,String> nonEditable = new LinkedHashMap<String,String>();
    		for (String key:allTags.keySet()) {
    			if (preset != null && preset.hasKeyValue(key, allTags.get(key))) {
    				if (preset.isRecommendedTag(key)) {
    					recommendedEditable.put(key, allTags.get(key));
    				} else {
    					optionalEditable.put(key, allTags.get(key));
    				}
    			} else {
    				nonEditable.put(key, allTags.get(key));
    			}
    		}
    		for (String key:recommendedEditable.keySet()) {
    			addRow(editableView,key, recommendedEditable.get(key),preset, allTags);
    		}
    		for (String key:optionalEditable.keySet()) {
    			addRow(editableView,key, optionalEditable.get(key),preset, allTags);
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
    	}	
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
		
		public String getKey() {
			return keyView.getText().toString();
		}
		
		public String getValue() { // FIXME check if returning the textedit value is actually ok
			return valueView.getText().toString();
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
		
		public String getKey() {
			return keyView.getText().toString();
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
		
		public String getKey() {
			return keyView.getText().toString();
		}
		
		public CheckBox getCheckBox() {
			return valueCheck;
		}
		
		public boolean isChecked() { 
			return valueCheck.isChecked();
		}
	}
}
