package de.blau.android.dialogs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;

import de.blau.android.R;


public class BookmarksHandler {

    private static AppCompatDialog dialog;
    public interface HandleResult {

        void onSuccess(String message,FragmentActivity activity);

        void onError();
    }

    public static void get(@NonNull final FragmentActivity activity, @NonNull final BookmarksHandler.HandleResult handler){

        dialog = TextLineDialog.get(activity, R.string.add_bookmark,R.string.bookmarks_desc,(input , check) -> {
            String comments = input.getText().toString();
            handler.onSuccess(comments,activity);
            dismiss();



        },false);
        dialog.show();


    }
    private static void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
