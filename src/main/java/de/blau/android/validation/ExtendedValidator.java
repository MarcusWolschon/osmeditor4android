package de.blau.android.validation;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import android.content.Context;
import androidx.annotation.NonNull;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetRole;
import de.blau.android.util.collections.LongHashSet;
import de.blau.android.util.collections.LongOsmElementMap;

/**
 * A Validator that adds validation we only want to do prior to uploads and manually
 * 
 * This delegates most work to an existing BaseValidator instance that needs to be provided
 * 
 * @author simon
 *
 */
public class ExtendedValidator implements Validator {

    private final BaseValidator base;
    private final Logic         logic;

    /**
     * Cache of way nodes we've already found
     * 
     * As finding the ways a node belongs to is very expensive, and say for a newly created building would be run for
     * each new node, we cache all the way nodes we've found
     */
    private final LongHashSet wayNodes = new LongHashSet();

    private boolean missingRoleValidation;
    private boolean relationLoopValidation;

    /**
     * Create a new UploadValidator
     * 
     * @param ctx an Android Context
     * @param base the BaseValidator to wrap
     */
    public ExtendedValidator(@NonNull Context ctx, @NonNull Validator base) {
        if (!(base instanceof BaseValidator)) {
            throw new IllegalArgumentException("Argument must be a BaseValidator");
        }
        this.base = (BaseValidator) base;
        logic = App.getLogic();
        init(ctx);
    }

    /**
     * Initialize the Validator
     * 
     * @param context an Android Context
     */
    public void init(@NonNull Context context) {
        missingRoleValidation = base.enabledValidations.contains(context.getString(R.string.VALIDATION_MISSING_ROLE));
        relationLoopValidation = base.enabledValidations.contains(context.getString(R.string.VALIDATION_RELATION_LOOP));
    }

    @Override
    public void reset(Context context) {
        base.reset(context);
        init(context);
    }

    @Override
    public int validate(Node node) {
        int result = base.validate(node);
        synchronized (wayNodes) {
            if (base.untaggedValidation && !wayNodes.contains(node.getOsmId()) && !node.hasTags() && !node.hasParentRelations()) {
                List<Way> ways = logic.getWaysForNode(node);
                if (ways.isEmpty()) {
                    return addResult(result, Validator.UNTAGGED);
                }
                for (Way w : ways) {
                    for (Node n : w.getNodes()) {
                        wayNodes.put(n.getOsmId());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public int validate(Way way) {
        return base.validate(way);
    }

    @Override
    public int validate(Relation relation) {
        int result = base.validate(relation);
        List<RelationMember> members = relation.getMembers();
        if (missingRoleValidation && members != null) {
            result = missingRoles(relation, members, result);
        }
        // loop check
        if (relationLoopValidation) {
            LongOsmElementMap<Relation> map = new LongOsmElementMap<>();
            map.put(relation.getOsmId(), relation);
            if (hasLoop(members, map)) {
                result = addResult(result, Validator.RELATION_LOOP);
            }
        }
        return result;
    }

    /**
     * Check for missing mandatory roles
     * 
     * @param relation the Relation to check
     * @param members the relation members
     * @param result the result prior to the check
     * @return the result
     */
    private int missingRoles(@NonNull Relation relation, @NonNull List<RelationMember> members, int result) {
        // check for missing roles
        PresetItem pi = Preset.findBestMatch(base.getPresets(), relation.getTags(), base.getIsoCodes(relation), ElementType.RELATION, false, null);
        if (pi != null) {
            List<PresetRole> presetRoles = pi.getRoles();
            if (presetRoles != null) {
                List<String> roles = new ArrayList<>();
                for (PresetRole role : presetRoles) {
                    roles.add(role.getRole());
                }
                for (RelationMember member : members) {
                    if (!roles.contains(member.getRole())) {
                        result = addResult(result, Validator.MISSING_ROLE);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Add an intermediate result
     * 
     * @param result the current result
     * @param intermediate the intermediate result
     * @return the new result
     */
    private int addResult(int result, int intermediate) {
        if (result == Validator.OK) {
            result = intermediate;
        } else {
            result |= intermediate;
        }
        return result;
    }

    /**
     * Recursively try to determine if the relation has a loop
     * 
     * @param members the current relation members
     * @param map a Map for bookkeeping
     * @return true if the relation has a loop
     */
    private boolean hasLoop(@Nullable List<RelationMember> members, @NonNull LongOsmElementMap<Relation> map) {
        if (members != null) {
            for (RelationMember member : members) {
                if (member.downloaded() && Relation.NAME.equals(member.getType())) {
                    if (map.containsKey(member.getRef())) {
                        return true;
                    }
                    Relation relation = (Relation) member.getElement();
                    map.put(member.getRef(), relation);
                    return hasLoop(relation.getMembers(), map);
                }
            }
        }
        return false;
    }

    @Override
    public String[] describeProblem(Context ctx, Node node) {
        return base.describeProblem(ctx, node);
    }

    @Override
    public String[] describeProblem(Context ctx, Way way) {
        return base.describeProblem(ctx, way);
    }

    @Override
    public String[] describeProblem(Context ctx, Relation relation) {
        return base.describeProblem(ctx, relation);
    }

    @Override
    public String[] describeProblem(Context ctx, OsmElement e) {
        if (e instanceof Node) {
            return describeProblem(ctx, (Node) e);
        }
        if (e instanceof Way) {
            return describeProblem(ctx, (Way) e);
        }
        if (e instanceof Relation) {
            return describeProblem(ctx, (Relation) e);
        }
        return new String[] {};
    }
}
