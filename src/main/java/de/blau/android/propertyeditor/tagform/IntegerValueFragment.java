package de.blau.android.propertyeditor.tagform;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.presets.PresetComboField;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetTagField;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;

public class IntegerValueFragment extends ValueWidgetFragment {

    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, IntegerValueFragment.class.getSimpleName().length());
    protected static final String DEBUG_TAG = IntegerValueFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "INTEGER_FRAGMENT";

    private int offset;

    /**
     * Show a dialog for adding/editing an integer value
     * 
     * @param caller calling Fragment
     * @param hint description of the key
     * @param key the key
     * @param value the existing value
     * @param values any additional values from the preset or mru
     * @param preset the preset item or null
     * @param allTags all current tags
     */
    public static void show(@NonNull Fragment caller, @NonNull String hint, @NonNull String key, @Nullable String value, @Nullable List<String> values,
            @Nullable PresetItem preset, @NonNull Map<String, String> allTags) {
        FragmentManager fm = caller.getChildFragmentManager();
        final PresetGroup rootGroup = App.getCurrentRootPreset(caller.getContext()).getRootGroup();
        IntegerValueFragment df = ValueWidgetFragment.setArguments(new IntegerValueFragment(), hint, key, value, values,
                preset != null && rootGroup != null ? preset.getPath(rootGroup) : null, allTags);
        de.blau.android.propertyeditor.Util.removeChildFragment(fm, TAG);
        df.show(fm, TAG);
    }

    @NonNull
    @Override
    ValueWidget getWidget(@NonNull FragmentActivity activity, @NonNull String value, @Nullable List<String> values) {
        return new IntegerWidget(activity, value, values);
    }

    class IntegerWidget implements ValueWidget {
        private static final int MAX_INT = 256;

        private final NumberPicker picker;

        final Set<Integer> values = new HashSet<>();

        /**
         * Construct a new widget
         * 
         * @param activity current FragmentActivity
         * @param value initial value
         * @param values any additional values from the preset or MRU
         */
        IntegerWidget(@NonNull FragmentActivity activity, @NonNull String value, @Nullable List<String> values) {
            picker = new NumberPicker(activity, null);
            int v = 0;
            try {
                v = Integer.parseInt(value);
            } catch (NumberFormatException nfex) {
                // do nothing
            }

            List<Integer> fieldInts = new ArrayList<>();
            fieldInts.add(v);

            PresetTagField field = preset.getField(key);
            if (field instanceof PresetComboField) {
                StringWithDescription[] fieldValues = ((PresetComboField) field).getValues();
                if (fieldValues != null) {
                    for (StringWithDescription s : fieldValues) {
                        addIntToList(fieldInts, s.getValue());
                    }
                }
            }

            if (values != null) {
                for (String val : values) {
                    addIntToList(fieldInts, val);
                }
            }

            Collections.sort(fieldInts, Integer::compare);

            boolean neg = fieldInts.isEmpty() || fieldInts.get(0) < 0;
            offset = neg ? MAX_INT : 0;
            picker.setFormatter((int n) -> String.valueOf(n - offset));
            picker.setMaxValue(neg ? MAX_INT + MAX_INT : MAX_INT);
            picker.setMinValue(0);
            picker.setValue(v + offset);
            picker.setBackgroundColor(ThemeUtils.getStyleAttribColorValue(activity, R.attr.highlight_background, R.color.black));
            picker.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));

            // see
            // https://stackoverflow.com/questions/17708325/android-numberpicker-with-formatter-doesnt-format-on-first-rendering
            View editView = picker.getChildAt(0);
            if (editView instanceof EditText) {
                // Remove default input filter
                ((EditText) editView).setFilters(new InputFilter[0]);
            }
        }

        /**
         * Parse a String as an int and add it to a List if sucessful
         * 
         * @param ints target list of ints
         * @param val value to parse and add
         */
        private void addIntToList(@NonNull List<Integer> ints, @NonNull String val) {
            try {
                ints.add(Integer.parseInt(val));
            } catch (NumberFormatException nfex) {
                // do nothing
            }
        }

        @Override
        @NonNull
        public String getValue() {
            return Integer.toString(picker.getValue() - offset);
        }

        @Override
        public boolean filter(@Nullable String v) {
            if (v == null || "".equals(v)) { // suppress empty value added for deletion
                return false;
            }
            try { // MRU may have numeric values too
                int temp = Integer.parseInt(v);
                return Math.abs(temp) > MAX_INT;
            } catch (NumberFormatException nfex) {
                // add
            }
            return true;
        }

        @Override
        public void enable() {
            picker.setEnabled(true);
            picker.setFocusable(true);
        }

        @Override
        public void disable() {
            picker.setEnabled(false);
            picker.setFocusable(false);
        }

        @Override
        public void onDismiss() {
            TagFormFragment caller = (TagFormFragment) getParentFragment();
            caller.enableTextRow(key);
        }

        @Override
        @NonNull
        public View getWidgetView() {
            return picker;
        }

        @Override
        @Nullable
        public String getUsageText(@NonNull Context ctx, boolean hasAdditionalValues) {
            return hasAdditionalValues ? ctx.getString(R.string.integer_widget_usage_with_additional) : null;
        }
    }
}
