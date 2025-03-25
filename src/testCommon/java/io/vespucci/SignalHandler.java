package io.vespucci;

import java.util.concurrent.CountDownLatch;

import androidx.annotation.NonNull;
import io.vespucci.AsyncResult;
import io.vespucci.PostAsyncActionHandler;

public class SignalHandler implements PostAsyncActionHandler {
    final CountDownLatch signal;

    /**
     * Handler that will count down a CountDownLatch
     * 
     * @param signal the CountDownLatch
     */
    public SignalHandler(@NonNull CountDownLatch signal) {
        this.signal = signal;
    }

    @Override
    public void onSuccess() {
        signal.countDown();
    }

    @Override
    public void onError(AsyncResult result) {
        signal.countDown();
    }
}
