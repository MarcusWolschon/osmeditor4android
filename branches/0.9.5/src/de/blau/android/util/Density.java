package de.blau.android.util;

import android.content.Context;
import de.blau.android.Application;

public class Density {
	/**
	 * Converts a size in dp to pixels
	 * @param dp size in display point
	 * @return size in pixels (for the current display metrics)
	 */
	static public int dpToPx(int dp) {
		return Math.round(dp * Application.mainActivity.getResources().getDisplayMetrics().density);
	}
	
	static public float dpToPx(float dp) {
		return Math.round(dp * Application.mainActivity.getResources().getDisplayMetrics().density);
	}

	public static int dpToPx(Context ctx, int dp) {
		return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
	}
}
