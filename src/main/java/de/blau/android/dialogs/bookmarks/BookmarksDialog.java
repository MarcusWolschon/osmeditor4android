package de.blau.android.dialogs.bookmarks;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.bookmarks.Bookmark;
import de.blau.android.bookmarks.BookmarkStorage;
import de.blau.android.layer.bookmarks.MapOverlay;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog showing the saved bookmarks
 */
public class BookmarksDialog implements BookmarkListAdapter.Listeners {

    private List<Bookmark>      bookmarks;
    final Map                   map = App.getLogic().getMap();
    private AlertDialog         dialog;
    private BookmarkListAdapter adapter;
    private Activity            activity;
    private BookmarkStorage     bookmarkStorage;

    /**
     * BookmarksDialog Constructor
     *
     * @param activity the calling activity
     */
    public BookmarksDialog(@NonNull Activity activity) {
        this.activity = activity;
        bookmarkStorage = new BookmarkStorage();
        bookmarks = bookmarkStorage.readList(activity);
    }

    /**
     * Builds show bookmarks dialog
     *
     * @param activity the calling activity
     * @param bookmarksStorages the bookmark arraylist
     * @return return the built alertdialog
     */
    @NonNull
    private AlertDialog bookmarkSelectDialog(@NonNull Activity activity, @NonNull List<Bookmark> bookmarksStorages) {

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
        bookmarkSelectDialog(activity, this.bookmarks).show();
    }

    /**
     * Options menu delete listener
     *
     * @param position id of the clicked view
     */
    @Override
    public void onDeleteListener(int position) {
        adapter.notifyItemRemoved(position);
        this.bookmarks.remove(position);
        bookmarkStorage.writeList(activity, this.bookmarks);
        if (activity instanceof Main) {
            MapOverlay layer = ((Main) activity).getMap().getBookmarksLayer();
            if (layer != null) {
                layer.invalidate();
            }
        }
    }

    /**
     * Tap listener, Navigates to the viewbox
     *
     * @param position id of the clicked view
     */
    @Override
    public void onGoListener(int position) {
        map.getViewBox().fitToBoundingBox(map, this.bookmarks.get(position).getViewBox());
        map.invalidate();
        dialog.dismiss();
    }
}
