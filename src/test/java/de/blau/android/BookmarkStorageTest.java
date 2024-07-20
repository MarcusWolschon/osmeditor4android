package de.blau.android;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.app.Activity;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import de.blau.android.bookmarks.BookmarkStorage;
import de.blau.android.bookmarks.Bookmark;
import de.blau.android.osm.ViewBox;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
public class BookmarkStorageTest {
    Context         context;
    Activity        activity;
    ViewBox         viewboxtest;
    BookmarkStorage ioTest;
    Logic           logic;
    Map             map;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        activity = Robolectric.buildActivity(Main.class).create().resume().get();
        ioTest = new BookmarkStorage();
        logic = App.getLogic();
        map = logic.getMap();
        viewboxtest = map.getViewBox();
    }

    /**
     * Check read list method when no file is present
     */
    @Test
    public void readcheck() {
        Assert.assertNotNull(ioTest.readList(context));
    }

    /**
     * Write test
     */
    @Test
    public void writecheck() {
        logic = App.getLogic();
        ArrayList<Bookmark> testlist = new ArrayList<>();
        testlist.add(new Bookmark("Test String", viewboxtest));
        Assert.assertTrue(ioTest.writeList(context, testlist));
    }

    /**
     * Checks if same file is read after write
     */
    @Test
    public void readwritecheck() {
        ArrayList<Bookmark> testlist = new ArrayList<>();
        testlist.add(new Bookmark("TestString", viewboxtest));
        ioTest.writeList(context, testlist);
        ArrayList<Bookmark> testlist2 = (ArrayList<Bookmark>) ioTest.readList(context);
        Assert.assertEquals("TestString", testlist2.get(0).getComment());
    }
}