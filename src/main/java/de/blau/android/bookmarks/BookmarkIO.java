package de.blau.android.bookmarks;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import de.blau.android.osm.ViewBox;
import de.blau.android.util.SavingHelper;


public class  BookmarkIO {
    private BookmarksStorage currentBookmarkStorage;
    private List<BookmarksStorage> bookmarksStorage;
    private final String fileName = "bookmarks.ser";
    SavingHelper<ArrayList<BookmarksStorage>> savingHelper;


    public BookmarkIO(){
        this.currentBookmarkStorage = new BookmarksStorage();
        this.bookmarksStorage = new ArrayList<>();
        this.savingHelper = new SavingHelper<>();
    }
    /**
     * adding a bookmark storage object to arraylist
     */
    public void addDatatolist(String comment, ViewBox viewbox) {
        currentBookmarkStorage.set(comment, viewbox);
        bookmarksStorage.add(currentBookmarkStorage);
    }
    /**
     * (over)Writes the bookmark file to storage.
     */
    public void writeList(Context context,ArrayList<BookmarksStorage> bookmarksStorage) {
        savingHelper.save(context, fileName, (ArrayList<BookmarksStorage>) bookmarksStorage, true);
    }
    /**
     * Reads the bookmark file from storage.
     */
    public ArrayList<BookmarksStorage> readList(Context context) {

        ArrayList<BookmarksStorage> savedList = savingHelper.load(context,fileName,true);
        if(savedList==null){
            return (ArrayList<BookmarksStorage>) this.bookmarksStorage;
        }
        return savedList;
    }
    /**
     * Utility for bookmark saver
     */
    public void writer(Context context,String message,ViewBox viewBox){
        this.bookmarksStorage = readList(context);
        addDatatolist(message,viewBox);
        writeList(context, (ArrayList<BookmarksStorage>) this.bookmarksStorage);
    }

}




