package io.vespucci.osm;

import android.content.Context;
import io.vespucci.R;

/**
 * Potential issues when replacing existing geometry
 * 
 * @author Simon
 *
 */
public enum ReplaceIssue implements Issue {
    EXTRACTED_NODE, MEMBER_REPLACED;

    @Override
    public String toTranslatedString(Context context) {
        if (EXTRACTED_NODE.equals(this)) {
            return context.getString(R.string.issue_replace_extracted_node);
        } else if (MEMBER_REPLACED.equals(this)) {
            return context.getString(R.string.issue_replace_member_element_replaced);
        } else {
            return "";
        }
    }

    @Override
    public boolean isError() {
        return false;
    }
}
