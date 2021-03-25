package de.blau.android.bookmarks;


import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.blau.android.R;

public class BookmarkListAdapter extends RecyclerView.Adapter<BookmarkListAdapter.ViewHolder> implements PopupMenu.OnMenuItemClickListener {

        private ArrayList<BookmarksStorage> bookmarksStorages;
        private LayoutParams viewLayoutParams;
        private int adapterPosition;
        private Listeners listener;



    public BookmarkListAdapter(ArrayList<BookmarksStorage> bookmarksStorages, ViewGroup.LayoutParams viewLayoutParams,Listeners listener){
            this.bookmarksStorages = bookmarksStorages;
            this.viewLayoutParams = viewLayoutParams;
            this.listener = listener;




    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bookmark_adapter,parent,false);
            view.setLayoutParams(viewLayoutParams);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.comments.setText(bookmarksStorages.get(position).comments);

    }

    @Override
    public int getItemCount() {
        return this.bookmarksStorages.size();
    }



    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView comments;
            TextView options;

        public ViewHolder(View view) {
            super(view);
            comments = itemView.findViewById(R.id.bookmarkname);
            options  = itemView.findViewById(R.id.textViewOptions);
            view.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            adapterPosition = getAdapterPosition();
            showOptions(options);

        }
    }
    private void showOptions(View view){
        PopupMenu popupMenu = new PopupMenu(view.getContext(),view);
        popupMenu.inflate(R.menu.bookmark_popup);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();


    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.show:
                listener.OnGoListener(adapterPosition);
                return true;

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
