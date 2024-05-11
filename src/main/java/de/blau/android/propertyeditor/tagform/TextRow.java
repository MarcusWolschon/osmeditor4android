package de.blau.android.propertyeditor.tagform;

import java.util.List;
import java.util.Map;

import com.redinput.compassview.CompassView;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.measure.Measure;
import de.blau.android.measure.Params;
import de.blau.android.measure.streetmeasure.MeasureContract.LengthUnit;
import de.blau.android.nsi.Names;
import de.blau.android.nsi.Names.NameAndTags;
import de.blau.android.osm.Tags;
import de.blau.android.presets.PresetComboField;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetTagField;
import de.blau.android.presets.PresetTextField;
import de.blau.android.presets.ValueType;
import de.blau.android.propertyeditor.InputTypeUtil;
import de.blau.android.propertyeditor.SanitizeTextWatcher;
import de.blau.android.propertyeditor.TagEditorFragment;
import de.blau.android.propertyeditor.tagform.TagFormFragment.EditableLayout;
import de.blau.android.sensors.CompassEventListener;
import de.blau.android.util.LocaleUtils;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.util.Value;
import de.blau.android.views.CustomAutoCompleteTextView;

/**
 * An editable text only row for a tag with a dropdown containg suggestions
 * 
 * @author simon
 *
 */
public class TextRow extends LinearLayout implements KeyValueRow {

    protected static final String DEBUG_TAG = TextRow.class.getSimpleName().substring(0, Math.min(23, TextRow.class.getSimpleName().length()));

    public static final int INPUTTYPE_CAPS_MASK = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_CAP_WORDS;

    private TextView                   keyView;
    private CustomAutoCompleteTextView valueView;
    private ValueType                  valueType;

    /**
     * Construct a editable text row for a tag
     * 
     * @param context Android Context
     */
    public TextRow(Context context) {
        super(context);
    }

    /**
     * Construct a editable text row for a tag
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public TextRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (isInEditMode()) {
            return; // allow visual editor to work
        }
        setKeyView((TextView) findViewById(R.id.textKey));
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

    @Override
    public String getKey() {
        return (String) keyView.getTag();
    }

    @Override
    public String getValue() {
        return valueView.getText().toString();
    }

    /**
     * Get the current ValueType if any
     * 
     * @return the ValueType or null
     */
    @Nullable
    public ValueType getValueType() {
        return valueType;
    }

    /**
     * @param valueType the valueType to set
     */
    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    /**
     * Get the AutoCompleteTextView for the values
     * 
     * @return a CustomAutoCompleteTextView
     */
    public CustomAutoCompleteTextView getValueView() {
        return valueView;
    }

    /**
     * @return the keyView
     */
    public TextView getKeyView() {
        return keyView;
    }

    /**
     * @param keyView the keyView to set
     */
    public void setKeyView(TextView keyView) {
        this.keyView = keyView;
    }

    /**
     * Get a row for an unstructured text value
     * 
     * @param caller the calling TagFormFragment instance
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the row
     * @param preset the best matched PresetITem for the key
     * @param field the PresetField
     * @param value any existing value for the tag
     * @param values a list of all the predefined values in the PresetItem for the key
     * @param allTags a Map of the tags currently being edited
     * @return a TagTextRow instance
     */
    static TextRow getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout,
            @Nullable final PresetItem preset, @NonNull final PresetTagField field, @Nullable final String value, @Nullable final List<String> values,
            final Map<String, String> allTags) {
        final TextRow row = (TextRow) inflater.inflate(R.layout.tag_form_text_row, rowLayout, false);
        final String key = field.getKey();
        final String hint = preset != null ? field.getHint() : null;
        final String defaultValue = field.getDefaultValue();
        row.valueType = preset != null ? preset.getValueType(key) : null;
        final boolean isName = Tags.isLikeAName(key);
        final Context context = rowLayout.getContext();
        final boolean imperial = App.getGeoContext(context).imperial(caller.propertyEditorListener.getElement());
        final boolean isMPHSpeed = !isName && Tags.isSpeedKey(key) && imperial;
        final TextView ourKeyView = row.getKeyView();
        ourKeyView.setText(hint != null ? hint : key);
        ourKeyView.setTag(key);
        final CustomAutoCompleteTextView ourValueView = row.getValueView();

        if (field instanceof PresetTextField) {
            final int length = ((PresetTextField) field).length();
            if (length > 0) { // if it isn't set don't bother
                ourValueView.getViewTreeObserver().addOnGlobalLayoutListener(new LayoutListener(ourKeyView, ourValueView, length));
            }
        }

        ourValueView.setText(value);

        // set empty value to be on the safe side
        ourValueView.setAdapter(new ArrayAdapter<>(caller.getActivity(), R.layout.autocomplete_row, new String[0]));

        if (field instanceof PresetComboField && ((PresetComboField) field).isMultiSelect() && preset != null) {
            ourValueView.setTokenizer(new CustomAutoCompleteTextView.SingleCharTokenizer(preset.getDelimiter(key)));
        }
        setHint(field, ourValueView);
        final ValueType valueType = row.getValueType();
        if (showMeasureDialog(context, row)) {
            ourValueView.setFocusable(false);
            ourValueView.setFocusableInTouchMode(false);
            ourValueView.setOnClickListener(v -> {
                final View finalView = v;
                finalView.setEnabled(false); // debounce
                final AlertDialog dialog = buildMeasureDialog(caller, hint != null ? hint : key, key,
                        caller.getValueAutocompleteAdapter(key, values, preset, null, allTags, true, false, -1), row, valueType, imperial);
                dialog.setOnDismissListener(d -> finalView.setEnabled(true));
                dialog.show();
                return;
            });
        }
        if (Tags.DIRECTION_KEYS.contains(key)) {
            ourValueView.setFocusable(false);
            ourValueView.setFocusableInTouchMode(false);
            ourValueView.setOnClickListener(v -> {
                final View finalView = v;
                finalView.setEnabled(false); // debounce
                final AlertDialog dialog = buildDirectionDialog(caller, hint != null ? hint : key, key, row, valueType);
                dialog.setOnDismissListener(d -> finalView.setEnabled(true));
                dialog.show();
                return;
            });
        }
        ourValueView.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(DEBUG_TAG, "onFocusChange");
            String rowValue = row.getValue();
            if (!hasFocus && !rowValue.equals(value)) {
                caller.updateSingleValue(key, rowValue);
                if (rowLayout instanceof EditableLayout) {
                    ((EditableLayout) rowLayout).putTag(key, rowValue);
                }
                return;
            }
            if (hasFocus) {
                ArrayAdapter<?> adapter = caller.getValueAutocompleteAdapter(key, values, preset, null, allTags, true, false, -1);
                setAdapter(ourValueView, adapter);
                if (isMPHSpeed) {
                    TagEditorFragment.initMPHSpeed(context, ourValueView, caller.propertyEditorListener);
                } else if (valueType == null) {
                    InputTypeUtil.enableTextSuggestions(ourValueView);
                }
                if (isName && LocaleUtils.usesLatinScript(Util.getPrimaryLocale(caller.getResources()))) {
                    ourValueView.setInputType(
                            (ourValueView.getInputType() & ~INPUTTYPE_CAPS_MASK) | InputType.TYPE_CLASS_TEXT | App.getLogic().getPrefs().getAutoNameCap());
                } else {
                    InputTypeUtil.setInputTypeFromValueType(ourValueView, valueType);
                }
            }
        });
        ourValueView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(DEBUG_TAG, "onItemClicked value");
            Object o = parent.getItemAtPosition(position);
            if (o instanceof Names.NameAndTags) {
                ourValueView.setOrReplaceText(((NameAndTags) o).getName());
                caller.applyTagSuggestions(((NameAndTags) o).getTags(), caller::update);
                caller.update();
                return;
            }
            setOrReplaceText(ourValueView, o);
            caller.updateSingleValue(key, row.getValue());
            if (rowLayout instanceof EditableLayout) {
                ((EditableLayout) rowLayout).putTag(key, row.getValue());
            }
        });
        ourValueView.addTextChangedListener(new SanitizeTextWatcher(caller.getActivity(), caller.maxStringLength));
        return row;
    }

    /**
     * Check if we should show the measuring modal
     * 
     * @param context an Android Context
     * @param row a textRow instance
     * @return true if we should show the modal
     */
    private static boolean showMeasureDialog(@NonNull final Context context, @NonNull TextRow row) {
        return (row.valueType == ValueType.DIMENSION_HORIZONTAL || row.valueType == ValueType.DIMENSION_VERTICAL) && Measure.isAvailable(context);
    }

    /**
     * Check if this row is suitable for initial focus, aka doesn't pop up anything
     * 
     * @param context an Android Context
     * @return true if we can safely focus on this
     */
    public boolean initialFoxus(@NonNull final Context context) {
        return "".equals(getValue()) && (valueType == null || !showMeasureDialog(context, this));
    }

    /**
     * Set the text to display
     * 
     * @param textView the CustomAutoCompleteTextView
     * @param text an Object holding the text
     */
    protected static void setOrReplaceText(final CustomAutoCompleteTextView textView, Object text) {
        if (text instanceof Value) {
            textView.setOrReplaceText(((Value) text).getValue());
            return;
        }
        if (text instanceof String) {
            textView.setOrReplaceText((String) text);
        }
    }

    /**
     * Set the hint on the value view
     * 
     * @param field the PresetField for the tag
     * @param valueView the View for the value
     */
    protected static void setHint(@Nullable final PresetTagField field, @NonNull final CustomAutoCompleteTextView valueView) {
        if (field instanceof PresetTextField) {
            valueView.setHint(R.string.tag_value_hint);
            return;
        }
        valueView.setHint(R.string.tag_autocomplete_value_hint);
    }

    /**
     * Build a dialog for adding/editing a value or measuring it with an external app
     * 
     * @param caller the calling TagFormFragment instance
     * @param hint a description to display
     * @param key the key
     * @param adapter an optional ArrayAdapter
     * @param row the row we are started from
     * @param valueType the field ValueType
     * @param imperial true if the element is in freedom unit space
     * 
     * @return an AlertDialog
     */
    private static AlertDialog buildMeasureDialog(@NonNull final TagFormFragment caller, @NonNull String hint, @NonNull String key,
            @Nullable ArrayAdapter<?> adapter, @NonNull final TextRow row, @NonNull final ValueType valueType, boolean imperial) {
        String value = row.getValue();
        Builder builder = new AlertDialog.Builder(caller.getActivity());
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(caller.getActivity());

        final View layout = themedInflater.inflate(R.layout.text_line, null);
        final AutoCompleteTextView input = layout.findViewById(R.id.text_line_edit);
        input.setText(value);
        setAdapter(input, adapter);
        input.setThreshold(0);
        input.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            final AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) v;
            if (autoCompleteTextView.getAdapter() != null) {
                if (hasFocus) {
                    autoCompleteTextView.showDropDown();
                } else {
                    autoCompleteTextView.dismissDropDown();
                }
            }
        });
        builder.setView(layout);

        builder.setNegativeButton(R.string.save, (dialog, which) -> {
            String ourValue = input.getText().toString();
            caller.updateSingleValue((String) layout.getTag(), ourValue);
            setOrReplaceText(row.getValueView(), ourValue);
        });
        builder.setPositiveButton(R.string.measure, null);
        builder.setNeutralButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();
        layout.setTag(key);

        dialog.setOnShowListener(d -> {
            Button positive = ((AlertDialog) d).getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener(view -> {
                caller.getMeasureLauncher()
                        .launch(new Params(key,
                                imperial && App.getPreferences(caller.getContext()).useImperialUnits() ? LengthUnit.FOOT_AND_INCH : LengthUnit.METER, 5, null,
                                valueType == ValueType.DIMENSION_VERTICAL, null));
                dialog.dismiss();
            });
        });
        return dialog;
    }

    /**
     * Build a dialog for adding/editing a direction value
     * 
     * @param caller the calling TagFormFragment instance
     * @param hint a description to display
     * @param key the key
     * @param row the row we are started from
     * @param valueType the field ValueType
     * @return an AlertDialog
     */
    private static AlertDialog buildDirectionDialog(@NonNull final TagFormFragment caller, @NonNull String hint, @NonNull String key,
            @NonNull final TextRow row, @NonNull final ValueType valueType) {
        String value = row.getValue();

        final FragmentActivity activity = caller.getActivity();
        Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);

        final View layout = themedInflater.inflate(R.layout.compass_direction, null);
        CompassView compass = new CompassView(activity, null);

        Float direction = 0f;
        try {
            direction = Float.parseFloat(value);
        } catch (NumberFormatException nfex) {
            direction = Tags.cardinalToDegrees(value);
            if (direction == null) {
                direction = 0f;
            }
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
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        compass.setLayoutParams(lp);

        SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

        CompassEventListener compassListener = new CompassEventListener((float azimut) -> compass.setDegrees(azimut, true));

        final Sensor rotation = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) : null;
        if (rotation != null) {
            sensorManager.registerListener(compassListener, rotation, SensorManager.SENSOR_DELAY_UI);
        }

        compass.setOnCompassDragListener((float azimut) -> {
            if (rotation != null) {
                sensorManager.unregisterListener(compassListener, rotation);
            }
            compass.setDegrees(azimut);
        });

        ((LinearLayout) layout).addView(compass);
        builder.setView(layout);

        builder.setNegativeButton(R.string.save, (dialog, which) -> {
            String ourValue = Integer.toString((int) compass.getDegrees());
            caller.updateSingleValue((String) layout.getTag(), ourValue);
            setOrReplaceText(row.getValueView(), ourValue);
        });
        builder.setNeutralButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();
        layout.setTag(key);
        dialog.setOnDismissListener((DialogInterface d) -> {
            if (rotation != null) {
                sensorManager.unregisterListener(compassListener, rotation);
            }
        });
        return dialog;
    }

    /**
     * Set an ArrayAdapter on an AutoCompleteTextView, checking that it isn't empty
     * 
     * @param textView the AutoCompleteTextView
     * @param adapter the ArrayAdapter
     */
    protected static void setAdapter(@NonNull final AutoCompleteTextView textView, @Nullable ArrayAdapter<?> adapter) {
        if (adapter != null && !adapter.isEmpty()) {
            textView.setAdapter(adapter);
        }
    }
}
