package de.blau.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import de.blau.android.App;

public class Density {
	/**
	 * Converts a size in dp to pixels
	 * @param dp size in display point
	 * @return size in pixels (for the current display metrics)
	 */
	public static int dpToPx(int dp) {
		return dpToPx(App.resources(), dp);
	}
	
	public static float dpToPx(float dp) {
		return dpToPx(App.getCurrentInstance(), dp);
	}

	/**
	 * Converts a size in dp to pixels
	 * @param ctx
	 * @param dp size in display point
	 * @return size in display point
	 */
	public static float dpToPx(Context ctx, float dp) {
		return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
	}
	
	/**
	 * Converts a size in dp to pixels
	 * @param ctx
	 * @param dp size in display point
	 * @return size in display point
	 */
	public static int dpToPx(Context ctx, int dp) {
		return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
	}
	
	/**
	 * Converts a size in dp to pixels
	 * @param r
	 * @param dp size in display point
	 * @return size in display point
	 */
	public static int dpToPx(Resources r, int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}
}
