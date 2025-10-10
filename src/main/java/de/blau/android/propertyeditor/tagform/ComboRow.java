package de.blau.android.propertyeditor.tagform;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import de.blau.android.R;
import de.blau.android.contract.Ui;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetListEntryWithIcon;
import de.blau.android.propertyeditor.tagform.TagFormFragment.EditableLayout;
import de.blau.android.util.Util;

/**
 * A row that shows a radio-button like UI for selecting a single value
 * 
 * @author simon
 *
 */
public class ComboRow extends LinearLayout {

    protected static final String DEBUG_TAG = ComboRow.class.getSimpleName().substring(0, Math.min(23, ComboRow.class.getSimpleName().length()));
    private TextView              keyView;
    private RadioGroup            valueGroup;
    private String                value;
    private Context               context;
    private int                   idCounter = 0;
    private boolean               changed   = false;

    /**
     * Construct a radio-button like UI for selecting a single value
     * 
     * @param context Android Context
     */
    public ComboRow(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    /**
     * Construct a radio-button like UI for selecting a single value
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public ComboRow(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (isInEditMode()) {
            return; // allow visual editor to work
        }
        setKeyView((TextView) findViewById(R.id.textKey));
        valueGroup = (RadioGroup) findViewById(R.id.valueGroup);
    }

    /**
     * Return the OSM key value
     * 
     * @return the key as a String
     */
    public String getKey() {
        return (String) getKeyView().getTag();
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
            Util.setCompoundDrawableWithIntrinsicBounds(Util.isRtlScript(context), button, icon);
            button.setCompoundDrawablePadding(Ui.COMPOUND_DRAWABLE_PADDING);
        }
        valueGroup.addView(button);
        if (selected) {
            setValue(value);
        }
        button.setOnClickListener(v -> {
            Log.d(DEBUG_TAG, "radio button clicked " + getValue() + " " + button.getTag());
            if (!changed) {
                RadioGroup g = (RadioGroup) v.getParent();
                g.clearCheck();
            } else {
                changed = false;
            }
        });
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
     * Get a row for a combo with inline RadioButtons
     * 
     * @param caller the calling TagFormFragment instance
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the row
     * @param preset the best matched PresetITem for the key
     * @param hint a textual description of what the key is
     * @param key the key
     * @param value any existing value for the tag
     * @param adapter an ArrayAdapter containing all the predefined values in the PresetItem for the key
     * @return a TagComboRow instance
     */
    @NonNull
    static ComboRow getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout,
            @NonNull final PresetItem preset, @Nullable final String hint, @NonNull final String key, @Nullable final String value,
            @Nullable final ArrayAdapter<?> adapter) {
        final ComboRow row = (ComboRow) inflater.inflate(R.layout.tag_form_combo_row, rowLayout, false);
        row.getKeyView().setText(hint != null ? hint : key);
        row.getKeyView().setTag(key);
        if (adapter == null) {
            return row;
        }
        for (int i = 0; i < adapter.getCount(); i++) {
            Object o = adapter.getItem(i);
            PresetListEntryWithIcon swd = new PresetListEntryWithIcon(o);
            String v = swd.getValue();
            String description = swd.getDescription();
            if (v == null || "".equals(v)) {
                continue;
            }
            if (description == null) {
                description = v;
            }
            Drawable icon = swd.getIcon(caller.getContext(), preset);
            row.addButton(description, v, v.equals(value), icon);
        }

        row.getRadioGroup().setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(DEBUG_TAG, "radio group onCheckedChanged");
            String v = "";
            if (checkedId != -1) {
                RadioButton button = (RadioButton) group.findViewById(checkedId);
                v = (String) button.getTag();
            }
            caller.updateSingleValue(key, v);
            if (rowLayout instanceof EditableLayout) {
                ((EditableLayout) rowLayout).putTag(key, v);
            }
            row.setValue(v);
            row.setChanged(true);
        });
        return row;
    }
}
