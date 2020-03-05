package de.blau.android.net;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.net.UrlCheck.CheckStatus;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UrlCheckTest {

    MockWebServerPlus       mockServer  = null;
    Context                 context     = null;
    Main                    main        = null;
    private Instrumentation instrumentation;
    UiDevice                device      = null;
    HttpUrl                 mockBaseUrl = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext();
        main = mActivityRule.getActivity();
        mockServer = new MockWebServerPlus();
        mockBaseUrl = mockServer.server().url("/");
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        System.out.println("mock url " + mockBaseUrl.toString());
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
            System.out.println("Stopping mock webserver exception " + ioex);
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
        Assert.assertTrue(CheckStatus.HTTP == result.getStatus());
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
        Assert.assertTrue(CheckStatus.UNREACHABLE == result.getStatus());
    }

    /**
     * Check http url with a url supplied
     */
    @Test
    public void httpUrlExistsWithUrl() {
        mockServer.enqueue("urlcheck1");
        mockServer.enqueue("urlcheck2");
        UrlCheck.Result result = UrlCheck.check(main, "http://" + mockBaseUrl.host() + ":" + mockBaseUrl.port());
        Assert.assertTrue(CheckStatus.HTTP == result.getStatus());
    }
}
