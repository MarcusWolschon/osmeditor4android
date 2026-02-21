package de.blau.android.propertyeditor.tagform;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import ch.poole.android.checkbox.IndeterminateCheckBox.OnStateChangedListener;
import de.blau.android.R;
import de.blau.android.presets.PresetCheckField;
import de.blau.android.presets.PresetCheckGroupField;
import de.blau.android.presets.PresetItem;
import de.blau.android.propertyeditor.tagform.TagFormFragment.EditableLayout;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.views.TriStateCheckBox;

/**
 * Row that displays checkgroup keys and values and allows changing them via a dialog
 */
public class CheckGroupDialogRow extends MultiselectDialogRow {

    private static final String DEBUG_TAG = CheckGroupDialogRow.class.getSimpleName().substring(0,
            Math.min(23, CheckGroupDialogRow.class.getSimpleName().length()));

    Map<String, String> keyValues;

    /**
     * Construct a row that will show a dialog that allows multiple values to be selected when clicked
     * 
     * @param context Android Context
     */
    public CheckGroupDialogRow(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct a row that will show a dialog that allows multiple values that represent to be selected when clicked
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public CheckGroupDialogRow(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Set the selected check values
     * 
     * @param context an Android Context
     * @param keyValues a Map of the current keys and values for this check group
     */
    public void setSelectedValues(@NonNull Context context, @NonNull Map<String, String> keyValues) {
        this.keyValues = keyValues;
        de.blau.android.propertyeditor.Util.resetValueLayout(valueList, () -> setValue("", ""));

        boolean first = true;
        PresetCheckGroupField field = null;

        for (Entry<String, String> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (field == null) {
                field = preset.getCheckGroupField(key);
                if (field == null) {
                    Log.e(DEBUG_TAG, key + " isn't in a checkgroup");
                    continue;
                }
            }
            PresetCheckField check = field.getCheckField(key);
            if (check != null && !"".equals(value)) {
                String d = check.isDeprecated() ? context.getString(R.string.deprecated, check.getHint()) : check.getHint();
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

        // this is a hack around ugly layouts if there is only one value and the key text wraps
        ViewGroup.LayoutParams layoutParams = getValueView().getLayoutParams();
        layoutParams.height = valueList.getChildCount() <= 1 ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        getValueView().setLayoutParams(layoutParams);

        setOnClickListener(listener);
    }

    private static final StrikethroughSpan STRIKE_THROUGH_SPAN = new StrikethroughSpan();

    /**
     * Strike through some text in a TextView
     * 
     * @param tv TextView to use
     */
    private void strikeThrough(@NonNull TextView tv) {
        Spannable spannable = new SpannableString(tv.getText());
        spannable.setSpan(STRIKE_THROUGH_SPAN, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(spannable);
    }

    /**
     * Add a row for a PresetCheckGroupField
     * 
     * Depending on the number of entries and if a hint is set or not, different layout variants will be used
     * 
     * @param caller the calling TagFormFragment instance
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the rows
     * @param field the PresetField for this row
     * @param keyValues a Map containing existing keys and corresponding values
     * @param preset the Preset we believe the key belongs to
     * @param allTags the other tags for this object
     */
    static void getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @Nullable final LinearLayout rowLayout,
            @NonNull final PresetCheckGroupField field, @Nullable final Map<String, String> keyValues, @NonNull PresetItem preset,
            Map<String, String> allTags) {
        final Context context = caller.getContext();
        final String key = field.getKey();
        if (rowLayout == null || keyValues == null) {
            Log.e(DEBUG_TAG, "addRow rowLayout " + rowLayout + " keyValues " + keyValues + " for " + field.getKey());
            return;
        }
        if (field.size() > caller.maxInlineValues) {
            final CheckGroupDialogRow row = (CheckGroupDialogRow) inflater.inflate(R.layout.tag_form_checkgroup_dialog_row, rowLayout, false);

            String tempHint = preset.getHint(key);
            if (tempHint == null) {
                // fudge something
                tempHint = caller.getString(R.string.default_checkgroup_hint);
            }
            final String hint = tempHint;
            row.keyView.setText(hint);
            row.keyView.setTag(key);
            row.setPreset(preset);
            row.setSelectedValues(context, keyValues);
            row.valueView.setHint(R.string.tag_dialog_value_hint);
            row.setOnClickListener(new ShowDialogOnClickListener() {
                @Override
                public AlertDialog buildDialog() {
                    return buildCheckGroupDialog(caller, hint, key, row, preset);
                }
            });
            rowLayout.addView(row);
            return;
        }
        String hint = preset.getHint(key);
        if (hint == null) { // simply display as individual checks
            for (PresetCheckField check : field.getCheckFields()) {
                caller.addRow(rowLayout, check, keyValues.get(check.getKey()), preset, allTags);
            }
        } else {
            final CheckGroupRow row = (CheckGroupRow) inflater.inflate(R.layout.tag_form_checkgroup_row, rowLayout, false);
            row.getKeyView().setText(hint);
            row.getKeyView().setTag(key);
            OnStateChangedListener onStateChangeListener = (checkBox, state) -> {
                PresetCheckField check = (PresetCheckField) checkBox.getTag();
                String checkKey = check.getKey();
                if (state == null) {
                    caller.updateSingleValue(checkKey, "");
                    keyValues.put(checkKey, "");
                } else if (!checkBox.isEnabled()) {
                    // unknown stuff
                    keyValues.put(checkKey, keyValues.get(checkKey));
                } else if (state) { // NOSONAR state can't be null here
                    caller.updateSingleValue(checkKey, check.getOnValue().getValue());
                    keyValues.put(checkKey, check.getOnValue().getValue());
                } else {
                    StringWithDescription offValue = check.getOffValue();
                    caller.updateSingleValue(checkKey, offValue == null ? "" : offValue.getValue());
                    keyValues.put(checkKey, offValue == null ? "" : offValue.getValue());
                }
                if (rowLayout instanceof EditableLayout) {
                    ((EditableLayout) rowLayout).putTag(checkKey, keyValues.get(checkKey));
                }
            };

            for (PresetCheckField check : field.getCheckFields()) {
                String checkKey = check.getKey();
                String checkValue = keyValues.get(checkKey);
                String checkHint = check.getHint();
                if (check.isDeprecated()) {
                    if (checkValue == null) {
                        continue;
                    }
                    checkHint = context.getString(R.string.deprecated, checkHint);
                }

                Boolean state = getState(check, checkValue);

                if (checkHint != null && !"".equals(checkHint)) {
                    TriStateCheckBox checkBox = row.addCheck(checkHint == null ? checkKey : checkHint, state, onStateChangeListener);
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
    }

    /**
     * Determine the state a tristate check box should be in
     * 
     * @param check the field
     * @param checkValue the value
     * @return a Boolean or null
     */
    @Nullable
    public static Boolean getState(@NonNull PresetCheckField check, @Nullable String checkValue) {
        Boolean state = null;
        boolean selected = checkValue != null && checkValue.equals(check.getOnValue().getValue());
        boolean off = check.isOffValue(checkValue);
        if (selected || off || check.getOffValue() == null) {
            state = selected;
        }
        return state;
    }

    /**
     * Build a dialog that allows multiple PresetCheckFields to be checked etc
     * 
     * @param caller the calling TagFormFragment instance
     * @param hint a description to display
     * @param key the key for the PresetCHeckGroupField
     * @param row the row we are started from
     * @param preset the relevant PresetItem
     * @return an AlertDialog
     */
    private static AlertDialog buildCheckGroupDialog(@NonNull final TagFormFragment caller, @NonNull String hint, @NonNull String key,
            @NonNull final CheckGroupDialogRow row, @NonNull final PresetItem preset) {
        Builder builder = ThemeUtils.getAlertDialogBuilder(caller.getActivity());
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(caller.getActivity());

        final View layout = themedInflater.inflate(R.layout.form_multiselect_dialog, null);
        final LinearLayout valueGroup = (LinearLayout) layout.findViewById(R.id.valueGroup);
        builder.setView(layout);

        android.view.ViewGroup.LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
        buttonLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;

        PresetCheckGroupField field = (PresetCheckGroupField) preset.getField(key);

        for (PresetCheckField check : field.getCheckFields()) {
            String checkKey = check.getKey();
            String checkValue = row.keyValues.get(checkKey);
            String checkHint = check.getHint();
            if (check.isDeprecated()) {
                if (checkValue == null) {
                    continue;
                }
                checkHint = caller.getContext().getString(R.string.deprecated, checkHint);
            }
            Boolean state = getState(check, checkValue);

            if (checkHint != null && !"".equals(checkHint)) {
                addTriStateCheck(caller.getActivity(), valueGroup, new StringWithDescription(checkKey, checkHint), state, null, buttonLayoutParams);
            } else {
                // unknown value: add non-editable checkbox
                TriStateCheckBox checkBox = addTriStateCheck(caller.getActivity(), valueGroup, new StringWithDescription(checkKey, checkKey + "=" + checkValue),
                        state, null, buttonLayoutParams);
                checkBox.setEnabled(false);
            }
        }

        builder.setNeutralButton(R.string.clear, (dialog, which) -> {
            // do nothing
        });
        valueGroup.setTag(key);
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            Map<String, String> ourKeyValues = new HashMap<>();
            for (int pos = 0; pos < valueGroup.getChildCount(); pos++) {
                View c = valueGroup.getChildAt(pos);
                if (c instanceof TriStateCheckBox) {
                    TriStateCheckBox checkBox = (TriStateCheckBox) c;
                    String k = ((StringWithDescription) checkBox.getTag()).getValue();
                    PresetCheckField check = field.getCheckField(k);
                    Boolean state = checkBox.getState();
                    if (state == null) {
                        ourKeyValues.put(k, "");
                    } else if (!checkBox.isEnabled()) {
                        // unknown stuff
                        ourKeyValues.put(k, row.keyValues.get(k));
                    } else if (state) { // NOSONAR state can't be null here
                        ourKeyValues.put(k, check.getOnValue().getValue());
                    } else {
                        StringWithDescription offValue = check.getOffValue();
                        ourKeyValues.put(k, offValue == null ? "" : offValue.getValue());
                    }
                }
            }
            String k = (String) ((AlertDialog) dialog).findViewById(R.id.valueGroup).getTag();
            updateTags(((AlertDialog) dialog).getContext(), k, ourKeyValues, false); // batch update
        });
        builder.setNegativeButton(R.string.cancel, null);
        return builder.create();
    }

    /**
     * Add a CheckBox to a Layout
     * 
     * @param context Android Context
     * @param layout the Layout we want to add the CheckBox to
     * @param swd the value
     * @param selected if true the CheckBox will be selected, null sets indeterminate
     * @param icon an icon if there is one
     * @param layoutParams the LayoutParams for the CheckBox
     * @return the CheckBox for further use
     */
    private static TriStateCheckBox addTriStateCheck(@NonNull Context context, @NonNull LinearLayout layout, @NonNull StringWithDescription swd,
            Boolean selected, @Nullable Drawable icon, @NonNull ViewGroup.LayoutParams layoutParams) {
        final TriStateCheckBox check = new TriStateCheckBox(context);
        String description = swd.getDescription();
        check.setText(description != null && !"".equals(description) ? description : swd.getValue());
        check.setTag(swd);
        if (icon != null) {
            Util.setCompoundDrawableWithIntrinsicBounds(Util.isRtlScript(context), check, icon);
        }
        check.setLayoutParams(layoutParams);
        check.setState(selected);
        layout.addView(check);
        return check;
    }
}
