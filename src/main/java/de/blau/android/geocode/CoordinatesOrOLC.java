package de.blau.android.geocode;

import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.openlocationcode.OpenLocationCode;
import com.google.openlocationcode.OpenLocationCode.CodeArea;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.dialogs.TextLineDialog;
import de.blau.android.geocode.Search.SearchResult;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.AdvancedPrefDatabase.Geocoder;
import de.blau.android.util.CoordinateParser;
import de.blau.android.util.LatLon;
import de.blau.android.util.NetworkStatus;

/**
 * Ask the user for coordinates or an OLC for example WF8Q+WF Praia, Cabo Verde
 * 
 * @author simon
 *
 */
public class CoordinatesOrOLC {

    protected static final String DEBUG_TAG = "CoordinatesOrOLC";

    private static final Pattern OLC_SHORT = Pattern.compile("(^|\\s)([23456789CFGHJMPQRVWX]{4,6}\\+[23456789CFGHJMPQRVWX]{2,3})\\s*(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern OLC_FULL  = Pattern
            .compile("(^|\\s)([23456789C][23456789CFGHJMPQRV][23456789CFGHJMPQRVWX]{6}\\+[23456789CFGHJMPQRVWX]{2,3})(\\s|$)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static AppCompatDialog dialog;

    public interface HandleResult {

        /**
         * Call this if we successfully determined a pair of coordinates
         * 
         * @param ll a LatLon object with the coordinates
         */
        void onSuccess(@NonNull LatLon ll);

        /**
         * Call this with an error message if things went wrong
         * 
         * @param message the message
         */
        void onError(@NonNull String message);
    }

    /**
     * Show a dialog and ask the user for input
     * 
     * @param activity the calling FragmentActivity
     * @param handler a handler for the results
     */
    public static void get(@NonNull final FragmentActivity activity, @NonNull final HandleResult handler) {
        dialog = TextLineDialog.get(activity, R.string.go_to_coordinates_title, R.string.go_to_coordinates_hint, input -> {
            String text = input.getText().toString();
            if ("".equals(text)) {
                return;
            }
            LatLon ll = null;
            try {
                ll = CoordinateParser.parseVerbatimCoordinates(text);
            } catch (ParseException pex) {
                try {
                    OpenLocationCode olc = null;
                    Matcher m = OLC_FULL.matcher(text);
                    if (m.find()) {
                        olc = new OpenLocationCode(m.group(2));
                    } else {
                        m = OLC_SHORT.matcher(text);
                        if (m.find()) {
                            olc = new OpenLocationCode(m.group(2));
                            final String loc = m.group(3);
                            if (!"".equals(loc)) { // user has supplied a location
                                if (new NetworkStatus(activity).isConnected()) {
                                    String url = getNominatimUrl(activity);
                                    if (url != null) {
                                        QueryNominatim querier = new QueryNominatim(null, url, null, false);
                                        querier.execute(loc);
                                        List<SearchResult> results;
                                        try {
                                            results = querier.get(5, TimeUnit.SECONDS); // FIXME move this
                                                                                        // to a thread
                                            if (results != null && !results.isEmpty()) {
                                                SearchResult result = results.get(0);
                                                olc = olc.recover(result.getLat(), result.getLon());
                                            } else {
                                                handler.onError(activity.getString(R.string.no_nominatim_result, loc));
                                                return;
                                            }
                                        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
                                            querier.cancel(true);
                                            handler.onError(activity.getString(R.string.no_nominatim_result, loc));
                                            return;
                                        }
                                    } else {
                                        handler.onError(activity.getString(R.string.no_nominatim_server));
                                        return;
                                    }
                                } else {
                                    handler.onError(activity.getString(R.string.network_required));
                                    return;
                                }
                            } else { // relative to screen center
                                Log.e(DEBUG_TAG, "trying to recover OLC");
                                ViewBox box = App.getLogic().getViewBox();
                                Log.e(DEBUG_TAG, "box " + box.toPrettyString());
                                if (box != null) {
                                    double[] c = box.getCenter();
                                    Log.e(DEBUG_TAG, "recover coords " + c[1] + " / " + c[0]);
                                    olc = olc.recover(c[1], c[0]);
                                }
                            }
                        } else {
                            Log.e(DEBUG_TAG, "Not an OLC " + text);
                            handler.onError(activity.getString(R.string.unparseable_coordinates));
                        }
                    }
                    if (olc != null) {
                        CodeArea ca = olc.decode();
                        ll = new LatLon(ca.getCenterLatitude(), ca.getCenterLongitude());
                    }
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, e.getMessage());
                    handler.onError(activity.getString(R.string.unparseable_coordinates));
                    return;
                }
            }
            if (ll != null) {
                handler.onSuccess(ll);
                dismiss();
            }
        }, false);
        dialog.show();
    }

    /**
     * Dismiss the dialog
     */
    private static void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Get a URL for a Nominatim server
     * 
     * @param activity the calling activity
     * @return the url or null
     */
    @Nullable
    private static String getNominatimUrl(@NonNull final FragmentActivity activity) {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(activity)) {
            final Geocoder[] geocoders = db.getActiveGeocoders();
            String url = null;
            for (Geocoder g : geocoders) {
                if (g.type == AdvancedPrefDatabase.GeocoderType.NOMINATIM) {
                    url = g.url;
                    break;
                }
            }
            return url;
        }
    }
}
