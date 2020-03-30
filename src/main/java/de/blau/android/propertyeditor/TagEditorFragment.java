package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.exception.UiStateException;
import de.blau.android.javascript.EvalCallback;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Wiki;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.MRUTags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.PresetKeyType;
import de.blau.android.presets.Preset.UseLastAsDefault;
import de.blau.android.presets.Preset.ValueType;
import de.blau.android.presets.PresetCheckField;
import de.blau.android.presets.PresetCheckGroupField;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetField;
import de.blau.android.presets.PresetFieldJavaScript;
import de.blau.android.presets.PresetFixedField;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.ClipboardUtils;
import de.blau.android.util.GeoContext.Properties;
import de.blau.android.util.KeyValue;
import de.blau.android.util.Screen;
import de.blau.android.util.Snack;
import de.blau.android.util.StreetPlaceNamesAdapter;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Util;
import de.blau.android.views.CustomAutoCompleteTextView;

public class TagEditorFragment extends BaseFragment implements PropertyRows, EditorUpdate {
    private static final String DEBUG_TAG = TagEditorFragment.class.getSimpleName();

    private static final String RECENTPRESETS_FRAGMENT = "recentpresets_fragment";

    private static final String SAVEDTAGS_KEY = "SAVEDTAGS";

    private static final String IDS_KEY = "ids";

    private static final String TYPES_KEY = "types";

    private static final String DISPLAY_MRU_PRESETS = "displayMRUpresets";

    private static final String FOCUS_ON_KEY = "focusOnKey";

    private static final String APPLY_LAST_ADDRESS_TAGS = "applyLastAddressTags";

    private static final String EXTRA_TAGS_KEY = "extraTags";

    private static final String PRESETSTOAPPLY_KEY = "presetsToApply";

    private static final String TAGS_KEY = "tags";

    private static SelectedRowsActionModeCallback tagSelectedActionModeCallback = null;
    private static final Object                   actionModeCallbackLock        = new Object();

    private Names names = null;

    private boolean       loaded       = false;
    private String[]      types;
    private long[]        osmIds;
    private Preferences   prefs        = null;
    private OsmElement[]  elements     = null;
    private ElementType[] elementTypes = null;
    private ElementType   elementType  = null;

    private LayoutInflater inflater = null;

    private NameAdapters nameAdapters = null;

    /**
     * saves any changed fields on onPause
     */
    private LinkedHashMap<String, List<String>> savedTags = null;

    /**
     * per tag preset association
     */
    private Map<String, PresetItem> tags2Preset = new HashMap<>();

    /**
     * Best matching preset
     */
    private PresetItem primaryPresetItem = null;

    /**
     * further matching presets
     */
    private List<PresetItem> secondaryPresets = new ArrayList<>();

    private FormUpdate formUpdate;

    private PresetUpdate presetFilterUpdate;

    private PropertyEditorListener propertyEditorListener;

    private int maxStringLength; // maximum key, value and role length

    OnPresetSelectedListener presetSelectedListener;

    /**
     * Interface for handling the key:value pairs in the TagEditor.
     * 
     * @author Andrew Gregory
     */
    interface KeyValueHandler {
        /**
         * Handle the contents of a TagEditorRow
         * 
         * @param keyEdit the EditText holding the key
         * @param valueEdit the EditText holding the value
         * @param tagValues a List of tag values
         */
        void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final List<String> tagValues);
    }

    /**
     * Perform some processing for each key:value pair in the TagEditor.
     * 
     * @param handler The handler that will be called for each key:value pair.
     */
    private void processKeyValues(final KeyValueHandler handler) {
        LinearLayout rowLayout = (LinearLayout) getOurView();
        processKeyValues(rowLayout, handler);
    }

    /**
     * Perform some processing for each key:value pair in the TagEditor.
     * 
     * @param rowLayout the Layout holding the rows
     * @param handler The handler that will be called for each key:value pair.
     */
    private void processKeyValues(@NonNull LinearLayout rowLayout, @NonNull final KeyValueHandler handler) {
        final int size = rowLayout.getChildCount();
        for (int i = 0; i < size; ++i) {
            View view = rowLayout.getChildAt(i);
            TagEditRow row = (TagEditRow) view;
            handler.handleKeyValue(row.keyEdit, row.valueEdit, row.tagValues);
        }
    }

    /**
     * Create a new instance of TagEditorFragment
     * 
     * @param elementIds an array of the ids of OsmElements to edit
     * @param elementTypes an array of the types of OsmElements to edit
     * @param tags a map containing the tags
     * @param applyLastAddressTags if true apply address tags
     * @param focusOnKey a key to focus on
     * @param displayMRUpresets if true show the MRU presets view
     * @param extraTags additional tags to add
     * @param presetsToApply a list of presets to apply
     * @return a new instance of TagEditorFragment
     */
    public static TagEditorFragment newInstance(@NonNull long[] elementIds, @NonNull String[] elementTypes,
            @NonNull ArrayList<LinkedHashMap<String, String>> tags, boolean applyLastAddressTags, String focusOnKey, boolean displayMRUpresets,
            @Nullable HashMap<String, String> extraTags, @Nullable ArrayList<PresetElementPath> presetsToApply) {
        TagEditorFragment f = new TagEditorFragment();

        Bundle args = new Bundle();

        args.putSerializable(IDS_KEY, elementIds);
        args.putSerializable(TYPES_KEY, elementTypes);
        args.putSerializable(TAGS_KEY, tags);
        args.putSerializable(APPLY_LAST_ADDRESS_TAGS, applyLastAddressTags);
        args.putSerializable(FOCUS_ON_KEY, focusOnKey);
        args.putSerializable(DISPLAY_MRU_PRESETS, displayMRUpresets);
        args.putSerializable(EXTRA_TAGS_KEY, extraTags);
        args.putSerializable(PRESETSTOAPPLY_KEY, presetsToApply);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttachToContext(Context context) {
        Log.d(DEBUG_TAG, "onAttachToContext");
        try {
            nameAdapters = (NameAdapters) context;
            formUpdate = (FormUpdate) context;
            presetFilterUpdate = (PresetUpdate) context;
            propertyEditorListener = (PropertyEditorListener) context;
            presetSelectedListener = (OnPresetSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    context.toString() + " must implement PropertyEditorListener, NameAdapters, FormUpdate, PresetFilterUpdate, OnPresetSelectedListener");
        }
        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ScrollView rowLayout = null;

        boolean applyLastAddressTags = false;
        String focusOnKey = null;
        boolean displayMRUpresets = false;

        if (savedInstanceState == null) {
            // No previous state to restore - get the state from the intent
            Log.d(DEBUG_TAG, "Initializing from original arguments");
            osmIds = (long[]) getArguments().getSerializable(IDS_KEY);
            types = (String[]) getArguments().getSerializable(TYPES_KEY);
            applyLastAddressTags = (Boolean) getArguments().getSerializable(APPLY_LAST_ADDRESS_TAGS);
            focusOnKey = (String) getArguments().getSerializable(FOCUS_ON_KEY);
            displayMRUpresets = (Boolean) getArguments().getSerializable(DISPLAY_MRU_PRESETS);
        } else {
            // Restore activity from saved state
            Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
            osmIds = (long[]) savedInstanceState.getSerializable(IDS_KEY);
            types = (String[]) savedInstanceState.getSerializable(TYPES_KEY);
            @SuppressWarnings("unchecked")
            Map<String, ArrayList<String>> temp = (Map<String, ArrayList<String>>) savedInstanceState.getSerializable(SAVEDTAGS_KEY);
            savedTags = new LinkedHashMap<>();
            savedTags.putAll(temp);
        }

        prefs = App.getLogic().getPrefs();

        if (prefs.getEnableNameSuggestions()) {
            names = App.getNames(getActivity());
        }

        Server server = prefs.getServer();
        maxStringLength = server.getCachedCapabilities().getMaxStringLength();

        this.inflater = inflater;
        rowLayout = (ScrollView) inflater.inflate(R.layout.taglist_view, container, false);

        LinearLayout editRowLayout = (LinearLayout) rowLayout.findViewById(R.id.edit_row_layout);
        editRowLayout.setSaveEnabled(false);

        elements = new OsmElement[osmIds.length];
        elementTypes = new ElementType[osmIds.length];
        StorageDelegator delegator = App.getDelegator();
        for (int i = 0; i < elements.length; i++) {
            elements[i] = delegator.getOsmElement(types[i], osmIds[i]);
            elementTypes[i] = elements[i].getType();
        }
        // set the summary element type
        elementType = getSummaryElementType(elementTypes);

        // set the value header if we are in multi-select mode
        if (elements.length > 1) {
            TextView valueHeader = (TextView) rowLayout.findViewById(R.id.header_value);
            int headerRes = Screen.isLandscape(getActivity()) || Screen.isLarge(getActivity()) ? R.string.multiselect_header_long
                    : R.string.multiselect_header_short;
            valueHeader.setText(getString(headerRes, elements.length));
        }

        LinkedHashMap<String, List<String>> tags;
        if (savedTags != null) { // view was destroyed and needs to be recreated with current state
            Log.d(DEBUG_TAG, "Restoring from instance variable");
            tags = savedTags;
        } else {
            tags = buildEdits();
        }

        loaded = false;
        for (Entry<String, List<String>> pair : tags.entrySet()) {
            insertNewEdit(editRowLayout, pair.getKey(), pair.getValue(), -1, false);
        }

        loaded = true;
        TagEditRow row = ensureEmptyRow(editRowLayout);

        if (row != null && getUserVisibleHint()) { // don't request focus if we are not visible
            Log.d(DEBUG_TAG, "is visible");
            row.keyEdit.requestFocus();
            row.keyEdit.dismissDropDown();

            if (focusOnKey != null) {
                focusOnValue(editRowLayout, focusOnKey);
            } else {
                focusOnEmptyValue(editRowLayout); // probably never actually works
            }
        }
        //
        if (applyLastAddressTags) {
            loadEdits(editRowLayout,
                    Address.predictAddressTags(getActivity(), getType(), getOsmId(),
                            ((StreetPlaceNamesAdapter) nameAdapters.getStreetNameAdapter(null)).getElementSearch(), getKeyValueMap(editRowLayout, false),
                            Address.DEFAULT_HYSTERESIS),
                    false);
            if (getUserVisibleHint()) {
                if (!focusOnValue(editRowLayout, Tags.KEY_ADDR_HOUSENUMBER)) {
                    focusOnValue(editRowLayout, Tags.KEY_ADDR_STREET);
                } // this could be a bit more refined
            }
        }

        // Add any extra tags that were supplied
        @SuppressWarnings("unchecked")
        HashMap<String, String> extraTags = (HashMap<String, String>) getArguments().getSerializable(EXTRA_TAGS_KEY);
        if (extraTags != null) {
            for (Entry<String, String> e : extraTags.entrySet()) {
                addTag(editRowLayout, e.getKey(), e.getValue(), true, false);
            }
        }

        updateAutocompletePresetItem(editRowLayout, null, false); // set preset from initial tags

        if (displayMRUpresets) {
            Log.d(DEBUG_TAG, "Adding MRU prests");
            FragmentManager fm = getChildFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment recentPresetsFragment = fm.findFragmentByTag(RECENTPRESETS_FRAGMENT);
            if (recentPresetsFragment != null) {
                ft.remove(recentPresetsFragment);
            }

            recentPresetsFragment = RecentPresetsFragment.newInstance(elements[0].getOsmId(), elements[0].getName()); // FIXME
            ft.add(R.id.tag_mru_layout, recentPresetsFragment, RECENTPRESETS_FRAGMENT);
            ft.commit();
        }

        CheckBox headerCheckBox = (CheckBox) rowLayout.findViewById(R.id.header_tag_selected);
        headerCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    selectAllRows();
                } else {
                    deselectAllRows();
                }
            }
        });

        if (savedInstanceState == null) { // the following should only happen once on initial creation
            @SuppressWarnings("unchecked")
            List<PresetElementPath> presetsToApply = (ArrayList<PresetElementPath>) getArguments().getSerializable(PRESETSTOAPPLY_KEY);
            if (presetsToApply != null) {
                Preset preset = App.getCurrentRootPreset(getActivity());
                PresetGroup rootGroup = preset.getRootGroup();
                for (PresetElementPath pp : presetsToApply) {
                    PresetElement pi = Preset.getElementByPath(rootGroup, pp);
                    if (pi instanceof PresetItem) {
                        applyPreset(editRowLayout, (PresetItem) pi, false, true, true);
                    }
                }
            } else if (prefs.autoApplyPreset()) {
                PresetItem pi = getBestPreset();
                if (pi != null) {
                    if (pi.autoapply()) {
                        applyPreset(editRowLayout, pi, false, true, false);
                    } else {
                        Snack.toastTopWarning(getActivity(), R.string.toast_cant_autoapply_preset);
                    }
                }
            }
        }
        Log.d(DEBUG_TAG, "onCreateView returning");
        return rowLayout;
    }

    /**
     * Determine the ElementType for all edited elements
     * 
     * @param elementTypes an array of ElementTypes
     * 
     * @return an ElementType or null if multiple are in use
     */
    @Nullable
    private ElementType getSummaryElementType(ElementType[] elementTypes) {
        ElementType result = null;
        for (ElementType et : elementTypes) {
            if (result == null) {
                result = et;
            } else if (result != et) {
                return result;
            }
        }
        return result;
    }

    /**
     * Build the data structure we use to build the initial edit display
     * 
     * @return a map of String (the keys) and ArrayList&lt;String&gt; (the values)
     */
    private LinkedHashMap<String, List<String>> buildEdits() {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> originalTags = (ArrayList<Map<String, String>>) getArguments().getSerializable(TAGS_KEY);
        //
        LinkedHashMap<String, List<String>> tags = new LinkedHashMap<>();
        for (Map<String, String> map : originalTags) {
            for (Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!tags.containsKey(key)) {
                    tags.put(key, new ArrayList<>());
                }
                tags.get(key).add(value);
            }
        }
        // for those keys that don't have a value for each element add an empty string
        int l = originalTags.size();
        for (List<String> v : tags.values()) {
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
        prefs = App.getLogic().getPrefs(); // may have changed
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
        outState.putSerializable(IDS_KEY, osmIds);
        outState.putSerializable(TYPES_KEY, types);
        outState.putSerializable(SAVEDTAGS_KEY, savedTags);
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(DEBUG_TAG, "onPause");
        savedTags = getKeyValueMap(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        synchronized (actionModeCallbackLock) {
            if (tagSelectedActionModeCallback != null) {
                tagSelectedActionModeCallback.currentAction.finish();
                tagSelectedActionModeCallback = null;
            }
        }
    }

    /**
     * Creates edit rows from a SortedMap containing tags (as sequential key-value pairs)
     * 
     * Backwards compatible version
     * 
     * @param tags map containing the tags
     * @param flush flush existing tags
     */
    private void loadEditsSingle(@NonNull final Map<String, String> tags, boolean flush) {
        LinearLayout rowLayout = (LinearLayout) getOurView();
        LinkedHashMap<String, List<String>> convertedTags = flush ? new LinkedHashMap<>() : getKeyValueMap(true);
        for (Entry<String, String> entry : tags.entrySet()) {
            ArrayList<String> v = new ArrayList<>();
            v.add(entry.getValue());
            convertedTags.put(entry.getKey(), v);
        }
        loadEdits(rowLayout, convertedTags, false);
    }

    /**
     * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
     * 
     * @param tags the map containing the tags
     * @param applyDefaults apply default values if true
     */
    private void loadEdits(@NonNull final Map<String, List<String>> tags, boolean applyDefaults) {
        LinearLayout rowLayout = (LinearLayout) getOurView();
        loadEdits(rowLayout, tags, applyDefaults);
    }

    /**
     * Creates edits from a SortedMap containing tags (as sequential key-value pairs)
     * 
     * @param rowLayout the Layout holding the rows
     * @param tags the map containing the tags
     * @param applyDefaults apply default values if true
     */
    private void loadEdits(LinearLayout rowLayout, final Map<String, List<String>> tags, boolean applyDefaults) {
        if (rowLayout == null) {
            Log.e(DEBUG_TAG, "loadEdits rowLayout null");
            return;
        }
        loaded = false;
        rowLayout.removeAllViews();
        for (Entry<String, List<String>> pair : tags.entrySet()) {
            insertNewEdit(rowLayout, pair.getKey(), pair.getValue(), -1, applyDefaults);
        }
        loaded = true;
        ensureEmptyRow(rowLayout);
    }

    @Override
    public void updatePresets() {
        updateAutocompletePresetItem(null);
    }

    /**
     * Edits may change the best fitting preset
     * 
     * @param presetItem if null determine best preset from existing tags
     */
    void updateAutocompletePresetItem(@Nullable PresetItem presetItem) {
        LinearLayout rowLayout = (LinearLayout) getOurView();
        updateAutocompletePresetItem(rowLayout, presetItem, false);
    }

    /**
     * Edits may change the best fitting preset
     * 
     * @param presetItem if null determine best preset from existing tags
     * @param addToMru add to MRU if true
     */
    void updateAutocompletePresetItem(@Nullable PresetItem presetItem, boolean addToMru) {
        LinearLayout rowLayout = (LinearLayout) getOurView();
        updateAutocompletePresetItem(rowLayout, presetItem, addToMru);
    }

    /**
     * If tags have changed the autocomplete adapters need to be recalculated on what is the current preset
     * 
     * @param rowLayout the layout containing the tag rows
     * @param presetItem if null determine best preset from existing tags
     * @param addToMru add to MRU if true
     */
    private void updateAutocompletePresetItem(@Nullable LinearLayout rowLayout, @Nullable PresetItem presetItem, boolean addToMru) {
        Log.d(DEBUG_TAG, "setting new autocompletePresetItem");
        Preset[] presets = App.getCurrentPresets(getActivity());
        PresetItem savedPrimaryPresetItem = primaryPresetItem;
        List<PresetItem> savedSecondaryPresets = new ArrayList<>(getSecondaryPresets());

        Map<String, String> allTags = getKeyValueMapSingle(rowLayout, true);
        determinePresets(allTags, presetItem, presets);

        // update hints
        if (rowLayout != null) {
            for (int i = 0; i < rowLayout.getChildCount() - 1; i++) { // don't update empty row at end
                setHint((TagEditRow) rowLayout.getChildAt(i));
            }
        } else {
            Log.e(DEBUG_TAG, "updateAutocompletePresetItem called with null layout");
        }

        if (elements.length == 1) {
            // update element type for preset filter if necessary
            ElementType oldType = elementType;
            elementType = elements[0].getType(allTags);
            if (elementType != oldType) {
                presetFilterUpdate.update(elementType);
            }

            if (presets != null && addToMru) {
                if ((primaryPresetItem != null && !primaryPresetItem.equals(savedPrimaryPresetItem)) || !savedSecondaryPresets.equals(getSecondaryPresets())) {
                    List<PresetItem> items = new ArrayList<>(getSecondaryPresets());
                    items.add(primaryPresetItem);
                    for (PresetItem item : items) {
                        addToMru(presets, item);
                    }
                    ((PropertyEditor) getActivity()).recreateRecentPresetView();
                }
            }
        } else if (rowLayout != null) { // this might be too expensive
            LinkedHashMap<String, List<String>> edits = getKeyValueMap(rowLayout, false);
            Map<String, String> tags = new HashMap<>();
            for (int i = 0; i < elements.length; i++) {
                tags.clear();
                for (Entry<String, List<String>> entry : edits.entrySet()) {
                    List<String> values = entry.getValue();
                    if (values != null && values.size() > i) {
                        String value = values.get(i);
                        if (value != null && !"".equals(value)) {
                            tags.put(entry.getKey(), value);
                        }
                    }
                }
                elementTypes[i] = elements[i].getType(tags);
            }
            elementType = getSummaryElementType(elementTypes);
        }
    }

    /**
     * Determine the best presets for the existing tags
     * 
     * @param allTags the current tags
     * @param presetItem the current beest PresetItem or null
     * @param presets the current presets
     */
    private void determinePresets(@NonNull Map<String, String> allTags, @Nullable PresetItem presetItem, @NonNull Preset[] presets) {
        clearPresets();
        clearSecondaryPresets();
        if (presetItem == null) {
            primaryPresetItem = Preset.findBestMatch(presets, allTags, true); // FIXME multiselect;
        } else {
            primaryPresetItem = presetItem;
        }
        Map<String, String> nonAssigned = addPresetsToTags(primaryPresetItem, allTags);
        int nonAssignedCount = nonAssigned.size();
        while (nonAssignedCount > 0) {
            PresetItem nonAssignedPreset = Preset.findBestMatch(presets, nonAssigned, true);
            if (nonAssignedPreset == null) {
                // no point in continuing
                break;
            }
            addSecondaryPreset(nonAssignedPreset);
            nonAssigned = addPresetsToTags(nonAssignedPreset, nonAssigned);
            nonAssignedCount = nonAssigned.size();
        }
    }

    /**
     * Add item to most recently used list of preset items
     * 
     * @param presets array containing all currently used Presets
     * @param item the PresetItem to add to the MRU
     */
    void addToMru(Preset[] presets, PresetItem item) {
        for (Preset p : presets) {
            if (p != null && p.contains(item)) {
                p.putRecentlyUsed(item, null);
                break;
            }
        }
    }

    /**
     * Set a hint on a row, the hint value is retrieved from the Preset
     * 
     * @param row the row to set the hint on
     */
    private void setHint(TagEditRow row) {
        String aTagKey = row.getKey();
        PresetItem preset = getPreset(aTagKey);
        if (preset != null && !aTagKey.equals("")) { // set hints even if value isen't empty
            String hint = preset.getHint(aTagKey);
            if (hint != null) {
                row.valueEdit.setHint(hint);
            } else if (preset.getKeyType(aTagKey) != PresetKeyType.TEXT) {
                row.valueEdit.setHint(R.string.tag_autocomplete_value_hint);
            } else {
                row.valueEdit.setHint(R.string.tag_value_hint);
            }
            if (!row.same) {
                row.valueEdit.setHint(R.string.tag_multi_value_hint); // overwrite the above
            }
        }
    }

    /**
     * Create a mapping from tag keys to preset item and return those that coudn't be assigned
     * 
     * Tags that are in linked presets are assigned to that preset
     * 
     * @param preset PresetItem that we want to assign tags to
     * @param tags the tags we want to assign
     * @return map of tags that couldn't be assigned
     */
    private Map<String, String> addPresetsToTags(@Nullable PresetItem preset, @NonNull Map<String, String> tags) {
        Map<String, String> leftOvers = new LinkedHashMap<>();
        if (preset != null) {
            List<PresetItem> linkedPresetList = preset.getLinkedPresets(true, App.getCurrentPresets(getContext()));
            for (Entry<String, String> entry : tags.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                PresetField field = preset.getField(key);
                if (field instanceof PresetCheckGroupField) {
                    field = ((PresetCheckGroupField) field).getCheckField(key);
                } else if (field instanceof PresetFixedField) {
                    if (value != null && !value.equals(((PresetFixedField) field).getValue().getValue())) {
                        field = null; // fixed fields need to match both key and value
                    }
                }
                if (field != null) {
                    storePreset(key, preset);
                } else {
                    boolean found = false;
                    if (linkedPresetList != null) {
                        for (PresetItem linkedPreset : linkedPresetList) {
                            if (linkedPreset.getFixedTagCount() > 0) {
                                // fixed key presets should always count as themselves
                                continue;
                            }
                            PresetField linkedField = linkedPreset.getField(key);
                            if (linkedField instanceof PresetCheckGroupField) {
                                linkedField = ((PresetCheckGroupField) linkedField).getCheckField(key);
                            }
                            if (linkedField != null) {
                                storePreset(key, linkedPreset);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        leftOvers.put(key, value);
                    }
                }
            }
        } else {
            Log.e(DEBUG_TAG, "addPresetsToTags called with null preset");
        }
        return leftOvers;
    }

    /**
     * Get the PresetITem associated with a specific key
     * 
     * @param key the key
     * @return the PresetItem or null if not found
     */
    PresetItem getPreset(@NonNull String key) {
        return tags2Preset.get(key);
    }

    /**
     * Store the key to PresetItem mapping
     * 
     * @param key the key
     * @param preset the PresetItem
     */
    private void storePreset(@NonNull String key, @NonNull PresetItem preset) {
        tags2Preset.put(key, preset);
    }

    /**
     * Clear the key to PresetITem mapping
     */
    private void clearPresets() {
        tags2Preset.clear();
    }

    @Override
    public Map<String, PresetItem> getAllPresets() {
        return tags2Preset;
    }

    /**
     * Add a further PresetItem to the List of secondary PresetItems
     * 
     * @param nonAssignedPreset the PresetItem to add
     */
    private void addSecondaryPreset(@NonNull PresetItem nonAssignedPreset) {
        secondaryPresets.add(nonAssignedPreset);
    }

    /**
     * Clear the List of secondary PrestItems
     */
    private void clearSecondaryPresets() {
        secondaryPresets.clear();
    }

    @Override
    public List<PresetItem> getSecondaryPresets() {
        return secondaryPresets;
    }

    @Override
    public PresetItem getBestPreset() {
        return primaryPresetItem;
    }

    @Override
    public void predictAddressTags(boolean allowBlanks) {
        loadEdits(Address.predictAddressTags(getActivity(), getType(), getOsmId(),
                ((StreetPlaceNamesAdapter) nameAdapters.getStreetNameAdapter(null)).getElementSearch(), getKeyValueMap(allowBlanks),
                Address.DEFAULT_HYSTERESIS), false);
        updateAutocompletePresetItem(null);
    }

    /**
     * Get an Adapter for keys
     * 
     * @param preset the current best matching PresetItem or null
     * @param rowLayout the Layout holding the tag rows
     * @param keyEdit the AutoCompleteTextView the Adapter is for
     * @return an ArrayAdapter holding the key Strings
     */
    private ArrayAdapter<String> getKeyAutocompleteAdapter(@Nullable PresetItem preset, @NonNull LinearLayout rowLayout,
            @NonNull AutoCompleteTextView keyEdit) {
        // Use a set to prevent duplicate keys appearing
        Set<String> keys = new LinkedHashSet<>();

        Preset[] presets = propertyEditorListener.getPresets();
        if (preset == null && presets != null) {
            updateAutocompletePresetItem(rowLayout, null, false);
        }

        if (preset != null) {
            for (PresetField field : preset.getFields().values()) {
                if (field instanceof PresetCheckGroupField) {
                    keys.addAll(((PresetCheckGroupField) field).getKeys());
                } else {
                    keys.add(field.getKey());
                }
            }
        }

        keys.addAll(App.getMruTags().getKeys(elementType));

        if (presets != null) {
            List<String> allKeys = new ArrayList<>(Preset.getAutocompleteKeys(presets, elementType));
            Collections.sort(allKeys);
            keys.addAll(allKeys);
        }

        keys.removeAll(getUsedKeys(rowLayout, keyEdit));

        List<String> result = new ArrayList<>(keys);

        return new ArrayAdapter<>(getActivity(), R.layout.autocomplete_row, result);
    }

    /**
     * Return true if the edited object has an address or is a "highway"
     * 
     * @param key the key
     * @param usedKeys all currently in use keys
     * @return true if key is the name of a street
     */
    public static boolean isStreetName(String key, Set<String> usedKeys) {
        return (Tags.KEY_ADDR_STREET.equalsIgnoreCase(key) || (Tags.KEY_NAME.equalsIgnoreCase(key) && usedKeys.contains(Tags.KEY_HIGHWAY)));
    }

    /**
     * Return true if the edited object has an address or is a "place"
     * 
     * @param key the key
     * @param usedKeys all currently in use keys
     * @return true if key is the name of a place
     */
    public static boolean isPlaceName(String key, Set<String> usedKeys) {
        return (Tags.KEY_ADDR_PLACE.equalsIgnoreCase(key) || (Tags.KEY_NAME.equalsIgnoreCase(key) && usedKeys.contains(Tags.KEY_PLACE)));
    }

    /**
     * Return true if the edited object could have a name in the name index
     * 
     * @param usedKeys Set containing keys used by the object
     * @return true if the edited object could have a name in the name index
     */
    public static boolean useNameSuggestions(Set<String> usedKeys) {
        return !(usedKeys.contains(Tags.KEY_HIGHWAY) || usedKeys.contains(Tags.KEY_WATERWAY) || usedKeys.contains(Tags.KEY_LANDUSE)
                || usedKeys.contains(Tags.KEY_NATURAL) || usedKeys.contains(Tags.KEY_RAILWAY));
    }

    /**
     * Get an Adapter for value autocomplete
     * 
     * @param preset the PresetItem for row
     * @param rowLayout the Layout holding the rows
     * @param row the actual row
     * @return an ArrayAdapter holding the values or null
     */
    @Nullable
    private ArrayAdapter<?> getValueAutocompleteAdapter(@Nullable PresetItem preset, @NonNull LinearLayout rowLayout, @NonNull TagEditRow row) {
        ArrayAdapter<?> adapter = null;
        String key = row.getKey();
        if (key != null && key.length() > 0) {
            HashSet<String> usedKeys = (HashSet<String>) getUsedKeys(rowLayout, null);

            boolean hasTagValues = row.tagValues != null && row.tagValues.size() > 1;
            if (isStreetName(key, usedKeys)) {
                adapter = nameAdapters.getStreetNameAdapter(hasTagValues ? row.tagValues : null);
            } else if (isPlaceName(key, usedKeys)) {
                adapter = nameAdapters.getPlaceNameAdapter(hasTagValues ? row.tagValues : null);
            } else if (!hasTagValues && key.equals(Tags.KEY_NAME) && (names != null) && useNameSuggestions(usedKeys)) {
                Log.d(DEBUG_TAG, "generate suggestions for name from name suggestion index");
                List<NameAndTags> values = names.getNames(new TreeMap<>(getKeyValueMapSingle(rowLayout, true)), propertyEditorListener.getIsoCodes()); // FIXME
                if (values != null && !values.isEmpty()) {
                    Collections.sort(values);
                    adapter = new ArrayAdapter<>(getActivity(), R.layout.autocomplete_row, values);
                }
            } else if (Tags.isSpeedKey(key)) {
                // check if we have localized maxspeed values
                Properties prop = App.getGeoContext(getContext()).getProperties(propertyEditorListener.getIsoCodes());
                if (prop != null) {
                    String[] speedLimits = prop.getSpeedLimits();
                    if (speedLimits != null) {
                        adapter = new ArrayAdapter<>(getActivity(), R.layout.autocomplete_row, speedLimits);
                    }
                }
            } else {
                Map<String, Integer> counter = new HashMap<>();
                ArrayAdapter<ValueWithCount> adapter2 = new ArrayAdapter<>(getActivity(), R.layout.autocomplete_row);
                if (hasTagValues) {
                    for (String t : row.tagValues) {
                        if (t.equals("")) {
                            continue;
                        }
                        if (counter.containsKey(t)) {
                            counter.put(t, counter.get(t) + 1);
                        } else {
                            counter.put(t, 1);
                        }
                    }
                    List<String> keys = new ArrayList<>(counter.keySet());
                    Collections.sort(keys);
                    for (String t : keys) {
                        // FIXME determine description in some way
                        ValueWithCount v = new ValueWithCount(t, counter.get(t));
                        adapter2.add(v);
                    }
                }
                if (preset != null) {
                    List<String> mruValues = App.getMruTags().getValues(preset, key);
                    if (mruValues != null) {
                        for (String v : mruValues) {
                            adapter2.add(new ValueWithCount(v));
                            counter.put(v, 1);
                        }
                    }
                    Collection<StringWithDescription> values = preset.getAutocompleteValues(key);
                    Log.d(DEBUG_TAG, "setting autocomplete adapter for values " + values + " based on " + preset.getName());
                    if (values != null && !values.isEmpty()) {
                        List<StringWithDescription> result = new ArrayList<>(values);
                        if (preset.sortValues(key)) {
                            Collections.sort(result);
                        }
                        for (StringWithDescription s : result) {
                            if (counter != null && counter.containsKey(s.getValue())) {
                                continue; // skip stuff that is already listed
                            }
                            adapter2.add(new ValueWithCount(s.getValue(), s.getDescription()));
                        }
                        Log.d(DEBUG_TAG, "key " + key + " type " + preset.getKeyType(key));
                    } else if (preset.isFixedTag(key)) {
                        for (StringWithDescription s : Preset.getAutocompleteValues(((PropertyEditor) getActivity()).presets, elementType, key)) {
                            adapter2.add(new ValueWithCount(s.getValue(), s.getDescription()));
                        }
                    }
                } else if (propertyEditorListener.getPresets() != null) {
                    Log.d(DEBUG_TAG, "generate suggestions for >" + key + "< from presets"); // only do this if there is
                                                                                             // no other source of
                                                                                             // suggestions
                    List<String> mruValues = App.getMruTags().getValues(key);
                    if (mruValues != null) {
                        for (String v : mruValues) {
                            adapter2.add(new ValueWithCount(v));
                        }
                    }
                    for (StringWithDescription s : Preset.getAutocompleteValues(propertyEditorListener.getPresets(), elementType, key)) {
                        adapter2.add(new ValueWithCount(s.getValue(), s.getDescription()));
                    }
                } else if (adapter2.getCount() == 0) {
                    // FIXME shouldn't happen but seems to
                    Log.d(DEBUG_TAG, "no suggestions for values for >" + key + "<");
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
     * 
     * @param rowLayout the Layout the row will be added to
     * @param aTagKey the key-value to start with
     * @param tagValues a List of values to start with.
     * @param position the position where this should be inserted. set to -1 to insert at end, or 0 to insert at
     *            beginning.
     * @param applyDefault if true apply default values if they are present
     * @return The new TagEditRow.
     */
    private TagEditRow insertNewEdit(final LinearLayout rowLayout, final String aTagKey, final List<String> tagValues, final int position,
            boolean applyDefault) {
        final TagEditRow row = (TagEditRow) inflater.inflate(R.layout.tag_edit_row, rowLayout, false);

        boolean same = true;
        if (tagValues.size() > 1) {
            for (int i = 1; i < tagValues.size(); i++) {
                if (!tagValues.get(i - 1).equals(tagValues.get(i))) {
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
                if (Tags.KEY_ADDR_STREET.equals(parent.getItemAtPosition(position)) && row.getValue().length() == 0) {
                    ArrayAdapter<ValueWithCount> adapter = nameAdapters.getStreetNameAdapter(tagValues);
                    if (adapter != null && adapter.getCount() > 0) {
                        row.valueEdit.setText(adapter.getItem(0).getValue());
                    }
                } else if (Tags.KEY_ADDR_PLACE.equals(parent.getItemAtPosition(position)) && row.getValue().length() == 0) {
                    ArrayAdapter<ValueWithCount> adapter = nameAdapters.getPlaceNameAdapter(tagValues);
                    if (adapter != null && adapter.getCount() > 0) {
                        row.valueEdit.setText(adapter.getItem(0).getValue());
                    }
                } else {
                    if (primaryPresetItem != null) {
                        String hint = primaryPresetItem.getHint(parent.getItemAtPosition(position).toString());
                        if (hint != null) { //
                            row.valueEdit.setHint(hint);
                        } else if (!primaryPresetItem.getFields().isEmpty()) { // FIXME check if fixed fields don't
                                                                               // cause an issue here
                            row.valueEdit.setHint(R.string.tag_value_hint);
                        }
                        if (applyDefault && row.getValue().length() == 0) {
                            String defaultValue = primaryPresetItem.getDefault(parent.getItemAtPosition(position).toString());
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
                Log.d(DEBUG_TAG, "onFocusChange key");
                PresetItem preset = getPreset(aTagKey);
                if (hasFocus) {
                    originalKey = row.getKey();
                    row.keyEdit.setAdapter(getKeyAutocompleteAdapter(preset, rowLayout, row.keyEdit));
                    if (PropertyEditor.running && row.getKey().length() == 0) {
                        row.keyEdit.showDropDown();
                    }
                } else {
                    String newKey = row.getKey();
                    if (!newKey.equals(originalKey)) { // our preset may have changed re-calc
                        originalKey = newKey;
                        updateAutocompletePresetItem(rowLayout, null, true);
                    }
                }
            }
        });
        row.valueEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            String originalValue;

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(DEBUG_TAG, "onFocusChange value");
                String key = row.getKey();
                PresetItem preset = getPreset(key);
                final ValueType valueType = preset != null ? preset.getValueType(key) : null;
                if (hasFocus) {
                    originalValue = row.getValue();

                    final PresetKeyType keyType = preset != null ? preset.getKeyType(key) : null;
                    row.valueEdit.setAdapter(getValueAutocompleteAdapter(preset, rowLayout, row));
                    if (preset != null && keyType == PresetKeyType.MULTISELECT) {
                        // FIXME this should be somewhere better obvious since it creates a non obvious side effect
                        row.valueEdit.setTokenizer(new CustomAutoCompleteTextView.SingleCharTokenizer(preset.getDelimiter(key)));
                    }
                    if (Tags.isSpeedKey(key)) {
                        initMPHSpeed(getActivity(), row.valueEdit, propertyEditorListener);
                    } else if (keyType == PresetKeyType.TEXT && valueType == null) {
                        InputTypeUtil.enableTextSuggestions(row.valueEdit);
                    }
                    InputTypeUtil.setInputTypeFromValueType(row.valueEdit, valueType);

                    if (PropertyEditor.running) {
                        if (row.valueEdit.getText().length() == 0) {
                            row.valueEdit.showDropDown();
                        }
                    }
                } else {
                    // our preset may have changed re-calc
                    String newValue = row.getValue();
                    if (!newValue.equals(originalValue)) {
                        originalValue = newValue;
                        // potentially we should update tagValues here
                        updateAutocompletePresetItem(rowLayout, null, true);
                    }
                }
            }
        });

        /**
         * This TextWatcher reacts to previously empty cells being filled to add additional rows where needed add
         * removes any formatting and truncates to maximum supported API string length
         */
        TextWatcher textWatcher = new TextWatcher() {
            private boolean wasEmpty;
            private String  prevValue = null;

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

                Util.sanitizeString(getActivity(), s, maxStringLength);

                // update presets but only if value has changed
                String newValue = s.toString();
                if (prevValue == null || !prevValue.equals(newValue)) {
                    prevValue = newValue;
                    updateAutocompletePresetItem(rowLayout, null, true);
                }
            }
        };
        row.keyEdit.addTextChangedListener(textWatcher);
        row.valueEdit.addTextChangedListener(textWatcher);

        row.valueEdit.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("TagEdit", "onItemClicked value");
                Object o = parent.getItemAtPosition(position);
                if (o instanceof Names.NameAndTags) {
                    row.valueEdit.setOrReplaceText(((NameAndTags) o).getName());
                    applyTagSuggestions(((NameAndTags) o).getTags(), null);
                } else if (o instanceof ValueWithCount) {
                    row.valueEdit.setOrReplaceText(((ValueWithCount) o).getValue());
                } else if (o instanceof StringWithDescription) {
                    row.valueEdit.setOrReplaceText(((StringWithDescription) o).getValue());
                } else if (o instanceof String) {
                    row.valueEdit.setOrReplaceText((String) o);
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
     * A row representing an editable tag, consisting of edits for key and value, labels and a delete button. Needs to
     * be static, otherwise the inflater will not find it.
     * 
     * @author Jan
     */
    public static class TagEditRow extends LinearLayout implements SelectedRowsActionModeCallback.Row {

        private PropertyEditor             owner;
        private AutoCompleteTextView       keyEdit;
        private CustomAutoCompleteTextView valueEdit;
        private CheckBox                   selected;
        private List<String>               tagValues;
        private boolean                    same = true;

        /**
         * Construct a View holding the key and value for a tag
         * 
         * @param context an Android Context
         */
        public TagEditRow(Context context) {
            super(context);
            owner = (PropertyEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or
                                                                        // in Eclipse
        }

        /**
         * Construct a View holding the key and value for a tag
         * 
         * @param context an Android Context
         * @param attrs am AttributeSet
         */
        public TagEditRow(Context context, AttributeSet attrs) {
            super(context, attrs);
            owner = (PropertyEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or
                                                                        // in Eclipse
        }

        // public TagEditRow(Context context, AttributeSet attrs, int defStyle) {
        // super(context, attrs, defStyle);
        // owner = (TagEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or in
        // Eclipse
        // }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            keyEdit = (AutoCompleteTextView) findViewById(R.id.editKey);
            keyEdit.setOnKeyListener(PropertyEditor.myKeyListener);
            // lastEditKey.setSingleLine(true);

            valueEdit = (CustomAutoCompleteTextView) findViewById(R.id.editValue);
            valueEdit.setOnKeyListener(PropertyEditor.myKeyListener);

            selected = (CheckBox) findViewById(R.id.tagSelected);

            OnClickListener autocompleteOnClick = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.hasFocus()) {
                        ((AutoCompleteTextView) v).showDropDown();
                    }
                }
            };
            // set an empty adapter on both views to be on the safe side
            ArrayAdapter<String> empty = new ArrayAdapter<>(owner, R.layout.autocomplete_row, new String[0]);
            keyEdit.setAdapter(empty);
            valueEdit.setAdapter(empty);
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
            // keyEdit.setDropDownVerticalOffset(-h);
            // valueEdit.setDropDownVerticalOffset(-h);
            valueEdit.setParentWidth(w);
            //
        }

        /**
         * Sets key and value values
         * 
         * @param aTagKey the key value to set
         * @param tagValues List of values to set
         * @param same the values are all the same
         * @return the TagEditRow object for convenience
         */
        public TagEditRow setValues(String aTagKey, List<String> tagValues, boolean same) {
            keyEdit.setText(aTagKey);
            this.tagValues = tagValues;
            this.same = same;
            if (same) {
                if (tagValues != null && !tagValues.isEmpty()) {
                    valueEdit.setText(tagValues.get(0));
                } else {
                    valueEdit.setText("");
                }
            } else {
                valueEdit.setHint(R.string.tag_multi_value_hint);
            }
            return this;
        }

        /**
         * Get the current contents of the EditText for the key
         * 
         * @return the tag key as a String
         */
        @NonNull
        public String getKey() {
            return keyEdit.getText().toString();
        }

        /**
         * Get the current contents of the EditText for the tag value
         * 
         * @return the key as a String
         */
        @NonNull
        public String getValue() { // FIXME check if returning the textedit value is actually ok
            return valueEdit.getText().toString();
        }

        /**
         * Deletes this row
         */
        @Override
        public void delete() { // FIXME the references to owner.tagEditorFragemnt are likely suspect
            deleteRow((LinearLayout) owner.tagEditorFragment.getOurView());
        }

        /**
         * Deletes this row
         * 
         * @param rowLayout the Layout holding the rows
         */
        public void deleteRow(@NonNull LinearLayout rowLayout) {
            // FIXME the references to owner.tagEditorFragemnt are likely
            // suspect
            View cf = owner.getCurrentFocus();
            if (cf == keyEdit || cf == valueEdit) {
                // about to delete the row that has focus!
                // try to move the focus to the next row or failing that to the previous row
                int current = owner.tagEditorFragment.rowIndex(this);
                if (!owner.tagEditorFragment.focusRow(current + 1)) {
                    owner.tagEditorFragment.focusRow(current - 1);
                }
            }
            rowLayout.removeView(this);
            if (isEmpty() && owner.tagEditorFragment != null) {
                owner.tagEditorFragment.ensureEmptyRow(rowLayout);
            }
        }

        /**
         * Checks if the fields in this row are empty
         * 
         * @return true if both fields are empty, false if at least one is filled
         */
        public boolean isEmpty() {
            return keyEdit.getText().toString().trim().equals("") && valueEdit.getText().toString().trim().equals("");
        }

        @Override
        public boolean isSelected() {
            return selected.isChecked();
        }

        @Override
        public void deselect() {
            selected.setChecked(false);
        }

        /**
         * Disable the selection CheckBox
         */
        public void disableCheckBox() {
            selected.setEnabled(false);
        }

        /**
         * Enable the selection CheckBox
         */
        void enableCheckBox() {
            selected.setEnabled(true);
        }
    }

    @Override
    public void applyTagSuggestions(@NonNull Names.TagMap tags, @Nullable Runnable afterApply) {
        final LinkedHashMap<String, List<String>> currentValues = getKeyValueMap(true);
        boolean replacedValue = false;

        // Fixed tags, always have a value. We overwrite mercilessly.
        for (Entry<String, String> tag : tags.entrySet()) {
            List<String> oldValue = currentValues.put(tag.getKey(), Util.wrapInList(tag.getValue()));
            if (oldValue != null && !oldValue.isEmpty() && !oldValue.contains(tag.getValue())) {
                replacedValue = true;
            }
        }
        if (replacedValue) {
            Builder dialog = new AlertDialog.Builder(getActivity());
            dialog.setTitle(R.string.tag_editor_name_suggestion);
            dialog.setMessage(R.string.tag_editor_name_suggestion_overwrite_message);
            dialog.setPositiveButton(R.string.replace, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    loadEdits(currentValues, false);// FIXME
                    if (afterApply != null) {
                        afterApply.run();
                    }
                }
            });
            dialog.setNegativeButton(R.string.cancel, null);
            dialog.create().show();
        } else {
            loadEdits(currentValues, false);// FIXME
        }
        if (prefs.nameSuggestionPresetsEnabled()) {
            PresetItem p = Preset.findBestMatch(propertyEditorListener.getPresets(), getKeyValueMapSingle(false)); // FIXME
            if (p != null) {
                applyPreset((LinearLayout) getOurView(), p, false, false, true);
            }
        }
    }

    /**
     * Start the TagSelectedActionModeCallback
     */
    private void tagSelected() {
        LinearLayout rowLayout = (LinearLayout) getOurView();
        synchronized (actionModeCallbackLock) {
            if (tagSelectedActionModeCallback == null) {
                tagSelectedActionModeCallback = new TagSelectedActionModeCallback(this, rowLayout);
                ((AppCompatActivity) getActivity()).startSupportActionMode(tagSelectedActionModeCallback);
            }
        }
    }

    @Override
    public void deselectRow() {
        synchronized (actionModeCallbackLock) {
            if (tagSelectedActionModeCallback != null) {
                if (tagSelectedActionModeCallback.rowsDeselected(false)) {
                    tagSelectedActionModeCallback = null;
                }
            }
        }
    }

    @Override
    public void selectAllRows() { // select all tags
        LinearLayout rowLayout = (LinearLayout) getOurView();
        if (loaded) {
            int i = rowLayout.getChildCount();
            while (--i >= 0) {
                TagEditRow row = (TagEditRow) rowLayout.getChildAt(i);
                if (row.selected.isEnabled()) {
                    row.selected.setChecked(true);
                }
            }
        }
    }

    @Override
    public void deselectAllRows() { // deselect all tags
        LinearLayout rowLayout = (LinearLayout) getOurView();
        if (loaded) {
            int i = rowLayout.getChildCount();
            while (--i >= 0) {
                TagEditRow row = (TagEditRow) rowLayout.getChildAt(i);
                if (row.selected.isEnabled()) {
                    row.selected.setChecked(false);
                }
            }
        }
    }

    /**
     * Ensures that at least one empty row exists (creating one if needed)
     * 
     * @param rowLayout layout holding the rows
     * @return the first empty row found (or the one created), or null if loading was not finished (loaded == false),
     *         null if rowLayout is null
     */
    @Nullable
    private TagEditRow ensureEmptyRow(@Nullable LinearLayout rowLayout) {
        if (rowLayout == null) {
            return null;
        }
        TagEditRow ret = null;
        if (loaded) {
            int i = rowLayout.getChildCount();
            while (--i >= 0) {
                TagEditRow row = (TagEditRow) rowLayout.getChildAt(i);
                if (row != null) {
                    boolean isEmpty = row.isEmpty();
                    if (ret == null) {
                        ret = isEmpty ? row : insertNewEdit(rowLayout, "", new ArrayList<>(), -1, false);
                    } else if (isEmpty) {
                        row.deleteRow(rowLayout);
                    }
                } else {
                    Log.e(DEBUG_TAG, "ensureEmptyRow no row at position " + i);
                }
            }
            if (ret == null) {
                ret = insertNewEdit(rowLayout, "", new ArrayList<>(), -1, false);
            }
        }
        return ret;
    }

    /**
     * Focus on the value field of the first tag with non empty key and empty value
     * 
     * @return true if successful
     */
    boolean focusOnEmptyValue() {
        Log.d(DEBUG_TAG, "focusOnEmptyValue");
        LinearLayout rowLayout = (LinearLayout) getOurView();
        return focusOnEmptyValue(rowLayout);
    }

    /**
     * Focus on the first empty value field
     * 
     * @param rowLayout layout holding the rows
     * @return true if successful
     */
    private boolean focusOnEmptyValue(LinearLayout rowLayout) {
        boolean found = false;
        for (int i = 0; i < rowLayout.getChildCount(); i++) {
            TagEditRow ter = (TagEditRow) rowLayout.getChildAt(i);
            if (ter.getKey() != null && !ter.getKey().equals("") && ter.getValue().equals("")) {
                focusRowValue(rowLayout, rowIndex(rowLayout, ter));
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Move the focus to the key field of the specified row.
     * 
     * @param index The index of the row to move to, counting from 0.
     * @return true if the row was successfully focused, false otherwise.
     */
    private boolean focusRow(int index) {
        LinearLayout rowLayout = (LinearLayout) getOurView();
        TagEditRow row = (TagEditRow) rowLayout.getChildAt(index);
        return row != null && row.keyEdit.requestFocus();
    }

    /**
     * Move the focus to the value field of the specified row.
     * 
     * @param rowLayout the Layout holding the rows
     * @param index The index of the row to move to, counting from 0.
     * @return true if the row was successfully focused, false otherwise.
     */
    private boolean focusRowValue(@NonNull LinearLayout rowLayout, int index) {
        TagEditRow row = (TagEditRow) rowLayout.getChildAt(index);
        return row != null && row.valueEdit.requestFocus();
    }

    /**
     * Given a tag edit row, calculate its position.
     *
     * @param row The tag edit row to find.
     * @return The position counting from 0 of the given row, or -1 if it couldn't be found.
     */
    private int rowIndex(TagEditRow row) {
        LinearLayout rowLayout = (LinearLayout) getOurView();
        return rowIndex(rowLayout, row);
    }

    /**
     * Get the index of a specific row in the layout
     * 
     * @param rowLayout layout holding the rows
     * @param row row we want the index for
     * @return the index or -1 if not found
     */
    private int rowIndex(LinearLayout rowLayout, TagEditRow row) {
        for (int i = rowLayout.getChildCount() - 1; i >= 0; --i) {
            if (rowLayout.getChildAt(i) == row) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Focus on the value field of a tag with key "key"
     * 
     * @param rowLayout the Layout holding the rows
     * @param key key that we want to find the value field for
     * @return true if successful
     */
    private boolean focusOnValue(@NonNull LinearLayout rowLayout, String key) {
        boolean found = false;
        for (int i = rowLayout.getChildCount() - 1; i >= 0; --i) {
            TagEditRow ter = (TagEditRow) rowLayout.getChildAt(i);
            if (ter.getKey().equals(key)) {
                focusRowValue(rowLayout, rowIndex(rowLayout, ter));
                found = true;
                break;
            }
        }
        return found;
    }

    @Override
    public void applyPreset(PresetItem preset, boolean addOptional) {
        applyPreset((LinearLayout) getOurView(), preset, addOptional, true, true);
    }

    /**
     * Applies a preset (e.g. selected from the dialog or MRU), i.e. adds the tags from the preset to the current tag
     * set
     * 
     * @param item the preset item to apply
     * @param addOptional add optional tags if true
     * @param addToMRU add to preset MRU list if true
     */
    void applyPreset(@NonNull PresetItem item, boolean addOptional, boolean addToMRU) {
        applyPreset((LinearLayout) getOurView(), item, addOptional, addToMRU, true);
    }

    /**
     * Applies a preset (e.g. selected from the dialog or MRU), i.e. adds the tags from the preset to the current tag
     * set
     * 
     * @param rowLayout the layout holding the rows
     * @param item the preset item to apply
     * @param addOptional add optional tags if true
     * @param addToMRU add to preset MRU list if true
     * @param useDefaults use any default values specified in the preset
     */
    void applyPreset(@NonNull LinearLayout rowLayout, @NonNull PresetItem item, boolean addOptional, boolean addToMRU, boolean useDefaults) {
        LinkedHashMap<String, List<String>> currentValues = getKeyValueMap(rowLayout, true);
        boolean wasEmpty = currentValues.size() == 0;
        boolean replacedValue = false;

        Log.d(DEBUG_TAG, "applying preset " + item.getName());

        // remove everything that doesn't have a value
        // given that these are likely leftovers from a previous preset
        Set<String> keySet = new HashSet<>(currentValues.keySet()); // shallow copy
        for (String key : keySet) {
            List<String> list = currentValues.get(key);
            if (list == null || list.isEmpty()) {
                currentValues.remove(key);
            }
        }

        // Fixed tags, always have a value. We overwrite mercilessly.
        for (Entry<String, PresetFixedField> tag : item.getFixedTags().entrySet()) {
            PresetFixedField field = tag.getValue();
            String v = field.getValue().getValue();
            List<String> oldValue = currentValues.put(tag.getKey(), Util.wrapInList(v));
            if (oldValue != null && !oldValue.isEmpty() && !oldValue.contains(v) && !(oldValue.size() == 1 && "".equals(oldValue.get(0)))) {
                replacedValue = true;
            }
        }

        // add tags with no fixed values, optional tags only if addOptional is set
        Map<String, String> scripts = new LinkedHashMap<>();
        for (Entry<String, PresetField> entry : item.getFields().entrySet()) {
            PresetField field = entry.getValue();
            boolean isOptional = field.isOptional();
            if (!isOptional || (isOptional && addOptional)) {
                if (field instanceof PresetCheckGroupField) {
                    for (PresetCheckField check : ((PresetCheckGroupField) field).getCheckFields()) {
                        addTagFromPreset(item, check, currentValues, check.getKey(), scripts, useDefaults);
                    }
                } else if (!(field instanceof PresetFixedField)) {
                    addTagFromPreset(item, field, currentValues, entry.getKey(), scripts, useDefaults);
                }
            }
        }

        if (!scripts.isEmpty()) {
            Preset[] presets = App.getCurrentPresets(getActivity());
            determinePresets(getKeyValueMapSingle(true), null, presets);
            for (Entry<String, String> entry : scripts.entrySet()) {
                evalJavaScript(item, currentValues, entry.getKey(), entry.getValue());
            }
        }

        loadEdits(rowLayout, currentValues, true);
        if (replacedValue) {
            Snack.barWarning(getActivity(), R.string.toast_preset_overwrote_tags);
        }

        // re-determine best preset
        updateAutocompletePresetItem(rowLayout, null, addToMRU);

        // only focus on an empty field if we are actually being shown
        if (propertyEditorListener != null && propertyEditorListener.onTop(this)) {
            focusOnEmptyValue(rowLayout);
        }
    }

    /**
     * Evaluate JavaScript
     * 
     * @param item the current PresetItem
     * @param currentValues current Tag values
     * @param key the key
     * @param script the script
     */
    private void evalJavaScript(@NonNull PresetItem item, @NonNull Map<String, List<String>> currentValues, @NonNull String key, @NonNull String script) {
        try {
            String defaultValue = item.getDefault(key) == null ? "" : item.getDefault(key);
            for (Entry<String, PresetItem> entry : tags2Preset.entrySet()) {
                Log.e(DEBUG_TAG, "evalJavaScript " + entry.getKey() + " " + (entry.getValue() != null ? entry.getValue().getName() : " null"));
            }
            String result = de.blau.android.javascript.Utils.evalString(getActivity(), " " + key, script, buildEdits(), currentValues, defaultValue,
                    tags2Preset, App.getCurrentPresets(getActivity()));
            if (result == null || "".equals(result)) {
                currentValues.remove(key);
            } else if (currentValues.containsKey(key)) {
                currentValues.put(key, Util.wrapInList(result));
            }
        } catch (Exception ex) {
            Snack.barError(getActivity(), ex.getLocalizedMessage());
        }
    }

    /**
     * Add tag from preset if the key isn't already present
     * 
     * If an entry in the MRU tags exists and UseLastAsDefault is TRUE or FORCE or a default value in the preset this
     * will be set as the value.
     * 
     * If evaluating the JS returns null, the key is removed.
     * 
     * @param item current Preset
     * @param field the current PresetField
     * @param tags map of current tags
     * @param key the key we are processing
     * @param scripts map containing any JS we find
     * @param useDefault use any default value if true
     * @return true if a value was set
     */
    private boolean addTagFromPreset(@NonNull PresetItem item, @Nullable PresetField field, @NonNull Map<String, List<String>> tags, @NonNull String key,
            Map<String, String> scripts, boolean useDefault) {
        if (!tags.containsKey(key)) {
            String value = "";
            if (field != null && useDefault) {
                String defaultValue = field.getDefaultValue();
                if (defaultValue != null) {
                    value = defaultValue;
                }
                UseLastAsDefault useLastAsDefault = field.getUseLastAsDefault();
                if (useLastAsDefault == UseLastAsDefault.TRUE || useLastAsDefault == UseLastAsDefault.FORCE) {
                    MRUTags mruTags = App.getMruTags();
                    String topValue = mruTags.getTopValue(item, key);
                    if (topValue != null) {
                        value = topValue;
                    }
                }
            }
            if (field instanceof PresetFieldJavaScript && scripts != null) {
                String script = ((PresetFieldJavaScript) field).getScript();
                if (script != null) {
                    scripts.put(key, script);
                }
            }
            tags.put(key, Util.wrapInList(value));
            return value != null && !"".equals(value);
        }
        return false;
    }

    /**
     * Merge a set of tags in to the current ones
     * 
     * @param newTags the new tags to merge
     */
    private void mergeTags(@NonNull Map<String, String> newTags) {
        LinkedHashMap<String, List<String>> currentValues = getKeyValueMap(true);

        boolean replacedValue = false;

        // Fixed tags, always have a value. We overwrite mercilessly.
        for (Entry<String, String> tag : newTags.entrySet()) {
            List<String> oldValue = currentValues.put(tag.getKey(), Util.wrapInList(tag.getValue()));
            if (oldValue != null && !oldValue.isEmpty() && !oldValue.contains(tag.getValue())) {
                replacedValue = true;
            }
        }

        loadEdits(currentValues, false);
        if (replacedValue) {
            Snack.barWarning(getActivity(), R.string.toast_preset_overwrote_tags);
        }
        focusOnEmptyValue();
    }

    /**
     * Merge a set of tags in to the current ones, with potentially empty keys
     * 
     * @param newTags the new tags to merge
     * @param replace overwrite values of existing keys
     */
    private void mergeTags(@NonNull List<KeyValue> newTags, boolean replace) {
        LinkedHashMap<String, List<String>> currentValues = getKeyValueMap(true);
        Map<String, KeyValue> keyIndex = new HashMap<>(); // needed for de-duping

        List<KeyValue> keysAndValues = new ArrayList<>();
        for (Entry<String, List<String>> entry : currentValues.entrySet()) {
            String key = entry.getKey();
            KeyValue keyValue = new KeyValue(key, entry.getValue());
            keysAndValues.add(keyValue);
            keyIndex.put(key, keyValue);
        }

        boolean replacedValue = false;

        //
        for (KeyValue tag : newTags) {
            KeyValue keyValue = keyIndex.get(tag.getKey());
            if (keyValue != null && replace) { // exists
                keyValue.setValue(tag.getValue());
                replacedValue = true;
            } else {
                keysAndValues.add(new KeyValue(tag.getKey(), tag.getValue()));
            }
        }

        // this code needs to be duplicated because we can't use a map here
        LinearLayout rowLayout = (LinearLayout) getOurView();
        loaded = false;
        rowLayout.removeAllViews();
        for (KeyValue keyValue : keysAndValues) {
            insertNewEdit(rowLayout, keyValue.getKey(), keyValue.getValues(), -1, false);
        }
        loaded = true;
        ensureEmptyRow(rowLayout);

        if (replacedValue) {
            Snack.barWarning(getActivity(), R.string.toast_preset_overwrote_tags);
        }
        focusOnEmptyValue();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.tag_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // disable address tagging for stuff that won't have an address
        // menu.findItem(R.id.tag_menu_address).setVisible(!type.equals(Way.NAME) ||
        // element.hasTagKey(Tags.KEY_BUILDING));
        menu.findItem(R.id.tag_menu_mapfeatures).setEnabled(propertyEditorListener.isConnectedOrConnecting());
        menu.findItem(R.id.tag_menu_paste).setEnabled(!App.getTagClipboard(getContext()).isEmpty());
        menu.findItem(R.id.tag_menu_paste_from_clipboard).setEnabled(pasteFromClipboardIsPossible());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            ((PropertyEditor) getActivity()).sendResultAndFinish();
            return true;
        case R.id.tag_menu_address:
            predictAddressTags(false);
            return true;
        case R.id.tag_menu_sourcesurvey:
            doSourceSurvey();
            return true;
        case R.id.tag_menu_apply_preset:
        case R.id.tag_menu_apply_preset_with_optional:
            PresetItem pi = Preset.findBestMatch(propertyEditorListener.getPresets(), getKeyValueMapSingle(false)); // FIXME
            if (pi != null) {
                presetSelectedListener.onPresetSelected(pi, item.getItemId() == R.id.tag_menu_apply_preset_with_optional);
            }
            return true;
        case R.id.tag_menu_paste:
            paste(true);
            return true;
        case R.id.tag_menu_paste_from_clipboard:
            pasteFromClipboard(true);
            return true;
        case R.id.tag_menu_revert:
            doRevert();
            return true;
        case R.id.tag_menu_mapfeatures:
            Wiki.displayMapFeatures(getActivity(), prefs, getBestPreset());
            return true;
        case R.id.tag_menu_resetMRU:
            for (Preset p : ((PropertyEditor) getActivity()).presets) {
                if (p != null) {
                    p.resetRecentlyUsed();
                }
            }
            ((PropertyEditor) getActivity()).recreateRecentPresetView();
            return true;
        case R.id.tag_menu_reset_address_prediction:
            // simply overwrite with an empty file
            Address.resetLastAddresses(getActivity());
            return true;
        case R.id.tag_menu_js_console:
            de.blau.android.javascript.Utils.jsConsoleDialog(getActivity(), R.string.js_console_msg_debug, new EvalCallback() {
                @Override
                public String eval(String input) {
                    return de.blau.android.javascript.Utils.evalString(getActivity(), "JS Preset Test", input, buildEdits(), getKeyValueMap(true), "test",
                            tags2Preset, App.getCurrentPresets(getActivity()));
                }
            });
            return true;
        case R.id.tag_menu_select_all:
            selectAllRows();
            return true;
        case R.id.tag_menu_help:
            HelpViewer.start(getActivity(), R.string.help_propertyeditor);
            return true;
        default:
            // do nothing
        }
        return false;
    }

    /**
     * Collect all key-value pairs into a LinkedHashMap&gt;String,String&lt;
     * 
     * @param allowBlanks If true, includes key-value pairs where one or the other is blank.
     * @return The LinkedHashMap&gt;String,String&lt; of key-value pairs.
     */
    private LinkedHashMap<String, List<String>> getKeyValueMap(final boolean allowBlanks) {
        return getKeyValueMap((LinearLayout) getOurView(), allowBlanks);
    }

    /**
     * Collect all key-value pairs into a LinkedHashMap&gt;String,String&lt;
     * 
     * @param rowLayout the Layout holding the rows
     * @param allowBlanks If true, includes key-value pairs where one or the other is blank.
     * @return The LinkedHashMap&gt;String,String&lt; of key-value pairs.
     */
    private LinkedHashMap<String, List<String>> getKeyValueMap(LinearLayout rowLayout, final boolean allowBlanks) {

        final LinkedHashMap<String, List<String>> tags = new LinkedHashMap<>();

        if (rowLayout == null && savedTags != null) {
            return savedTags;
        }

        if (rowLayout != null) {
            processKeyValues(rowLayout, new KeyValueHandler() {
                @Override
                public void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final List<String> tagValues) {
                    String key = keyEdit.getText().toString().trim();
                    String value = valueEdit.getText().toString().trim();
                    boolean keyBlank = "".equals(key);
                    boolean valueBlank = "".equals(value);
                    boolean bothBlank = keyBlank && valueBlank;
                    boolean neitherBlank = !keyBlank && !valueBlank;
                    if (!bothBlank) {
                        // both blank is never acceptable
                        if (neitherBlank || allowBlanks || (valueBlank && tagValues != null && !tagValues.isEmpty())) {
                            if (valueBlank) {
                                tags.put(key, tagValues.size() == 1 ? Util.wrapInList("") : tagValues);
                            } else {
                                tags.put(key, Util.wrapInList(value));
                            }
                        }
                    }
                }
            });
        } else {
            Log.e(DEBUG_TAG, "rowLayout null in getKeyValueMapSingle");
        }
        return tags;
    }

    /**
     * Return the current tags as a Map, single element only, if multiple are selected only the tags of the first will
     * be returned
     * 
     * @param allowBlanks allow blank keys of values (not both)
     * @return a LinkedHashMap of the current tags
     */
    @NonNull
    public LinkedHashMap<String, String> getKeyValueMapSingle(final boolean allowBlanks) {
        LinearLayout rowLayout = (LinearLayout) getOurView();
        return getKeyValueMapSingle(rowLayout, allowBlanks);
    }

    /**
     * Return the current tags as a Map, single element only, if multiple are selected only the tags of the first will
     * be returned
     * 
     * @param rowLayout the Layout holding the rows
     * @param allowBlanks allow blank keys of values (not both)
     * @return a LinkedHashMap of the current tags
     */
    @NonNull
    private LinkedHashMap<String, String> getKeyValueMapSingle(LinearLayout rowLayout, final boolean allowBlanks) {
        final LinkedHashMap<String, String> tags = new LinkedHashMap<>();
        if (rowLayout == null && savedTags != null) {
            for (Entry<String, List<String>> entry : savedTags.entrySet()) {
                String key = entry.getKey().trim();
                List<String> tagValues = entry.getValue();
                String value = tagValues != null && !tagValues.isEmpty() ? (tagValues.get(0) != null ? tagValues.get(0) : "") : "";
                boolean valueBlank = "".equals(value);
                boolean bothBlank = "".equals(key) && valueBlank;
                boolean neitherBlank = !"".equals(key) && !valueBlank;
                if (!bothBlank) {
                    // both blank is never acceptable
                    if (neitherBlank || allowBlanks || valueBlank) {
                        if (valueBlank) {
                            tags.put(key, tagValues == null || tagValues.size() == 1 ? "" : tagValues.get(0)); // FIXME
                                                                                                               // if
                                                                                                               // multi-select
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
                public void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final List<String> tagValues) {
                    String key = keyEdit.getText().toString().trim();
                    String value = valueEdit.getText().toString().trim();
                    boolean valueBlank = "".equals(value);
                    boolean bothBlank = "".equals(key) && valueBlank;
                    boolean neitherBlank = !"".equals(key) && !valueBlank;
                    if (!bothBlank) {
                        // both blank is never acceptable
                        boolean hasValues = tagValues != null && !tagValues.isEmpty();
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
            Log.e(DEBUG_TAG, "rowLayout null in getKeyValueMapSingle");
        }
        return tags;
    }

    /**
     * Given an OSM key value, determine it's corresponding source key. For example, the source of "name" is
     * "source:name". The source of "source" is "source". The source of "mf:name" is "mf.source:name".
     * 
     * @param key the key to be sourced.
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
     * Add a source:key field and pre-fill it with "survey"
     */
    private void doSourceSurvey() { // FIXME
        // determine the key (if any) that has the current focus in the key or its value
        final String[] focusedKey = new String[] { null }; // array to work around unsettable final
        processKeyValues(new KeyValueHandler() {
            @Override
            public void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final List<String> tagValues) {
                if (keyEdit.isFocused() || valueEdit.isFocused()) {
                    focusedKey[0] = keyEdit.getText().toString().trim();
                }
            }
        });
        // ensure source(:key)=survey is tagged
        final String sourceKey = sourceForKey(focusedKey[0]);
        final boolean[] sourceSet = new boolean[] { false }; // array to work around unsettable final
        processKeyValues(new KeyValueHandler() {
            @Override
            public void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final List<String> tagValues) {
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
            ArrayList<String> v = new ArrayList<>();
            v.add(Tags.VALUE_SURVEY);
            insertNewEdit((LinearLayout) getOurView(), sourceKey, v, -1, false);
        }
    }

    @Override
    public boolean paste(boolean replace) {
        Map<String, String> copiedTags = App.getTagClipboard(getContext()).paste();
        if (copiedTags != null) {
            mergeTags(copiedTags);
        }
        updateAutocompletePresetItem(null);
        return copiedTags != null;
    }

    @Override
    public boolean pasteFromClipboardIsPossible() {
        return ClipboardUtils.checkForText(getActivity());
    }

    @Override
    public boolean pasteFromClipboard(boolean replace) {
        List<KeyValue> paste = ClipboardUtils.getKeyValues(getActivity());
        if (paste != null) {
            mergeTags(paste, replace);
        }
        updateAutocompletePresetItem(null);
        return paste != null;
    }

    /**
     * Copy tags to internal and system clipboard
     * 
     * @param tags the tags
     */
    public void copyTags(@NonNull Map<String, String> tags) {
        App.getTagClipboard(getContext()).copy(tags);
        ClipboardUtils.copyTags(getActivity(), tags);
    }

    /**
     * reload original arguments
     */
    void doRevert() {
        loadEdits(buildEdits(), false);
        updateAutocompletePresetItem(null);
    }

    /**
     * Get the OSM id of the (first if multiple) element currently edited by the editor
     * 
     * This is only used for address prediction
     * 
     * @return the OSM id
     */
    public long getOsmId() {
        return osmIds[0];
    }

    /**
     * Get the type of the (first if multiple) element currently edited by the editor
     * 
     * This is only used for address prediction
     * 
     * @return the type
     */
    public String getType() {
        return types[0];
    }

    /**
     * Get all key values currently in the editor, optionally skipping one field.
     * 
     * @param rowLayout the Layout holding the rows
     * @param ignoreEdit optional - if not null, this key field will be skipped, i.e. the key in it will not be included
     *            in the output
     * @return the set of all (or all but one) keys currently entered in the edit boxes
     */
    private Set<String> getUsedKeys(LinearLayout rowLayout, final EditText ignoreEdit) {
        final HashSet<String> keys = new HashSet<>();
        processKeyValues(rowLayout, new KeyValueHandler() {
            @Override
            public void handleKeyValue(final EditText keyEdit, final EditText valueEdit, final List<String> tagValues) {
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
     * 
     * @return the row container view
     */
    @NonNull
    private View getOurView() {
        // android.support.v4.app.NoSaveStateFrameLayout
        View v = getView();
        if (v != null) {
            if (v.getId() == R.id.edit_row_layout) {
                Log.d(DEBUG_TAG, "got correct view in getView");
                return v;
            } else {
                v = v.findViewById(R.id.edit_row_layout);
                if (v == null) {
                    Log.d(DEBUG_TAG, "didn't find R.id.edit_row_layout");
                    throw new UiStateException("didn't find R.id.edit_row_layout");
                } else {
                    Log.d(DEBUG_TAG, "Found R.id.edit_row_layout");
                }
                return v;
            }
        } else {
            // given that this is always fatal might as well throw the exception here
            Log.d(DEBUG_TAG, "got null view in getView");
            throw new UiStateException("got null view in getView");
        }
    }

    /**
     * Enable the MRU list of Presets
     */
    public void enableRecentPresets() {
        FragmentManager fm = getChildFragmentManager();
        Fragment recentPresetsFragment = fm.findFragmentByTag(PropertyEditor.RECENTPRESETS_FRAGMENT);
        if (recentPresetsFragment != null) {
            ((RecentPresetsFragment) recentPresetsFragment).enable();
        }
    }

    /**
     * Disable the MRU list of Presets
     */
    public void disableRecentPresets() {
        FragmentManager fm = getChildFragmentManager();
        Fragment recentPresetsFragment = fm.findFragmentByTag(PropertyEditor.RECENTPRESETS_FRAGMENT);
        if (recentPresetsFragment != null) {
            ((RecentPresetsFragment) recentPresetsFragment).disable();
        }
    }

    /**
     * Update the original list of tags to reflect edits
     * 
     * Note this will silently remove tags with empty key or value
     * 
     * @return list of maps containing the tags
     */
    public List<LinkedHashMap<String, String>> getUpdatedTags() {
        @SuppressWarnings("unchecked")

        List<Map<String, String>> oldTags = (ArrayList<Map<String, String>>) getArguments().getSerializable(TAGS_KEY);
        // make a (nearly) full copy
        List<LinkedHashMap<String, String>> newTags = new ArrayList<>();
        for (Map<String, String> map : oldTags) {
            newTags.add(new LinkedHashMap<>(map));
        }

        LinkedHashMap<String, List<String>> edits = getKeyValueMap(true);
        if (edits == null) {
            // if we didn't get a LinkedHashMap as input we need to copy
            List<LinkedHashMap<String, String>> newOldTags = new ArrayList<>();
            for (Map<String, String> map : oldTags) {
                newOldTags.add(new LinkedHashMap<>(map));
            }
            return newOldTags;
        }

        for (LinkedHashMap<String, String> map : newTags) {
            for (String key : new TreeSet<>(map.keySet())) {
                if (edits.containsKey(key)) {
                    List<String> valueList = edits.get(key);
                    if (valueList.size() == 1) { // if more than one value, we haven't changed anything in multi-select
                        String value = valueList.get(0).trim();
                        String oldValue = map.get(key);
                        Log.e(DEBUG_TAG, "new " + value + " old " + oldValue);
                        if (!value.equals(oldValue)) {
                            if (saveTag(key, value)) {
                                addTagToResult(map, key, value);
                            } else {
                                map.remove(key); // zap stuff with empty values or just the HTTP prefix
                            }
                        }
                    }
                } else { // key deleted
                    map.remove(key);
                }
            }
            // check for new tags
            for (Entry<String, List<String>> entry : edits.entrySet()) {
                String editsKey = entry.getKey();
                List<String> valueList = entry.getValue();
                if (editsKey != null && !"".equals(editsKey) && !map.containsKey(editsKey) && valueList.size() == 1) {
                    String value = valueList.get(0).trim();
                    if (saveTag(editsKey, value)) {
                        addTagToResult(map, editsKey, value);
                    }
                }
            }
        }
        return newTags;
    }

    /**
     * Check if we should keep this
     * 
     * @param key string containing the key
     * @param value string containing the value
     * @return true if the value isn't only a pre-filled in prefix or suffix
     */
    private boolean saveTag(@NonNull String key, String value) {
        return !onlyWebsitePrefix(key, value) && !onlyMphSuffix(key, value);
    }

    /**
     * Check if this is a website tag with the protocol prefix in it
     * 
     * @param key key of the tag
     * @param value value of the tag
     * @return true if this is only http:// or https://
     */
    boolean onlyWebsitePrefix(@NonNull String key, @Nullable String value) {
        PresetItem pi = getPreset(key);
        return (Tags.isWebsiteKey(key) || (pi != null && ValueType.WEBSITE == pi.getValueType(key)))
                && (Tags.HTTP_PREFIX.equalsIgnoreCase(value) || Tags.HTTPS_PREFIX.equalsIgnoreCase(value));
    }

    /**
     * Check if this is a speed tag that only contains MPH
     * 
     * @param key key of the tag
     * @param value value of the tag
     * @return true if this is only MPH
     */
    boolean onlyMphSuffix(@Nullable String key, @Nullable String value) {
        return Tags.isSpeedKey(key) && Tags.MPH.trim().equalsIgnoreCase(value);
    }

    /**
     * Add tag if it doesn't exist
     * 
     * @param rowLayout layout holding the rows
     * @param key key of the tag
     * @param value value of the tag
     * @param replace if true replace existing key values, otherwise don't
     * @param update if true update the the best matched presets
     */
    private void addTag(LinearLayout rowLayout, @NonNull String key, @NonNull String value, boolean replace, boolean update) {
        Log.d(DEBUG_TAG, "adding tag " + key + "=" + value);
        LinkedHashMap<String, List<String>> currentValues = getKeyValueMap(rowLayout, true);
        if (!currentValues.containsKey(key) || replace) {
            currentValues.put(key, Util.wrapInList(value));
            loadEdits(rowLayout, currentValues, false);
            if (update) {
                updateAutocompletePresetItem(null);
            }
        }
    }

    @Override
    public void updateSingleValue(@NonNull String key, @NonNull String value) {
        LinkedHashMap<String, List<String>> currentValues = getKeyValueMap(true);
        currentValues.put(key, Util.wrapInList(value));
        loadEdits(currentValues, false);
        updateAutocompletePresetItem(null);
    }

    @Override
    public void updateTags(Map<String, String> tags, boolean flush) {
        loadEditsSingle(tags, flush);
        updateAutocompletePresetItem(null);
    }

    @Override
    public void revertTags() {
        doRevert();
    }

    @Override
    public void deleteTag(@Nullable final String key) {
        LinearLayout l = (LinearLayout) getOurView();
        if (l != null) {
            for (int i = l.getChildCount() - 1; i >= 0; --i) {
                TagEditRow ter = (TagEditRow) l.getChildAt(i);
                if (ter.getKey().equals(key)) {
                    ter.delete();
                    break;
                }
            }
        }
    }

    /**
     * Add (or remove if empty) key-value to a map stripping trailing list separator, and as a side effect add to the
     * mru tags
     * 
     * @param result target map
     * @param key string containing the key
     * @param value string containing the value
     */
    private void addTagToResult(@NonNull Map<String, String> result, @NonNull String key, @NonNull String value) {
        PresetItem pi = getPreset(key);
        MRUTags mruTags = App.getMruTags();
        boolean nonEmptyValue = !"".equals(value);
        if (pi != null) {
            PresetField field = pi.getField(key);
            boolean useLastAsDefault = field != null && field.getUseLastAsDefault() != UseLastAsDefault.FALSE;
            if (pi.getKeyType(key) == PresetKeyType.MULTISELECT) {
                // trim potential trailing separators
                char delimter = pi.getDelimiter(key);
                if (value.endsWith(String.valueOf(delimter))) {
                    value = value.substring(0, value.length() - 1);
                }
                List<String> values = Preset.splitValues(Util.wrapInList(value), pi, key);
                if (values != null) {
                    Collections.reverse(values);
                    for (String v : values) {
                        if (!"".equals(v) || useLastAsDefault) {
                            mruTags.put(pi, key, v);
                        }
                    }
                }
            } else if (nonEmptyValue || useLastAsDefault) {
                mruTags.put(pi, key, value);
            }
        } else if (nonEmptyValue) {
            mruTags.put(key, value);
        }
        // actually save the result
        if (nonEmptyValue) {
            result.put(key, value);
        } else {
            result.remove(key);
        }
    }

    /**
     * Add http:// to empty EditTexts that are supposed to contain a website and set input mode
     * 
     * @param valueEdit the EditText holding the value
     */
    public static void initWebsite(@NonNull final EditText valueEdit) {
        valueEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        if (valueEdit.getText().length() == 0) {
            valueEdit.setText(Tags.HTTP_PREFIX);
            valueEdit.setSelection(Tags.HTTP_PREFIX.length());
        }
    }

    /**
     * Add mph to empty EditTexts that are supposed to contain speed and are in relevant country and set input mode
     * 
     * @param ctx Android Context
     * @param valueEdit the EditTExt holding the value
     * @param listener callback to the activity
     */
    public static void initMPHSpeed(@NonNull Context ctx, @NonNull final AutoCompleteTextView valueEdit, @NonNull PropertyEditorListener listener) {
        valueEdit.setInputType(InputType.TYPE_CLASS_TEXT);
        ListAdapter adapter = valueEdit.getAdapter();
        Properties prop = App.getGeoContext(ctx).getProperties(listener.getIsoCodes());
        if (valueEdit.getText().length() == 0 && (adapter == null || adapter.getCount() == 0) && (prop != null && prop.imperialUnits())) {
            // in the case of multi-select/ there is no guarantee that this makes sense
            valueEdit.setText(Tags.MPH);
            valueEdit.setSelection(0);
        }
    }
}
