package io.vespucci.dialogs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentManager;
import io.vespucci.R;
import io.vespucci.services.TrackerService;
import io.vespucci.util.ImmersiveDialogFragment;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.ThemeUtils;

/**
 * Display a dialog for calibrating the pressure sensor
 *
 */
public class BarometerCalibration extends ImmersiveDialogFragment {
    private static final String DEBUG_TAG = BarometerCalibration.class.getSimpleName().substring(0, Math.min(23, BarometerCalibration.class.getSimpleName().length()));

    private static final String TAG = "fragment_calibration_form";

    private static final Pattern FLOAT_PATTERN = Pattern.compile("[^0-9]*([0-9]+[.][0-9]*)[^0-9]*");

    /**
     * Display a dialog allowing barometer calibration
     * 
     * @param activity the calling FragmentActivity
     */
    public static void showDialog(@NonNull AppCompatActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            BarometerCalibration calibrationFragment = newInstance();
            calibrationFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the Dialog
     * 
     * @param activity the calling FragmentActivity
     */
    private static void dismissDialog(@NonNull AppCompatActivity activity) {
        io.vespucci.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create new instance of this object
     * 
     * @return a BarometerCallibration instance
     */
    private static BarometerCalibration newInstance() {
        BarometerCalibration f = new BarometerCalibration();
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.calibration, null);

        Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(R.string.menu_tools_calibrate_height);

        dialogBuilder.setView(layout);

        final EditText valueEdit = (EditText) layout.findViewById(R.id.barometer_calibration_edit);
        final Spinner calibrationMethod = (Spinner) layout.findViewById(R.id.barometer_calibration_method);
        dialogBuilder.setNegativeButton(R.string.cancel, null);

        dialogBuilder.setPositiveButton(R.string.barometer_calibration_calibrate, (dialog, which) -> {
            Intent intent = new Intent(getActivity(), TrackerService.class);
            intent.putExtra(TrackerService.CALIBRATE_KEY, true);
            String calibrationValue = valueEdit.getText().toString().trim();
            try {
                switch (calibrationMethod.getSelectedItemPosition()) {
                case 0: // height
                    intent.putExtra(TrackerService.CALIBRATE_HEIGHT_KEY, Integer.parseInt(calibrationValue));
                    break;
                case 1: // reference pressure
                    Matcher matcher = FLOAT_PATTERN.matcher(calibrationValue);
                    if (matcher.find()) {
                        intent.putExtra(TrackerService.CALIBRATE_P0_KEY, Float.parseFloat(matcher.group(1)));
                    } else {
                        throw new NumberFormatException(calibrationValue);
                    }
                    break;
                default: // GNSS
                    // this is the default
                }
                getActivity().startService(intent);
            } catch (NumberFormatException nfex) {
                ScreenMessage.toastTopError(getActivity(), getString(R.string.toast_invalid_number_format, nfex.getMessage()));
            }
        });

        return dialogBuilder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}
