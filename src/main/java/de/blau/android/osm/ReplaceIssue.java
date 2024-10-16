package de.blau.android.osm;

import android.content.Context;
import de.blau.android.R;

/**
 * Potential issues when replacing existing geometry
 * 
 * @author Simon
 *
 */
public enum ReplaceIssue implements Issue {
    EXTRACTED_NODE;

    @Override
    public String toTranslatedString(Context context) {
        if (EXTRACTED_NODE.equals(this)) {
            return context.getString(R.string.issue_replace_extracted_node);
        } else {
            return "";
        }
    }

    @Override
    public boolean isError() {
        return false;
    }
}
