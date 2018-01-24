package de.blau.android.validation;

import android.content.Context;
import android.support.annotation.NonNull;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;

public interface Validator {
    int NOT_VALIDATED  = 0;
    int OK             = 0x00000001;
    int AGE            = 0x00000002;
    int FIXME          = 0x00000004;
    int MISSING_TAG    = 0x00000008;
    int HIGHWAY_NAME   = 0x00000010;
    int HIGHWAY_ROAD   = 0x00000020;
    int NO_TYPE        = 0x00000040;
    int IMPERIAL_UNITS = 0x00000080;

    /**
     * Reset the state of the validator
     * 
     * @param context Android Context
     */
    void reset(Context context);

    /**
     * Validate an OSM Node
     * 
     * @param node Node to validate
     * @return an int with bits set according to issues found
     */
    int validate(@NonNull Node node);

    /**
     * Validate an OSM Way
     * 
     * @param way Way to validate
     * @return an int with bits set according to issues found
     */
    int validate(@NonNull Way way);

    /**
     * Validate an OSM RElation
     * 
     * @param relation Relation to validate
     * @return an int with bits set according to issues found
     */
    int validate(@NonNull Relation relation);

    /**
     * Get problem descriptions
     * 
     * Note should only be called if the element actually does have an issue
     * 
     * @param ctx Android Context
     * @param node Node to return the issues for
     * @return array of Strings containing short descriptions of any issues
     */
    @NonNull
    String[] describeProblem(@NonNull Context ctx, @NonNull Node node);

    /**
     * Get problem descriptions
     * 
     * Note should only be called if the element actually does have an issue
     * 
     * @param ctx Android Context
     * @param way Way to return the issues for
     * @return array of Strings containing short descriptions of any issues
     */
    @NonNull
    String[] describeProblem(@NonNull Context ctx, @NonNull Way way);

    /**
     * Get problem descriptions
     * 
     * Note should only be called if the element actually does have an issue
     * 
     * @param ctx Android Context
     * @param relation Relation to return the issues for
     * @return array of Strings containing short descriptions of any issues
     */
    @NonNull
    String[] describeProblem(@NonNull Context ctx, @NonNull Relation relation);

    /**
     * Get problem descriptions
     * 
     * Note should only be called if the element actually does have an issue
     * 
     * @param ctx Android Context
     * @param e OsmElement to return the issues for
     * @return array of Strings containing short descriptions of any issues
     */
    @NonNull
    String[] describeProblem(@NonNull Context ctx, @NonNull OsmElement e);
}
