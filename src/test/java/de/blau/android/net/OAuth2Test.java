package de.blau.android.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.ShadowWorkManager;
import de.blau.android.exception.OsmException;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.VespucciURLActivity;
import de.blau.android.prefs.API.AuthParams;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
@LargeTest
public class OAuth2Test {

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
        prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null,  new AuthParams(API.Auth.OAUTH2, null, null, null, null), false);
        prefDB.selectAPI("Test");
        System.out.println("mock api url " + mockBaseUrl.toString()); // NOSONAR
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(ApplicationProvider.getApplicationContext())) {
            KeyDatabaseHelper.replaceOrDeleteKey(keyDatabase.getWritableDatabase(), "Test", EntryType.API_OAUTH2_KEY, "1212121212", true, false, "empty",
                    mockBaseUrl.toString());
        }
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
     * Generate an authorisation url
     */
    @Test
    public void authorisationUrl() {
        try {
            OAuth2Helper oa = new OAuth2Helper(ApplicationProvider.getApplicationContext(), "Test");
            String authUrl = oa.getAuthorisationUrl(ApplicationProvider.getApplicationContext());
            // http://127.0.0.1/oauth2/authorize?response_type=code&client_id=1212121212&scope=read_prefs%20write_prefs%20write_api%20read_gpx%20write_gpx%20write_notes&redirect_uri=vespucci%3A%2Foauth2%2F&state=Test&code_challenge_method=S256&code_challenge=WbUnPg5qbjrJYrAPux3iAm0w_SS9CDFb-nXCrzdgF0E
            Uri parsed = Uri.parse(authUrl);
            assertEquals("1212121212", parsed.getQueryParameter("client_id"));
            String codeChallenge = parsed.getQueryParameter("code_challenge");
            assertNotNull(codeChallenge);
        } catch (OsmException e) {
            fail(e.getMessage());
        }
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
     * Test if starting VespucciURLActivity via the redirect url retrieves the access token
     */
    @Test
    public void accessTokenRetrieval() {
        authorisationUrl();
        mockServer.enqueue("accesstoken");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(OAuth2Helper.REDIRECT_URI + "?" + OAuth2Helper.CODE_PARAM + "=12345" + "&state=Test"));
        VespucciURLActivity activity = Robolectric.buildActivity(VespucciURLActivity.class, intent).create().start().resume().get();
        runLooper();
        API api = prefDB.getCurrentAPI();
        assertEquals("2YotnFZFEjr1zCsicMWpAA", api.accesstoken);
        try {
            RecordedRequest request = mockServer.takeRequest();
            // code=12345&grant_type=authorization_code&redirect_uri=vespucci%3A%2Foauth2%2F&client_id=1212121212&code_verifier=m....
            Uri uri = Uri.parse("http://127.0.0.1/?" + request.getBody().readUtf8());
            assertEquals("12345", uri.getQueryParameter(OAuth2Helper.CODE_PARAM));
            assertEquals("1212121212", uri.getQueryParameter(OAuth2Helper.CLIENT_ID_PARAM));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}
