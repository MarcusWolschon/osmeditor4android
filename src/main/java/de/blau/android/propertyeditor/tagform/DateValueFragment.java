package de.blau.android.propertyeditor.tagform;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.Calendar;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;
import de.blau.android.util.ThemeUtils;
import de.blau.android.views.DateRangePicker;

public class DateValueFragment extends ValueWidgetFragment {

    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, DateValueFragment.class.getSimpleName().length());
    protected static final String DEBUG_TAG = DateValueFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "DATE_FRAGMENT";

    private static final Pattern DATE_FULL_PATTERN       = Pattern.compile("^([012][0-9][0-9][0-9])\\-([01]?[0-9])\\-([0-3]?[0-9])$");
    private static final Pattern DATE_YEAR_MONTH_PATTERN = Pattern.compile("^([012][0-9][0-9][0-9])\\-([01]?[0-9])$");
    private static final Pattern DATE_YEAR_PATTERN       = Pattern.compile("^([012][0-9][0-9][0-9])$");

    private static final String DATE_FULL       = "%04d-%02d-%02d";
    private static final String DATE_YEAR_MONTH = "%04d-%02d";
    private static final String DATE_YEAR       = "%04d";

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
        DateValueFragment df = ValueWidgetFragment.setArguments(new DateValueFragment(), hint, key, value, values,
                preset != null && rootGroup != null ? preset.getPath(rootGroup) : null, allTags);
        de.blau.android.propertyeditor.Util.removeChildFragment(fm, TAG);
        df.show(fm, TAG);
    }

    @NonNull
    @Override
    ValueWidget getWidget(@NonNull FragmentActivity activity, @NonNull String value, @Nullable List<String> values) {
        return new DateWidget(activity, value, values);
    }

    private static final class Date {
        int year;
        int month;
        int dayOfMonth;
    }

    class DateWidget extends BaseValueWidget {

        final Set<String> values = new HashSet<>();

        /**
         * Construct a new widget
         * 
         * @param activity current FragmentActivity
         * @param value initial value
         * @param values any additional values from the preset or MRU
         */
        DateWidget(@NonNull FragmentActivity activity, @NonNull String value, @Nullable List<String> values) {
            super(activity.getLayoutInflater().inflate(R.layout.editor_daterangepicker, null));
            Date date = parseDate(value);
            final Calendar calendar = Calendar.getInstance();
            ((DateRangePicker) picker).init(true, date != null ? date.year : calendar.get(Calendar.YEAR), date != null ? date.month : 0,
                    date != null ? date.dayOfMonth : 0, 0, 0, 0);

            picker.setBackgroundColor(ThemeUtils.getStyleAttribColorValue(activity, R.attr.highlight_background, R.color.black));
            picker.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        }

        /**
         * Try to parse a date
         * 
         * @param value the input date string
         * @return the parsed date or null
         */
        @Nullable
        private Date parseDate(String value) {
            Date date = new Date();
            try {
                Matcher m = DATE_FULL_PATTERN.matcher(value);
                if (m.find()) {
                    date.year = Integer.parseInt(m.group(1));
                    date.month = Integer.parseInt(m.group(2));
                    date.dayOfMonth = Integer.parseInt(m.group(3));
                    return date;
                }
                m = DATE_YEAR_MONTH_PATTERN.matcher(value);
                if (m.find()) {
                    date.year = Integer.parseInt(m.group(1));
                    date.month = Integer.parseInt(m.group(2));
                    date.dayOfMonth = 0;
                    return date;
                }
                m = DATE_YEAR_PATTERN.matcher(value);
                if (m.find()) {
                    date.year = Integer.parseInt(m.group(1));
                    date.month = 0;
                    date.dayOfMonth = 0;
                    return date;
                }
            } catch (NumberFormatException nfex) {
                Log.e(DEBUG_TAG, "Date parse error " + nfex.getMessage());
            }
            return null;
        }

        @Override
        @NonNull
        public String getValue() {
            DateRangePicker datePicker = (DateRangePicker) picker;
            final int startYear = datePicker.getStartYear();
            final int startMonth = datePicker.getStartMonth();
            final int startDay = datePicker.getStartDayOfMonth();
            try {
                if (startMonth != 0 && startDay != 0) {
                    return String.format(DATE_FULL, startYear, startMonth, startDay);
                }
                if (startMonth != 0) {
                    return String.format(DATE_YEAR_MONTH, startYear, startMonth);
                }
                return String.format(DATE_YEAR, startYear);
            } catch (IllegalFormatException ifex) {
                String output = startYear + "-" + startMonth + "-" + startDay;
                Log.e(DEBUG_TAG, "Output formating failed for " + output);
                return output;
            }
        }

        @Override
        public boolean filter(@Nullable String v) {
            if (v == null || "".equals(v)) { // suppress empty value added for deletion
                return false;
            }
            // we want to drop all date values
            return parseDate(v) == null;
        }

        @Override
        @Nullable
        public String getUsageText(@NonNull Context ctx, boolean hasAdditionalValues) {
            return hasAdditionalValues ? ctx.getString(R.string.date_widget_usage_with_additional) : null;
        }
    }
}
