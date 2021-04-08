package de.blau.android.bookmarks;

import androidx.annotation.NonNull;

import java.io.Serializable;

import de.blau.android.osm.ViewBox;
/**
 * Storage for a comments and viewbox for a bookmark
 *
 */
public class BookmarksStorage implements Serializable {
    public ViewBox viewBox;
    public String comments;

    /**
     * Sets the comments and viewbox for a bookmark object
     *
     * @param comments Bookmark name/comment
     * @param viewBox Map viewbox
     */
    public void set(@NonNull String comments ,@NonNull ViewBox viewBox){
        this.comments = comments;
        this.viewBox = viewBox;
    }

    /**
     * Return the comments for a bookmark object
     *
     * @return an AlertDialog instance
     */
    @NonNull
    public String getComment(){
        return comments;
    }
}
















