package de.blau.android.propertyeditor;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
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
import android.content.res.Resources;
import android.os.Bundle;
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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.address.Address;
import de.blau.android.dialogs.ConsoleDialog;
import de.blau.android.dialogs.ElementInfo;
import de.blau.android.dialogs.EvalCallback;
import de.blau.android.exception.DuplicateKeyException;
import de.blau.android.exception.UiStateException;
import de.blau.android.nsi.Names;
import de.blau.android.nsi.Names.NameAndTags;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.osm.Wiki;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.MRUTags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetCheckField;
import de.blau.android.presets.PresetCheckGroupField;
import de.blau.android.presets.PresetComboField;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetField;
import de.blau.android.presets.PresetFieldJavaScript;
import de.blau.android.presets.PresetFixedField;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetItemLink;
import de.blau.android.presets.PresetKeyType;
import de.blau.android.presets.PresetTagField;
import de.blau.android.presets.UseLastAsDefaultType;
import de.blau.android.presets.ValueType;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ArrayAdapterWithRuler;
import de.blau.android.util.ClipboardUtils;
import de.blau.android.util.GeoContext.Properties;
import de.blau.android.util.KeyValue;
import de.blau.android.util.Screen;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.StreetPlaceNamesAdapter;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.util.Value;
import de.blau.android.views.CustomAutoCompleteTextView;

public class TagEditorFragment extends SelectableRowsFragment implements PropertyRows, EditorUpdate, DataUpdate {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, TagEditorFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = TagEditorFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String SAVEDTAGS_KEY           = "SAVEDTAGS";
    private static final String IDS_KEY                 = "ids";
    private static final String TYPES_KEY               = "types";
    private static final String DISPLAY_MRU_PRESETS     = "displayMRUpresets";
    private static final String FOCUS_ON_KEY            = "focusOnKey";
    private static final String APPLY_LAST_ADDRESS_TAGS = "applyLastAddressTags";
    private static final String EXTRA_TAGS_KEY          = "extraTags";
    private static final String PRESETSTOAPPLY_KEY      = "presetsToApply";
    private static final String TAGS_KEY                = "tags";

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

    PresetUpdate presetFilterUpdate;

    private PropertyEditorListener propertyEditorListener;

    private int maxStringLength; // maximum key, value and role length

    OnPresetSelectedListener presetSelectedListener;

    private final class Ruler extends ValueWithCount {

        /**
         * Create a new Ruler
         */
        public Ruler() {
            super("");
        }
    }

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
        for (int i = 0; i < size; i++) {
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
    @NonNull
    public static <T extends List<Map<String, String>> & Serializable, M extends Map<String, String> & Serializable, L extends List<PresetElementPath> & Serializable> TagEditorFragment newInstance(
            @NonNull long[] elementIds, @NonNull String[] elementTypes, @NonNull T tags, boolean applyLastAddressTags, String focusOnKey,
            boolean displayMRUpresets, @Nullable M extraTags, @Nullable L presetsToApply) {
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
        Fragment parent = getParentFragment();
        Util.implementsInterface(parent, NameAdapters.class, FormUpdate.class, PresetUpdate.class, PropertyEditorListener.class,
                OnPresetSelectedListener.class);
        nameAdapters = (NameAdapters) parent;
        formUpdate = (FormUpdate) parent;
        presetFilterUpdate = (PresetUpdate) parent;
        propertyEditorListener = (PropertyEditorListener) parent;
        presetSelectedListener = (OnPresetSelectedListener) parent;
        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        boolean applyLastAddressTags = false;
        boolean displayMRUpresets = false;
        String focusOnKey = null;

        if (savedInstanceState == null) {
            // No previous state to restore - get the state from the intent
            Log.d(DEBUG_TAG, "Initializing from original arguments");
            osmIds = Util.getSerializeable(getArguments(), IDS_KEY, long[].class);
            types = Util.getSerializeable(getArguments(), TYPES_KEY, String[].class);
            applyLastAddressTags = getArguments().getBoolean(APPLY_LAST_ADDRESS_TAGS, false);
            displayMRUpresets = getArguments().getBoolean(DISPLAY_MRU_PRESETS, false);
            focusOnKey = Util.getSerializeable(getArguments(), FOCUS_ON_KEY, String.class);
        } else {
            // Restore activity from saved state
            Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
            osmIds = Util.getSerializeable(savedInstanceState, IDS_KEY, long[].class);
            types = Util.getSerializeable(savedInstanceState, TYPES_KEY, String[].class);
            @SuppressWarnings("unchecked")
            Map<String, ArrayList<String>> temp = (Map<String, ArrayList<String>>) Util.getSerializeable(savedInstanceState, SAVEDTAGS_KEY, Serializable.class);
            if (temp != null) {
                savedTags = new LinkedHashMap<>();
                savedTags.putAll(temp);
            } else {
                Log.e(DEBUG_TAG, "saved state, but no saved tags");
            }
        }

        prefs = App.getLogic().getPrefs();

        if (prefs.getEnableNameSuggestions()) {
            names = App.getNames(getActivity());
        }

        Server server = prefs.getServer();
        maxStringLength = server.getCachedCapabilities().getMaxStringLength();

        this.inflater = inflater;
        LinearLayout rowLayout = (LinearLayout) inflater.inflate(R.layout.taglist_view, container, false);
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
            tags = getTagsInEditForm();
        }

        loaded = false;
        for (Entry<String, List<String>> pair : tags.entrySet()) {
            insertNewEdit(editRowLayout, pair.getKey(), pair.getValue(), -1, false);
        }

        loaded = true;

        // Add any extra tags that were supplied
        @SuppressWarnings("unchecked")
        Map<String, String> extraTags = Util.getSerializeable(getArguments(), EXTRA_TAGS_KEY, HashMap.class);
        if (extraTags != null && !extraTags.isEmpty()) {
            for (Entry<String, String> e : extraTags.entrySet()) {
                addTag(editRowLayout, e.getKey(), e.getValue(), true, false);
            }
        }

        if (displayMRUpresets) {
            // FIXME this is arguably wrong for multiselect
            de.blau.android.propertyeditor.Util.addMRUPresetsFragment(getChildFragmentManager(), R.id.mru_layout, elements[0].getOsmId(),
                    elements[0].getName());
        }

        CheckBox headerCheckBox = (CheckBox) rowLayout.findViewById(R.id.header_tag_selected);
        headerCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectAllRows();
            } else {
                deselectAllRows();
            }
        });

        if (savedInstanceState == null) { // the following should only happen once on initial creation
            List<PresetElementPath> presetsToApply = Util.getSerializeableArrayList(getArguments(), PRESETSTOAPPLY_KEY, PresetElementPath.class);
            if (Util.notEmpty(presetsToApply)) {
                FragmentActivity activity = getActivity();
                Preset preset = App.getCurrentRootPreset(activity);
                PresetGroup rootGroup = preset.getRootGroup();
                for (PresetElementPath pp : presetsToApply) {
                    // can't use the listener here as onAttach will not have happened
                    PresetElement pi = Preset.getElementByPath(rootGroup, pp, propertyEditorListener.getIsoCodes(), false);
                    if (pi instanceof PresetItem) {
                        applyPreset(editRowLayout, (PresetItem) pi, false, false, true, Prefill.PRESET);
                    }
                }
                updateAutocompletePresetItem(editRowLayout, null, false); // here after preset has been applied
            } else {
                updateAutocompletePresetItem(editRowLayout, null, false); // here before preset has been applied
                PresetItem pi = getBestPreset();
                if (elements.length == 1 && prefs.autoApplyPreset() && pi != null) {
                    if (pi.autoapply()) {
                        applyPreset(editRowLayout, pi, prefs.applyWithOptionalTags(getContext(), pi), false, true, Prefill.NEVER);
                    } else {
                        ScreenMessage.toastTopWarning(getActivity(), R.string.toast_cant_autoapply_preset);
                    }
                }
            }
        } else {
            updateAutocompletePresetItem(editRowLayout, null, false);
        }
        // this needs to happen -after- the preset has been applied
        if (applyLastAddressTags) {
            loadEdits(editRowLayout,
                    Address.predictAddressTags(getActivity(), getType(), getOsmId(),
                            ((StreetPlaceNamesAdapter) nameAdapters.getStreetNameAdapter(null)).getElementSearch(), getKeyValueMap(editRowLayout, false),
                            Address.DEFAULT_HYSTERESIS, true),
                    false);
        }
        final TagEditRow row = ensureEmptyRow(editRowLayout);

        if (isVisible()) { // don't request focus if we are not visible
            Log.d(DEBUG_TAG, "is visible setting focus");
            // all of this is only done on initial creation
            if (applyLastAddressTags) {
                if (!focusOnValue(editRowLayout, Tags.KEY_ADDR_HOUSENUMBER)) {
                    focusOnValue(editRowLayout, Tags.KEY_ADDR_STREET);
                } // this could be a bit more refined
            } else if (focusOnKey != null) {
                focusOnValue(editRowLayout, focusOnKey);
            } else if (savedInstanceState == null) {
                row.keyEdit.requestFocus();
                row.keyEdit.dismissDropDown();
            }
        }

        Log.d(DEBUG_TAG, "onCreateView returning");
        return rowLayout;
    }

    @Override
    public void onDataUpdate() {
        Log.d(DEBUG_TAG, "onDataUpdate");
        List<Map<String, String>> currentTags = new ArrayList<>();
        for (OsmElement e : elements) {
            currentTags.add(new LinkedHashMap<>(e.getTags()));
        }
        if (!currentTags.equals(propertyEditorListener.getOriginalTags())) {
            // simple case as we don't have to check for deleted elements
            ScreenMessage.toastTopInfo(getContext(), R.string.toast_updating_tags);
            loadEdits(getTagsInEditForm(currentTags), false);
            formUpdate.tagsUpdated();
        }
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
     * Build the data structure we use to build the edit display
     * 
     * @return a map of String (the keys) and ArrayList&lt;String&gt; (the values)
     */
    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, List<String>> getTagsInEditForm() {
        return getTagsInEditForm((ArrayList<Map<String, String>>) Util.getSerializeable(getArguments(), TAGS_KEY, Serializable.class));
    }

    /**
     * Build the data structure we use to build the edit display
     * 
     * @param original tags in original format
     * 
     * @return a map of String (the keys) and ArrayList&lt;String&gt; (the values)
     */
    private LinkedHashMap<String, List<String>> getTagsInEditForm(List<Map<String, String>> original) {
        //
        LinkedHashMap<String, List<String>> tags = new LinkedHashMap<>();
        int l = original.size();
        List<String> valueTemplate = new ArrayList<>(l);
        for (int j = 0; j < l; j++) {
            valueTemplate.add("");
        }
        for (int i = 0; i < l; i++) {
            Map<String, String> map = original.get(i);
            for (Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!tags.containsKey(key)) {
                    tags.put(key, new ArrayList<>(valueTemplate));
                }
                tags.get(key).set(i, value);
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
                    propertyEditorListener.updateRecentPresets();
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
     * @param presetItem the current best PresetItem or null
     * @param presets the current presets
     */
    private void determinePresets(@NonNull Map<String, String> allTags, @Nullable PresetItem presetItem, @NonNull Preset[] presets) {
        clearPresets();
        clearSecondaryPresets();
        final List<String> regions = propertyEditorListener.getIsoCodes();
        if (presetItem == null) {
            primaryPresetItem = Preset.findBestMatch(getContext(), presets, allTags, regions, elements[0], true);
        } else {
            primaryPresetItem = presetItem;
        }
        Map<String, String> nonAssigned = addPresetsToTags(primaryPresetItem, allTags);
        int nonAssignedCount = nonAssigned.size();
        while (nonAssignedCount > 0) {
            PresetItem nonAssignedPreset = Preset.findBestMatch(getContext(), presets, nonAssigned, regions, elements[0], true);
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
    void addToMru(@NonNull Preset[] presets, @NonNull PresetItem item) {
        final List<String> regions = propertyEditorListener.getIsoCodes();
        for (Preset p : presets) {
            if (p != null && p.contains(item)) {
                p.putRecentlyUsed(item, regions);
                break;
            }
        }
    }

    /**
     * Set a hint on a row, the hint value is retrieved from the Preset
     * 
     * @param row the row to set the hint on
     */
    private void setHint(@NonNull TagEditRow row) {
        String aTagKey = row.getKey();
        PresetItem preset = getPreset(aTagKey);
        if (preset != null && !"".equals(aTagKey)) { // set hints even if value isn't empty
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
    @NonNull
    private Map<String, String> addPresetsToTags(@Nullable PresetItem preset, @NonNull Map<String, String> tags) {
        Map<String, String> leftOvers = new LinkedHashMap<>();
        if (preset == null) {
            Log.e(DEBUG_TAG, "addPresetsToTags called with null preset");
            return leftOvers;
        }
        List<PresetItem> linkedPresetList = preset.getLinkedPresets(true, App.getCurrentPresets(getContext()), propertyEditorListener.getIsoCodes());
        for (Entry<String, String> entry : tags.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            PresetTagField field = preset.getField(key);
            if (field instanceof PresetCheckGroupField) {
                field = ((PresetCheckGroupField) field).getCheckField(key);
            } else if (field instanceof PresetFixedField && value != null && !value.equals(((PresetFixedField) field).getValue().getValue())) {
                field = null; // fixed fields need to match both key and value
            }
            if (field != null) {
                storePreset(key, preset);
                continue;
            }
            boolean found = false;
            if (linkedPresetList != null) {
                for (PresetItem linkedPreset : linkedPresetList) {
                    if (linkedPreset.getFixedTagCount() == 0) {
                        // fixed key presets should always count as themselves
                        PresetTagField linkedField = linkedPreset.getField(key);
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
            }
            if (!found) {
                leftOvers.put(key, value);
            }
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
                ((StreetPlaceNamesAdapter) nameAdapters.getStreetNameAdapter(null)).getElementSearch(), getKeyValueMap(allowBlanks), Address.DEFAULT_HYSTERESIS,
                true), false);
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
        if (preset == null) {
            updateAutocompletePresetItem(rowLayout, null, false);
        } else {
            List<String> regions = propertyEditorListener.getIsoCodes();
            for (PresetTagField field : preset.getTagFields()) {
                if (!field.appliesIn(regions)) {
                    continue;
                }
                if (field instanceof PresetCheckGroupField) {
                    for (PresetCheckField check : ((PresetCheckGroupField) field).getCheckFields()) {
                        if (check.appliesIn(regions)) {
                            keys.add(check.getKey());
                        }
                    }
                } else {
                    keys.add(field.getKey());
                }
            }
        }

        keys.addAll(App.getMruTags().getKeys(elementType));

        List<String> allKeys = new ArrayList<>(Preset.getAutocompleteKeys(propertyEditorListener.getPresets(), elementType));
        Collections.sort(allKeys);
        keys.addAll(allKeys);

        keys.removeAll(getUsedKeys(rowLayout, keyEdit));

        List<String> result = new ArrayList<>(keys);

        return new ArrayAdapter<>(requireContext(), R.layout.autocomplete_row, result);
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
        String key = row.getKey();
        if (key != null && key.length() > 0) {
            Set<String> usedKeys = getUsedKeys(rowLayout, null);
            boolean hasTagValues = row.tagValues != null && row.tagValues.size() > 1;
            List<String> tagValues = hasTagValues ? row.tagValues : null;
            if (isStreetName(key, usedKeys)) {
                return nameAdapters.getStreetNameAdapter(tagValues);
            }
            if (isPlaceName(key, usedKeys)) {
                return nameAdapters.getPlaceNameAdapter(tagValues);
            }
            if (!hasTagValues && key.equals(Tags.KEY_NAME) && (names != null) && useNameSuggestions(usedKeys)) {
                return getNameSuggestions(getContext(), names, getKeyValueMapSingle(rowLayout, true), propertyEditorListener);
            }
            boolean isSpeedKey = Tags.isSpeedKey(key) && !Tags.isConditional(key);
            // generate from preset
            Map<String, Integer> counter = new HashMap<>();
            Map<String, ValueWithCount> valueMap = new HashMap<>();
            ArrayAdapterWithRuler<ValueWithCount> adapter2 = new ArrayAdapterWithRuler<>(getActivity(), R.layout.autocomplete_row, Ruler.class);
            if (hasTagValues) {
                for (String t : row.tagValues) {
                    if ("".equals(t)) {
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
                    ValueWithCount v = new ValueWithCount(t, counter.get(t));
                    adapter2.add(v);
                    valueMap.put(t, v);
                }
                adapter2.add(new Ruler());
            }

            if (preset != null) {
                List<String> mruValues = App.getMruTags().getValues(preset, key);
                if (mruValues != null && !mruValues.isEmpty()) {
                    for (String v : mruValues) {
                        if (!valueMap.containsKey(v)) {
                            ValueWithCount vwc = new ValueWithCount(v);
                            adapter2.add(vwc);
                            counter.put(v, 1);
                            valueMap.put(v, vwc);
                        }
                    }
                    adapter2.add(new Ruler());
                }
                if (isSpeedKey) {
                    addMaxSpeeds(adapter2);
                }
                Collection<StringWithDescription> values = preset.getAutocompleteValues(key, propertyEditorListener.getIsoCodes());
                Log.d(DEBUG_TAG, "setting autocomplete adapter for values " + values + " based on " + preset.getName());
                if (values != null && !values.isEmpty()) {
                    List<StringWithDescription> result = new ArrayList<>(values);
                    if (preset.sortValues(key)) {
                        Collections.sort(result);
                    }
                    for (StringWithDescription s : result) {
                        ValueWithCount v = valueMap.get(s.getValue());
                        if (v != null) {
                            v.setDescription(s.getDescription());
                        }
                        adapter2.add(new ValueWithCount(s.getValue(), s.getDescription()));
                    }
                    Log.d(DEBUG_TAG, "key " + key + " type " + preset.getKeyType(key));
                } else if (preset.isFixedTag(key)) {
                    for (StringWithDescription s : Preset.getAutocompleteValues(propertyEditorListener.getPresets(), elementType, key)) {
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
                    adapter2.add(new Ruler());
                }
                if (isSpeedKey) {
                    addMaxSpeeds(adapter2);
                }
                for (StringWithDescription s : Preset.getAutocompleteValues(propertyEditorListener.getPresets(), elementType, key)) {
                    adapter2.add(new ValueWithCount(s.getValue(), s.getDescription()));
                }
            }
            Log.d(DEBUG_TAG, "adapter2 has " + adapter2.getCount() + " elements");
            if (adapter2.getCount() > 0) {
                return adapter2;
            }
        }
        return null;
    }

    /**
     * Add max speed values to an adapter
     * 
     * @param adapter the adapter
     */
    private void addMaxSpeeds(ArrayAdapter<ValueWithCount> adapter) {
        String[] maxSpeeds = TagEditorFragment.getSpeedLimits(getContext(), propertyEditorListener);
        if (maxSpeeds != null) {
            for (String maxSpeed : maxSpeeds) {
                adapter.add(new ValueWithCount(maxSpeed));
            }
        }
    }

    /**
     * Get suggested names from the NSI
     * 
     * @param ctx an Android Context
     * @param names structure holding the NSI Names
     * @param tags current tags
     * @param listener a PropertyEditorListener
     * @return an Adapter with the suggestions or null
     */
    @Nullable
    public static ArrayAdapter<NameAndTags> getNameSuggestions(@NonNull Context ctx, @NonNull Names names, @NonNull Map<String, String> tags,
            @NonNull PropertyEditorListener listener) {
        Log.d(DEBUG_TAG, "generate suggestions for name from name suggestion index");
        List<NameAndTags> suggestions = names.getNames(new TreeMap<>(tags), listener.getIsoCodes());
        if (!suggestions.isEmpty()) {
            List<NameAndTags> result = suggestions;
            Collections.sort(result);
            return new ArrayAdapter<>(ctx, R.layout.autocomplete_row, result);
        }
        return null;
    }

    /**
     * Get suggested speed limits if they exist for the current region
     *
     * @param ctx an Android Context
     * @param listener a PropertyEditorListener
     * @return an Adapter with the limits or null
     */
    @Nullable
    public static String[] getSpeedLimits(@NonNull Context ctx, @NonNull PropertyEditorListener listener) {
        // check if we have localized maxspeed values
        Properties prop = App.getGeoContext(ctx).getProperties(listener.getIsoCodes());
        if (prop != null) {
            return prop.getSpeedLimits();
        }
        return null;
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
        row.setOwner(this);
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
        row.keyEdit.setOnItemClickListener((parent, view, pos, id) -> {
            if (Tags.KEY_ADDR_STREET.equals(parent.getItemAtPosition(pos)) && row.getValue().length() == 0) {
                ArrayAdapter<ValueWithCount> adapter = nameAdapters.getStreetNameAdapter(tagValues);
                if (adapter != null && adapter.getCount() > 0) {
                    row.valueEdit.setText(adapter.getItem(0).getValue());
                }
            } else if (Tags.KEY_ADDR_PLACE.equals(parent.getItemAtPosition(pos)) && row.getValue().length() == 0) {
                ArrayAdapter<ValueWithCount> adapter = nameAdapters.getPlaceNameAdapter(tagValues);
                if (adapter != null && adapter.getCount() > 0) {
                    row.valueEdit.setText(adapter.getItem(0).getValue());
                }
            } else {
                if (primaryPresetItem != null) {
                    String hint = primaryPresetItem.getHint(parent.getItemAtPosition(pos).toString());
                    if (hint != null) { //
                        row.valueEdit.setHint(hint);
                    } else if (!primaryPresetItem.getFields().isEmpty()) {
                        row.valueEdit.setHint(R.string.tag_value_hint);
                    }
                    if (applyDefault && row.getValue().length() == 0) {
                        String defaultValue = primaryPresetItem.getDefault(parent.getItemAtPosition(pos).toString());
                        if (defaultValue != null) { //
                            row.valueEdit.setText(defaultValue);
                        }
                    }
                }
                // set focus on value
                row.valueEdit.requestFocus();
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
                    if (row.getKey().length() == 0) {
                        row.keyEdit.post(() -> {
                            try {
                                row.keyEdit.showDropDown();
                            } catch (android.view.WindowManager.BadTokenException btex) {
                                Log.e(DEBUG_TAG, "onFocusChange " + btex.getMessage());
                            }
                        });
                    }
                } else {
                    String newKey = row.getKey();
                    if (!newKey.equals(originalKey)) { // our preset may have changed re-calc
                        originalKey = newKey;
                        updateAutocompletePresetItem(rowLayout, null, true);
                    }
                    row.keyEdit.post(() -> row.keyEdit.dismissDropDown());
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
                        row.valueEdit.setTokenizer(new CustomAutoCompleteTextView.SingleCharTokenizer(preset.getDelimiter(key)));
                    }
                    if (Tags.isSpeedKey(key)) {
                        initMPHSpeed(getActivity(), row.valueEdit, propertyEditorListener);
                    } else if (keyType == PresetKeyType.TEXT && valueType == null) {
                        InputTypeUtil.enableTextSuggestions(row.valueEdit);
                    }
                    InputTypeUtil.setInputTypeFromValueType(row.valueEdit, valueType);
                    if (row.valueEdit.getText().length() == 0 && (row.tagValues == null || row.tagValues.isEmpty())) {
                        row.valueEdit.post(() -> row.valueEdit.showDropDown());
                    }
                } else {
                    // our preset may have changed re-calc
                    String newValue = row.getValue();
                    if (!newValue.equals(originalValue)) {
                        originalValue = newValue;
                        // potentially we should update tagValues here
                        updateAutocompletePresetItem(rowLayout, null, true);
                    }
                    row.valueEdit.post(() -> row.valueEdit.dismissDropDown());
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

        final TextWatcher valueTextWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                textWatcher.beforeTextChanged(s, start, count, after);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textWatcher.onTextChanged(s, start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {
                textWatcher.afterTextChanged(s);
                Log.d(DEBUG_TAG, "afterTextChanged >" + s + "<");
                row.valueEdit.removeTextChangedListener(this);
                setValue(row, s.toString());
                row.valueEdit.addTextChangedListener(this);
            }
        };
        row.valueEdit.addTextChangedListener(valueTextWatcher);

        row.valueEdit.setOnItemClickListener((parent, view, pos, id) -> {
            if (parent == null) {
                Log.d(DEBUG_TAG, "onItemClicked parent null");
                return;
            }
            Object o = parent.getItemAtPosition(pos);
            Log.d(DEBUG_TAG, "onItemClicked value " + o);
            if (o instanceof Names.NameAndTags) {
                row.valueEdit.setOrReplaceText(((NameAndTags) o).getName());
                applyTagSuggestions(((NameAndTags) o).getTags(), null);
            } else if (o instanceof Value) {
                row.valueEdit.setOrReplaceText(((Value) o).getValue());
            } else if (o instanceof String) {
                row.valueEdit.setOrReplaceText((String) o);
            }
        });

        row.selected.setOnCheckedChangeListener(getOnCheckedChangeListener(row));

        if (row.isEmpty()) {
            row.disableCheckBox();
        }
        rowLayout.addView(row, (position == -1) ? rowLayout.getChildCount() : position);
        //
        return row;
    }

    /**
     * Construct the OnCheckedChangeListener for a row
     * 
     * @param row the row
     * @return an OnCheckedChangeListener
     */
    @NonNull
    private OnCheckedChangeListener getOnCheckedChangeListener(@NonNull TagEditRow row) {
        return (buttonView, isChecked) -> {
            Log.d(DEBUG_TAG, "onCheckedChangedListener value " + isChecked);
            if (row.isEmpty()) {
                row.deselect();
                return;
            }
            if (isChecked) {
                tagSelected();
            } else {
                deselectRow();
            }
        };

    }

    /**
     * Set the value and recreate the autocomplete adapter
     * 
     * If the there are multiple values set them all to the same
     * 
     * @param row the row
     * @param newValue the new value
     */
    private void setValue(@NonNull final TagEditRow row, @NonNull String newValue) {
        final int length = osmIds.length;
        List<String> newValues = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            newValues.add(newValue);
        }
        final String key = row.getKey();
        row.setValues(key, newValues, true);
    }

    /**
     * A row representing an editable tag, consisting of edits for key and value, labels and a delete button. Needs to
     * be static, otherwise the inflater will not find it.
     * 
     * @author Jan
     */
    public static class TagEditRow extends LinearLayout implements SelectedRowsActionModeCallback.Row {

        private TagEditorFragment          owner;
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
        public TagEditRow(@NonNull Context context) {
            super(context);
        }

        /**
         * Construct a View holding the key and value for a tag
         * 
         * @param context an Android Context
         * @param attrs am AttributeSet
         */
        public TagEditRow(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        /**
         * Set the fragment for this view
         * 
         * @param owner the "owning" Fragment
         */
        public void setOwner(@NonNull TagEditorFragment owner) {
            this.owner = owner;
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            keyEdit = (AutoCompleteTextView) findViewById(R.id.editKey);
            keyEdit.setOnKeyListener(PropertyEditorFragment.myKeyListener);

            valueEdit = (CustomAutoCompleteTextView) findViewById(R.id.editValue);
            valueEdit.setOnKeyListener(PropertyEditorFragment.myKeyListener);

            selected = (CheckBox) findViewById(R.id.tagSelected);

            OnClickListener autocompleteOnClick = v -> {
                if (v.hasFocus()) {
                    ((AutoCompleteTextView) v).showDropDown();
                }
            };
            // set an empty adapter on both views to be on the safe side
            ArrayAdapter<String> empty = new ArrayAdapter<>(getContext(), R.layout.autocomplete_row, new String[0]);
            keyEdit.setAdapter(empty);
            valueEdit.setAdapter(empty);
            keyEdit.setOnClickListener(autocompleteOnClick);
            valueEdit.setOnClickListener(autocompleteOnClick);
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            if (w == 0 && h == 0) {
                return;
            }

            // this is not really satisfactory
            keyEdit.setDropDownAnchor(valueEdit.getId());
            // note wrap_content does not actually wrap the contents of the drop
            // down, instead in makes it the same width as the AutoCompleteTextView
            valueEdit.setDropDownWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
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
        @NonNull
        public TagEditRow setValues(String aTagKey, List<String> tagValues, boolean same) {
            keyEdit.setText(aTagKey);
            this.tagValues = tagValues;
            this.same = same;
            if (same) {
                if (tagValues != null && !tagValues.isEmpty()) {
                    String newValue = tagValues.get(0);
                    if (!valueEdit.getText().toString().equals(newValue)) {
                        valueEdit.setText(tagValues.get(0));
                    }
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
        public String getValue() {
            return valueEdit.getText().toString();
        }

        /**
         * Deletes this row
         */
        @Override
        public void delete() {
            deleteRow((LinearLayout) owner.getOurView());
        }

        /**
         * Deletes this row
         * 
         * @param rowLayout the Layout holding the rows
         */
        public void deleteRow(@NonNull LinearLayout rowLayout) {
            // suspect
            View cf = owner.getActivity().getCurrentFocus();
            if (cf == keyEdit || cf == valueEdit) {
                // about to delete the row that has focus!
                // try to move the focus to the next row or failing that to the previous row
                int current = owner.rowIndex((LinearLayout) getParent(), this);
                if (!focusRow(rowLayout, current + 1)) {
                    focusRow(rowLayout, current - 1);
                }
            }
            rowLayout.removeView(this);
            if (isEmpty() && owner != null) {
                owner.ensureEmptyRow(rowLayout);
            }
        }

        /**
         * Move the focus to the key field of the specified row.
         * 
         * @param layout the LinearLayout hold the rows
         * @param index The index of the row to move to, counting from 0.
         * @return true if the row was successfully focused, false otherwise.
         */
        private boolean focusRow(@NonNull LinearLayout layout, int index) {
            TagEditRow row = (TagEditRow) layout.getChildAt(index);
            return row != null && row.keyEdit.requestFocus();
        }

        /**
         * Checks if the fields in this row are empty
         * 
         * @return true if both fields are empty, false if at least one is filled
         */
        public boolean isEmpty() {
            return "".equals(keyEdit.getText().toString().trim()) && "".equals(valueEdit.getText().toString().trim());
        }

        @Override
        public void select() {
            setRowSelected(true);
        }

        @Override
        public boolean isSelected() {
            return selected.isChecked();
        }

        @Override
        public void deselect() {
            setRowSelected(false);
            // check if all have been deselected
            owner.deselectRow();
        }

        /**
         * Set the row to checked/non-checked state
         *
         * @param state target state
         */
        public void setRowSelected(boolean state) {
            selected.setOnCheckedChangeListener(null);
            selected.setChecked(state);
            selected.setOnCheckedChangeListener(owner.getOnCheckedChangeListener(this));
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
            final String value = tag.getValue();
            List<String> oldValue = currentValues.put(tag.getKey(), Util.wrapInList(value));
            if (oldValue != null && !oldValue.isEmpty() && !"".equals(oldValue.get(0)) && !oldValue.contains(value)) {
                replacedValue = true;
            }
        }
        if (replacedValue) {
            Builder dialog = ThemeUtils.getAlertDialogBuilder(getActivity());
            dialog.setTitle(R.string.tag_editor_name_suggestion);
            dialog.setMessage(R.string.tag_editor_name_suggestion_overwrite_message);
            dialog.setPositiveButton(R.string.replace, (d, which) -> {
                loadEdits(currentValues, false);
                if (afterApply != null) {
                    afterApply.run();
                }
            });
            dialog.setNegativeButton(R.string.cancel, null);
            dialog.create().show();
        } else {
            loadEdits(currentValues, false);
        }
        if (prefs.nameSuggestionPresetsEnabled()) {
            PresetItem p = Preset.findBestMatch(propertyEditorListener.getPresets(), getKeyValueMapSingle(false), null, null); // FIXME
                                                                                                                               // multiselect
            if (p != null) {
                applyPreset((LinearLayout) getOurView(), p, false, false, false, Prefill.PRESET);
            }
        }
    }

    @Override
    protected SelectedRowsActionModeCallback getActionModeCallback() {
        return new TagSelectedActionModeCallback(this, (LinearLayout) getOurView());
    }

    /**
     * Start the TagSelectedActionModeCallback
     */
    private void tagSelected() {
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback == null) {
                actionModeCallback = getActionModeCallback();
                ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
            }
        }
    }

    @Override
    public void selectAllRows() { // select all tags
        setSelectedRows((boolean current) -> true);
    }

    /**
     * Iterate over all rows and set the selection status
     * 
     * @param change method that sets the selection status
     */
    private void setSelectedRows(@NonNull final ChangeSelectionStatus change) {
        LinearLayout rowLayout = (LinearLayout) getOurView();
        if (loaded) {
            int i = rowLayout.getChildCount();
            while (--i >= 0) {
                TagEditRow row = (TagEditRow) rowLayout.getChildAt(i);
                final CheckBox selected = row.selected;
                if (selected.isEnabled()) {
                    row.setRowSelected(change.set(selected.isChecked()));
                }
            }
            deselectRow();
        }
    }

    @Override
    public void deselectAllRows() { // deselect all tags
        setSelectedRows((boolean current) -> false);
    }

    @Override
    public void invertSelectedRows() {
        setSelectedRows((boolean current) -> !current);
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
        if (rowLayout == null || !loaded) {
            return null;
        }
        TagEditRow ret = null;
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
            return insertNewEdit(rowLayout, "", new ArrayList<>(), -1, false);
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
        return focusOnEmptyValue((LinearLayout) getOurView());
    }

    /**
     * Focus on the first empty value field
     * 
     * @param rowLayout layout holding the rows
     * @return true if successful
     */
    private boolean focusOnEmptyValue(LinearLayout rowLayout) {
        for (int i = 0; i < rowLayout.getChildCount(); i++) {
            TagEditRow ter = (TagEditRow) rowLayout.getChildAt(i);
            if (ter != null && !"".equals(ter.getKey()) && "".equals(ter.getValue())) {
                ter.valueEdit.requestFocus();
                ter.valueEdit.dismissDropDown();
                return true;
            }
        }
        return false;
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
        Log.d(DEBUG_TAG, "focusOnValue " + key);
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
        applyPreset((LinearLayout) getOurView(), preset, addOptional, false, true, Prefill.PRESET);
    }

    /**
     * Applies a preset (e.g. selected from the dialog or MRU), i.e. adds the tags from the preset to the current tag
     * set
     * 
     * @param item the preset item to apply
     * @param addOptional add optional tags if true
     * @param isAlternative is a alternative to the existing tagging
     * @param addToMRU add to preset MRU list if true
     * @param prefill etermine how to prefill empty values
     */
    void applyPreset(@NonNull PresetItem item, boolean addOptional, boolean isAlternative, boolean addToMRU, @NonNull Prefill prefill) {
        applyPreset((LinearLayout) getOurView(), item, addOptional, isAlternative, addToMRU, prefill);
    }

    /**
     * Applies a preset (e.g. selected from the dialog or MRU), i.e. adds the tags from the preset to the current tag
     * set
     * 
     * @param rowLayout the layout holding the rows
     * @param item the preset item to apply
     * @param addOptional add optional tags if true
     * @param isAlternative is a alternative to the existing tagging
     * @param addToMRU add to preset MRU list if true
     * @param prefill determine how to prefill empty values
     */
    void applyPreset(@NonNull LinearLayout rowLayout, @NonNull PresetItem item, boolean addOptional, boolean isAlternative, boolean addToMRU,
            @NonNull Prefill prefill) {
        List<String> regions = propertyEditorListener.getIsoCodes();
        Log.d(DEBUG_TAG, "applying preset " + item.getName() + " for region " + regions);
        final LinkedHashMap<String, List<String>> currentValues = getKeyValueMap(rowLayout, true);

        int replacedOrRemoved = 0;

        // remove everything that doesn't have a value
        // given that these are likely leftovers from a previous preset
        Set<String> keySet = new HashSet<>(currentValues.keySet()); // shallow copy
        for (String key : keySet) {
            List<String> list = currentValues.get(key);
            if (list == null || list.isEmpty()) {
                currentValues.remove(key);
            }
        }

        if (isAlternative) {
            // remove fixed tags for alternatives
            List<PresetItemLink> alternatives = item.getAlternativePresetItems();
            if (alternatives != null && !alternatives.isEmpty()) {
                for (PresetItemLink alternative : alternatives) {
                    for (PresetItem current : getAllPresets().values()) {
                        if (alternative.getPresetName().equals(current.getName())) {
                            for (String key : current.getFixedTags().keySet()) {
                                if (currentValues.remove(key) != null) {
                                    replacedOrRemoved++;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fixed tags, always have a value. We overwrite mercilessly.
        for (Entry<String, PresetFixedField> tag : item.getFixedTags().entrySet()) {
            PresetFixedField field = tag.getValue();
            if (!field.appliesIn(regions)) {
                continue;
            }
            String v = field.getValue().getValue();
            List<String> oldValue = currentValues.put(tag.getKey(), Util.wrapInList(v));
            if (oldValue != null && !oldValue.isEmpty() && !oldValue.contains(v) && !(oldValue.size() == 1 && "".equals(oldValue.get(0)))) {
                replacedOrRemoved++;
            }
        }

        // add tags with no fixed values, optional tags only if addOptional is set
        Map<String, String> scripts = new LinkedHashMap<>();
        for (Entry<String, PresetField> entry : item.getFields().entrySet()) {
            PresetField field = entry.getValue();
            if (!field.appliesIn(regions)) {
                continue;
            }
            if (field instanceof PresetTagField) {
                PresetTagField tagField = (PresetTagField) field;
                boolean isOptional = tagField.isOptional();
                if (!isOptional || (isOptional && addOptional)) {
                    if (tagField instanceof PresetCheckGroupField) {
                        for (PresetCheckField check : ((PresetCheckGroupField) tagField).getCheckFields()) {
                            if (!check.appliesIn(regions)) {
                                continue;
                            }
                            addTagFromPreset(item, check, currentValues, check.getKey(), scripts, prefill);
                        }
                    } else if (!(tagField instanceof PresetFixedField)) {
                        addTagFromPreset(item, tagField, currentValues, entry.getKey(), scripts, prefill);
                    }
                }
            }
        }

        if (!scripts.isEmpty()) {
            Preset[] presets = App.getCurrentPresets(getActivity());
            determinePresets(getKeyValueMapSingle(rowLayout, true), null, presets);
            for (Entry<String, String> entry : scripts.entrySet()) {
                evalJavaScript(item, currentValues, entry.getKey(), entry.getValue());
            }
        }

        loadEdits(rowLayout, currentValues, true);
        if (replacedOrRemoved > 0) {
            Resources r = getContext().getResources();
            ScreenMessage.barWarning(getActivity(), r.getQuantityString(R.plurals.toast_preset_removed_or_replaced_tags, replacedOrRemoved, replacedOrRemoved));
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
    private void evalJavaScript(final @NonNull PresetItem item, final @NonNull Map<String, List<String>> currentValues, final @NonNull String key,
            final @NonNull String script) {
        try {
            String defaultValue = item.getDefault(key) == null ? "" : item.getDefault(key);
            for (Entry<String, PresetItem> entry : tags2Preset.entrySet()) {
                Log.e(DEBUG_TAG, "evalJavaScript " + entry.getKey() + " " + (entry.getValue() != null ? entry.getValue().getName() : " null"));
            }
            String result = de.blau.android.javascript.Utils.evalString(getActivity(), " " + key, script, getTagsInEditForm(), currentValues, defaultValue,
                    tags2Preset, App.getCurrentPresets(getActivity()));
            if (result == null || "".equals(result)) {
                currentValues.remove(key);
            } else if (currentValues.containsKey(key)) {
                currentValues.put(key, Util.wrapInList(result));
            }
        } catch (Exception ex) {
            ScreenMessage.toastTopError(getActivity(), ex.getLocalizedMessage());
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
     * @param prefill determine how to prefill empty values
     * @return true if a value was set
     */
    private boolean addTagFromPreset(@NonNull PresetItem item, @Nullable PresetTagField field, @NonNull Map<String, List<String>> tags, @NonNull String key,
            @Nullable Map<String, String> scripts, @NonNull Prefill prefill) {
        List<String> values = tags.get(key);
        boolean isDeprecated = field != null && field.isDeprecated();
        if ((values != null && ((values.size() == 1 && !"".equals(values.get(0))) || values.size() > 1)) || isDeprecated) {
            return false;
        }
        String value = "";
        if (field != null && prefill != Prefill.NEVER) {
            String defaultValue = field.getDefaultValue();
            if (defaultValue != null) {
                value = defaultValue;
            }
            UseLastAsDefaultType useLastAsDefault = field.getUseLastAsDefault();
            if (useLastAsDefault == UseLastAsDefaultType.TRUE || useLastAsDefault == UseLastAsDefaultType.FORCE || prefill == Prefill.FORCE_LAST) {
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
        return !"".equals(value);
    }

    /**
     * Merge a set of tags in to the current ones
     * 
     * @param newTags the new tags to merge
     */
    private void mergeTags(@NonNull Map<String, String> newTags) {
        LinkedHashMap<String, List<String>> currentValues = getKeyValueMap(true);
        int replacedValues = 0;

        // Existing tags with the same key will be overwritten
        for (Entry<String, String> tag : newTags.entrySet()) {
            List<String> oldValues = currentValues.put(tag.getKey(), Util.wrapInList(tag.getValue()));
            if (oldValues != null && !oldValues.isEmpty() && !oldValues.contains(tag.getValue()) && (oldValues.size() == 1 && !"".equals(oldValues.get(0)))) {
                replacedValues++;
            }
        }

        loadEdits(currentValues, false);
        if (replacedValues > 0) {
            ScreenMessage.barWarning(getActivity(), R.string.toast_merge_overwrote_tags);
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
            ScreenMessage.barWarning(getActivity(), R.string.toast_merge_overwrote_tags);
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
        // disable address prediction for stuff that won't have an address
        try {
            menu.findItem(R.id.tag_menu_address).setVisible(elements.length == 1 && (!(elements[0] instanceof Way) || ((Way) elements[0]).isClosed()));
            boolean multiSelect = elements.length > 1;
            menu.findItem(R.id.tag_menu_apply_preset).setEnabled(!multiSelect);
            menu.findItem(R.id.tag_menu_apply_preset_with_optional).setEnabled(!multiSelect);
            menu.findItem(R.id.tag_menu_mapfeatures).setEnabled(propertyEditorListener.isConnectedOrConnecting());
            menu.findItem(R.id.tag_menu_paste).setEnabled(!App.getTagClipboard(getContext()).isEmpty());
            menu.findItem(R.id.tag_menu_paste_from_clipboard).setEnabled(pasteFromClipboardIsPossible());
        } catch (NullPointerException npe) {
            // this should never happen as the menu should have been inflated and the item found, however it does now
            // and then
            Log.e(DEBUG_TAG, "onPrepareOptionsMenu " + npe.getMessage());
            ACRAHelper.nocrashReport(null, "onPrepareOptionsMenu " + npe.getMessage());
        }
    }

    /**
     * Using a lambda pulls in the fragment when serializing in the console
     */
    private static class PresetJSEvalCallback implements EvalCallback {

        private static final long serialVersionUID = 1L;

        private final Map<String, List<String>> originalTags;
        private final Map<String, List<String>> tags;
        private final Map<String, PresetItem>   key2PresetItem;

        /**
         * Construct a new callback
         * 
         * @param originalTags original tags the property editor was called with
         * @param tags the current tags
         * @param key2PresetItem map from key to PresetItem
         */
        PresetJSEvalCallback(@NonNull Map<String, List<String>> originalTags, @NonNull Map<String, List<String>> tags,
                @NonNull Map<String, PresetItem> key2PresetItem) {
            this.originalTags = originalTags;
            this.tags = tags;
            this.key2PresetItem = key2PresetItem;
        }

        @Override
        public String eval(Context context, String input, boolean flag1, boolean flag2) {
            return de.blau.android.javascript.Utils.evalString(context, "JS Preset Test", input, originalTags, tags, "test", key2PresetItem,
                    App.getCurrentPresets(context));
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home:
            propertyEditorListener.updateAndFinish();
            return true;
        case R.id.tag_menu_address:
            predictAddressTags(false);
            return true;
        case R.id.tag_menu_sourcesurvey:
            doSourceSurvey();
            return true;
        case R.id.tag_menu_apply_preset:
        case R.id.tag_menu_apply_preset_with_optional:
            PresetItem pi = Preset.findBestMatch(propertyEditorListener.getPresets(), getKeyValueMapSingle(false), null, null);
            if (pi != null) {
                boolean displayOptional = itemId == R.id.tag_menu_apply_preset_with_optional;
                presetSelectedListener.onPresetSelected(pi, displayOptional, false,
                        prefs.applyWithLastValues(getContext(), pi) ? Prefill.FORCE_LAST : Prefill.PRESET);
                if (displayOptional) {
                    formUpdate.displayOptional(pi, true);
                }
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
            for (Preset p : propertyEditorListener.getPresets()) {
                if (p != null) {
                    p.resetRecentlyUsed();
                }
            }
            propertyEditorListener.updateRecentPresets();
            return true;
        case R.id.tag_menu_reset_address_prediction:
            // simply overwrite with an empty file
            Address.resetLastAddresses(getActivity());
            return true;
        case R.id.tag_menu_js_console:
            ConsoleDialog.showDialog(getActivity(), R.string.tag_menu_js_console, -1, -1, null, null,
                    new PresetJSEvalCallback(getTagsInEditForm(), getKeyValueMap(true), tags2Preset), false);
            return true;
        case R.id.tag_menu_select_all:
            selectAllRows();
            return true;
        case R.id.tag_menu_info:
            ElementInfo.showDialog(getActivity(), propertyEditorListener.getElement(), false, false);
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
    @NonNull
    private LinkedHashMap<String, List<String>> getKeyValueMap(final boolean allowBlanks) {
        return getKeyValueMap((LinearLayout) getOurView(), allowBlanks);
    }

    /**
     * Collect all key-value pairs into a LinkedHashMap&gt;String,String&lt;
     * 
     * @param rowLayout the Layout holding the rows
     * @param allowBlanks If true, includes key-value pairs where one or the other is blank.
     * @return The LinkedHashMap&lt;String,List&lt;String&gt;&gt; of key-value pairs.
     */
    @NonNull
    private LinkedHashMap<String, List<String>> getKeyValueMap(LinearLayout rowLayout, final boolean allowBlanks) {
        final LinkedHashMap<String, List<String>> tags = new LinkedHashMap<>();
        if (rowLayout == null) {
            Log.e(DEBUG_TAG, "rowLayout null in getKeyValueMapSingle");
            if (savedTags != null) {
                return savedTags;
            }
            return tags;
        }
        processKeyValues(rowLayout, (keyEdit, valueEdit, tagValues) -> {
            String key = keyEdit.getText().toString().trim();
            String value = valueEdit.getText().toString().trim();
            boolean keyBlank = "".equals(key);
            boolean valueBlank = "".equals(value);
            boolean bothBlank = keyBlank && valueBlank;
            boolean neitherBlank = !keyBlank && !valueBlank;
            // both blank is never acceptable
            if (!bothBlank && (neitherBlank || allowBlanks || (valueBlank && tagValues != null && !tagValues.isEmpty()))) {
                List<String> existing = tags.get(key);
                boolean existingIsEmpty = existing == null || (existing.size() == 1 && "".equals(existing.get(0)));
                if (!existingIsEmpty) {
                    Log.e(DEBUG_TAG, "Attempt to overwrite existing non-empty value");
                    return;
                }
                if (valueBlank) {
                    tags.put(key, areEmpty(tagValues) ? Util.wrapInList("") : tagValues);
                } else {
                    tags.put(key, Util.wrapInList(value));
                }
            }
        });
        return tags;
    }

    /**
     * Check that all values in a list are empty
     * 
     * @param values the List of values
     * @return true if all are empty
     */
    private boolean areEmpty(@NonNull List<String> values) {
        for (String v : values) {
            if (Util.notEmpty(v)) {
                return false;
            }
        }
        return true;
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
    private LinkedHashMap<String, String> getKeyValueMapSingle(@Nullable LinearLayout rowLayout, final boolean allowBlanks) {
        final LinkedHashMap<String, String> tags = new LinkedHashMap<>();
        if (rowLayout == null && savedTags != null) {
            for (Entry<String, List<String>> entry : savedTags.entrySet()) {
                getSingleTag(entry.getKey().trim(), "", entry.getValue(), tags, allowBlanks);
            }
        }
        if (rowLayout != null) {
            processKeyValues(rowLayout, (keyEdit, valueEdit, tagValues) -> {
                String key = keyEdit.getText().toString().trim();
                String value = valueEdit.getText().toString().trim();
                getSingleTag(key, value, tagValues, tags, allowBlanks);
            });
        } else {
            Log.e(DEBUG_TAG, "rowLayout null in getKeyValueMapSingle");
        }
        return tags;
    }

    /**
     * Add a single tag to tags checking for blanks and existing keys
     * 
     * @param key the tag key
     * @param value the tag value
     * @param tagValues if multiple values are present this will contain them
     * @param tags a map containing the values we need
     * @param allowBlanks allow either a blank key or value
     */
    private void getSingleTag(@NonNull String key, @NonNull String value, @Nullable List<String> tagValues, @NonNull final LinkedHashMap<String, String> tags,
            final boolean allowBlanks) {
        boolean hasValues = tagValues != null && !tagValues.isEmpty();
        value = "".equals(value) && hasValues && tagValues.get(0) != null ? tagValues.get(0) : value;
        boolean valueBlank = "".equals(value);
        boolean bothBlank = "".equals(key) && valueBlank;
        boolean neitherBlank = !"".equals(key) && !valueBlank;
        if (!bothBlank && (neitherBlank || allowBlanks || (valueBlank && hasValues))) {
            String existing = tags.get(key);
            boolean existingIsEmpty = existing == null || "".equals(existing);
            if (existingIsEmpty) {
                tags.put(key, value);
            } else {
                Log.e(DEBUG_TAG, "Attempt to overwrite existing non-empty value");
            }
        }
    }

    /**
     * Given an OSM key value, determine it's corresponding source key. For example, the source of "name" is
     * "source:name". The source of "source" is "source". The source of "mf:name" is "mf.source:name".
     * 
     * @param key the key to be sourced.
     * @return The source key for the given key.
     */
    private static String sourceForKey(final String key) {
        String result = Tags.KEY_SOURCE;
        if (Util.notEmpty(key) && !Tags.KEY_SOURCE.equals(key)) {
            // key is neither blank nor "source"
            // check if it's namespaced
            int i = key.indexOf(Tags.NS_SEP);
            if (i == -1) {
                result = Tags.KEY_SOURCE + Tags.NS_SEP + key;
            } else {
                // handle already namespaced keys as per
                // http://wiki.openstreetmap.org/wiki/Key:source
                result = key.substring(0, i) + "." + Tags.KEY_SOURCE + key.substring(i);
            }
        }
        return result;
    }

    /**
     * Add a source:key field and pre-fill it with "survey"
     * 
     * Note that it isn't clear if this is still a reasonable thing to support
     */
    private void doSourceSurvey() {
        // determine the key (if any) that has the current focus in the key or its value
        final String[] focusedKey = new String[] { null }; // array to work around unsettable final
        processKeyValues((keyEdit, valueEdit, tagValues) -> {
            if (keyEdit.isFocused() || valueEdit.isFocused()) {
                focusedKey[0] = keyEdit.getText().toString().trim();
            }
        });
        // ensure source(:key)=survey is tagged
        final String sourceKey = sourceForKey(focusedKey[0]);
        final boolean[] sourceSet = new boolean[] { false }; // array to work around unsettable final
        processKeyValues((keyEdit, valueEdit, tagValues) -> {
            if (!sourceSet[0]) {
                String key = keyEdit.getText().toString().trim();
                String value = valueEdit.getText().toString().trim();
                // if there's a blank row - use them
                if ("".equals(key) && "".equals(value)) {
                    key = sourceKey;
                    keyEdit.setText(key);
                }
                if (key.equals(sourceKey)) {
                    valueEdit.setText(Tags.VALUE_SURVEY);
                    sourceSet[0] = true;
                }
            }
        });
        if (!sourceSet[0]) {
            // source wasn't set above - add a new pair
            List<String> v = new ArrayList<>();
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
     * reload original arguments
     */
    void doRevert() {
        Log.d(DEBUG_TAG, "doRevert");
        loadEdits(getTagsInEditForm(propertyEditorListener.getOriginalTags()), false);
        updateAutocompletePresetItem(null);
        formUpdate.tagsUpdated();
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
        processKeyValues(rowLayout, (keyEdit, valueEdit, tagValues) -> {
            if (!keyEdit.equals(ignoreEdit)) {
                String key = keyEdit.getText().toString().trim();
                if (key.length() > 0) {
                    keys.add(key);
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
     * Check for any issues in the tags, throw an exception if there is
     */
    public void validate() {
        List<String> keys = new ArrayList<>();
        processKeyValues((LinearLayout) getOurView(), (keyEdit, valueEdit, tagValues) -> {
            String key = keyEdit.getText().toString().trim();
            String value = valueEdit.getText().toString().trim();
            if (keys.contains(key) && !"".equals(value) && tagValues.isEmpty()) {
                throw new DuplicateKeyException(key);
            }
            keys.add(key);
        });
    }

    /**
     * Update the original list of tags to reflect edits
     * 
     * Note this will silently remove tags with empty key or value
     * 
     * @return list of maps containing the tags
     */
    @NonNull
    public List<Map<String, String>> getUpdatedTags() {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> oldTags = propertyEditorListener.getOriginalTags();
        // make a (nearly) full copy
        List<Map<String, String>> newTags = new ArrayList<>();
        for (Map<String, String> map : oldTags) {
            newTags.add(new LinkedHashMap<>(map));
        }

        LinkedHashMap<String, List<String>> edits = getKeyValueMap(true);
        if (edits == null) {
            // if we didn't get a LinkedHashMap as input we need to copy
            List<Map<String, String>> newOldTags = new ArrayList<>();
            for (Map<String, String> map : oldTags) {
                newOldTags.add(new LinkedHashMap<>(map));
            }
            return newOldTags;
        }

        for (int index = 0; index < newTags.size(); index++) {
            Map<String, String> map = newTags.get(index);
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
                final int size = valueList.size();
                if (editsKey != null && !"".equals(editsKey) && !map.containsKey(editsKey) && size > 0) {
                    String value = valueList.get(size == 1 || index >= size ? 0 : index).trim();
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
        for (int i = l.getChildCount() - 1; i >= 0; --i) {
            TagEditRow ter = (TagEditRow) l.getChildAt(i);
            if (ter.getKey().equals(key)) {
                ter.delete();
                break;
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
            PresetTagField field = pi.getField(key);
            boolean useLastAsDefault = field != null && field.getUseLastAsDefault() != UseLastAsDefaultType.FALSE;
            if (field instanceof PresetComboField && ((PresetComboField) field).isMultiSelect()) {
                // trim potential trailing separators, or ensure that we have as many fields as we are supposed to
                char delimiter = pi.getDelimiter(key);
                String valueCountKey = ((PresetComboField) field).getValueCountKey();
                String valueCountValue = valueCountKey != null ? result.get(valueCountKey) : null;
                if (valueCountValue != null) {
                    // this will fill up any missing fields
                    try {
                        int valueCount = Integer.parseInt(valueCountValue);
                        int currentCount = Util.countChar(value, delimiter) + 1;
                        if (currentCount < valueCount) {
                            for (int i = currentCount; i < valueCount; i++) {
                                value += delimiter; // NOSONAR
                            }
                        } else if (currentCount > valueCount) {
                            // only remove trailing delimiters
                            for (int i = valueCount; i < currentCount; i++) {
                                if (value.endsWith(String.valueOf(delimiter))) {
                                    value = value.substring(0, value.length() - 1);
                                } else {
                                    break;
                                }
                            }
                        }
                    } catch (NumberFormatException nfex) {
                        // something is wrong, don't touch value
                    }
                } else {
                    if (value.endsWith(String.valueOf(delimiter))) {
                        value = value.substring(0, value.length() - 1);
                    }
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
