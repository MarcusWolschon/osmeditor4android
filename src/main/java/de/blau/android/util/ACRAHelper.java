package de.blau.android.util;

import org.acra.ACRA;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.osm.StorageDelegator;

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
        StringBuilder builder = new StringBuilder();
        addElementCounts(builder, "<BR>");
        ACRA.getErrorReporter().putCustomData("DEBUGINFO", builder.toString());
        ACRA.getErrorReporter().putCustomData("CAUSE", cause);
        ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
        ACRA.getErrorReporter().handleException(ex);
    }

    /**
     * Add current element counts to a StringBuilder
     * 
     * @param builder the StringBuilder
     * @param eol the EOL to use
     */
    public static void addElementCounts(@NonNull StringBuilder builder, @NonNull String eol) {
        StorageDelegator delegator = App.getDelegator();
        if (delegator != null) {
            builder.append("Relations (current/API): " + delegator.getCurrentStorage().getRelations().size() + "/" + delegator.getApiRelationCount() + eol);
            builder.append("Ways (current/API): " + delegator.getCurrentStorage().getWays().size() + "/" + delegator.getApiWayCount() + eol);
            builder.append("Nodes (current/Waynodes/API): " + delegator.getCurrentStorage().getNodes().size() + "/"
                    + delegator.getCurrentStorage().getWayNodes().size() + "/" + delegator.getApiNodeCount() + eol);
        }
    }
}
