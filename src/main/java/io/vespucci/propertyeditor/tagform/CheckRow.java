package io.vespucci.propertyeditor.tagform;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import ch.poole.android.checkbox.IndeterminateCheckBox;
import io.vespucci.R;
import io.vespucci.views.TriStateCheckBox;

public class CheckRow extends LinearLayout {

    private TextView              keyView;
    private IndeterminateCheckBox valueCheck;

    /**
     * Construct a row with a single CheckBox
     * 
     * @param context Android Context
     */
    public CheckRow(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct a row with a single CheckBox
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public CheckRow(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (isInEditMode()) {
            return; // allow visual editor to work
        }
        setKeyView((TextView) findViewById(R.id.textKey));
        valueCheck = (TriStateCheckBox) findViewById(R.id.valueSelected);
    }

    /**
     * Return the OSM key value
     * 
     * @return the key as a String
     */
    public String getKey() {
        return (String) getKeyView().getTag();
    }

    /**
     * Get the CheckBox
     * 
     * @return return the CheckBox associated with this row
     */
    public IndeterminateCheckBox getCheckBox() {
        return valueCheck;
    }

    /**
     * Check if the CheckBox for this row is checked
     * 
     * @return true if the CHeckBox is checked
     */
    public boolean isChecked() {
        return valueCheck.isChecked();
    }

    /**
     * @return the keyView
     */
    public TextView getKeyView() {
        return keyView;
    }

    /**
     * @param keyView the keyView to set
     */
    public void setKeyView(TextView keyView) {
        this.keyView = keyView;
    }
}
