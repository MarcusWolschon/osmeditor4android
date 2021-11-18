package de.blau.android.propertyeditor.tagform;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.contract.Ui;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.tagform.TagFormFragment.EditableLayout;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.StringWithDescriptionAndIcon;

/**
 * Display a single value and allow editing via a dialog
 */
public class DialogRow extends LinearLayout {

    private static final String DEBUG_TAG = "DialogRow";

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
    public DialogRow(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct a row that will display a Dialog when clicked
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public DialogRow(@NonNull Context context, AttributeSet attrs) {
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
        valueView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
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
            icon = ((StringWithDescriptionAndIcon) swd).getIcon(getContext(), preset);
            valueView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            valueView.setCompoundDrawablePadding(Ui.COMPOUND_DRAWABLE_PADDING);
        }
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
     * Set the value to s and don't touch the description
     * 
     * @param s the value to set
     */
    public void setValueOnly(String s) {
        value = s;
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
