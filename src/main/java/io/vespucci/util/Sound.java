package io.vespucci.util;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import io.vespucci.App;
import io.vespucci.prefs.Preferences;

public final class Sound {
    private static final String DEBUG_TAG = Sound.class.getSimpleName().substring(0, Math.min(23, Sound.class.getSimpleName().length()));

    private static final int BEEP_DURATION       = 200;
    public static final int  BEEP_DEFAULT_VOLUME = 50;

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
        Preferences prefs = App.getLogic().getPrefs();
        int volume = BEEP_DEFAULT_VOLUME;
        if (prefs != null) {
            volume = prefs.getBeepVolume();
        }
        try {
            ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, volume);
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, BEEP_DURATION);
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (toneGenerator != null) {
                    toneGenerator.release();
                }
            }, BEEP_DURATION + 50L);
        } catch (RuntimeException rex) { // NOSONAR
            Log.e(DEBUG_TAG, "beep failed with " + rex.getMessage());
        }
    }
}
