package de.blau.android.util;

import android.content.Context;
import de.blau.android.App;

public class Density {
    /**
     * Converts a size in dp to pixels
     * 
     * @param dp size in display point
     * @return size in pixels (for the current display metrics)
     */
    public static int dpToPx(int dp) {
        return Math.round(dp * App.resources().getDisplayMetrics().density);
    }

    public static float dpToPx(float dp) {
        return Math.round(dp * App.resources().getDisplayMetrics().density);
    }

    public static float dpToPx(Context ctx, float dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    public static int dpToPx(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
    
    public static int pxToDp(Context ctx, int px) {
        return Math.round(px / ctx.getResources().getDisplayMetrics().density);
    }
}
