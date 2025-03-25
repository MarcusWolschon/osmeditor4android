package io.vespucci.net;

import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.LayerUtils;
import io.vespucci.Main;
import io.vespucci.TestUtils;
import io.vespucci.net.UrlCheck;
import io.vespucci.net.UrlCheck.CheckStatus;
import io.vespucci.prefs.Preferences;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UrlCheckTest {

    MockWebServerPlus mockServer  = null;
    Context           context     = null;
    Main              main        = null;
    UiDevice          device      = null;
    HttpUrl           mockBaseUrl = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext();
        main = mActivityRule.getActivity();
        mockServer = new MockWebServerPlus();
        mockBaseUrl = mockServer.server().url("/");
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        System.out.println("mock url " + mockBaseUrl.toString()); // NOSONAR
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
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
    }

    /**
     * Check http url
     */
    @Test
    public void httpUrlExists() {
        mockServer.enqueue("urlcheck1");
        mockServer.enqueue("urlcheck2");
        UrlCheck.Result result = UrlCheck.check(main, mockBaseUrl.host() + ":" + mockBaseUrl.port());
        assertSame(CheckStatus.HTTP, result.getStatus());
    }

    // /**
    // * Check https url
    // */
    // @Test
    // public void httpsUrlExists() {
    // SSLContext sslContext = null;
    // try {
    // sslContext = SSLContext.getDefault();
    // } catch (NoSuchAlgorithmException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // mockServer.server().useHttps(sslContext.getSocketFactory(), false);
    // mockServer.enqueue("urlcheck1");
    // mockServer.enqueue("urlcheck2");
    // UrlCheck.Result result = UrlCheck.check(main, mockBaseUrl.host() + ":" + mockBaseUrl.port());
    // System.out.println(result.toString());
    // }

    /**
     * Check non-existent
     */
    @Test
    public void noUrlExists() {
        UrlCheck.Result result = UrlCheck.check(main, mockBaseUrl.host() + ":" + mockBaseUrl.port());
        assertSame(CheckStatus.UNREACHABLE, result.getStatus());
    }

    /**
     * Check http url with a url supplied
     */
    @Test
    public void httpUrlExistsWithUrl() {
        mockServer.enqueue("urlcheck1");
        mockServer.enqueue("urlcheck2");
        UrlCheck.Result result = UrlCheck.check(main, "http://" + mockBaseUrl.host() + ":" + mockBaseUrl.port());
        assertSame(CheckStatus.HTTP, result.getStatus());
    }
}
