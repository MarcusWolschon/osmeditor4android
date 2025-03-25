package io.vespucci.validation;

import android.content.Context;
import androidx.annotation.NonNull;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Relation;
import io.vespucci.osm.Way;

public interface Validator {
    int NOT_VALIDATED        = 0;
    int OK                   = 0x00000001;
    int AGE                  = 0x00000002;
    int FIXME                = 0x00000004;
    int MISSING_TAG          = 0x00000008;
    int HIGHWAY_NAME         = 0x00000010; // no longer used
    int HIGHWAY_ROAD         = 0x00000020;
    int NO_TYPE              = 0x00000040;
    int IMPERIAL_UNITS       = 0x00000080;
    int INVALID_OBJECT       = 0x00000100;
    int UNTAGGED             = 0x00000200;
    int UNCONNECTED_END_NODE = 0x00000400;
    int DEGENERATE_WAY       = 0x00000800;
    int EMPTY_RELATION       = 0x00001000;
    int MISSING_ROLE         = 0x00002000;
    int RELATION_LOOP        = 0x00004000;
    int WRONG_ELEMENT_TYPE   = 0x00008000;

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
     * Validate an OSM Relation
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
