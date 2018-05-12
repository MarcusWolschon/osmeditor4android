package de.blau.android.geocode;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.ProgressDialog;
import de.blau.android.geocode.Search.SearchResult;
import de.blau.android.osm.ViewBox;
import de.blau.android.util.Snack;

class Query extends AsyncTask<String, Void, List<SearchResult>> {
    private static final String DEBUG_TAG = "Search.Query";

    AlertDialog progress = null;

    final ViewBox          bbox;
    final String           url;
    final FragmentActivity activity;

    /**
     * Query a geocoder
     * 
     * @param activity the calling FragmentActivity, if null no progress spinner will be shown
     * @param url URL for the specific instance of the geocoder
     * @param bbox a ViewBox to restrict the query to, if null the whole world will be considered
     */
    public Query(@Nullable FragmentActivity activity, @NonNull String url, @Nullable ViewBox bbox) {
        this.url = url;
        this.bbox = bbox;
        this.activity = activity;
    }

    @Override
    protected void onPreExecute() {
        if (activity != null) {
            progress = ProgressDialog.get(activity, Progress.PROGRESS_SEARCHING);
            progress.show();
        }
    }

    @Override
    protected List<SearchResult> doInBackground(String... params) {
        return new ArrayList<>();
    }

    @Override
    protected void onPostExecute(List<SearchResult> res) {
        try {
            progress.dismiss();
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "dismiss dialog failed with " + ex);
        }
    }

    /**
     * Show a toast if connection to server has failed
     * 
     * @param message message to display
     */
    void connectionError(@NonNull final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snack.toastTopError(activity, activity.getString(R.string.toast_server_connection_failed, message));
            }
        });
    }
}
