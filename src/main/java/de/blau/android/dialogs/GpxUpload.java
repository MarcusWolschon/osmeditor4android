package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.gpx.Track;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.gpx.MapOverlay;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.listener.GpxUploadListener;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.validation.FormValidation;
import de.blau.android.validation.NotEmptyValidator;

/**
 * Display a dialog to select a file to upload
 *
 */
public class GpxUpload extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, GpxUpload.class.getSimpleName().length());
    private static final String DEBUG_TAG = GpxUpload.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG       = "fragment_gpx_upload";
    private static final String TRACK_KEY = "track";

    /**
     * Show an instance of this dialog
     * 
     * @param activity the calling FragmentActivity
     * @param trackId id of the track layer
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull String trackId) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            GpxUpload gpxUploadFragment = newInstance(trackId);
            gpxUploadFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss any current showing instance of this dialog
     * 
     * @param activity the calling FragmentActivity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Get a new instance of this fragment
     * 
     * @param trackId id of the track layer
     * @return a new instance of GpxUpload
     */
    @NonNull
    private static GpxUpload newInstance(String trackId) {
        GpxUpload f = new GpxUpload();
        f.setShowsDialog(true);
        Bundle args = new Bundle();
        args.putString(TRACK_KEY, trackId);
        f.setArguments(args);
        return f;
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.upload_gpx_title);
        DoNothingListener doNothingListener = new DoNothingListener();
        View layout = inflater.inflate(R.layout.upload_gpx, null);
        builder.setView(layout);

        EditText descriptionField = (EditText) layout.findViewById(R.id.upload_gpx_description);
        final FormValidation descriptionValidator = new NotEmptyValidator(descriptionField, getString(R.string.upload_validation_error_empty_description));

        String trackId = getArguments().getString(TRACK_KEY);
        de.blau.android.layer.gpx.MapOverlay layer = (MapOverlay) App.getLogic().getMap().getLayer(LayerType.GPX, trackId);
        if (layer != null) {
            Track track = layer.getTrack();
            if (track != null) {
                builder.setPositiveButton(R.string.transfer_download_current_upload, new GpxUploadListener(getActivity(), track, descriptionField,
                        (EditText) layout.findViewById(R.id.upload_gpx_tags), (Spinner) layout.findViewById(R.id.upload_gpx_visibility)));
            }
        }
        builder.setNegativeButton(R.string.cancel, doNothingListener);
        AppCompatDialog dialog = builder.create();
        dialog.setOnShowListener(d -> descriptionValidator.validate());
        return dialog;
    }
}
