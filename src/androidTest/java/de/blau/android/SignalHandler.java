package de.blau.android;

import java.util.concurrent.CountDownLatch;

import androidx.annotation.NonNull;

public class SignalHandler extends PostAsyncActionHandler {
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
    public void onError() {
        signal.countDown();
    }
}
