package io.vespucci.util;

import static io.vespucci.osm.DelegatorUtil.addWayToStorage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.Manifest;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;
import androidx.work.ListenableWorker.Result;
import androidx.work.testing.TestWorkerBuilder;
import io.vespucci.R;
import io.vespucci.TestUtils;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Way;
import io.vespucci.util.UploadChecker;

@RunWith(AndroidJUnit4.class)
public class UploadCheckerTest {
    private Context  context;
    private Executor executor;
    private UiDevice device;
    
    /** this is needed for writing coverage information */
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    /**
     * Pre-test setup
     */
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        executor = Executors.newSingleThreadExecutor();
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    /**
     * Run the UploadChecker, with no unsaved changes
     */
    @Test
    public void noChanges() {
        StorageDelegator delegator = new StorageDelegator();
        try {
            delegator.writeToFile(context);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        UploadChecker worker = TestWorkerBuilder.from(context, UploadChecker.class, executor).build();

        Result result = worker.doWork();
        assertEquals(Result.success(), result);
        assertFalse(TestUtils.findNotification(device, context.getResources().getQuantityString(R.plurals.upload_checker_message, 6, 6)));
    }

    /**
     * Run the UploadChecker, with unsaved changes
     */
    @Test
    public void withChanges() {
        StorageDelegator delegator = new StorageDelegator();
        Way w = addWayToStorage(delegator, false);
        assertNotNull(w);
        try {
            delegator.writeToFile(context);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        UploadChecker worker = TestWorkerBuilder.from(context, UploadChecker.class, executor).build();

        Result result = worker.doWork();
        assertEquals(Result.success(), result);
        assertTrue(TestUtils.findNotification(device, context.getResources().getQuantityString(R.plurals.upload_checker_message, 6, 6)));
    }
}