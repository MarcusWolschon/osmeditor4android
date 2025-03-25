package io.vespucci.util.mvt.style;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.util.ColorUtil;

public final class Color {

    private static final String DEBUG_TAG = Color.class.getSimpleName().substring(0, Math.min(23, Color.class.getSimpleName().length()));

    private static final Map<String, Integer> HTML_COLORS = new HashMap<>();
    static {
        HTML_COLORS.put("Black".toLowerCase(Locale.US), Integer.parseInt("000000", 16));
        HTML_COLORS.put("Navy".toLowerCase(Locale.US), Integer.parseInt("000080", 16));
        HTML_COLORS.put("DarkBlue".toLowerCase(Locale.US), Integer.parseInt("00008B", 16));
        HTML_COLORS.put("MediumBlue".toLowerCase(Locale.US), Integer.parseInt("0000CD", 16));
        HTML_COLORS.put("Blue".toLowerCase(Locale.US), Integer.parseInt("0000FF", 16));
        HTML_COLORS.put("DarkGreen".toLowerCase(Locale.US), Integer.parseInt("006400", 16));
        HTML_COLORS.put("Green".toLowerCase(Locale.US), Integer.parseInt("008000", 16));
        HTML_COLORS.put("Teal".toLowerCase(Locale.US), Integer.parseInt("008080", 16));
        HTML_COLORS.put("DarkCyan".toLowerCase(Locale.US), Integer.parseInt("008B8B", 16));
        HTML_COLORS.put("DeepSkyBlue".toLowerCase(Locale.US), Integer.parseInt("00BFFF", 16));
        HTML_COLORS.put("DarkTurquoise".toLowerCase(Locale.US), Integer.parseInt("00CED1", 16));
        HTML_COLORS.put("MediumSpringGreen".toLowerCase(Locale.US), Integer.parseInt("00FA9A", 16));
        HTML_COLORS.put("Lime".toLowerCase(Locale.US), Integer.parseInt("00FF00", 16));
        HTML_COLORS.put("SpringGreen".toLowerCase(Locale.US), Integer.parseInt("00FF7F", 16));
        HTML_COLORS.put("Aqua".toLowerCase(Locale.US), Integer.parseInt("00FFFF", 16));
        HTML_COLORS.put("Cyan".toLowerCase(Locale.US), Integer.parseInt("00FFFF", 16));
        HTML_COLORS.put("MidnightBlue".toLowerCase(Locale.US), Integer.parseInt("191970", 16));
        HTML_COLORS.put("DodgerBlue".toLowerCase(Locale.US), Integer.parseInt("1E90FF", 16));
        HTML_COLORS.put("LightSeaGreen".toLowerCase(Locale.US), Integer.parseInt("20B2AA", 16));
        HTML_COLORS.put("ForestGreen".toLowerCase(Locale.US), Integer.parseInt("228B22", 16));
        HTML_COLORS.put("SeaGreen".toLowerCase(Locale.US), Integer.parseInt("2E8B57", 16));
        HTML_COLORS.put("DarkSlateGray".toLowerCase(Locale.US), Integer.parseInt("2F4F4F", 16));
        HTML_COLORS.put("DarkSlateGrey".toLowerCase(Locale.US), Integer.parseInt("2F4F4F", 16));
        HTML_COLORS.put("LimeGreen".toLowerCase(Locale.US), Integer.parseInt("32CD32", 16));
        HTML_COLORS.put("MediumSeaGreen".toLowerCase(Locale.US), Integer.parseInt("3CB371", 16));
        HTML_COLORS.put("Turquoise".toLowerCase(Locale.US), Integer.parseInt("40E0D0", 16));
        HTML_COLORS.put("RoyalBlue".toLowerCase(Locale.US), Integer.parseInt("4169E1", 16));
        HTML_COLORS.put("SteelBlue".toLowerCase(Locale.US), Integer.parseInt("4682B4", 16));
        HTML_COLORS.put("DarkSlateBlue".toLowerCase(Locale.US), Integer.parseInt("483D8B", 16));
        HTML_COLORS.put("MediumTurquoise".toLowerCase(Locale.US), Integer.parseInt("48D1CC", 16));
        HTML_COLORS.put("Indigo".toLowerCase(Locale.US), Integer.parseInt("4B0082", 16));
        HTML_COLORS.put("DarkOliveGreen".toLowerCase(Locale.US), Integer.parseInt("556B2F", 16));
        HTML_COLORS.put("CadetBlue".toLowerCase(Locale.US), Integer.parseInt("5F9EA0", 16));
        HTML_COLORS.put("CornflowerBlue".toLowerCase(Locale.US), Integer.parseInt("6495ED", 16));
        HTML_COLORS.put("RebeccaPurple".toLowerCase(Locale.US), Integer.parseInt("663399", 16));
        HTML_COLORS.put("MediumAquaMarine".toLowerCase(Locale.US), Integer.parseInt("66CDAA", 16));
        HTML_COLORS.put("DimGray".toLowerCase(Locale.US), Integer.parseInt("696969", 16));
        HTML_COLORS.put("DimGrey".toLowerCase(Locale.US), Integer.parseInt("696969", 16));
        HTML_COLORS.put("SlateBlue".toLowerCase(Locale.US), Integer.parseInt("6A5ACD", 16));
        HTML_COLORS.put("OliveDrab".toLowerCase(Locale.US), Integer.parseInt("6B8E23", 16));
        HTML_COLORS.put("SlateGray".toLowerCase(Locale.US), Integer.parseInt("708090", 16));
        HTML_COLORS.put("SlateGrey".toLowerCase(Locale.US), Integer.parseInt("708090", 16));
        HTML_COLORS.put("LightSlateGray".toLowerCase(Locale.US), Integer.parseInt("778899", 16));
        HTML_COLORS.put("LightSlateGrey".toLowerCase(Locale.US), Integer.parseInt("778899", 16));
        HTML_COLORS.put("MediumSlateBlue".toLowerCase(Locale.US), Integer.parseInt("7B68EE", 16));
        HTML_COLORS.put("LawnGreen".toLowerCase(Locale.US), Integer.parseInt("7CFC00", 16));
        HTML_COLORS.put("Chartreuse".toLowerCase(Locale.US), Integer.parseInt("7FFF00", 16));
        HTML_COLORS.put("Aquamarine".toLowerCase(Locale.US), Integer.parseInt("7FFFD4", 16));
        HTML_COLORS.put("Maroon".toLowerCase(Locale.US), Integer.parseInt("800000", 16));
        HTML_COLORS.put("Purple".toLowerCase(Locale.US), Integer.parseInt("800080", 16));
        HTML_COLORS.put("Olive".toLowerCase(Locale.US), Integer.parseInt("808000", 16));
        HTML_COLORS.put("Gray".toLowerCase(Locale.US), Integer.parseInt("808080", 16));
        HTML_COLORS.put("Grey".toLowerCase(Locale.US), Integer.parseInt("808080", 16));
        HTML_COLORS.put("SkyBlue".toLowerCase(Locale.US), Integer.parseInt("87CEEB", 16));
        HTML_COLORS.put("LightSkyBlue".toLowerCase(Locale.US), Integer.parseInt("87CEFA", 16));
        HTML_COLORS.put("BlueViolet".toLowerCase(Locale.US), Integer.parseInt("8A2BE2", 16));
        HTML_COLORS.put("DarkRed".toLowerCase(Locale.US), Integer.parseInt("8B0000", 16));
        HTML_COLORS.put("DarkMagenta".toLowerCase(Locale.US), Integer.parseInt("8B008B", 16));
        HTML_COLORS.put("SaddleBrown".toLowerCase(Locale.US), Integer.parseInt("8B4513", 16));
        HTML_COLORS.put("DarkSeaGreen".toLowerCase(Locale.US), Integer.parseInt("8FBC8F", 16));
        HTML_COLORS.put("LightGreen".toLowerCase(Locale.US), Integer.parseInt("90EE90", 16));
        HTML_COLORS.put("MediumPurple".toLowerCase(Locale.US), Integer.parseInt("9370DB", 16));
        HTML_COLORS.put("DarkViolet".toLowerCase(Locale.US), Integer.parseInt("9400D3", 16));
        HTML_COLORS.put("PaleGreen".toLowerCase(Locale.US), Integer.parseInt("98FB98", 16));
        HTML_COLORS.put("DarkOrchid".toLowerCase(Locale.US), Integer.parseInt("9932CC", 16));
        HTML_COLORS.put("YellowGreen".toLowerCase(Locale.US), Integer.parseInt("9ACD32", 16));
        HTML_COLORS.put("Sienna".toLowerCase(Locale.US), Integer.parseInt("A0522D", 16));
        HTML_COLORS.put("Brown".toLowerCase(Locale.US), Integer.parseInt("A52A2A", 16));
        HTML_COLORS.put("DarkGray".toLowerCase(Locale.US), Integer.parseInt("A9A9A9", 16));
        HTML_COLORS.put("DarkGrey".toLowerCase(Locale.US), Integer.parseInt("A9A9A9", 16));
        HTML_COLORS.put("LightBlue".toLowerCase(Locale.US), Integer.parseInt("ADD8E6", 16));
        HTML_COLORS.put("GreenYellow".toLowerCase(Locale.US), Integer.parseInt("ADFF2F", 16));
        HTML_COLORS.put("PaleTurquoise".toLowerCase(Locale.US), Integer.parseInt("AFEEEE", 16));
        HTML_COLORS.put("LightSteelBlue".toLowerCase(Locale.US), Integer.parseInt("B0C4DE", 16));
        HTML_COLORS.put("PowderBlue".toLowerCase(Locale.US), Integer.parseInt("B0E0E6", 16));
        HTML_COLORS.put("FireBrick".toLowerCase(Locale.US), Integer.parseInt("B22222", 16));
        HTML_COLORS.put("DarkGoldenRod".toLowerCase(Locale.US), Integer.parseInt("B8860B", 16));
        HTML_COLORS.put("MediumOrchid".toLowerCase(Locale.US), Integer.parseInt("BA55D3", 16));
        HTML_COLORS.put("RosyBrown".toLowerCase(Locale.US), Integer.parseInt("BC8F8F", 16));
        HTML_COLORS.put("DarkKhaki".toLowerCase(Locale.US), Integer.parseInt("BDB76B", 16));
        HTML_COLORS.put("Silver".toLowerCase(Locale.US), Integer.parseInt("C0C0C0", 16));
        HTML_COLORS.put("MediumVioletRed".toLowerCase(Locale.US), Integer.parseInt("C71585", 16));
        HTML_COLORS.put("IndianRed".toLowerCase(Locale.US), Integer.parseInt("CD5C5C", 16));
        HTML_COLORS.put("Peru".toLowerCase(Locale.US), Integer.parseInt("CD853F", 16));
        HTML_COLORS.put("Chocolate".toLowerCase(Locale.US), Integer.parseInt("D2691E", 16));
        HTML_COLORS.put("Tan".toLowerCase(Locale.US), Integer.parseInt("D2B48C", 16));
        HTML_COLORS.put("LightGray".toLowerCase(Locale.US), Integer.parseInt("D3D3D3", 16));
        HTML_COLORS.put("LightGrey".toLowerCase(Locale.US), Integer.parseInt("D3D3D3", 16));
        HTML_COLORS.put("Thistle".toLowerCase(Locale.US), Integer.parseInt("D8BFD8", 16));
        HTML_COLORS.put("Orchid".toLowerCase(Locale.US), Integer.parseInt("DA70D6", 16));
        HTML_COLORS.put("GoldenRod".toLowerCase(Locale.US), Integer.parseInt("DAA520", 16));
        HTML_COLORS.put("PaleVioletRed".toLowerCase(Locale.US), Integer.parseInt("DB7093", 16));
        HTML_COLORS.put("Crimson".toLowerCase(Locale.US), Integer.parseInt("DC143C", 16));
        HTML_COLORS.put("Gainsboro".toLowerCase(Locale.US), Integer.parseInt("DCDCDC", 16));
        HTML_COLORS.put("Plum".toLowerCase(Locale.US), Integer.parseInt("DDA0DD", 16));
        HTML_COLORS.put("BurlyWood".toLowerCase(Locale.US), Integer.parseInt("DEB887", 16));
        HTML_COLORS.put("LightCyan".toLowerCase(Locale.US), Integer.parseInt("E0FFFF", 16));
        HTML_COLORS.put("Lavender".toLowerCase(Locale.US), Integer.parseInt("E6E6FA", 16));
        HTML_COLORS.put("DarkSalmon".toLowerCase(Locale.US), Integer.parseInt("E9967A", 16));
        HTML_COLORS.put("Violet".toLowerCase(Locale.US), Integer.parseInt("EE82EE", 16));
        HTML_COLORS.put("PaleGoldenRod".toLowerCase(Locale.US), Integer.parseInt("EEE8AA", 16));
        HTML_COLORS.put("LightCoral".toLowerCase(Locale.US), Integer.parseInt("F08080", 16));
        HTML_COLORS.put("Khaki".toLowerCase(Locale.US), Integer.parseInt("F0E68C", 16));
        HTML_COLORS.put("AliceBlue".toLowerCase(Locale.US), Integer.parseInt("F0F8FF", 16));
        HTML_COLORS.put("HoneyDew".toLowerCase(Locale.US), Integer.parseInt("F0FFF0", 16));
        HTML_COLORS.put("Azure".toLowerCase(Locale.US), Integer.parseInt("F0FFFF", 16));
        HTML_COLORS.put("SandyBrown".toLowerCase(Locale.US), Integer.parseInt("F4A460", 16));
        HTML_COLORS.put("Wheat".toLowerCase(Locale.US), Integer.parseInt("F5DEB3", 16));
        HTML_COLORS.put("Beige".toLowerCase(Locale.US), Integer.parseInt("F5F5DC", 16));
        HTML_COLORS.put("WhiteSmoke".toLowerCase(Locale.US), Integer.parseInt("F5F5F5", 16));
        HTML_COLORS.put("MintCream".toLowerCase(Locale.US), Integer.parseInt("F5FFFA", 16));
        HTML_COLORS.put("GhostWhite".toLowerCase(Locale.US), Integer.parseInt("F8F8FF", 16));
        HTML_COLORS.put("Salmon".toLowerCase(Locale.US), Integer.parseInt("FA8072", 16));
        HTML_COLORS.put("AntiqueWhite".toLowerCase(Locale.US), Integer.parseInt("FAEBD7", 16));
        HTML_COLORS.put("Linen".toLowerCase(Locale.US), Integer.parseInt("FAF0E6", 16));
        HTML_COLORS.put("LightGoldenRodYellow".toLowerCase(Locale.US), Integer.parseInt("FAFAD2", 16));
        HTML_COLORS.put("OldLace".toLowerCase(Locale.US), Integer.parseInt("FDF5E6", 16));
        HTML_COLORS.put("Red".toLowerCase(Locale.US), Integer.parseInt("FF0000", 16));
        HTML_COLORS.put("Fuchsia".toLowerCase(Locale.US), Integer.parseInt("FF00FF", 16));
        HTML_COLORS.put("Magenta".toLowerCase(Locale.US), Integer.parseInt("FF00FF", 16));
        HTML_COLORS.put("DeepPink".toLowerCase(Locale.US), Integer.parseInt("FF1493", 16));
        HTML_COLORS.put("OrangeRed".toLowerCase(Locale.US), Integer.parseInt("FF4500", 16));
        HTML_COLORS.put("Tomato".toLowerCase(Locale.US), Integer.parseInt("FF6347", 16));
        HTML_COLORS.put("HotPink".toLowerCase(Locale.US), Integer.parseInt("FF69B4", 16));
        HTML_COLORS.put("Coral".toLowerCase(Locale.US), Integer.parseInt("FF7F50", 16));
        HTML_COLORS.put("DarkOrange".toLowerCase(Locale.US), Integer.parseInt("FF8C00", 16));
        HTML_COLORS.put("LightSalmon".toLowerCase(Locale.US), Integer.parseInt("FFA07A", 16));
        HTML_COLORS.put("Orange".toLowerCase(Locale.US), Integer.parseInt("FFA500", 16));
        HTML_COLORS.put("LightPink".toLowerCase(Locale.US), Integer.parseInt("FFB6C1", 16));
        HTML_COLORS.put("Pink".toLowerCase(Locale.US), Integer.parseInt("FFC0CB", 16));
        HTML_COLORS.put("Gold".toLowerCase(Locale.US), Integer.parseInt("FFD700", 16));
        HTML_COLORS.put("PeachPuff".toLowerCase(Locale.US), Integer.parseInt("FFDAB9", 16));
        HTML_COLORS.put("NavajoWhite".toLowerCase(Locale.US), Integer.parseInt("FFDEAD", 16));
        HTML_COLORS.put("Moccasin".toLowerCase(Locale.US), Integer.parseInt("FFE4B5", 16));
        HTML_COLORS.put("Bisque".toLowerCase(Locale.US), Integer.parseInt("FFE4C4", 16));
        HTML_COLORS.put("MistyRose".toLowerCase(Locale.US), Integer.parseInt("FFE4E1", 16));
        HTML_COLORS.put("BlanchedAlmond".toLowerCase(Locale.US), Integer.parseInt("FFEBCD", 16));
        HTML_COLORS.put("PapayaWhip".toLowerCase(Locale.US), Integer.parseInt("FFEFD5", 16));
        HTML_COLORS.put("LavenderBlush".toLowerCase(Locale.US), Integer.parseInt("FFF0F5", 16));
        HTML_COLORS.put("SeaShell".toLowerCase(Locale.US), Integer.parseInt("FFF5EE", 16));
        HTML_COLORS.put("Cornsilk".toLowerCase(Locale.US), Integer.parseInt("FFF8DC", 16));
        HTML_COLORS.put("LemonChiffon".toLowerCase(Locale.US), Integer.parseInt("FFFACD", 16));
        HTML_COLORS.put("FloralWhite".toLowerCase(Locale.US), Integer.parseInt("FFFAF0", 16));
        HTML_COLORS.put("Snow".toLowerCase(Locale.US), Integer.parseInt("FFFAFA", 16));
        HTML_COLORS.put("Yellow".toLowerCase(Locale.US), Integer.parseInt("FFFF00", 16));
        HTML_COLORS.put("LightYellow".toLowerCase(Locale.US), Integer.parseInt("FFFFE0", 16));
        HTML_COLORS.put("Ivory".toLowerCase(Locale.US), Integer.parseInt("FFFFF0", 16));
        HTML_COLORS.put("White".toLowerCase(Locale.US), Integer.parseInt("FFFFFF", 16));
    }

    private static final Pattern COLOR_6_HEX = Pattern.compile("(?i)^#([a-f0-9]{6})$");
    private static final Pattern COLOR_3_HEX = Pattern.compile("(?i)^#([a-f0-9])([a-f0-9])([a-f0-9])$");
    private static final Pattern COLOR_RGB   = Pattern.compile("(?i)^rgb\\(\\s*([0-9]{1,3})\\s*,\\s*([0-9]{1,3})\\s*,\\s*([0-9]{1,3})\\s*\\)$");
    private static final Pattern COLOR_RGBA  = Pattern
            .compile("(?i)^\\s*rgba\\(\\s*([0-9]{1,3})\\s*,\\s*([0-9]{1,3})\\s*,\\s*([0-9]{1,3})\\s*,\\s*([0-1](?:\\.[0-9]*)?)\\s*\\)\\s*$");
    private static final Pattern COLOR_HSL   = Pattern.compile("(?i)^\\s*hsl\\(\\s*([0-9]{1,3})\\s*,\\s*([0-9]{1,3})\\%\\s*,\\s*([0-9]{1,3})\\%\\s*\\)\\s*$");
    private static final Pattern COLOR_HSLA  = Pattern
            .compile("(?i)^\\s*hsla\\(\\s*([0-9]{1,3})\\s*,\\s*([0-9]{1,3})\\%\\s*,\\s*([0-9]{1,3})\\%\\s*,\\s*([0-1](?:\\.[0-9]*)?)\\s*\\)\\s*$");
    private static final Pattern COLOR_HTML  = Pattern.compile("(?i)^\\s*([a-z]*)\\s*$");

    /**
     * Private constructor to inhibit instantiation
     */
    private Color() {
        // empty
    }

    /**
     * Parser a Mapbox-GL color string
     * 
     * @param colorString the Mapbox-GL String
     * @return an int in ARGB format, 0 (Black) if something goes wrong
     */
    public static int parseColor(@NonNull String colorString) {
        try {
            Matcher m = COLOR_6_HEX.matcher(colorString);
            if (m.matches()) {
                return Integer.parseInt(m.group(1), 16);
            }
            m = COLOR_3_HEX.matcher(colorString);
            if (m.matches()) {
                int r = Integer.parseInt(m.group(1), 16);
                int g = Integer.parseInt(m.group(2), 16);
                int b = Integer.parseInt(m.group(3), 16);
                return r << 20 | r << 16 | g << 12 | g << 8 | b << 4 | b;
            }
            m = COLOR_RGB.matcher(colorString);
            if (m.matches()) {
                return android.graphics.Color.rgb(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            }
            m = COLOR_RGBA.matcher(colorString);
            if (m.matches()) {
                return ColorUtil.argb(Math.round(Float.parseFloat(m.group(4)) * 255), Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3)));
            }
            m = COLOR_HSL.matcher(colorString);
            if (m.matches()) {
                return toRGB(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            }
            m = COLOR_HSLA.matcher(colorString);
            if (m.matches()) {
                return toRGB(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Float.parseFloat(m.group(4)));
            }
            m = COLOR_HTML.matcher(colorString);
            if (m.matches()) {
                Integer htmlColor = HTML_COLORS.get(m.group(1).toLowerCase(Locale.US));
                if (htmlColor != null) {
                    return htmlColor;
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "Unparseable color string " + colorString + " " + e.getMessage());
        }
        Log.e(DEBUG_TAG, "Unparseable color string " + colorString);
        return 0;
    }

    /**
     * The following code is adapted from https://tips4java.wordpress.com/2009/07/05/hsl-color/
     */
    /**
     * Convert HSL values to a RGB Color with a default alpha value of 1.
     *
     * @param h Hue is specified as degrees in the range 0 - 360.
     * @param s Saturation is specified as a percentage in the range 1 - 100.
     * @param l Lumanance is specified as a percentage in the range 1 - 100.
     *
     * @return the RGB Color object
     */
    public static int toRGB(float h, float s, float l) {
        return toRGB(h, s, l, 1.0f);
    }

    /**
     * Convert HSL values to a RGB Color.
     *
     * @param h Hue is specified as degrees in the range 0 - 360.
     * @param s Saturation is specified as a percentage in the range 1 - 100.
     * @param l Lumanance is specified as a percentage in the range 1 - 100.
     * @param alpha the alpha value between 0 - 1
     *
     * @return the RGB Color object
     */
    public static int toRGB(float h, float s, float l, float alpha) {
        if (s < 0.0f || s > 100.0f) {
            String message = "Color parameter outside of expected range - Saturation";
            throw new IllegalArgumentException(message);
        }

        if (l < 0.0f || l > 100.0f) {
            String message = "Color parameter outside of expected range - Luminance";
            throw new IllegalArgumentException(message);
        }

        if (alpha < 0.0f || alpha > 1.0f) {
            String message = "Color parameter outside of expected range - Alpha";
            throw new IllegalArgumentException(message);
        }
        // Formula needs all values between 0 - 1.

        h = h % 360.0f;
        h /= 360f;
        s /= 100f;
        l /= 100f;

        float q = 0;

        if (l < 0.5) {
            q = l * (1 + s);
        } else {
            q = (l + s) - (s * l);
        }

        float p = 2 * l - q;

        float r = Math.max(0, hueToRGB(p, q, h + (1.0f / 3.0f)));
        float g = Math.max(0, hueToRGB(p, q, h));
        float b = Math.max(0, hueToRGB(p, q, h - (1.0f / 3.0f)));

        r = Math.min(r, 1.0f);
        g = Math.min(g, 1.0f);
        b = Math.min(b, 1.0f);

        return ColorUtil.argb(alpha, r, g, b);
    }

    /**
     * Convert hue values to RGB single color value
     * 
     * @param p
     * @param q
     * @param h
     * @return a float RGB color value ?
     */
    private static float hueToRGB(float p, float q, float h) {
        if (h < 0) {
            h += 1;
        }

        if (h > 1) {
            h -= 1;
        }

        if (6 * h < 1) {
            return p + ((q - p) * 6 * h);
        }

        if (2 * h < 1) {
            return q;
        }

        if (3 * h < 2) {
            return p + ((q - p) * 6 * ((2.0f / 3.0f) - h));
        }

        return p;
    }

}
