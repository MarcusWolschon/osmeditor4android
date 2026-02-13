package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.ShadowWorkManager;
import de.blau.android.SignalUtils;
import de.blau.android.osm.ApiTest.FailOnErrorHandler;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.API.AuthParams;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.NoteComment;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TransferTasks;
import okhttp3.HttpUrl;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk = 33)
@LargeTest
public class NotesApiTest {

    private static final String NOTES_DOWNLOAD1_FIXTURE = "notesDownload1";

    public static final int TIMEOUT = 10;

    private MockWebServerPlus    mockServer = null;
    private AdvancedPrefDatabase prefDB     = null;
    private Main                 main       = null;
    private Preferences          prefs      = null;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
        main = Robolectric.buildActivity(Main.class).create().resume().get();
        prefDB = new AdvancedPrefDatabase(main);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, "user", "pass", null, null), false);
        prefDB.selectAPI("Test");
        System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
        Logic logic = App.getLogic();
        prefs = new Preferences(main);
        logic.setPrefs(prefs);
        logic.getMap().setPrefs(main, prefs);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }
        prefDB.selectAPI(AdvancedPrefDatabase.ID_DEFAULT);
        prefDB.close();
        prefs.close();
    }

    /**
     * Super ugly hack to get the looper to run
     */
    private void runLooper() {
        try {
            Thread.sleep(3000); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            // Ignore
        }
        shadowOf(Looper.getMainLooper()).idle();
    }

    /**
     * Download Notes for a bounding box
     */
    @Test
    public void notesDownload() {
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue(NOTES_DOWNLOAD1_FIXTURE);
        App.getTaskStorage().reset();
        try {
            final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), ApiTest.GENERATOR_NAME);
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext());
            Resources r = ApplicationProvider.getApplicationContext().getResources();
            String notesSelector = r.getString(R.string.bugfilter_notes);
            Set<String> set = new HashSet<>(Arrays.asList(notesSelector));
            p.edit().putStringSet(r.getString(R.string.config_bugFilter_key), set).commit();
            final Preferences preferences = new Preferences(ApplicationProvider.getApplicationContext());
            assertTrue(preferences.taskFilter().contains(notesSelector));
            App.getLogic().setPrefs(preferences);
            TransferTasks.downloadBox(ApplicationProvider.getApplicationContext(), s, new BoundingBox(8.3844600D, 47.3892400D, 8.3879800D, 47.3911300D), false,
                    TransferTasks.MAX_PER_REQUEST, new FailOnErrorHandler(signal));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT*2);
        List<Task> tasks = App.getTaskStorage().getTasks();
        // note the fixture contains 100 notes, however 41 of them are closed and expired
        assertEquals(59, tasks.size());
        try {
            tasks = App.getTaskStorage().getTasks(new BoundingBox(-0.0918, 51.532, -0.0917, 51.533));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertTrue(tasks.get(0) instanceof Note);
        assertEquals(458427, ((Note) tasks.get(0)).getId());
    }

    /**
     * Upload a single new Note
     */
    @Test
    public void noteUpload() {
        Main main = Robolectric.setupActivity(Main.class);
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("noteUpload1");
        App.getTaskStorage().reset();
        try {
            final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), ApiTest.GENERATOR_NAME);
            Note n = new Note((int) (51.0 * 1E7D), (int) (0.1 * 1E7D));
            assertTrue(n.isNew());
            assertTrue(TransferTasks.uploadNote(main, s, n, new NoteComment(n, "ThisIsANote"), false, new FailOnErrorHandler(signal)));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        try {
            assertFalse(App.getTaskStorage().isEmpty());
            List<Task> tasks = App.getTaskStorage().getTasks(new BoundingBox(0.099, 50.99, 0.111, 51.01));
            assertEquals(1, tasks.size());
            Note n = (Note) tasks.get(0);
            assertEquals("<p>ThisIsANote</p>", n.getLastComment().getText());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Add a comment to an existing note
     * 
     * https://master.apis.dev.openstreetmap.org/api/0.6/notes/1/comment?text=New+comment
     */
    @Test
    public void addComment() {
        noteUpload();
        List<Task> tasks = App.getTaskStorage().getTasks(new BoundingBox(0.099, 50.99, 0.111, 51.01));
        Note n = (Note) tasks.get(0);
        Main main = Robolectric.setupActivity(Main.class);
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("noteComment");
        try {
            final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), ApiTest.GENERATOR_NAME);
            assertFalse(n.isNew());
            assertTrue(TransferTasks.uploadNote(main, s, n, new NoteComment(n, "New comment"), false, new FailOnErrorHandler(signal)));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        try {
            assertEquals("<p>New comment</p>", n.getLastComment().getText());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Add a comment to an existing but closed note
     * 
     */
    @Test
    public void addCommentToClosed() {
        noteUpload();
        List<Task> tasks = App.getTaskStorage().getTasks(new BoundingBox(0.099, 50.99, 0.111, 51.01));
        Note n = (Note) tasks.get(0);
        Main main = Robolectric.setupActivity(Main.class);
        final CountDownLatch signal = new CountDownLatch(1);
        mockServer.enqueue("noteAlreadyClosed");
        mockServer.enqueue("200");
        mockServer.enqueue("noteComment");
        try {
            final Server s = new Server(ApplicationProvider.getApplicationContext(), prefDB.getCurrentAPI(), ApiTest.GENERATOR_NAME);
            assertFalse(n.isNew());
            assertTrue(TransferTasks.uploadNote(main, s, n, new NoteComment(n, "New comment"), false, new FailOnErrorHandler(signal)));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        runLooper();
        SignalUtils.signalAwait(signal, TIMEOUT);
        try {
            assertEquals("<p>New comment</p>", n.getLastComment().getText());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
