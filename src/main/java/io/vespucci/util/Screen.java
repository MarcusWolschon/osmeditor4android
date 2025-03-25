package io.vespucci.util;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

public final class Screen {

    /**
     * Private default construtor to inhibit instantiation
     */
    private Screen() {
        // empty
    }

    /**
     * Reliably determine if we are in landscape orientation
     * 
     * @param activity the calling Activity
     * @return true if we are in landscape orientation
     */
    public static boolean isLandscape(@NonNull Activity activity) {
        // reliable determine if we are in landscape mode
        WindowMetrics metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity);
        Rect size = metrics.getBounds();
        return isLarge(activity) && size.width() > size.height();
    }

    /**
     * Determine if we are running on a large device
     * 
     * @param activity the calling Activity
     * @return true if we are running on a large device
     */
    public static boolean isLarge(@NonNull Activity activity) {
        int screenSize = activity.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE);
    }

    /**
     * Get the size of the smaller side of the screen
     * 
     * @param activity the calling Activity
     * @return the smaller side in px
     */
    public static int getScreenSmallDimension(@NonNull Activity activity) {
        WindowMetrics metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity);
        Rect size = metrics.getBounds();
        if (size.width() < size.height()) {
            return size.width();
        }
        return size.height();
    }
}
