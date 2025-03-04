package de.blau.android.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.prefs.API.Auth;
import de.blau.android.prefs.API.AuthParams;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class AdvancedPrefDatabaseTest {

    /**
     * Check that setting access tokens only affects the relevant API entries
     */
    @Test
    public void setAPIAccessTokenTest() {
        final Context ctx = ApplicationProvider.getApplicationContext();
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ctx)) {
            API current = db.getCurrentAPI();
            assertEquals(AdvancedPrefDatabase.ID_DEFAULT, current.id);
            assertEquals("OpenStreetMap", current.name);
            assertEquals(Auth.OAUTH2, current.auth);
            db.addAPI("test_1", "test_1", current.url, null, null, new AuthParams(current.auth, null, null, null, null));
            db.addAPI("test_2", "test_2", current.url, null, null, new AuthParams(Auth.OAUTH1A, null, null, null, null));
            db.setAPIAccessToken("12345", "67890");
            API[] test1 = db.getAPIs("test_1");
            assertEquals(1, test1.length);
            assertEquals("12345", test1[0].accesstoken);
            assertEquals("67890", test1[0].accesstokensecret);
            API[] test2 = db.getAPIs("test_2");
            assertEquals(1, test2.length);
            assertNull(test2[0].accesstoken);
            assertNull(test2[0].accesstokensecret);
            API[] sandbox = db.getAPIs(AdvancedPrefDatabase.ID_SANDBOX);
            assertEquals(1, sandbox.length);
            assertNull(sandbox[0].accesstoken);
            assertNull(sandbox[0].accesstokensecret);
        }
    }

    /**
     * Check tokens get zapped if we set Auth.BASIC
     */
    @Test
    public void setAPIDescriptorsTest() {
        final Context ctx = ApplicationProvider.getApplicationContext();
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ctx)) {
            API current = db.getCurrentAPI();
            assertEquals(AdvancedPrefDatabase.ID_DEFAULT, current.id);
            assertEquals("OpenStreetMap", current.name);
            assertEquals(Auth.OAUTH2, current.auth);
            db.addAPI("test_1", "test_1", current.url, null, null, new AuthParams(current.auth, null, null, null, null));
            db.setAPIAccessToken("12345", "67890");

            API[] test1 = db.getAPIs("test_1");
            assertEquals(1, test1.length);
            assertEquals("12345", test1[0].accesstoken);
            assertEquals("67890", test1[0].accesstokensecret);
            db.setAPIDescriptors(test1[0].id, test1[0].name, test1[0].url, null, null, Auth.BASIC);
            test1 = db.getAPIs("test_1");
            assertEquals(1, test1.length);
            assertNull(test1[0].accesstoken);
            assertNull(test1[0].accesstokensecret);
        }
    }
}
