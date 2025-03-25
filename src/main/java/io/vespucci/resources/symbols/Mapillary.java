package io.vespucci.resources.symbols;

import android.content.Context;
import io.vespucci.util.Density;

public class Mapillary extends Symbol {
    public static final String NAME = Mapillary.class.getSimpleName();

    @Override
    public void draw(Context ctx, float scale) {
        path.rewind();
        path.moveTo(0, Density.dpToPx(ctx, -8) * scale);
        path.lineTo(Density.dpToPx(ctx, 8) * scale, Density.dpToPx(ctx, 10) * scale);
        path.lineTo(0, Density.dpToPx(ctx, 5) * scale);
        path.lineTo(Density.dpToPx(ctx, -8) * scale, Density.dpToPx(ctx, 10) * scale);
        path.lineTo(0, Density.dpToPx(ctx, -8) * scale);
    }
}
