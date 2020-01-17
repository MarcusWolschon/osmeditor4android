package de.blau.android.propertyeditor.tagform;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.buildware.widget.indeterm.IndeterminateCheckBox;
import com.buildware.widget.indeterm.IndeterminateCheckBox.OnStateChangedListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import ch.poole.conditionalrestrictionparser.ConditionalRestrictionParser;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Server;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Wiki;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.PresetKeyType;
import de.blau.android.presets.Preset.ValueType;
import de.blau.android.presets.PresetCheckField;
import de.blau.android.presets.PresetCheckGroupField;
import de.blau.android.presets.PresetComboField;
import de.blau.android.presets.PresetField;
import de.blau.android.presets.PresetFixedField;
import de.blau.android.presets.PresetTextField;
import de.blau.android.propertyeditor.Address;
import de.blau.android.propertyeditor.EditorUpdate;
import de.blau.android.propertyeditor.FormUpdate;
import de.blau.android.propertyeditor.NameAdapters;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.propertyeditor.PropertyEditor;
import de.blau.android.propertyeditor.PropertyEditorListener;
import de.blau.android.propertyeditor.RecentPresetsFragment;
import de.blau.android.propertyeditor.TagEditorFragment;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.GeoContext.Properties;
import de.blau.android.util.Snack;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Util;
import de.blau.android.views.CustomAutoCompleteTextView;

public class TagFormFragment extends BaseFragment implements FormUpdate {

    private static final String DEBUG_TAG = TagFormFragment.class.getSimpleName();

    private static final String FRAGMENT_CONDITIONAL_RESTRICTION_TAG = "fragment_conditional_restriction";

    private static final String FOCUS_TAG = "focusTag";

    private static final String FOCUS_ON_ADDRESS = "focusOnAddress";

    private static final String DISPLAY_MRU_PRESETS = "displayMRUpresets";

    private LayoutInflater inflater = null;

    private Names names = null;

    Preferences prefs = null;

    PropertyEditorListener propertyEditorListener;

    OnPresetSelectedListener presetSelectedListener;

    EditorUpdate tagListener = null;

    private NameAdapters nameAdapters = null;

    private boolean focusOnAddress = false;

    private String focusTag = null;

    int maxInlineValues = 3;

    int maxStringLength; // maximum key, value and role length

    private StringWithDescription.LocaleComparator comparator;

    /**
     * Create a new instance of the fragment
     * 
     * @param displayMRUpresets display the MRU list of Presets
     * @param focusOnAddress focus on any address keys
     * @param focusTag focus on this tag
     * @return a TagFormFragment instance
     */
    public static TagFormFragment newInstance(boolean displayMRUpresets, boolean focusOnAddress, String focusTag) {
        TagFormFragment f = new TagFormFragment();

        Bundle args = new Bundle();

        args.putSerializable(DISPLAY_MRU_PRESETS, displayMRUpresets);
        args.putSerializable(FOCUS_ON_ADDRESS, focusOnAddress);
        args.putSerializable(FOCUS_TAG, focusTag);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttachToContext(Context context) {
        Log.d(DEBUG_TAG, "onAttachToContext");
        try {
            tagListener = (EditorUpdate) context;
            nameAdapters = (NameAdapters) context;
            propertyEditorListener = (PropertyEditorListener) context;
            presetSelectedListener = (OnPresetSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    context.toString() + " must implement OnPresetSelectedListener, NameAdapters, PropertyEditorListener, OnPresetSelectedListener");
        }
        setHasOptionsMenu(true);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
        comparator = new StringWithDescription.LocaleComparator();
    }

    /**
     * display member elements of the relation if any
     * 
     * @param members
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ScrollView rowLayout = null;

        if (savedInstanceState == null) {
            // No previous state to restore - get the state from the intent
            Log.d(DEBUG_TAG, "Initializing from original arguments");
        } else {
            // Restore activity from saved state
            Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
        }

        this.inflater = inflater;
        rowLayout = (ScrollView) inflater.inflate(R.layout.tag_form_view, container, false);

        boolean displayMRUpresets = (Boolean) getArguments().getSerializable(DISPLAY_MRU_PRESETS);
        focusOnAddress = (Boolean) getArguments().getSerializable(FOCUS_ON_ADDRESS);
        focusTag = getArguments().getString(FOCUS_TAG);

        if (getUserVisibleHint()) { // don't request focus if we are not visible
            Log.d(DEBUG_TAG, "is visible");
        }
        //
        prefs = new Preferences(getActivity());

        if (prefs.getEnableNameSuggestions()) {
            names = App.getNames(getActivity());
        }

        maxInlineValues = prefs.getMaxInlineValues();

        Server server = prefs.getServer();
        maxStringLength = server.getCachedCapabilities().getMaxStringLength();

        if (displayMRUpresets) {
            Log.d(DEBUG_TAG, "Adding MRU prests");
            FragmentManager fm = getChildFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
            if (recentPresetsFragment != null) {
                ft.remove(recentPresetsFragment);
            }
            // FIXME multiselect or what?
            recentPresetsFragment = RecentPresetsFragment.newInstance(propertyEditorListener.getElement().getOsmId(),
                    propertyEditorListener.getElement().getName());
            ft.add(R.id.form_mru_layout, recentPresetsFragment, "recentpresets_fragment");
            ft.commit();
        }

        Log.d(DEBUG_TAG, "onCreateView returning");
        return rowLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }

    @Override
    public void onDestroyView() {
        // remove onFocusChangeListeners or else bad things might happen (at least with API 23)
        ViewGroup v = (ViewGroup) getView();
        if (v != null) {
            loopViews(v);
        }
        super.onDestroyView();
        Log.d(DEBUG_TAG, "onDestroyView");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        updateEditorFromText();
    }

    /**
     * Recursively loop over the child Views of the ViewGroup and remove onFocusChangeListeners, might be worth it to
     * make this more generic
     * 
     * @param viewGroup the ViewGroup
     */
    private void loopViews(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View v = viewGroup.getChildAt(i);
            if (v instanceof ViewGroup) {
                this.loopViews((ViewGroup) v);
            } else {
                viewGroup.setOnFocusChangeListener(null);
            }
        }
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
    ArrayAdapter<?> getValueAutocompleteAdapter(@Nullable String key, @Nullable List<String> values, @Nullable PresetItem preset, @Nullable PresetField field,
            @NonNull Map<String, String> allTags) {
        ArrayAdapter<?> adapter = null;

        if (key != null && key.length() > 0) {
            Set<String> usedKeys = allTags.keySet();

            if (TagEditorFragment.isStreetName(key, usedKeys)) {
                adapter = nameAdapters.getStreetNameAdapter(values);
            } else if (TagEditorFragment.isPlaceName(key, usedKeys)) {
                adapter = nameAdapters.getPlaceNameAdapter(values);
            } else if (key.equals(Tags.KEY_NAME) && (names != null) && TagEditorFragment.useNameSuggestions(usedKeys)) {
                Log.d(DEBUG_TAG, "generate suggestions for name from name suggestion index");
                List<NameAndTags> suggestions = names.getNames(new TreeMap<>(allTags), propertyEditorListener.getIsoCodes());
                if (suggestions != null && !suggestions.isEmpty()) {
                    List<NameAndTags> result = suggestions;
                    Collections.sort(result);
                    adapter = new ArrayAdapter<>(getActivity(), R.layout.autocomplete_row, result);
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
                int position = 0;
                ArrayAdapter<StringWithDescription> adapter2 = new ArrayAdapter<>(getActivity(), R.layout.autocomplete_row);
                if (preset != null) {
                    Collection<StringWithDescription> presetValues;
                    if (field != null) {
                        presetValues = preset.getAutocompleteValues(field);
                    } else {
                        presetValues = preset.getAutocompleteValues(key);
                    }
                    List<String> mruValues = App.getMruTags().getValues(preset, key);
                    if (mruValues != null) {
                        for (String v : mruValues) {
                            StringWithDescription mruValue = null;
                            for (StringWithDescription swd : presetValues) {
                                if (v.equals(swd.getValue())) {
                                    mruValue = swd;
                                    break;
                                }
                            }
                            if (mruValue == null) {
                                mruValue = new StringWithDescription(v);
                            }
                            adapter2.add(mruValue);
                            counter.put(v, position++);
                        }
                    }
                    Log.d(DEBUG_TAG, "setting autocomplete adapter for values " + presetValues);
                    if (!presetValues.isEmpty()) {
                        List<StringWithDescription> result = new ArrayList<>(presetValues);
                        if (preset.sortValues(key)) {
                            Collections.sort(result, comparator);
                        }
                        for (StringWithDescription s : result) {
                            Integer storedPosition = counter.get(s.getValue());
                            if (storedPosition != null) {
                                if (storedPosition >= 0) { // hack so that we retain the descriptions
                                    StringWithDescription r = adapter2.getItem(storedPosition);
                                    r.setDescription(s.getDescription());
                                }
                                continue; // skip stuff that is already listed
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
                    OsmElement element = propertyEditorListener.getElement();
                    if (propertyEditorListener.getPresets() != null) {
                        Log.d(DEBUG_TAG, "generate suggestions for >" + key + "< from presets");
                        // only do this if/ there is no other source of suggestions
                        for (StringWithDescription s : Preset.getAutocompleteValues(propertyEditorListener.getPresets(), element.getType(), key)) {
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
                        if (!"".equals(value) && !counter.containsKey(value)) {
                            StringWithDescription s = new StringWithDescription(value);
                            // FIXME determine description in some way
                            // ValueWithCount v = new ValueWithCount(value, 1);
                            adapter2.remove(s);
                            adapter2.insert(s, 0);
                        }
                    }
                }
                Log.d(DEBUG_TAG, adapter2 == null ? "adapter2 is null" : "adapter2 has " + adapter2.getCount() + " elements");
                if (adapter2.getCount() > 0) {
                    return adapter2;
                }
            }
        }
        Log.d(DEBUG_TAG, adapter == null ? "adapter is null" : "adapter has " + adapter.getCount() + " elements");
        return adapter;
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Log.d(DEBUG_TAG, "home pressed");
            updateEditorFromText();
            ((PropertyEditor) getActivity()).sendResultAndFinish();
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
                presetSelectedListener.onPresetSelected(pi, item.getItemId() == R.id.tag_menu_apply_preset_with_optional);
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
            ((PropertyEditor) getActivity()).recreateRecentPresetView();
            return true;
        case R.id.tag_menu_reset_address_prediction:
            // simply overwrite with an empty file
            Address.resetLastAddresses(getActivity());
            return true;
        case R.id.tag_menu_locale:
            // add locale to any name keys present
            LinkedHashMap<String, String> allTags = tagListener.getKeyValueMapSingle(true);
            if (allTags == null) {
                Log.e(DEBUG_TAG, "getKeyValueMapSingle returned null");
                return true;
            }
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
        if (l != null) { // FIXME this might need an alert
            View v = l.findFocus();
            Log.d(DEBUG_TAG, "focus is on " + v);
            if (v instanceof CustomAutoCompleteTextView || v instanceof EditText) {
                View row = v;
                do {
                    row = (View) row.getParent();
                } while (row != null && !(row instanceof TextRow || row instanceof MultiTextRow));
                if (row != null) {
                    tagListener.updateSingleValue(((KeyValueRow) row).getKey(), ((KeyValueRow) row).getValue());
                    if (row.getParent() instanceof EditableLayout) {
                        ((EditableLayout) row.getParent()).putTag(((KeyValueRow) row).getKey(), ((KeyValueRow) row).getValue());
                    }
                }
            }
        }
        return true;
    }

    /**
     * Return the view we have our rows in and work around some android craziness
     * 
     * @return the immutable row layout or null if it couldn't be found
     */
    @Nullable
    private View getImmutableView() {
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
        LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
        if (ll != null) {
            while (ll.getChildAt(0) instanceof EditableLayout) {
                ll.removeViewAt(0);
            }
        } else {
            Log.d(DEBUG_TAG, "update container layout null");
            return;
        }
        final EditableLayout editableView = (EditableLayout) inflater.inflate(R.layout.tag_form_editable, ll, false);
        editableView.setSaveEnabled(false);
        int pos = 0;
        ll.addView(editableView, pos++);

        LinearLayout nonEditableView = (LinearLayout) getImmutableView();
        if (nonEditableView != null && nonEditableView.getChildCount() > 0) {
            nonEditableView.removeAllViews();
        }

        PresetItem mainPreset = tagListener.getBestPreset();
        editableView.setTitle(mainPreset);
        editableView.setListeners(tagListener, this);
        editableView.applyPresetButton.setVisibility(View.GONE);
        editableView.applyPresetWithOptionalButton.setVisibility(View.GONE);

        LinkedHashMap<String, String> allTags = tagListener.getKeyValueMapSingle(true);
        Map<String, String> nonEditable;
        if (mainPreset != null) {
            nonEditable = addTagsToViews(editableView, mainPreset, allTags);
            for (PresetItem preset : tagListener.getSecondaryPresets()) {
                final EditableLayout editableView1 = (EditableLayout) inflater.inflate(R.layout.tag_form_editable, ll, false);
                editableView1.setSaveEnabled(false);
                editableView1.setTitle(preset);
                editableView1.setListeners(tagListener, this);
                ll.addView(editableView1, pos++);
                nonEditable = addTagsToViews(editableView1, preset, (LinkedHashMap<String, String>) nonEditable);
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
        // some final UI stuff
        if (focusOnAddress) {
            focusOnAddress = false; // only do it once
            if (!focusOnTag(Tags.KEY_ADDR_HOUSENUMBER) && !focusOnTag(Tags.KEY_ADDR_STREET)) {
                focusOnEmpty();
            }
        } else if (focusTag != null) {
            if (!focusOnTag(focusTag)) {
                focusOnEmpty();
            }
            focusTag = null;
        } else {
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
        LinkedHashMap<PresetField, String> editable = new LinkedHashMap<>();
        LinkedHashMap<PresetField, String> linkedTags = new LinkedHashMap<>();
        Map<String, PresetItem> keyToLinkedPreset = new HashMap<>();
        Map<String, Map<String, String>> checkGroupKeyValues = new HashMap<>();
        boolean groupingRequired = false;
        LinkedHashMap<String, String> tagList = new LinkedHashMap<>(tags);
        if (preset != null) {
            // iterate over preset entries so that we maintain ordering
            for (Entry<String, PresetField> entry : preset.getFields().entrySet()) {
                PresetField field = entry.getValue();
                String key = field.getKey();
                String value = tagList.get(key);
                Log.e(DEBUG_TAG, "field " + field.getClass().getCanonicalName());
                if (value != null) {
                    if (field instanceof PresetFixedField) { 
                        if (value.equals(((PresetFixedField) field).getValue().getValue())) {
                            tagList.remove(key);
                            editableView.putTag(key, value);
                        } // else leave this fixed key for further processing
                    } else if (field.getKey().equals(key)) {
                        editable.put(field, value);
                        tagList.remove(key);
                        editableView.putTag(key, value);
                    }
                } else if (field instanceof PresetCheckGroupField) {
                    Map<String, String> keyValues = new HashMap<>();
                    for (PresetCheckField check : ((PresetCheckGroupField) field).getCheckFields()) {
                        key = check.getKey();
                        value = tagList.get(key);
                        if (value != null) {
                            keyValues.put(key, value);
                            tagList.remove(key);
                            editableView.putTag(key, value);
                        }
                    }
                    if (!keyValues.isEmpty()) {
                        editable.put(field, "");
                        checkGroupKeyValues.put(field.getKey(), keyValues);
                    }
                }
            }
            // process any remaining tags
            List<PresetItem> linkedPresets = preset.getLinkedPresets(true, App.getCurrentPresets(getContext()));
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
                        PresetField field = l.getField(key);
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
                                i18nFound = true;
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
        if (groupingRequired) {
            List<String> i18nKeys = getI18nKeys(preset);
            preset.groupI18nKeys(i18nKeys);
            de.blau.android.presets.Util.groupI18nKeys(i18nKeys, editable);
            de.blau.android.presets.Util.groupI18nKeys(i18nKeys, linkedTags);
        }
        de.blau.android.presets.Util.groupAddrKeys(linkedTags);
        for (Entry<PresetField, String> entry : editable.entrySet()) {
            PresetField field = entry.getKey();
            if (field instanceof PresetCheckGroupField) {
                CheckGroupDialogRow.getRow(this, inflater, editableView, (PresetCheckGroupField) field, checkGroupKeyValues.get(field.getKey()), preset, tags);
            } else {
                addRow(editableView, field, entry.getValue(), preset, tags);
            }
        }
        for (Entry<PresetField, String> entry : linkedTags.entrySet()) {
            PresetItem linkedItem = keyToLinkedPreset.get(entry.getKey().getKey());
            PresetField field = entry.getKey();
            if (field instanceof PresetCheckGroupField) {
                CheckGroupDialogRow.getRow(this, inflater, editableView, (PresetCheckGroupField) field, checkGroupKeyValues.get(field.getKey()), linkedItem,
                        tags);
            } else {
                addRow(editableView, field, entry.getValue(), linkedItem, tags);
            }
        }

        return tagList;
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
            Set<String> presetI18nKeys = preset.getI18nKeys();
            if (presetI18nKeys != null) {
                i18nKeys.addAll(presetI18nKeys);
            }
            i18nKeys.addAll(Tags.I18N_NAME_KEYS);
        }
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
                    preset.addTag(optional, key, PresetKeyType.TEXT, null);
                    String hint = preset.getHint(tag);
                    if (hint != null) {
                        // FIXME RTL
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
    void addRow(@Nullable final LinearLayout rowLayout, @NonNull final PresetField field, final String value, @Nullable PresetItem preset,
            Map<String, String> allTags) {
        final String key = field.getKey();
        if (rowLayout != null) {
            if (preset != null) {
                if (!(field instanceof PresetFixedField)) {
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
                    //
                    ValueType valueType = field.getValueType();
                    if (field instanceof PresetTextField || key.startsWith(Tags.KEY_ADDR_BASE)
                            || (isComboField && ((PresetComboField) field).isEditable() && ValueType.OPENING_HOURS_MIXED != valueType)
                            || key.endsWith(Tags.KEY_CONDITIONAL_SUFFIX)) {
                        if (key.endsWith(Tags.KEY_CONDITIONAL_SUFFIX) || ValueType.CONDITIONAL == valueType) {
                            rowLayout.addView(getConditionalRestrictionDialogRow(rowLayout, preset, hint, key, value, values, allTags));
                        } else if (isOpeningHours(key, valueType)) {
                            rowLayout.addView(OpeningHoursDialogRow.getRow(this, inflater, rowLayout, preset, hint, key, value, null));
                        } else if (ValueType.PHONE == valueType) {
                            rowLayout.addView(MultiTextRow.getRow(this, inflater, rowLayout, preset, hint, key, values, null, null));
                        } else {
                            rowLayout.addView(TextRow.getRow(this, inflater, rowLayout, preset, field, value, values, allTags));
                        }
                    } else {
                        ArrayAdapter<?> adapter = getValueAutocompleteAdapter(key, values, preset, field, allTags);
                        int count = 0;
                        if (adapter != null) {
                            count = adapter.getCount();
                        } else {
                            Log.d(DEBUG_TAG, "adapter null " + key + " " + value + " " + preset);
                        }
                        if (isComboField || (isCheckField && count > 2)) {
                            if (isOpeningHours(key, valueType)) {
                                rowLayout.addView(OpeningHoursDialogRow.getRow(this, inflater, rowLayout, preset, hint, key, value, adapter));
                            } else if (count <= maxInlineValues) {
                                rowLayout.addView(ComboRow.getRow(this, inflater, rowLayout, preset, hint, key, value, adapter));
                            } else {
                                rowLayout.addView(DialogRow.getComboRow(this, inflater, rowLayout, preset, hint, key, value, adapter));
                            }
                        } else if (isMultiSelectField) {
                            if (((PresetComboField) field).isEditable()) {
                                rowLayout.addView(MultiTextRow.getRow(this, inflater, rowLayout, preset, hint, key, values, null, adapter));
                            } else {
                                if (count <= maxInlineValues) {
                                    rowLayout.addView(MultiselectRow.getRow(this, inflater, rowLayout, preset, hint, key, values, adapter));
                                } else {
                                    rowLayout.addView(MultiselectDialogRow.getRow(this, inflater, rowLayout, preset, hint, key, values, adapter));
                                }
                            }
                        } else if (isCheckField) {
                            if (adapter != null) {
                                final String valueOn = ((PresetCheckField) field).getOnValue().getValue();
                                StringWithDescription tempOff = ((PresetCheckField) field).getOffValue();
                                final String valueOff = tempOff == null ? "" : tempOff.getValue();
                                String description = tempOff == null ? "" : tempOff.getDescription();
                                if (description == null) {
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
                                checkBox.setOnStateChangedListener(new OnStateChangedListener() {
                                    @Override
                                    public void onStateChanged(IndeterminateCheckBox check, Boolean state) {
                                        String checkValue = state != null ? (state ? valueOn : valueOff) : ""; // NOSONAR
                                                                                                               // state
                                                                                                               // can't
                                                                                                               // be
                                                                                                               // null
                                                                                                               // here
                                        tagListener.updateSingleValue(key, checkValue);
                                        if (rowLayout instanceof EditableLayout) {
                                            ((EditableLayout) rowLayout).putTag(key, checkValue);
                                        }
                                    }
                                });
                            } else {
                                Log.e(DEBUG_TAG, "preset element type " + key + " " + value + " " + preset.getName() + " adapter for checkbox is null");
                            }
                        } else {
                            Log.e(DEBUG_TAG, "unknown preset element type " + key + " " + value + " " + preset.getName());
                        }
                    }
                }
            } else { // no preset here so we can only handle hardwired stuff specially
                if (key.endsWith(Tags.KEY_CONDITIONAL_SUFFIX)) {
                    rowLayout.addView(getConditionalRestrictionDialogRow(rowLayout, null, null, key, value, null, allTags));
                } else if (Tags.OPENING_HOURS_SYNTAX.contains(key) && Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                    // FIXME need at least SDK 12 for now
                    rowLayout.addView(OpeningHoursDialogRow.getRow(this, inflater, rowLayout, null, null, key, value, null));
                } else {
                    PresetTextField textField = new PresetTextField(key);
                    rowLayout.addView(TextRow.getRow(this, inflater, rowLayout, null, textField, value, null, allTags));
                }
            }
        } else {
            Log.d(DEBUG_TAG, "addRow rowLayout null");
        }
    }

    /**
     * Check if a key has opening_hours semantics
     * 
     * FIXME need at least SDK 12 for now
     * 
     * @param key the key
     * @param valueType the ValueType
     * @return true if the key has opening_hours semantics
     */
    public boolean isOpeningHours(@NonNull final String key, @NonNull ValueType valueType) {
        return Tags.OPENING_HOURS_SYNTAX.contains(key) || ValueType.OPENING_HOURS == valueType
                || ValueType.OPENING_HOURS_MIXED == valueType && Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB;
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
     * @return a TagFormDialogRow instance
     */
    private DialogRow getConditionalRestrictionDialogRow(LinearLayout rowLayout, PresetItem preset, final String hint, final String key, final String value,
            @Nullable final List<String> values, Map<String, String> allTags) {
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
                    if (v == null || "".equals(v)) {
                        continue;
                    }
                    Log.d(DEBUG_TAG, "adding " + v + " to templates");
                    templates.add(v);
                }
            }
        }
        if (value != null && !"".equals(value)) {
            ConditionalRestrictionParser parser = new ConditionalRestrictionParser(new ByteArrayInputStream(value.getBytes()));
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
        row.setOnClickListener(new OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                FragmentManager fm = getChildFragmentManager();
                de.blau.android.propertyeditor.Util.removeChildFragment(fm, FRAGMENT_CONDITIONAL_RESTRICTION_TAG);
                ConditionalRestrictionFragment conditionalRestrictionDialog = ConditionalRestrictionFragment.newInstance(key, value, templates, ohTemplates);
                conditionalRestrictionDialog.show(fm, FRAGMENT_CONDITIONAL_RESTRICTION_TAG);
            }
        });
        return row;
    }

    /**
     * Focus on the value field of a tag with key "key"
     * 
     * @param key the key value we want to focus on
     * @return true if the key was found
     */
    private boolean focusOnTag(String key) {
        boolean found = false;
        View sv = getView();
        LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
        if (ll != null) {
            int pos = 0;
            while (ll.getChildAt(pos) instanceof EditableLayout && pos < ll.getChildCount() && !found) {
                EditableLayout ll2 = (EditableLayout) ll.getChildAt(pos);
                Log.d(DEBUG_TAG, "focusOnTag key " + key);
                for (int i = ll2.getChildCount() - 1; i >= 0; --i) {
                    View v = ll2.getChildAt(i);
                    if (v instanceof TextRow && ((TextRow) v).getKey().equals(key)) {
                        ((TextRow) v).getValueView().requestFocus();
                        Util.scrollToRow(sv, v, true, true);
                        found = true;
                        break;
                    } else if (v instanceof DialogRow && ((DialogRow) v).getKey().equals(key)) {
                        Util.scrollToRow(sv, v, true, true);
                        ((DialogRow) v).click();
                        found = true;
                    }
                }
                pos++;
            }
        } else {
            Log.d(DEBUG_TAG, "focusOnTag container layout null");
            return false;
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
        View sv = getView();
        LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
        if (ll != null) {
            int pos = 0;
            while (ll.getChildAt(pos) instanceof EditableLayout && pos < ll.getChildCount() && !found) {
                EditableLayout ll2 = (EditableLayout) ll.getChildAt(pos);
                for (int i = 0; i < ll2.getChildCount(); i++) {
                    View v = ll2.getChildAt(i);
                    if (v instanceof TextRow && "".equals(((TextRow) v).getValue())) {
                        ((TextRow) v).getValueView().requestFocus();
                        found = true;
                        break;
                    }
                }
                pos++;
            }
        } else {
            Log.d(DEBUG_TAG, "update container layout null");
            return false;
        }
        return found;
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
        private LinearLayout                  rowLayout;
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
        public EditableLayout(Context context) {
            super(context);
        }

        /**
         * Construct a Layout representing the tags that match a specific PresetItem
         * 
         * @param context Android Context
         * @param attrs an AttributeSet
         */
        public EditableLayout(Context context, AttributeSet attrs) {
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
            rowLayout = (LinearLayout) findViewById(R.id.form_editable_row_layout);
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
        public void setListeners(final EditorUpdate editorListener, final FormUpdate formListener) {
            Log.d(DEBUG_TAG, "setting listeners");
            applyPresetButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    editorListener.applyPreset(preset, false);
                    formListener.tagsUpdated();
                }
            });
            applyPresetWithOptionalButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    editorListener.applyPreset(preset, true);
                    formListener.tagsUpdated();
                }
            });
            copyButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    formListener.updateEditorFromText();
                    App.getTagClipboard(getContext()).copy(tags);
                    Snack.toastTopInfo(getContext(), R.string.toast_tags_copied);
                }
            });
            cutButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    formListener.updateEditorFromText();
                    App.getTagClipboard(getContext()).cut(tags);
                    for (String key : tags.keySet()) {
                        editorListener.deleteTag(key);
                    }
                    editorListener.updatePresets();
                    formListener.tagsUpdated();
                    Snack.toastTopInfo(getContext(), R.string.toast_tags_cut);
                }
            });
            deleteButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setMessage(v.getContext().getString(R.string.delete_tags, headerTitleView.getText()));
                    builder.setNegativeButton(R.string.cancel, null);
                    builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (String key : tags.keySet()) {
                                editorListener.deleteTag(key);
                            }
                            editorListener.updatePresets();
                            formListener.tagsUpdated();
                        }
                    });
                    builder.show();
                }
            });
        }

        /**
         * Set the title and PresetItem
         * 
         * @param preset the PresetItem
         */
        public void setTitle(@Nullable PresetItem preset) {
            if (preset != null) {
                Drawable icon = preset.getIconIfExists(preset.getIconpath());
                this.preset = preset;
                if (icon != null) {
                    headerIconView.setVisibility(View.VISIBLE);
                    // NOTE directly using the icon seems to trash it, so make a copy
                    headerIconView.setImageDrawable(icon.getConstantState().newDrawable());
                } else {
                    headerIconView.setVisibility(View.GONE);
                }
                headerTitleView.setText(preset.getTranslatedName());
                applyPresetButton.setVisibility(View.VISIBLE);
                applyPresetWithOptionalButton.setVisibility(View.VISIBLE);
                copyButton.setVisibility(View.VISIBLE);
                cutButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);
            } else {
                headerTitleView.setText(R.string.tag_form_unknown_element);
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
}
