package de.blau.android.util.mvt.style;

import android.graphics.Paint;
import androidx.annotation.NonNull;
import de.blau.android.util.SerializablePaint;

public class FillExtrusion extends Fill {

    private static final long serialVersionUID = 4L;

    private static final String DEBUG_TAG = FillExtrusion.class.getSimpleName();

    /**
     * Default constructor
     */
    public FillExtrusion(@NonNull String sourceLayer) {
        super(sourceLayer);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    /**
     * Copy constructor
     * 
     * @param other another Style
     */
    public FillExtrusion(@NonNull FillExtrusion other) {
        super(other);
    }

    /**
     * Create a rudimentary style from Paint objects for the geometries and labels
     * 
     * @param paint the Paint to use for the geometries
     * @param labelPaint the Paint to use for labels
     * @param symbolPath a Path to use for point symbols
     * @return a Style
     */
    @NonNull
    public static FillExtrusion fromPaint(@NonNull String layer, @NonNull Paint paint) {
        FillExtrusion style = new FillExtrusion(layer);
        style.paint = new SerializablePaint(paint);
        style.paint.setAntiAlias(true);
        style.paint.setStyle(Paint.Style.FILL_AND_STROKE);
        return style;
    }
}
