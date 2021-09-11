package de.blau.android.dialogs;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.TreeMap;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.osm.Node;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.TrackerService;
import de.blau.android.services.util.ExtendedLocation;
import de.blau.android.util.GeoMath;
import de.blau.android.util.InfoDialogFragment;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Very simple dialog fragment to display some info on a GeoJSON element
 * 
 * @author simon
 *
 */
public class GnssPositionInfo extends InfoDialogFragment {

    private static final String DEBUG_TAG = GnssPositionInfo.class.getName();

    private static final String LOCATION_KEY = "location";

    private static final String TAG = "fragment_feature_info";

    private Location location = null;

    TableLayout.LayoutParams tp;
    TableLayout              tl;

    /**
     * Show an info dialog for the supplied Location
     * 
     * @param activity the calling Activity
     * @param location the Location we want to display
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull Location location) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            GnssPositionInfo elementInfoFragment = newInstance(location);
            elementInfoFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Show an info dialog for the supplied GeoJSON Feature
     * 
     * @param activity the calling Activity
     * @param tracker the current TrackerService instance
     */
    public static void showDialog(@NonNull FragmentActivity activity, @Nullable TrackerService tracker) {
        dismissDialog(activity);
        Location location = null;
        try {
            // try our sources first
            if (tracker != null) {
                Location tempLocation = tracker.getLastLocation();
                if (tempLocation != null) {
                    String provider = tempLocation.getProvider();
                    if (provider.equals(activity.getString(R.string.gps_source_nmea)) || provider.equals(LocationManager.GPS_PROVIDER)) {
                        location = tempLocation;
                    }
                }
            }
            if (location == null) {
                LocationManager locationManager = (LocationManager) activity.getSystemService(android.content.Context.LOCATION_SERVICE);
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        } catch (SecurityException sex) {
            // can be safely ignored, this is only called when GPS is enabled
        }
        if (location != null) {
            GnssPositionInfo.showDialog(activity, location);
        } else {
            Snack.toastTopError(activity, R.string.toast_no_usable_location);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new instance of the GnssPositionInfo dialog
     * 
     * @param location the Location we want to display
     * @return an instance of GnssPositionInfo
     */
    @NonNull
    private static GnssPositionInfo newInstance(@NonNull Location location) {
        GnssPositionInfo f = new GnssPositionInfo();

        Bundle args = new Bundle();
        args.putParcelable(LOCATION_KEY, location);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            location = savedInstanceState.getParcelable(LOCATION_KEY);
            Log.d(DEBUG_TAG, "restoring from saved state");
        } else {
            location = getArguments().getParcelable(LOCATION_KEY);
        }
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
        if (location != null) {
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            nf.setMaximumFractionDigits(8);
            nf.setRoundingMode(RoundingMode.HALF_UP);
            try {
                final double lon = Double.parseDouble(nf.format(location.getLongitude()));
                final double lat = Double.parseDouble(nf.format(location.getLatitude()));

                builder.setNeutralButton(R.string.share_position, (dialog, which) -> {
                    double[] lonLat = new double[2];
                    lonLat[0] = lon;
                    lonLat[1] = lat;
                    Util.sharePosition(getActivity(), lonLat, null);
                });

                builder.setNegativeButton(R.string.menu_newnode_gps, (dialog, which) -> {
                    if (Util.notZero(lon) || Util.notZero(lat)) {
                        if (GeoMath.coordinatesInCompatibleRange(lon, lat)) {
                            addNodeAtLocation(lon, lat);
                        } else {
                            Snack.barError(getActivity(), R.string.toast_null_island);
                        }
                    }
                });
            } catch (NumberFormatException nfex) {
                Log.e(DEBUG_TAG, nfex.getMessage());
            }
        }
        builder.setTitle(R.string.position_info_title);
        builder.setView(createView(null));
        return builder.create();
    }

    /**
     * Add a new Node at the specified coordinates
     * 
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     */
    private void addNodeAtLocation(final double lon, final double lat) {
        try {
            Logic logic = App.getLogic();
            Node node = logic.performAddNode(getActivity(), lon, lat);
            TreeMap<String, String> tags = new TreeMap<>(node.getTags());

            if (location instanceof ExtendedLocation) {
                ExtendedLocation loc = (ExtendedLocation) location;
                Preferences prefs = logic.getPrefs();
                if (loc.hasBarometricHeight()) {
                    if (prefs.useBarometricHeight()) {
                        tags.put(Tags.KEY_ELE, String.format(Locale.US, "%.3f", loc.getBarometricHeight()));
                        tags.put(Tags.KEY_SOURCE_ELE, Tags.VALUE_BAROMETER);
                    }
                    tags.put(Tags.KEY_ELE_BAROMETRIC, String.format(Locale.US, "%.3f", loc.getBarometricHeight()));
                }
                if (loc.hasGeoidHeight()) {
                    if (!prefs.useBarometricHeight()) {
                        tags.put(Tags.KEY_ELE, String.format(Locale.US, "%.3f", loc.getGeoidHeight()));
                        tags.put(Tags.KEY_SOURCE, Tags.VALUE_GNSS);
                    }
                    tags.put(Tags.KEY_ELE_GEOID, String.format(Locale.US, "%.3f", loc.getGeoidHeight()));
                }
                if (loc.hasGeoidCorrection()) {
                    tags.put("note:ele", "geoid correction " + String.format(Locale.US, "%.3f", loc.getGeoidCorrection()));
                }
                if (loc.hasHdop()) {
                    tags.put(Tags.KEY_GNSS_HDOP, String.format(Locale.US, "%.1f", loc.getHdop()));
                }
            }
            if (location != null && location.hasAltitude()) { // location may have gone
                tags.put(Tags.KEY_ELE_ELLIPSOID, String.format(Locale.US, "%.3f", location.getAltitude()));
            }

            logic.setTags(getActivity(), node, tags);
            if (getActivity() instanceof Main) {
                ((Main) getActivity()).edit(node);
            }
        } catch (OsmIllegalOperationException e) {
            Snack.barError(getActivity(), e.getLocalizedMessage());
            Log.d(DEBUG_TAG, "Caught exception " + e);
        }
    }

    Runnable update = new Runnable() { // NOSONAR

        @Override
        public void run() {
            FragmentActivity activity = getActivity();
            if (activity instanceof Main && ((Main) activity).getTracker() != null) {
                location = ((Main) activity).getTracker().getLastLocation();
                if (location != null) {
                    updateView(activity, tl, tp);
                }
                tl.postDelayed(update, 1000);
            }
        }
    };

    @Override
    protected View createView(@Nullable ViewGroup container) {
        LayoutInflater inflater;
        FragmentActivity activity = getActivity();
        inflater = ThemeUtils.getLayoutInflater(activity);
        ScrollView sv = (ScrollView) inflater.inflate(R.layout.element_info_view, container, false);
        tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);

        tp = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);

        if (location != null) {
            tl.setColumnShrinkable(1, true);
            updateView(activity, tl, tp);
            tl.postDelayed(update, 1000);
        }
        return sv;
    }

    /**
     * Create/update the contents of the table layout
     * 
     * @param activity calling activity
     * @param tl the TableLAyout
     * @param tp layout params
     */
    private void updateView(@NonNull FragmentActivity activity, @NonNull TableLayout tl, @NonNull TableLayout.LayoutParams tp) {
        tl.removeAllViews();
        if (location != null) {
            tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lat_label, String.format(Locale.US, "%.8f", location.getLatitude()), tp));
            tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lon_label, String.format(Locale.US, "%.8f", location.getLongitude()), tp));
            tl.addView(TableLayoutUtils.divider(activity));

            if (location instanceof ExtendedLocation) {
                ExtendedLocation loc = (ExtendedLocation) location;
                if (loc.hasHdop()) {
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.position_info_hdop, String.format(Locale.US, "%.1f", loc.getHdop()), tp));
                }
                tl.addView(TableLayoutUtils.createRow(activity, R.string.position_info_altitude, null, tp));
                if (loc.hasGeoidHeight()) {
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.position_info_geoid_height, String.format(Locale.US, "%.3f", loc.getGeoidHeight()),
                            tp));
                }
                if (loc.hasBarometricHeight()) {
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.position_info_barometric_height,
                            String.format(Locale.US, "%.3f", loc.getBarometricHeight()), tp));
                }
                if (loc.hasGeoidCorrection()) {
                    tl.addView(TableLayoutUtils.createRow(activity, R.string.position_info_geoid_correction,
                            String.format(Locale.US, "%.3f", loc.getGeoidCorrection()), tp));
                }
            }
            if (location.hasAltitude()) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.position_info_ellipsoid_height,
                        String.format(Locale.US, "%.3f", location.getAltitude()), tp));
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(LOCATION_KEY, location);
    }
}
