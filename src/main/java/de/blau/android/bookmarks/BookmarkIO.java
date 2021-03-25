package de.blau.android.bookmarks;

import android.content.Context;

import java.util.ArrayList;

import de.blau.android.osm.ViewBox;
import de.blau.android.util.SavingHelper;


public class  BookmarkIO {
    private BookmarksStorage currentBookmarkStorage = new BookmarksStorage();
    private ArrayList<BookmarksStorage> bookmarksStorage = new ArrayList<>();
    private final String fileName = "bookmarks.ser";
    SavingHelper<ArrayList<BookmarksStorage>> savingHelper = new SavingHelper();

    /**
     * adding a bookmark storage object to arraylist
     */
    public void addDatatolist(String s, ViewBox viewbox) {
        currentBookmarkStorage.set(s, viewbox);
        bookmarksStorage.add(currentBookmarkStorage);
    }
    /**
     * (over)Writes the bookmark file to storage.
     */
    public void writeList(Context context) {
        savingHelper.save(context, fileName, bookmarksStorage, true);
    }
    /**
     * Reads the bookmark file from storage.
     */
    public ArrayList<BookmarksStorage> readList(Context context) {

        ArrayList<BookmarksStorage> savedList = savingHelper.load(context,fileName,true);
        if(savedList==null){
            writeList(context);
            readList(context);
        }
        return savedList;

    }
    /**
     * Utility for bookmark saver
     */
    public void writer(Context context,String message,ViewBox viewBox){
        this.bookmarksStorage = readList(context);
        addDatatolist(message,viewBox);
        writeList(context);


    }

}




