package de.blau.android.osm;

import android.content.Context;
import de.blau.android.R;

/**
 * Potential issues when splitting a Way
 * 
 * @author Simon
 *
 */
public enum SplitIssue implements Issue {
    SPLITMETRIC;

    @Override
    public String toTranslatedString(Context context) {
        if (SPLITMETRIC.equals(this)) {
            return context.getString(R.string.issue_split_metric);
        } else {
            return "";
        }
    }

    @Override
    public boolean isError() {
        return false;
    }
}
