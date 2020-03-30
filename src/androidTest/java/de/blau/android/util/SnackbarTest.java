package de.blau.android.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.android.material.snackbar.Snackbar;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import android.view.View;
import de.blau.android.Main;
import de.blau.android.TestUtils;

/**
 *
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SnackbarTest {

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

        Snackbar s1 = Snackbar.make(v, "Test1", Snackbar.LENGTH_LONG);
        Snackbar s2 = Snackbar.make(v, "Test2", Snackbar.LENGTH_LONG);
        Snackbar s3 = Snackbar.make(v, "Test3", Snackbar.LENGTH_LONG);
        Snackbar s4 = Snackbar.make(v, "Test4", Snackbar.LENGTH_LONG);

        Snack.enqueue(Snack.infoQueue, s1);
        Snack.enqueue(Snack.infoQueue, s2);
        Snack.enqueue(Snack.infoQueue, s3);

        Assert.assertTrue(Snack.infoQueue.contains(s1));
        Assert.assertTrue(Snack.infoQueue.contains(s2));
        Assert.assertTrue(Snack.infoQueue.contains(s3));

        // just to be sure that our assumptions are true
        Assert.assertEquals(3, Snack.QUEUE_CAPACITY); // NOSONAR
        Snack.enqueue(Snack.infoQueue, s4);

        Assert.assertFalse(Snack.infoQueue.contains(s1));
        Assert.assertTrue(Snack.infoQueue.contains(s2));
        Assert.assertTrue(Snack.infoQueue.contains(s3));
        Assert.assertTrue(Snack.infoQueue.contains(s4));
    }

    /**
     * Test that an info message is shown
     */
    @Test
    public void infoQueue() {
        Snackbar s = Snackbar.make(v, "Test", Snackbar.LENGTH_LONG);
        final CountDownLatch signal = new CountDownLatch(1);
        s.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar s, int event) {
            }

            @Override
            public void onShown(Snackbar sb) {
                signal.countDown();
            }
        });
        Snack.enqueueInfo(s);
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
        final CountDownLatch signal = new CountDownLatch(1);
        s.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar s, int event) {
            }

            @Override
            public void onShown(Snackbar sb) {
                signal.countDown();
            }
        });
        Snack.enqueueInfo(s);
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
        final CountDownLatch signal = new CountDownLatch(1);
        s.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar s, int event) {
            }

            @Override
            public void onShown(Snackbar sb) {
                signal.countDown();
            }
        });
        Snack.enqueueInfo(s);
        try {
            signal.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }
}
