package de.blau.android.dialogs;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.bookmarks.BookmarkIO;
import de.blau.android.bookmarks.BookmarksStorage;
import de.blau.android.util.ThemeUtils;

public class BookmarksDialog extends AppCompatActivity implements BookmarkListAdapter.Listeners {

    private ArrayList<BookmarksStorage> bookmarksStorages;
    final Map map = App.getLogic().getMap();
    private AlertDialog dialog;
    private BookmarkListAdapter adapter;
    private Activity activity;
    private BookmarkIO bookmarkIO = new BookmarkIO();


    public BookmarksDialog(Activity activity) {
        this.activity = activity;
        bookmarksStorages = bookmarkIO.readList(activity);
    }
    /**
     * Main Dialog
     */
    private AlertDialog bookmarkSelectDialog(Activity activity, ArrayList<BookmarksStorage> bookmarksStorages)  {


            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);
            final View layout = themedInflater.inflate(R.layout.bookmark_dialog, null);

            builder.setView(layout);
            builder.setTitle("Bookmarks");
            builder.setNegativeButton(R.string.done, null);

            final AlertDialog dialog = builder.create();



            RecyclerView bookmarksList = (RecyclerView) layout.findViewById(R.id.bookmarkslist);
            LayoutParams viewLayoutParams = bookmarksList.getLayoutParams();
            viewLayoutParams.width = LayoutParams.MATCH_PARENT;

            LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
            bookmarksList.setLayoutManager(layoutManager);

            final BookmarkListAdapter adapter = new BookmarkListAdapter(bookmarksStorages,viewLayoutParams, this);
            bookmarksList.setAdapter(adapter);

            setAdapter(adapter);
            setDialog(dialog);

            return dialog;
    }
    /**
     * ShowDialog method
     */
    public void showDialog(){

        bookmarkSelectDialog(activity,bookmarksStorages).show();
    }

    /**
     * Options menu delete listener
     */
    @Override
    public void OnDeleteListener(int position) {
            adapter.notifyItemRemoved(position);
            this.bookmarksStorages.remove(position);
            bookmarkIO.writeList(activity,this.bookmarksStorages);
    }
    /**
     * Options menu Go listener
     *
     */
    @Override
    public void OnGoListener(int position) {
        map.getViewBox().fitToBoundingBox(map,bookmarksStorages.get(position).viewBox);
        map.invalidate();
        dialog.dismiss();

    }

    public void setAdapter(BookmarkListAdapter adapter){
        this.adapter = adapter;

    }
    public void setDialog(AlertDialog dialog){
        this.dialog = dialog;

    }
}
