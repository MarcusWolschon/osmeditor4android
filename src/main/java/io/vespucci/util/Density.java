package io.vespucci.util;

import android.content.Context;

public final class Density {

    /**
     * Private constructor to stop instantiation
     */
    private Density() {
        // private
    }

    /**
     * Converts a size in dp to pixels
     * 
     * @param ctx Android Context
     * @param dp size in density independent pixels
     * @return size in pixels (for the current display metrics)
     */
    public static float dpToPx(Context ctx, float dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    /**
     * Converts a size in dp to pixels
     * 
     * @param ctx Android Context
     * @param dp size in density independent pixels
     * @return size in pixels (for the current display metrics)
     */
    public static int dpToPx(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    /**
     * Converts a size in pixels to dp
     * 
     * @param ctx Android Context
     * @param px size in pixels
     * @return size in density independent pixels
     */
    public static int pxToDp(Context ctx, int px) {
        return Math.round(px / ctx.getResources().getDisplayMetrics().density);
    }
}
