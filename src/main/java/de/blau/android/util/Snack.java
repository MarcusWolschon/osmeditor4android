package de.blau.android.util;

import java.util.LinkedList;
import java.util.Random;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.BaseTransientBottomBar;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import de.blau.android.R;

/**
 * Helper methods to display Snackbars in a consistent way and queuing on one of three priority queues
 * 
 * @author simon
 *
 */
public final class Snack {

    private static final String DEBUG_TAG = Snack.class.getName();

    private static final int SHOW_DURATION_ACTION = 5000;

    protected static final int QUEUE_CAPACITY = 3;

    private static final String NULL_VIEW_IN_BAR_INFO    = "null View in barInfo";
    private static final String NULL_VIEW_IN_BAR_ERROR   = "null View in barError";
    private static final String NULL_VIEW_IN_BAR_WARNING = "null View in barWarning";
    private static final String LOG_BAR_WARNING          = "barWarning got ";
    private static final String LOG_BAR_INFO             = "barInfo got ";
    private static final String LOG_BAR_ERROR            = "barError got ";

    private static final Object queueLock = new Object();

    // google has declared Snackbar as final, making life difficult for everybody ....
    protected static LinkedList<Snackbar> infoQueue    = new LinkedList<>();
    protected static LinkedList<Snackbar> warningQueue = new LinkedList<>();
    protected static LinkedList<Snackbar> errorQueue   = new LinkedList<>();

    private static final Random random = new Random();

    /**
     * Private constructor to stop instantiation
     */
    private Snack() {
        // private
    }

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
                if (!infoQueue.remove(s) && !warningQueue.remove(s)) {
                    errorQueue.remove(s);
                }
                if (!showFirst(errorQueue) && !showFirst(warningQueue)) {
                    showFirst(infoQueue);
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
            Log.e(DEBUG_TAG, NULL_VIEW_IN_BAR_ERROR);
            return;
        }
        try {
            barError(v, Snackbar.make(v, res, BaseTransientBottomBar.LENGTH_LONG));
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_ERROR + e.getMessage());
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
            Log.e(DEBUG_TAG, NULL_VIEW_IN_BAR_ERROR);
            return;
        }
        try {
            barError(v, Snackbar.make(v, msg, BaseTransientBottomBar.LENGTH_LONG));
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_ERROR + e.getMessage());
        }
    }

    /**
     * Display a snackbar with an error message
     * 
     * @param v view to display the snackbar on
     * @param snackbar the Snackbar to display
     */
    private static void barError(@NonNull View v, @NonNull Snackbar snackbar) {
        try {
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_error, R.color.material_red));
            snackbar.addCallback(callback);
            enqueueError(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_ERROR + e.getMessage());
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
            Log.e(DEBUG_TAG, NULL_VIEW_IN_BAR_ERROR);
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, msgRes, BaseTransientBottomBar.LENGTH_LONG);
            snackbar.setDuration(SHOW_DURATION_ACTION);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_error, R.color.material_red));
            snackbar.setActionTextColor(ContextCompat.getColor(v.getContext(), R.color.ccc_white));
            snackbar.setAction(actionRes, listener);
            snackbar.addCallback(callback);
            enqueueError(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_ERROR + e.getMessage());
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
        barInfo(v, res, BaseTransientBottomBar.LENGTH_LONG);
    }

    /**
     * Display a snackbar with an informational message for a short duration
     * 
     * @param v view to display the snackbar on
     * @param res resource id of the message to display
     */
    public static void barInfoShort(View v, int res) {
        barInfo(v, res, BaseTransientBottomBar.LENGTH_SHORT);
    }

    /**
     * Display a snackbar with an informational message for a short duration
     * 
     * @param activity activity calling us
     * @param res resource id of the message to display
     */
    public static void barInfoShort(Activity activity, int res) {
        if (activity != null) {
            barInfo(activity.findViewById(android.R.id.content), res, BaseTransientBottomBar.LENGTH_SHORT);
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
            Log.e(DEBUG_TAG, NULL_VIEW_IN_BAR_INFO);
            return;
        }
        try {
            barInfo(v, Snackbar.make(v, res, duration));
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_INFO + e.getMessage());
        }
    }

    /**
     * Display a snackbar with an informational message
     * 
     * @param v view to display the snackbar on
     * @param snackbar the Snackbar
     */
    private static void barInfo(@NonNull View v, @NonNull Snackbar snackbar) {
        try {
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, R.color.material_teal));
            snackbar.addCallback(callback);
            enqueueInfo(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_INFO + e.getMessage());
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
     * Display a snackbar with an informational message for a short duration
     * 
     * @param activity activity calling us
     * @param msg message to display
     */
    public static void barInfoShort(Activity activity, String msg) {
        if (activity != null) {
            barInfo(activity.findViewById(android.R.id.content), msg, BaseTransientBottomBar.LENGTH_SHORT);
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
            Log.e(DEBUG_TAG, NULL_VIEW_IN_BAR_INFO);
            return;
        }
        try {
            barInfo(v, Snackbar.make(v, msg, duration));
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_INFO + e.getMessage());
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
            Log.e(DEBUG_TAG, NULL_VIEW_IN_BAR_INFO);
            return;
        }
        try {
            barInfo(v, Snackbar.make(v, msg, BaseTransientBottomBar.LENGTH_LONG));
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_INFO + e.getMessage());
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
            Log.e(DEBUG_TAG, NULL_VIEW_IN_BAR_INFO);
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, msg, BaseTransientBottomBar.LENGTH_LONG);
            snackbar.setDuration(SHOW_DURATION_ACTION);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_info, R.color.material_teal));
            snackbar.setActionTextColor(ContextCompat.getColor(v.getContext(), R.color.ccc_white));
            snackbar.setAction(actionRes, listener);
            snackbar.addCallback(callback);
            enqueueInfo(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_INFO + e.getMessage());
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
            barWarning(activity.findViewById(android.R.id.content), res, BaseTransientBottomBar.LENGTH_LONG);
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
            barWarning(activity.findViewById(android.R.id.content), res, BaseTransientBottomBar.LENGTH_SHORT);
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
            Log.e(DEBUG_TAG, NULL_VIEW_IN_BAR_WARNING);
            return;
        }
        try {
            barWarning(v, Snackbar.make(v, res, duration));
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_WARNING + e.getMessage());
        }
    }

    /**
     * Display a snackbar with a warning
     * 
     * @param activity activity calling us
     * @param msg the message to display
     */
    public static void barWarning(Activity activity, @NonNull String msg) {
        if (activity != null) {
            barWarning(activity.findViewById(android.R.id.content), msg, BaseTransientBottomBar.LENGTH_LONG);
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
            Log.e(DEBUG_TAG, NULL_VIEW_IN_BAR_WARNING);
            return;
        }
        try {
            barWarning(v, Snackbar.make(v, msg, duration));
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_WARNING + e.getMessage());
        }
    }

    /**
     * Display a snackbar with a warning
     * 
     * @param v view to display the snackbar on
     * @param snackbar the Snackbar to display
     */
    private static void barWarning(@NonNull View v, @NonNull Snackbar snackbar) {
        try {
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_warning, R.color.material_yellow));
            snackbar.addCallback(callback);
            enqueueWarning(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_WARNING + e.getMessage());
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
        barWarning(activity.findViewById(android.R.id.content), msg, actionRes, listener);
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
            Log.e(DEBUG_TAG, NULL_VIEW_IN_BAR_WARNING);
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(v, msg, BaseTransientBottomBar.LENGTH_LONG);
            snackbar.setDuration(SHOW_DURATION_ACTION);
            snackbar.getView().setBackgroundColor(ThemeUtils.getStyleAttribColorValue(v.getContext(), R.attr.snack_warning, R.color.material_yellow));
            snackbar.setActionTextColor(ContextCompat.getColor(v.getContext(), R.color.ccc_white));
            snackbar.setAction(actionRes, listener);
            snackbar.addCallback(callback);
            enqueueWarning(snackbar);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, LOG_BAR_WARNING + e.getMessage());
        }

    }

    /**
     * Display an info toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msg the message to display
     */
    public static void toastTopInfo(@Nullable Context context, @NonNull String msg) {
        if (context != null) {
            toastTop(context, msg, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_info, R.color.material_teal), Toast.LENGTH_SHORT);
        }
    }

    /**
     * Display an into toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msgRes the message resource to display
     */
    public static void toastTopInfo(@Nullable Context context, int msgRes) {
        if (context != null) {
            toastTop(context, msgRes, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_info, R.color.material_teal), Toast.LENGTH_LONG);
        }
    }

    /**
     * Display a warning toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msg the message to display
     */
    public static void toastTopWarning(@Nullable Context context, @NonNull String msg) {
        if (context != null) {
            toastTop(context, msg, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_warning, R.color.material_yellow), Toast.LENGTH_LONG);
        }
    }

    /**
     * Display a warning toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msgRes the message resource to display
     */
    public static void toastTopWarning(@Nullable Context context, int msgRes) {
        if (context != null) {
            toastTop(context, msgRes, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_warning, R.color.material_yellow), Toast.LENGTH_LONG);
        }
    }

    /**
     * Display an error toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msgRes the message resource to display
     */
    public static void toastTopError(@Nullable Context context, int msgRes) {
        toastTopError(context, msgRes, true);
    }

    /**
     * Display an error toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msgRes the message resource to display
     * @param persist persist message as a notification
     */
    public static void toastTopError(@Nullable Context context, int msgRes, boolean persist) {
        if (context != null) {
            toastTop(context, msgRes, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_error, R.color.material_red), Toast.LENGTH_LONG);
            if (persist) {
                Notifications.error(context, R.string.error, context.getString(msgRes), random.nextInt());
            }
        }
    }

    /**
     * Display an error toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msg the message to display
     */
    public static void toastTopError(@Nullable Context context, @NonNull String msg) {
        toastTopError(context, msg, true);
    }

    /**
     * Display an error toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msg the message to display
     * @param persist persist message as a notification
     */
    public static void toastTopError(@Nullable Context context, @NonNull String msg, boolean persist) {
        if (context != null) {
            toastTop(context, msg, ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_error, R.color.material_red), Toast.LENGTH_LONG);
            if (persist) {
                Notifications.error(context, R.string.error, msg, random.nextInt());
            }
        }
    }

    /**
     * Display a toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msgRes the message resource to display
     * @param color background color of the message
     * @param duration how long to display the message
     */
    private static void toastTop(@Nullable Context context, int msgRes, int color, int duration) {
        if (context != null) {
            toastTop(context, context.getResources().getString(msgRes), color, duration);
        }
    }

    /**
     * Display a toast underneath the top action bar
     * 
     * @param context Android Context that called this
     * @param msg the message to display
     * @param color background color of the message
     * @param duration how long to display the message
     */
    private static void toastTop(@NonNull Context context, @NonNull String msg, int color, int duration) {
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View layout = inflater.inflate(R.layout.toast, null);
            layout.setBackgroundColor(color);
            TextView text = (TextView) layout.findViewById(R.id.text);
            text.setText(msg);

            Toast toast = new Toast(context);
            int yOffset = ThemeUtils.getActionBarHeight(context) + 5;
            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, yOffset);
            toast.setDuration(duration);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "toast failed with " + e.getMessage());
        }
    }
}
