package de.blau.android;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.exception.OsmException;
import de.blau.android.geocode.Search;
import de.blau.android.geocode.Search.SearchResult;
import de.blau.android.geocode.SearchItemSelectedCallback;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.AdvancedPrefDatabase.Geocoder;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.BugFixedAppCompatActivity;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Util;

/**
 * Activity in which the user can pick a Location and a radius (more precisely: a square with "radius" as half of the
 * edge length.
 * 
 * This class will return valid geo boundaries for a {@link BoundingBox} as extra data. ResultType will be RESULT_OK
 * when the {@link BoundingBox} should be loaded from a OSM Server, otherwise RESULT_CANCEL.<br>
 * This class acts as its own LocationListener: We will offers the best location to the user.
 * 
 * @author mb
 * @author Simon Poole
 */
public class BoxPicker extends BugFixedAppCompatActivity implements LocationListener, SearchItemSelectedCallback {

    private static final int MINUTE_SECONDS = 60;

    private static final int HOUR_SECONDS = 3600;

    private static final int DAY_SECONDS = 24 * 3600;

    /**
     * Tag used for Android-logging.
     */
    private static final String DEBUG_TAG = BoxPicker.class.getName();

    /**
     * LocationManager. Needed as field for unregister in {@link #onPause()}.
     */
    private LocationManager locationManager = null;

    /**
     * The current location with the best accuracy.
     */
    private Location currentLocation = null;

    /**
     * The user-chosen radius by the SeekBar. Value in Meters.
     */
    private int currentRadius = 0;

    /**
     * Last known location.
     */
    private Location lastLocation = null;

    /**
     * Tag for Intent extras.
     */
    public static final String RESULT_LEFT = "de.blau.android.BoxPicker.left";

    /**
     * Tag for Intent extras.
     */
    public static final String RESULT_BOTTOM = "de.blau.android.BoxPicker.bottom";

    /**
     * Tag for Intent extras.
     */
    public static final String RESULT_RIGHT = "de.blau.android.BoxPicker.right";

    /**
     * Tag for Intent extras.
     */
    public static final String RESULT_TOP = "de.blau.android.BoxPicker.top";

    private static final int MIN_WIDTH = 50;

    private static final String TITLE_KEY = "title";

    Button   loadMapButton;
    EditText latEdit;
    EditText lonEdit;

    /**
     * Start this activity
     * 
     * @param activity the calling Activity
     * @param titleResId resources id for the title
     * @param requestCode request code for the result
     */
    public static void startForResult(@NonNull Activity activity, int titleResId, int requestCode) {
        Log.d(DEBUG_TAG, "startForResult");
        Intent intent = new Intent(activity, BoxPicker.class);

        intent.putExtra(TITLE_KEY, Integer.valueOf(titleResId));
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Registers some listeners, sets the content view and initialize {@link #currentRadius}. {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customLight);
        }

        super.onCreate(savedInstanceState);

        int title = R.string.boxpicker_firsttimetitle;
        Serializable arg = getIntent().getSerializableExtra(TITLE_KEY);
        if (arg instanceof Integer) {
            title = (Integer) arg;
        }

        setContentView(R.layout.location_picker_view);

        // Load Views
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.location_type_group);
        loadMapButton = (Button) findViewById(R.id.location_button_current);
        Button dontLoadMapButton = (Button) findViewById(R.id.location_button_no_location);
        latEdit = (EditText) findViewById(R.id.location_lat_edit);
        lonEdit = (EditText) findViewById(R.id.location_lon_edit);
        EditText searchEdit = (EditText) findViewById(R.id.location_search_edit);
        SeekBar seeker = (SeekBar) findViewById(R.id.location_radius_seeker);

        currentRadius = seeker.getProgress();

        // register listeners
        seeker.setOnSeekBarChangeListener(createSeekBarListener());
        radioGroup.setOnCheckedChangeListener(createRadioGroupListener(loadMapButton, latEdit, lonEdit));
        OnClickListener onClickListener = createButtonListener(radioGroup, latEdit, lonEdit);
        loadMapButton.setOnClickListener(onClickListener);
        dontLoadMapButton.setOnClickListener(onClickListener);

        // the following shares a lot of code with SearchForm, but is unluckily slightly different
        final Spinner searchGeocoder = (Spinner) findViewById(R.id.location_search_geocoder);
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(this)) {
            final Geocoder[] geocoders = db.getActiveGeocoders();
            String[] geocoderNames = new String[geocoders.length];
            for (int i = 0; i < geocoders.length; i++) {
                geocoderNames[i] = geocoders[i].name;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, geocoderNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            searchGeocoder.setAdapter(adapter);
            int geocoderIndex = prefs.getGeocoder();
            // if a non-active geocoder is selected revert to default
            if (geocoderIndex > adapter.getCount() - 1) {
                geocoderIndex = 0;
                prefs.setGeocoder(geocoderIndex);
            }
            searchGeocoder.setSelection(geocoderIndex);
            searchGeocoder.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                    prefs.setGeocoder(pos);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // required by OnItemSelectedListener but not needed
                }
            });

            searchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH
                            || (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        Search search = new Search(BoxPicker.this, null, BoxPicker.this);
                        search.find(geocoders[searchGeocoder.getSelectedItemPosition()], v.getText().toString(), null, false);
                        return true;
                    }
                    return false;
                }
            });

            ActionBar actionbar = getSupportActionBar();
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle(title);
        }
    }

    @Override
    public void onItemSelected(SearchResult sr) {
        RadioButton rb = (RadioButton) findViewById(R.id.location_coordinates);
        rb.setChecked(true); // note potential race condition with setting the lat/lon
        LinearLayout coordinateView = (LinearLayout) findViewById(R.id.location_coordinates_layout);
        coordinateView.setVisibility(View.VISIBLE);
        loadMapButton.setEnabled(true);
        latEdit.setText(Double.toString(sr.getLat()));
        lonEdit.setText(Double.toString(sr.getLon()));
    }

    @Override
    protected void onPause() {
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException sex) {
            // can be safely ignored
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Location l = registerLocationListener();
        if (l != null) {
            lastLocation = l;
        }
        setLocationRadioButton(R.id.location_last, R.string.location_last_text_parameterized, lastLocation, null);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Util.clearCaches(this, newConfig);
    }

    /**
     * Registers this class for location updates from all available location providers.
     * 
     * @return the best current Location
     */
    private Location registerLocationListener() {
        Preferences prefs = new Preferences(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            try {
                locationManager.requestLocationUpdates(provider, prefs.getGpsInterval(), prefs.getGpsDistance(), this);
                Location location = locationManager.getLastKnownLocation(provider);
                if (bestLocation == null || !bestLocation.hasAccuracy()
                        || (location != null && location.hasAccuracy() && location.getAccuracy() < bestLocation.getAccuracy())) {
                    bestLocation = location;
                }
            } catch (IllegalArgumentException | SecurityException e) {
                Log.d(DEBUG_TAG, "registerLocationListener got " + e.getMessage());
            }
        }
        return bestLocation;
    }

    /**
     * As soon as the user checks one of the radio buttons, the "load/don't load"-buttons will be enabled. Additionally,
     * the lat/lon-EditTexts will be visible/invisible when the user chooses to insert the coordinate manually.
     * 
     * @param loadMapButton the "Load!"-button.
     * @param latEdit latitude EditText.
     * @param lonEdit longitude EditText.
     * @return the new created listener.
     */
    private OnCheckedChangeListener createRadioGroupListener(final Button loadMapButton, final EditText latEdit, final EditText lonEdit) {
        return new OnCheckedChangeListener() {

            /**
             * Set the EditTexts to the coordinates of a Location
             * 
             * @param location the Location
             * @param latEdit latitude EditText
             * @param lonEdit longitude EditText
             */
            private void setmanualCoordinates(@NonNull Location location, @NonNull final EditText latEdit, @NonNull final EditText lonEdit) {
                latEdit.setText(String.format(Locale.US, "%4g", location.getLatitude()));
                lonEdit.setText(String.format(Locale.US, "%4g", location.getLongitude()));
            }

            @Override
            public void onCheckedChanged(final RadioGroup group, final int checkedId) {
                LinearLayout coordinateView = (LinearLayout) findViewById(R.id.location_coordinates_layout);
                loadMapButton.setEnabled(true);
                if (checkedId == R.id.location_coordinates) {
                    coordinateView.setVisibility(View.VISIBLE);
                    // don't overwrite existing values...
                    if (latEdit.getText().length() == 0 && lonEdit.getText().length() == 0) {
                        if (currentLocation != null) {
                            setmanualCoordinates(currentLocation, latEdit, lonEdit);
                        } else if (lastLocation != null) {
                            setmanualCoordinates(lastLocation, latEdit, lonEdit);
                        }
                    }
                } else {
                    coordinateView.setVisibility(View.GONE);
                }
            }

        };
    }

    /**
     * First, the minimum radius will be assured, second, the {@link #currentRadius} will be set and the label will be
     * updated.
     * 
     * @return the new created listener.
     */
    private OnSeekBarChangeListener createSeekBarListener() {
        return new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, int progress, final boolean fromTouch) {
                if (progress < MIN_WIDTH) {
                    progress = MIN_WIDTH;
                }
                currentRadius = progress;
                TextView radiusText = (TextView) findViewById(R.id.location_radius_text);
                radiusText.setText(getString(R.string.location_radius, progress));
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
                // required by OnSeekBarChangeListener but not needed
            }

            @Override
            public void onStopTrackingTouch(final SeekBar arg0) {
                // required by OnSeekBarChangeListener but not needed
            }
        };
    }

    /**
     * Reads the manual coordinate EditTexts and registers the button listeners.
     * 
     * @param radioGroup the RadioGroup with the buttons
     * @param latEdit Manual Latitude EditText.
     * @param lonEdit Manual Longitude EditText.
     * @return the OnClickListener
     */
    private OnClickListener createButtonListener(@NonNull final RadioGroup radioGroup, @NonNull final EditText latEdit, @NonNull final EditText lonEdit) {
        return new OnClickListener() {
            @Override
            public void onClick(final View view) {
                String lat = latEdit.getText().toString();
                String lon = lonEdit.getText().toString();
                performClick(view.getId(), radioGroup.getCheckedRadioButtonId(), lat, lon);
            }
        };
    }

    /**
     * Do the action when the user clicks a Button. Generates the {@link BoundingBox} from the coordinate and chosen
     * radius, sets the resultType (RESULT_OK when a map should be loaded, otherwise false) and calls
     * {@link #sendResultAndExit(BoundingBox, int)}
     * 
     * @param buttonId android-id from the clicked Button.
     * @param checkedRadioButtonId android-id from the checked RadioButton.
     * @param lat latitude from the EditText.
     * @param lon longitude from the EditText.
     */
    private void performClick(final int buttonId, final int checkedRadioButtonId, @NonNull final String lat, @NonNull final String lon) {
        BoundingBox box = null;
        int resultState = (buttonId == R.id.location_button_current) ? RESULT_OK : RESULT_CANCELED;

        switch (checkedRadioButtonId) { // NOSONAR
        case R.id.location_current:
            box = createBoxForLocation(currentLocation);
            break;
        case R.id.location_last:
            box = createBoxForLocation(lastLocation);
            break;
        case R.id.location_coordinates:
            box = createBoxForManualLocation(lat, lon);
            break;
        }

        if (box != null) {
            sendResultAndExit(box, resultState);
        } else {
            finish();
        }
    }

    /**
     * Create a bounding box for a Location
     * 
     * @param location the Location that we want to create a BoundingBox around
     * @return {@link BoundingBox} for {@link #currentLocation} and {@link #currentRadius}
     */
    @Nullable
    private BoundingBox createBoxForLocation(@NonNull Location location) {
        try {
            return GeoMath.createBoundingBoxForCoordinates(location.getLatitude(), location.getLongitude(), currentRadius, true);
        } catch (OsmException e) {
            ACRAHelper.nocrashReport(e, e.getMessage());
        }
        return null;
    }

    /**
     * Tries to parse lat and lon and creates a new {@link BoundingBox} if successful.
     * 
     * @param lat manual latitude
     * @param lon manual longitude
     * @return {@link BoundingBox} for lat, lon and {@link #currentRadius}
     */
    private BoundingBox createBoxForManualLocation(@NonNull final String lat, @NonNull final String lon) {
        BoundingBox box = null;
        try {
            float userLat = Float.parseFloat(lat);
            float userLon = Float.parseFloat(lon);
            box = GeoMath.createBoundingBoxForCoordinates(userLat, userLon, currentRadius, true);
        } catch (NumberFormatException | OsmException e) {
            ErrorAlert.showDialog(this, ErrorCodes.NAN);
        }
        return box;
    }

    /**
     * Creates the {@link Intent} with the boundaries of box as extra data.
     * 
     * @param box the box with the chosen boundaries.
     * @param resultState RESULT_OK when the map should be loaded, otherwise RESULT_CANCEL.
     */
    private void sendResultAndExit(final BoundingBox box, final int resultState) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_LEFT, box.getLeft());
        intent.putExtra(RESULT_BOTTOM, box.getBottom());
        intent.putExtra(RESULT_RIGHT, box.getRight());
        intent.putExtra(RESULT_TOP, box.getTop());
        setResult(resultState, intent);
        finish();
    }

    /**
     * When a location was found which has more accuracy than {@link #currentLocation}, then the newLocation will be set
     * as currentLocation.<br>
     * {@inheritDoc}
     */
    @Override
    public void onLocationChanged(final Location newLocation) {
        Log.w(DEBUG_TAG, "Got location: " + newLocation);
        if (newLocation != null && isNewLocationMoreAccurate(newLocation)) {
            currentLocation = newLocation;
            setLocationRadioButton(R.id.location_current, R.string.location_current_text_parameterized, newLocation, null);
            if (lastLocation != null) {
                setLocationRadioButton(R.id.location_last, R.string.location_last_text_parameterized, newLocation, lastLocation);
            }
        }
    }

    /**
     * Set the text of a location (last or current) radio button.
     * 
     * @param buttonId The resource ID of the radio button.
     * @param textId The resource ID of the button text.
     * @param location The location data to update the button text (may be null).
     * @param lastLocation The last location
     */
    private void setLocationRadioButton(final int buttonId, final int textId, @Nullable final Location location, @Nullable Location lastLocation) {
        String locationMetaData = getString(R.string.location_text_unknown);
        if (location != null) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();

            String locationString = "";
            if (location.hasAccuracy()) {
                locationString = getString(R.string.location_text_metadata_accuracy, location.getAccuracy());
            }
            String accuracyMetaData = getString(R.string.location_text_metadata, locationString, location.getProvider());

            long fixTime = Math.max(0, (System.currentTimeMillis() - location.getTime()) / 1000);
            // slightly hackish
            String fixString = "";
            if (fixTime > DAY_SECONDS) {
                fixString = getQuantityString(R.plurals.days, (int) (fixTime / DAY_SECONDS));
            } else if (fixTime > HOUR_SECONDS) {
                fixString = getQuantityString(R.plurals.hours, (int) (fixTime / HOUR_SECONDS));
            } else if (fixTime > MINUTE_SECONDS) {
                fixString = getQuantityString(R.plurals.minutes, (int) (fixTime / MINUTE_SECONDS));
            } else if (fixTime >= 0) {
                fixString = getQuantityString(R.plurals.seconds, (int) fixTime);
            }
            if (lastLocation != null && currentLocation != null) { // add distance from old location
                int fixDistance = Math.round(lastLocation.distanceTo(currentLocation));
                if (fixDistance > 1000) {
                    fixString = getQuantityString(R.plurals.km, (int) Math.round(fixDistance / 1000.0)) + " " + fixString;
                } else if (fixDistance >= 0) {
                    fixString = getQuantityString(R.plurals.meters, fixDistance) + " " + fixString;
                }
            }
            locationMetaData = getString(R.string.location_text_metadata_location, lat, lon, accuracyMetaData, fixString);
        }
        RadioButton rb = (RadioButton) findViewById(buttonId);
        rb.setEnabled(location != null);
        rb.setText(getString(textId, locationMetaData));
    }

    /**
     * Get a quantity string from resources
     * 
     * @param res the plurals resources id
     * @param number the number to display
     * @return a String with the correct plural and placeholder replaced
     */
    String getQuantityString(int res, int number) {
        return getResources().getQuantityString(res, number, number);
    }

    /**
     * Checks if the new location is more accurate than {@link #currentLocation}.
     * 
     * @param newLocation new location
     * @return true, if the new location is more accurate than the old one or one of them has no accuracy anyway.
     */
    private boolean isNewLocationMoreAccurate(final Location newLocation) {
        return currentLocation == null || !newLocation.hasAccuracy() || !currentLocation.hasAccuracy()
                || newLocation.getAccuracy() <= currentLocation.getAccuracy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProviderDisabled(final String provider) {
        // required by LocationListener but not needed
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProviderEnabled(final String provider) {
        // required by LocationListener but not needed
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStatusChanged(final String provider, final int status, final Bundle extras) {
        // required by LocationListener but not needed
    }
}
