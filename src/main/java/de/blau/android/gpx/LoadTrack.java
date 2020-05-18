package de.blau.android.gpx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.util.Snack;

public final class LoadTrack {

    protected static final String DEBUG_TAG = LoadTrack.class.getSimpleName();

    /**
     * Private constructor to stop instantiation
     */
    private LoadTrack() {
        // unused
    }

    /**
     * Read a file in GPX format from device
     * 
     * @param activity activity this was called from
     * @param uri Uri for the file to read
     * @param track the target Track
     * @param handler handler to use after the file has been loaded if not null
     */
    public static void fromFile(@NonNull final FragmentActivity activity, @NonNull final Uri uri, @NonNull Track track,
            @Nullable PostAsyncActionHandler handler) {

        new AsyncTask<Void, Void, Integer>() {

            static final int FILENOTFOUND = -1;
            static final int OK           = 0;

            @Override
            protected void onPreExecute() {
                if (activity != null) {
                    Progress.showDialog(activity, Progress.PROGRESS_LOADING);
                }
            }

            @Override
            protected Integer doInBackground(Void... arg) {
                int result = OK;
                try (InputStream is = activity.getContentResolver().openInputStream(uri); BufferedInputStream in = new BufferedInputStream(is)) {
                    track.importFromGPX(in);
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "Error reading file: ", e);
                    result = FILENOTFOUND;
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer result) {
                try {
                    Progress.dismissDialog(activity, Progress.PROGRESS_LOADING);
                    if (result == OK) {
                        if (handler != null) {
                            handler.onSuccess();
                        }
                        int trackPointCount = track.getTrackPoints().size();
                        int wayPointCount = track.getWayPoints().length;
                        String message = activity.getResources().getQuantityString(R.plurals.toast_imported_track_points, wayPointCount, trackPointCount,
                                wayPointCount);
                        Snack.barInfo(activity, message);
                    } else {
                        if (handler != null) {
                            handler.onError();
                        }
                        Snack.barError(activity, R.string.toast_file_not_found);
                    }
                    activity.invalidateOptionsMenu();
                } catch (IllegalStateException e) {
                    // Avoid crash if activity is paused
                    Log.e(DEBUG_TAG, "onPostExecute", e);
                }
            }

        }.execute();
    }
}
