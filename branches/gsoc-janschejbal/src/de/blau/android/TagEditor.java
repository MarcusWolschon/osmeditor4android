package de.blau.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences.Editor;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.PresetDialog;
import de.blau.android.presets.StreetTagValueAutocompletionAdapter;
import de.blau.android.presets.TagKeyAutocompletionAdapter;
import de.blau.android.presets.TagValueAutocompletionAdapter;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;

/**
 * An Activity to edit OSM-Tags. Sends the edited Tags as Result to its caller-Activity (normally {@link Main}).
 * 
 * @author mb
 */
public class TagEditor extends Activity implements OnDismissListener {

	// TODO autosuggest based on extended preset properties
	// TODO persistent saving for MRU
	
	public static final String TAGS = "tags";
	public static final String TAGS_ORIG = "tags_original";

	public static final String TYPE = "type";

	public static final String OSM_ID = "osm_id";

	/** The layout containing the entire editor */
	private LinearLayout verticalLayout = null;
	
	/** The layout containing the edit rows */
	private LinearLayout rowLayout = null;

	/**
	 * The tag we use for Android-logging.
	 */
//    @SuppressWarnings("unused")
	private static final String DEBUG_TAG = TagEditor.class.getName();

	private static final String PREF_LAST_TAG = "tagEditor.lastTagSet";

	private long osmId;

	private String type;

	/**
	 * Handles "enter" key presses.
	 */
	private final OnKeyListener myKeyListener = new MyKeyListener();

	/** Set to true once values are loaded. used to suppress adding of empty rows while loading. */
	private boolean loaded;
	
	private PresetDialog presetDialog;
	
	/**
	 * The tags present when this editor was created (for undoing changes)
	 */
	private ArrayList<String> originalTags;


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
	 * Ensures that at least one empty row exists
	 */
	private void ensureEmptyRow() {
		if (!loaded) return;
		final int size = rowLayout.getChildCount();
		for (int i = 0; i < size; ++i) {
			View view = rowLayout.getChildAt(i);
			TagEditRow row = (TagEditRow)view;
			if (row.isEmpty()) return;
		}
		// no empty rows found, make one
		insertNewEdit("", "", false);
	}
	
	@SuppressWarnings("unchecked")
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
		loaded = false;
		if (savedInstanceState == null) {
			// No previous state to restore - get the state from the intent
			osmId = getIntent().getLongExtra(OSM_ID, 0);
			type = getIntent().getStringExtra(TYPE);
			originalTags = (ArrayList<String>)getIntent().getSerializableExtra(TAGS);
			loadEdits(originalTags);
		} else {
			// Restore activity from saved state
			osmId = savedInstanceState.getLong(OSM_ID, 0);
			type = savedInstanceState.getString(TYPE);
			loadEdits(savedInstanceState.getStringArrayList(TAGS));
			originalTags = savedInstanceState.getStringArrayList(TAGS_ORIG);
		}
		
		loaded = true;
		ensureEmptyRow();
		
		createSourceSurveyButton();
		createApplyPresetButton();
		createRepeatLastButton();
		createRevertButton();
		createOkButton();
		
		createRecentPresetView();
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
	
	/**
	 * Create a source=survey button for tagging keys as "survey".
	 * Tapping the button will set (creating a new key/value if they don't exist)
	 * "source:key=survey", where key is the key of the currently focused key/value.
	 * If the key of the currently focused key/value is blank or "source", then the
	 * plain "source" key is used.
	 * For example, if you were editing the "name" key, then this would add
	 * "source:name=survey". On the other hand, if you had a blank key/value field
	 * focused, or were editing an existing "source" key/value, then "source=survey"
	 * would be set.
	 */
	private void createSourceSurveyButton() {
		Button sourcesurveyButton = (Button) findViewById(R.id.sourcesurveyButton);
		sourcesurveyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
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
					insertNewEdit(sourceKey, "survey", false);
				}
			}
		});
	}
	
	private void createApplyPresetButton() {
		Button presetButton = (Button) findViewById(R.id.applyPresetButton);
		presetButton.setEnabled(Main.currentPreset != null);
		if (Main.currentPreset == null) return;
		
		presetButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				showPresetDialog();
			}
		});
	}

	private void createRepeatLastButton() {
		Button button = (Button) findViewById(R.id.repeatLastButton);

		final String last = PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_LAST_TAG, null);
		button.setEnabled(last != null);
		if (last == null) return;
		
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				rowLayout.removeAllViews();
				loadEdits(last);					
			}
		});

	}

	private void createRevertButton() {
		Button button = (Button) findViewById(R.id.revertButton);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				rowLayout.removeAllViews();
				loadEdits(originalTags);
			}
		});
	}
	
	private void createOkButton() {
		Button okButton = (Button) findViewById(R.id.okButton);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				sendResultAndFinish();
			}
		});
	}
	
	private void createRecentPresetView() {
		if (Main.currentPreset == null) return;
		
		ElementType filterType = Main.logic.delegator.getOsmElement(getType(), getOsmId()).getType();
		View v = Main.currentPreset.getRecentPresetView(new PresetClickHandler() {
			
			@Override
			public void onItemClick(PresetItem item) {
				applyPreset(item);
			}
			
			@Override
			public void onGroupClick(PresetGroup group) {
				// should not have groups
			}
		},filterType);
		v.setBackgroundColor(0x80000000);
		v.setPadding(20, 20, 20, 20);
		v.setId(R.id.recentPresets);
		MarginLayoutParams p = new MarginLayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		p.setMargins(10, 10, 10, 10);
		verticalLayout.addView(v);
	}

	private void recreateRecentPresetView() {
		View currentView = verticalLayout.findViewById(R.id.recentPresets);
		if (currentView != null) verticalLayout.removeView(currentView);
		createRecentPresetView();
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tag_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.tag_menu_mapfeatures:
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_mapfeatures)));
			startActivity(intent);
			return true;
		}

		return false;
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			sendResultAndFinish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * 
	 */
	protected void sendResultAndFinish() {
		// Save current tags for "repeat last" button
		Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
		prefEdit.putString(PREF_LAST_TAG,getKeyValueString(false)).apply();
		
		Intent intent = new Intent();
		intent.putExtras(getKeyValueBundle(false)); // discards blank or partially blank pairs
		intent.putExtra(OSM_ID, osmId);
		intent.putExtra(TYPE, type);
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Creates edits from an List containing tags (as sequential key-value pairs)
	 */
	protected void loadEdits(final List<String> tags) {
		loaded = false;
		for (int i = 0, size = tags.size(); i < size-1; i += 2) {
			insertNewEdit(tags.get(i), tags.get(i + 1), false);
		}
		loaded = true;
		ensureEmptyRow();
	}
	
	/**
	 * Creates edits from a String containing newline-separated sequential key-value pairs
	 */
	protected void loadEdits(String tags) {
		if (tags.isEmpty()) {
			ensureEmptyRow();
			return;
		}
		String[] tagArray = tags.split("\n");
		loadEdits(Arrays.asList(tagArray));
	}

	/** Save the state of this activity instance for future restoration.
	 * @param outState The object to receive the saved state.
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		outState.putLong(OSM_ID, osmId);
		outState.putString(TYPE, type);
		outState.putStringArrayList(TAGS_ORIG, originalTags);
		outState.putAll(getKeyValueBundle(true)); // save partially blank pairs too
		super.onSaveInstanceState(outState);
	}

	/**
	 * Insert a new row with one key and one value to edit.
	 * 
	 * @param aTagKey the key-value to start with
	 * @param aTagValue the value to start with.
	 */
	protected void insertNewEdit(final String aTagKey, final String aTagValue, final boolean atStart) {
		TagEditRow row = (TagEditRow)View.inflate(this, R.layout.tag_edit_row, null);
		row.setValues(aTagKey, aTagValue);
		rowLayout.addView(row, atStart? 0 : rowLayout.getChildCount());

	}
	
	public static class TagEditRow extends LinearLayout {

		private TagEditor owner;
		private AutoCompleteTextView keyEdit;
		private AutoCompleteTextView valueEdit;
		
		public TagEditRow(Context context) {
			super(context);
			owner = (TagEditor) context; // Can only be instantiated inside TagEditor
		}

		public TagEditRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			owner = (TagEditor) context; // Can only be instantiated inside TagEditor
		}

		public TagEditRow(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			owner = (TagEditor) context; // Can only be instantiated inside TagEditor
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
						
			keyEdit = (AutoCompleteTextView)findViewById(R.id.editKey);
			keyEdit.setOnKeyListener(owner.myKeyListener);
			//lastEditKey.setSingleLine(true);
			ArrayAdapter<String> knownTagNamesAdapter;
			try {
				knownTagNamesAdapter = new TagKeyAutocompletionAdapter(owner,
						android.R.layout.simple_dropdown_item_1line, owner.type);
			} catch (Exception e) {
				Log.e(DEBUG_TAG, "cannot create TagKeyAutocompletionAdapter", e);
				knownTagNamesAdapter = new ArrayAdapter<String>(owner, android.R.layout.simple_dropdown_item_1line,
						getResources().getStringArray(R.array.known_tags));
			}
			keyEdit.setAdapter(knownTagNamesAdapter);

			
			valueEdit = (AutoCompleteTextView)findViewById(R.id.editValue);
			valueEdit.setOnKeyListener(owner.myKeyListener);

			// change auto-completion -values for the tag-value if the tag-key changes.
			//TODO: if the rule is <combo only provide a list
			keyEdit.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(final CharSequence aS, final int aStart, final int aBefore, final int aCount) {
					setAutocompletion();
				}

				@Override
				public void beforeTextChanged(final CharSequence aS, final int aStart, final int aCount, final int aAfter) {
					setAutocompletion();
				}

				@Override
				public void afterTextChanged(final Editable aS) {
					setAutocompletion();
				}

				/**
				 * add an adapter to valueEdit, that gives autocompletion-suggestions based on the value of
				 * keyEdit.getText().toString().
				 */
				private void setAutocompletion() {
					ArrayAdapter<String> knownTagValuesAdapter = null;
					String tagKey = keyEdit.getText().toString();
					try {
						Bundle tags = owner.getKeyValueBundle(false);
						if ((Main.logic != null && Main.logic.delegator != null) &&
								(tagKey.equalsIgnoreCase("addr:street") ||
								(tagKey.equalsIgnoreCase("name") && bundleContainsTagKey(tags, "highway")))) {
							knownTagValuesAdapter = new StreetTagValueAutocompletionAdapter(owner,
									android.R.layout.simple_dropdown_item_1line, Main.logic.delegator, owner.type, owner.osmId);
							valueEdit.setThreshold(0);
							valueEdit.setAdapter(knownTagValuesAdapter);
							// auto-select the nearest street unless the user already entered sth.
							if (valueEdit.getText().toString().length() == 0 && knownTagValuesAdapter.getCount() > 0) {
								valueEdit.setText(knownTagValuesAdapter.getItem(0));
							}
							valueEdit.performCompletion();
						} else {
							knownTagValuesAdapter = new TagValueAutocompletionAdapter(owner,
									android.R.layout.simple_dropdown_item_1line, tagKey);
							valueEdit.setThreshold(1);
							valueEdit.setAdapter(knownTagValuesAdapter);
						}
					} catch (Exception e) {
						Log.e(DEBUG_TAG, "cannot create TagValueAutocompletionAdapter forkey \"" + tagKey + "\"", e);
						knownTagValuesAdapter = new ArrayAdapter<String>(owner,
								android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(
									R.array.known_tags));
						valueEdit.setAdapter(knownTagValuesAdapter);
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

			
			TextWatcher emptyWatcher = new TextWatcher() {
				private boolean wasEmpty;
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// nop
				}
				
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					wasEmpty = isEmpty(); // TagEditorRow.isEmpty()
				}
				
				@Override
				public void afterTextChanged(Editable s) {
					if (wasEmpty && s.length() > 0) {
						owner.ensureEmptyRow();
					}
				}
			};
			keyEdit.addTextChangedListener(emptyWatcher);
			valueEdit.addTextChangedListener(emptyWatcher);
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
			owner.rowLayout.removeView(this);
			if (isEmpty()) {
				owner.ensureEmptyRow();
			}
		}
		
		/**
		 * Checks if the fields in this row are empty
		 * @return true if both fields are empty, true if at least one is filled
		 */
		public boolean isEmpty() {
			return keyEdit.getText().toString().trim().isEmpty()
					&& valueEdit.getText().toString().trim().isEmpty();
		}

	}

	/**
	 * Collect all key-value pairs into a bundle to return them.
	 * 
	 * @param allowBlanks If true, includes key-value pairs where one or the other is blank.
	 * @return The bundle of key-value pairs.
	 */
	private Bundle getKeyValueBundle(final boolean allowBlanks) {
		final Bundle bundle = new Bundle(1);
		final ArrayList<String> tags = getKeyValueStringList(allowBlanks);
		bundle.putSerializable(TAGS, tags);
		return bundle;
	}
	
	/**
	 * Collect all key-value pairs into a single string
	 * 
	 * @param allowBlanks If true, includes key-value pairs where one or the other is blank.
	 * @return A string representing the key-value pairs
	 */
	private String getKeyValueString(final boolean allowBlanks) {
		final ArrayList<String> tags = getKeyValueStringList(allowBlanks);
		StringBuilder b = new StringBuilder();
		boolean empty = true;
		for (String entry : tags) {
			if (!empty) {
				b.append('\n');
			} else {
				empty = false;
			}
			b.append(entry);
		}
		return b.toString();
	}

	/**
	 * Collect all key-value pairs into an ArrayList<String>
	 * 
	 * @param allowBlanks If true, includes key-value pairs where one or the other is blank.
	 * @return The ArrayList<String> of key-value pairs.
	 */
	private ArrayList<String> getKeyValueStringList(final boolean allowBlanks) {
		final ArrayList<String> tags = new ArrayList<String>();
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
						tags.add(key);
						tags.add(value);
					}
				}
			}
		});
		return tags;
	}
	
	private static boolean bundleContainsTagKey(Bundle b, String key) {
		ArrayList<String> tags = b.getStringArrayList(TAGS);
		if (tags != null) {
			while (!tags.isEmpty()) {
				String tag = tags.remove(0);
				if (tag.equals(key)) {
					return true;
				}
				if (!tags.isEmpty()) {
					tags.remove(0); // value
				}
			}
		}
		return false;
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

	public long getOsmId() {
		return osmId;
	}

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
		if (Main.currentPreset == null) return;
		OsmElement element = Main.logic.delegator.getOsmElement(getType(), getOsmId());
		presetDialog = new PresetDialog(this, Main.currentPreset, element);
		presetDialog.setOnDismissListener(this);
		presetDialog.show();
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


	private void applyPreset(PresetItem item) {
		for (Entry<String, String> tag : item.getTags().entrySet()) {
			insertNewEdit(tag.getKey(), tag.getValue(), true);
		}
		if (Main.currentPreset != null) Main.currentPreset.putRecentlyUsed(item);
		recreateRecentPresetView();
	}
}
