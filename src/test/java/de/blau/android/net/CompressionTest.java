package de.blau.android.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParserException;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.ShadowWorkManager;
import de.blau.android.UnitTestUtils;
import de.blau.android.exception.NoOAuthConfigurationException;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.Server;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.VespucciURLActivity;
import de.blau.android.prefs.API.AuthParams;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.NoteComment;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk = 33)
@LargeTest
public class CompressionTest {

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
        HttpUrl mockBaseUrl = mockServer.server().url("");
        main = Robolectric.buildActivity(Main.class).create().resume().get();
        prefDB = new AdvancedPrefDatabase(main);
        prefDB.deleteAPI("Test");
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, new AuthParams(API.Auth.BASIC, null, null, null, null), true);
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
     * Empty request body, check that it isn't compressed
     */
    @Test
    public void emptyRequestBody() {
        mockServer.enqueue("200");
        Server server = prefs.getServer();
        Note note = new Note(0, 0);
        try {
            server.addNote(note, new NoteComment(note, "Test"));
        } catch (XmlPullParserException | IOException e) {
            fail(e.getMessage());
        }
        UnitTestUtils.runLooper();
        try {
            RecordedRequest request = mockServer.takeRequest();
            byte[] buffer = new byte[(int) request.getBodySize()];
            request.getBody().readFully(buffer);
            assertEquals(0, buffer.length);
        } catch (InterruptedException | EOFException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Request body with content, check that it was compressed
     */
    @Test
    public void compressedRequestBody() {
        mockServer.enqueue("changeset1");
        Server server = prefs.getServer();
        try {
            server.openChangeset(false, "TestTest", null, null, null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        UnitTestUtils.runLooper();
        try {
            RecordedRequest request = mockServer.takeRequest();
            GZIPInputStream is = new GZIPInputStream(request.getBody().inputStream());
            String s = new String(is.readAllBytes());
            assertTrue(s.contains("<tag k=\"comment\" v=\"TestTest\" />"));
        } catch (InterruptedException | IOException e) {
            fail(e.getMessage());
        }
    }
}
