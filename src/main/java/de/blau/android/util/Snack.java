package de.blau.android.util;

import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import de.blau.android.R;

/**
 * Helper methods to display Snackbars in a consistent way and queuing on one of three priority queues
 * 
 * @author simon
 *
 */
public class Snack {

    private static final String DEBUG_TAG = Snack.class.getName();

    private static final int SHOW_DURATION_ACTION = 5000;

    protected static final int QUEUE_CAPACITY = 3;

    private static final Object queueLock = new Object();

    // google has declared Snackbar as final, making life difficult for everybody ....
    protected static LinkedList<Snackbar> infoQueue    = new LinkedList<>();
    protected static LinkedList<Snackbar> warningQueue = new LinkedList<>();
    protected static LinkedList<Snackbar> errorQueue   = new LinkedList<>();

    /**
     * Check if we are showing a SnackBar
     * 
     * @param queue the queue
     * @return true if a SnackBar is being shown
     */
    static boolean isShowing(@NonNull LinkedList<Snackbar> queue) {
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
     * 
     * @param queue the queue to use
     * @param s the snackbar to queue
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
     * 
     * @param queue the queue to check
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
     * 
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
     * called when a snackbar has been dismissed, removes itself from the queue and shows the next eligible snackbar if
     * any.
     */
    static Snackbar.Callback callback = new Snackbar.Callback() {
        @Override
        public void onDismissed(Snackbar s, int event) {
            synchronized (queueLock) {
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
    public static void barError(@Nullable View v, int res) {
        if (v == null) {
            Log.e(DEBUG_TAG, "null View in barError");
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, res, Snackbar.LENGTH_LONG);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_error, R.color.material_red));
            snackbar.setCallback(callback);
            enqueueError(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "barError got " + e.getMessage());
        }
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
    public static void barError(@Nullable View v, @NonNull String msg) {
        if (v == null) {
            Log.e(DEBUG_TAG, "null View in barError");
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_error, R.color.material_red));
            snackbar.setCallback(callback);
            enqueueError(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "barError got " + e.getMessage());
        }
    }

    /**
     * Display a snackbar with an error message with a possible action
     * 
     * @param activity activity calling us
     * @param msgRes message to display
     * @param actionRes action textRes resource for the text of an action
     * @param listener called when action is selected
     */
    public static void barError(Activity activity, int msgRes, int actionRes, View.OnClickListener listener) {
        if (activity != null) {
            barError(activity.findViewById(android.R.id.content), msgRes, actionRes, listener);
        }
    }

    /**
     * Display a snackbar with an error message with a possible action
     * 
     * @param v view to display the snackbar on
     * @param msgRes message to display
     * @param actionRes action textRes resource for the text of an action
     * @param listener called when action is selected
     */
    public static void barError(@Nullable View v, int msgRes, int actionRes, View.OnClickListener listener) {
        if (v == null) {
            Log.e(DEBUG_TAG, "null View in barError");
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, msgRes, Snackbar.LENGTH_LONG);
            snackbar.setDuration(5000);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_error, R.color.material_red));
            snackbar.setActionTextColor(ContextCompat.getColor(v.getContext(), R.color.ccc_white));
            snackbar.setAction(actionRes, listener);
            snackbar.setCallback(callback);
            enqueueError(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "barError got " + e.getMessage());
        }
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
     * @param duration how long to display the message in ms
     */
    public static void barInfo(@Nullable View v, int res, int duration) {
        if (v == null) {
            Log.e(DEBUG_TAG, "null View in barInfo");
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, res, duration);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, R.color.material_teal));
            snackbar.setCallback(callback);
            enqueueInfo(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "barInfo got " + e.getMessage());
        }
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
     * Display a snackbar with an informational message
     * 
     * @param activity activity calling us
     * @param res string resource for the message to display
     * @param duration how long to display the message in ms
     */
    public static void barInfo(Activity activity, int res, int duration) {
        if (activity != null) {
            barInfo(activity.findViewById(android.R.id.content), res, duration);
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
     * @param duration how long to display the message in ms
     */
    public static void barInfo(@Nullable View v, @NonNull String msg, int duration) {
        if (v == null) {
            Log.e(DEBUG_TAG, "null View in barInfo");
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, msg, duration);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, R.color.material_teal));
            snackbar.setCallback(callback);
            enqueueInfo(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "barInfo got " + e.getMessage());
        }
    }

    /**
     * Display a snackbar with an informational message
     * 
     * @param v view to display the snackbar on
     * @param msg message to display
     */
    public static void barInfo(@Nullable View v, @NonNull String msg) {
        if (v == null) {
            Log.e(DEBUG_TAG, "null View in barInfo");
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, R.color.material_teal));
            snackbar.setCallback(callback);
            enqueueInfo(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "barInfo got " + e.getMessage());
        }
    }

    /**
     * Display a snackbar with an informational message with a possible action
     * 
     * @param activity activity calling us
     * @param msg message to display
     * @param actionRes action textRes resource for the text of an action
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
     * @param actionRes action textRes resource for the text of an action
     * @param listener called when action is selected
     */
    public static void barInfo(@Nullable View v, @NonNull String msg, int actionRes, View.OnClickListener listener) {
        if (v == null) {
            Log.e(DEBUG_TAG, "null View in barInfo");
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG);
            snackbar.setDuration(5000);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, R.color.material_teal));
            snackbar.setActionTextColor(ContextCompat.getColor(v.getContext(), R.color.ccc_white));
            snackbar.setAction(actionRes, listener);
            snackbar.setCallback(callback);
            enqueueInfo(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "barInfo got " + e.getMessage());
        }
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
    public static void barInfo(@Nullable View v, @NonNull String msg, String action, View.OnClickListener listener) {
        if (v == null) {
            Log.e(DEBUG_TAG, "null View in barInfo");
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG);
            snackbar.setDuration(SHOW_DURATION_ACTION);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, R.color.material_teal));
            snackbar.setActionTextColor(ContextCompat.getColor(v.getContext(), R.color.ccc_white));
            snackbar.setAction(action, listener);
            snackbar.setCallback(callback);
            enqueueInfo(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "barInfo got " + e.getMessage());
        }
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
     * @param duration how long to display the message in ms
     */
    public static void barWarning(@Nullable View v, int res, int duration) {
        if (v == null) {
            Log.e(DEBUG_TAG, "null View in barWarning");
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, res, duration);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_warning, R.color.material_yellow));
            snackbar.setCallback(callback);
            enqueueWarning(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "barWarning got " + e.getMessage());
        }
    }

    /**
     * Display a snackbar with a warning
     * 
     * @param v view to display the snackbar on
     * @param msg resource id of the message to display
     * @param duration how long to display the message in ms
     */
    public static void barWarning(@Nullable View v, @NonNull String msg, int duration) {
        if (v == null) {
            Log.e(DEBUG_TAG, "null View in barWarning");
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, msg, duration);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_warning, R.color.material_yellow));
            snackbar.setCallback(callback);
            enqueueWarning(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "barWarning got " + e.getMessage());
        }
    }

    /**
     * Display a snackbar with an warning message with a possible action
     * 
     * @param activity activity calling us
     * @param msg message to display
     * @param actionRes action textRes resource for the text of an action
     * @param listener called when action is selected
     */
    public static void barWarning(@NonNull Activity activity, @NonNull String msg, int actionRes, View.OnClickListener listener) {
        if (activity != null) {
            barWarning(activity.findViewById(android.R.id.content), msg, actionRes, listener);
        }
    }

    /**
     * Display a snackbar with an warning message with a possible action
     * 
     * @param v view to display the snackbar on
     * @param msg message to display
     * @param actionRes action text resrouce id
     * @param listener called when action is selected
     */
    public static void barWarning(@Nullable View v, @NonNull String msg, int actionRes, View.OnClickListener listener) {
        if (v == null) {
            Log.e(DEBUG_TAG, "null View in barWarning");
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG);
            snackbar.setDuration(5000);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_warning, R.color.material_yellow));
            snackbar.setActionTextColor(ContextCompat.getColor(v.getContext(), R.color.ccc_white));
            snackbar.setAction(actionRes, listener);
            snackbar.setCallback(callback);
            enqueueWarning(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, "barWarning got " + e.getMessage());
        }

    }

    /**
     * Display an info toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msg the message to display
     */
    public static void toastTopInfo(Context context, String msg) {
        toastTop(context, msg, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_info, R.color.material_teal), Toast.LENGTH_SHORT);
    }

    /**
     * Display an into toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msgRes the message resource to display
     */
    public static void toastTopInfo(Context context, int msgRes) {
        toastTop(context, msgRes, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_info, R.color.material_teal), Toast.LENGTH_LONG);
    }

    /**
     * Display a warning toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msg the message to display
     */
    public static void toastTopWarning(Context context, String msg) {
        toastTop(context, msg, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_warning, R.color.material_yellow), Toast.LENGTH_LONG);
    }

    /**
     * Display a warning toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msgRes the message resource to display
     */
    public static void toastTopWarning(Context context, int msgRes) {
        toastTop(context, msgRes, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_warning, R.color.material_yellow), Toast.LENGTH_LONG);
    }

    /**
     * Display an error toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msgRes the message resource to display
     */
    public static void toastTopError(Context context, int msgRes) {
        toastTop(context, msgRes, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_error, R.color.material_red), Toast.LENGTH_LONG);
    }

    /**
     * Display an error toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msg the message to display
     */
    public static void toastTopError(Context context, String msg) {
        toastTop(context, msg, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_error, R.color.material_red), Toast.LENGTH_LONG);
    }

    /**
     * Display a toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msgRes the message resource to display
     * @param color background color of the message
     * @param duration how long to display the message
     */
    private static void toastTop(Context context, int msgRes, int color, int duration) {
        toastTop(context, context.getResources().getString(msgRes), color, duration);
    }

    /**
     * Display a toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msg the message to display
     * @param color background color of the message
     * @param duration how long to display the message
     */
    private static void toastTop(Context context, String msg, int color, int duration) {
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View layout = inflater.inflate(R.layout.toast, null);
            layout.setBackgroundColor(color);
            TextView text = (TextView) layout.findViewById(R.id.text);
            text.setText(msg);

            Toast toast = new Toast(context);
            int yOffset = ThemeUtils.getActionBarHeight(context) + 5;
            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, yOffset);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "toast failed with " + e.getMessage());
        }
    }
}
