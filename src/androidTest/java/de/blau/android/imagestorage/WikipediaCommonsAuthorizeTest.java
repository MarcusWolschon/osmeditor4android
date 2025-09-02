package de.blau.android.imagestorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.TestUtils;
import de.blau.android.contract.Urls;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.AdvancedPrefDatabase.ImageStorageType;
import de.blau.android.prefs.ImageStorageConfiguration;
import de.blau.android.resources.KeyDatabaseHelper;

/**
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WikipediaCommonsAuthorizeTest {

    Context                 context      = null;
    AdvancedPrefDatabase    prefDB       = null;
    Main                    main         = null;
    UiDevice                device       = null;
    Map                     map          = null;
    Logic                   logic        = null;
    private Instrumentation instrumentation;
    ActivityScenario<Main>  mainScenario = null;
    MockWebServerPlus       mockServer   = null;

    @Rule
    public ActivityScenarioRule<Main> activityScenarioRule = new ActivityScenarioRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        mainScenario = ActivityScenario.launch(Main.class);
        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        instrumentation.removeMonitor(monitor);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        mockServer = new MockWebServerPlus();
        prefDB = new AdvancedPrefDatabase(context);

    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        mainScenario.moveToState(State.DESTROYED);
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
    }

    /**
     * Parse the token page
     */
    @Test
    public void tokenExtraction() {
        Intent intent = new Intent(main, WikimediaCommonsAuthorize.class);
        intent.putExtra(WikimediaCommonsAuthorize.CONFIGURATION_KEY, new ImageStorageConfiguration(AdvancedPrefDatabase.ID_WIKIMEDIA_COMMONS,
                AdvancedPrefDatabase.ID_WIKIMEDIA_COMMONS, ImageStorageType.WIKIMEDIA_COMMONS, Urls.DEFAULT_WIKIMEDIA_COMMONS_API_URL, true));
        intent.putExtra(WikimediaCommonsAuthorize.REGISTRATION_URL_KEY, mockServer.server().url("/").toString());
        mockServer.enqueue("wm-consumer-registration");

        ActivityMonitor monitor = instrumentation.addMonitor(WikimediaCommonsAuthorize.class.getName(), null, false);

        try (ActivityScenario<WikimediaCommonsAuthorize> scenario = ActivityScenario.launch(intent)) {
            WikimediaCommonsAuthorize activity = (WikimediaCommonsAuthorize) instrumentation.waitForMonitorWithTimeout(monitor, 30000);
            
            ActivityMonitor mainMonitor = instrumentation.addMonitor(Main.class.getName(), null, false);
            instrumentation.waitForMonitorWithTimeout(mainMonitor, 30000); // wait for main to run again
            
            try {
                mockServer.server().takeRequest(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            try (KeyDatabaseHelper kdb = new KeyDatabaseHelper(activity); SQLiteDatabase db = kdb.getWritableDatabase()) {
                String token = KeyDatabaseHelper.getKey(db, AdvancedPrefDatabase.ID_WIKIMEDIA_COMMONS, KeyDatabaseHelper.EntryType.WIKIMEDIA_COMMONS_KEY);
                assertEquals(
                        "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiJhYTQ3ZWU5MTQyNWU2NDQ3OWQ0MGRkMmYwZjBlMGVmZiIsImp0aSI6IjhlMDQ2ZjMyYjk1NGY2MTRjYzg5ODkwODg2YmY5YzEwZDkzYmQwNTgyOWQ0ZWYwZWJhZDgyMmE1NjcwYTQ5NTNjNDk5YWVkNzFmZDJjZTMyIiwiaWF0IjoxNzU3ODQ2NDcwLjYwNDI1MywibmJmIjoxNzU3ODQ2NDcwLjYwNDI1NCwiZXhwIjozMzMxNDc1NTI3MC42MDE2NjUsInN1YiI6IjE3MjgyMDA2IiwiaXNzIjoiaHR0cHM6Ly9tZXRhLndpa2ltZWRpYS5vcmciLCJyYXRlbGltaXQiOnsicmVxdWVzdHNfcGVyX3VuaXQiOjUwMDAsInVuaXQiOiJIT1VSIn0sInNjb3BlcyI6WyJiYXNpYyIsImhpZ2h2b2x1bWUiLCJjcmVhdGVlZGl0bW92ZXBhZ2UiLCJ1cGxvYWRmaWxlIiwidXBsb2FkZWRpdG1vdmVmaWxlIl19.m5mpney7aPciGTrGEr1g667PPsaWTxwpLpLlGfy5XSRD3b-U9ikEWsKRI-WTp5E75WUsfFqSAokCRQg1Kgf5Re5Pj8-SOJ7Bwodkn74Ux2NrdOgIfutTaGRUVUSFiqyrbavoFie5vyYG5Ii7NG3ZUW1rXzEmIvZcy9CMGwgFgBpEaL5K-tD4XZKuBGWRtN1DkHuIZWh6M4SYjQzKmUijHrLaeCkt9PhjMQQD0z1rCrBqgQY0oSIJ3Kap36BHco4ZCNep5BKeJJBUO40igQ0AJeiFBeTseqiOyNkvE26XbZJMmpSh4NVaH2uELABnr2cvtYhE99EpW-sPc_xrVvSGyRPJ31vc8P7mNAKX9ddAejxqiqq31sgwXYwYocnHLbykpFb8ykI74zEGl9tDcm69MEv8qwih0peFgWucSaVAfFAlrCALpDjZ5rejJAW9PNKd0CXWrU4xD767jNNn2yV6Y2R2NsfHb86o69heEKh4R9cybh_MHEwyxNOj9Yfh1S-01A-DH0ny24HX2SJ2eLLWDR0ItRsZL-xTB57jBT3hstE2uilLE3d2vOckwcsdll14NNnZAGnb5_YhsF8F_j4oXWoBsUdCPwb-IsBM3fv2fIqFoJFX2T_9HoE5cfC5NvN5TAvuvB8AZAsNRn5N6Uz6oSKHB4mcoY8_b028EE70QCs",
                        token);
            }
        }
    }
}
