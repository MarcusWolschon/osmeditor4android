package de.blau.android.osm;

import android.content.Context;
import de.blau.android.R;

public enum ReverseIssue implements Issue {
    NOTREVERSABLE, SHAREDNODE, TAGSREVERSED, ROLEREVERSED, ONEWAYDIRECTIONREVERSED;

    @Override
    public String toTranslatedString(Context context) {
        switch (this) {
        case NOTREVERSABLE:
            return context.getString(R.string.issue_not_reversable);
        case SHAREDNODE:
            return context.getString(R.string.issue_shared_node);
        case TAGSREVERSED:
            return context.getString(R.string.issue_tags_reversed);
        case ROLEREVERSED:
            return context.getString(R.string.issue_role_reversed);
        case ONEWAYDIRECTIONREVERSED:
            return context.getString(R.string.issue_oneway_direction_reversed);
        default:
            return "";
        }
    }

    @Override
    public boolean isError() {
        return this != TAGSREVERSED && this != ROLEREVERSED && this != ONEWAYDIRECTIONREVERSED;
    }
}
