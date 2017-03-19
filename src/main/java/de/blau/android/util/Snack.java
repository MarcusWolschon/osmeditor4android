package de.blau.android.util;

import java.util.LinkedList;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;
import de.blau.android.R;

/**
 * Helper methods to display Snackbars in a consistent way and queuing on one of three priority queues
 * @author simon
 *
 */
public class Snack {
	
	private static final int SHOW_DURATION_ACTION = 5000;
	
	protected static final int QUEUE_CAPACITY = 3;
	
	private static Object queueLock = new Object();
	// google has declared Snackbar as final, making life difficult for everybody ....
	protected static LinkedList<Snackbar> infoQueue = new LinkedList<Snackbar>();
	protected static LinkedList<Snackbar> warningQueue = new LinkedList<Snackbar>();
	protected static LinkedList<Snackbar> errorQueue = new LinkedList<Snackbar>();

	static boolean isShowing(LinkedList<Snackbar> queue) {
		Snackbar first = queue.peekFirst();
		if (first != null) {
			return first.isShown();
		}
		return false;
	}
	
	/**
	 * Enqueue a snackbar on a queue removing items if space is exhausted
	 * 
	 * Note: caller needs to provide synchronization
	 * @param queue
	 * @param s
	 */
	static void enqueue(LinkedList<Snackbar> queue, Snackbar s) {
		if (queue.size() >= QUEUE_CAPACITY) {
			Snackbar first = queue.peekFirst();
			if (first != null) {
				queue.removeFirst();
				if (first.isShown()) {
					first.dismiss(); // will try to remove itself but that is OK
				}
			}
		}
		queue.offer(s);
	}
	
	/**
	 * Show the first snackbar on the queue if any and not already being shown
	 * 
	 * Note: caller needs to provide synchronization
	 * @param queue to check
	 * @return true if a snackbar was found and shown
	 */
	static boolean showFirst(LinkedList<Snackbar> queue) {
		Snackbar first = queue.peekFirst();
		if (first != null && !first.isShown()) {
			first.show();
			return true;
		}
		return false;
	}
	
	/**
	 * Dismiss the first entry on the queue if it is being shown
	 * 
	 * Note: caller needs to provide synchronization
	 * @param queue the queue to check
	 */
	static void dismiss(LinkedList<Snackbar> queue) {
		Snackbar first = queue.peekFirst();
		if (first != null && !first.isShown()) {
			first.dismiss();
		}
	}
	
	/**
	 * Enqueue a snackbar on the info queue
	 * 
	 * @param s the snackbar to queue
	 */
	static void enqueueInfo(Snackbar s) {
		synchronized (queueLock) {
			enqueue(infoQueue, s);
			if (!isShowing(warningQueue) && !isShowing(errorQueue)) {
				showFirst(infoQueue);
			}
		}
	}
	
	/**
	 * Enqueue a snackbar on the warning queue
	 * 
	 * @param s the snackbar to queue
	 */
	static void enqueueWarning(Snackbar s) {
		synchronized (queueLock) {
			enqueue(warningQueue, s);
			if (isShowing(errorQueue)) {
				return;
			}
			dismiss(warningQueue);
			showFirst(warningQueue);
		}
	}
	
	/**
	 * Enqueue a snackbar on the error queue
	 * 
	 * @param s the snackbar to queue
	 */
	static void enqueueError(Snackbar s) {
		synchronized (queueLock) {
			enqueue(errorQueue, s);
			dismiss(infoQueue);
			dismiss(warningQueue);
			showFirst(errorQueue);
		}
	}
	
	/**
	 * called when a snackbar has been dismissed, removes itself from the queue 
	 * and shows the next eligible snackbar if any. 
	 */
	static Snackbar.Callback callback = new Snackbar.Callback() {
		@Override
		public void onDismissed(Snackbar s, int event) {
			synchronized(queueLock) {
				if (!infoQueue.remove(s)) {
					if (!warningQueue.remove(s)) {
						errorQueue.remove(s);
					}
				}			
				if (!showFirst(errorQueue)) {
					if (!showFirst(warningQueue)) {
						showFirst(infoQueue);
					}
				}
			}
		}
	};
	
	
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
		snackbar.setCallback(callback);
		enqueueError(snackbar);
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
		snackbar.setCallback(callback);
		enqueueError(snackbar);
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
		snackbar.setCallback(callback);
		enqueueInfo(snackbar);
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
		snackbar.setCallback(callback);
		enqueueInfo(snackbar);
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
		snackbar.setCallback(callback);
		enqueueInfo(snackbar);
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
		snackbar.setCallback(callback);
		enqueueInfo(snackbar);
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
		snackbar.setCallback(callback);
		enqueueInfo(snackbar);
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
		snackbar.setCallback(callback);
		enqueueWarning(snackbar);
	}	
}
