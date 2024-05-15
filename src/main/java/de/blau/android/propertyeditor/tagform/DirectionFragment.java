package de.blau.android.propertyeditor.tagform;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.redinput.compassview.CompassView;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.Tags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;
import de.blau.android.sensors.CompassEventListener;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class DirectionFragment extends DialogFragment {

    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, DirectionFragment.class.getSimpleName().length());
    protected static final String DEBUG_TAG = DirectionFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "DIRECTION_FRAGMENT";

    private static final String KEY_KEY      = "key";
    private static final String VALUE_KEY    = "value";
    private static final String VALUES_KEY   = "values";
    private static final String PRESET_KEY   = "preset";
    private static final String HINT_KEY     = "hint";
    private static final String ALL_TAGS_KEY = "all_tags";

    private static final int MAX_BUTTONS_WITHOUT_MRU = 15;

    private int    lastChecked = -1;
    private String value       = null;

    public static void show(@NonNull Fragment caller, @NonNull String hint, @NonNull String key, @Nullable String value, @Nullable List<String> values,
            @Nullable PresetItem preset, @NonNull Map<String, String> allTags) {
        FragmentManager fm = caller.getChildFragmentManager();

        final PresetGroup rootGroup = App.getCurrentRootPreset(caller.getContext()).getRootGroup();
        DirectionFragment df = DirectionFragment.newInstance(hint, key, value, values, preset != null && rootGroup != null ? preset.getPath(rootGroup) : null,
                allTags);
        de.blau.android.propertyeditor.Util.removeChildFragment(fm, TAG);
        df.show(fm, TAG);
    }

    /**
     * Build a dialog for adding/editing a direction value
     * 
     * @param caller the calling TagFormFragment instance
     * @param hint a description to display
     * @param key the key
     * @param allTags
     * @param preset
     * @param adapter an optional ArrayAdapter
     * @param row the row we are started from
     * @return an AlertDialog
     */
    @NonNull
    public static DirectionFragment newInstance(@NonNull String hint, @NonNull String key, @Nullable String value, @Nullable List<String> values,
            @Nullable PresetElementPath presetPath, @NonNull Map<String, String> allTags) {
        //
        DirectionFragment f = new DirectionFragment();

        Bundle args = new Bundle();
        args.putString(HINT_KEY, hint);
        args.putString(KEY_KEY, key);
        args.putString(VALUE_KEY, value);
        args.putStringArrayList(VALUES_KEY, new ArrayList<>(values));
        args.putSerializable(PRESET_KEY, presetPath);
        args.putSerializable(ALL_TAGS_KEY, new HashMap<>(allTags));
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(DEBUG_TAG, "onAttach");
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

        PresetItem preset = (PresetItem) Preset.getElementByPath(App.getCurrentRootPreset(getContext()).getRootGroup(), presetPath);
        final FragmentActivity activity = getActivity();
        Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);

        final ViewGroup layout = (ViewGroup) themedInflater.inflate(R.layout.value_fragment, null);

        CompassWidget widget = new CompassWidget(activity);

        final TextView usageText = layout.findViewById(R.id.usageText);
        usageText.setText(widget.getUsageText(activity));

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
        if (adapter != null) {
            addValuesToRadioGroup(activity, themedInflater, widget, adapter, valueGroup);
            valueGroup.setTag(key);
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
     * Add the values from the adapter to the radio group
     * 
     * @param activity the calling FragmentActivity
     * @param inflater an inflater instance
     * @param widget the widget for filters
     * @param adapter the adapter holding the values
     * @param valueGroup the group to add the values to
     */
    private void addValuesToRadioGroup(@NonNull final FragmentActivity activity, @NonNull final LayoutInflater inflater, @NonNull CompassWidget widget,
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
            if (o instanceof TagFormFragment.Ruler) {
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

    private class CompassWidget {

        private final CompassView  compass;
        final Sensor               rotation;
        final SensorManager        sensorManager;
        final CompassEventListener compassListener;

        /**
         * Construct a new widget
         * 
         * @param activity current FragmentActivity
         */
        CompassWidget(@NonNull FragmentActivity activity) {
            compass = new CompassView(activity, null);
            Float direction = Tags.parseDirection(value);
            if (direction == Float.NaN) {
                direction = 0f;
            }
            if (direction < 0) {
                direction = direction + 360f;
            }

            compass.setDegrees(direction, true); // with animation
            compass.setBackgroundColor(ThemeUtils.getStyleAttribColorValue(activity, R.attr.highlight_background, R.color.black));
            compass.setLineColor(Color.RED);
            compass.setMarkerColor(Color.RED);
            compass.setTextColor(ThemeUtils.getStyleAttribColorValue(activity, R.attr.text_normal, R.color.ccc_white));
            compass.setShowMarker(true);
            compass.setTextSize(37);
            compass.setRangeDegrees(50);
            compass.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));

            sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

            compassListener = new CompassEventListener((float azimut) -> {
                if (compass.isEnabled()) {
                    compass.setDegrees(azimut, true);
                }
            });

            rotation = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) : null;
            if (rotation != null) {
                sensorManager.registerListener(compassListener, rotation, SensorManager.SENSOR_DELAY_UI);
            } else {
                ScreenMessage.toastTopInfo(activity, R.string.toast_no_compass);
            }

            compass.setOnCompassDragListener((float azimut) -> {
                if (rotation != null) {
                    sensorManager.unregisterListener(compassListener, rotation);
                }
                compass.setDegrees(azimut);
            });
        }

        /**
         * Get the current value
         * 
         * @return the current value as a String
         */
        @NonNull
        String getValue() {
            return Integer.toString((int) compass.getDegrees());
        }

        /**
         * Test if we want to retain a value for selection
         * 
         * @param v the value
         * @return true if we want to keep it
         */
        boolean filter(@Nullable String v) {
            if (v == null || "".equals(v)) { // suppress empty value added for deletion
                return false;
            }
            try { // MRU may have numeric values too
                Float.parseFloat(v);
                return false;
            } catch (NumberFormatException nfex) {
                // add
            }
            return true;
        }

        /**
         * Enable the widget
         */
        void enable() {
            compass.setEnabled(true);
            compass.setFocusable(true);
            compass.setLineColor(Color.RED);
            compass.setMarkerColor(Color.RED);
        }

        /**
         * Disable the widget
         */
        void disable() {
            compass.setEnabled(false);
            compass.setFocusable(false);
            compass.setLineColor(Color.GRAY);
            compass.setMarkerColor(Color.GRAY);
        }

        /**
         * Called when the dialog is dismissed
         */
        void onDismiss() {
            if (rotation != null) {
                sensorManager.unregisterListener(compassListener, rotation);
            }
        }

        /**
         * Get the view
         * 
         * @return a view
         */
        @NonNull
        View getWidgetView() {
            return compass;
        }

        @Nullable
        String getUsageText(@NonNull Context ctx) {
            return ctx.getString(R.string.compass_widget_usage);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putString(VALUE_KEY, value);
    }
}
