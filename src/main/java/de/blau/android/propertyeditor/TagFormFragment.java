package de.blau.android.propertyeditor;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.buildware.widget.indeterm.IndeterminateCheckBox;
import com.buildware.widget.indeterm.IndeterminateCheckBox.OnStateChangedListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils.TruncateAt;
import android.text.style.StrikethroughSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import ch.poole.conditionalrestrictionparser.ConditionalRestrictionParser;
import ch.poole.openinghoursfragment.OpeningHoursFragment;
import ch.poole.openinghoursfragment.ValueWithDescription;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.names.Names.TagMap;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Server;
import de.blau.android.osm.Tags;
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
import de.blau.android.presets.ValueWithCount;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.GeoContext.Properties;
import de.blau.android.util.Snack;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.StringWithDescriptionAndIcon;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.views.CustomAutoCompleteTextView;
import de.blau.android.views.TriStateCheckBox;

public class TagFormFragment extends BaseFragment implements FormUpdate {

    private static final String FOCUS_TAG = "focusTag";

    private static final String FOCUS_ON_ADDRESS = "focusOnAddress";

    private static final String DISPLAY_MRU_PRESETS = "displayMRUpresets";

    private static final String ASK_FOR_NAME = "askForName";

    private static final String DEBUG_TAG = TagFormFragment.class.getSimpleName();

    private LayoutInflater inflater = null;

    private Names names = null;

    private Preferences prefs = null;

    private PropertyEditorListener propertyEditorListener;

    private EditorUpdate tagListener = null;

    private NameAdapters nameAdapters = null;

    private boolean focusOnAddress = false;

    private String focusTag = null;

    private boolean askForName = false;

    private int maxInlineValues = 3;

    private int maxStringLength; // maximum key, value and role length

    private StringWithDescription.LocaleComparator comparator;

    /**
     * Create a new instance of the fragment
     * 
     * @param displayMRUpresets display the MRU list of Presets
     * @param focusOnAddress focus on any address keys
     * @param focusTag focus on this tag
     * @param askForName ask for a name value first
     * @return a TagFormFragment instance
     */
    public static TagFormFragment newInstance(boolean displayMRUpresets, boolean focusOnAddress, String focusTag, boolean askForName) {
        TagFormFragment f = new TagFormFragment();

        Bundle args = new Bundle();

        args.putSerializable(DISPLAY_MRU_PRESETS, displayMRUpresets);
        args.putSerializable(FOCUS_ON_ADDRESS, focusOnAddress);
        args.putSerializable(FOCUS_TAG, focusTag);
        args.putSerializable(ASK_FOR_NAME, askForName);

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
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPresetSelectedListener, NameAdapters, PropertyEditorListener");
        }
        setHasOptionsMenu(true);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        askForName = (Boolean) getArguments().getSerializable(ASK_FOR_NAME);
        // Log.d(DEBUG_TAG,"element " + element + " tags " + tags);

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
            recentPresetsFragment = RecentPresetsFragment.newInstance(propertyEditorListener.getElement().getOsmId(), propertyEditorListener.getElement().getName());
            ft.add(R.id.form_mru_layout, recentPresetsFragment, "recentpresets_fragment");
            ft.commit();
        }

        Log.d(DEBUG_TAG, "onCreateView returning");
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
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
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
    private ArrayAdapter<?> getValueAutocompleteAdapter(@Nullable String key, @Nullable ArrayList<String> values, @Nullable PresetItem preset,
            @Nullable PresetField field, @NonNull Map<String, String> allTags) {
        ArrayAdapter<?> adapter = null;

        if (key != null && key.length() > 0) {
            Set<String> usedKeys = allTags.keySet();
            if (TagEditorFragment.isStreetName(key, usedKeys)) {
                adapter = nameAdapters.getStreetNameAdapter(values);
            } else if (TagEditorFragment.isPlaceName(key, usedKeys)) {
                adapter = nameAdapters.getPlaceNameAdapter(values);
            } else if (key.equals(Tags.KEY_NAME) && (names != null) && TagEditorFragment.useNameSuggestions(usedKeys)) {
                Log.d(DEBUG_TAG, "generate suggestions for name from name suggestion index");
                List<NameAndTags> suggestions = (ArrayList<NameAndTags>) names.getNames(new TreeMap<>(allTags));
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
                        adapter = new ArrayAdapter<String>(getActivity(), R.layout.autocomplete_row, speedLimits);
                    }
                }
            } else {
                HashMap<String, Integer> counter = new HashMap<>();
                ArrayAdapter<StringWithDescription> adapter2 = new ArrayAdapter<>(getActivity(), R.layout.autocomplete_row);

                if (preset != null) {
                    Collection<StringWithDescription> presetValues;
                    if (field != null) {
                        presetValues = preset.getAutocompleteValues(field);
                    } else {
                        presetValues = preset.getAutocompleteValues(key);
                    }
                    Log.d(DEBUG_TAG, "setting autocomplete adapter for values " + presetValues);
                    if (!presetValues.isEmpty()) {
                        List<StringWithDescription> result = new ArrayList<>(presetValues);
                        if (preset.sortValues(key)) {
                            Collections.sort(result, comparator);
                        }
                        for (StringWithDescription s : result) {
                            if (counter != null && counter.containsKey(s.getValue())) {
                                continue; // skip stuff that is already listed
                            }
                            adapter2.add(s);
                            counter.put(s.getValue(), 1);
                        }
                        Log.d(DEBUG_TAG, "key " + key + " type " + preset.getKeyType(key));
                    }
                } else {
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
        FragmentActivity activity = getActivity();
        menu.findItem(R.id.tag_menu_mapfeatures).setEnabled(propertyEditorListener.isConnectedOrConnecting());
        menu.findItem(R.id.tag_menu_paste).setVisible(!App.getTagClipboard(getContext()).isEmpty());
        menu.findItem(R.id.tag_menu_paste_from_clipboard).setVisible(tagListener.pasteFromClipboardIsPossible());
        Locale locale = Locale.getDefault();
        if (activity != null && !(locale.equals(Locale.US) || locale.equals(Locale.UK))) {
            menu.findItem(R.id.tag_menu_locale).setVisible(true).setTitle(activity.getString(R.string.tag_menu_i8n, locale.toString()));
        } else {
            menu.findItem(R.id.tag_menu_locale).setVisible(false);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // disable address tagging for stuff that won't have an address
        // menu.findItem(R.id.tag_menu_address).setVisible(!type.equals(Way.NAME) ||
        // element.hasTagKey(Tags.KEY_BUILDING));
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
            PresetItem pi = tagListener.getBestPreset();
            if (pi != null) {
                tagListener.applyPreset(pi, true);
                tagsUpdated();
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
            startActivity(Preset.getMapFeaturesIntent(getActivity(), tagListener.getBestPreset()));
            return true;
        case R.id.tag_menu_resetMRU:
            for (Preset p : propertyEditorListener.getPresets()) {
                p.resetRecentlyUsed();
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
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            Locale locale = Locale.getDefault();
            List<String> i18nKeys = getI18nKeys(tagListener.getBestPreset());
            for (Entry<String, String> e : allTags.entrySet()) {
                String key = e.getKey();
                result.put(key, e.getValue());
                if (i18nKeys.contains(key)) {
                    String languageKey = key + ":" + locale.getLanguage();
                    String variantKey = key + ":" + locale.toString();
                    if (!allTags.containsKey(languageKey)) {
                        result.put(languageKey, "");
                    }
                    if (!allTags.containsKey(variantKey)) {
                        result.put(variantKey, "");
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
            if (v instanceof CustomAutoCompleteTextView) {
                View row = v;
                do {
                    row = (View) row.getParent();
                } while (row != null && !(row instanceof TagTextRow));
                if (row != null) {
                    tagListener.updateSingleValue(((TagTextRow) row).getKey(), ((TagTextRow) row).getValue());
                    if (row.getParent() instanceof EditableLayout) {
                        ((EditableLayout) row.getParent()).putTag(((TagTextRow) row).getKey(), ((TagTextRow) row).getValue());
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
     * Recreate the MRU list of Presets
     */
    void recreateRecentPresetView() {
        Log.d(DEBUG_TAG, "Updating MRU prests");
        FragmentManager fm = getChildFragmentManager();
        Fragment recentPresetsFragment = fm.findFragmentByTag(PropertyEditor.RECENTPRESETS_FRAGMENT);
        if (recentPresetsFragment != null) {
            ((RecentPresetsFragment) recentPresetsFragment).recreateRecentPresetView();
        }
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
        // display dialog for name selection for store/other chains
        if (askForName) {
            askForName = false; // only do this once
            AlertDialog d = buildNameDialog(getActivity());
            d.show();
            // force dropdown and keyboard to appear
            final View v = d.findViewById(R.id.textValue);
            if (v instanceof AutoCompleteTextView) {
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        ((AutoCompleteTextView) v).showDropDown();
                        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        mgr.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        }
    }

    /**
     * Given a map of tags and the best matching PresetItem, loop over the fields in the PresetItem creating rows for
     * the tags that have matching keys
     * 
     * @param editableView the Layout foldings rows for tags that we were able to identify
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
                    if (field instanceof PresetFixedField && value.equals(((PresetFixedField) field).getValue().getValue())) {
                        tagList.remove(key);
                        editableView.putTag(key, value);
                    } else if (Preset.hasKeyValue(field, key, value)) {
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
            List<PresetItem> linkedPresets = preset.getLinkedPresets(true);
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
                            } else if (Preset.hasKeyValue(field, key, value)) {
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
                addCheckGroupRow(editableView, (PresetCheckGroupField) field, checkGroupKeyValues.get(field.getKey()), preset, tags);
            } else {
                addRow(editableView, field, entry.getValue(), preset, tags);
            }
        }
        for (Entry<PresetField, String> entry : linkedTags.entrySet()) {
            PresetItem linkedItem = keyToLinkedPreset.get(entry.getKey().getKey());
            PresetField field = entry.getKey();
            if (field instanceof PresetCheckGroupField) {
                addCheckGroupRow(editableView, (PresetCheckGroupField) field, checkGroupKeyValues.get(field.getKey()), linkedItem, tags);
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
    private void addRow(@Nullable final LinearLayout rowLayout, @NonNull final PresetField field, final String value, @Nullable PresetItem preset,
            Map<String, String> allTags) {
        final String key = field.getKey();
        if (rowLayout != null) {
            if (preset != null) {
                if (!(field instanceof PresetFixedField)) {
                    ArrayList<String> values = null;
                    boolean isCheckField = field instanceof PresetCheckField;
                    boolean isComboField = field instanceof PresetComboField && !((PresetComboField) field).isMultiSelect();
                    boolean isMultiSelectField = field instanceof PresetComboField && ((PresetComboField) field).isMultiSelect();
                    if (isMultiSelectField) {
                        values = Preset.splitValues(Util.getArrayList(value), preset, key);
                    } else {
                        values = Util.getArrayList(value);
                    }
                    String hint = field.getHint();
                    //
                    ValueType valueType = preset.getValueType(key);
                    if (field instanceof PresetTextField || key.startsWith(Tags.KEY_ADDR_BASE)
                            || (preset.isEditable(key) && ValueType.OPENING_HOURS_MIXED != valueType) || key.endsWith(Tags.KEY_CONDITIONAL_SUFFIX)) {
                        if (key.endsWith(Tags.KEY_CONDITIONAL_SUFFIX) || ValueType.CONDITIONAL == valueType) {
                            rowLayout.addView(getConditionalRestrictionDialogRow(rowLayout, preset, hint, key, value, values, allTags));
                        } else if ((Tags.OPENING_HOURS_SYNTAX.contains(key) || ValueType.OPENING_HOURS == valueType)
                                && Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                            // FIXME need at least SDK 12 for now
                            rowLayout.addView(getOpeningHoursDialogRow(rowLayout, preset, hint, key, value, null));
                        } else {
                            rowLayout.addView(getTextRow(rowLayout, preset, field, value, values, allTags));
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
                            if (ValueType.OPENING_HOURS_MIXED == valueType && Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                                // FIXME need at least SDK 12 for now
                                rowLayout.addView(getOpeningHoursDialogRow(rowLayout, preset, hint, key, value, adapter));
                            } else if (count <= maxInlineValues) {
                                rowLayout.addView(getComboRow(rowLayout, preset, hint, key, value, adapter));
                            } else {
                                rowLayout.addView(getComboDialogRow(rowLayout, preset, hint, key, value, adapter));
                            }
                        } else if (isMultiSelectField) {
                            if (count <= maxInlineValues) {
                                rowLayout.addView(getMultiselectRow(rowLayout, preset, hint, key, values, adapter));
                            } else {
                                rowLayout.addView(getMultiselectDialogRow(rowLayout, preset, hint, key, value, adapter));
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
                                final TagCheckRow row = (TagCheckRow) inflater.inflate(R.layout.tag_form_check_row, rowLayout, false);
                                row.keyView.setText(hint != null ? hint : key);
                                row.keyView.setTag(key);
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
                                        String checkValue = state != null ? (state ? valueOn : valueOff) : "";
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
                    rowLayout.addView(getOpeningHoursDialogRow(rowLayout, null, null, key, value, null));
                } else {
                    PresetTextField textField = new PresetTextField(key);
                    rowLayout.addView(getTextRow(rowLayout, null, textField, value, null, allTags));
                }
            }
        } else {
            Log.d(DEBUG_TAG, "addRow rowLayout null");
        }
    }

    /**
     * Add a row for a PresetCheckGroupField
     * 
     * Depending on the number of etries and if a hint is set or not, different layout variants will be used
     * 
     * @param rowLayout the Layout holding the rows
     * @param field the PresetField for this row
     * @param keyValues a Map containing existing keys and corresponding values
     * @param preset the Preset we believe the key belongs to
     * @param allTags the other tags for this object
     */
    private void addCheckGroupRow(@Nullable final LinearLayout rowLayout, @NonNull final PresetCheckGroupField field,
            @NonNull final Map<String, String> keyValues, @NonNull PresetItem preset, Map<String, String> allTags) {
        final String key = field.getKey();
        if (rowLayout != null) {
            if (field.size() <= maxInlineValues) {
                String hint = preset.getHint(key);
                if (hint == null) { // simply display as individual checks
                    for (PresetCheckField check : field.getCheckFields()) {
                        addRow(rowLayout, check, keyValues.get(check.getKey()), preset, allTags);
                    }
                } else {
                    final TagCheckGroupRow row = (TagCheckGroupRow) inflater.inflate(R.layout.tag_form_checkgroup_row, rowLayout, false);
                    row.keyView.setText(hint);
                    row.keyView.setTag(key);
                    OnStateChangedListener onStateChangeListener = new OnStateChangedListener() {
                        @Override
                        public void onStateChanged(IndeterminateCheckBox checkBox, Boolean state) {
                            PresetCheckField check = (PresetCheckField) checkBox.getTag();
                            String checkKey = check.getKey();
                            if (state == null) {
                                tagListener.updateSingleValue(checkKey, "");
                                keyValues.put(checkKey, "");
                            } else if (!checkBox.isEnabled()) {
                                // unknown stuff
                                keyValues.put(checkKey, keyValues.get(checkKey));
                            } else if (state) {
                                tagListener.updateSingleValue(checkKey, check.getOnValue().getValue());
                                keyValues.put(checkKey, check.getOnValue().getValue());
                            } else {
                                StringWithDescription offValue = check.getOffValue();
                                tagListener.updateSingleValue(checkKey, offValue == null ? "" : offValue.getValue());
                                keyValues.put(checkKey, offValue == null ? "" : offValue.getValue());
                            }
                            if (rowLayout instanceof EditableLayout) {
                                ((EditableLayout) rowLayout).putTag(checkKey, keyValues.get(checkKey));
                            }
                        }
                    };

                    for (PresetCheckField check : field.getCheckFields()) {
                        String checkKey = check.getKey();
                        String checkValue = keyValues.get(checkKey);
                        Boolean state = null;

                        boolean selected = checkValue != null && checkValue.equals(check.getOnValue().getValue());
                        boolean off = check.isOffValue(checkValue);

                        String d = check.getHint();
                        if (checkValue == null || "".equals(checkValue) || selected || off) {
                            if (selected || off || check.getOffValue() == null) {
                                state = selected;
                            }
                            TriStateCheckBox checkBox = row.addCheck(d == null ? checkKey : d, state, onStateChangeListener);
                            checkBox.setTag(check);
                        } else {
                            // unknown value: add non-editable checkbox
                            TriStateCheckBox checkBox = row.addCheck(checkKey + "=" + checkValue, state, onStateChangeListener);
                            checkBox.setTag(check);
                            checkBox.setEnabled(false);
                        }
                    }
                    rowLayout.addView(row);
                }
            } else {
                final TagFormCheckGroupDialogRow row = (TagFormCheckGroupDialogRow) inflater.inflate(R.layout.tag_form_checkgroup_dialog_row, rowLayout, false);

                String tempHint = preset.getHint(key);
                if (tempHint == null) {
                    // fudge something
                    tempHint = getString(R.string.ugly_checkgroup_hint, field.getCheckFields().get(0).getHint());
                }
                final String hint = tempHint;
                row.keyView.setText(hint);
                row.keyView.setTag(key);
                row.setPreset(preset);
                row.setSelectedValues(keyValues);
                row.valueView.setHint(R.string.tag_dialog_value_hint);

                row.setOnClickListener(new OnClickListener() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View v) {
                        final View finalView = v;
                        finalView.setEnabled(false); // debounce
                        final AlertDialog dialog = buildCheckGroupDialog(hint, key, row, preset);
                        final Object tag = finalView.getTag();
                        dialog.setOnShowListener(new OnShowListener() {
                            @Override
                            public void onShow(DialogInterface d) {
                                if (tag instanceof String) {
                                    scrollDialogToValue((String) tag, dialog, R.id.valueGroup);
                                }
                            }
                        });
                        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                finalView.setEnabled(true);
                            }
                        });
                        dialog.show();
                        // the following is one big awful hack to stop the button dismissing the dialog
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                LinearLayout valueGroup = (LinearLayout) dialog.findViewById(R.id.valueGroup);
                                for (int pos = 0; pos < valueGroup.getChildCount(); pos++) {
                                    View c = valueGroup.getChildAt(pos);
                                    if (c instanceof AppCompatCheckBox) {
                                        ((AppCompatCheckBox) c).setChecked(false);
                                    }
                                }
                            }
                        });
                    }
                });
                rowLayout.addView(row);
            }
        } else {
            Log.e(DEBUG_TAG, "addRow rowLayout null");
        }
    }

    /**
     * Add a row for an unstructured text value
     * 
     * @param rowLayout the Layout holding the row
     * @param preset the best matched PresetITem for the key
     * @param field the PresetField
     * @param value any existing value for the tag
     * @param values a list of all the predefined values in the PresetItem for the key
     * @param allTags a Map of the tags currently being edited
     * @return a TagTextRow instance
     */
    private TagTextRow getTextRow(@NonNull final LinearLayout rowLayout, @Nullable final PresetItem preset, @NonNull final PresetField field,
            @Nullable final String value, @Nullable final ArrayList<String> values, final Map<String, String> allTags) {
        final TagTextRow row = (TagTextRow) inflater.inflate(R.layout.tag_form_text_row, rowLayout, false);
        final String key = field.getKey();
        final String hint = preset != null ? field.getHint() : null;
        final String defaultValue = field.getDefaultValue();
        final ValueType valueType = preset != null ? preset.getValueType(key) : null;
        final boolean isWebsite = Tags.isWebsiteKey(key) || ValueType.WEBSITE == valueType;
        final boolean isMPHSpeed = Tags.isSpeedKey(key) && App.getGeoContext(getActivity()).imperial(propertyEditorListener.getElement());
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) { // stop Hint from wrapping
            row.valueView.setEllipsize(TruncateAt.END);
        }
        row.valueView.setText(value);

        // set empty value to be on the safe side
        row.valueView.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.autocomplete_row, new String[0]));

        if (field instanceof PresetComboField && ((PresetComboField) field).isMultiSelect() && preset != null) {
            // FIXME this should be somewhere better since it creates a non obvious side effect
            row.valueView.setTokenizer(new CustomAutoCompleteTextView.SingleCharTokenizer(preset.getDelimiter(key)));
        }
        if (field instanceof PresetTextField) {
            row.valueView.setHint(R.string.tag_value_hint);
        } else {
            row.valueView.setHint(R.string.tag_autocomplete_value_hint);
        }
        row.valueView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                String rowValue = row.getValue();
                if (!hasFocus && !rowValue.equals(value)) {
                    Log.d(DEBUG_TAG, "onFocusChange");
                    tagListener.updateSingleValue(key, rowValue);
                    if (rowLayout instanceof EditableLayout) {
                        ((EditableLayout) rowLayout).putTag(key, rowValue);
                    }
                } else if (hasFocus) {
                    boolean hasValues = values != null && !values.isEmpty();
                    if (hasValues) {
                        ArrayAdapter<?> adapter = getValueAutocompleteAdapter(key, values, preset, null, allTags);
                        if (adapter != null) {
                            row.valueView.setAdapter(adapter);
                        }
                    }
                    if (isWebsite) {
                        TagEditorFragment.initWebsite(row.valueView);
                    } else if (isMPHSpeed) {
                        TagEditorFragment.initMPHSpeed(getActivity(), row.valueView, propertyEditorListener);
                    } else if (valueType == null) {
                        InputTypeUtil.enableTextSuggestions(row.valueView);
                    }
                    InputTypeUtil.setInputTypeFromValueType(row.valueView, valueType);
                }
            }
        });
        row.valueView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(DEBUG_TAG, "onItemClicked value");
                Object o = parent.getItemAtPosition(position);
                if (o instanceof Names.NameAndTags) {
                    row.valueView.setOrReplaceText(((NameAndTags) o).getName());
                    tagListener.applyTagSuggestions(((NameAndTags) o).getTags());
                    update();
                    return;
                } else if (o instanceof ValueWithCount) {
                    row.valueView.setOrReplaceText(((ValueWithCount) o).getValue());
                } else if (o instanceof StringWithDescription) {
                    row.valueView.setOrReplaceText(((StringWithDescription) o).getValue());
                } else if (o instanceof String) {
                    row.valueView.setOrReplaceText((String) o);
                }
                tagListener.updateSingleValue(key, row.getValue());
                if (rowLayout instanceof EditableLayout) {
                    ((EditableLayout) rowLayout).putTag(key, row.getValue());
                }
            }
        });
        row.valueView.addTextChangedListener(new SanitizeTextWatcher(getActivity(), maxStringLength));

        return row;
    }

    /**
     * Add a row for a combo with inline RadioButtons
     * 
     * @param rowLayout the Layout holding the row
     * @param preset the best matched PresetITem for the key
     * @param hint a textual description of what the key is
     * @param key the key
     * @param value any existing value for the tag
     * @param adapter an ArrayAdapter containing all the predefined values in the PresetItem for the key
     * @return a TagComboRow instance
     */
    private TagComboRow getComboRow(@NonNull final LinearLayout rowLayout, @NonNull final PresetItem preset, @Nullable final String hint,
            @NonNull final String key, @Nullable final String value, @Nullable final ArrayAdapter<?> adapter) {
        final TagComboRow row = (TagComboRow) inflater.inflate(R.layout.tag_form_combo_row, rowLayout, false);
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                Object o = adapter.getItem(i);
                StringWithDescription swd = new StringWithDescription(o);
                String v = swd.getValue();
                String description = swd.getDescription();
                if (v == null || "".equals(v)) {
                    continue;
                }
                if (description == null) {
                    description = v;
                }
                Drawable icon = null;
                if (o instanceof StringWithDescriptionAndIcon && ((StringWithDescriptionAndIcon) o).getIcon(preset) != null) {
                    icon = ((StringWithDescriptionAndIcon) o).getIcon(preset);
                }
                row.addButton(description, v, v.equals(value), icon);
            }

            row.getRadioGroup().setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    Log.d(DEBUG_TAG, "radio group onCheckedChanged");
                    String value = "";
                    if (checkedId != -1) {
                        RadioButton button = (RadioButton) group.findViewById(checkedId);
                        value = (String) button.getTag();
                    }
                    tagListener.updateSingleValue(key, value);
                    if (rowLayout instanceof EditableLayout) {
                        ((EditableLayout) rowLayout).putTag(key, value);
                    }
                    row.setValue(value);
                    row.setChanged(true);
                }
            });
        }
        return row;
    }

    /**
     * Add a row for a multi-select with inline CheckBoxes
     * 
     * @param rowLayout the Layout holding the row
     * @param preset the best matched PresetITem for the key
     * @param hint a textual description of what the key is
     * @param key the key
     * @param values existing values for the tag
     * @param adapter an ArrayAdapter containing all the predefined values in the PresetItem for the key
     * @return a TagMultiselectRow instance
     */
    private TagMultiselectRow getMultiselectRow(@NonNull final LinearLayout rowLayout, @NonNull final PresetItem preset, @Nullable final String hint,
            final String key, @Nullable final ArrayList<String> values, @Nullable ArrayAdapter<?> adapter) {
        final TagMultiselectRow row = (TagMultiselectRow) inflater.inflate(R.layout.tag_form_multiselect_row, rowLayout, false);
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        if (adapter != null) {
            row.setDelimiter(preset.getDelimiter(key));
            CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    tagListener.updateSingleValue(key, row.getValue());
                    if (rowLayout instanceof EditableLayout) {
                        ((EditableLayout) rowLayout).putTag(key, row.getValue());
                    }
                }
            };
            int count = adapter.getCount();
            for (int i = 0; i < count; i++) {
                Object o = adapter.getItem(i);
                StringWithDescription swd = new StringWithDescription(o);
                String v = swd.getValue();
                String description = swd.getDescription();
                if (v == null || "".equals(v)) {
                    continue;
                }
                if (description == null) {
                    description = v;
                }
                Drawable icon = null;
                if (o instanceof StringWithDescriptionAndIcon && ((StringWithDescriptionAndIcon) o).getIcon(preset) != null) {
                    icon = ((StringWithDescriptionAndIcon) o).getIcon(preset);
                }
                row.addCheck(description, v, values != null && values.contains(v), icon, onCheckedChangeListener);
            }
        }
        return row;
    }

    /**
     * Add a row for a conditional restriction that will start the correspondign editor if clicked
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
    private TagFormDialogRow getConditionalRestrictionDialogRow(LinearLayout rowLayout, PresetItem preset, final String hint, final String key,
            final String value, @Nullable final ArrayList<String> values, Map<String, String> allTags) {
        final TagFormDialogRow row = (TagFormDialogRow) inflater.inflate(R.layout.tag_form_combo_dialog_row, rowLayout, false);
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
                FragmentTransaction ft = fm.beginTransaction();
                Fragment prev = fm.findFragmentByTag("fragment_conditional_restriction");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.commit();
                ConditionalRestrictionFragment conditionalRestrictionDialog = ConditionalRestrictionFragment.newInstance(key, value, templates, ohTemplates);
                conditionalRestrictionDialog.show(fm, "fragment_conditional_restriction");
            }
        });
        return row;
    }

    /**
     * Add a row that display an opening hours value and starts the OH editor when clicked
     * 
     * @param rowLayout the Layout holding the rows
     * @param preset the current preset
     * @param hint a hint for the value
     * @param key the key
     * @param value the current value if any
     * @param adapter an Adapter holding any suggested values
     * @return a TagFormDialogRow
     */
    private TagFormDialogRow getOpeningHoursDialogRow(@NonNull final LinearLayout rowLayout, @Nullable PresetItem preset, @Nullable final String hint,
            @NonNull final String key, @Nullable String value, @Nullable final ArrayAdapter<?> adapter) {
        final TagFormOpeningHoursDialogRow row = (TagFormOpeningHoursDialogRow) inflater.inflate(R.layout.tag_form_openinghours_dialog_row, rowLayout, false);
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        row.setPreset(preset);

        boolean strictSucceeded = false;
        boolean lenientSucceeded = false;
        ArrayList<Rule> rules = null;

        OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(value.getBytes()));

        try {
            rules = parser.rules(true);
            strictSucceeded = true;
        } catch (Exception e) {
            parser = new OpeningHoursParser(new ByteArrayInputStream(value.getBytes()));
            try {
                rules = parser.rules(false);
                value = ch.poole.openinghoursparser.Util.rulesToOpeningHoursString(rules);
                tagListener.updateSingleValue(key, value);
                lenientSucceeded = true;
            } catch (Exception e1) {
                // failed
                rules = null;
            }
        }
        row.setValue(value, rules);

        if (value != null && !"".equals(value)) {
            if (!strictSucceeded && lenientSucceeded) {
                rowLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        Snack.barWarning(rowLayout, getString(R.string.toast_openinghours_autocorrected, row.keyView.getText().toString()),
                                Snackbar.LENGTH_LONG);
                    }
                });
            } else if (!strictSucceeded && adapter == null) {
                // only warn if the value should be an OH string
                rowLayout.post(new Runnable() {

                    @Override
                    public void run() {
                        Snack.barWarning(rowLayout, getString(R.string.toast_openinghours_invalid, row.keyView.getText().toString()), Snackbar.LENGTH_LONG);
                    }
                });
            }
        }

        row.valueView.setHint(R.string.tag_tap_to_edit_hint);
        final String finalValue = value;
        row.setOnClickListener(new OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                FragmentManager fm = getChildFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                Fragment prev = fm.findFragmentByTag("fragment_opening_hours");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.commit();
                ValueWithDescription keyWithDescription = new ValueWithDescription(key, hint);
                ArrayList<ValueWithDescription> textValues = null;
                if (adapter != null) {
                    textValues = new ArrayList<>();
                    int count = adapter.getCount();
                    for (int i = 0; i < count; i++) {
                        Object o = adapter.getItem(i);
                        String val = null;
                        String des = null;

                        if (o instanceof String) {
                            val = (String) o;
                        } else if (o instanceof ValueWithCount) {
                            val = ((ValueWithCount) o).getValue();
                            des = ((ValueWithCount) o).getDescription();
                        } else if (o instanceof StringWithDescription) {
                            val = ((StringWithDescription) o).getValue();
                            des = ((StringWithDescription) o).getDescription();
                        }
                        // this is expensive, but we need a way so stripping out OH values
                        if (val != null) {
                            OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(val.getBytes()));
                            try {
                                @SuppressWarnings("unused")
                                List<Rule> rules = parser.rules(false);
                                continue;
                            } catch (Exception e1) {
                                // not an OH value ... add
                                textValues.add(new ValueWithDescription(val, des));
                            }
                        }
                    }
                }
                OpeningHoursFragment openingHoursDialog = OpeningHoursFragment.newInstance(keyWithDescription, finalValue,
                        prefs.lightThemeEnabled() ? R.style.Theme_AppCompat_Light_Dialog_Alert : R.style.Theme_AppCompat_Dialog_Alert, -1, true, textValues);
                openingHoursDialog.show(fm, "fragment_opening_hours");
            }
        });
        return row;
    }

    /**
     * Add a row that displays a dialog for selecting a single when clicked
     * 
     * @param rowLayout the Layout holding the roes
     * @param preset the relevant PresetItem
     * @param hint a description of the value to display
     * @param key the key
     * @param value the current value
     * @param adapter an ArrayAdapter with the selectable values
     * @return an instance of TagFormDialogRow
     */
    private TagFormDialogRow getComboDialogRow(@NonNull final LinearLayout rowLayout, @NonNull final PresetItem preset, @Nullable final String hint,
            @NonNull final String key, @NonNull final String value, @Nullable final ArrayAdapter<?> adapter) {
        final TagFormDialogRow row = (TagFormDialogRow) inflater.inflate(R.layout.tag_form_combo_dialog_row, rowLayout, false);
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        row.setPreset(preset);
        if (adapter != null) {
            String selectedValue = null;
            for (int i = 0; i < adapter.getCount(); i++) {
                Object o = adapter.getItem(i);

                StringWithDescription swd;
                if (o instanceof StringWithDescriptionAndIcon) {
                    swd = new StringWithDescriptionAndIcon(o);
                } else {
                    swd = new StringWithDescription(o);
                }
                String v = swd.getValue();
                String description = swd.getDescription();

                if (v == null || "".equals(v)) {
                    continue;
                }
                if (description == null) {
                    description = v;
                }
                if (v.equals(value)) {
                    row.setValue(swd);
                    selectedValue = v;
                    break;
                }
            }
            row.valueView.setHint(R.string.tag_dialog_value_hint);
            final String finalSelectedValue;
            if (selectedValue != null) {
                finalSelectedValue = selectedValue;
            } else {
                finalSelectedValue = null;
            }
            row.setOnClickListener(new OnClickListener() {
                @SuppressLint("NewApi")
                @Override
                public void onClick(View v) {
                    final View finalView = v;
                    finalView.setEnabled(false); // debounce
                    final AlertDialog dialog = buildComboDialog(hint != null ? hint : key, key, adapter, row, preset);
                    dialog.setOnShowListener(new OnShowListener() {
                        @Override
                        public void onShow(DialogInterface d) {
                            if (finalSelectedValue != null) {
                                scrollDialogToValue(finalSelectedValue, dialog, R.id.valueGroup);
                            }
                        }
                    });
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finalView.setEnabled(true);
                        }
                    });
                    dialog.show();
                }
            });
        }
        return row;
    }

    /**
     * Add a row that displays a dialog for selecting multi-values when clicked
     * 
     * @param rowLayout the Layout holding the roes
     * @param preset the relevant PresetItem
     * @param hint a description of the value to display
     * @param key the key
     * @param value the current value
     * @param adapter an ArrayAdapter with the selectable values
     * @return an instance of TagFormMultiselectDialogRow
     */
    private TagFormMultiselectDialogRow getMultiselectDialogRow(@NonNull LinearLayout rowLayout, @NonNull final PresetItem preset, @Nullable final String hint,
            @NonNull final String key, @NonNull final String value, @Nullable final ArrayAdapter<?> adapter) {
        final TagFormMultiselectDialogRow row = (TagFormMultiselectDialogRow) inflater.inflate(R.layout.tag_form_multiselect_dialog_row, rowLayout, false);
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        row.setPreset(preset);
        Log.d(DEBUG_TAG, "addMultiselectDialogRow value " + value);
        ArrayList<String> multiselectValues = Preset.splitValues(Util.getArrayList(value), preset, key);

        if (adapter != null) {
            ArrayList<StringWithDescription> selectedValues = new ArrayList<>();
            for (int i = 0; i < adapter.getCount(); i++) {
                Object o = adapter.getItem(i);

                StringWithDescription swd = new StringWithDescription(o);
                String v = swd.getValue();

                if (v == null || "".equals(v)) {
                    continue;
                }
                if (multiselectValues != null) {
                    for (String m : multiselectValues) {
                        if (v.equals(m)) {
                            selectedValues.add(swd);
                            break;
                        }
                    }
                }
            }
            row.setValue(selectedValues);
            row.valueView.setHint(R.string.tag_dialog_value_hint);

            row.setOnClickListener(new OnClickListener() {
                @SuppressLint("NewApi")
                @Override
                public void onClick(View v) {
                    final View finalView = v;
                    finalView.setEnabled(false); // debounce
                    final AlertDialog dialog = buildMultiselectDialog(hint != null ? hint : key, key, adapter, row, preset);
                    final Object tag = finalView.getTag();
                    dialog.setOnShowListener(new OnShowListener() {
                        @Override
                        public void onShow(DialogInterface d) {
                            if (tag instanceof String) {
                                scrollDialogToValue((String) tag, dialog, R.id.valueGroup);
                            }
                        }
                    });
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finalView.setEnabled(true);
                        }
                    });
                    dialog.show();
                    // the following is one big awful hack to stop the button dismissing the dialog
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            LinearLayout valueGroup = (LinearLayout) dialog.findViewById(R.id.valueGroup);
                            for (int pos = 0; pos < valueGroup.getChildCount(); pos++) {
                                View c = valueGroup.getChildAt(pos);
                                if (c instanceof AppCompatCheckBox) {
                                    ((AppCompatCheckBox) c).setChecked(false);
                                }
                            }
                        }
                    });

                }
            });
        }
        return row;
    }

    /**
     * Build a dialog for selecting a single value of none via a scrollable list of radio buttons
     * 
     * @param hint a description to display
     * @param key the key
     * @param adapter the ArrayAdapter holding the values
     * @param row the row we are started from
     * @param preset the relevant PresetItem
     * @return an AlertDialog
     */
    private AlertDialog buildComboDialog(@NonNull String hint, @NonNull String key, @Nullable final ArrayAdapter<?> adapter,
            @NonNull final TagFormDialogRow row, @NonNull final PresetItem preset) {
        String value = row.getValue();
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(getActivity());

        final View layout = themedInflater.inflate(R.layout.form_combo_dialog, null);
        RadioGroup valueGroup = (RadioGroup) layout.findViewById(R.id.valueGroup);
        builder.setView(layout);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(DEBUG_TAG, "radio button clicked " + row.getValue() + " " + v.getTag());
                if (!row.hasChanged()) {
                    RadioGroup g = (RadioGroup) v.getParent();
                    g.clearCheck();
                } else {
                    row.setChanged(false);
                }
            }
        };

        LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
        buttonLayoutParams.width = LayoutParams.MATCH_PARENT;

        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                Object o = adapter.getItem(i);
                StringWithDescription swd;
                Drawable icon = null;
                if (o instanceof StringWithDescriptionAndIcon) {
                    icon = ((StringWithDescriptionAndIcon) o).getIcon(preset);
                    if (icon != null) {
                        swd = new StringWithDescriptionAndIcon(o);
                    } else {
                        swd = new StringWithDescription(o);
                    }
                } else {
                    swd = new StringWithDescription(o);
                }
                String v = swd.getValue();

                if (v == null || "".equals(v)) {
                    continue;
                }
                addButton(getActivity(), valueGroup, i, swd, v.equals(value), icon, listener, buttonLayoutParams);
            }
        }
        final Handler handler = new Handler();
        builder.setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                tagListener.updateSingleValue((String) layout.getTag(), "");
                row.setValue("", "");
                row.setChanged(true);
                final DialogInterface finalDialog = dialog;
                // allow a tiny bit of time to see that the action actually worked
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finalDialog.dismiss();
                    }
                }, 100);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        final AlertDialog dialog = builder.create();
        layout.setTag(key);
        valueGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(DEBUG_TAG, "radio group onCheckedChanged");
                StringWithDescription value = null;
                if (checkedId != -1) {
                    RadioButton button = (RadioButton) group.findViewById(checkedId);
                    value = (StringWithDescription) button.getTag();
                    tagListener.updateSingleValue((String) layout.getTag(), value.getValue());
                    row.setValue(value);
                    row.setChanged(true);
                }
                // allow a tiny bit of time to see that the action actually worked
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                }, 100);
            }
        });

        return dialog;
    }

    /**
     * Build a dialog that allows multiple values to be selected
     * 
     * @param hint a description to display
     * @param key the key
     * @param adapter the ArrayAdapter holding the values
     * @param row the row we are started from
     * @param preset the relevant PresetItem
     * @return an AlertDialog
     */
    private AlertDialog buildMultiselectDialog(@NonNull String hint, @NonNull String key, @Nullable ArrayAdapter<?> adapter,
            @NonNull final TagFormMultiselectDialogRow row, @NonNull final PresetItem preset) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(getActivity());

        final View layout = themedInflater.inflate(R.layout.form_multiselect_dialog, null);
        final LinearLayout valueGroup = (LinearLayout) layout.findViewById(R.id.valueGroup);
        builder.setView(layout);

        LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
        buttonLayoutParams.width = LayoutParams.MATCH_PARENT;

        layout.setTag(key);
        ArrayList<String> values = Preset.splitValues(Util.getArrayList(row.getValue()), row.getPreset(), key);
        if (adapter != null) {
            int count = adapter.getCount();
            for (int i = 0; i < count; i++) {
                Object o = adapter.getItem(i);
                StringWithDescription swd;
                Drawable icon = null;
                if (o instanceof StringWithDescriptionAndIcon) {
                    icon = ((StringWithDescriptionAndIcon) o).getIcon(preset);
                    if (icon != null) {
                        swd = new StringWithDescriptionAndIcon(o);
                    } else {
                        swd = new StringWithDescription(o);
                    }
                } else {
                    swd = new StringWithDescription(o);
                }
                String v = swd.getValue();
                if (v == null || "".equals(v)) {
                    continue;
                }
                addCheck(getActivity(), valueGroup, swd, values != null ? values.contains(v) : false, icon, buttonLayoutParams);
            }
        }
        builder.setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<StringWithDescription> values = new ArrayList<>();
                for (int pos = 0; pos < valueGroup.getChildCount(); pos++) {
                    View c = valueGroup.getChildAt(pos);
                    if (c instanceof AppCompatCheckBox) {
                        AppCompatCheckBox checkBox = (AppCompatCheckBox) c;
                        if (checkBox.isChecked()) {
                            values.add((StringWithDescription) checkBox.getTag());
                        }
                    }
                }
                row.setValue(values);
                tagListener.updateSingleValue((String) layout.getTag(), row.getValue());
                row.setChanged(true);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        return builder.create();
    }

    /**
     * Build a dialog that allows multiple PresetCheckFields to be checked etc
     * 
     * @param hint a description to display
     * @param key the key for the PresetCHeckGroupField
     * @param row the row we are started from
     * @param preset the relevant PresetItem
     * @return an AlertDialog
     */
    private AlertDialog buildCheckGroupDialog(@NonNull String hint, @NonNull String key, @NonNull final TagFormCheckGroupDialogRow row,
            @NonNull final PresetItem preset) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(getActivity());

        final View layout = themedInflater.inflate(R.layout.form_multiselect_dialog, null);
        final LinearLayout valueGroup = (LinearLayout) layout.findViewById(R.id.valueGroup);
        builder.setView(layout);

        LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
        buttonLayoutParams.width = LayoutParams.MATCH_PARENT;

        layout.setTag(key);
        PresetCheckGroupField field = (PresetCheckGroupField) preset.getField(key);

        for (PresetCheckField check : field.getCheckFields()) {
            String checkKey = check.getKey();
            String checkValue = row.keyValues.get(checkKey);
            boolean selected = checkValue != null && checkValue.equals(check.getOnValue().getValue());
            boolean off = checkValue == null || "".equals(checkValue) || check.isOffValue(checkValue);
            if (selected || off) {
                addTriStateCheck(getActivity(), valueGroup, new StringWithDescription(checkKey, check.getHint()), selected, null, buttonLayoutParams);
            } else {
                // unknown value: add non-editable checkbox
                TriStateCheckBox checkBox = addTriStateCheck(getActivity(), valueGroup, new StringWithDescription(checkKey, checkKey + "=" + checkValue), false,
                        null, buttonLayoutParams);
                checkBox.setEnabled(false);
            }
        }

        builder.setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Map<String, String> keyValues = new HashMap<>();
                for (int pos = 0; pos < valueGroup.getChildCount(); pos++) {
                    View c = valueGroup.getChildAt(pos);
                    if (c instanceof TriStateCheckBox) {
                        TriStateCheckBox checkBox = (TriStateCheckBox) c;
                        String k = ((StringWithDescription) checkBox.getTag()).getValue();
                        PresetCheckField check = field.getCheckField(k);
                        Boolean state = checkBox.getState();
                        if (state == null) {
                            keyValues.put(k, "");
                        } else if (!checkBox.isEnabled()) {
                            // unknown stuff
                            keyValues.put(k, row.keyValues.get(k));
                        } else if (state) {
                            keyValues.put(k, check.getOnValue().getValue());
                        } else {
                            StringWithDescription offValue = check.getOffValue();
                            keyValues.put(k, offValue == null ? "" : offValue.getValue());
                        }
                    }
                }
                tagListener.updateTags(keyValues, false); // batch update
                row.setSelectedValues(keyValues);
                row.setChanged(true);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        return builder.create();
    }

    /**
     * Scroll the view in the dialog to show the value, assumes the ScrollView has id R.is.scrollView
     * 
     * @param value the value we want to scroll to
     * @param dialog the enclosing dialog
     * @param containerId ?
     */
    private void scrollDialogToValue(String value, AlertDialog dialog, int containerId) {
        Log.d(DEBUG_TAG, "scrollDialogToValue scrolling to " + value);
        final View sv = dialog.findViewById(R.id.myScrollView);
        if (sv != null) {
            ViewGroup container = (ViewGroup) dialog.findViewById(containerId);
            if (container != null) {
                for (int pos = 0; pos < container.getChildCount(); pos++) {
                    View child = container.getChildAt(pos);
                    Object tag = child.getTag();
                    if (tag instanceof StringWithDescription && ((StringWithDescription) tag).equals(value)) {
                        Util.scrollToRow(sv, child, true, true);
                        return;
                    }
                }
            } else {
                Log.d(DEBUG_TAG, "scrollDialogToValue container view null");
            }
        } else {
            Log.d(DEBUG_TAG, "scrollDialogToValue scroll view null");
        }
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
                    if (v instanceof TagTextRow && ((TagTextRow) v).getKey().equals(key)) {
                        ((TagTextRow) v).getValueView().requestFocus();
                        Util.scrollToRow(sv, v, true, true);
                        found = true;
                        break;
                    } else if (v instanceof TagFormDialogRow && ((TagFormDialogRow) v).getKey().equals(key)) {
                        Util.scrollToRow(sv, v, true, true);
                        ((TagFormDialogRow) v).click();
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
                    if (v instanceof TagTextRow && "".equals(((TagTextRow) v).getValue())) {
                        ((TagTextRow) v).getValueView().requestFocus();
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
     * An editable text only row for a tag with a dropdown containg suggestions
     * 
     * @author simon
     *
     */
    public static class TagTextRow extends LinearLayout {

        private TextView                   keyView;
        private CustomAutoCompleteTextView valueView;

        /**
         * Construct a editable text row for a tag
         * 
         * @param context Android Context
         */
        public TagTextRow(Context context) {
            super(context);
        }

        /**
         * Construct a editable text row for a tag
         * 
         * @param context Android Context
         * @param attrs an AttributeSet
         */
        public TagTextRow(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            keyView = (TextView) findViewById(R.id.textKey);
            valueView = (CustomAutoCompleteTextView) findViewById(R.id.textValue);
        }

        /**
         * Set the text via id of the key view
         * 
         * @param k a string resource id for the key
         */
        public void setKeyText(int k) {
            keyView.setText(k);
        }

        /**
         * Set the ArrayAdapter for values
         * 
         * @param a the ArrayAdapter
         */
        public void setValueAdapter(ArrayAdapter<?> a) {
            valueView.setAdapter(a);
        }

        /**
         * Return the OSM key value
         * 
         * @return the key as a String
         */
        public String getKey() {
            return (String) keyView.getTag();
        }

        /**
         * Get the current value
         * 
         * @return the current value as a String
         */
        public String getValue() {
            return valueView.getText().toString();
        }

        /**
         * Get the AutoCompleteTextView for the values
         * 
         * @return a CustomAutoCompleteTextView
         */
        public CustomAutoCompleteTextView getValueView() {
            return valueView;
        }
    }

    /**
     * A row that shows a radio-button like UI for selecting a single value
     * 
     * @author simon
     *
     */
    public static class TagComboRow extends LinearLayout {

        private TextView   keyView;
        private RadioGroup valueGroup;
        private String     value;
        private Context    context;
        private int        idCounter = 0;
        private boolean    changed   = false;

        /**
         * Construct a radio-button like UI for selecting a single value
         * 
         * @param context Android Context
         */
        public TagComboRow(@NonNull Context context) {
            super(context);
            this.context = context;
        }

        /**
         * Construct a radio-button like UI for selecting a single value
         * 
         * @param context Android Context
         * @param attrs an AttributeSet
         */
        public TagComboRow(@NonNull Context context, AttributeSet attrs) {
            super(context, attrs);
            this.context = context;
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            keyView = (TextView) findViewById(R.id.textKey);
            valueGroup = (RadioGroup) findViewById(R.id.valueGroup);
        }

        /**
         * Return the OSM key value
         * 
         * @return the key as a String
         */
        public String getKey() {
            return (String) keyView.getTag();
        }

        /**
         * Get the RadioGroup that holds the buttons
         * 
         * @return the RadioGroup
         */
        @NonNull
        public RadioGroup getRadioGroup() {
            return valueGroup;
        }

        /**
         * Set the current value
         * 
         * @param value the value
         */
        public void setValue(@NonNull String value) {
            this.value = value;
        }

        /**
         * Get the current value
         * 
         * @return the current value as a String
         */
        public String getValue() {
            return value;
        }

        /**
         * Set the changed flag
         * 
         * @param changed value to set the flag to
         */
        public void setChanged(boolean changed) {
            this.changed = changed;
        }

        /**
         * Check if the value has changed
         * 
         * @return true if changed
         */
        public boolean hasChanged() {
            return changed;
        }

        /**
         * Add a button to the RadioDroup
         * 
         * @param description description of the value
         * @param value the value
         * @param selected if true the value is selected
         * @param icon icon to display if any
         */
        public void addButton(@NonNull String description, @NonNull String value, boolean selected, @Nullable Drawable icon) {
            final AppCompatRadioButton button = new AppCompatRadioButton(context);
            button.setText(description);
            button.setTag(value);
            button.setChecked(selected);
            button.setId(idCounter++);
            if (icon != null) {
                button.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            }
            valueGroup.addView(button);
            if (selected) {
                setValue(value);
            }
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(DEBUG_TAG, "radio button clicked " + getValue() + " " + button.getTag());
                    if (!changed) {
                        RadioGroup g = (RadioGroup) v.getParent();
                        g.clearCheck();
                    } else {
                        changed = false;
                    }
                }
            });
        }
    }

    /**
     * Adda button to a RadioGroup
     * 
     * @param context Android Context
     * @param group the RadioGroup we ant to add the button to
     * @param id an id for the button
     * @param swd the value for the button
     * @param selected is true the button is selected
     * @param icon an icon to display if any
     * @param listener the Listenet to call if the button is clicked
     * @param layoutParams LayoutParams for the button
     */
    private void addButton(@NonNull Context context, @NonNull RadioGroup group, int id, @NonNull StringWithDescription swd, boolean selected,
            @Nullable Drawable icon, @NonNull View.OnClickListener listener, @NonNull LayoutParams layoutParams) {
        final AppCompatRadioButton button = new AppCompatRadioButton(context);
        String description = swd.getDescription();
        button.setText(description != null && !"".equals(description) ? description : swd.getValue());
        button.setTag(swd);
        button.setChecked(selected);
        button.setId(id);
        if (icon != null) {
            button.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }
        button.setLayoutParams(layoutParams);
        group.addView(button);
        button.setOnClickListener(listener);
    }

    /**
     * Display a single value and allow editing via a dialog
     */
    public static class TagFormDialogRow extends LinearLayout {

        TextView        keyView;
        TextView        valueView;
        private String  value;
        private boolean changed = false;
        PresetItem      preset;

        /**
         * Construct a row that will display a Dialog when clicked
         * 
         * @param context Android Context
         */
        public TagFormDialogRow(@NonNull Context context) {
            super(context);
        }

        /**
         * Construct a row that will display a Dialog when clicked
         * 
         * @param context Android Context
         * @param attrs an AttributeSet
         */
        public TagFormDialogRow(@NonNull Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            keyView = (TextView) findViewById(R.id.textKey);
            valueView = (TextView) findViewById(R.id.textValue);
        }

        @Override
        public void setOnClickListener(final OnClickListener listener) {
            valueView.setOnClickListener(listener);
        }

        /**
         * Return the OSM key value
         * 
         * @return the key as a String
         */
        public String getKey() {
            return (String) keyView.getTag();
        }

        /**
         * Set the value for this row
         * 
         * @param value the value
         * @param description a description of the values
         */
        public void setValue(String value, String description) {
            this.value = value;
            setValueDescription(description);
            valueView.setTag(value);
            if (getParent() instanceof EditableLayout) {
                ((EditableLayout) getParent()).putTag(getKey(), getValue());
            }
        }

        /**
         * Just set the description of the value
         * 
         * @param description the description
         */
        public void setValueDescription(String description) {
            valueView.setText(description, TextView.BufferType.SPANNABLE);
        }

        /**
         * Get the TextView for this row
         * 
         * @return a TextView
         */
        public TextView getValueView() {
            return valueView;
        }

        /**
         * Set the value for this row
         * 
         * @param swd the value
         */
        public void setValue(@NonNull StringWithDescription swd) {
            String description = swd.getDescription();
            setValue(swd.getValue(), description != null && !"".equals(description) ? description : swd.getValue());
            Drawable icon = null;
            Log.d(DEBUG_TAG, "got swd but no swdai");
            if (swd instanceof StringWithDescriptionAndIcon) {
                icon = ((StringWithDescriptionAndIcon) swd).getIcon(preset);
            }
            valueView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }

        /**
         * Set the value for the row, setting the description to the same value
         * 
         * @param s the value
         */
        public void setValue(String s) {
            setValue(s, s);
        }

        /**
         * Get the value for this row
         * 
         * @return the value as a String
         */
        public String getValue() {
            return value;
        }

        /**
         * Set the changed flag for this row
         * 
         * @param changed the value for the changed flag
         */
        public void setChanged(boolean changed) {
            this.changed = changed;
        }

        /**
         * Check if the row has been changed
         * 
         * @return true if changed
         */
        public boolean hasChanged() {
            return changed;
        }

        /**
         * Set the PresetItem this row belongs too
         * 
         * @param preset the PresetItem
         */
        public void setPreset(@Nullable PresetItem preset) {
            this.preset = preset;
        }

        /**
         * Get the PresetITem this row belongs too
         * 
         * @return the PresetItem or null
         */
        @Nullable
        public PresetItem getPreset() {
            return preset;
        }

        /**
         * Click on this row
         */
        public void click() {
            valueView.performClick();
        }
    }

    /**
     * Row that displays opening_hours values and allows changing them via a dialog
     */
    public static class TagFormOpeningHoursDialogRow extends TagFormDialogRow {

        OnClickListener listener;

        LinearLayout         valueList;
        final LayoutInflater inflater;
        int                  errorTextColor = ContextCompat.getColor(getContext(),
                ThemeUtils.getStyleAttribColorValue(getContext(), R.attr.textColorError, R.color.material_red));

        /**
         * Construct a row that will show the OpeningHoursFragmeent when clicked
         * 
         * @param context Android Context
         */
        public TagFormOpeningHoursDialogRow(Context context) {
            super(context);
            inflater = LayoutInflater.from(context);
        }

        /**
         * Construct a row that will show the OpeningHoursFragmeent when clicked
         * 
         * @param context Android Context
         * @param attrs an AttributeSet
         */
        public TagFormOpeningHoursDialogRow(Context context, AttributeSet attrs) {
            super(context, attrs);
            inflater = LayoutInflater.from(context);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            valueList = (LinearLayout) findViewById(R.id.valueList);

        }

        /**
         * Set the onclicklistener for every value
         */
        @Override
        public void setOnClickListener(final OnClickListener listener) {
            this.listener = listener;
            for (int pos = 0; pos < valueList.getChildCount(); pos++) {
                View v = valueList.getChildAt(pos);
                if (v instanceof TextView) {
                    ((TextView) v).setOnClickListener(listener);
                }
            }
        }

        /**
         * Set the OH value for the row
         * 
         * @param ohValue the original opening hours value
         * @param rules rules parsed from the value
         */
        public void setValue(String ohValue, @Nullable List<Rule> rules) {
            int childCount = valueList.getChildCount();
            for (int pos = 0; pos < childCount; pos++) { // don't delete first child, just clear
                if (pos == 0) {
                    setValue("", "");
                } else {
                    valueList.removeViewAt(1);
                }
            }
            boolean first = true;
            if (rules != null && !rules.isEmpty()) {
                for (Rule r : rules) {
                    if (first) {
                        setValue(r.toString());
                        first = false;
                    } else {
                        TextView extraValue = (TextView) inflater.inflate(R.layout.form_dialog_multiselect_value, valueList, false);
                        extraValue.setText(r.toString());
                        extraValue.setTag(r.toString());
                        valueList.addView(extraValue);
                    }
                }
            } else {
                setValue(ohValue);
                if (preset == null || preset.getValueType(getKey()) != ValueType.OPENING_HOURS_MIXED) {
                    if (ohValue != null && !"".equals(ohValue)) {
                        valueView.setTextColor(errorTextColor);
                    }
                } else {
                    // try to find a description for the value
                    Map<String, PresetField> map = preset.getFields();
                    if (map != null) {
                        PresetField field = map.get(getKey());
                        if (field instanceof PresetComboField) {
                            StringWithDescription[] values = ((PresetComboField) field).getValues();
                            if (values != null) {
                                for (StringWithDescription swd : values) {
                                    if (swd.getValue().equals(ohValue)) {
                                        setValueDescription(swd.getDescription());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            super.value = ohValue;
            setOnClickListener(listener);
        }
    }

    /**
     * Row that displays multiselect values and allows changing them via a dialog
     */
    public static class TagFormMultiselectDialogRow extends TagFormDialogRow {

        OnClickListener listener;

        LinearLayout         valueList;
        final LayoutInflater inflater;

        /**
         * Construct a row that will show a dialog that allows multiple values to be selected when clicked
         * 
         * @param context Android Context
         */
        public TagFormMultiselectDialogRow(Context context) {
            super(context);
            inflater = LayoutInflater.from(context);
        }

        /**
         * Construct a row that will show a dialog that allows multiple values to be selected when clicked
         * 
         * @param context Android Context
         * @param attrs an AttributeSet
         */
        public TagFormMultiselectDialogRow(Context context, AttributeSet attrs) {
            super(context, attrs);
            inflater = LayoutInflater.from(context);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            valueList = (LinearLayout) findViewById(R.id.valueList);
        }

        /**
         * Set the onclicklistener for every value
         */
        @Override
        public void setOnClickListener(final OnClickListener listener) {
            this.listener = listener;
            for (int pos = 0; pos < valueList.getChildCount(); pos++) {
                View v = valueList.getChildAt(pos);
                if (v instanceof TextView) {
                    ((TextView) v).setOnClickListener(listener);
                }
            }
        }

        /**
         * Add additional description values as individual TextViews
         * 
         * @param values the List of values
         */
        public void setValue(List<StringWithDescription> values) {
            String value = "";
            char delimiter = preset.getDelimiter(getKey());
            int childCount = valueList.getChildCount();
            for (int pos = 0; pos < childCount; pos++) { // don^t delete first child, just clear
                if (pos == 0) {
                    setValue("", "");
                } else {
                    valueList.removeViewAt(1);
                }
            }
            boolean first = true;
            StringBuilder builder = new StringBuilder(value);
            for (StringWithDescription swd : values) {
                String d = swd.getDescription();
                if (first) {
                    setValue(swd.getValue(), d != null && !"".equals(d) ? d : swd.getValue());
                    first = false;
                } else {
                    TextView extraValue = (TextView) inflater.inflate(R.layout.form_dialog_multiselect_value, valueList, false);
                    extraValue.setText(d != null && !"".equals(d) ? d : swd.getValue());
                    extraValue.setTag(swd.getValue());
                    valueList.addView(extraValue);
                }
                // collect the individual values for what we actually store
                if (builder.length() != 0) {
                    builder.append(delimiter);
                }
                builder.append(swd.getValue());
            }
            super.value = builder.toString();
            setOnClickListener(listener);
        }
    }

    /**
     * Inline multiselect value display with checkboxes
     */
    public static class TagMultiselectRow extends LinearLayout {
        private TextView     keyView;
        private LinearLayout valueLayout;
        private Context      context;
        private char         delimiter;

        /**
         * Construct a row that will multiple values to be selected
         * 
         * @param context Android Context
         */
        public TagMultiselectRow(Context context) {
            super(context);
            this.context = context;
        }

        /**
         * Construct a row that will multiple values to be selected
         * 
         * @param context Android Context
         * @param attrs and AttriuteSet
         */
        public TagMultiselectRow(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.context = context;
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            keyView = (TextView) findViewById(R.id.textKey);
            valueLayout = (LinearLayout) findViewById(R.id.valueGroup);
        }

        /**
         * Return the OSM key value
         * 
         * @return the key as a String
         */
        public String getKey() {
            return (String) keyView.getTag();
        }

        /**
         * Get the Layout containing the CheckBoxes for the values
         * 
         * @return a LinearLayout
         */
        public LinearLayout getValueGroup() {
            return valueLayout;
        }

        /**
         * Return all checked values concatenated with the required delimiter
         * 
         * @return a String containg an OSM style list of values
         */
        public String getValue() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < valueLayout.getChildCount(); i++) {
                AppCompatCheckBox check = (AppCompatCheckBox) valueLayout.getChildAt(i);
                if (check.isChecked()) {
                    if (result.length() > 0) { // not the first entry
                        result.append(delimiter);
                    }
                    result.append(valueLayout.getChildAt(i).getTag());
                }
            }
            return result.toString();
        }

        /**
         * Set the delimiter to be used when creating an OSM value String from the contents of the row
         * 
         * @param delimiter the delimter to set
         */
        public void setDelimiter(char delimiter) {
            this.delimiter = delimiter;
        }

        /**
         * Add a CheckBox to this row
         * 
         * @param description the description to display
         * @param value the value to use if the CheckBox is selected
         * @param selected if true the CheckBox will be selected
         * @param icon an icon if there is one
         * @param listener a listener to call when the CheckBox is clicked
         * @return the CheckBox for further use
         */
        public AppCompatCheckBox addCheck(@NonNull String description, @NonNull String value, boolean selected, @Nullable Drawable icon,
                @NonNull CompoundButton.OnCheckedChangeListener listener) {
            final AppCompatCheckBox check = new AppCompatCheckBox(context);
            check.setText(description);
            check.setTag(value);
            if (icon != null) {
                check.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            }
            check.setChecked(selected);
            valueLayout.addView(check);
            check.setOnCheckedChangeListener(listener);
            return check;
        }
    }

    /**
     * Add a CheckBox to a Layout
     * 
     * @param context Android Context
     * @param layout the Layout we want to add the CheckBox to
     * @param swd the value
     * @param selected if true the CheckBox will be selected
     * @param icon an icon if there is one
     * @param layoutParams the LayoutParams for the CheckBox
     * @return the CheckBox for further use
     */
    private AppCompatCheckBox addCheck(@NonNull Context context, @NonNull LinearLayout layout, @NonNull StringWithDescription swd, boolean selected,
            @Nullable Drawable icon, @NonNull LayoutParams layoutParams) {
        final AppCompatCheckBox check = new AppCompatCheckBox(context);
        String description = swd.getDescription();
        check.setText(description != null && !"".equals(description) ? description : swd.getValue());
        check.setTag(swd);
        if (icon != null) {
            check.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }
        check.setLayoutParams(layoutParams);
        check.setChecked(selected);
        layout.addView(check);
        return check;
    }

    /**
     * Add a CheckBox to a Layout
     * 
     * @param context Android Context
     * @param layout the Layout we want to add the CheckBox to
     * @param swd the value
     * @param selected if true the CheckBox will be selected
     * @param icon an icon if there is one
     * @param layoutParams the LayoutParams for the CheckBox
     * @return the CheckBox for further use
     */
    private TriStateCheckBox addTriStateCheck(@NonNull Context context, @NonNull LinearLayout layout, @NonNull StringWithDescription swd, boolean selected,
            @Nullable Drawable icon, @NonNull LayoutParams layoutParams) {
        final TriStateCheckBox check = new TriStateCheckBox(context);
        String description = swd.getDescription();
        check.setText(description != null && !"".equals(description) ? description : swd.getValue());
        check.setTag(swd);
        if (icon != null) {
            check.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }
        check.setLayoutParams(layoutParams);
        check.setChecked(selected);
        layout.addView(check);
        return check;
    }

    public static class TagCheckRow extends LinearLayout {

        private TextView              keyView;
        private IndeterminateCheckBox valueCheck;

        /**
         * Construct a row with a single CheckBox
         * 
         * @param context Android Context
         */
        public TagCheckRow(@NonNull Context context) {
            super(context);
        }

        /**
         * Construct a row with a single CheckBox
         * 
         * @param context Android Context
         * @param attrs an AttributeSet
         */
        public TagCheckRow(@NonNull Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            keyView = (TextView) findViewById(R.id.textKey);
            valueCheck = (TriStateCheckBox) findViewById(R.id.valueSelected);
        }

        /**
         * Return the OSM key value
         * 
         * @return the key as a String
         */
        public String getKey() {
            return (String) keyView.getTag();
        }

        /**
         * Get the CheckBox
         * 
         * @return return the CheckBox associated with this row
         */
        public IndeterminateCheckBox getCheckBox() {
            return valueCheck;
        }

        /**
         * Check if the CheckBox for this row is checked
         * 
         * @return true if the CHeckBox is checked
         */
        public boolean isChecked() {
            return valueCheck.isChecked();
        }
    }

    /**
     * Inline CheckGroup row with tri-state checkboxes
     */
    public static class TagCheckGroupRow extends LinearLayout {
        private TextView     keyView;
        private LinearLayout valueLayout;
        private Context      context;
        private char         delimiter;

        /**
         * Construct a row that will multiple values to be selected
         * 
         * @param context Android Context
         */
        public TagCheckGroupRow(Context context) {
            super(context);
            this.context = context;
        }

        /**
         * Construct a row that will multiple values to be selected
         * 
         * @param context Android Context
         * @param attrs and AttriuteSet
         */
        public TagCheckGroupRow(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.context = context;
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            keyView = (TextView) findViewById(R.id.textKey);
            valueLayout = (LinearLayout) findViewById(R.id.valueGroup);
        }

        /**
         * Return the OSM key value
         * 
         * @return the key as a String
         */
        public String getKey() {
            return (String) keyView.getTag();
        }

        /**
         * Get the Layout containing the CheckBoxes for the values
         * 
         * @return a LinearLayout
         */
        public LinearLayout getValueGroup() {
            return valueLayout;
        }

        /**
         * Return all checked values concatenated with the required delimiter
         * 
         * @return a String containg an OSM style list of values
         */
        public String getValue() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < valueLayout.getChildCount(); i++) {
                AppCompatCheckBox check = (AppCompatCheckBox) valueLayout.getChildAt(i);
                if (check.isChecked()) {
                    if (result.length() > 0) { // not the first entry
                        result.append(delimiter);
                    }
                    result.append(valueLayout.getChildAt(i).getTag());
                }
            }
            return result.toString();
        }

        /**
         * Add a CheckBox to this row
         * 
         * @param description the description to display
         * @param state if true/false the CheckBox will be checekd/unchecked, if null it will be set to indetermiante
         *            state,
         * @param listener a listener to call when the CheckBox is clicked
         * @return the CheckBox for further use
         */
        public TriStateCheckBox addCheck(@NonNull String description, @Nullable Boolean state, @NonNull OnStateChangedListener listener) {
            final TriStateCheckBox check = new TriStateCheckBox(context);
            check.setText(description);
            check.setState(state);
            valueLayout.addView(check);
            check.setOnStateChangedListener(listener);
            return check;
        }
    }

    /**
     * Row that displays checkgroup keys and values and allows changing them via a dialog
     */
    public static class TagFormCheckGroupDialogRow extends TagFormDialogRow {

        OnClickListener listener;

        LinearLayout         valueList;
        final LayoutInflater inflater;

        Map<String, String> keyValues;

        /**
         * Construct a row that will show a dialog that allows multiple values to be selected when clicked
         * 
         * @param context Android Context
         */
        public TagFormCheckGroupDialogRow(Context context) {
            super(context);
            inflater = LayoutInflater.from(context);
        }

        /**
         * Construct a row that will show a dialog that allows multiple values to be selected when clicked
         * 
         * @param context Android Context
         * @param attrs an AttributeSet
         */
        public TagFormCheckGroupDialogRow(Context context, AttributeSet attrs) {
            super(context, attrs);
            inflater = LayoutInflater.from(context);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            valueList = (LinearLayout) findViewById(R.id.valueList);
        }

        /**
         * Set the onclicklistener for every value
         */
        @Override
        public void setOnClickListener(final OnClickListener listener) {
            this.listener = listener;
            for (int pos = 0; pos < valueList.getChildCount(); pos++) {
                View v = valueList.getChildAt(pos);
                if (v instanceof TextView) {
                    ((TextView) v).setOnClickListener(listener);
                }
            }
        }

        /**
         * Add additional description values as individual TextViews
         * 
         * @param keyValues a Map of the current keys and values for this check group
         */
        public void setSelectedValues(Map<String, String> keyValues) {
            this.keyValues = keyValues;
            int childCount = valueList.getChildCount();
            for (int pos = 0; pos < childCount; pos++) { // don^t delete first child, just clear
                if (pos == 0) {
                    setValue("", "");
                } else {
                    valueList.removeViewAt(1);
                }
            }

            boolean first = true;
            PresetCheckGroupField field = null;

            for (Entry<String, String> entry : keyValues.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (field == null) {
                    PresetField f = preset.getField(key);
                    if (f == null || !(f instanceof PresetCheckGroupField)) {
                        return;
                    }
                    field = (PresetCheckGroupField) f;
                }
                PresetCheckField check = field.getCheckField(key);
                if (check != null && !"".equals(value)) {
                    String d = check.getHint();
                    String valueOn = check.getOnValue().getValue();
                    StringWithDescription valueOff = check.getOffValue();
                    boolean off = valueOff != null && valueOff.getValue().equals(value);
                    boolean nonEditable = false;
                    if (!valueOn.equals(value) && !off) {
                        // unknown value
                        d = check.getKey() + "=" + value;
                        nonEditable = true;
                    }
                    if (first) {
                        setValue(key, d != null && !"".equals(d) ? d : key);
                        first = false;
                        if (off) {
                            strikeThrough(getValueView());
                        } else if (nonEditable) {
                            getValueView().setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        }
                    } else {
                        TextView extraValue = (TextView) inflater.inflate(R.layout.form_dialog_multiselect_value, valueList, false);
                        extraValue.setText(d != null && !"".equals(d) ? d : key);
                        extraValue.setTag(key);
                        valueList.addView(extraValue);
                        if (off) {
                            strikeThrough(extraValue);
                        } else if (nonEditable) {
                            extraValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        }
                    }
                }
            }

            setOnClickListener(listener);
        }
    }

    private static final StrikethroughSpan STRIKE_THROUGH_SPAN = new StrikethroughSpan();

    /**
     * Strike through some text in a TextView
     * 
     * @param tv TextView to use
     */
    private static void strikeThrough(@NonNull TextView tv) {
        Spannable spannable = (Spannable) tv.getText();
        spannable.setSpan(STRIKE_THROUGH_SPAN, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
                copyButton.setVisibility(View.VISIBLE);
                cutButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);
            } else {
                headerTitleView.setText(R.string.tag_form_unknown_element);
                applyPresetButton.setVisibility(View.GONE);
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

    /**
     * Show a dialog to select a name
     * 
     * @param ctx Android context
     * @return an AlertDialog
     */
    private AlertDialog buildNameDialog(Context ctx) {
        if (names == null) {
            names = App.getNames(ctx);
        }
        ArrayList<NameAndTags> suggestions = (ArrayList<NameAndTags>) names.getNames(new TreeMap<>());
        ArrayAdapter<NameAndTags> adapter = null;
        if (suggestions != null && !suggestions.isEmpty()) {
            Collections.sort(suggestions);
            adapter = new ArrayAdapter<>(ctx, R.layout.autocomplete_row, suggestions);
        }

        Builder builder = new AlertDialog.Builder(ctx);

        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(ctx);

        final CustomAutoCompleteTextView autoComplete = (CustomAutoCompleteTextView) inflater.inflate(R.layout.customautocomplete, null);
        builder.setView(autoComplete);

        autoComplete.setHint(R.string.tag_autocomplete_name_hint);
        autoComplete.setAdapter(adapter);

        builder.setNegativeButton(R.string.cancel, null);
        final AlertDialog dialog = builder.create();

        autoComplete.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(DEBUG_TAG, "onItemClicked value");
                Object o = parent.getItemAtPosition(position);
                if (o instanceof Names.NameAndTags) {
                    TagMap tags = ((NameAndTags) o).getTags();
                    tags.put(Tags.KEY_NAME, ((NameAndTags) o).getName());
                    tagListener.applyTagSuggestions(tags);
                } else if (o instanceof String) {
                    tagListener.updateSingleValue(Tags.KEY_NAME, (String) o);
                } else {
                    Log.e(DEBUG_TAG, "got a " + o.getClass().getName() + " instead of NameAndTags");
                }
                // allow a tiny bit of time to see that the action actually worked
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        update();
                    }
                }, 100);
            }
        });

        autoComplete.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP || event.getAction() == KeyEvent.ACTION_MULTIPLE) {
                    if (v instanceof EditText) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            tagListener.updateSingleValue(Tags.KEY_NAME, ((EditText) v).getText().toString());
                            dialog.dismiss();
                            update();
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        return dialog;
    }
}
