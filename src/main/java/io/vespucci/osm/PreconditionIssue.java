package io.vespucci.osm;

import android.content.Context;
import io.vespucci.R;

/**
 * API or data model imposed conditions that could be violated
 * 
 * @author Simon
 *
 */
public enum PreconditionIssue implements Issue {
    RELATION_MEMBER_COUNT;

    @Override
    public String toTranslatedString(Context context) {
        if (RELATION_MEMBER_COUNT.equals(this)) {
            return context.getString(R.string.issue_precondition_relation_member_count);
        } else {
            return "";
        }
    }

    @Override
    public boolean isError() {
        return true; // always errors
    }
}
