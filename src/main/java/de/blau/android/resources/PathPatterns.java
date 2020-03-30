package de.blau.android.resources;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathDashPathEffect.Style;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

abstract class PathPattern {
    /**
     * Create the pattern Path
     * 
     * @param size step size
     * @return a Path for the pattern
     */
    abstract Path draw(float size);

    /**
     * Amount to advance the pattern
     * 
     * @param size step size
     * @return the amount to advance the pattern
     */
    float advance(float size) {
        return size;
    }

    /**
     * How the pattern should applied
     * 
     * @return a PathDashPathEffect.Style value
     */
    Style style() {
        return PathDashPathEffect.Style.ROTATE;
    }
}

class TriangleLeft extends PathPattern {
    static final String NAME = "triangle_left";

    Path path = new Path();

    @Override
    public Path draw(float size) {
        path.rewind();
        float half = size / 2;
        path.moveTo(-half, 0);
        path.lineTo(half, 0);
        path.lineTo(0, -size);
        path.lineTo(-half, 0);
        return path;
    }
}

class TriangleRight extends PathPattern {
    static final String NAME = "triangle_right";

    Path path = new Path();

    @Override
    public Path draw(float size) {
        path.rewind();
        float half = size / 2;
        path.moveTo(-half, 0);
        path.lineTo(half, 0);
        path.lineTo(0, size);
        path.lineTo(-half, 0);
        return path;
    }
}

class Border extends PathPattern {

    Path  path = new Path();
    float width;
    float height;

    @Override
    public Path draw(float size) {
        path.rewind();
        width = Math.max(size, 2);
        height = Math.max(size * 2, 2);
        height = Math.min(height, 32);
        path.moveTo(0, 0);
        path.lineTo(width, 0);
        return path;
    }

    @Override
    public float advance(float size) {
        return Math.max(size, 2);
    }

    @Override
    Style style() {
        return PathDashPathEffect.Style.MORPH;
    }
}

class BorderLeft extends Border {
    static final String NAME = "border_left";

    @Override
    public Path draw(float size) {
        super.draw(size);
        path.lineTo(width, -height);
        path.lineTo(0, -height);
        path.lineTo(0, 0);
        return path;
    }
}

class BorderRight extends Border {
    static final String NAME = "border_right";

    @Override
    public Path draw(float size) {
        super.draw(size);
        path.lineTo(width, height);
        path.lineTo(0, height);
        path.lineTo(0, 0);
        return path;
    }
}

final class PathPatterns {

    /**
     * Private constructor to disallow instantiation
     */
    private PathPatterns() {
        // empty
    }

    static Map<String, PathPattern> patterns = new HashMap<>();

    static {
        patterns.put(TriangleLeft.NAME, new TriangleLeft());
        patterns.put(TriangleRight.NAME, new TriangleRight());
        patterns.put(BorderLeft.NAME, new BorderLeft());
        patterns.put(BorderRight.NAME, new BorderRight());
    }

    /**
     * Get a pattern by name
     * 
     * @param name the name of the pattern
     * @return the pattern or null if it couldn't be found
     */
    @Nullable
    public static PathPattern get(@NonNull String name) {
        return patterns.get(name);
    }
}
