package de.blau.android.propertyeditor.tagform;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import ch.poole.openinghoursfragment.OpeningHoursFragment;
import ch.poole.openinghoursfragment.ValueWithDescription;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.ValueType;
import de.blau.android.presets.PresetComboField;
import de.blau.android.presets.PresetField;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.util.Snack;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;

/**
 * Row that displays opening_hours values and allows changing them via a dialog
 */
public class OpeningHoursDialogRow extends MultiselectDialogRow {

    private static final String FRAGMENT_OPENING_HOURS_TAG = "fragment_opening_hours";

    int errorTextColor = ContextCompat.getColor(getContext(), ThemeUtils.getStyleAttribColorValue(getContext(), R.attr.textColorError, R.color.material_red));

    /**
     * Construct a row that will show the OpeningHoursFragmeent when clicked
     * 
     * @param context Android Context
     */
    public OpeningHoursDialogRow(Context context) {
        super(context);
    }

    /**
     * Construct a row that will show the OpeningHoursFragmeent when clicked
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public OpeningHoursDialogRow(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        super.setValueOnly(ohValue);
        setOnClickListener(listener);
    }

    /**
     * Add a row that display an opening hours value and starts the OH editor when clicked
     * 
     * @param caller the calling TagFormFragment instance
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the rows
     * @param preset the current preset
     * @param hint a hint for the value
     * @param key the key
     * @param value the current value if any
     * @param adapter an Adapter holding any suggested values
     * @return a TagFormDialogRow
     */
    static OpeningHoursDialogRow getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout,
            @Nullable PresetItem preset, @Nullable final String hint, @NonNull final String key, @NonNull String value,
            @Nullable final ArrayAdapter<?> adapter) {
        final OpeningHoursDialogRow row = (OpeningHoursDialogRow) inflater.inflate(R.layout.tag_form_openinghours_dialog_row, rowLayout, false);
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        row.setPreset(preset);

        boolean strictSucceeded = false;
        boolean lenientSucceeded = false;
        List<Rule> rules = null;

        OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(value.getBytes()));

        try {
            rules = parser.rules(true);
            strictSucceeded = true;
        } catch (Exception e) {
            parser = new OpeningHoursParser(new ByteArrayInputStream(value.getBytes()));
            try {
                rules = parser.rules(false);
                value = ch.poole.openinghoursparser.Util.rulesToOpeningHoursString(rules);
                caller.tagListener.updateSingleValue(key, value);
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
                        Snack.barWarning(rowLayout, caller.getString(R.string.toast_openinghours_autocorrected, row.keyView.getText().toString()),
                                Snackbar.LENGTH_LONG);
                    }
                });
            } else if (!strictSucceeded && adapter == null) {
                // only warn if the value should be an OH string
                rowLayout.post(new Runnable() {

                    @Override
                    public void run() {
                        Snack.barWarning(rowLayout, caller.getString(R.string.toast_openinghours_invalid, row.keyView.getText().toString()),
                                Snackbar.LENGTH_LONG);
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
                FragmentManager fm = caller.getChildFragmentManager();
                de.blau.android.propertyeditor.Util.removeChildFragment(fm, FRAGMENT_OPENING_HOURS_TAG);
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
                List<String> isoCodes = caller.propertyEditorListener.getIsoCodes();
                OpeningHoursFragment openingHoursDialog = OpeningHoursFragment.newInstance(keyWithDescription,
                        isoCodes != null && !isoCodes.isEmpty() ? isoCodes.get(0) : null,
                        preset != null ? preset.getObjectTag(App.getCurrentPresets(caller.getContext()), caller.tagListener.getKeyValueMapSingle(false)) : null,
                        finalValue, caller.prefs.lightThemeEnabled() ? R.style.Theme_AppCompat_Light_Dialog_Alert : R.style.Theme_AppCompat_Dialog_Alert, -1,
                        true, textValues);
                openingHoursDialog.show(fm, FRAGMENT_OPENING_HOURS_TAG);
            }
        });
        return row;
    }
}
