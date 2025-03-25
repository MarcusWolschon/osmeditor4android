package io.vespucci.util;

import java.util.concurrent.ExecutorService;

import android.os.Handler;
import androidx.annotation.NonNull;

public abstract class PlaybackTask<I, P, O> extends ExecutorTask<I, P, O> {
    /**
     * Create a new instance
     * 
     * @param executorService the ExecutorService to use
     * @param handler the Handler to use
     */
    protected PlaybackTask(@NonNull ExecutorService executorService, @NonNull Handler handler) {
        super(executorService, handler);
    }

    /**
     * Pause playback
     */
    public abstract void pause();

    /**
     * Resume playback
     */
    public abstract void resume();

    /**
     * Check if playback is paused
     * 
     * @return true if playback is paused
     */
    public abstract boolean isPaused();
}
