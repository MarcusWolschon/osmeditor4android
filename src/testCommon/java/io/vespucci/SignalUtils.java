package io.vespucci;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

public final class SignalUtils {

    /**
     * Stop instantiation
     */
    private SignalUtils() {
        // empty
    }
    
    /**
     * Wait for the CountDownLatch to be triggered
     * 
     * @param signal the CountDownLatch
     * @param timeout timeout in s
     */
    public static void signalAwait(@NonNull final CountDownLatch signal, long timeout) {
        try {
            assertTrue(signal.await(timeout, TimeUnit.SECONDS));
        } catch (InterruptedException e) { // NOSONAR
            fail(e.getMessage());
        }
    }
}