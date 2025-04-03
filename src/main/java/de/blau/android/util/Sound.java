package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.concurrent.locks.ReentrantLock;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.prefs.Preferences;

public final class Sound {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Sound.class.getSimpleName().length());
    private static final String DEBUG_TAG = Sound.class.getSimpleName().substring(0, TAG_LEN);

    private static final int BEEP_DURATION       = 200;
    public static final int  BEEP_DEFAULT_VOLUME = 50;

    private static final ReentrantLock lock = new ReentrantLock();

    /**
     * Private constructor to prevent instantiation
     */
    private Sound() {
        // nothing
    }

    /**
     * Beep
     */
    public static void beep() {
        new ExecutorTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void v) {
                try {
                    if (!lock.tryLock()) {
                        Log.w(DEBUG_TAG, "beep locked");
                        return null;
                    }
                    Preferences prefs = App.getLogic().getPrefs();
                    int volume = BEEP_DEFAULT_VOLUME;
                    if (prefs != null) {
                        volume = prefs.getBeepVolume();
                        if (volume == 0) {
                            return null; // turned off
                        }
                    }
                    try {
                        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, volume);
                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, BEEP_DURATION);
                        toneGenerator.release();
                    } catch (Exception ex) { // NOSONAR
                        Log.e(DEBUG_TAG, "beep failed with " + ex.getMessage());
                    }
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
                return null;
            }
        }.execute();
    }
}
