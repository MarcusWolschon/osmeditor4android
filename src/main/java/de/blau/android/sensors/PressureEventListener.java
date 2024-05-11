package de.blau.android.sensors;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.services.util.ExtendedLocation;

public class PressureEventListener implements SensorEventListener {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PressureEventListener.class.getSimpleName().length());
    private static final String DEBUG_TAG = PressureEventListener.class.getSimpleName().substring(0, TAG_LEN);

    static final double ZERO_CELSIUS = 273.15;

    private float millibarsOfPressure = 0;
    private float barometricHeight    = 0;
    private float pressureAtSeaLevel  = SensorManager.PRESSURE_STANDARD_ATMOSPHERE;
    private float temperature         = 15;

    private float tempP  = 0;
    private float mCount = 0;

    private final Location location;

    /**
     * Construct new instance
     * 
     * @param location Location object to potentially update
     */
    public PressureEventListener(@NonNull Location location) {
        this.location = location;
    }

    /**
     * Calibrate barometric height from current height
     * 
     * @param calibrationHeight current height from external source or GPS
     * 
     * @see <a href="https://en.wikipedia.org/wiki/Barometric_formula">Barometric formula</A>
     */
    public void calibrate(float calibrationHeight) {
        // p0 = ph * (Th / (Th + 0.0065 * h))^-5.255
        double temp = ZERO_CELSIUS + temperature;
        pressureAtSeaLevel = (float) (millibarsOfPressure * Math.pow(temp / (temp + 0.0065 * calibrationHeight), -5.255));
        recalc();
        Log.d(DEBUG_TAG, "Calibration new p0 " + pressureAtSeaLevel + " current h " + calibrationHeight + " ambient temperature " + temp + " current pressure "
                + millibarsOfPressure);
    }

    /**
     * Recalculate the current height after calibration
     */
    private void recalc() {
        barometricHeight = SensorManager.getAltitude(pressureAtSeaLevel, millibarsOfPressure);
        if (location instanceof ExtendedLocation) {
            ((ExtendedLocation) location).setBarometricHeight(barometricHeight);
        }
    }

    /**
     * Set the reference sea level pressure
     * 
     * @param p0 the pressure in hPa
     */
    public void setP0(float p0) {
        pressureAtSeaLevel = p0;
        recalc();
    }

    /**
     * Set the ambient temperature
     * 
     * @param temp the temperature in ° celsius
     */
    public void setTemperature(float temp) {
        temperature = temp;
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // Ignore
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mCount == 10) {
            millibarsOfPressure = tempP / 10;
            recalc();
            tempP = event.values[0];
            mCount = 1;
        } else {
            tempP = tempP + event.values[0];
            mCount++;
        }
    }

    /**
     * @return the barometricHeight
     */
    public float getBarometricHeight() {
        return barometricHeight;
    }

    /**
     * @return the millibarsOfPressure
     */
    public float getMillibarsOfPressure() {
        return millibarsOfPressure;
    }

    /**
     * @return the pressureAtSeaLevel
     */
    public float getPressureAtSeaLevel() {
        return pressureAtSeaLevel;
    }
}
