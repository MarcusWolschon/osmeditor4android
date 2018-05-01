package de.blau.android.geocode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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

    private AppCompatActivity activity;

    private SearchItemSelectedCallback callback;

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
     * @param callback will be called when search result is selected
     */
    public Search(AppCompatActivity activity, SearchItemSelectedCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    /**
     * Query and then display a list of results to pick from
     * 
     * @param geocoder the geocoder to use for the querey
     * @param q the query string
     * @param bbox bounding box to limit the search to
     */
    public void find(Geocoder geocoder, String q, ViewBox bbox) {
        Query querier = null;
        boolean multiline = false;
        switch (geocoder.type) {
        case PHOTON:
            querier = new QueryPhoton(activity, geocoder.url, bbox);
            multiline = true;
            break;
        case NOMINATIM:
        default:
            querier = new QueryNominatim(activity, geocoder.url, bbox);
            multiline = false;
            break;
        }
        querier.execute(q);
        try {
            List<SearchResult> result = querier.get(20, TimeUnit.SECONDS);
            if (!result.isEmpty()) {
                AppCompatDialog sr = createSearchResultsDialog(result, multiline ? R.layout.search_results_item_multi_line : R.layout.search_results_item);
                sr.show();
            } else {
                Snack.barInfo(activity, R.string.toast_nothing_found);
            }
        } catch (InterruptedException | ExecutionException e) {
            Log.e(DEBUG_TAG, "find got exception " + e.getMessage());
            Snack.barError(activity, R.string.no_connection_title);
        } catch (TimeoutException e) {
            Log.e(DEBUG_TAG, "find got exception " + e.getMessage());
            Snack.barError(activity, R.string.toast_timeout);
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
    private AppCompatDialog createSearchResultsDialog(final List<SearchResult> searchResults, int itemLayout) {
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
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                // Log.d("Search","Result at pos " + position + " clicked");
                callback.onItemSelected(searchResults.get(position));
                dialog.dismiss();
            }
        });
        return dialog;
    }
}
