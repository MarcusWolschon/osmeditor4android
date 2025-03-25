package io.vespucci.util.mvt.style;

public class Exponential implements Interpolation {

    @Override
    public double interpolate(float base, double x1, double y1, double x2, double y2, double x) {
        final double dY = y1 - y2;
        final double dX = Math.pow(base, x1) - Math.pow(base, x2);
        double h = logBase(base, dX / dY);
        double k = y1 - Math.pow(base, x1) * dY / dX;

        return Math.pow(base, x - h) + k;
    }

    /**
     * Calculate the log of x in the specified base
     * 
     * @param base the base
     * @param x x
     * @return log x
     */
    private static double logBase(double base, double x) {
        return Math.log(x) / Math.log(base);
    }
}
