package de.blau.android.osm;

import android.content.Context;
import de.blau.android.R;

public enum ReverseIssue implements Issue {
    NOT_REVERSABLE, SHARED_NODE, TAGS_REVERSED, ROLE_REVERSED, ONEWAY_DIRECTION_REVERSED, ONEWAY_REVERSING_NOT_SUPPORTED;

    @Override
    public String toTranslatedString(Context context) {
        switch (this) {
        case NOT_REVERSABLE:
            return context.getString(R.string.issue_not_reversable);
        case SHARED_NODE:
            return context.getString(R.string.issue_shared_node);
        case TAGS_REVERSED:
            return context.getString(R.string.issue_tags_reversed);
        case ROLE_REVERSED:
            return context.getString(R.string.issue_role_reversed);
        case ONEWAY_DIRECTION_REVERSED:
            return context.getString(R.string.issue_oneway_direction_reversed);
        case ONEWAY_REVERSING_NOT_SUPPORTED:
            return context.getString(R.string.issue_oneway_reversing_not_supported);
        default:
            return "";
        }
    }

    @Override
    public boolean isError() {
        return this != TAGS_REVERSED && this != ROLE_REVERSED && this != ONEWAY_DIRECTION_REVERSED;
    }
}
