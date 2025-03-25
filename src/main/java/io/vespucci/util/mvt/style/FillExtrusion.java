package io.vespucci.util.mvt.style;

import android.graphics.Paint;
import androidx.annotation.NonNull;
import io.vespucci.util.SerializableTextPaint;

public class FillExtrusion extends Fill {

    private static final long serialVersionUID = 4L;

    /**
     * Default constructor
     * 
     * @param sourceLayer the source (data) layer
     */
    public FillExtrusion(@NonNull String sourceLayer) {
        super(sourceLayer);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    /**
     * Copy constructor
     * 
     * @param other another Layer
     */
    public FillExtrusion(@NonNull FillExtrusion other) {
        super(other);
    }

    /**
     * Create a rudimentary Layer from Paint objects for the geometries
     * 
     * @param layer source layer
     * @param paint the Paint to use for the geometries
     * @return a FillExtrusion Layer
     */
    @NonNull
    public static FillExtrusion fromPaint(@NonNull String layer, @NonNull Paint paint) {
        FillExtrusion style = new FillExtrusion(layer);
        style.paint = new SerializableTextPaint(paint);
        style.paint.setAntiAlias(true);
        style.paint.setStyle(Paint.Style.FILL_AND_STROKE);
        return style;
    }
}
