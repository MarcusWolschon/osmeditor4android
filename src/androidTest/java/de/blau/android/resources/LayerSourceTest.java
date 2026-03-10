package de.blau.android.resources;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.net.UrlCheck;
import de.blau.android.net.UrlCheck.CheckStatus;
import de.blau.android.net.UrlCheck.Result;
import de.blau.android.util.ExecutorTask;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LayerSourceTest {

    private static final String DEBUG_TAG = LayerSourceTest.class.getSimpleName();
    private static final long   TIMEOUT   = 5000;

    private static final List<String> PROBLEM_HOSTS = Arrays.asList("mbk.mkrada.gov.ua", "gis.cityofberkeley.info", "arcgis.mansfieldcity.com",
            "maps.nassauflpa.com", "maps2.cattco.org", "servisi.lgia.gov.lv", "tiles.maps.eox.at", " wms.michreichert.de");

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    private Context context;
    private Main    main;

    Map<URL, Result> done = new LinkedHashMap<>();

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        UiDevice device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext();
        main = mActivityRule.getActivity();
        try (TileLayerDatabase db = new TileLayerDatabase(main)) {
            TileLayerSource.createOrUpdateFromAssetsSource(main, db.getWritableDatabase(), false, false);
            TileLayerSource.getListsLocked(main, db.getReadableDatabase(), true);
        }
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    @Test
    public void testCertAuthority() {
        de.blau.android.layer.Util.populateImageryLists(main);
        int count = 0;
        int problems = 0;
        for (String id : TileLayerSource.getIds(null, false, null, null)) {
            count++;
            TileLayerSource source = TileLayerSource.get(context, id, false);
            Result result = connect(source.getTileUrl());
            if (result.getStatus() == CheckStatus.SSLERROR) {
                Log.d(DEBUG_TAG, "SSL broken for " + result.toString());
                problems++;
            }
        }

        for (String id : TileLayerSource.getOverlayIds(null, false, null, null)) {
            count++;
            TileLayerSource source = TileLayerSource.get(context, id, false);
            Result result = connect(source.getTileUrl());
            if (result.getStatus() == CheckStatus.SSLERROR) {
                Log.d(DEBUG_TAG, "SSL broken for " + result.toString());
                problems++;
            }
        }
        Log.d(DEBUG_TAG, "Total sources " + count);
        Log.d(DEBUG_TAG, "Total hosts " + done.size());
        Log.d(DEBUG_TAG, "Total problems " + problems);
        boolean fail = false;
        for (Entry<URL, Result> entry : done.entrySet()) {
            if (entry.getValue().getStatus() == CheckStatus.SSLERROR) {
                final String host = entry.getKey().getHost();
                if (!PROBLEM_HOSTS.contains(host)) {
                    Log.d(DEBUG_TAG, "New problem " + host);
                    fail = true;
                    continue;
                }
                Log.d(DEBUG_TAG, host);
            }
        }
        if (fail) {
            fail("New problem host(s)");
        }
    }

    /**
     * Try to connect to a remote site and return if it was possible
     * 
     * @param urlOrDomain a URL or a naked domain
     * @param https use httos if true
     * @return a Result object with the result
     */
    private Result connect(@NonNull final String inputString) {
        ExecutorTask<String, Void, Result> loader = new ExecutorTask<String, Void, Result>() {

            @Override
            protected UrlCheck.Result doInBackground(String inputString) {
                URL url = null;
                try {
                    // remove query
                    URL input = new URL(inputString);
                    url = new URL(input.getProtocol(), input.getHost(), input.getPort(), "");
                    Result cached = done.get(url);
                    if (cached != null) {
                        return cached;
                    }
                    Request request = new Request.Builder().url(url).head().build();
                    OkHttpClient.Builder builder = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT,
                            TimeUnit.MILLISECONDS);
                    OkHttpClient client = builder.build();
                    Call readCall = client.newCall(request);
                    try (Response readCallResponse = readCall.execute()) {
                        final int code = readCallResponse.code();
                        if (code == HttpURLConnection.HTTP_OK) {
                            Result result = new Result(CheckStatus.HTTPS, url.toString());
                            done.put(url, result);
                            return result;
                        }
                        Result result = new Result(CheckStatus.INVALID, url.toString(), code, readCallResponse.message());
                        done.put(url, result);
                        return result;
                    }
                } catch (MalformedURLException | IllegalArgumentException e) {
                    return new Result(CheckStatus.MALFORMEDURL, inputString);
                } catch (UnknownHostException uhe) {
                    Result result = new Result(CheckStatus.DOESNTEXIST, inputString);
                    done.put(url, result);
                    return result;
                } catch (IOException e) {
                    if (e instanceof javax.net.ssl.SSLHandshakeException) {
                        Log.d(DEBUG_TAG, "Cause: " + ((javax.net.ssl.SSLHandshakeException) e).getCause());
                        Result result = new Result(CheckStatus.SSLERROR, inputString);
                        done.put(url, result);
                        return result;
                    }
                    Result result = new Result(CheckStatus.UNREACHABLE, inputString);
                    done.put(url, result);
                    return result;
                }
            }
        };
        loader.execute(inputString);
        try {
            return loader.get();
        } catch (InterruptedException | ExecutionException e) {
            return new Result(CheckStatus.INVALID, inputString);
        }
    }
}
