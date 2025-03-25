package io.vespucci.geocode;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.openlocationcode.OpenLocationCode;
import com.google.openlocationcode.OpenLocationCode.CodeArea;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.contract.Schemes;
import io.vespucci.dialogs.TextLineDialog;
import io.vespucci.geocode.Search.SearchResult;
import io.vespucci.osm.ViewBox;
import io.vespucci.util.CoordinateParser;
import io.vespucci.util.ExecutorTask;
import io.vespucci.util.GeoUriData;
import io.vespucci.util.LatLon;
import io.vespucci.util.NetworkStatus;

/**
 * Ask the user for input Supported are coordinates or an OLC for example WF8Q+WF Praia, Cabo Verde, or an geo: Uri
 * 
 * @author simon
 *
 */
public class GeocodeInput {

    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, GeocodeInput.class.getSimpleName().length());
    protected static final String DEBUG_TAG = GeocodeInput.class.getSimpleName().substring(0, TAG_LEN);

    private static final Pattern OLC_SHORT = Pattern.compile("^([23456789CFGHJMPQRVWX]{4,6}\\+[23456789CFGHJMPQRVWX]{2,3})\\s*(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern OLC_FULL  = Pattern.compile("^([23456789C][23456789CFGHJMPQRV][23456789CFGHJMPQRVWX]{6}\\+[23456789CFGHJMPQRVWX]{2,3})(\\s|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

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
     * @param context the calling FragmentActivity
     * @param handler a handler for the results
     */
    public static void get(@NonNull final Context context, @NonNull final HandleResult handler) {
        dialog = TextLineDialog.get(context, R.string.go_to_coordinates_title, R.string.go_to_coordinates_hint, (input, check) -> {
            String text = input.getText().toString().trim();
            if ("".equals(text)) {
                return;
            }
            parse(context, handler, text);
        }, false);
        dialog.show();
    }

    /**
     * Parse the entered string either as a coordinate tupel or an OLC
     * 
     * @param context an Android Context
     * @param handler a Handler to use for success and error
     * @param text the user provided text
     */
    private static void parse(@NonNull final Context context, @NonNull final HandleResult handler, @NonNull String text) {
        new ExecutorTask<String, Void, LatLon>() {
            @Override
            protected LatLon doInBackground(String param) {
                try {
                    return CoordinateParser.parseVerbatimCoordinates(text);
                } catch (ParseException pex) {
                    try {
                        return parseOLC(context, handler, text);
                    } catch (Exception e) {
                        try {
                            Uri uri = Uri.parse(text);
                            if (Schemes.GEO.equals(uri.getScheme())) {
                                return GeoUriData.parse(uri.getSchemeSpecificPart()).getLatLon();
                            }
                        } catch (Exception e2) {
                            Log.e(DEBUG_TAG, e.getMessage());
                            handler.onError(context.getString(R.string.unparseable_coordinates));
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(LatLon result) {
                if (result != null) {
                    handler.onSuccess(result);
                    dismiss();
                }
            }
        }.execute(text);
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
     * Parse an OLC (code)
     * 
     * @param context an android context
     * @param handler handler for errors
     * @param text the input text
     * @return a LatLon object
     * @throws IOException on any kind of errror
     */
    private static LatLon parseOLC(@NonNull final Context context, @NonNull final HandleResult handler, @NonNull String text) throws IOException {
        OpenLocationCode olc = null;
        Matcher m = OLC_FULL.matcher(text);
        if (m.find()) {
            olc = new OpenLocationCode(m.group(1));
        } else {
            m = OLC_SHORT.matcher(text);
            if (m.find()) {
                olc = new OpenLocationCode(m.group(1));
                final String loc = m.group(2);
                if (!"".equals(loc)) { // user has supplied a location
                    olc = recoverLocation(context, handler, olc, loc);
                } else { // relative to screen center
                    ViewBox box = App.getLogic().getViewBox();
                    if (box != null) {
                        double[] c = box.getCenter();
                        olc = olc.recover(c[1], c[0]);
                    }
                }
            }
        }
        if (olc == null) {
            throw new IOException("Unparseable OLC " + text);
        }
        CodeArea ca = olc.decode();
        return new LatLon(ca.getCenterLatitude(), ca.getCenterLongitude());
    }

    /**
     * Recover coordinates for a location from Nominatim and update the provided OLC
     * 
     * @param context an Android Context
     * @param handler handler to use for errors
     * @param olc original OLC
     * @param loc location
     * @return possibly a new OLC with the location set
     */
    @NonNull
    private static OpenLocationCode recoverLocation(@NonNull final Context context, @NonNull final HandleResult handler, @NonNull OpenLocationCode olc,
            @NonNull final String loc) {
        if (new NetworkStatus(context).isConnected()) {
            String url = QueryNominatim.getNominatimUrl(context);
            if (url != null) {
                QueryNominatim querier = new QueryNominatim(null, url, null, false);
                querier.execute(loc);
                List<SearchResult> results;
                try {
                    results = querier.get(5, TimeUnit.SECONDS);
                    if (results != null && !results.isEmpty()) {
                        SearchResult result = results.get(0);
                        olc = olc.recover(result.getLat(), result.getLon());
                    } else {
                        handler.onError(context.getString(R.string.no_nominatim_result, loc));
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
                    querier.cancel();
                    handler.onError(context.getString(R.string.no_nominatim_result, loc));
                }
            } else {
                handler.onError(context.getString(R.string.no_nominatim_server));
            }
        } else {
            handler.onError(context.getString(R.string.network_required));
        }
        return olc;
    }
}
