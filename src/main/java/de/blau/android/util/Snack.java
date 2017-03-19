package de.blau.android.util;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;
import de.blau.android.R;

/**
 * Helper methods to display Snackbars in a consistent way
 * @author simon
 *
 */
public class Snack {
	
	private static final int SHOW_DURATION_ACTION = 5000;

	/**
	 * Display a snackbar with an error message
	 * 
	 * @param activity activity calling us
	 * @param res resource id of the message to display
	 */
	public static void barError(Activity activity, int res) {
		if (activity != null) {
			barError(activity.findViewById(android.R.id.content), res);
		}
	}
	
	/**
	 * Display a snackbar with an error message
	 * 
	 * @param v view to display the snackbar on
	 * @param res resource id of the message to display
	 */
	public static void barError(View v, int res) {
		Snackbar snackbar = Snackbar.make(v, res, Snackbar.LENGTH_LONG);
		snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_error, android.R.color.holo_red_light));
		snackbar.show();
	}
	
	/**
	 * * Display a snackbar with an error message
	 * 
	 * @param activity activity calling us
	 * @param msg message to display
	 */
	public static void barError(Activity activity, String msg) {
		if (activity != null) {
			barError(activity.findViewById(android.R.id.content), msg);
		}
	}
	
	/**
	 * Display a snackbar with an error message
	 * 
	 * @param v view to display the snackbar on
	 * @param msg message to display
	 */
	public static void barError(View v, String msg) {
		Snackbar snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG);
		snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_error, android.R.color.holo_red_light));
		snackbar.show();
	}
	
	/**
	 * Display a snackbar with an informational message
	 * 
	 * @param activity activity calling us
	 * @param res resource id of the message to display
	 */
	public static void barInfo(Activity activity, int res) {
		if (activity != null) {
			barInfo(activity.findViewById(android.R.id.content), res);
		}
	}
	
	/**
	 * Display a snackbar with an informational message
	 * 
	 * @param v view to display the snackbar on
	 * @param res resource id of the message to display
	 */
	public static void barInfo(View v, int res) {
		barInfo(v, res, Snackbar.LENGTH_LONG);
	}
	
	/**
	 * Display a snackbar with an informational message for a short duration
	 * 
	 * @param v view to display the snackbar on
	 * @param res resource id of the message to display
	 */
	public static void barInfoShort(View v, int res) {
		barInfo(v, res, Snackbar.LENGTH_SHORT);
	}
	
	/**
	 * Display a snackbar with an informational message for a short duration
	 * 
	 * @param activity activity calling us
	 * @param res resource id of the message to display
	 */
	public static void barInfoShort(Activity activity, int res) {
		if (activity != null) {
			barInfo(activity.findViewById(android.R.id.content), res, Snackbar.LENGTH_SHORT);
		}
	}
	
	/**
	 * Display a snackbar with an informational message
	 * 
	 * @param v view to display the snackbar on
	 * @param res resource id of the message to display
	 * @param duration hw long to display the message in ms
	 */
	public static void barInfo(View v, int res, int duration) {
		Snackbar snackbar = Snackbar.make(v, res, duration);
		snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, android.R.color.holo_green_light));
		snackbar.show();
	}	
	
	/**
	 * Display a snackbar with an informational message
	 * 
	 * @param activity activity calling us
	 * @param msg message to display
	 */
	public static void barInfo(Activity activity, String msg) {
		if (activity != null) {
			barInfo(activity.findViewById(android.R.id.content), msg);
		}
	}
	
	/**
	 * Display a snackbar with an informational message for a short duration
	 * 
	 * @param v view to display the snackbar on
	 * @param msg message to display
	 */
	public static void barInfoShort(View v, String msg) {
		barInfo(v, msg, Snackbar.LENGTH_SHORT);
	}
	
	/**
	 * Display a snackbar with an informational message for a short duration
	 * 
	 * @param activity activity calling us
	 * @param msg message to display
	 */
	public static void barInfoShort(Activity activity, String msg) {
		if (activity != null) {
			barInfo(activity.findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT);
		}
	}
	
	/**
	 * Display a snackbar with an informational message
	 * 
	 * @param v view to display the snackbar on
	 * @param msg message to display
	 * @param duration hw long to display the message in ms
	 */
	public static void barInfo(View v, String msg, int duration) {
		Snackbar snackbar = Snackbar.make(v, msg, duration);
		snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, android.R.color.holo_green_light));
		snackbar.show();
	}
	
	/**
	 * Display a snackbar with an informational message
	 * 
	 * @param v view to display the snackbar on
	 * @param msg message to display
	 */
	public static void barInfo(View v, String msg) {
		Snackbar snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG);
		snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, android.R.color.holo_green_light));
		snackbar.show();
	}
	
	/**
	 * Display a snackbar with an informational message with a possible action
	 * 
	 * @param activity activity calling us
	 * @param msg message to display
	 * @param action action textRes resource for the text of an action
	 * @param listener called when action is selected
	 */
	public static void barInfo(Activity activity, String msg, int actionRes, View.OnClickListener listener) {
		if (activity != null) {
			barInfo(activity.findViewById(android.R.id.content), msg, actionRes, listener);
		}
	}
	
	/**
	 * Display a snackbar with an informational message with a possible action
	 * 
	 * @param v view to display the snackbar on
	 * @param msg message to display
	 * @param action action textRes resource for the text of an action
	 * @param listener called when action is selected
	 */
	public static void barInfo(View v, String msg, int actionRes, View.OnClickListener listener) {
		Snackbar snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG);
		snackbar.setDuration(5000);
		snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, android.R.color.holo_green_light));
		snackbar.setActionTextColor(ContextCompat.getColor(v.getContext(), R.color.ccc_white));
		snackbar.setAction (actionRes, listener);
		snackbar.show();
	}
	
	/**
	 * Display a snackbar with an informational message with a possible action
	 * 
	 * @param activity activity calling us
	 * @param msg message to display
	 * @param action action text
	 * @param listener called when action is selected
	 */
	public static void barInfo(Activity activity, String msg, String action, View.OnClickListener listener) {
		if (activity != null) {
			barInfo(activity.findViewById(android.R.id.content), msg, action, listener);
		}
	}
	
	/**
	 * Display a snackbar with an informational message with a possible action
	 * 
	 * @param v view to display the snackbar on
	 * @param msg message to display
	 * @param action action text
	 * @param listener called when action is selected
	 */
	public static void barInfo(View v, String msg, String action, View.OnClickListener listener) {
		Snackbar snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG);
		snackbar.setDuration(SHOW_DURATION_ACTION);
		snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, android.R.color.holo_green_light));
		snackbar.setActionTextColor(ContextCompat.getColor(v.getContext(), R.color.ccc_white));
		snackbar.setAction(action, listener);
		snackbar.show();
	}

	/**
	 * Display a snackbar with a warning
	 * 
	 * @param activity activity calling us
	 * @param res resource id of the message to display
	 */
	public static void barWarning(Activity activity, int res) {
		if (activity != null) {
			barWarning(activity.findViewById(android.R.id.content), res, Snackbar.LENGTH_LONG);
		}
	}
	
	/**
	 * Display a snackbar with a warning for a short duration
	 * 
	 * @param activity activity calling us
	 * @param res resource id of the message to display
	 */
	public static void barWarningShort(Activity activity, int res) {
		if (activity != null) {
			barWarning(activity.findViewById(android.R.id.content), res, Snackbar.LENGTH_SHORT);
		}
	}
	
	/**
	 * Display a snackbar with a warning
	 * 
	 * @param v view to display the snackbar on
	 * @param res resource id of the message to display
	 * @param duration hw long to display the message in ms
	 */
	public static void barWarning(View v, int res, int duration) {
		Snackbar snackbar = Snackbar.make(v, res, duration);
		snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_warning, android.R.color.holo_green_light));
		// snackbar.getView().setBackgroundColor(R.color.ccc_ocher);
		snackbar.show();
	}	
}
