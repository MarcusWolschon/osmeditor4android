package de.blau.android.dialogs;


import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.blau.android.R;
import de.blau.android.bookmarks.BookmarksStorage;
/**
 * Recyclerview adapter for displaying bookmarks
 *
 */
public class BookmarkListAdapter extends RecyclerView.Adapter<BookmarkListAdapter.ViewHolder> implements PopupMenu.OnMenuItemClickListener {

    private List<BookmarksStorage> bookmarksStorages;
    private LayoutParams viewLayoutParams;
    private int adapterPosition;
    private Listeners listener;

    public BookmarkListAdapter(@NonNull ArrayList<BookmarksStorage> bookmarksStorages,@NonNull ViewGroup.LayoutParams viewLayoutParams,@NonNull Listeners listener){
        this.bookmarksStorages = bookmarksStorages;
        this.viewLayoutParams = viewLayoutParams;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bookmark_adapter,parent,false);
        view.setLayoutParams(viewLayoutParams);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.comments.setText(bookmarksStorages.get(position).comments);
    }

    @Override
    public int getItemCount() {
        if(this.bookmarksStorages==null){
            return 0;
        }else{
            return this.bookmarksStorages.size();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView comments;
        ImageView options;

        public ViewHolder(View view) {
            super(view);
            comments = itemView.findViewById(R.id.bookmarkname);
            options  = itemView.findViewById(R.id.textViewOptions);

            //Handles view tap
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapterPosition = getAdapterPosition();
                    listener.OnGoListener(adapterPosition);
                }
            });
            //Handles options menu tap
            options.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapterPosition = getAdapterPosition();
                    showOptions(options);
                }
            });
        }
    }

    /**
     * Display a popmenu with bookmark delete option
     *
     * @param view the view involved
     */
    private void showOptions(@NonNull View view){
        PopupMenu popupMenu = new PopupMenu(view.getContext(),view);
        popupMenu.inflate(R.menu.bookmark_popup);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }

    /**
     * handle menu item click
     *
     * @param item menu item
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bookmarkdiscard:
                listener.OnDeleteListener(adapterPosition);
                return true;
            default:
                return false;
        }
    }

    public interface Listeners{
        void OnDeleteListener(int position);
        void OnGoListener(int position);
    }
}