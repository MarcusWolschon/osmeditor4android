package io.vespucci.propertyeditor.tagform;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.List;
import java.util.Map;

import com.redinput.compassview.CompassView;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.osm.Tags;
import io.vespucci.presets.PresetGroup;
import io.vespucci.presets.PresetItem;
import io.vespucci.sensors.CompassEventListener;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.ThemeUtils;

public class DirectionValueFragment extends ValueWidgetFragment {

    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, DirectionValueFragment.class.getSimpleName().length());
    protected static final String DEBUG_TAG = DirectionValueFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "DIRECTION_FRAGMENT";

    /**
     * Show a dialog for adding/editing a direction value
     * 
     * @param caller calling Fragment
     * @param hint description of the key
     * @param key the key
     * @param value the existing value
     * @param values any additional values from the preset or mru
     * @param preset the preset item or null
     * @param allTags all current tags
     */
    public static void show(@NonNull Fragment caller, @NonNull String hint, @NonNull String key, @Nullable String value, @Nullable List<String> values,
            @Nullable PresetItem preset, @NonNull Map<String, String> allTags) {
        FragmentManager fm = caller.getChildFragmentManager();
        final PresetGroup rootGroup = App.getCurrentRootPreset(caller.getContext()).getRootGroup();
        DirectionValueFragment df = ValueWidgetFragment.setArguments(new DirectionValueFragment(), hint, key, value, values,
                preset != null && rootGroup != null ? preset.getPath(rootGroup) : null, allTags);
        io.vespucci.propertyeditor.Util.removeChildFragment(fm, TAG);
        df.show(fm, TAG);
    }

    @NonNull
    @Override
    ValueWidget getWidget(@NonNull FragmentActivity activity, @NonNull String value, @Nullable List<String> values) {
        return new CompassWidget(activity, value);
    }

    class CompassWidget implements ValueWidget {

        private final CompassView  compass;
        final Sensor               rotation;
        final SensorManager        sensorManager;
        final CompassEventListener compassListener;

        /**
         * Construct a new widget
         * 
         * @param activity current FragmentActivity
         * @param value initial value
         */
        CompassWidget(@NonNull FragmentActivity activity, @NonNull String value) {
            compass = new CompassView(activity, null);
            Float direction = Tags.parseDirection(value);
            if (direction == Float.NaN) {
                direction = 0f;
            }
            if (direction < 0) {
                direction = direction + 360f;
            }

            compass.setDegrees(direction, true); // with animation
            compass.setBackgroundColor(ThemeUtils.getStyleAttribColorValue(activity, R.attr.highlight_background, R.color.black));
            compass.setLineColor(Color.RED);
            compass.setMarkerColor(Color.RED);
            compass.setTextColor(ThemeUtils.getStyleAttribColorValue(activity, R.attr.text_normal, R.color.ccc_white));
            compass.setShowMarker(true);
            compass.setTextSize(37);
            compass.setRangeDegrees(50);
            compass.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));

            sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

            compassListener = new CompassEventListener((float azimut) -> {
                if (compass.isEnabled()) {
                    compass.setDegrees(azimut, true);
                }
            });

            rotation = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) : null;
            if (rotation != null) {
                sensorManager.registerListener(compassListener, rotation, SensorManager.SENSOR_DELAY_UI);
            } else {
                ScreenMessage.toastTopInfo(activity, R.string.toast_no_compass);
            }

            compass.setOnCompassDragListener((float azimut) -> {
                if (rotation != null) {
                    sensorManager.unregisterListener(compassListener, rotation);
                }
                compass.setDegrees(azimut);
            });
        }

        @Override
        @NonNull
        public String getValue() {
            return Integer.toString((int) compass.getDegrees());
        }

        @Override
        public boolean filter(@Nullable String v) {
            if (v == null || "".equals(v)) { // suppress empty value added for deletion
                return false;
            }
            try { // MRU may have numeric values too
                Float.parseFloat(v);
                return false;
            } catch (NumberFormatException nfex) {
                // add
            }
            return true;
        }

        @Override
        public void enable() {
            compass.setEnabled(true);
            compass.setFocusable(true);
            compass.setLineColor(Color.RED);
            compass.setMarkerColor(Color.RED);
        }

        @Override
        public void disable() {
            compass.setEnabled(false);
            compass.setFocusable(false);
            compass.setLineColor(Color.GRAY);
            compass.setMarkerColor(Color.GRAY);
        }

        @Override
        public void onDismiss() {
            if (rotation != null) {
                sensorManager.unregisterListener(compassListener, rotation);
            }
            TagFormFragment caller = (TagFormFragment) getParentFragment();
            caller.enableDialogRow(key);
        }

        @Override
        @NonNull
        public View getWidgetView() {
            return compass;
        }

        @Override
        @Nullable
        public String getUsageText(@NonNull Context ctx, boolean hasAdditionalValues) {
            return ctx.getString(hasAdditionalValues ? R.string.compass_widget_usage_with_additional : R.string.compass_widget_usage);
        }
    }
}
