package de.blau.android.propertyeditor.tagform;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.buildware.widget.indeterm.IndeterminateCheckBox;
import com.buildware.widget.indeterm.IndeterminateCheckBox.OnStateChangedListener;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
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
import de.blau.android.R;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetCheckField;
import de.blau.android.presets.PresetCheckGroupField;
import de.blau.android.presets.PresetField;
import de.blau.android.propertyeditor.tagform.TagFormFragment.EditableLayout;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.views.TriStateCheckBox;

/**
 * Row that displays checkgroup keys and values and allows changing them via a dialog
 */
public class CheckGroupDialogRow extends MultiselectDialogRow {

    private static final String DEBUG_TAG = "CheckGroupDialogRow";

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
     * Add additional description values as individual TextViews
     * 
     * @param keyValues a Map of the current keys and values for this check group
     */
    public void setSelectedValues(@NonNull Map<String, String> keyValues) {
        this.keyValues = keyValues;
        int childCount = valueList.getChildCount();
        for (int pos = 0; pos < childCount; pos++) { // don^t delete first child, just clear
            if (pos == 0) {
                setValue("", "");
            } else {
                valueList.removeViewAt(1);
            }
        }

        boolean first = true;
        PresetCheckGroupField field = null;

        for (Entry<String, String> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (field == null) {
                PresetField f = preset.getField(key);
                if (!(f instanceof PresetCheckGroupField)) {
                    return;
                }
                field = (PresetCheckGroupField) f;
            }
            PresetCheckField check = field.getCheckField(key);
            if (check != null && !"".equals(value)) {
                String d = check.getHint();
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
        final String key = field.getKey();
        if (rowLayout != null && keyValues != null) {
            if (field.size() <= caller.maxInlineValues) {
                String hint = preset.getHint(key);
                if (hint == null) { // simply display as individual checks
                    for (PresetCheckField check : field.getCheckFields()) {
                        caller.addRow(rowLayout, check, keyValues.get(check.getKey()), preset, allTags);
                    }
                } else {
                    final CheckGroupRow row = (CheckGroupRow) inflater.inflate(R.layout.tag_form_checkgroup_row, rowLayout, false);
                    row.getKeyView().setText(hint);
                    row.getKeyView().setTag(key);
                    OnStateChangedListener onStateChangeListener = new OnStateChangedListener() {
                        @Override
                        public void onStateChanged(IndeterminateCheckBox checkBox, Boolean state) {
                            PresetCheckField check = (PresetCheckField) checkBox.getTag();
                            String checkKey = check.getKey();
                            if (state == null) {
                                caller.tagListener.updateSingleValue(checkKey, "");
                                keyValues.put(checkKey, "");
                            } else if (!checkBox.isEnabled()) {
                                // unknown stuff
                                keyValues.put(checkKey, keyValues.get(checkKey));
                            } else if (state) { // NOSONAR state can't be null here
                                caller.tagListener.updateSingleValue(checkKey, check.getOnValue().getValue());
                                keyValues.put(checkKey, check.getOnValue().getValue());
                            } else {
                                StringWithDescription offValue = check.getOffValue();
                                caller.tagListener.updateSingleValue(checkKey, offValue == null ? "" : offValue.getValue());
                                keyValues.put(checkKey, offValue == null ? "" : offValue.getValue());
                            }
                            if (rowLayout instanceof EditableLayout) {
                                ((EditableLayout) rowLayout).putTag(checkKey, keyValues.get(checkKey));
                            }
                        }
                    };

                    for (PresetCheckField check : field.getCheckFields()) {
                        String checkKey = check.getKey();
                        String checkValue = keyValues.get(checkKey);
                        Boolean state = null;

                        boolean selected = checkValue != null && checkValue.equals(check.getOnValue().getValue());
                        boolean off = check.isOffValue(checkValue);

                        String d = check.getHint();
                        if (checkValue == null || "".equals(checkValue) || selected || off) {
                            if (selected || off || check.getOffValue() == null) {
                                state = selected;
                            }
                            TriStateCheckBox checkBox = row.addCheck(d == null ? checkKey : d, state, onStateChangeListener);
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
            } else {
                final CheckGroupDialogRow row = (CheckGroupDialogRow) inflater.inflate(R.layout.tag_form_checkgroup_dialog_row, rowLayout, false);

                String tempHint = preset.getHint(key);
                if (tempHint == null) {
                    // fudge something
                    tempHint = caller.getString(R.string.ugly_checkgroup_hint, field.getCheckFields().get(0).getHint());
                }
                final String hint = tempHint;
                row.keyView.setText(hint);
                row.keyView.setTag(key);
                row.setPreset(preset);
                row.setSelectedValues(keyValues);
                row.valueView.setHint(R.string.tag_dialog_value_hint);
                row.setOnClickListener(new ShowDialogOnClickListener() {
                    @Override
                    public AlertDialog buildDialog() {
                        return buildCheckGroupDialog(caller, hint, key, row, preset);
                    }
                });
                rowLayout.addView(row);
            }
        } else {
            Log.e(DEBUG_TAG, "addRow rowLayout " + rowLayout + " keyValues " + keyValues + " for " + field.getKey());
        }
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
        Builder builder = new AlertDialog.Builder(caller.getActivity());
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(caller.getActivity());

        final View layout = themedInflater.inflate(R.layout.form_multiselect_dialog, null);
        final LinearLayout valueGroup = (LinearLayout) layout.findViewById(R.id.valueGroup);
        builder.setView(layout);

        android.view.ViewGroup.LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
        buttonLayoutParams.width = LayoutParams.MATCH_PARENT;

        layout.setTag(key);
        PresetCheckGroupField field = (PresetCheckGroupField) preset.getField(key);

        for (PresetCheckField check : field.getCheckFields()) {
            String checkKey = check.getKey();
            String checkValue = row.keyValues.get(checkKey);
            boolean selected = checkValue != null && checkValue.equals(check.getOnValue().getValue());
            boolean off = checkValue == null || "".equals(checkValue) || check.isOffValue(checkValue);
            if (selected || off) {
                addTriStateCheck(caller.getActivity(), valueGroup, new StringWithDescription(checkKey, check.getHint()), selected, null, buttonLayoutParams);
            } else {
                // unknown value: add non-editable checkbox
                TriStateCheckBox checkBox = addTriStateCheck(caller.getActivity(), valueGroup, new StringWithDescription(checkKey, checkKey + "=" + checkValue),
                        false, null, buttonLayoutParams);
                checkBox.setEnabled(false);
            }
        }

        builder.setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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
                caller.tagListener.updateTags(ourKeyValues, false); // batch update
                row.setSelectedValues(ourKeyValues);
                row.setChanged(true);
            }
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
     * @param selected if true the CheckBox will be selected
     * @param icon an icon if there is one
     * @param layoutParams the LayoutParams for the CheckBox
     * @return the CheckBox for further use
     */
    private static TriStateCheckBox addTriStateCheck(@NonNull Context context, @NonNull LinearLayout layout, @NonNull StringWithDescription swd,
            boolean selected, @Nullable Drawable icon, @NonNull ViewGroup.LayoutParams layoutParams) {
        final TriStateCheckBox check = new TriStateCheckBox(context);
        String description = swd.getDescription();
        check.setText(description != null && !"".equals(description) ? description : swd.getValue());
        check.setTag(swd);
        if (icon != null) {
            check.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }
        check.setLayoutParams(layoutParams);
        check.setChecked(selected);
        layout.addView(check);
        return check;
    }
}
