package de.blau.android;

import java.util.concurrent.CountDownLatch;

public class SignalHandler extends PostAsyncActionHandler {
    final CountDownLatch signal;

    public SignalHandler(CountDownLatch signal) {
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
