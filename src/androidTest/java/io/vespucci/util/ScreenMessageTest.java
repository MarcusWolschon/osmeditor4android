package io.vespucci.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.android.material.snackbar.Snackbar;

import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.Main;
import io.vespucci.TestUtils;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.ScreenMessage.MessageControl;
import io.vespucci.util.ScreenMessage.SnackbarWrapper;

/**
 *
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ScreenMessageTest {

    Main main = null;
    View v    = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        main = mActivityRule.getActivity();
        v = main.findViewById(android.R.id.content);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Queue 3 messages and then a fourth one and test if the 1st message is removed
     */
    @Test
    public void queue() {

        MessageControl s1 = new SnackbarWrapper(Snackbar.make(v, "Test1", Snackbar.LENGTH_LONG));
        MessageControl s2 = new SnackbarWrapper(Snackbar.make(v, "Test2", Snackbar.LENGTH_LONG));
        MessageControl s3 = new SnackbarWrapper(Snackbar.make(v, "Test3", Snackbar.LENGTH_LONG));
        MessageControl s4 = new SnackbarWrapper(Snackbar.make(v, "Test4", Snackbar.LENGTH_LONG));

        ScreenMessage.enqueue(ScreenMessage.infoQueue, s1);
        ScreenMessage.enqueue(ScreenMessage.infoQueue, s2);
        ScreenMessage.enqueue(ScreenMessage.infoQueue, s3);

        Assert.assertTrue(ScreenMessage.infoQueue.contains(s1));
        Assert.assertTrue(ScreenMessage.infoQueue.contains(s2));
        Assert.assertTrue(ScreenMessage.infoQueue.contains(s3));

        // just to be sure that our assumptions are true
        Assert.assertEquals(3, ScreenMessage.QUEUE_CAPACITY); // NOSONAR
        ScreenMessage.enqueue(ScreenMessage.infoQueue, s4);

        Assert.assertFalse(ScreenMessage.infoQueue.contains(s1));
        Assert.assertTrue(ScreenMessage.infoQueue.contains(s2));
        Assert.assertTrue(ScreenMessage.infoQueue.contains(s3));
        Assert.assertTrue(ScreenMessage.infoQueue.contains(s4));
    }

    /**
     * Test that an info message is shown
     */
    @Test
    public void infoQueue() {
        final Snackbar s = Snackbar.make(v, "Test", Snackbar.LENGTH_LONG);
        MessageControl message = new SnackbarWrapper(s);
        final CountDownLatch signal = new CountDownLatch(1);
        s.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar s, int event) {
                // empty
            }

            @Override
            public void onShown(Snackbar sb) {
                signal.countDown();
            }
        });
        ScreenMessage.enqueueInfo(message);
        try {
            signal.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test that a warning message is shown
     */
    @Test
    public void warningQueue() {
        Snackbar s = Snackbar.make(v, "Test", Snackbar.LENGTH_LONG);
        MessageControl message = new SnackbarWrapper(s);
        final CountDownLatch signal = new CountDownLatch(1);
        s.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar s, int event) {
                // empty
            }

            @Override
            public void onShown(Snackbar sb) {
                signal.countDown();
            }
        });
        ScreenMessage.enqueueInfo(message);
        try {
            signal.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test that an error message is shown
     */
    @Test
    public void errorQueue() {
        Snackbar s = Snackbar.make(v, "Test", Snackbar.LENGTH_LONG);
        MessageControl message = new SnackbarWrapper(s);
        final CountDownLatch signal = new CountDownLatch(1);
        s.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar s, int event) {
                // empty
            }

            @Override
            public void onShown(Snackbar sb) {
                signal.countDown();
            }
        });
        ScreenMessage.enqueueInfo(message);
        try {
            signal.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }
}
