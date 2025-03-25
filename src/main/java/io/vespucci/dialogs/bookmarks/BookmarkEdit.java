package io.vespucci.dialogs.bookmarks;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import io.vespucci.R;
import io.vespucci.Main;
import io.vespucci.dialogs.TextLineDialog;

/**
 * Display a dialog asking for the name of the viewbox bookmark
 */
public class BookmarkEdit {

    private static AppCompatDialog dialog;

    public interface HandleResult {

        /**
         * Saves the current viewbox to bookmarks when called
         *
         * @param comments comments for the current viewbox bookmark
         * @param activity the calling activity
         */
        void onSuccess(@NonNull String comments, @NonNull Context context);

        /**
         * Call this if an error occurs
         */
        void onError(@NonNull Context context);
    }

    /**
     * Show a dialog and ask the user for input
     *
     * @param activity the calling FragmentActivity
     * @param current existing name if any
     * @param handler a handler for the results
     */
    public static void get(@NonNull final FragmentActivity activity, @Nullable String current, @NonNull final BookmarkEdit.HandleResult handler) {

        dialog = TextLineDialog.get(activity, current == null ? R.string.add_bookmark_title : R.string.edit_bookmark_title, R.string.bookmarks_desc, current,
                (input, check) -> {
                    String comments = input.getText().toString();
                    handler.onSuccess(comments, activity);
                    if (activity instanceof Main) {
                        io.vespucci.layer.bookmarks.MapOverlay layer = ((Main) activity).getMap().getBookmarksLayer();
                        if (layer != null) {
                            layer.invalidate();
                        }
                    }
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