package de.blau.android.resources;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathDashPathEffect.Style;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    static final String name = "triangle_left";

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
    static final String name = "triangle_right";

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

class BorderLeft extends PathPattern {
    static final String name = "border_left";

    Path path = new Path();

    @Override
    public Path draw(float size) {
        path.rewind();
        float width = Math.max(size, 2);
        float height = Math.max(size * 2, 2);
        height = Math.min(height, 32);
        path.moveTo(0, 0);
        path.lineTo(width, 0);
        path.lineTo(width, -height);
        path.lineTo(0, -height);
        path.lineTo(0, 0);
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

class BorderRight extends PathPattern {
    static final String name = "border_right";

    Path path = new Path();

    @Override
    public Path draw(float size) {
        path.rewind();
        float width = Math.max(size, 2);
        float height = Math.max(size * 2, 2);
        height = Math.min(height, 32);
        path.moveTo(0, 0);
        path.lineTo(width, 0);
        path.lineTo(width, height);
        path.lineTo(0, height);
        path.lineTo(0, 0);
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

class PathPatterns {

    static Map<String, PathPattern> patterns = new HashMap<>();

    static {
        patterns.put(TriangleLeft.name, new TriangleLeft());
        patterns.put(TriangleRight.name, new TriangleRight());
        patterns.put(BorderLeft.name, new BorderLeft());
        patterns.put(BorderRight.name, new BorderRight());
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
