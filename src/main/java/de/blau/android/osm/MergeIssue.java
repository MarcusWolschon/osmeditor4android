package de.blau.android.osm;

import android.content.Context;
import de.blau.android.R;

public enum MergeIssue implements Issue {
    ROLECONFLICT, MERGEDTAGS, NOTREVERSABLE, SAMEOBJECT;

    @Override
    public String toTranslatedString(Context context) {
        switch (this) {
        case ROLECONFLICT:
            return context.getString(R.string.issue_role_conflict);
        case MERGEDTAGS:
            return context.getString(R.string.issue_merged_tags);
        case NOTREVERSABLE:
            return context.getString(R.string.issue_not_reversable);
        case SAMEOBJECT:
            return context.getString(R.string.issue_same_object);
        default:
            return "";
        }
    }

    @Override
    public boolean isError() {
        return true;
    }
}
