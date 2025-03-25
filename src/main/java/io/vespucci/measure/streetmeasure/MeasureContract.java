package io.vespucci.measure.streetmeasure;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.measure.Length;
import io.vespucci.measure.LengthInFeetAndInches;
import io.vespucci.measure.LengthInMeters;
import io.vespucci.measure.Params;

public class MeasureContract extends ActivityResultContract<Params, Length> {

    private static final String PARAM_MEASURE_VERTICAL     = "measure_vertical";
    private static final String PARAM_UNIT                 = "unit";
    private static final String UNIT_METER                 = "meter";
    private static final String UNIT_FOOT_AND_INCH         = "foot_and_inch";
    private static final String PARAM_REQUEST_RESULT       = "request_result";
    private static final String PARAM_PRECISION_CM         = "precision_cm";
    private static final String PARAM_PRECISION_INCH       = "precision_inch";
    private static final String PARAM_MEASURING_TAPE_COLOR = "measuring_tape_color";
    private static final String RESULT_METERS              = "meters";
    private static final String RESULT_FEET                = "feet";
    private static final String RESULT_INCHES              = "inches";

    public enum LengthUnit {
        METER, FOOT_AND_INCH
    }

    private String key;

    @Override
    public Intent createIntent(@NonNull Context context, @NonNull Params input) {
        key = input.getKey();
        String unit = null;
        LengthUnit lengthUnit = input.getLengthUnit();
        if (lengthUnit != null) {
            switch (lengthUnit) {
            case METER:
                unit = UNIT_METER;
                break;
            case FOOT_AND_INCH:
                unit = UNIT_FOOT_AND_INCH;
                break;
            default:
                throw new IllegalArgumentException("Unknown LengthUnit " + lengthUnit);
            }
        }
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getString(R.string.streetmeasure_package));
        if (intent == null) {
            throw new ActivityNotFoundException();
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(PARAM_REQUEST_RESULT, true);
        intent.putExtra(PARAM_UNIT, unit);
        intent.putExtra(PARAM_PRECISION_CM, input.getPrecisionCm());
        intent.putExtra(PARAM_PRECISION_INCH, input.getPrecisionInch());
        intent.putExtra(PARAM_MEASURE_VERTICAL, input.measureVertical());
        intent.putExtra(PARAM_MEASURING_TAPE_COLOR, input.getMeasuringTapeColor());
        return intent;
    }

    @Override
    public Length parseResult(int resultCode, @Nullable Intent intent) {
        if (resultCode != Activity.RESULT_OK || intent == null) {
            return null;
        }

        double meters = intent.getDoubleExtra(RESULT_METERS, -1.0);
        if (meters != -1.0) {
            return new LengthInMeters(key, meters);
        }

        int feet = intent.getIntExtra(RESULT_FEET, -1);
        int inches = intent.getIntExtra(RESULT_INCHES, -1);
        if (feet != -1 && inches != -1) {
            return new LengthInFeetAndInches(key, feet, inches);
        }

        return null;
    }
}
