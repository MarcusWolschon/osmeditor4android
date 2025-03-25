package io.vespucci.util.mvt.style;

public interface Interpolation {
    /**
     * Return a interpolated value
     * 
     * @param base the base if 1 we will use linear interpolation
     * @param x1 x of first point
     * @param y1 y of first point
     * @param x2 x of 2nd point
     * @param y2 y of 2nd point
     * @param x the x value we want to interpolate for
     * @return f(x)
     */
    double interpolate(float base, double x1, double y1, double x2, double y2, double x);
}
