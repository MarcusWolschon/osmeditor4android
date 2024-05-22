package de.blau.android.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.annotation.NonNull;

/**
 * @see <a href=
 *      "https://web.archive.org/web/20110415003722/http://www.codingforandroid.com/2011/01/using-orientation-sensors-simple.html">Using
 *      orientation sensors: Simple Compass sample</a>
 * @see <a href="https://www.deviantdev.com/journal/android-compass-azimuth-calculating">Android: Compass Implementation
 *      - Calculating the Azimuth</a>
 */
public class CompassEventListener implements SensorEventListener {

    public interface OnChangedListener {
        /**
         * Called when the direction has changed
         * 
         * @param azimut the new direction
         */
        void onChanged(float azimut);
    }

    private float[] truncatedRotationVector;

    private final OnChangedListener listener;

    /**
     * Construct a new instance
     * 
     * @param listener listener that gets called on direction changes
     */
    public CompassEventListener(@NonNull OnChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // unused
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] orientation = new float[3];
        float[] rotationMatrix = new float[9];
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            if (event.values.length > 4) {
                // See
                // https://groups.google.com/forum/#!topic/android-developers/U3N9eL5BcJk
                // for more information on this
                //
                // On some Samsung devices
                // SensorManager.getRotationMatrixFromVector
                // appears to throw an exception if rotation vector has length > 4.
                // For the purposes of this class the first 4 values of the
                // rotation vector are sufficient (see crbug.com/335298 for details).
                if (truncatedRotationVector == null) {
                    truncatedRotationVector = new float[4];
                }
                System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                SensorManager.getRotationMatrixFromVector(rotationMatrix, truncatedRotationVector);
            } else {
                // calculate the rotation matrix
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            }
        }
        SensorManager.getOrientation(rotationMatrix, orientation);
        float azimut = (int) (Math.toDegrees(SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360) % 360;
        listener.onChanged(azimut);
    }
}
