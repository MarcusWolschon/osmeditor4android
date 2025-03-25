package io.vespucci.propertyeditor.tagform;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import io.vespucci.R;
import io.vespucci.presets.PresetLabelField;

/**
 * An editable text only row for a tag with a dropdown containg suggestions
 * 
 * @author simon
 *
 */
public class LabelRow extends LinearLayout {

    protected static final String DEBUG_TAG = LabelRow.class.getSimpleName().substring(0, Math.min(23, LabelRow.class.getSimpleName().length()));

    private TextView labelView;

    /**
     * Construct a editable text row for a tag
     * 
     * @param context Android Context
     */
    public LabelRow(Context context) {
        super(context);
    }

    /**
     * Construct a editable text row for a tag
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public LabelRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        labelView = findViewById(R.id.label);
    }

    /**
     * Set tha label
     * 
     * @param label the label
     */
    public void setLabel(@NonNull String label) {
        getLabelView().setText(label);
    }

    /**
     * @return the labelView
     */
    private TextView getLabelView() {
        return labelView;
    }

    /**
     * Get a row for an unstructured text value
     * 
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the row
     * @param field the PresetLabelField
     * @return a LabelRow instance
     */
    static LabelRow getRow(@NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout, @NonNull final PresetLabelField field) {
        final LabelRow row = (LabelRow) inflater.inflate(R.layout.tag_form_label_row, rowLayout, false);
        final String label = field.getLabel();
        row.setLabel(label);
        FormattingRow.setBackgroundColor(row.getLabelView(), field);
        return row;
    }
}
