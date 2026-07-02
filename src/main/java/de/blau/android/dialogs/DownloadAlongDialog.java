package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;
import static de.blau.android.util.GeoMath.OSM_SCALE;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import ch.poole.android.numberpicker.library.NumberPicker;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.BoundingBoxCoverage;
import de.blau.android.osm.GeoPoint;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.DownloadService;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.GeoMath;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class DownloadAlongDialog extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, DownloadAlongDialog.class.getSimpleName().length());
    private static final String DEBUG_TAG = DownloadAlongDialog.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_download_along";

    private static final int MIN_BOUNDING_BOX_DIM = 20;

    private static final String TITLE_KEY          = "title_key";
    private static final String DATA_DOWNLOAD_KEY  = "data";
    private static final String TASKS_DOWNLOAD_KEY = "tasks";
    private static final String BUFFER_KEY         = "buffer";
    private static final String MAX_DIMENSION_KEY  = "maxDim";
    private static final String ELEMENT_KEY        = "element";
    private static final String REF_POINT_KEY      = "refPoint";

    private int                 titleRes;
    private BoundingBoxCoverage element;
    private CheckBox            downloadData;
    private CheckBox            downloadTasks;
    private NumberPicker        bufferPicker;
    private NumberPicker        maxDimPicker;
    private GeoPoint            refPoint;

    /**
     * Show a dialog with a list of issues
     * 
     * @param activity the calling FragmentActivity
     * @param titleRes resource id for the title
     * @param elment the object that we want to download along
     * @param refPoint a GeoPoint indicating what bit of element is in view for better sorting
     *
     */
    public static void show(@NonNull AppCompatActivity activity, int titleRes, @NonNull BoundingBoxCoverage element, @Nullable GeoPoint refPoint) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            if (activity instanceof Main) {
                ((Main) activity).descheduleAutoLock();
            }
            DownloadAlongDialog elementIssueFragment = newInstance(titleRes, element, refPoint);
            elementIssueFragment.show(fm, TAG);
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
     * @param titleRes resource id for the title
     * @param refPoint
     * @param messageRes resource if for the message
     * @param tipKeyRes tip key resource
     * @param tipRes tip message resource
     * @param result the List of Result elements
     * @return a TagConflictDialog instance
     */
    private static DownloadAlongDialog newInstance(int titleRes, @NonNull BoundingBoxCoverage element, @Nullable GeoPoint refPoint) {
        DownloadAlongDialog f = new DownloadAlongDialog();
        Bundle args = new Bundle();
        args.putInt(TITLE_KEY, titleRes);
        args.putSerializable(REF_POINT_KEY, refPoint);
        args.putSerializable(ELEMENT_KEY, element);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public AppCompatDialog onCreateDialog(Bundle savedState) {
        if (savedState != null) {
            Log.d(DEBUG_TAG, "Recreating from saved state");
            getArgsOrState(savedState);
        } else {
            getArgsOrState(getArguments());
        }
        final FragmentActivity activity = getActivity();
        
        if (!(activity instanceof Main)) {
            throw new IllegalStateException("Can currently only be called from Main");
        }
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
        ((Main) activity).descheduleAutoLock();

        final Preferences prefs = App.getPreferences(activity);
        View layout = inflater.inflate(R.layout.download_along, null);
        downloadData = ((CheckBox) layout.findViewById(R.id.download_data_check));
        downloadData.setChecked(hasState(savedState, DATA_DOWNLOAD_KEY) ? savedState.getBoolean(DATA_DOWNLOAD_KEY) : prefs.getDownloadAlongData());
        downloadTasks = ((CheckBox) layout.findViewById(R.id.download_tasks_check));
        downloadTasks.setChecked(hasState(savedState, TASKS_DOWNLOAD_KEY) ? savedState.getBoolean(TASKS_DOWNLOAD_KEY) : prefs.getDownloadAlongTasks());
        bufferPicker = ((NumberPicker) layout.findViewById(R.id.download_buffer));
        bufferPicker.setValue(hasState(savedState, BUFFER_KEY) ? savedState.getInt(BUFFER_KEY) : prefs.getDownloadAlongBuffer());
        maxDimPicker = ((NumberPicker) layout.findViewById(R.id.download_max_dimension));
        maxDimPicker.setValue(hasState(savedState, MAX_DIMENSION_KEY) ? savedState.getInt(MAX_DIMENSION_KEY) : prefs.getDownloadAlongMaxDimension());

        Logic logic = App.getLogic();
        return ThemeUtils.getAlertDialogBuilder(activity).setTitle(titleRes).setView(layout)
                .setPositiveButton(R.string.submit, (dialog, which) -> new ExecutorTask<Void, Void, Void>(logic.getExecutorService(), logic.getHandler()) {
                    @Override
                    protected void onPreExecute() {
                        Progress.showDialog(activity, Progress.PROGRESS_CALCULATING);
                    }

                    @Override
                    protected Void doInBackground(Void id) throws IOException {
                        List<BoundingBox> boxes = element.getCoverage(bufferPicker.getValue(), MIN_BOUNDING_BOX_DIM, maxDimPicker.getValue());
                        Log.d(DEBUG_TAG, "Downloading " + boxes.size() + " boxes");
                        // this is just to improve the optics
                        if (refPoint != null) {
                            sortCoverage(refPoint, boxes);
                        }
                        //
                        prefs.setDownloadAlongData(downloadData.isChecked());
                        prefs.setDownloadAlongTasks(downloadTasks.isChecked());
                        prefs.setDownloadAlongBuffer(bufferPicker.getValue());
                        prefs.setDownloadAlongMaxDimension(maxDimPicker.getValue());
                        DownloadService downloadService = ((Main) activity).getDownloadService();
                        if (downloadService != null) {
                            downloadService.start(boxes, downloadData.isChecked(), downloadTasks.isChecked());
                        }
                        return null;
                    }

                    @Override
                    protected void onBackgroundError(Exception e) {
                        Log.e(DEBUG_TAG, "Exception during download along way " + e.getMessage());
                        Progress.dismissDialog(activity, Progress.PROGRESS_CALCULATING);
                        ScreenMessage.toastTopError(activity, e.getLocalizedMessage());
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        Progress.dismissDialog(activity, Progress.PROGRESS_CALCULATING);
                    }
                }.execute()).setNeutralButton(R.string.cancel, null).create();
    }

    /**
     * Get arguments or state from a Bundle
     * 
     * @param bundle the bundle
     */
    private void getArgsOrState(@NonNull Bundle bundle) {
        titleRes = bundle.getInt(TITLE_KEY);
        refPoint = Util.getSerializeable(bundle, REF_POINT_KEY, GeoPoint.class);
        element = Util.getSerializeable(bundle, ELEMENT_KEY, BoundingBoxCoverage.class);
    }

    /**
     * Sorts the list of bounding boxes by the distance to a reference point
     * 
     * @param ref the reference GeoPoint
     * @param boxes the list of BoundingBox
     */
    private void sortCoverage(@NonNull GeoPoint ref, @NonNull List<BoundingBox> boxes) {
        double refLon = ref.getLon() / OSM_SCALE;
        double refLat = ref.getLat() / OSM_SCALE;
        Collections.sort(boxes, (BoundingBox box1, BoundingBox box2) -> {
            double[] center1 = box1.getCenter();
            double[] center2 = box2.getCenter();
            return Double.compare(GeoMath.haversineDistance(refLon, center2[0], refLat, center2[1]),
                    GeoMath.haversineDistance(refLon, center1[0], refLat, center1[1]));
        });
    }

    /**
     * Check if we have saved state for a key
     * 
     * @param savedState the Bundle holding state or null
     * @param key the key
     * @return true if there is state available
     */
    private boolean hasState(@Nullable Bundle savedState, @NonNull String key) {
        return savedState != null && savedState.containsKey(key);
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        Tip.showDialog(getActivity(), R.string.tip_download_along_key, R.string.tip_download_along);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() instanceof Main) {
            ((Main) getActivity()).scheduleAutoLock();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putInt(TITLE_KEY, titleRes);
        outState.putSerializable(REF_POINT_KEY, refPoint);
        outState.putSerializable(ELEMENT_KEY, element);
        outState.putBoolean(DATA_DOWNLOAD_KEY, downloadData.isChecked());
        outState.putBoolean(TASKS_DOWNLOAD_KEY, downloadTasks.isChecked());
        outState.putInt(BUFFER_KEY, bufferPicker.getValue());
        outState.putInt(MAX_DIMENSION_KEY, maxDimPicker.getValue());
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }
}
