package de.blau.android.propertyeditor.tagform;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowInsetsCompat;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.Ui;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetItem;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Row that displays multiselect values and allows changing them via a dialog
 */
public class MultiselectDialogRow extends DialogRow {

    OnClickListener listener;

    LinearLayout         valueList;
    final LayoutInflater inflater;

    /**
     * Construct a row that will show a dialog that allows multiple values to be selected when clicked
     * 
     * @param context Android Context
     */
    public MultiselectDialogRow(@NonNull Context context) {
        super(context);
        inflater = LayoutInflater.from(context);
    }

    /**
     * Construct a row that will show a dialog that allows multiple values to be selected when clicked
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public MultiselectDialogRow(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflater = LayoutInflater.from(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        valueList = (LinearLayout) findViewById(R.id.valueList);
    }

    /**
     * Set the OnClickListener for every value
     */
    @Override
    public void setOnClickListener(final OnClickListener listener) {
        this.listener = listener;
        for (int pos = 0; pos < valueList.getChildCount(); pos++) {
            View v = valueList.getChildAt(pos);
            if (v instanceof TextView) {
                v.setOnClickListener(listener);
            }
        }
    }

    /**
     * Add additional description values as individual TextViews
     * 
     * @param values the List of values
     */
    public void setValue(List<StringWithDescription> values) {
        String value = "";
        char delimiter = preset.getDelimiter(getKey());
        de.blau.android.propertyeditor.Util.resetValueLayout(valueList, () -> setValue("", ""));
        boolean first = true;
        StringBuilder builder = new StringBuilder(value);
        for (StringWithDescription swd : values) {
            String d = swd.getDescription();
            if (first) {
                setValue(swd.getValue(), d != null && !"".equals(d) ? d : swd.getValue());
                first = false;
            } else {
                TextView extraValue = (TextView) inflater.inflate(R.layout.form_dialog_multiselect_value, valueList, false);
                extraValue.setText(d != null && !"".equals(d) ? d : swd.getValue());
                extraValue.setTag(swd.getValue());
                valueList.addView(extraValue);
            }
            // collect the individual values for what we actually store
            if (builder.length() != 0) {
                builder.append(delimiter);
            }
            builder.append(swd.getValue());
        }
        setValueOnly(builder.toString()); // don't use setValue as that does a lot more
        setOnClickListener(listener);
    }

    /**
     * Add a row that displays a dialog for selecting multi-values when clicked
     * 
     * @param caller the calling TagFormFragment instance
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the roes
     * @param preset the relevant PresetItem
     * @param hint a description of the value to display
     * @param key the key
     * @param values the current values
     * @param adapter an ArrayAdapter with the selectable values
     * @return an instance of TagFormMultiselectDialogRow
     */
    static MultiselectDialogRow getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @NonNull LinearLayout rowLayout,
            @NonNull final PresetItem preset, @Nullable final String hint, @NonNull final String key, @Nullable final List<String> values,
            @Nullable final ArrayAdapter<?> adapter) {
        final MultiselectDialogRow row = (MultiselectDialogRow) inflater.inflate(R.layout.tag_form_multiselect_dialog_row, rowLayout, false);
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        row.setPreset(preset);

        if (adapter != null) {
            ArrayList<StringWithDescription> selectedValues = new ArrayList<>();
            for (int i = 0; i < adapter.getCount(); i++) {
                Object o = adapter.getItem(i);

                StringWithDescription swd = new StringWithDescription(o);
                String v = swd.getValue();

                if (v == null || "".equals(v)) {
                    continue;
                }
                if (values != null) {
                    for (String m : values) {
                        if (v.equals(m)) {
                            selectedValues.add(swd);
                            break;
                        }
                    }
                }
            }
            row.setValue(selectedValues);
            row.valueView.setHint(R.string.tag_dialog_value_hint);
            row.setOnClickListener(new ShowDialogOnClickListener() {
                @Override
                public AlertDialog buildDialog() {
                    return buildMultiselectDialog(caller, hint != null ? hint : key, key, adapter, row, preset);
                }
            });
        }
        return row;
    }

    /**
     * Build a dialog that allows multiple values to be selected
     * 
     * @param caller the calling TagFormFragment instance
     * @param hint a description to display
     * @param key the key
     * @param adapter the ArrayAdapter holding the values
     * @param row the row we are started from
     * @param preset the relevant PresetItem
     * @return an AlertDialog
     */
    private static AlertDialog buildMultiselectDialog(@NonNull final TagFormFragment caller, @NonNull String hint, @NonNull String key,
            @Nullable ArrayAdapter<?> adapter, @NonNull final MultiselectDialogRow row, @NonNull final PresetItem preset) {
        Builder builder = ThemeUtils.getAlertDialogBuilder(caller.getActivity());
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(caller.getActivity());

        final View layout = themedInflater.inflate(R.layout.form_multiselect_dialog, null);
        final LinearLayout valueGroup = (LinearLayout) layout.findViewById(R.id.valueGroup);
        final View divider = themedInflater.inflate(R.layout.divider2, null);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        builder.setView(layout);

        if (adapter != null) {
            List<String> values = Preset.splitValues(Util.wrapInList(row.getValue()), preset, key);
            ComboDialogRow.addButtons(caller.getContext(), adapter, valueGroup, divider, preset, (context, i, swd, v, icon,
                    buttonLayoutParams) -> addCheck(context, valueGroup, swd, values != null && values.contains(v), icon, buttonLayoutParams));
        }
        builder.setNeutralButton(R.string.clear, (dialog, iwhich) -> {
            // do nothing
        });
        valueGroup.setTag(new String[] { key, String.valueOf(preset.getDelimiter(key)) });
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String[] kd = (String[]) ((AlertDialog) dialog).findViewById(R.id.valueGroup).getTag();
            String delimiter = kd[1];
            List<StringWithDescription> valueList = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();
            int childCount = valueGroup.getChildCount();
            for (int pos = 0; pos < childCount; pos++) {
                View c = valueGroup.getChildAt(pos);
                if (c instanceof AppCompatCheckBox) {
                    AppCompatCheckBox checkBox = (AppCompatCheckBox) c;
                    if (checkBox.isChecked()) {
                        if (stringBuilder.length() != 0) {
                            stringBuilder.append(delimiter);
                        }
                        final StringWithDescription swd = (StringWithDescription) checkBox.getTag();
                        stringBuilder.append(swd.getValue());
                        valueList.add(swd);
                    }
                }
            }
            updateTag(((AlertDialog) dialog).getContext(), kd[0], stringBuilder.toString(), valueList);
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
     */
    private static void addCheck(@NonNull Context context, @NonNull ViewGroup layout, @NonNull StringWithDescription swd, boolean selected,
            @Nullable Drawable icon, @NonNull ViewGroup.LayoutParams layoutParams) {
        final AppCompatCheckBox check = new AppCompatCheckBox(context);
        String description = swd.getDescription();
        check.setText(description != null && !"".equals(description) ? description : swd.getValue());
        check.setTag(swd);
        if (icon != null) {
            Util.setCompoundDrawableWithIntrinsicBounds(Util.isRtlScript(context), check, icon);
            check.setCompoundDrawablePadding(Ui.COMPOUND_DRAWABLE_PADDING);
        }
        check.setLayoutParams(layoutParams);
        check.setChecked(selected);
        layout.addView(check);
    }
}
