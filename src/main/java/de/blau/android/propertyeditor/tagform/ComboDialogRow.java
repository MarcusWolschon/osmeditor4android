package de.blau.android.propertyeditor.tagform;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.AppCompatRadioButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import de.blau.android.R;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.StringWithDescriptionAndIcon;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Display a single value and allow editing via a dialog
 */
public class ComboDialogRow extends DialogRow {

    private static final String DEBUG_TAG = "ComboDialogRow";

    /**
     * Construct a row that will display a Dialog when clicked
     * 
     * @param context Android Context
     */
    public ComboDialogRow(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct a row that will display a Dialog when clicked
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public ComboDialogRow(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Scroll the view in the dialog to show the value, assumes the ScrollView has id R.id.myScrollView
     * 
     * @param value the value we want to scroll to
     * @param dialog the enclosing dialog
     * @param containerId ?
     */
    static void scrollDialogToValue(String value, AlertDialog dialog, int containerId) {
        Log.d(DEBUG_TAG, "scrollDialogToValue scrolling to " + value);
        final View sv = dialog.findViewById(R.id.myScrollView);
        if (sv != null) {
            ViewGroup container = (ViewGroup) dialog.findViewById(containerId);
            if (container != null) {
                for (int pos = 0; pos < container.getChildCount(); pos++) {
                    View child = container.getChildAt(pos);
                    Object tag = child.getTag();
                    if (tag instanceof StringWithDescription && ((StringWithDescription) tag).equals(value)) {
                        Util.scrollToRow(sv, child, true, true);
                        return;
                    }
                }
            } else {
                Log.d(DEBUG_TAG, "scrollDialogToValue container view null");
            }
        } else {
            Log.d(DEBUG_TAG, "scrollDialogToValue scroll view null");
        }
    }

    /**
     * Add a row that displays a dialog for selecting a single when clicked
     * 
     * @param caller the calling TagFormFragment instance
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the roes
     * @param preset the relevant PresetItem
     * @param hint a description of the value to display
     * @param key the key
     * @param value the current value
     * @param adapter an ArrayAdapter with the selectable values
     * @return an instance of TagFormDialogRow
     */
    static DialogRow getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout,
            @NonNull final PresetItem preset, @Nullable final String hint, @NonNull final String key, @NonNull final String value,
            @Nullable final ArrayAdapter<?> adapter) {
        final DialogRow row = (DialogRow) inflater.inflate(R.layout.tag_form_combo_dialog_row, rowLayout, false);
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        row.setPreset(preset);
        if (adapter != null) {
            String selectedValue = null;
            for (int i = 0; i < adapter.getCount(); i++) {
                Object o = adapter.getItem(i);

                StringWithDescription swd;
                if (o instanceof StringWithDescriptionAndIcon) {
                    swd = new StringWithDescriptionAndIcon(o);
                } else {
                    swd = new StringWithDescription(o);
                }
                String v = swd.getValue();
                String description = swd.getDescription();

                if (v == null || "".equals(v)) {
                    continue;
                }
                if (description == null) {
                    description = v;
                }
                if (v.equals(value)) {
                    row.setValue(swd);
                    selectedValue = v;
                    break;
                }
            }
            row.valueView.setHint(R.string.tag_dialog_value_hint);
            final String finalSelectedValue;
            if (selectedValue != null) {
                finalSelectedValue = selectedValue;
            } else {
                finalSelectedValue = null;
            }
            row.setOnClickListener(new OnClickListener() {
                @SuppressLint("NewApi")
                @Override
                public void onClick(View v) {
                    final View finalView = v;
                    finalView.setEnabled(false); // debounce
                    final AlertDialog dialog = buildComboDialog(caller, hint != null ? hint : key, key, adapter, row, preset);
                    dialog.setOnShowListener(new OnShowListener() {
                        @Override
                        public void onShow(DialogInterface d) {
                            if (finalSelectedValue != null) {
                                ComboDialogRow.scrollDialogToValue(finalSelectedValue, dialog, R.id.valueGroup);
                            }
                        }
                    });
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finalView.setEnabled(true);
                        }
                    });
                    dialog.show();
                }
            });
        }
        return row;
    }

    /**
     * Build a dialog for selecting a single value of none via a scrollable list of radio buttons
     * 
     * @param caller the calling TagFormFragment instance
     * @param hint a description to display
     * @param key the key
     * @param adapter the ArrayAdapter holding the values
     * @param row the row we are started from
     * @param preset the relevant PresetItem
     * @return an AlertDialog
     */
    private static AlertDialog buildComboDialog(@NonNull final TagFormFragment caller, @NonNull String hint, @NonNull String key,
            @Nullable final ArrayAdapter<?> adapter, @NonNull final DialogRow row, @NonNull final PresetItem preset) {
        String value = row.getValue();
        Builder builder = new AlertDialog.Builder(caller.getActivity());
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(caller.getActivity());

        final View layout = themedInflater.inflate(R.layout.form_combo_dialog, null);
        RadioGroup valueGroup = (RadioGroup) layout.findViewById(R.id.valueGroup);
        builder.setView(layout);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(DEBUG_TAG, "radio button clicked " + row.getValue() + " " + v.getTag());
                if (!row.hasChanged()) {
                    RadioGroup g = (RadioGroup) v.getParent();
                    g.clearCheck();
                } else {
                    row.setChanged(false);
                }
            }
        };

        android.view.ViewGroup.LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
        buttonLayoutParams.width = LayoutParams.MATCH_PARENT;

        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                Object o = adapter.getItem(i);
                StringWithDescription swd;
                Drawable icon = null;
                if (o instanceof StringWithDescriptionAndIcon) {
                    icon = ((StringWithDescriptionAndIcon) o).getIcon(preset);
                    if (icon != null) {
                        swd = new StringWithDescriptionAndIcon(o);
                    } else {
                        swd = new StringWithDescription(o);
                    }
                } else {
                    swd = new StringWithDescription(o);
                }
                String v = swd.getValue();

                if (v == null || "".equals(v)) {
                    continue;
                }
                addButton(caller.getActivity(), valueGroup, i, swd, v.equals(value), icon, listener, buttonLayoutParams);
            }
        }
        final Handler handler = new Handler();
        builder.setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                caller.tagListener.updateSingleValue((String) layout.getTag(), "");
                row.setValue("", "");
                row.setChanged(true);
                final DialogInterface finalDialog = dialog;
                // allow a tiny bit of time to see that the action actually worked
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finalDialog.dismiss();
                    }
                }, 100);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        final AlertDialog dialog = builder.create();
        layout.setTag(key);
        valueGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(DEBUG_TAG, "radio group onCheckedChanged");
                StringWithDescription ourValue = null;
                if (checkedId != -1) {
                    RadioButton button = (RadioButton) group.findViewById(checkedId);
                    ourValue = (StringWithDescription) button.getTag();
                    caller.tagListener.updateSingleValue((String) layout.getTag(), ourValue.getValue());
                    row.setValue(ourValue);
                    row.setChanged(true);
                }
                // allow a tiny bit of time to see that the action actually worked
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                }, 100);
            }
        });

        return dialog;
    }

    /**
     * Add a button to a RadioGroup
     * 
     * @param context Android Context
     * @param group the RadioGroup we ant to add the button to
     * @param id an id for the button
     * @param swd the value for the button
     * @param selected is true the button is selected
     * @param icon an icon to display if any
     * @param listener the Listenet to call if the button is clicked
     * @param layoutParams LayoutParams for the button
     */
    private static void addButton(@NonNull Context context, @NonNull RadioGroup group, int id, @NonNull StringWithDescription swd, boolean selected,
            @Nullable Drawable icon, @NonNull View.OnClickListener listener, @NonNull ViewGroup.LayoutParams layoutParams) {
        final AppCompatRadioButton button = new AppCompatRadioButton(context);
        String description = swd.getDescription();
        button.setText(description != null && !"".equals(description) ? description : swd.getValue());
        button.setTag(swd);
        button.setChecked(selected);
        button.setId(id);
        if (icon != null) {
            button.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }
        button.setLayoutParams(layoutParams);
        group.addView(button);
        button.setOnClickListener(listener);
    }
}
