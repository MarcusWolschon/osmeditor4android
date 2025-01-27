package de.blau.android.util.mvt.style;

public class Linear implements Interpolation {

    @Override
    public double interpolate(float base, double x1, double y1, double x2, double y2, double x) {
        final double a = (x2 * y1 - x1 * y2) / (x2 - x1);
        final double s = (y2 - a) / x2;
        return x * s + a;
    }
}
