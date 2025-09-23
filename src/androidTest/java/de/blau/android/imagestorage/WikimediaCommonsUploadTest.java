package de.blau.android.imagestorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
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
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.SignalUtils;
import de.blau.android.TestUtils;
import de.blau.android.contract.MimeTypes;
import de.blau.android.osm.Node;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.AdvancedPrefDatabase.ImageStorageType;
import de.blau.android.resources.KeyDatabaseHelper;

/**
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WikimediaCommonsUploadTest {

    private static final String WM_TEST = "WM-TEST";

    private static final String PHOTO_FILE3 = "test3.jpg";

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
        prefDB.addImageStore(WM_TEST, WM_TEST, ImageStorageType.WIKIMEDIA_COMMONS, mockServer.url("/"), true);
        File photo3 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), PHOTO_FILE3);
        try {
            JavaResources.copyFileFromResources(PHOTO_FILE3, null, photo3);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        final CountDownLatch signal = new CountDownLatch(1);
        MediaScannerConnection.scanFile(context, new String[] { photo3.getAbsolutePath() }, new String[] { MimeTypes.JPEG },
                (String path, Uri uri) -> signal.countDown());
        SignalUtils.signalAwait(signal, 10);
        try (KeyDatabaseHelper kdb = new KeyDatabaseHelper(main); SQLiteDatabase db = kdb.getWritableDatabase()) {
            KeyDatabaseHelper.replaceOrDeleteKey(db, WM_TEST, KeyDatabaseHelper.EntryType.WIKIMEDIA_COMMONS_KEY,
                    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiJhYTQ3ZWU5MTQyNWU2NDQ3OWQ0MGRkMmYwZjBlMGVmZiIsImp0aSI6IjhlMDQ2ZjMyYjk1NGY2MTRjYzg5ODkwODg2YmY5YzEwZDkzYmQwNTgyOWQ0ZWYwZWJhZDgyMmE1NjcwYTQ5NTNjNDk5YWVkNzFmZDJjZTMyIiwiaWF0IjoxNzU3ODQ2NDcwLjYwNDI1MywibmJmIjoxNzU3ODQ2NDcwLjYwNDI1NCwiZXhwIjozMzMxNDc1NTI3MC42MDE2NjUsInN1YiI6IjE3MjgyMDA2IiwiaXNzIjoiaHR0cHM6Ly9tZXRhLndpa2ltZWRpYS5vcmciLCJyYXRlbGltaXQiOnsicmVxdWVzdHNfcGVyX3VuaXQiOjUwMDAsInVuaXQiOiJIT1VSIn0sInNjb3BlcyI6WyJiYXNpYyIsImhpZ2h2b2x1bWUiLCJjcmVhdGVlZGl0bW92ZXBhZ2UiLCJ1cGxvYWRmaWxlIiwidXBsb2FkZWRpdG1vdmVmaWxlIl19.m5mpney7aPciGTrGEr1g667PPsaWTxwpLpLlGfy5XSRD3b-U9ikEWsKRI-WTp5E75WUsfFqSAokCRQg1Kgf5Re5Pj8-SOJ7Bwodkn74Ux2NrdOgIfutTaGRUVUSFiqyrbavoFie5vyYG5Ii7NG3ZUW1rXzEmIvZcy9CMGwgFgBpEaL5K-tD4XZKuBGWRtN1DkHuIZWh6M4SYjQzKmUijHrLaeCkt9PhjMQQD0z1rCrBqgQY0oSIJ3Kap36BHco4ZCNep5BKeJJBUO40igQ0AJeiFBeTseqiOyNkvE26XbZJMmpSh4NVaH2uELABnr2cvtYhE99EpW-sPc_xrVvSGyRPJ31vc8P7mNAKX9ddAejxqiqq31sgwXYwYocnHLbykpFb8ykI74zEGl9tDcm69MEv8qwih0peFgWucSaVAfFAlrCALpDjZ5rejJAW9PNKd0CXWrU4xD767jNNn2yV6Y2R2NsfHb86o69heEKh4R9cybh_MHEwyxNOj9Yfh1S-01A-DH0ny24HX2SJ2eLLWDR0ItRsZL-xTB57jBT3hstE2uilLE3d2vOckwcsdll14NNnZAGnb5_YhsF8F_j4oXWoBsUdCPwb-IsBM3fv2fIqFoJFX2T_9HoE5cfC5NvN5TAvuvB8AZAsNRn5N6Uz6oSKHB4mcoY8_b028EE70QCs",
                    false, true, null, null);

        }
        map = main.getMap();
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        TestUtils.stopEasyEdit(main);
        map.getViewBox().fitToBoundingBox(map, map.getDataLayer().getExtent());
        logic.updateStyle();
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(device, main, 20);
        map.invalidate();
        TestUtils.unlock(device);

        device.waitForWindowUpdate(null, 2000);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
        mainScenario.moveToState(State.DESTROYED);
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex);
        }
    }

    /**
     * Upload existing and add to element
     */
    @Test
    public void uploadExisting() {
        TestUtils.clickAwayTip(device, context);

        mockServer.enqueue("wm-csrf");
        mockServer.enqueue("wm-upload-response");

        Node node = (Node) App.getDelegator().getOsmElement(Node.NAME, 3465444349L);
        assertNotNull(node);
        TestUtils.clickAtCoordinates(device, map, node.getLon(), node.getLat(), true);
        TestUtils.sleep();
        TestUtils.clickAwayTip(device, context);
        assertTrue(TestUtils.clickTextContains(device, "Toilets", true, 5000));
        node = App.getLogic().getSelectedNode();
        assertNotNull(node);
        assertEquals(3465444349L, node.getOsmId());
        assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));
        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(main.getString(R.string.menu_add_existing_image), false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_add_existing_image), true, false));
        TestUtils.selectFile(device, context, "Pictures", PHOTO_FILE3, false);

        assertTrue(TestUtils.findText(device, false, main.getString(R.string.image_upload_title)));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.image_upload), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.wikimedia_meta_title)));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.image_upload), true));

        try {
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
            mockServer.server().takeRequest(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertEquals("File:Tschopp-creativ-center-basel.jpg", node.getTagWithKey(Tags.KEY_WIKIMEDIA_COMMONS));
    }
}
