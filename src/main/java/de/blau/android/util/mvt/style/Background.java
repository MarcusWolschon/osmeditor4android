package de.blau.android.util.mvt.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import de.blau.android.util.mvt.VectorTileDecoder.Feature;

public class Background extends Layer {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public Background() {
        super((String) null);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * Copy constructor
     * 
     * @param other another Style
     */
    public Background(@NonNull Background other) {
        super(other);
    }

    @Override
    public void render(Canvas c, Style style, Feature feature, int z, Rect screenRect, Rect destinationRect, float scaleX, float scaleY) {
        super.render(c, style, feature, z, screenRect, destinationRect, scaleX, scaleY);
        this.destinationRect = destinationRect;
        c.drawRect(destinationRect, paint);
    }

    @Override
    @NonNull
    public String toString() {
        return super.toString() + " " + getClass().getSimpleName() + " " + Integer.toHexString(paint.getColor());
    }
}
