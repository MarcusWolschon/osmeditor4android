package de.blau.android;

import java.util.List;

import org.acra.ACRA;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.GeoMath;

/**
 * Activity where the user can pick a Location and a radius (more precisely: a
 * square with "radius" as half of the edge length. This class will return
 * valid geo boundaries for a {@link BoundingBox} as extra data. ResultType
 * will be RESULT_OK when the {@link BoundingBox} should be loaded from a OSM
 * Server, otherwise RESULT_CANCEL.<br>
 * This class acts as its own LocationListener: We will offers the best
 * location to the user.
 * 
 * @author mb
 */
public class BoxPicker extends SherlockActivity implements LocationListener {
	
	/**
	 * Tag used for Android-logging.
	 */
	private final static String DEBUG_TAG = BoxPicker.class.getName();
	
	/**
	 * Shown when the user inserts an invalid decimal number.
	 */
	private final static int DIALOG_NAN = 0;
	
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
	
	/**
	 * Registers some listeners, sets the content view and initialize
	 * {@link #currentRadius}.</br> {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_picker_view);
		
		//Load Views
		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.location_type_group);
		// Button loadMapButton = (Button) findViewById(R.id.location_button_current);
		// Button dontLoadMapButton = ((Button) findViewById(R.id.location_button_no_location));
		EditText latEdit = (EditText) findViewById(R.id.location_lat_edit);
		EditText lonEdit = (EditText) findViewById(R.id.location_lon_edit);
		SeekBar seeker = (SeekBar) findViewById(R.id.location_radius_seeker);
		
		currentRadius = seeker.getProgress();
		
		//register listeners
		seeker.setOnSeekBarChangeListener(createSeekBarListener());
		radioGroup.setOnCheckedChangeListener(createRadioGroupListener(null /* loadMapButton */, null /* dontLoadMapButton */, latEdit,
			lonEdit));
		OnClickListener onClickListener = createButtonListener(radioGroup, latEdit, lonEdit);
		// loadMapButton.setOnClickListener(onClickListener);
		// dontLoadMapButton.setOnClickListener(onClickListener);
		
		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
	}
	
	
	@Override
	protected void onPause() {
		locationManager.removeUpdates(this);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Location l = registerLocationListener();
		if (l != null) {
			lastLocation = l;
		}
		setLocationRadioButton(R.id.location_last, R.string.location_last_text_parameterized, lastLocation);
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Registers this class for location updates from all available location
	 * providers.
	 */
	private Location registerLocationListener() {
		Preferences prefs = new Preferences(this);
		locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = locationManager.getProviders(true);
		Location bestLocation = null;
		for (String provider : providers) {
			try {
				locationManager.requestLocationUpdates(provider,
						prefs.getGpsInterval(), prefs.getGpsDistance(), this);
				Location location = locationManager.getLastKnownLocation(provider);
				if (bestLocation == null || !bestLocation.hasAccuracy() ||
						(location != null && location.hasAccuracy() &&
								location.getAccuracy() < bestLocation.getAccuracy())) {
					bestLocation = location;
				}
			} catch (IllegalArgumentException e) {
			} catch (SecurityException e) {
			}
		}
		return bestLocation;
	}
	
	/**
	 * As soon as the user checks one of the radio buttons, the "load/don't
	 * load"-buttons will be enabled. Additionally, the lat/lon-EditTexts will
	 * be visible/invisible when the user chooses to insert the coordinate
	 * manually.
	 * 
	 * @param loadMapButton the "Load!"-button.
	 * @param dontLoadMapButton the "Don't load anything"-button.
	 * @param latEdit latitude EditText.
	 * @param lonEdit longitude EditText.
	 * @return the new created listener.
	 */
	private OnCheckedChangeListener createRadioGroupListener(final Button loadMapButton,
			final Button dontLoadMapButton, final EditText latEdit, final EditText lonEdit) {
		return new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final RadioGroup group, final int checkedId) {
				LinearLayout coordinateView = (LinearLayout) findViewById(R.id.location_coordinates_layout);
				// loadMapButton.setEnabled(true);
				// dontLoadMapButton.setEnabled(true);
				if (checkedId == R.id.location_coordinates) {
					coordinateView.setVisibility(View.VISIBLE);
					if (currentLocation != null) {
						latEdit.setText(Double.toString(currentLocation.getLatitude()));
						lonEdit.setText(Double.toString(currentLocation.getLongitude()));
					} else if (lastLocation != null) {
						latEdit.setText(Double.toString(lastLocation.getLatitude()));
						lonEdit.setText(Double.toString(lastLocation.getLongitude()));
					}
				} else {
					coordinateView.setVisibility(View.GONE);
				}
			}
		};
	}
	
	/**
	 * First, the minimum radius will be assured, second, the
	 * {@link #currentRadius} will be set and the label will be updated.
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
				radiusText.setText(" " + progress + "m");
			}
			
			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
			}
			
			@Override
			public void onStopTrackingTouch(final SeekBar arg0) {
			}
		};
	}
	
	/**
	 * Reads the manual coordinate EditTexts and registers the button
	 * listeners.
	 * 
	 * @param radioGroup
	 * @param latEdit Manual Latitude EditText.
	 * @param lonEdit Manual Longitude EditText.
	 */
	private OnClickListener createButtonListener(final RadioGroup radioGroup, final EditText latEdit,
			final EditText lonEdit) {
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
	 * Do the action when the user clicks a Button. Generates the
	 * {@link BoundingBox} from the coordinate and chosen radius, sets the
	 * resultType (RESULT_OK when a map should be loaded, otherwise false) and
	 * calls {@link #sendResultAndExit(BoundingBox, int)}
	 * 
	 * @param buttonId android-id from the clicked Button.
	 * @param checkedRadioButtonId android-id from the checked RadioButton.
	 * @param lat latitude from the EditText.
	 * @param lon longitude from the EditText.
	 */
	private void performClick(final int buttonId, final int checkedRadioButtonId, final String lat, final String lon) {
		BoundingBox box = null;
		int resultState = (buttonId == R.id.location_button_current) ? RESULT_OK : RESULT_CANCELED;
		
		switch (checkedRadioButtonId) {
		case R.id.location_current:
			box = createBoxForCurrentLocation();
			break;
		case R.id.location_last:
			box = createBoxForLastLocation();
			break;
		case R.id.location_coordinates:
			box = createBoxForManualLocation(lat, lon);
			break;
		}
		
		if (box != null) {
			sendResultAndExit(box, resultState);
		}
	}

	/**
	 * @return {@link BoundingBox} for {@link #currentLocation} and
	 * {@link #currentRadius}
	 */
	private BoundingBox createBoxForCurrentLocation() {
		BoundingBox box = null;
		try {
			box = GeoMath.createBoundingBoxForCoordinates(currentLocation.getLatitude(),
				currentLocation.getLongitude(), currentRadius);
		} catch (OsmException e) {
			ACRA.getErrorReporter().handleException(e);
		}
		return box;
	}
	
	/**
	 * @return {@link BoundingBox} for {@link #lastLocation} and
	 * {@link #currentRadius}
	 */
	private BoundingBox createBoxForLastLocation() {
		BoundingBox box = null;
		try {
			box = GeoMath.createBoundingBoxForCoordinates(lastLocation.getLatitude(),
					lastLocation.getLongitude(), currentRadius);
		} catch (OsmException e) {
			ACRA.getErrorReporter().handleException(e);
		}
		return box;
	}
	
	/**
	 * Tries to parse lat and lon and creates a new {@link BoundingBox} if
	 * successful.
	 * 
	 * @param lat manual latitude
	 * @param lon manual longitude
	 * @return {@link BoundingBox} for lat, lon and {@link #currentRadius}
	 */
	private BoundingBox createBoxForManualLocation(final String lat, final String lon) {
		BoundingBox box = null;
		try {
			float userLat = Float.parseFloat(lat);
			float userLon = Float.parseFloat(lon);
			box = GeoMath.createBoundingBoxForCoordinates(userLat, userLon, currentRadius);
		} catch (NumberFormatException e) {
			showDialog(DIALOG_NAN);
		} catch (OsmException e) {
			showDialog(DIALOG_NAN);
		}
		return box;
	}
	
	/**
	 * Creates the {@link Intent} with the boundaries of box as extra data.
	 * 
	 * @param box the box with the chosen boundaries.
	 * @param resultState RESULT_OK when the map should be loaded, otherwise
	 * RESULT_CANCEL.
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
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(final int id) {
		switch (id) {
		case DIALOG_NAN:
			return createDialogNan();
		}
		return super.onCreateDialog(id);
	}
	
	/**
	 * @see #DIALOG_NAN
	 * @return
	 */
	private AlertDialog createDialogNan() {
		Builder dialog = new AlertDialog.Builder(this);
		dialog.setIcon(R.drawable.alert_dialog_icon);
		dialog.setTitle(R.string.location_nan_title);
		dialog.setMessage(R.string.location_nan_message);
		dialog.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int whichButton) {}
		});
		return dialog.create();
	}
	
	/**
	 * When a location was found which has more accuracy than
	 * {@link #currentLocation}, then the newLocation will be set as
	 * currentLocation.<br>
	 * {@inheritDoc}
	 */
	@Override
	public void onLocationChanged(final Location newLocation) {
		Log.w(DEBUG_TAG, "Got location: " + newLocation);
		if (newLocation != null) {
			if (isNewLocationMoreAccurate(newLocation)) {
				setLocationRadioButton(R.id.location_current, R.string.location_current_text_parameterized, newLocation);
				currentLocation = newLocation;
			}
		}
	}
	
	/**
	 * Set the text of a location (last or current) radio button.
	 * @param buttonId The resource ID of the radio button.
	 * @param textId The resource ID of the button text.
	 * @param location The location data to update the button text (may be null).
	 */
	private void setLocationRadioButton(final int buttonId, final int textId, final Location location) {
		String locationMetaData = getString(R.string.location_text_unknown);
		if (location != null) {
			String accuracyMetaData = "";
			double lat = location.getLatitude();
			double lon = location.getLongitude();
			accuracyMetaData = " (";
			
			if (location.hasAccuracy()) {
				accuracyMetaData += getString(R.string.location_text_metadata_accuracy, location.getAccuracy());
			}
			accuracyMetaData += location.getProvider() + ")";
			locationMetaData = getString(R.string.location_text_metadata_location, lat, lon, accuracyMetaData);
		}
		RadioButton rb = (RadioButton)findViewById(buttonId);
		rb.setEnabled(location != null);
		rb.setText(getString(textId, locationMetaData));
	}
	
	/**
	 * Checks if the new location is more accurate than
	 * {@link #currentLocation}.
	 * 
	 * @param newLocation new location
	 * @return true, if the new location is more accurate than the old one or
	 * one of them has no accuracy anyway.
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
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onProviderEnabled(final String provider) {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStatusChanged(final String provider, final int status, final Bundle extras) {
	}
	
}
