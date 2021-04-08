package de.blau.android.dialogs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;

import de.blau.android.R;

/**
 * Display a dialog asking for comments for the viewbox bookmark
 *
 */
public class BookmarksHandler {

    private static AppCompatDialog dialog;

    public interface HandleResult {
        void onSuccess(@NonNull String message,@NonNull FragmentActivity activity);
        void onError();
    }

    /**
     * Show a dialog and ask the user for input
     *
     * @param activity the calling FragmentActivity
     * @param handler a handler for the results
     */
    public static void get(@NonNull final FragmentActivity activity, @NonNull final BookmarksHandler.HandleResult handler){

        dialog = TextLineDialog.get(activity, R.string.add_bookmark,R.string.bookmarks_desc,(input , check) -> {
            String comments = input.getText().toString();
            handler.onSuccess(comments,activity);
            dismiss();

        },false);
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
