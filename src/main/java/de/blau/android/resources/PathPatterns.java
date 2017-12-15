package de.blau.android.resources;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Path;

abstract class PathPattern {
    abstract Path draw(float size);
}

class TriangleLeft extends PathPattern {
    final static String name = "triangle_left";

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
    final static String name = "triangle_right";

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

class PathPatterns {

    static Map<String, PathPattern> patterns = new HashMap<>();

    static {
        patterns.put(TriangleLeft.name, new TriangleLeft());
        patterns.put(TriangleRight.name, new TriangleRight());
    }

    public static PathPattern get(String name) {
        return patterns.get(name);
    }
}
