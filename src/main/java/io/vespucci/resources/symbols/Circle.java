package io.vespucci.resources.symbols;

import android.content.Context;
import android.graphics.Path.Direction;
import io.vespucci.util.Density;

public class Circle extends Symbol {
    public static final String NAME = Circle.class.getSimpleName();

    @Override
    public void draw(Context ctx, float scale) {
        path.rewind();
        int side = (int) Density.dpToPx(ctx, 8 * scale);
        path.addCircle(0, 0, side, Direction.CW);
    }
}
