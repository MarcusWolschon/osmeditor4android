package de.blau.android.dialogs.bookmarks;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.bookmarks.BookmarkIO;
import de.blau.android.bookmarks.BookmarksStorage;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog showing the saved bookmarks
 */
public class BookmarksDialog implements BookmarkListAdapter.Listeners {

    private ArrayList<BookmarksStorage> bookmarksStorages;
    final Map                           map = App.getLogic().getMap();
    private AlertDialog                 dialog;
    private BookmarkListAdapter         adapter;
    private Activity                    activity;
    private BookmarkIO                  bookmarkIO;

    /**
     * BookmarksDialog Constructor
     *
     * @param activity the calling activity
     */
    public BookmarksDialog(@NonNull Activity activity) {
        this.activity = activity;
        bookmarkIO = new BookmarkIO();
        bookmarksStorages = bookmarkIO.readList(activity);
    }

    /**
     * Builds show bookmarks dialog
     *
     * @param activity the calling activity
     * @param bookmarksStorages the bookmark arraylist
     * @return return the built alertdialog
     */
    @NonNull
    private AlertDialog bookmarkSelectDialog(@NonNull Activity activity, @NonNull ArrayList<BookmarksStorage> bookmarksStorages) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);
        final View layout = themedInflater.inflate(R.layout.bookmark_dialog, null);

        builder.setView(layout);
        builder.setTitle(R.string.bookmarkstitle);
        builder.setNegativeButton(R.string.done, null);

        dialog = builder.create();

        RecyclerView bookmarksList = layout.findViewById(R.id.bookmarkslist);
        LayoutParams viewLayoutParams = bookmarksList.getLayoutParams();
        viewLayoutParams.width = LayoutParams.MATCH_PARENT;

        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        bookmarksList.setLayoutManager(layoutManager);

        adapter = new BookmarkListAdapter(bookmarksStorages, viewLayoutParams, this);
        bookmarksList.setAdapter(adapter);

        return dialog;
    }

    /**
     * Displays the constructed dialog
     */
    public void showDialog() {
        bookmarkSelectDialog(activity, this.bookmarksStorages).show();
    }

    /**
     * Options menu delete listener
     *
     * @param position id of the clicked view
     */
    @Override
    public void onDeleteListener(int position) {
        adapter.notifyItemRemoved(position);
        this.bookmarksStorages.remove(position);
        bookmarkIO.writeList(activity, this.bookmarksStorages);
    }

    /**
     * Tap listener, Navigates to the viewbox
     *
     * @param position id of the clicked view
     */
    @Override
    public void onGoListener(int position) {
        map.getViewBox().fitToBoundingBox(map, this.bookmarksStorages.get(position).viewBox);
        map.invalidate();
        dialog.dismiss();
    }
}
