package io.vespucci.propertyeditor.tagform;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.android.checkbox.IndeterminateCheckBox.OnStateChangedListener;
import io.vespucci.views.TriStateCheckBox;

/**
 * Inline CheckGroup row with tri-state checkboxes
 */
public class CheckGroupRow extends MultiselectRow {
    /**
     * Construct a row that will multiple values to be selected
     * 
     * @param context Android Context
     */
    public CheckGroupRow(Context context) {
        super(context);
    }

    /**
     * Construct a row that will multiple values to be selected
     * 
     * @param context Android Context
     * @param attrs and AttriuteSet
     */
    public CheckGroupRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Add a CheckBox to this row
     * 
     * @param description the description to display
     * @param state if true/false the CheckBox will be checked/unchecked, if null it will be set to indetermiante state,
     * @param listener a listener to call when the CheckBox is clicked
     * @return the CheckBox for further use
     */
    public TriStateCheckBox addCheck(@NonNull String description, @Nullable Boolean state, @NonNull OnStateChangedListener listener) {
        final TriStateCheckBox check = new TriStateCheckBox(context);
        check.setText(description);
        check.setState(state);
        valueLayout.addView(check);
        check.setOnStateChangedListener(listener);
        return check;
    }
}
