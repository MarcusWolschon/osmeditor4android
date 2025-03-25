package io.vespucci.views;

import java.util.GregorianCalendar;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import ch.poole.android.numberpickerview.library.NumberPickerView;
import ch.poole.android.numberpickerview.library.NumberPickerView.OnValueChangeListener;
import ch.poole.openinghoursfragment.R;

/**
 * 
 *
 */
public class DateRangePicker extends LinearLayout {

    private static final int MIN_YEAR = 0;
    private static final int MAX_YEAR = 2100;

    public static final int     NOTHING_SELECTED = 0;
    private static final String NOTHING          = "-";

    private static final int   JANUARY            = 1;
    private static final int   FEBRUARY           = 2;
    private static final int   FEBRUARY_LEAP_DAYS = 29;
    private static final int   DECEMBER           = 12;
    private static final int   MONTHS_IN_YEAR     = 12;
    private static final int   MAX_DAYS_IN_MONTH  = 31;
    /**
     * This is not really correct for all years since 0
     */
    private static final int[] DAYS_IN_MONTH      = new int[] { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    private static final String[] YEAR_VALUES = new String[MAX_YEAR + 1];
    private static final String[] DAY_VALUES  = new String[32];
    static {
        for (int i = MIN_YEAR; i <= MAX_YEAR; i++) {
            YEAR_VALUES[i] = Integer.toString(i);
        }
        DAY_VALUES[NOTHING_SELECTED] = NOTHING;
        for (int i = 1; i <= 31; i++) {
            DAY_VALUES[i] = Integer.toString(i);
        }
    }

    private boolean startOnly;
    private int     startYear;
    private int     startMonth;
    private int     startDayOfMonth;
    private int     endYear;
    private int     endMonth;
    private int     endDayOfMonth;

    NumberPickerView npvStartYear;
    NumberPickerView npvStartMonth;
    NumberPickerView npvStartDay;
    NumberPickerView npvEndYear;
    NumberPickerView npvEndMonth;
    NumberPickerView npvEndDay;

    /**
     * Standard View constructor
     * 
     * @param context Android Context
     */
    public DateRangePicker(Context context) {
        super(context);

    }

    /**
     * Standard View constructor
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public DateRangePicker(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    /**
     * Standard View constructor
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     * @param defStyleAttr a Style resource id
     */
    public DateRangePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(boolean startOnly, int startYear, int startMonth, int startDayOfMonth, int endYear, int endMonth, int endDayOfMonth) {
        this.startOnly = startOnly;

        this.startYear = startYear;
        npvStartYear.setValue(startYear);
        this.startMonth = startMonth;
        npvStartMonth.setValue(startMonth);
        this.startDayOfMonth = startDayOfMonth;
        npvStartDay.setValue(startDayOfMonth);
        if (startOnly) {
            npvEndYear.setVisibility(View.GONE);
            npvEndMonth.setVisibility(View.GONE);
            npvEndDay.setVisibility(View.GONE);
        } else {
            this.endYear = endYear;
            npvEndYear.setValue(endYear);
            this.endMonth = endMonth;
            npvEndMonth.setValue(endMonth);
            this.endDayOfMonth = endDayOfMonth;
            npvEndDay.setValue(endDayOfMonth);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final GregorianCalendar calendar = new GregorianCalendar();

        npvStartYear = (NumberPickerView) findViewById(R.id.startYear);
        npvStartYear.setDisplayedValues(YEAR_VALUES);
        npvStartYear.setMinValue(MIN_YEAR);
        npvStartYear.setMaxValue(MAX_YEAR);
        npvStartYear.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (!startOnly) {
                int endY = npvEndYear.getValue();
                if (newVal >= endY && endY >= 0) {
                    npvEndYear.smoothScrollToValue(newVal);
                }
            }
            if (!calendar.isLeapYear(newVal) && npvStartMonth.getValue() == FEBRUARY && npvStartDay.getValue() > DAYS_IN_MONTH[FEBRUARY]) {
                npvStartDay.smoothScrollToValue(DAYS_IN_MONTH[FEBRUARY]);
            }
        });

        String[] monthEntries = new String[MONTHS_IN_YEAR + 1];
        monthEntries[NOTHING_SELECTED] = NOTHING;
        System.arraycopy(getResources().getStringArray(R.array.months_entries), 0, monthEntries, JANUARY, DECEMBER);

        npvStartMonth = (NumberPickerView) findViewById(R.id.startMonth);
        npvStartMonth.setDisplayedValues(monthEntries);
        npvStartMonth.setMinValue(NOTHING_SELECTED);
        npvStartMonth.setMaxValue(MONTHS_IN_YEAR);

        npvStartDay = (NumberPickerView) findViewById(R.id.startDay);
        npvStartDay.setDisplayedValues(DAY_VALUES);
        npvStartDay.setMinValue(NOTHING_SELECTED);
        npvStartDay.setMaxValue(MAX_DAYS_IN_MONTH);

        npvStartMonth.setOnValueChangedListener(new OnMonthChangedListener(npvStartYear, npvStartDay));
        npvStartDay.setOnValueChangedListener(new OnDayChangedListener(npvStartYear, npvStartMonth));

        npvEndYear = (NumberPickerView) findViewById(R.id.endYear);
        npvEndYear.setDisplayedValues(YEAR_VALUES);
        npvEndYear.setMinValue(NOTHING_SELECTED);
        npvEndYear.setMaxValue(MAX_YEAR);
        npvEndYear.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (!calendar.isLeapYear(newVal) && npvEndMonth.getValue() == FEBRUARY && npvEndDay.getValue() > DAYS_IN_MONTH[FEBRUARY]) {
                npvEndDay.smoothScrollToValue(DAYS_IN_MONTH[FEBRUARY]);
            }
        });

        npvEndMonth = (NumberPickerView) findViewById(R.id.endMonth);
        npvEndMonth.setDisplayedValues(monthEntries);
        npvEndMonth.setMinValue(NOTHING_SELECTED);
        npvEndMonth.setMaxValue(MONTHS_IN_YEAR);

        npvEndDay = (NumberPickerView) findViewById(R.id.endDay);
        npvEndDay.setDisplayedValues(DAY_VALUES);
        npvEndDay.setMinValue(NOTHING_SELECTED);
        npvEndDay.setMaxValue(MAX_DAYS_IN_MONTH);

        npvEndMonth.setOnValueChangedListener(new OnMonthChangedListener(npvEndYear, npvEndDay));
        npvEndDay.setOnValueChangedListener(new OnDayChangedListener(npvEndYear, npvEndMonth));
    }

    private class OnMonthChangedListener implements OnValueChangeListener {
        private final GregorianCalendar calendar = new GregorianCalendar();
        private final NumberPickerView  yearPicker;
        private final NumberPickerView  dayPicker;

        OnMonthChangedListener(@NonNull final NumberPickerView yearPicker, @NonNull final NumberPickerView dayPicker) {
            this.yearPicker = yearPicker;
            this.dayPicker = dayPicker;
        }

        @Override
        public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
            if (newVal == NOTHING_SELECTED) {
                dayPicker.smoothScrollToValue(NOTHING_SELECTED);
                return;
            }
            final int monthIndex = newVal - 1;
            if (dayPicker.getValue() > DAYS_IN_MONTH[monthIndex]) {
                if (!calendar.isLeapYear(yearPicker.getValue()) || newVal != FEBRUARY) {
                    dayPicker.smoothScrollToValue(DAYS_IN_MONTH[monthIndex]);
                    return;
                }
                dayPicker.smoothScrollToValue(FEBRUARY_LEAP_DAYS);
            }
        }
    }

    private class OnDayChangedListener implements OnValueChangeListener {
        private final GregorianCalendar calendar = new GregorianCalendar();
        private final NumberPickerView  yearPicker;
        private final NumberPickerView  monthPicker;

        OnDayChangedListener(@NonNull final NumberPickerView yearPicker, @NonNull final NumberPickerView monthPicker) {
            this.yearPicker = yearPicker;
            this.monthPicker = monthPicker;
        }

        @Override
        public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
            int month = monthPicker.getValue();
            final int monthIndex = month - 1;
            if (month != NOTHING_SELECTED && newVal > DAYS_IN_MONTH[monthIndex]) {
                if (!calendar.isLeapYear(yearPicker.getValue()) || month != FEBRUARY) {
                    picker.smoothScrollToValue(DAYS_IN_MONTH[monthIndex]);
                    return;
                }
                picker.smoothScrollToValue(FEBRUARY_LEAP_DAYS);
            }
        }
    }

    /**
     * Get the start year
     * 
     * @return the start year
     */
    public int getStartYear() {
        return npvStartYear.getValue();
    }

    /**
     * Get the start month
     * 
     * @return the start month, 0 == nothing selected
     */
    public int getStartMonth() {
        return npvStartMonth.getValue();
    }

    /**
     * Get the start day
     * 
     * @return the start day, 0 == nothing selected
     */
    public int getStartDayOfMonth() {
        return npvStartDay.getValue();
    }

    /**
     * Get the end year
     * 
     * @return the end year
     */
    public int getEndYear() {
        return npvEndYear.getValue();
    }

    /**
     * Get the end month
     * 
     * @return the end month, 0 == nothing selected
     */
    public int getEndMonth() {
        return npvEndMonth.getValue();
    }

    /**
     * Get the end day
     * 
     * @return the end day, 0 == nothing selected
     */
    public int getEndDay() {
        return npvEndDay.getValue();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.startOnly = startOnly;
        ss.startYear = startYear;
        ss.startMonth = startMonth;
        ss.startDayOfMonth = startDayOfMonth;
        ss.endYear = endYear;
        ss.endMonth = endMonth;
        ss.endDayOfMonth = endDayOfMonth;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        init(ss.startOnly, ss.startYear, ss.startMonth, ss.startDayOfMonth, ss.endYear, ss.endMonth, ss.endDayOfMonth);
    }

    /**
     * Holds important values when we need to save instance state.
     */
    public static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {

            /**
             * Get a new SavedState object
             * 
             * @param in a Parcel
             * @return a SavedState object
             */
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            /**
             * Get an array for SavedState objects
             * 
             * @param size the size of the Array
             * @return an SavedState array of size size
             */
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        boolean startOnly;
        int     startYear;
        int     startMonth;
        int     startDayOfMonth;
        int     endYear;
        int     endMonth;
        int     endDayOfMonth;

        /**
         * Construct a new instance from a Parcelable
         * 
         * @param superState saved state
         */
        SavedState(@NonNull Parcelable superState) {
            super(superState);
        }

        /**
         * Construct a new instance from a Parcel
         * 
         * @param in the Parcel
         */
        private SavedState(@NonNull Parcel in) {
            super(in);
            startOnly = 1 == in.readInt();
            startYear = in.readInt();
            startMonth = in.readInt();
            startDayOfMonth = in.readInt();
            endYear = in.readInt();
            endMonth = in.readInt();
            endDayOfMonth = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(startOnly ? 1 : 0);
            out.writeInt(startYear);
            out.writeInt(startMonth);
            out.writeInt(startDayOfMonth);
            out.writeInt(endYear);
            out.writeInt(endMonth);
            out.writeInt(endDayOfMonth);
        }
    }
}
