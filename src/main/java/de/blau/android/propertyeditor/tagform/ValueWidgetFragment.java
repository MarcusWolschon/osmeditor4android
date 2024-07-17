package de.blau.android.propertyeditor.tagform;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetItem;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public abstract class ValueWidgetFragment extends DialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ValueWidgetFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = ValueWidgetFragment.class.getSimpleName().substring(0, TAG_LEN);

    protected static final String KEY_KEY      = "key";
    protected static final String VALUE_KEY    = "value";
    protected static final String VALUES_KEY   = "values";
    protected static final String PRESET_KEY   = "preset";
    protected static final String HINT_KEY     = "hint";
    protected static final String ALL_TAGS_KEY = "all_tags";

    private static final int MAX_BUTTONS_WITHOUT_MRU = 15;

    private int    lastChecked = -1;
    private String value       = null;

    /**
     * Set the fragment arguments
     * 
     * @param f the fragment instance
     * @param hint description of the key
     * @param key the key
     * @param value the existing value
     * @param values any additional values from the preset or mru
     * @param presetPath the preset items path or null
     * @param allTags all current tags
     * @return an f with params set
     */
    @NonNull
    public static <T extends ValueWidgetFragment> T setArguments(@NonNull T f, @NonNull String hint, @NonNull String key, @Nullable String value,
            @Nullable List<String> values, @Nullable PresetElementPath presetPath, @NonNull Map<String, String> allTags) {
        //
        Bundle args = new Bundle();
        args.putString(HINT_KEY, hint);
        args.putString(KEY_KEY, key);
        args.putString(VALUE_KEY, value);
        args.putStringArrayList(VALUES_KEY, values != null ? new ArrayList<>(values) : new ArrayList<>());
        args.putSerializable(PRESET_KEY, presetPath);
        args.putSerializable(ALL_TAGS_KEY, new HashMap<>(allTags));
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        TagFormFragment caller = (TagFormFragment) getParentFragment();

        String hint = getArguments().getString(HINT_KEY);
        String key = getArguments().getString(KEY_KEY);
        value = savedInstanceState != null ? savedInstanceState.getString(VALUE_KEY) : getArguments().getString(VALUE_KEY);
        List<String> values = getArguments().getStringArrayList(VALUES_KEY);
        PresetElementPath presetPath = Util.getSerializeable(getArguments(), PRESET_KEY, PresetElementPath.class);
        Map<String, String> allTags = Util.getSerializeable(getArguments(), ALL_TAGS_KEY, HashMap.class);

        PresetItem preset = presetPath != null ? (PresetItem) Preset.getElementByPath(App.getCurrentRootPreset(getContext()).getRootGroup(), presetPath) : null;
        final FragmentActivity activity = getActivity();
        Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);

        final ViewGroup layout = (ViewGroup) themedInflater.inflate(R.layout.value_fragment, null);

        ValueWidget widget = getWidget(activity, value, values);

        layout.addView(widget.getWidgetView());

        // filter 1
        List<String> filteredValues = new ArrayList<>();
        for (String v : values) {
            if (widget.filter(v)) {
                filteredValues.add(v);
            }
        }

        ArrayAdapter<?> adapter = caller.getValueAutocompleteAdapter(key, filteredValues, preset, null, allTags, true, false, MAX_BUTTONS_WITHOUT_MRU);
        final RadioGroup valueGroup = (RadioGroup) layout.findViewById(R.id.valueGroup);
        if (adapter != null && !adapter.isEmpty()) {
            addValuesToRadioGroup(activity, themedInflater, widget, adapter, valueGroup);
            valueGroup.setTag(key);
        }

        boolean hasRadioButtons = valueGroup.getChildCount() > 0;

        if (!hasRadioButtons) {
            valueGroup.setVisibility(View.GONE);
        }
        final TextView usageText = layout.findViewById(R.id.usageText);
        String usageString = widget.getUsageText(activity, hasRadioButtons);
        if (usageString != null) {
            usageText.setText(usageString);
        } else {
            usageText.setVisibility(View.GONE);
        }

        builder.setView(layout);

        builder.setPositiveButton(R.string.save, (d, which) -> {
            String ourValue = null;
            int checkedId = valueGroup.getCheckedRadioButtonId();
            if (checkedId != -1) {
                RadioButton button = (RadioButton) valueGroup.findViewById(checkedId);
                ourValue = ((StringWithDescription) button.getTag()).getValue();
            } else {
                ourValue = widget.getValue();
            }
            caller.updateSingleValue((String) layout.getTag(), ourValue);
        });
        builder.setNegativeButton(R.string.clear, (d, which) -> caller.updateSingleValue((String) layout.getTag(), ""));
        builder.setNeutralButton(R.string.cancel, (d, which) -> caller.enableTextRow((String) layout.getTag()));

        final AlertDialog dialog = builder.create();
        layout.setTag(key);
        dialog.setOnDismissListener((DialogInterface d) -> widget.onDismiss());
        dialog.setOnShowListener((DialogInterface d) -> valueGroup.post(() -> {
            if (valueGroup.getCheckedRadioButtonId() != -1) {
                widget.disable();
            }
        }));
        return dialog;
    }

    /**
     * Get the widget
     * 
     * @param activity current FragmentActivity
     * @param value current value
     * @param values optional list of values from preset and/or MRU list
     * @return
     */
    abstract ValueWidget getWidget(@NonNull FragmentActivity activity, @NonNull String value, @Nullable List<String> values);

    /**
     * Add the values from the adapter to the radio group
     * 
     * @param activity the calling FragmentActivity
     * @param inflater an inflater instance
     * @param widget the widget for filters
     * @param adapter the adapter holding the values
     * @param valueGroup the group to add the values to
     */
    private void addValuesToRadioGroup(@NonNull final FragmentActivity activity, @NonNull final LayoutInflater inflater, @NonNull ValueWidget widget,
            @NonNull ArrayAdapter<?> adapter, @NonNull final RadioGroup valueGroup) {
        final View.OnClickListener listener = v -> {
            RadioGroup g = (RadioGroup) v.getParent();
            int id = ((RadioButton) v).getId();
            if (((RadioButton) v).isChecked() && widget.getWidgetView().isEnabled()) {
                widget.disable();
            } else if (((RadioButton) v).isChecked() && lastChecked == id) {
                g.clearCheck();
                widget.enable();
                lastChecked = -1;
                return;
            }
            lastChecked = id;
        };
        final View divider = inflater.inflate(R.layout.divider2, null);
        divider.setLayoutParams(new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));

        android.view.ViewGroup.LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
        buttonLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;

        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            Object o = adapter.getItem(i);
            if (o instanceof TagFormFragment.Ruler && valueGroup.getChildCount() > 0) {
                valueGroup.addView(divider);
                continue;
            }
            StringWithDescription swd = new StringWithDescription(o);
            String v = swd.getValue();
            if (widget.filter(v)) { // filter 2
                final boolean selected = v.equals(value);
                if (selected) {
                    lastChecked = i;
                }
                ComboDialogRow.addButton(activity, valueGroup, i, swd, selected, null, listener, buttonLayoutParams);
            }
        }
    }

    interface ValueWidget {

        /**
         * Get the current value
         * 
         * @return the current value as a String
         */
        @NonNull
        String getValue();

        /**
         * Test if we want to retain a value for selection
         * 
         * @param v the value
         * @return true if we want to keep it
         */
        boolean filter(@Nullable String v);

        /**
         * Enable the widget
         */
        void enable();

        /**
         * Disable the widget
         */
        void disable();

        /**
         * Called when the dialog is dismissed
         */
        void onDismiss();

        /**
         * Get the view
         * 
         * @return a view
         */
        @NonNull
        View getWidgetView();

        /**
         * Get any usage test
         * 
         * @param ctx an Android Context
         * @param hasAdditionalValues if additional values are present
         * @return a String or null
         */
        @Nullable
        String getUsageText(@NonNull Context ctx, boolean hasAdditionalValues);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putString(VALUE_KEY, value);
    }
}
