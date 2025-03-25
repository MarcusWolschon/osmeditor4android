package io.vespucci.resources.symbols;

import android.content.Context;
import io.vespucci.util.Density;

public class TriangleDown extends Symbol {
    public static final String NAME = TriangleDown.class.getSimpleName();

    @Override
    public void draw(Context ctx, float scale) {
        path.rewind();
        int side = (int) Density.dpToPx(ctx, 8 * scale);
        path.moveTo(0, 0);
        path.lineTo(side, -side * 2f);
        path.lineTo(-side, -side * 2f);
        path.lineTo(0, 0);
    }
}
