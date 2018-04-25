package de.blau.android.util;

import org.acra.ACRA;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * 
 * A couple of helper methods for ACRA
 * 
 * @author Simon Poole
 *
 */
public class ACRAHelper {

    /**
     * Submit a crash report indicating that we didn't actually crash
     * 
     * @param ex the Exception leading to this
     * @param cause a cause
     */
    public static void nocrashReport(@Nullable Throwable ex, @NonNull String cause) {
        ACRA.getErrorReporter().putCustomData("CAUSE", cause);
        ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
        ACRA.getErrorReporter().handleException(ex);
    }
}
