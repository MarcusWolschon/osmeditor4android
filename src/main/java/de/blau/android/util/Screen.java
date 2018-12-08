package de.blau.android.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.Display;

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
    @SuppressLint("NewApi")
    public static boolean isLandscape(@NonNull Activity activity) {
        // reliable determine if we are in landscape mode
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(size);
        } else {
            // noinspection deprecation
            size.x = display.getWidth();
            // noinspection deprecation
            size.y = display.getHeight();
        }
        return isLarge(activity) && size.x > size.y;
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
    public static int getScreenSmallDimemsion(@NonNull Activity activity) {
        Point size = new Point();
        Display display = activity.getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(size);
        } else {
            // noinspection deprecation
            size.x = display.getWidth(); // NOSONAR
            // noinspection deprecation
            size.y = display.getHeight(); // NOSONAR
        }

        if (size.x < size.y) {
            return size.x;
        }
        return size.y;
    }
}
