package de.blau.android.propertyeditor.tagform;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.AppCompatRadioButton;
import de.blau.android.R;
import de.blau.android.contract.Ui;
import de.blau.android.presets.PresetComboField;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetTagField;
import de.blau.android.propertyeditor.tagform.TagFormFragment.Ruler;
import de.blau.android.util.SelectByImageFragment;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.StringWithDescriptionAndIcon;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.util.Value;

/**
 * Display a single value and allow editing via a dialog
 */
public class ComboDialogRow extends DialogRow {

    private static final String DEBUG_TAG = ComboDialogRow.class.getSimpleName().substring(0, Math.min(23, ComboDialogRow.class.getSimpleName().length()));

    private static final int DEBOUNCE_DELAY = 1000;

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
     * Scroll the view in the dialog to show the value
     * 
     * @param value the value we want to scroll to
     * @param dialog the enclosing dialog
     * @param containerId resource id of the ViewGroup containing the rows
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

                StringWithDescription swd = o instanceof StringWithDescriptionAndIcon ? new StringWithDescriptionAndIcon(o) : new StringWithDescription(o);
                String v = swd.getValue();

                if (v != null && !"".equals(v) && v.equals(value)) {
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
            row.setOnClickListener(v -> {
                v.setEnabled(false); // debounce
                v.postDelayed(() -> v.setEnabled(true), DEBOUNCE_DELAY);
                PresetTagField field = preset.getField(key);
                if (field instanceof PresetComboField && ((PresetComboField) field).useImages()) {
                    buildImageComboDialog(caller, key, adapter, row);
                } else {
                    final AlertDialog dialog = buildComboDialog(caller, hint != null ? hint : key, key, adapter, row, preset);
                    dialog.setOnShowListener(d -> {
                        if (finalSelectedValue != null) {
                            ComboDialogRow.scrollDialogToValue(finalSelectedValue, dialog, R.id.valueGroup);
                        }
                    });
                    dialog.show();
                }
            });
        }
        return row;
    }

    /**
     * Build a dialog for selecting a single value or none via a scrollable list of radio buttons
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
        Builder builder = ThemeUtils.getAlertDialogBuilder(caller.getActivity());
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(caller.getActivity());
        final View layout = themedInflater.inflate(R.layout.form_combo_dialog, null);
        final View divider = themedInflater.inflate(R.layout.divider2, null);
        divider.setLayoutParams(new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        RadioGroup valueGroup = (RadioGroup) layout.findViewById(R.id.valueGroup);
        builder.setView(layout);

        View.OnClickListener listener = v -> {
            Log.d(DEBUG_TAG, "radio button clicked " + row.getValue() + " " + v.getTag());
            if (!row.hasChanged()) {
                RadioGroup g = (RadioGroup) v.getParent();
                g.clearCheck();
            } else {
                row.setChanged(false);
            }
        };

        if (adapter != null) {
            addButtons(caller.getContext(), adapter, valueGroup, divider, preset, (context, i, swd, v, icon, buttonLayoutParams) -> addButton(context,
                    valueGroup, i, swd, v.equals(value), icon, listener, buttonLayoutParams));
        }
        builder.setPositiveButton(R.string.clear, (dialog, which) -> {
            View groupView = ((AlertDialog) dialog).findViewById(R.id.valueGroup);
            String k = (String) groupView.getTag();
            updateTag(((AlertDialog) dialog).getContext(), k, new StringWithDescription(""));
            // allow a tiny bit of time to see that the action actually worked
            groupView.postDelayed(dialog::dismiss, 100);
        });
        builder.setNegativeButton(R.string.cancel, null);
        final AlertDialog dialog = builder.create();
        valueGroup.setTag(key);
        valueGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(DEBUG_TAG, "radio group onCheckedChanged");
            StringWithDescription ourValue = null;
            if (checkedId != -1) {
                RadioButton button = (RadioButton) group.findViewById(checkedId);
                ourValue = (StringWithDescription) button.getTag();
                updateTag(button.getContext(), (String) group.getTag(), ourValue);
            }
            // allow a tiny bit of time to see that the action actually worked
            group.postDelayed(dialog::dismiss, 100);
        });

        return dialog;
    }

    interface AddValue {
        /**
         * Add button for a value
         * 
         * @param context an Android COntext
         * @param i the position
         * @param swd a StringWithDescription for the value
         * @param v the current value
         * @param icon an icon or null
         * @param buttonLayoutParams layout params
         */
        void add(@NonNull Context context, int i, @NonNull StringWithDescription swd, @NonNull String v, @Nullable Drawable icon,
                @NonNull android.view.ViewGroup.LayoutParams buttonLayoutParams);
    }

    /**
     * Common code for Combo and Multiselect to add buttons to the dialog layout
     * 
     * @param context an Android Context
     * @param adapter the Adapter holding the values for the buttons
     * @param valueGroup the layout we are adding the buttons too
     * @param divider divider View
     * @param preset the PresetItem
     * @param addValue callback to actually add the button
     */
    static void addButtons(@NonNull Context context, @NonNull Adapter adapter, @NonNull ViewGroup valueGroup, @NonNull View divider, @NonNull PresetItem preset,
            @NonNull AddValue addValue) {
        android.view.ViewGroup.LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
        buttonLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            Object o = adapter.getItem(i);
            if (o instanceof TagFormFragment.Ruler) {
                valueGroup.addView(divider);
            } else {
                Drawable icon = null;
                if (o instanceof StringWithDescriptionAndIcon) {
                    icon = ((StringWithDescriptionAndIcon) o).getIcon(context, preset);
                }
                StringWithDescription swd = new StringWithDescription(o);
                String v = swd.getValue();
                if (v == null || "".equals(v)) {
                    continue;
                }
                addValue.add(context, i, swd, v, icon, buttonLayoutParams);
            }
        }
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
     * @param listener the Listener to call if the button is clicked
     * @param layoutParams LayoutParams for the button
     */
    static void addButton(@NonNull Context context, @NonNull ViewGroup group, int id, @NonNull StringWithDescription swd, boolean selected,
            @Nullable Drawable icon, @NonNull View.OnClickListener listener, @NonNull ViewGroup.LayoutParams layoutParams) {
        final AppCompatRadioButton button = new AppCompatRadioButton(context);
        String description = swd.getDescription();
        button.setText(description != null && !"".equals(description) ? description : swd.getValue());
        button.setTag(swd);
        button.setChecked(selected);
        button.setId(id);
        if (icon != null) {
            Util.setCompoundDrawableWithIntrinsicBounds(Util.isRtlScript(context), button, icon);
            button.setCompoundDrawablePadding(Ui.COMPOUND_DRAWABLE_PADDING);
        }
        button.setLayoutParams(layoutParams);
        group.addView(button);
        button.setOnClickListener(listener);
    }

    /**
     * Build a dialog for selecting a single value or none via a scrollable list of images
     * 
     * @param caller the calling TagFormFragment instance
     * @param key the key
     * @param adapter the ArrayAdapter holding the values
     * @param row the row we are started from
     */
    private static void buildImageComboDialog(@NonNull final TagFormFragment caller, @NonNull final String key, @NonNull final ArrayAdapter<?> adapter,
            @NonNull final DialogRow row) {
        String value = row.getValue();

        ArrayList<String> images = new ArrayList<>();
        final List<Value> values = new ArrayList<>();
        // Note we can't use the adapter directly in the ImageLoader as it will potentially be serialized
        // Start with 1 to avoid empty entry
        for (int i = 1; i < adapter.getCount(); i++) {
            Object o = adapter.getItem(i);
            if (o instanceof Ruler) {
                continue;
            }
            final Value aValue = o instanceof Value ? ((Value) o) : new StringWithDescription((String) o);
            values.add(aValue);
            if (o instanceof StringWithDescriptionAndIcon && ((StringWithDescriptionAndIcon) o).hasImagePath()) {
                images.add(((StringWithDescriptionAndIcon) o).getImagePath());
            } else {
                images.add("");
            }
        }
        int pos = 0;
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).getValue().equals(value)) {
                pos = i;
                break;
            }
        }
        SelectByImageFragment.showDialog(caller, images, pos, new ComboImageLoader(key, values));
    }
}
