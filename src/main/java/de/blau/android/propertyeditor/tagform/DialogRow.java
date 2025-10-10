package de.blau.android.propertyeditor.tagform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.R;
import de.blau.android.contract.Ui;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetListEntryWithIcon;
import de.blau.android.propertyeditor.PropertyEditorFragment;
import de.blau.android.propertyeditor.tagform.TagFormFragment.EditableLayout;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Util;

/**
 * Display a single value and allow editing via a dialog
 */
public class DialogRow extends LinearLayout implements KeyValueRow {

    private static final String DEBUG_TAG = DialogRow.class.getSimpleName().substring(0, Math.min(23, DialogRow.class.getSimpleName().length()));

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

    @Override
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
        if (swd instanceof PresetListEntryWithIcon) {
            final Context context = getContext();
            icon = ((PresetListEntryWithIcon) swd).getIcon(context, preset);
            Util.setCompoundDrawableWithIntrinsicBounds(Util.isRtlScript(context), valueView, icon);
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

    @Override
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

    /**
     * Find the parent TagFormFragment and update the tag
     * 
     * This is ugly, but works reliably
     * 
     * @param context the relevant Android Context
     * @param key the tag key
     * @param value the tag value
     */
    protected static void updateTag(@NonNull Context context, @NonNull String key, @NonNull StringWithDescription value) {
        TagFormFragment fragment = findFragment(context);
        if (fragment != null) {
            fragment.updateSingleValue(key, value.getValue());
            fragment.updateDialogRow(key, value);
        }
    }

    /**
     * Find the TagFormFragment
     * 
     * @param context the relevant Android Context
     * @return the fragment or null if not found
     */
    @Nullable
    private static TagFormFragment findFragment(@NonNull Context context) {
        context = unwrap(context);
        if (!(context instanceof FragmentActivity)) {
            Log.e(DEBUG_TAG, "Context is not a FragmentActivity, instead it is " + context.getClass().getCanonicalName());
            return null;
        }
        FragmentManager fm = ((FragmentActivity) context).getSupportFragmentManager();
        List<Fragment> fragments = new ArrayList<>(fm.getFragments());
        Collections.reverse(fragments); // latest added first
        for (Fragment fragment : fragments) {
            if (fragment instanceof PropertyEditorFragment) {
                fm = fragment.getChildFragmentManager();
                for (Fragment f : fm.getFragments()) {
                    if (f instanceof TagFormFragment) {
                        return (TagFormFragment) f;
                    }
                }
            }
        }
        Log.e(DEBUG_TAG, "TagFormFragment not found");
        return null;
    }

    /**
     * Find the parent TagFormFragment and update the tag
     * 
     * @param context the relevant Android Context
     * @param key the tag key
     * @param value the concatenated tag value
     * @param valueList list of individual values
     */
    protected static void updateTag(@NonNull Context context, @NonNull String key, @NonNull String value, @NonNull List<StringWithDescription> valueList) {
        TagFormFragment fragment = findFragment(context);
        if (fragment != null) {
            fragment.updateSingleValue(key, value);
            fragment.updateDialogRow(key, valueList);
        }
    }

    /**
     * Find the parent TagFormFragment and update multiple tags
     * 
     * This is just as ugly as above, but works reliably
     * 
     * @param context the relevant Android Context
     * @param key a String identifying the row to change
     * @param tags map containing the new key - value pairs
     * @param flush if true delete all existing tags before applying the update
     */
    protected static void updateTags(@NonNull Context context, @NonNull String key, @NonNull final Map<String, String> tags, final boolean flush) {
        TagFormFragment fragment = findFragment(context);
        if (fragment != null) {
            fragment.updateTags(tags, flush);
            fragment.updateDialogRow(key, tags);
        }
    }

    /**
     * Unwrap a Context till we find a FragmentActivity
     * 
     * @param context the Context
     * @return the unwrapped Context
     */
    @NonNull
    private static Context unwrap(@NonNull Context context) {
        while (!(context instanceof FragmentActivity)) {
            if (context instanceof ContextThemeWrapper) {
                context = ((ContextThemeWrapper) context).getBaseContext();
            } else if (context instanceof android.view.ContextThemeWrapper) {
                context = ((android.view.ContextThemeWrapper) context).getBaseContext();
            } else {
                break;
            }
        }
        return context;
    }
}
