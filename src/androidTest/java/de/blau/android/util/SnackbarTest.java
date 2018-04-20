package de.blau.android.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.design.widget.Snackbar;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
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

    @Before
    public void setup() {
        main = mActivityRule.getActivity();
        v = main.findViewById(android.R.id.content);
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
    }

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

        Assert.assertEquals(Snack.QUEUE_CAPACITY, 3); // just to be sure that our assumptions are true
        Snack.enqueue(Snack.infoQueue, s4);

        Assert.assertFalse(Snack.infoQueue.contains(s1));
        Assert.assertTrue(Snack.infoQueue.contains(s2));
        Assert.assertTrue(Snack.infoQueue.contains(s3));
        Assert.assertTrue(Snack.infoQueue.contains(s4));
    }

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
