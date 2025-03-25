package io.vespucci.util;

import org.acra.ACRA;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.App;
import io.vespucci.osm.Storage;
import io.vespucci.osm.StorageDelegator;

/**
 * 
 * A couple of helper methods for ACRA
 * 
 * @author Simon Poole
 *
 */
public final class ACRAHelper {

    /**
     * Private constructor to stop instantiation
     */
    private ACRAHelper() {
        // private
    }

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
        Storage currentStorage = delegator.getCurrentStorage();
        builder.append("Relations (current/API): " + currentStorage.getRelations().size() + "/" + delegator.getApiRelationCount() + eol);
        builder.append("Ways (current/API): " + currentStorage.getWays().size() + "/" + delegator.getApiWayCount() + eol);
        builder.append("Nodes (current/Waynodes/API): " + currentStorage.getNodes().size() + "/" + currentStorage.getWayNodes().size() + "/"
                + delegator.getApiNodeCount() + eol);
    }
}
