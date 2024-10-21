package de.blau.android.propertyeditor.tagform;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.presets.PresetComboField;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetTagField;
import de.blau.android.presets.ValueType;
import de.blau.android.propertyeditor.TagChanged;
import de.blau.android.util.StringWithDescription;

public class ValueWidgetRow extends DialogRow implements TagChanged {

    private ValueType      valueType;
    private PresetTagField field;

    /**
     * Construct a row that will display a Dialog when clicked
     * 
     * @param context Android Context
     */
    public ValueWidgetRow(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct a row that will display a Dialog when clicked
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public ValueWidgetRow(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Add a row that displays a dialog for selecting a single when clicked
     * 
     * @param caller the calling TagFormFragment instance
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the roes
     * @param preset the relevant PresetItem
     * @param field the current PresetTagField
     * @param value the current value
     * @param values all relevant values (preset MRU etc)
     * @param allTags all current tags
     * @return a ValueWidgetRow
     */
    static ValueWidgetRow getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout,
            @NonNull final PresetItem preset, @NonNull final PresetTagField field, @Nullable final String value, @Nullable final List<String> values,
            @NonNull final Map<String, String> allTags) {
        final ValueWidgetRow row = (ValueWidgetRow) inflater.inflate(R.layout.tag_form_value_widget_row, rowLayout, false);
        final String key = field.getKey();
        final String hint = field.getHint();
        row.keyView.setText(hint);
        row.keyView.setTag(key);
        row.setPreset(preset);
        row.valueType = preset.getValueType(key);
        row.field = field;

        row.valueView.setHint(R.string.tag_dialog_value_hint);
        row.setValue(value == null ? "" : value);

        row.valueView.setFocusable(false);
        row.valueView.setFocusableInTouchMode(false);
        row.setOnClickListener(v -> {
            final View finalView = v;
            finalView.setEnabled(false); // debounce
            if (ValueType.INTEGER == row.valueType) {
                IntegerValueFragment.show(caller, hint, key, row.getValue(), values, preset, allTags);
            }
            if (ValueType.CARDINAL_DIRECTION == row.valueType) {
                DirectionValueFragment.show(caller, hint, key, row.getValue(), values, preset, allTags);
            }
        });
        return row;
    }

    @Override
    public void changed(String key, String value) {
        if (key.equals(this.getKey())) {
            valueView.setEnabled(true);
            setValue(value);
        }
    }

    @Override
    public void setValue(@NonNull String value) {
        // it might be simpler to simply use the normal autocomplete adapter here
        if (field instanceof PresetComboField) {
            for (StringWithDescription swd : ((PresetComboField) field).getValues()) {
                if (value.equals(swd.getValue())) {
                    setValue(swd);
                    return;
                }
            }
        }
        super.setValue(value);
    }
}
