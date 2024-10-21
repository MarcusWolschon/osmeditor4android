package de.blau.android.propertyeditor.tagform;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import ch.poole.android.checkbox.IndeterminateCheckBox;
import ch.poole.conditionalrestrictionparser.ConditionalRestrictionParser;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.address.Address;
import de.blau.android.measure.Length;
import de.blau.android.measure.Measure;
import de.blau.android.measure.Params;
import de.blau.android.nsi.Names;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.osm.Wiki;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.MatchType;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetCheckField;
import de.blau.android.presets.PresetCheckGroupField;
import de.blau.android.presets.PresetComboField;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetField;
import de.blau.android.presets.PresetFixedField;
import de.blau.android.presets.PresetFormattingField;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetKeyType;
import de.blau.android.presets.PresetLabelField;
import de.blau.android.presets.PresetSpaceField;
import de.blau.android.presets.PresetTagField;
import de.blau.android.presets.PresetTextField;
import de.blau.android.presets.ValueType;
import de.blau.android.propertyeditor.EditorUpdate;
import de.blau.android.propertyeditor.FormUpdate;
import de.blau.android.propertyeditor.NameAdapters;
import de.blau.android.propertyeditor.Prefill;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.propertyeditor.PropertyEditorListener;
import de.blau.android.propertyeditor.TagChanged;
import de.blau.android.propertyeditor.TagEditorFragment;
import de.blau.android.util.ArrayAdapterWithRuler;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.ExtendedStringWithDescription;
import de.blau.android.util.GeoContext.Properties;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.StringWithDescriptionAndIcon;
import de.blau.android.util.Util;
import de.blau.android.views.CustomAutoCompleteTextView;

public class TagFormFragment extends BaseFragment implements FormUpdate {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, TagFormFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = TagFormFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String FRAGMENT_CONDITIONAL_RESTRICTION_TAG = "fragment_conditional_restriction";

    private static final String FOCUS_TAG              = "focusTag";
    private static final String FOCUS_ON_ADDRESS       = "focusOnAddress";
    private static final String DISPLAY_MRU_PRESETS    = "displayMRUpresets";
    private static final String SAVED_DISPLAY_OPTIONAL = "SAVED_DISPLAY_OPTIONAL";

    private LayoutInflater inflater = null;

    private Names names = null;

    Preferences prefs = null;

    PropertyEditorListener propertyEditorListener;

    OnPresetSelectedListener presetSelectedListener;

    private EditorUpdate tagListener = null;

    private NameAdapters nameAdapters = null;

    private boolean focusOnAddress = false;
    private boolean displayMRUpresets;
    private String  focusTag       = null;
    private boolean firstUpdate    = true;

    int maxInlineValues = 3;

    int         maxStringLength; // maximum key, value and role length
    private int longStringLimit;

    private Map<PresetItem, Boolean> displayOptional = new HashMap<>();

    final class Ruler extends StringWithDescription {
        private static final long serialVersionUID = 1L;

        /**
         * Create a new Ruler
         */
        public Ruler() {
            super("");
        }
    }

    private StringWithDescription.LocaleComparator comparator;

    /**
     * Create a new instance of the fragment
     * 
     * @param displayMRUpresets display the MRU list of Presets
     * @param focusOnAddress focus on any address keys
     * @param focusTag focus on this tag
     * @return a TagFormFragment instance
     */
    @NonNull
    public static TagFormFragment newInstance(boolean displayMRUpresets, boolean focusOnAddress, @Nullable String focusTag) {
        TagFormFragment f = new TagFormFragment();

        Bundle args = new Bundle();

        args.putSerializable(DISPLAY_MRU_PRESETS, displayMRUpresets);
        args.putSerializable(FOCUS_ON_ADDRESS, focusOnAddress);
        args.putSerializable(FOCUS_TAG, focusTag);

        f.setArguments(args);

        return f;
    }

    // this needs to be setup before onCreate
    private final ActivityResultLauncher<Params> measureLauncher = registerForActivityResult(Measure.getContract(), (Length length) -> {
        if (length != null) { // silently ignore for now
            final String key = length.getKey();
            View row = getRow(key);
            if (row instanceof TextRow) {
                Util.scrollToRow(getView(), row, true, true);
                final String lengthString = length.toString();
                TextRow.setOrReplaceText(((TextRow) row).getValueView(), lengthString);
                updateSingleValue(key, lengthString);
            } else {
                Log.e(DEBUG_TAG, "onActivityResult row for " + key + " not found");
            }
        }
    });

    @Override
    public void onAttachToContext(Context context) {
        Log.d(DEBUG_TAG, "onAttachToContext");
        Fragment parent = getParentFragment();
        Util.implementsInterface(parent, EditorUpdate.class, NameAdapters.class, PropertyEditorListener.class, OnPresetSelectedListener.class);
        tagListener = (EditorUpdate) parent;
        nameAdapters = (NameAdapters) parent;
        propertyEditorListener = (PropertyEditorListener) parent;
        presetSelectedListener = (OnPresetSelectedListener) parent;
        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
        comparator = new StringWithDescription.LocaleComparator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        this.inflater = inflater;
        ScrollView rowLayout = (ScrollView) inflater.inflate(R.layout.tag_form_view, container, false);

        if (savedInstanceState == null) {
            // No previous state to restore - get the state from the intent
            Log.d(DEBUG_TAG, "Initializing from original arguments");
            displayMRUpresets = getArguments().getBoolean(DISPLAY_MRU_PRESETS, false);
            focusOnAddress = getArguments().getBoolean(FOCUS_ON_ADDRESS, false);
            focusTag = getArguments().getString(FOCUS_TAG);
        } else {
            // Restore activity from saved state
            Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
            displayMRUpresets = savedInstanceState.getBoolean(DISPLAY_MRU_PRESETS);
            Serializable temp = Util.getSerializeable(savedInstanceState, SAVED_DISPLAY_OPTIONAL, Serializable.class);
            if (temp instanceof Map<?, ?>) {
                final PresetGroup rootGroup = App.getCurrentRootPreset(getContext()).getRootGroup();
                for (Entry<PresetElementPath, Boolean> entry : ((Map<PresetElementPath, Boolean>) temp).entrySet()) {
                    PresetElement item = Preset.getElementByPath(rootGroup, entry.getKey());
                    if (item instanceof PresetItem) {
                        displayOptional.put((PresetItem) item, entry.getValue());
                    }
                }
            }
        }

        //
        prefs = App.getPreferences(getActivity());

        if (prefs.getEnableNameSuggestions()) {
            names = App.getNames(getActivity());
        }

        maxInlineValues = prefs.getMaxInlineValues();

        maxStringLength = propertyEditorListener.getCapabilities().getMaxStringLength();
        longStringLimit = prefs.getLongStringLimit();

        if (displayMRUpresets) {
            de.blau.android.propertyeditor.Util.addMRUPresetsFragment(getChildFragmentManager(), R.id.mru_layout,
                    propertyEditorListener.getElement().getOsmId(), propertyEditorListener.getElement().getName());
        }
        Log.d(DEBUG_TAG, "onCreateView returning");
        return rowLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putBoolean(DISPLAY_MRU_PRESETS, displayMRUpresets);
        Map<PresetElementPath, Boolean> temp = new HashMap<>();
        final PresetGroup rootGroup = App.getCurrentRootPreset(getContext()).getRootGroup();
        for (Entry<PresetItem, Boolean> entry : displayOptional.entrySet()) {
            temp.put(entry.getKey().getPath(rootGroup), entry.getValue());
        }
        outState.putSerializable(SAVED_DISPLAY_OPTIONAL, (Serializable) temp);
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        updateEditorFromText();
    }

    /**
     * Get an Adapter containing value suggestions for a specific key
     * 
     * Simplified version for non-multi-select and preset only situation
     * 
     * @param key the key for which we are generating the adapter
     * @param values existing values
     * @param preset the PresetItem that matched the tags
     * @param field a PresetField or null
     * @param allTags all the tags of the element
     * @return an ArrayAdapter for key, or null if something went wrong
     */
    @Nullable
    ArrayAdapter<?> getValueAutocompleteAdapter(@Nullable String key, @Nullable List<String> values, @Nullable PresetItem preset,
            @Nullable PresetTagField field, @NonNull Map<String, String> allTags) {
        return getValueAutocompleteAdapter(key, values, preset, field, allTags, false, true, maxInlineValues);
    }

    /**
     * Get an Adapter containing value suggestions for a specific key
     * 
     * Simplified version for non-multi-select and preset only situation
     * 
     * @param key the key for which we are generating the adapter
     * @param values existing values
     * @param preset the PresetItem that matched the tags
     * @param field a PresetField or null
     * @param allTags all the tags of the element
     * @param addRuler add a special value to indicate the position of a ruler
     * @param dedup if true de-duplicate the results
     * @param addMruSize number of values from on we add the full MRU tag list
     * @return an ArrayAdapter for key, or null if something went wrong
     */
    @Nullable
    ArrayAdapter<?> getValueAutocompleteAdapter(@Nullable String key, @Nullable List<String> values, @Nullable PresetItem preset,
            @Nullable PresetTagField field, @NonNull Map<String, String> allTags, boolean addRuler, boolean dedup, int addMruSize) {
        if (Util.notEmpty(key)) {
            Set<String> usedKeys = allTags.keySet();
            if (TagEditorFragment.isStreetName(key, usedKeys)) {
                return nameAdapters.getStreetNameAdapter(values);
            }
            if (TagEditorFragment.isPlaceName(key, usedKeys)) {
                return nameAdapters.getPlaceNameAdapter(values);
            }
            if (key.equals(Tags.KEY_NAME) && (names != null) && TagEditorFragment.useNameSuggestions(usedKeys)) {
                return TagEditorFragment.getNameSuggestions(getContext(), names, allTags, propertyEditorListener);
            }
            // generate from preset
            Map<String, Integer> counter = new HashMap<>();
            int position = 0;
            ArrayAdapterWithRuler<StringWithDescription> adapter2 = new ArrayAdapterWithRuler<>(getActivity(), R.layout.autocomplete_row, Ruler.class);
            boolean isSpeedKey = Tags.isSpeedKey(key) && !Tags.isConditional(key);
            if (preset != null) {
                List<String> regions = propertyEditorListener.getIsoCodes();
                Collection<StringWithDescription> presetValues = field != null ? preset.getAutocompleteValues(field, regions)
                        : preset.getAutocompleteValues(key, regions);
                int presetValuesCount = presetValues.size();
                List<String> mruValues = App.getMruTags().getValues(preset, key);
                if (mruValues != null) {
                    for (String v : mruValues) {
                        StringWithDescription mruValue = preset.getStringWithDescriptionForValue(key, v);
                        if (mruValue == null) {
                            mruValue = new StringWithDescription(v);
                        } else if (presetValuesCount < addMruSize) {
                            // for small numbers only add unknown values
                            continue;
                        }
                        adapter2.add(mruValue);
                        counter.put(v, position++);
                    }
                    addRuler(addRuler, adapter2);
                }
                if (isSpeedKey) {
                    addMaxSpeeds(adapter2);
                }
                Log.d(DEBUG_TAG, "setting autocomplete adapter for values " + presetValues);
                if (!presetValues.isEmpty()) {
                    List<StringWithDescription> result = new ArrayList<>(presetValues);
                    if (preset.sortValues(key)) {
                        Collections.sort(result, comparator);
                    }
                    for (StringWithDescription s : result) {
                        boolean deprecated = (s instanceof ExtendedStringWithDescription) && ((ExtendedStringWithDescription) s).isDeprecated();
                        Integer storedPosition = counter.get(s.getValue());
                        if (storedPosition != null) {
                            if (dedup) {
                                continue;
                            }
                            if (storedPosition >= 0) { // hack so that we retain the descriptions
                                StringWithDescription r = adapter2.getItem(storedPosition);
                                r.setDescription(s.getDescription());
                            }
                        } else if (deprecated) { // skip deprecated values except if it is actually already present
                            if (!values.contains(s.getValue())) {
                                continue;
                            }
                            s = new StringWithDescriptionAndIcon(s);
                            s.setDescription(getContext().getString(R.string.deprecated, s.getDescription()));
                        }
                        adapter2.add(s);
                        counter.put(s.getValue(), position++);
                    }
                    Log.d(DEBUG_TAG, "key " + key + " type " + preset.getKeyType(key));
                }
            } else {
                List<String> mruValues = App.getMruTags().getValues(key);
                if (mruValues != null) {
                    for (String v : mruValues) {
                        adapter2.add(new StringWithDescription(v));
                    }
                }
                addRuler(addRuler, adapter2);
                if (isSpeedKey) {
                    addMaxSpeeds(adapter2);
                }
                if (propertyEditorListener.getPresets() != null) {
                    Log.d(DEBUG_TAG, "generate suggestions for >" + key + "< from presets");
                    // only do this if/ there is no other source of suggestions
                    for (StringWithDescription s : Preset.getAutocompleteValues(propertyEditorListener.getPresets(),
                            propertyEditorListener.getElement().getType(), key)) {
                        adapter2.add(s);
                    }
                }
            }

            if (!counter.containsKey("") && !counter.containsKey(null) && !(field instanceof PresetCheckField)) {
                // add empty value so that we can remove tag
                StringWithDescription s = new StringWithDescription("", getString(R.string.tag_not_set));
                adapter2.insert(s, 0);
            }
            if (values != null) { // add in any non-standard non-empty values
                for (String value : values) {
                    if (Util.notEmpty(value) && !counter.containsKey(value)) {
                        StringWithDescription s = new StringWithDescription(value);
                        adapter2.remove(s);
                        adapter2.insert(s, 0);
                    }
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
     * Add ruler to the adapter
     * 
     * @param addRuler if true add the ruler
     * @param adapter the Adapter
     */
    private void addRuler(boolean addRuler, ArrayAdapterWithRuler<StringWithDescription> adapter) {
        if (addRuler && !adapter.isEmpty()) {
            adapter.add(new Ruler());
        }
    }

    /**
     * Add max speed values to an adapter
     * 
     * @param adapter the adapter
     */
    private void addMaxSpeeds(ArrayAdapter<StringWithDescription> adapter) {
        String[] maxSpeeds = TagEditorFragment.getSpeedLimits(getContext(), propertyEditorListener);
        if (maxSpeeds != null) {
            for (String maxSpeed : maxSpeeds) {
                adapter.add(new StringWithDescription(maxSpeed));
            }
        }
    }

    /**
     * Get an ActivityResultLauncher for an external measuring app
     * 
     * @return the appropriate ActivityResultLauncher
     */
    @NonNull
    ActivityResultLauncher<Params> getMeasureLauncher() {
        return measureLauncher;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.tag_form_menu, menu);
        menu.findItem(R.id.tag_menu_mapfeatures).setEnabled(propertyEditorListener.isConnectedOrConnecting());
        menu.findItem(R.id.tag_menu_paste).setVisible(!App.getTagClipboard(getContext()).isEmpty());
        menu.findItem(R.id.tag_menu_paste_from_clipboard).setVisible(tagListener.pasteFromClipboardIsPossible());

        Properties prop = App.getGeoContext(getContext()).getProperties(propertyEditorListener.getIsoCodes());
        menu.findItem(R.id.tag_menu_locale).setVisible(prop != null && prop.getLanguages() != null);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // disable address prediction for stuff that won't have an address
        OsmElement element = propertyEditorListener.getElement();
        menu.findItem(R.id.tag_menu_address).setVisible((!(element instanceof Way) || ((Way) element).isClosed()));
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Log.d(DEBUG_TAG, "home pressed");
            updateEditorFromText();
            propertyEditorListener.updateAndFinish();
            return true;
        case R.id.tag_menu_address:
            updateEditorFromText();
            tagListener.predictAddressTags(true);
            update();
            if (!focusOnTag(Tags.KEY_ADDR_HOUSENUMBER)) {
                focusOnTag(Tags.KEY_ADDR_STREET);
            }
            return true;
        case R.id.tag_menu_apply_preset:
        case R.id.tag_menu_apply_preset_with_optional:
            PresetItem pi = tagListener.getBestPreset();
            if (pi != null) {
                boolean withOptional = item.getItemId() == R.id.tag_menu_apply_preset_with_optional;
                displayOptional.put(pi, withOptional);
                presetSelectedListener.onPresetSelected(pi, withOptional, false,
                        prefs.applyWithLastValues(getContext(), pi) ? Prefill.FORCE_LAST : Prefill.PRESET);
            }
            return true;
        case R.id.tag_menu_revert:
            doRevert();
            return true;
        case R.id.tag_menu_paste:
            if (tagListener.paste(true)) {
                update();
            }
            return true;
        case R.id.tag_menu_paste_from_clipboard:
            if (tagListener.pasteFromClipboard(true)) {
                update();
            }
            return true;
        case R.id.tag_menu_mapfeatures:
            Wiki.displayMapFeatures(getActivity(), prefs, tagListener.getBestPreset());
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
        case R.id.tag_menu_locale:
            // add locale to any name keys present
            LinkedHashMap<String, String> allTags = tagListener.getKeyValueMapSingle(true);
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            Properties prop = App.getGeoContext(getContext()).getProperties(propertyEditorListener.getIsoCodes());
            String[] languages = prop.getLanguages();
            List<String> i18nKeys = getI18nKeys(tagListener.getBestPreset());
            if (languages != null) {
                for (Entry<String, String> e : allTags.entrySet()) {
                    String key = e.getKey();
                    result.put(key, e.getValue());
                    if (i18nKeys.contains(key)) {
                        for (String language : languages) {
                            String languageKey = key + ":" + language;
                            if (!allTags.containsKey(languageKey)) {
                                result.put(languageKey, "");
                            }
                        }
                    }
                }
            }
            if (result.size() != allTags.size()) {
                tagListener.updateTags(result, true);
                update();
            }
            return true;
        case R.id.tag_menu_help:
            HelpViewer.start(getActivity(), R.string.help_propertyeditor);
            return true;
        default:
            return false;
        }
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
     * 
     * @return true is it worked
     */
    @Override
    public boolean updateEditorFromText() {
        Log.d(DEBUG_TAG, "updating data from last text field");
        // check for focus on text field
        View fragementView = getView();
        if (fragementView == null) {
            return false; // already destroyed?
        }
        LinearLayout l = (LinearLayout) fragementView.findViewById(R.id.form_container_layout);
        if (l == null) {
            return true;
        }
        View v = l.findFocus();
        Log.d(DEBUG_TAG, "focus is on " + v);
        if (v instanceof CustomAutoCompleteTextView || v instanceof EditText) {
            View row = v;
            do {
                row = (View) row.getParent();
            } while (row != null && !(row instanceof TextRow || row instanceof MultiTextRow));
            if (row != null) {
                String rowKey = ((KeyValueRow) row).getKey();
                String rowValue = ((KeyValueRow) row).getValue();
                updateSingleValue(rowKey, rowValue);
                if (row.getParent() instanceof EditableLayout) {
                    ((EditableLayout) row.getParent()).putTag(rowKey, rowValue);
                }
            }
        }
        return true;
    }

    @Override
    public void displayOptional(PresetItem presetItem, boolean optional) {
        displayOptional.put(presetItem, optional);
    }

    /**
     * Return the view we have our rows in and work around some android craziness
     * 
     * @return the non-preset row layout or null if it couldn't be found
     */
    @Nullable
    private View getNonPresetView() {
        // android.support.v4.app.NoSaveStateFrameLayout
        View v = getView();
        if (v != null) {
            if (v.getId() == R.id.form_immutable_row_layout) {
                Log.d(DEBUG_TAG, "got correct view in getView");
                return v;
            } else {
                v = v.findViewById(R.id.form_immutable_row_layout);
                if (v == null) {
                    Log.d(DEBUG_TAG, "didn't find R.id.form_immutable_row_layout");
                } else {
                    Log.d(DEBUG_TAG, "Found R.id.form_immutable_row_layout");
                }
                return v;
            }
        } else {
            Log.d(DEBUG_TAG, "got null view in getView");
        }
        return null;
    }

    /**
     * Update the contents of the Fragment
     */
    public void update() {
        Log.d(DEBUG_TAG, "update");
        // remove all editable stuff
        View sv = getView();
        if (sv == null) {
            Log.e(DEBUG_TAG, "update ScrollView null");
            return;
        }
        LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
        if (ll == null) {
            Log.e(DEBUG_TAG, "update container layout null");
            return;
        }
        while (ll.getChildAt(0) instanceof EditableLayout) {
            ll.removeViewAt(0);
        }
        final EditableLayout editableView = (EditableLayout) inflater.inflate(R.layout.tag_form_editable, ll, false);
        editableView.setSaveEnabled(false);
        int pos = 0;
        ll.addView(editableView, pos++);

        LinearLayout nonEditableView = (LinearLayout) getNonPresetView();
        if (nonEditableView != null && nonEditableView.getChildCount() > 0) {
            nonEditableView.removeAllViews();
        }

        LinkedHashMap<String, String> allTags = tagListener.getKeyValueMapSingle(true);

        PresetItem mainPreset = tagListener.getBestPreset();
        editableView.setTitle(mainPreset, allTags.isEmpty());
        editableView.setListeners(tagListener, this);
        editableView.applyPresetButton.setVisibility(View.GONE);
        editableView.applyPresetWithOptionalButton.setVisibility(View.GONE);

        Map<String, String> nonEditable;
        if (mainPreset != null) {
            nonEditable = addTagsToViews(editableView, mainPreset, allTags);
            for (PresetItem preset : new ArrayList<>(tagListener.getSecondaryPresets())) {
                final EditableLayout editableView1 = (EditableLayout) inflater.inflate(R.layout.tag_form_editable, ll, false);
                editableView1.setSaveEnabled(false);
                editableView1.setTitle(preset, false);
                editableView1.setListeners(tagListener, this);
                ll.addView(editableView1, pos++);
                nonEditable = addTagsToViews(editableView1, preset, nonEditable);
            }
        } else {
            nonEditable = allTags;
        }

        LinearLayout nel = (LinearLayout) getView().findViewById(R.id.form_immutable_header_layout);
        if (nel != null) {
            nel.setVisibility(View.GONE);
            if (nonEditable.size() > 0) {
                nel.setVisibility(View.VISIBLE);
                for (Entry<String, String> entry : nonEditable.entrySet()) {
                    PresetTextField textField = new PresetTextField(entry.getKey());
                    addRow(nonEditableView, textField, entry.getValue(), null, allTags);
                }
            }
        }

        // if we have alternative tagging suggestions display them
        if (displayMRUpresets) {
            de.blau.android.propertyeditor.Util.addAlternativePresetItemsFragment(getChildFragmentManager(), R.id.alternative_layout,
                    tagListener.getBestPreset());
        }

        // some final UI stuff only once
        if (firstUpdate) {
            firstUpdate = false;
            if (focusOnAddress) {
                focusOnAddress = false; // only do it once
                if (!focusOnTag(Tags.KEY_ADDR_HOUSENUMBER) && !focusOnTag(Tags.KEY_ADDR_STREET)) {
                    focusOnEmpty();
                }
                return;
            }
            if (focusTag != null) {
                if (!focusOnTag(focusTag)) {
                    focusOnEmpty();
                }
                focusTag = null;
                return;
            }
            focusOnEmpty();
        }
    }

    /**
     * Given a map of tags and the best matching PresetItem, loop over the fields in the PresetItem creating rows for
     * the tags that have matching keys
     * 
     * @param editableView the Layout holding rows for tags that we were able to identify
     * @param preset the best matching PresetItem
     * @param tags the tags we want display
     * @return a Map containing the tags that coudn't be found in the PresetITem or linked PresetItems
     */
    private Map<String, String> addTagsToViews(@NonNull EditableLayout editableView, @Nullable PresetItem preset, @NonNull Map<String, String> tags) {
        Boolean optional = displayOptional.get(preset);
        LinkedHashMap<PresetField, String> editable = new LinkedHashMap<>();
        LinkedHashMap<PresetField, String> linkedTags = new LinkedHashMap<>();
        Map<String, PresetItem> keyToLinkedPreset = new HashMap<>();
        Map<String, Map<String, String>> checkGroupKeyValues = new HashMap<>();
        boolean groupingRequired = false;
        LinkedHashMap<String, String> tagList = new LinkedHashMap<>(tags);
        if (preset != null) {
            PresetField previous = null;
            // iterate over preset entries so that we maintain ordering
            for (Entry<String, PresetField> entry : preset.getFields().entrySet()) {
                PresetField field = entry.getValue();
                if (field instanceof PresetTagField) {
                    PresetTagField tagField = (PresetTagField) field;
                    String key = tagField.getKey();
                    String value = tagList.get(key);
                    if (value != null) {
                        if (tagField instanceof PresetFixedField) {
                            if (value.equals(((PresetFixedField) tagField).getValue().getValue())) {
                                tagList.remove(key);
                                editableView.putTag(key, value);
                            } // else leave this fixed key for further processing
                        } else if (tagField.getKey().equals(key)) {
                            editable.put(tagField, value);
                            tagList.remove(key);
                            editableView.putTag(key, value);
                        }
                    } else if (tagField instanceof PresetCheckGroupField) {
                        Map<String, String> keyValues = new HashMap<>();
                        for (PresetCheckField check : ((PresetCheckGroupField) tagField).getCheckFields()) {
                            key = check.getKey();
                            value = tagList.get(key);
                            if (value != null) {
                                keyValues.put(key, value);
                                tagList.remove(key);
                                editableView.putTag(key, value);
                            }
                        }
                        if (!keyValues.isEmpty()) {
                            editable.put(tagField, "");
                            checkGroupKeyValues.put(tagField.getKey(), keyValues);
                        }
                    }
                } else if ((field instanceof PresetFormattingField) && (!field.isOptional() || (optional != null && optional))
                        && !(field instanceof PresetSpaceField && (previous == null || previous instanceof PresetSpaceField))) {
                    editable.put(field, "");
                }
                previous = field;
            }
            // process any remaining tags
            List<PresetItem> linkedPresets = preset.getLinkedPresets(true, App.getCurrentPresets(getContext()), propertyEditorListener.getIsoCodes());
            // loop over the tags assigning them to the linked presets
            for (Entry<String, String> e : new ArrayList<>(tagList.entrySet())) {
                String key = e.getKey();
                String value = e.getValue();
                // check if i18n version of a tag
                boolean i18nFound = addI18nKeyToPreset(key, value, preset, editable, editableView);
                if (i18nFound) {
                    groupingRequired = true;
                    tagList.remove(key);
                } else if (linkedPresets != null) { // check if tag is in a linked preset
                    for (PresetItem l : linkedPresets) {
                        if (l.getFixedTagCount() > 0) {
                            continue; // Presets with fixed tags should always get their own entry
                        }
                        PresetTagField field = l.getField(key);
                        if (field != null) {
                            if (field instanceof PresetCheckGroupField) {
                                Map<String, String> keyValues = new HashMap<>();
                                for (PresetCheckField check : ((PresetCheckGroupField) field).getCheckFields()) {
                                    String checkKey = check.getKey();
                                    value = tagList.get(checkKey);
                                    if (value != null) {
                                        keyValues.put(checkKey, value);
                                        editableView.putTag(checkKey, value);
                                        tagList.remove(checkKey);
                                    }
                                }
                                if (!keyValues.isEmpty()) {
                                    linkedTags.put(field, "");
                                    keyToLinkedPreset.put(field.getKey(), l);
                                    checkGroupKeyValues.put(field.getKey(), keyValues);
                                    break;
                                }
                            } else if (field.getKey().equals(key)) {
                                linkedTags.put(field, value);
                                keyToLinkedPreset.put(key, l);
                                editableView.putTag(key, value);
                                tagList.remove(key);
                                break;
                            }
                        }
                        // check if i18n version of a name tag
                        i18nFound = addI18nKeyToPreset(key, value, l, linkedTags, editableView);
                        if (i18nFound) {
                            keyToLinkedPreset.put(key, l);
                            groupingRequired = true;
                            tagList.remove(key);
                            break;
                        }
                    }
                }
            }
        } else {
            Log.e(DEBUG_TAG, "addTagsToViews called with null preset");
        }
        if (groupingRequired) { // this is only true if preset isn't null
            List<String> i18nKeys = getI18nKeys(preset);
            preset.groupI18nKeys(i18nKeys);
            de.blau.android.presets.Util.groupI18nKeys(i18nKeys, editable);
            de.blau.android.presets.Util.groupI18nKeys(i18nKeys, linkedTags);
        }
        de.blau.android.presets.Util.groupAddrKeys(linkedTags);
        for (Entry<PresetField, String> entry : editable.entrySet()) {
            addFieldToView(editableView, preset, tags, checkGroupKeyValues, entry);
        }
        for (Entry<PresetField, String> entry : linkedTags.entrySet()) {
            final PresetField field = entry.getKey();
            PresetItem linkedItem = field instanceof PresetTagField ? keyToLinkedPreset.get(((PresetTagField) field).getKey()) : null;
            addFieldToView(editableView, linkedItem, tags, checkGroupKeyValues, entry);
        }

        return tagList;
    }

    /**
     * Add Fields to a view
     * 
     * @param editableView the target view
     * @param preset the PresetItem the Field belongs to
     * @param tags the Tags for the Object
     * @param checkGroupKeyValues a map of keys for a CheckGroup
     * @param entry the Entry containing the FIeld
     */
    private void addFieldToView(@NonNull EditableLayout editableView, PresetItem preset, @NonNull Map<String, String> tags,
            @NonNull Map<String, Map<String, String>> checkGroupKeyValues, @NonNull Entry<PresetField, String> entry) {
        PresetField field = entry.getKey();
        if (field instanceof PresetCheckGroupField) {
            CheckGroupDialogRow.getRow(this, inflater, editableView, (PresetCheckGroupField) field,
                    checkGroupKeyValues.get(((PresetCheckGroupField) field).getKey()), preset, tags);
        } else if (field instanceof PresetTagField) {
            addRow(editableView, (PresetTagField) field, entry.getValue(), preset, tags);
        } else if (field instanceof PresetLabelField) {
            editableView.addView(LabelRow.getRow(inflater, editableView, (PresetLabelField) field));
        } else if (field instanceof PresetFormattingField) {
            editableView.addView(FormattingRow.getRow(inflater, editableView, (PresetFormattingField) field));
        }
    }

    /**
     * Return the list of relevant i18n keys
     * 
     * @param preset in use PresetItem
     * @return list containing keys that potentially have i18n variants
     */
    @NonNull
    private List<String> getI18nKeys(@Nullable PresetItem preset) {
        List<String> i18nKeys = new ArrayList<>();
        if (preset != null) {
            i18nKeys.addAll(preset.getI18nKeys());
        }
        i18nKeys.addAll(Tags.I18N_NAME_KEYS);
        return i18nKeys;
    }

    /**
     * Add internationalized keys to preset and to their resp. maps so that the entries will match
     * 
     * @param key base key
     * @param value value
     * @param preset current preset
     * @param editableMap target map containing the recommended tags
     * @param editableView out current layout
     * @return true if i18n variants were added
     */
    private boolean addI18nKeyToPreset(@NonNull String key, String value, PresetItem preset, @NonNull Map<PresetField, String> editableMap,
            @NonNull EditableLayout editableView) {
        if (preset != null) {
            List<String> i18nKeys = getI18nKeys(preset);
            for (String tag : i18nKeys) {
                if (key.startsWith(tag + ":") && preset.hasKey(tag)) {
                    String i18nPart = key.substring(tag.length() + 1);
                    boolean optional = preset.isOptionalTag(tag);
                    PresetTextField field = new PresetTextField(key);
                    preset.addTag(optional, key, PresetKeyType.TEXT, null, MatchType.NONE);
                    String hint = preset.getHint(tag);
                    if (hint != null) {
                        preset.setHint(key, getActivity().getString(R.string.internationalized_hint, hint, i18nPart));
                    }
                    editableMap.put(field, value);
                    editableView.putTag(key, value);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add a row per key, determines the kind of row based on the preset field type and the number of values
     * 
     * @param rowLayout the Layout holding the rows
     * @param field the PresetField for this row
     * @param value the current value is any
     * @param preset the Preset we believe the key belongs to
     * @param allTags the other tags for this object
     */
    void addRow(@Nullable final LinearLayout rowLayout, @NonNull final PresetTagField field, final String value, @Nullable PresetItem preset,
            @NonNull Map<String, String> allTags) {
        final String key = field.getKey();
        if (rowLayout == null) {
            Log.e(DEBUG_TAG, "addRow rowLayout null");
            return;
        }
        if (field instanceof PresetFixedField) {
            Log.e(DEBUG_TAG, "addRow called for fixed field " + field);
            return;
        }
        final boolean longString = value != null && longStringLimit <= value.length();
        if (preset == null) { // no preset here so we can only handle hardwired stuff specially
            if (key.endsWith(Tags.KEY_CONDITIONAL_SUFFIX)) {
                rowLayout.addView(getConditionalRestrictionDialogRow(rowLayout, null, null, key, value, null, allTags));
                return;
            }
            if (Tags.OPENING_HOURS_SYNTAX.contains(key)) {
                rowLayout.addView(OpeningHoursDialogRow.getRow(this, inflater, rowLayout, null, null, key, value, null));
                return;
            }
            if (longString) {
                rowLayout.addView(LongTextDialogRow.getRow(this, inflater, rowLayout, null, (PresetTextField) field, value, maxStringLength));
                return;
            }
            rowLayout.addView(TextRow.getRow(this, inflater, rowLayout, null, new PresetTextField(key), value, null, allTags));
            return;
        }

        List<String> values = null;
        boolean isCheckField = field instanceof PresetCheckField;
        boolean isComboField = field instanceof PresetComboField && !((PresetComboField) field).isMultiSelect();
        boolean isMultiSelectField = field instanceof PresetComboField && ((PresetComboField) field).isMultiSelect();
        if (isMultiSelectField) {
            values = Preset.splitValues(Util.wrapInList(value), preset, key);
        } else {
            values = Util.wrapInList(value);
        }
        String hint = field.getHint();
        if (field.isDeprecated() && Util.notEmpty(hint)) {
            hint = getString(R.string.deprecated, hint);
        }
        //
        ValueType valueType = field.getValueType();
        if (field instanceof PresetTextField || key.startsWith(Tags.KEY_ADDR_BASE)
                || (isComboField && ((PresetComboField) field).isEditable() && ValueType.OPENING_HOURS_MIXED != valueType) || Tags.isConditional(key)
                || ValueType.INTEGER == valueType || ValueType.CARDINAL_DIRECTION == valueType) {
            if (Tags.isConditional(key)) {
                rowLayout.addView(getConditionalRestrictionDialogRow(rowLayout, preset, hint, key, value, values, allTags));
                return;
            }
            if (isOpeningHours(key, valueType)) {
                rowLayout.addView(OpeningHoursDialogRow.getRow(this, inflater, rowLayout, preset, hint, key, value, null));
                return;
            }
            if (ValueType.PHONE == valueType) {
                rowLayout.addView(MultiTextRow.getRow(this, inflater, rowLayout, preset, hint, key, values, null, null, null, null));
                return;
            }
            if (ValueType.WEBSITE == valueType || Tags.isWebsiteKey(key)) {
                rowLayout.addView(UrlDialogRow.getRow(this, inflater, rowLayout, preset, hint, key, value));
                return;
            }
            if (field instanceof PresetTextField && (longStringLimit <= ((PresetTextField) field).length() || longString)) {
                rowLayout.addView(LongTextDialogRow.getRow(this, inflater, rowLayout, preset, (PresetTextField) field, value, maxStringLength));
                return;
            }
            if ( ValueType.INTEGER == valueType || ValueType.CARDINAL_DIRECTION == valueType) {
                rowLayout.addView(ValueWidgetRow.getRow(this, inflater, rowLayout, preset, field, value, values, allTags));
                return;
            }
            rowLayout.addView(TextRow.getRow(this, inflater, rowLayout, preset, field, value, values, allTags));
            return;
        }

        ArrayAdapter<?> adapter = getValueAutocompleteAdapter(key, values, preset, field, allTags, true, true, maxInlineValues * 2);
        int count = 0;
        if (adapter != null) {
            // adapters other than for PresetCheckField have an empty value added that we don't want to
            // count
            count = adapter.getCount() - (isCheckField ? 0 : 1);
        } else {
            Log.d(DEBUG_TAG, "adapter null " + key + " " + value + " " + preset);
        }
        if (isComboField || (isCheckField && count > 2)) {
            if (isOpeningHours(key, valueType)) {
                rowLayout.addView(OpeningHoursDialogRow.getRow(this, inflater, rowLayout, preset, hint, key, value, adapter));
                return;
            }
            if (count <= maxInlineValues) {
                rowLayout.addView(ComboRow.getRow(this, inflater, rowLayout, preset, hint, key, value, adapter));
                return;
            }
            rowLayout.addView(ComboDialogRow.getRow(this, inflater, rowLayout, preset, hint, key, value, adapter));
            return;
        }
        if (isMultiSelectField) {
            if (((PresetComboField) field).isEditable()) {
                String valueCountKey = ((PresetComboField) field).getValueCountKey();
                String valueCountValue = valueCountKey != null ? allTags.get(valueCountKey) : null;
                MultiTextRow row = MultiTextRow.getRow(this, inflater, rowLayout, preset, hint, key, values, null, valueCountKey, valueCountValue, adapter);
                row.changed(valueCountKey, valueCountValue);
                rowLayout.addView(row);
                return;
            }
            if (count <= maxInlineValues) {
                rowLayout.addView(MultiselectRow.getRow(this, inflater, rowLayout, preset, hint, key, values, adapter));
                return;
            }
            rowLayout.addView(MultiselectDialogRow.getRow(this, inflater, rowLayout, preset, hint, key, values, adapter));
            return;
        }
        if (isCheckField) {
            final String valueOn = ((PresetCheckField) field).getOnValue().getValue();
            StringWithDescription tempOff = ((PresetCheckField) field).getOffValue();
            final String valueOff = tempOff == null ? "" : tempOff.getValue();
            String description = tempOff == null ? "" : tempOff.getDescription();
            if (Util.isEmpty(description)) {
                description = valueOff;
            }
            final CheckRow row = (CheckRow) inflater.inflate(R.layout.tag_form_check_row, rowLayout, false);
            row.getKeyView().setText(hint != null ? hint : key);
            row.getKeyView().setTag(key);
            IndeterminateCheckBox checkBox = row.getCheckBox();
            checkBox.setIndeterminate(tempOff != null && (value == null || "".equals(value))); // tri-state
                                                                                               // needed
            if (!checkBox.isIndeterminate()) {
                checkBox.setChecked(valueOn != null && valueOn.equals(value));
            }
            rowLayout.addView(row);
            checkBox.setOnStateChangedListener((check, state) -> {
                String checkValue = state != null ? (state ? valueOn : valueOff) : ""; // NOSONAR
                updateSingleValue(key, checkValue);
                if (rowLayout instanceof EditableLayout) {
                    ((EditableLayout) rowLayout).putTag(key, checkValue);
                }
            });
            return;
        }
        Log.e(DEBUG_TAG, "unknown preset element type " + key + " " + value + " " + preset.getName());
    }

    /**
     * Check if a key has opening_hours semantics
     * 
     * @param key the key
     * @param valueType the ValueType
     * @return true if the key has opening_hours semantics
     */
    public boolean isOpeningHours(@NonNull final String key, @NonNull ValueType valueType) {
        return Tags.OPENING_HOURS_SYNTAX.contains(key) || ValueType.OPENING_HOURS == valueType || ValueType.OPENING_HOURS_MIXED == valueType;
    }

    /**
     * Add a row for a conditional restriction that will start the corresponding editor if clicked
     * 
     * @param rowLayout the Layout holding the row
     * @param preset the best matched PresetITem for the key
     * @param hint a textual description of what the key is
     * @param key the key
     * @param value existing value for the tag
     * @param values a list containing all the predefined values in the PresetItem for the key
     * @param allTags a Map of the tags currently being edited
     * @return a DialogRow instance
     */
    @NonNull
    private DialogRow getConditionalRestrictionDialogRow(@Nullable LinearLayout rowLayout, @Nullable PresetItem preset, @Nullable final String hint,
            @Nullable final String key, @Nullable final String value, @Nullable final List<String> values, Map<String, String> allTags) {
        final DialogRow row = (DialogRow) inflater.inflate(R.layout.tag_form_combo_dialog_row, rowLayout, false);
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        row.setPreset(preset);

        final ArrayList<String> templates = new ArrayList<>();
        if (values != null) {
            ArrayAdapter<?> adapter = getValueAutocompleteAdapter(key, values, preset, null, allTags);
            if (adapter != null) {
                Log.d(DEBUG_TAG, "adapter size " + adapter.getCount());
                for (int i = 0; i < adapter.getCount(); i++) {
                    Object o = adapter.getItem(i);
                    StringWithDescription swd = new StringWithDescription(o);
                    Log.d(DEBUG_TAG, "adding " + swd);
                    String v = swd.getValue();
                    if (Util.isEmpty(v)) {
                        continue;
                    }
                    Log.d(DEBUG_TAG, "adding " + v + " to templates");
                    templates.add(v);
                }
            }
        }
        if (Util.notEmpty(value)) {
            ConditionalRestrictionParser parser = new ConditionalRestrictionParser(new ByteArrayInputStream(value.getBytes())); // NOSONAR
                                                                                                                                // can't
                                                                                                                                // be
                                                                                                                                // null
            try {
                row.setValue(ch.poole.conditionalrestrictionparser.Util.prettyPrint(parser.restrictions()));
            } catch (Exception ex) {
                row.setValue(value);
            }
        }
        final ArrayList<String> ohTemplates = new ArrayList<>();
        for (StringWithDescription s : Preset.getAutocompleteValues(propertyEditorListener.getPresets(), propertyEditorListener.getElement().getType(),
                Tags.KEY_OPENING_HOURS)) {
            ohTemplates.add(s.getValue());
        }
        row.valueView.setHint(R.string.tag_tap_to_edit_hint);
        row.setOnClickListener(v -> {
            FragmentManager fm = getChildFragmentManager();
            de.blau.android.propertyeditor.Util.removeChildFragment(fm, FRAGMENT_CONDITIONAL_RESTRICTION_TAG);
            ConditionalRestrictionFragment conditionalRestrictionDialog = ConditionalRestrictionFragment.newInstance(key, value, templates, ohTemplates,
                    maxStringLength);
            conditionalRestrictionDialog.show(fm, FRAGMENT_CONDITIONAL_RESTRICTION_TAG);
        });
        return row;
    }

    /**
     * Focus on the value field of a tag with key "key"
     * 
     * @param key the key value we want to focus on
     * @return true if the key was found
     */
    public boolean focusOnTag(@NonNull String key) {
        boolean found = false;
        View sv = getView();
        LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
        if (ll == null) {
            Log.d(DEBUG_TAG, "focusOnTag container layout null");
            return false;
        }
        int pos = 0;
        while (ll.getChildAt(pos) instanceof EditableLayout && pos < ll.getChildCount() && !found) {
            EditableLayout ll2 = (EditableLayout) ll.getChildAt(pos);
            Log.d(DEBUG_TAG, "focusOnTag key " + key);
            for (int i = ll2.getChildCount() - 1; i >= 0; --i) {
                View v = ll2.getChildAt(i);
                boolean isTextRow = v instanceof TextRow;
                if ((v instanceof DialogRow || isTextRow) && ((KeyValueRow) v).hasKey(key)) {
                    Util.scrollToRow(sv, v, true, true);
                    if (isTextRow) {
                        ((TextRow) v).getValueView().requestFocus();
                    } else {
                        ((DialogRow) v).click();
                    }
                    found = true;
                    break;
                }
            }
            pos++;
        }
        return found;
    }

    /**
     * Focus on the first empty value field
     * 
     * @return true if we found an empty text field
     */
    private boolean focusOnEmpty() {
        boolean found = false;
        LinearLayout ll = (LinearLayout) getView().findViewById(R.id.form_container_layout);
        if (ll == null) {
            Log.d(DEBUG_TAG, "container layout null");
            return false;
        }
        int pos = 0;
        while (ll.getChildAt(pos) instanceof EditableLayout && pos < ll.getChildCount() && !found) {
            EditableLayout ll2 = (EditableLayout) ll.getChildAt(pos);
            for (int i = 0; i < ll2.getChildCount(); i++) {
                View v = ll2.getChildAt(i);
                // we currently only focus on TextRows without a special ValueType
                if (v instanceof TextRow && ((TextRow) v).initialFoxus(getContext())) {
                    ((TextRow) v).getValueView().requestFocus();
                    found = true;
                    break;
                }
            }
            pos++;
        }
        return found;
    }

    /**
     * Find a row for a specific key
     * 
     * @param key the key to search for
     * @return the Row or null
     */
    @Nullable
    public View getRow(@NonNull String key) {
        LinearLayout ll = (LinearLayout) getView().findViewById(R.id.form_container_layout);
        if (ll != null) {
            int pos = 0;
            while (pos < ll.getChildCount()) {
                final View child = ll.getChildAt(pos);
                pos++;
                if (!(child instanceof LinearLayout)) {
                    continue;
                }
                LinearLayout ll2 = (LinearLayout) child;
                for (int i = ll2.getChildCount() - 1; i >= 0; --i) {
                    View v = ll2.getChildAt(i);
                    if (v instanceof KeyValueRow && ((KeyValueRow) v).hasKey(key)) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Update a DialogRow
     * 
     * @param key the key
     * @param value the new value
     */
    public void updateDialogRow(@NonNull String key, @NonNull StringWithDescription value) {
        View row = getRow(key);
        if (row instanceof DialogRow) {
            ((DialogRow) row).setValue(value);
            ((DialogRow) row).setChanged(true);
        }
    }

    /**
     * Update a DialogRow
     * 
     * @param key the key
     * @param valueList a list of new values
     */
    public void updateDialogRow(@NonNull String key, @NonNull List<StringWithDescription> valueList) {
        View row = getRow(key);
        if (row instanceof MultiselectDialogRow) {
            ((MultiselectDialogRow) row).setValue(valueList);
            ((DialogRow) row).setChanged(true);
        }
    }

    /**
     * Update a DialogRow
     * 
     * @param key the key
     * @param tags a Map of selected tags
     */
    public void updateDialogRow(@NonNull String key, @NonNull final Map<String, String> tags) {
        View row = getRow(key);
        if (row instanceof CheckGroupDialogRow) {
            ((CheckGroupDialogRow) row).setSelectedValues(tags);
            ((DialogRow) row).setChanged(true);
        }
    }

    /**
     * A Layout representing the tags the match a specific PresetItem
     * 
     * @author simon
     *
     */
    public static class EditableLayout extends LinearLayout {
        private ImageView                     headerIconView;
        private TextView                      headerTitleView;
        private ImageButton                   applyPresetButton;
        private ImageButton                   applyPresetWithOptionalButton;
        private ImageButton                   copyButton;
        private ImageButton                   cutButton;
        private ImageButton                   deleteButton;
        private PresetItem                    preset;
        private LinkedHashMap<String, String> tags = new LinkedHashMap<>();

        /**
         * Construct a Layout representing the tags the match a specific PresetItem
         * 
         * @param context Android Context
         */
        public EditableLayout(@NonNull Context context) {
            super(context);
        }

        /**
         * Construct a Layout representing the tags that match a specific PresetItem
         * 
         * @param context Android Context
         * @param attrs an AttributeSet
         */
        public EditableLayout(@NonNull Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            headerIconView = (ImageView) findViewById(R.id.form_header_icon_view);
            headerTitleView = (TextView) findViewById(R.id.form_header_title);
            applyPresetButton = (ImageButton) findViewById(R.id.tag_menu_apply_preset);
            applyPresetWithOptionalButton = (ImageButton) findViewById(R.id.tag_menu_apply_preset_with_optional);
            copyButton = (ImageButton) findViewById(R.id.form_header_copy);
            cutButton = (ImageButton) findViewById(R.id.form_header_cut);
            deleteButton = (ImageButton) findViewById(R.id.form_header_delete);
        }

        /**
         * As side effect this sets the onClickListeners for the buttons
         * 
         * @param editorListener the Listener called when we change the data
         * @param formListener Listener to call after changes to update the form
         */
        public void setListeners(@NonNull final EditorUpdate editorListener, @NonNull final FormUpdate formListener) {
            Log.d(DEBUG_TAG, "setting listeners");
            applyPresetButton.setOnClickListener(v -> {
                formListener.displayOptional(preset, false);
                editorListener.applyPreset(preset, false);
                formListener.tagsUpdated();
            });
            applyPresetWithOptionalButton.setOnClickListener(v -> {
                formListener.displayOptional(preset, true);
                editorListener.applyPreset(preset, true);
                formListener.tagsUpdated();
            });
            copyButton.setOnClickListener(v -> {
                formListener.updateEditorFromText();
                App.getTagClipboard(getContext()).copy(tags);
                ScreenMessage.toastTopInfo(getContext(), R.string.toast_tags_copied);
            });
            cutButton.setOnClickListener(v -> {
                formListener.updateEditorFromText();
                App.getTagClipboard(getContext()).copy(tags);
                for (String key : tags.keySet()) {
                    editorListener.deleteTag(key);
                }
                editorListener.updatePresets();
                formListener.tagsUpdated();
                ScreenMessage.toastTopInfo(getContext(), R.string.toast_tags_cut);
            });
            deleteButton.setOnClickListener(v -> {
                Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage(v.getContext().getString(R.string.delete_tags, headerTitleView.getText()));
                builder.setNegativeButton(R.string.cancel, null);
                builder.setPositiveButton(R.string.okay, (dialog, which) -> {
                    for (String key : tags.keySet()) {
                        editorListener.deleteTag(key);
                    }
                    editorListener.updatePresets();
                    formListener.tagsUpdated();
                });
                builder.show();
            });
        }

        /**
         * Set the title and PresetItem
         * 
         * @param preset the PresetItem
         * @param untagged the object has no tags
         */
        public void setTitle(@Nullable PresetItem preset, boolean untagged) {
            if (preset != null) {
                Drawable icon = preset.getIconIfExists(getContext(), preset.getIconpath());
                this.preset = preset;
                if (icon != null) {
                    headerIconView.setVisibility(View.VISIBLE);
                    // NOTE directly using the icon seems to trash it, so make a copy
                    headerIconView.setImageDrawable(icon.getConstantState().newDrawable());
                } else {
                    headerIconView.setVisibility(View.GONE);
                }
                headerTitleView.setText(preset.getDisplayName(getContext()));
                applyPresetButton.setVisibility(View.VISIBLE);
                applyPresetWithOptionalButton.setVisibility(View.VISIBLE);
                copyButton.setVisibility(View.VISIBLE);
                cutButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);
            } else {
                headerTitleView.setText(untagged ? R.string.tag_form_untagged_element : R.string.tag_form_unknown_element);
                applyPresetButton.setVisibility(View.GONE);
                applyPresetWithOptionalButton.setVisibility(View.GONE);
                copyButton.setVisibility(View.GONE);
                cutButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
            }
        }

        /**
         * Add a tag to the ones used for this Layout
         * 
         * @param key the key
         * @param value the value
         */
        public void putTag(String key, String value) {
            tags.put(key, value);
        }
    }

    @Override
    public void tagsUpdated() {
        update();
    }

    /*
     * The following methods allow us to intercept calls to the tag editor
     */

    /**
     * Update or add a single key value pair in the tag editor
     * 
     * @param key the key
     * @param value the value
     */
    void updateSingleValue(@NonNull final String key, @NonNull final String value) {
        tagListener.updateSingleValue(key, value);
        iterateOverRows((View row) -> {
            if (row instanceof TagChanged) {
                ((TagChanged) row).changed(key, value);
            }
        });
    }

    /**
     * Enable a specific text row
     * 
     * @param key the key of the row
     */
    void enableTextRow(@NonNull final String key) {
        iterateOverRows((View row) -> {
            if (row instanceof TextRow && key.equals(((TextRow) row).getKey())) {
                ((TextRow) row).getValueView().setEnabled(true);
            }
        });
    }

    private interface ProcessRow {
        /**
         * Perform an action on a row
         * 
         * @param row the row view
         */
        void process(@NonNull View row);
    }

    /**
     * Iterate over all the rows and perform an action on them
     * 
     * @param processRow the callback to use
     */
    private void iterateOverRows(@NonNull ProcessRow processRow) {
        LinearLayout ll = (LinearLayout) getView().findViewById(R.id.form_container_layout);
        if (ll != null) {
            int childCount = ll.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View v = ll.getChildAt(i);
                if (v instanceof EditableLayout || v instanceof LinearLayout) {
                    int editableChildCount = ((LinearLayout) v).getChildCount();
                    for (int j = 0; j < editableChildCount; j++) {
                        processRow.process(((LinearLayout) v).getChildAt(j));
                    }
                }
            }
        }
    }

    /**
     * Update or add multiple keys
     * 
     * @param tags map containing the new key - value pairs
     * @param flush if true delete all existing tags before applying the update
     */
    void updateTags(@NonNull final Map<String, String> tags, final boolean flush) {
        tagListener.updateTags(tags, flush);
    }

    /**
     * Get tags from tag editor
     * 
     * @param allowBlanks allow blank values
     * @return a LinkedHashMap of the tags
     */
    @Nullable
    LinkedHashMap<String, String> getKeyValueMapSingle(final boolean allowBlanks) {
        return tagListener.getKeyValueMapSingle(allowBlanks);
    }

    /**
     * Apply tag suggestion from name index
     * 
     * @param tags a map with the tags
     * @param afterApply run this after applying additional tags
     */
    void applyTagSuggestions(@NonNull Names.TagMap tags, @Nullable Runnable afterApply) {
        tagListener.applyTagSuggestions(tags, afterApply);
    }
}
