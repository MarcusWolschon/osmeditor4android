package de.blau.android.bookmarks;

import java.io.Serializable;

import de.blau.android.osm.ViewBox;

public class BookmarksStorage implements Serializable {
    public ViewBox viewBox;
    public String comments;

    public void set(String s , ViewBox viewBox){
        this.comments = s;
        this.viewBox = viewBox;



    }


}
















