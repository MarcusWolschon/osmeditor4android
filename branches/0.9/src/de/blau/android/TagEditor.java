package de.blau.android;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetDialog;
import de.blau.android.presets.StreetTagValueAutocompletionAdapter;
import de.blau.android.util.SavingHelper;

/**
 * An Activity to edit OSM-Tags. Sends the edited Tags as Result to its caller-Activity (normally {@link Main}).
 * 
 * @author mb
 */
public class TagEditor extends SherlockActivity implements OnDismissListener {
	public static final String TAGEDIT_DATA = "dataClass";
	
	/** The layout containing the entire editor */
	private LinearLayout verticalLayout = null;
	
	/** The layout containing the edit rows */
	private LinearLayout rowLayout = null;
	
	/** The layout containing the presets */
	private LinearLayout presetsLayout = null;
	
	/** The layout containing the relations */
	private LinearLayout relationsLayout = null;
	
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
	
	private PresetItem autocompletePresetItem = null;
	Preset preset = null;
	
	private static final String LAST_TAGS_FILE = "lasttags.dat"; 
	private SavingHelper<LinkedHashMap<String,String>> savingHelper
				= new SavingHelper<LinkedHashMap<String,String>>();
	
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
	 * Move the focus to the key field of the specified row.
	 * @param index The index of the row to move to, counting from 0.
	 * @return true if the row was successfully focussed, false otherwise.
	 */
	private boolean focusRow(int index) {
		TagEditRow row = (TagEditRow)rowLayout.getChildAt(index);
		return row != null && row.keyEdit.requestFocus();
	}
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Not yet implemented by Google
		//getWindow().requestFeature(Window.FEATURE_CUSTOM_TITLE);
		//getWindow().setTitle(getString(R.string.tag_title) + " " + type + " " + osmId);
		
		// Disabled because it slows down the Motorola Milestone/Droid
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		setContentView(R.layout.tag_view);
		
		verticalLayout = (LinearLayout) findViewById(R.id.vertical_layout);
		rowLayout = (LinearLayout) findViewById(R.id.edit_row_layout);
		presetsLayout = (LinearLayout) findViewById(R.id.presets_layout);
		relationsLayout = (LinearLayout) findViewById(R.id.relations_layout);
		loaded = false;
		TagEditorData loadData;
		if (savedInstanceState == null) {
			// No previous state to restore - get the state from the intent
			Log.d(DEBUG_TAG, "Initializing from intent");
			loadData = (TagEditorData)getIntent().getSerializableExtra(TAGEDIT_DATA);
		} else {
			// Restore activity from saved state
			Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
			loadData = (TagEditorData)savedInstanceState.getSerializable(TAGEDIT_DATA);
		}
		Log.d(DEBUG_TAG, "... done.");
		osmId = loadData.osmId;
		type = loadData.type;
		loadEdits(loadData.tags);
		originalTags = loadData.originalTags != null ? loadData.originalTags : loadData.tags;
		element = Main.logic.delegator.getOsmElement(type, osmId);
		preset = Main.getCurrentPreset();
		loaded = true;
		TagEditRow row = ensureEmptyRow();
		row.keyEdit.requestFocus();
		row.keyEdit.dismissDropDown();
		createRecentPresetView();
		createRelationView();
		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayShowTitleEnabled(false);
		actionbar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		running = true;
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
						valueEdit.setText("survey");
						sourceSet[0] = true;
					}
				}
			}
		});
		if (!sourceSet[0]) {
			// source wasn't set above - add a new pair
			insertNewEdit(sourceKey, "survey", -1);
		}
	}
	
	private void doPresets() {
		if (Main.getCurrentPreset() != null) {
			showPresetDialog();
		}
	}
	
	private void doRepeatLast() {
		final Map<String, String> last = savingHelper.load(LAST_TAGS_FILE, false);
		if (last != null) {
			loadEdits(last);
		}
	}
	
	private void doRevert() {
		loadEdits(originalTags);
	}
	
	private void createRecentPresetView() {
		Preset preset = Main.getCurrentPreset();
		if (preset != null && element != null && preset.hasMRU()) {
			ElementType filterType = element.getType();
			View v = preset.getRecentPresetView(this, new PresetClickHandler() {
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
			v.setPadding(20, 20, 20, 20);
			v.setId(R.id.recentPresets);
			presetsLayout.addView(v);
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
	 * display relation memberships if any
	 */
	private void createRelationView() {
		
		if (element != null) { 
			ArrayList<Relation> pr = element.getParentRelations();
			if (element.getParentRelations() != null && element.getParentRelations().size() > 0) {
				TextView v = new TextView(this);
				v.setText(getResources().getString(R.string.relation_membership),TextView.BufferType.NORMAL);
				v.setTypeface(null, Typeface.BOLD);
				v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				v.setBackgroundColor(getResources().getColor(R.color.tagedit_field_bg));
				v.setPadding(10, 5, 5, 10);
				v.setSingleLine();
				relationsLayout.addView(v);
				for (Relation r :  pr) {
					RelationMember rm = r.getMember(element);
					v = new TextView(this);
					v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
					String desc = r.getDescription();
					String role = rm.getRole();
					if (role != null && !role.equals("")) {
						desc = desc + " role " + role;
					}
					v.setText(desc,TextView.BufferType.NORMAL);
					v.setBackgroundColor(getResources().getColor(R.color.tagedit_field_bg));
					v.setPadding(10, 5, 5, 10);
					v.setSingleLine();
					relationsLayout.addView(v);
				}
			}
			// if this is a relation get members
			if (element.getName().equals("relation")) {
			
				ArrayList<RelationMember> members = (ArrayList<RelationMember>) ((Relation)element).getMembers();
				if (members != null && members.size() > 0) {
					TextView v = new TextView(this);
					v.setText(getResources().getString(R.string.relation_members),TextView.BufferType.NORMAL);
					v.setTypeface(null, Typeface.BOLD);
					v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
					v.setBackgroundColor(getResources().getColor(R.color.tagedit_field_bg));
					v.setPadding(10, 5, 5, 10);
					v.setSingleLine();
					relationsLayout.addView(v);
					int pos=1;
					for (RelationMember rm :  members) {
						v = new TextView(this);
						v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
						String desc = rm.getType() + "\t#" + Long.toString(rm.getRef());
						OsmElement e = rm.getElement();
						if (e != null) {
							desc = desc + "\t" + e.getDescription();
						} else {
							desc = desc + "\tnot downloaded";
						}
						String role = rm.getRole();
						if (role != null && !role.equals("")) {
							desc = desc + "\trole " + role;
						}
						v.setText(desc,TextView.BufferType.NORMAL);
						v.setBackgroundColor(getResources().getColor(R.color.tagedit_field_bg));
						v.setPadding(10, 5, 5, 10);
						v.setSingleLine();
						relationsLayout.addView(v);
					}
				}
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
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			sendResultAndFinish();
			return true;
		case R.id.tag_menu_sourcesurvey:
			doSourceSurvey();
			return true;
		case R.id.tag_menu_preset:
			doPresets();
			return true;
		case R.id.tag_menu_repeat:
			doRepeatLast();
			return true;
		case R.id.tag_menu_revert:
			doRevert();
			return true;
		case R.id.tag_menu_mapfeatures:
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_mapfeatures)));
			startActivity(intent);
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onBackPressed() {
		sendResultAndFinish();
	}
	
	/**
	 * 
	 */
	protected void sendResultAndFinish() {
		// Save current tags for "repeat last" button
		savingHelper.save(LAST_TAGS_FILE, getKeyValueMap(false), false);
		
		Intent intent = new Intent();
		Map<String, String> currentTags = getKeyValueMap(false);
		if (!currentTags.equals(originalTags)) {
			// changes were made
			intent.putExtra(TAGEDIT_DATA, new TagEditorData(osmId, type, currentTags, null));
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
		outState.putSerializable(TAGEDIT_DATA, new TagEditorData(osmId, type, getKeyValueMap(true), originalTags));
	}
	
	/** When the Activity is interrupted, save MRUs*/
	@Override
	protected void onPause() {
		running = false;
		if (Main.getCurrentPreset() != null) Main.getCurrentPreset().saveMRU();
		super.onPause();
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
		
		public TagEditRow(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in Eclipse
		}
		
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
					if ("addr:street".equals(parent.getItemAtPosition(position)) &&
							valueEdit.getText().toString().length() == 0) {
						ArrayAdapter<String> adapter = getStreetNameAutocompleteAdapter();
						if (adapter != null && adapter.getCount() > 0) {
							valueEdit.setText(adapter.getItem(0));
						}
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
						if (running && valueEdit.getText().length() == 0) valueEdit.showDropDown();
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
			
			if (owner.autocompletePresetItem == null) {
				owner.autocompletePresetItem = owner.preset.findBestMatch(owner.getKeyValueMap(false));
			}
			
			if (owner.autocompletePresetItem != null) {
				keys.addAll(owner.autocompletePresetItem.getTags().keySet());
				keys.addAll(owner.autocompletePresetItem.getRecommendedTags().keySet());
				keys.addAll(owner.autocompletePresetItem.getOptionalTags().keySet());
			}
			
			if (owner.preset != null && owner.element != null) {
				keys.addAll(owner.preset.getAutocompleteKeys(owner.element.getType()));
			}
			
			keys.removeAll(owner.getUsedKeys(keyEdit));
			
			List<String> result = new ArrayList<String>(keys);
			Collections.sort(result);
			return new ArrayAdapter<String>(owner, R.layout.autocomplete_row, result);
		}
		
		protected ArrayAdapter<String> getValueAutocompleteAdapter() {
			ArrayAdapter<String> adapter = null;
			String key = keyEdit.getText().toString();
			if (key != null && key.length() > 0) {
				boolean isStreetName = ("addr:street".equalsIgnoreCase(key) ||
						("name".equalsIgnoreCase(key) && owner.getUsedKeys(null).contains("highway")));
				if (isStreetName) {
					adapter = getStreetNameAutocompleteAdapter();
				} else {
					if (owner.preset != null && owner.element != null) {
						Collection<String> values = owner.preset.getAutocompleteValues(owner.element.getType(), key);
						if (values != null && !values.isEmpty()) {
							List<String> result = new ArrayList<String>(values);
							Collections.sort(result);
							adapter = new ArrayAdapter<String>(owner, R.layout.autocomplete_row, result);
						}
					}
				}
			}
			return adapter;
		}
		
		/**
		 * Gets an adapter for the autocompletion of street names based on the neighborhood of the edited item.
		 * @return
		 */
		private ArrayAdapter<String> getStreetNameAutocompleteAdapter() {
			return (Main.logic == null || Main.logic.delegator == null) ? null :
				new StreetTagValueAutocompletionAdapter(owner,
						R.layout.autocomplete_row, Main.logic.delegator,
						owner.type, owner.osmId);
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
		if (Main.getCurrentPreset() != null && element != null) {
			presetDialog = new PresetDialog(this, Main.getCurrentPreset(), element);
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
		autocompletePresetItem = item;
		LinkedHashMap<String, String> currentValues = getKeyValueMap(true);
		
		boolean replacedValue = false;	
		
		// Fixed tags, always have a value. We overwrite mercilessly.
		for (Entry<String, String> tag : item.getTags().entrySet()) {
			String oldValue = currentValues.put(tag.getKey(), tag.getValue());
			if (oldValue != null && oldValue.length() > 0 && !oldValue.equals(tag.getValue())) replacedValue = true;
		}
		
		// Recommended tags, no fixed value is given. We add only those that do not already exist.
		for (Entry<String, String[]> tag : item.getRecommendedTags().entrySet()) {
			if (!currentValues.containsKey(tag.getKey())) currentValues.put(tag.getKey(), "");
		}
		
		loadEdits(currentValues);
		if (replacedValue) Toast.makeText(this, R.string.toast_preset_overwrote_tags, Toast.LENGTH_LONG).show();
		
		if (Main.getCurrentPreset() != null) Main.getCurrentPreset().putRecentlyUsed(item);
		recreateRecentPresetView();
	}
	
	/**
	 * Removes a preset from the MRU
	 * @param item the preset to apply
	 */
	private void removePresetFromMRU(PresetItem item) {
		
		if (Main.getCurrentPreset() != null) Main.getCurrentPreset().removeRecentlyUsed(item);
		recreateRecentPresetView();
	}
	
	/**
	 * Holds data sent in intents.
	 * Directly serializing a TreeMap in an intent does not work, as it comes out as a HashMap (?!?) 
	 * @author Jan
	 */
	public static class TagEditorData implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public final long osmId;
		public final String type;
		public final Map<String,String> tags;
		public final Map<String,String> originalTags;
		
		public TagEditorData(long osmId, String type, Map<String, String> tags, Map<String, String> originalTags) {
			this.osmId = osmId;
			this.type = type;
			this.tags = tags;
			this.originalTags = originalTags;
		}
		
		public TagEditorData(OsmElement selectedElement) {
			osmId = selectedElement.getOsmId();
			type = selectedElement.getName();
			tags = new LinkedHashMap<String, String>(selectedElement.getTags());
			originalTags = tags;
		}
	}
}
