package de.blau.android;

import android.app.Activity;
import android.content.Context;
import org.robolectric.*;


import androidx.test.core.app.ApplicationProvider;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import de.blau.android.bookmarks.BookmarkIO;
import de.blau.android.bookmarks.BookmarksStorage;
import de.blau.android.osm.ViewBox;

@RunWith(RobolectricTestRunner.class)
public class BookmarkIOTest {
    Context context;
    Activity activity;
    ViewBox viewboxtest;
    BookmarkIO IOtest;
    Logic logic;
    Map map;
    BookmarksStorage Storagetest;


    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        activity = Robolectric.buildActivity(Main.class).create().resume().get();
        IOtest = new BookmarkIO();
        Storagetest = new BookmarksStorage();
        logic = App.getLogic();
        map = logic.getMap();
        viewboxtest = map.getViewBox();
    }

    /**
     * Check read list method when no file is present
     */
    @Test
    public void readcheck() {
        Assert.assertNotNull(IOtest.readList(context));
    }

    /**
     * Write test
     */
    @Test
    public void writecheck() {
        logic = App.getLogic();
        ArrayList<BookmarksStorage> testlist = new ArrayList<>();
        testlist.add(new BookmarksStorage("Test String",viewboxtest));
        Assert.assertTrue(IOtest.writeList(context,testlist));
    }

    /**
     * Checks if same file is read after write
     */
    @Test
    public void readwritecheck() {
        ArrayList<BookmarksStorage> testlist = new ArrayList<>();
        testlist.add(new BookmarksStorage("TestString",viewboxtest));
        IOtest.writeList(context,testlist);
        ArrayList<BookmarksStorage> testlist2;
        testlist2 = IOtest.readList(context);
        Assert.assertEquals("TestString", testlist2.get(0).comments);
    }
}