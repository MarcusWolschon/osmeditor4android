package de.blau.android.dialogs.bookmarks;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;

import de.blau.android.R;
import de.blau.android.dialogs.TextLineDialog;

/**
 * Display a dialog asking for comments for the viewbox bookmark
 */
public class BookmarkHandler {

    private static AppCompatDialog dialog;

    public interface HandleResult {

        /**
         * Saves the current viewbox to bookmarks when called
         *
         * @param comments comments for the current viewbox bookmark
         * @param activity the calling activity
         */
        void onSuccess(@NonNull String comments, @NonNull FragmentActivity activity);

        /**
         * Call this if an error occurs
         */
        void onError();
    }

    /**
     * Show a dialog and ask the user for input
     *
     * @param activity the calling FragmentActivity
     * @param handler a handler for the results
     */
    public static void get(@NonNull final FragmentActivity activity, @NonNull final BookmarkHandler.HandleResult handler) {

        dialog = TextLineDialog.get(activity, R.string.add_bookmark, R.string.bookmarks_desc, (input, check) -> {
            String comments = input.getText().toString();
            handler.onSuccess(comments, activity);
            dismiss();

        }, false);
        dialog.show();
    }

    /**
     * Dismisses the dialog
     */
    private static void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}