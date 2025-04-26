package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.android.material.slider.RangeSlider;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.layer.streetlevel.DateRangeInterface;
import de.blau.android.layer.tiles.MapTilesLayer;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog allowing the user to change some properties of the current background
 *
 */
public class DateRangeDialog extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, DateRangeDialog.class.getSimpleName().length());
    private static final String DEBUG_TAG = DateRangeDialog.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_daterange";

    public static final String LABEL_FORMAT = "yyyy MMM dd";

    private static final String LAYERINDEX = "layer_index";
    private static final String START_DATE = "start_date";
    private static final String END_DATE   = "end_date";

    private static final long FROM_DATE = 1377990000000L;

    private SimpleDateFormat labelDate = new SimpleDateFormat(LABEL_FORMAT);

    /**
     * Display a dialog allowing the user to change some properties of the current background
     * 
     * @param activity the calling Activity
     * @param layerIndex the index of the Layer
     * @param endDate
     * @param startDate
     */
    public static void showDialog(@NonNull FragmentActivity activity, int layerIndex, long startDate, long endDate) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            DateRangeDialog backgroundPropertiesFragment = newInstance(layerIndex, startDate, endDate);
            backgroundPropertiesFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Get a new DateRange dialog instance
     * 
     * @param layerIndex the index of the Layer
     * @param startDate the start date in ms since the epoch
     * @param endDate the end date in ms since the epoch
     * @return a DateRange instance
     */
    @NonNull
    private static DateRangeDialog newInstance(int layerIndex, long startDate, long endDate) {
        DateRangeDialog f = new DateRangeDialog();
        Bundle args = new Bundle();
        args.putInt(LAYERINDEX, layerIndex);
        args.putLong(START_DATE, startDate);
        args.putLong(END_DATE, endDate);
        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.date_range_title);
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        View layout = inflater.inflate(R.layout.daterange, null);
        RangeSlider slider = (RangeSlider) layout.findViewById(R.id.range_slider);
        slider.setLabelFormatter((float v) -> labelDate.format(new Date(fromDays(v))));
        MapTilesLayer<?> layer = (MapTilesLayer<?>) App.getLogic().getMap().getLayer(getArguments().getInt(LAYERINDEX, -1));
        if (layer instanceof DateRangeInterface) {
            long today = new Date().getTime();
            slider.setValues(toDays(Math.max(FROM_DATE, getArguments().getLong(START_DATE, -1L))),
                    toDays(Math.min(getArguments().getLong(END_DATE, today), today)));
            slider.setValueTo(toDays(today));
            slider.addOnChangeListener((RangeSlider s, float arg1, boolean arg2) -> {
                final List<Float> values = s.getValues();
                if (values != null && values.size() == 2) {
                    ((DateRangeInterface) layer).setDateRange(fromDays(values.get(0)), fromDays(values.get(1)));
                }
                layer.invalidate();
            });
        } else {
            ACRAHelper.nocrashReport(null, "layer null or doesn't implement  DateRangeInterface");
        }
        builder.setView(layout);
        builder.setPositiveButton(R.string.okay, doNothingListener);

        return builder.create();
    }

    /**
     * Convert from days to ms
     * 
     * @param days the number of days
     * @return ms
     */
    private static long fromDays(float days) {
        return (long) (days * 24 * 3600000L);
    }

    /**
     * Convert from ms to days
     * 
     * @param ms the number of ms
     * @return the number of days
     */
    private static float toDays(long ms) {
        return ms / (24f * 3600000L);
    }
}
