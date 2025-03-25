package io.vespucci.util;

import androidx.core.graphics.ColorUtils;

public final class ColorUtil {

    /**
     * Private constructor to stop instantiation
     */
    private ColorUtil() {
        // empty
    }

    /**
     * Construct a int color value from components
     * 
     * All values must be between 0 and 255
     * 
     * @param a alpha
     * @param r red
     * @param g green
     * @param b blue
     * @return an int color
     */
    public static int argb(int a, int r, int g, int b) {
        return (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
    }

    /**
     * Construct a int color value from components
     * 
     * All values must be between 0 and 1
     * 
     * @param alpha alpha
     * @param r red
     * @param g green
     * @param b blue
     * @return an int color
     */
    public static int argb(float alpha, float r, float g, float b) {
        return argb(Math.round(alpha * 255), Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }

    /**
     * Generate a color
     * 
     * @param index index of the color to create
     * @param steps the number of steps in HSL space
     * @param seed the initial color (index == 0 will return this)
     * @return a color value
     */
    public static int generateColor(int index, int steps, int seed) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(seed, hsl);
        return (ColorUtils.HSLToColor(new float[] { (hsl[0] + (index * 360f / steps)) % 360f, hsl[1], hsl[2] }) & 0x00FFFFFF) | (seed & 0xFF000000);
    }
}
