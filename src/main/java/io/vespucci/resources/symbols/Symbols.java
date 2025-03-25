package io.vespucci.resources.symbols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.util.Density;

abstract class Symbol {

    Path path = new Path();

    /**
     * Draw the marker to a Path
     * 
     * @param ctx an Android Context
     * @param scale scaling factor
     */
    abstract void draw(@NonNull Context ctx, float scale);

    /**
     * Get the generated Path
     * 
     * @return the Path for the marker
     */
    @NonNull
    Path getPath() {
        return path;
    }

    /**
     * Create an ImageView with the Marker
     * 
     * @param ctx an Android Context
     * @param markerPaint the Paint to draw the image with
     * @return an ImageView
     */
    @NonNull
    ImageView getImage(@NonNull Context ctx, @NonNull Paint markerPaint) {
        final int size = Density.dpToPx(ctx, Symbols.SIZE);
        Bitmap tempBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(tempBitmap);
        c.save();
        c.translate(size / 2f, size / 2f);
        c.drawPath(getPath(), markerPaint);
        c.restore();
        ImageView v = new ImageView(ctx);
        v.setImageBitmap(tempBitmap);
        return v;
    }
}

public final class Symbols {

    public static final int SIZE = 48;

    /**
     * Private constructor to disallow instantiation
     */
    private Symbols() {
        // empty
    }

    static Map<String, Symbol> symbols = new HashMap<>();

    static {
        symbols.put(TriangleDown.NAME, new TriangleDown());
        symbols.put(Mapillary.NAME, new Mapillary());
        symbols.put(Circle.NAME, new Circle());
    }

    /**
     * Get a pattern by name
     * 
     * @param name the name of the pattern
     * @return the pattern or null if it couldn't be found
     */
    @Nullable
    public static Path get(@NonNull String name) {
        Symbol symbol = symbols.get(name);
        return symbol != null ? symbol.getPath() : null;
    }

    /**
     * Draw the paths for all markers
     * 
     * @param ctx an Android Context
     * @param scale the scale factor
     */
    public static void draw(@NonNull Context ctx, float scale) {
        for (Symbol symbol : symbols.values()) {
            symbol.draw(ctx, scale);
        }
    }

    /**
     * Get a Map containing ImageViews and the corresponding marker name
     * 
     * @param ctx an Android Context
     * @param paint the Paint to use
     * @return a Map
     */
    public static Map<String, ImageView> getImages(@NonNull Context ctx, @NonNull Paint paint) {
        // more that a one liner because we want to keep things sorted
        Map<String, ImageView> result = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(symbols.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            result.put(key, symbols.get(key).getImage(ctx, paint));
        }
        return result;
    }
}
