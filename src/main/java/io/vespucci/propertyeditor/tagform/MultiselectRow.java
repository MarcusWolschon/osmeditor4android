package io.vespucci.propertyeditor.tagform;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import io.vespucci.R;
import io.vespucci.contract.Ui;
import io.vespucci.presets.PresetItem;
import io.vespucci.propertyeditor.tagform.TagFormFragment.EditableLayout;
import io.vespucci.util.StringWithDescriptionAndIcon;
import io.vespucci.util.Util;

/**
 * Inline multiselect value display with checkboxes
 */
public class MultiselectRow extends LinearLayout {
    private TextView       keyView;
    protected LinearLayout valueLayout;
    protected Context      context;
    private char           delimiter;

    /**
     * Construct a row that will multiple values to be selected
     * 
     * @param context Android Context
     */
    public MultiselectRow(Context context) {
        super(context);
        this.context = context;
    }

    /**
     * Construct a row that will multiple values to be selected
     * 
     * @param context Android Context
     * @param attrs and AttriuteSet
     */
    public MultiselectRow(Context context, AttributeSet attrs) {
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
            Util.setCompoundDrawableWithIntrinsicBounds(Util.isRtlScript(context), check, icon);
            check.setCompoundDrawablePadding(Ui.COMPOUND_DRAWABLE_PADDING);
        }
        check.setChecked(selected);
        valueLayout.addView(check);
        check.setOnCheckedChangeListener(listener);
        return check;
    }

    /**
     * Get the TextView for the key
     * 
     * @return the TextView for the key
     */
    public TextView getKeyView() {
        return keyView;
    }

    /**
     * Add a row for a multi-select with inline CheckBoxes
     * 
     * @param caller the calling TagFormFragment
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the row
     * @param preset the best matched PresetITem for the key
     * @param hint a textual description of what the key is
     * @param key the key
     * @param values existing values for the tag
     * @param adapter an ArrayAdapter containing all the predefined values in the PresetItem for the key
     * @return a TagMultiselectRow instance
     */
    @NonNull
    static MultiselectRow getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout,
            @NonNull final PresetItem preset, @Nullable final String hint, final String key, @Nullable final List<String> values,
            @Nullable ArrayAdapter<?> adapter) {
        final MultiselectRow row = (MultiselectRow) inflater.inflate(R.layout.tag_form_multiselect_row, rowLayout, false);
        row.getKeyView().setText(hint != null ? hint : key);
        row.getKeyView().setTag(key);
        if (adapter != null) {
            row.setDelimiter(preset.getDelimiter(key));
            CompoundButton.OnCheckedChangeListener onCheckedChangeListener = (buttonView, isChecked) -> {
                caller.updateSingleValue(key, row.getValue());
                if (rowLayout instanceof EditableLayout) {
                    ((EditableLayout) rowLayout).putTag(key, row.getValue());
                }
            };
            int count = adapter.getCount();
            for (int i = 0; i < count; i++) {
                Object o = adapter.getItem(i);
                StringWithDescriptionAndIcon swd = new StringWithDescriptionAndIcon(o);
                String v = swd.getValue();
                String description = swd.getDescription();
                if (v == null || "".equals(v)) {
                    continue;
                }
                if (description == null) {
                    description = v;
                }
                Drawable icon = swd.getIcon(caller.getContext(), preset);
                row.addCheck(description, v, values != null && values.contains(v), icon, onCheckedChangeListener);
            }
        }
        return row;
    }
}
