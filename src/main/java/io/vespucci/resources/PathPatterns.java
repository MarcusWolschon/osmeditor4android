package io.vespucci.resources;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathDashPathEffect.Style;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

interface PathPattern {
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
    default float advance(float size) {
        return size;
    }

    /**
     * How the pattern should applied
     * 
     * @return a PathDashPathEffect.Style value
     */
    default Style style() {
        return PathDashPathEffect.Style.ROTATE;
    }
}

class TriangleLeft implements PathPattern {
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

class TriangleRight implements PathPattern {
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

class SquareLeft implements PathPattern {
    static final String NAME = "square_left";

    Path path = new Path();

    @Override
    public Path draw(float size) {
        path.rewind();
        float half = size / 2;
        float line = size / 8;
        path.moveTo(0, 0);
        path.lineTo(0, -half);
        path.lineTo(-half, -half);
        path.lineTo(-half, line);
        path.lineTo(half, line);
        path.lineTo(half, 0);
        path.lineTo(0, 0);
        return path;
    }
}

class SquareRight implements PathPattern {
    static final String NAME = "square_right";

    Path path = new Path();

    @Override
    public Path draw(float size) {
        path.rewind();
        float half = size / 2;
        float line = size / 8;
        path.moveTo(0, 0);
        path.lineTo(0, half);
        path.lineTo(-half, half);
        path.lineTo(-half, -line);
        path.lineTo(half, -line);
        path.lineTo(half, 0);
        path.lineTo(0, 0);
        return path;
    }
}

class SquareBoth implements PathPattern {
    static final String NAME = "square_both";

    Path path = new Path();

    @Override
    public Path draw(float size) {
        path.rewind();
        float half = size / 2;
        path.moveTo(0, 0);
        path.moveTo(-half, 0);
        path.lineTo(-half, half);
        path.lineTo(0, half);
        path.lineTo(0, -half);
        path.lineTo(half, -half);
        path.lineTo(half, 0);
        path.lineTo(0, 0);
        return path;
    }
}

class Border implements PathPattern {

    Path  path = new Path();
    float width;
    float height;

    @Override
    public Path draw(float size) {
        path.rewind();
        width = Math.max(size * 5, 2);
        height = Math.max(size * 2, 2);
        height = Math.min(height, 32);
        path.moveTo(0, 0);
        path.lineTo(width, 0);
        return path;
    }

    @Override
    public float advance(float size) {
        return width;
    }

    @Override
    public Style style() {
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

class Wiggle implements PathPattern {
    static final String NAME = "wiggle";

    Path path = new Path();

    @Override
    public Path draw(float size) {
        path.rewind();
        float half = size / 2;
        float quarter = half / 2;
        path.moveTo(-half, 0);
        path.lineTo(-quarter, half);
        path.lineTo(quarter, -half);
        path.lineTo(half, 0);
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
        patterns.put(SquareLeft.NAME, new SquareLeft());
        patterns.put(SquareRight.NAME, new SquareRight());
        patterns.put(SquareBoth.NAME, new SquareBoth());
        patterns.put(BorderLeft.NAME, new BorderLeft());
        patterns.put(BorderRight.NAME, new BorderRight());
        patterns.put(Wiggle.NAME, new Wiggle());
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
