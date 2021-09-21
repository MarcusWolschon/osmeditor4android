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
    SPLIT_METRIC, SPLIT_ROUTE_ORDERING;

    @Override
    public String toTranslatedString(Context context) {
        if (SPLIT_METRIC.equals(this)) {
            return context.getString(R.string.issue_split_metric);
        } else if (SPLIT_ROUTE_ORDERING.equals(this)) {
            return context.getString(R.string.issue_split_route_ordering);
        } else {
            return "";
        }
    }

    @Override
    public boolean isError() {
        return false;
    }
}
