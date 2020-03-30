package de.blau.android.geocode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.blau.android.R;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.AdvancedPrefDatabase.Geocoder;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Search with nominatim, photon and maybe others
 * 
 * @author simon
 *
 */
public class Search {
    static final String DEBUG_TAG = "Search";

    private final AppCompatActivity activity;

    private final SearchItemSelectedCallback callback;

    private final Dialog dialog;

    public static class SearchResult {
        private double lat;
        private double lon;
        String         displayName;

        @Override
        public String toString() {
            return "lat: " + getLat() + " lon: " + getLon() + " " + displayName;
        }

        /**
         * @return the lat
         */
        public double getLat() {
            return lat;
        }

        /**
         * @param lat the lat to set
         */
        public void setLat(double lat) {
            this.lat = lat;
        }

        /**
         * @return the lon
         */
        public double getLon() {
            return lon;
        }

        /**
         * @param lon the lon to set
         */
        public void setLon(double lon) {
            this.lon = lon;
        }
    }

    /**
     * Constructor
     * 
     * @param activity activity calling this
     * @param dialog the dialog used to get the input
     * @param callback will be called when search result is selected
     */
    public Search(@NonNull AppCompatActivity activity, @Nullable Dialog dialog, @NonNull SearchItemSelectedCallback callback) {
        this.activity = activity;
        this.dialog = dialog;
        this.callback = callback;
    }

    /**
     * Query and then display a list of results to pick from
     * 
     * @param geocoder the geocoder to use for the querey
     * @param q the query string
     * @param bbox bounding box to limit the search to
     * @param limitSearch if true limitSearch to bbox
     */
    public void find(@NonNull Geocoder geocoder, @NonNull String q, @Nullable ViewBox bbox, boolean limitSearch) {
        Query querier = null;
        boolean multiline = false;
        switch (geocoder.type) {
        case PHOTON:
            querier = new QueryPhoton(activity, geocoder.url, bbox);
            multiline = true;
            break;
        case NOMINATIM:
        default:
            querier = new QueryNominatim(activity, geocoder.url, bbox, limitSearch);
            multiline = false;
            break;
        }
        querier.execute(q);
        try {
            List<SearchResult> result = querier.get(20, TimeUnit.SECONDS);
            if (result != null && !result.isEmpty()) {
                if (dialog != null) { // dismiss keyboard
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    Window window = dialog.getWindow();
                    if (window != null) {
                        imm.hideSoftInputFromWindow(window.getDecorView().getWindowToken(), 0);
                    }
                }
                AppCompatDialog sr = createSearchResultsDialog(result, multiline ? R.layout.search_results_item_multi_line : R.layout.search_results_item);
                sr.show();
            } else {
                Snack.toastTopWarning(activity, R.string.toast_nothing_found);
            }
        } catch (InterruptedException | ExecutionException e) { // NOSONAR cancel does interrupt the thread in question
            Log.e(DEBUG_TAG, "find got exception " + e.getMessage());
            querier.cancel(true);
            Snack.toastTopError(activity, R.string.no_connection_title);
        } catch (TimeoutException e) {
            Log.e(DEBUG_TAG, "find got exception " + e.getMessage());
            Snack.toastTopError(activity, R.string.toast_timeout);
        }
    }

    /**
     * Create a Dialog with the results of a search
     * 
     * @param searchResults List containing the SearchResults
     * @param itemLayout an Android resource id for the Layout
     * @return a Dialog object
     */
    @SuppressLint("InflateParams")
    private AppCompatDialog createSearchResultsDialog(@NonNull final List<SearchResult> searchResults, int itemLayout) {
        //
        Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.search_results_title);
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
        ListView lv = (ListView) inflater.inflate(R.layout.search_results, null);
        builder.setView(lv);

        List<Spanned> ar = new ArrayList<>();
        for (SearchResult sr : searchResults) {
            ar.add(Util.fromHtml(sr.displayName));
        }
        lv.setAdapter(new ArrayAdapter<>(activity, itemLayout, ar));
        lv.setSelection(0);
        builder.setNegativeButton(R.string.cancel, null);
        final AppCompatDialog dialog = builder.create();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                callback.onItemSelected(searchResults.get(position));
                dialog.dismiss();
            }
        });
        return dialog;
    }
}
