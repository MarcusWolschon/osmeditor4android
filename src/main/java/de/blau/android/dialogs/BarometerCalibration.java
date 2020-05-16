package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
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
import de.blau.android.R;
import de.blau.android.services.TrackerService;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog for calibrating the pressure sensor
 *
 */
public class BarometerCalibration extends ImmersiveDialogFragment {
    private static final String DEBUG_TAG = BarometerCalibration.class.getSimpleName();

    private static final String TAG = "fragment_calibration_form";

    /**
     * Display a dialog asking for a search term and allowing selection of geocoers
     * 
     * @param activity the calling FragmentActivity
     * @param bbox a BoundingBox to restrict the query to if null the whole world is considered
     */
    public static void showDialog(@NonNull AppCompatActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            BarometerCalibration searchFormFragment = newInstance();
            searchFormFragment.show(fm, TAG);
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
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create new instance of this object
     * 
     * @param bbox a BoundingBox to restrict the query to if null the whole world is considered
     * @return a SearchForm instance
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

        dialogBuilder.setPositiveButton(R.string.barometer_calibration_calibrate, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                Intent intent = new Intent(getActivity(), TrackerService.class);
                intent.putExtra(TrackerService.CALIBRATE_KEY, true);
                String calibrationValue = valueEdit.getText().toString().trim();
                try {
                    switch (calibrationMethod.getSelectedItemPosition()) {
                    case 0: // height
                        intent.putExtra(TrackerService.CALIBRATE_HEIGHT_KEY, Integer.parseInt(calibrationValue));
                        break;
                    case 1: // reference pressure
                        intent.putExtra(TrackerService.CALIBRATE_P0_KEY, Float.parseFloat(calibrationValue.substring(0, calibrationValue.length() - 2).trim()));
                        break;
                    default: // GNSS
                        // this is the defaukt
                    }
                    getActivity().startService(intent);
                } catch (NumberFormatException nfex) {
                    Snack.toastTopError(getActivity(), getString(R.string.toast_invalid_number_format, nfex.getMessage()));
                }
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
