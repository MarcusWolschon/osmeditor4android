package io.vespucci.propertyeditor.tagform;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.presets.FieldHeight;
import io.vespucci.presets.PresetField;
import io.vespucci.presets.PresetFormattingField;
import io.vespucci.presets.PresetItemSeparatorField;
import io.vespucci.presets.PresetSpaceField;
import io.vespucci.util.Density;
import io.vespucci.util.ThemeUtils;

/**
 * A row purely for formatting
 * 
 * @author simon
 *
 */
public class FormattingRow extends LinearLayout {
    protected static final String DEBUG_TAG = FormattingRow.class.getSimpleName().substring(0, Math.min(23, FormattingRow.class.getSimpleName().length()));

    private static final int DEFAULT_SPACE_HEIGHT     = 34;
    private static final int DEFAULT_SEPARATOR_HEIGHT = 2;

    /**
     * Construct a editable text row for a tag
     * 
     * @param context Android Context
     */
    public FormattingRow(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct a editable text row for a tag
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public FormattingRow(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Get a row for formatting purposes
     * 
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the row
     * @param field a PresetFormattingField
     * @return a FormattingRow instance
     */
    static FormattingRow getRow(@NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout, @NonNull PresetFormattingField field) {
        FormattingRow row = (FormattingRow) inflater.inflate(R.layout.tag_form_formatting_row, rowLayout, false);
        View view = row.findViewById(R.id.row);
        final Context context = rowLayout.getContext();
        if (field instanceof PresetItemSeparatorField) {
            setHeight(context, (FieldHeight) field, view, DEFAULT_SEPARATOR_HEIGHT);
            view.setBackgroundColor(ThemeUtils.getStyleAttribColorValue(context, R.attr.colorAccent, R.color.material_teal));
            setBackgroundColor(view, field);
        } else if (field instanceof PresetSpaceField) {
            setHeight(context, (FieldHeight) field, view, DEFAULT_SPACE_HEIGHT);
            setBackgroundColor(view, field);
        }
        return row;
    }

    /**
     * Set the height of the row
     * 
     * @param context Android Context
     * @param field a field implementing FieldHeight
     * @param view the current View
     * @param defaultHeight default value for the height (in dip)
     */
    private static void setHeight(@NonNull final Context context, @NonNull FieldHeight field, @NonNull View view, int defaultHeight) {
        int height = field.getHeight();
        android.view.ViewGroup.LayoutParams lp = view.getLayoutParams();
        final int dp = Density.dpToPx(context, height == 0 ? defaultHeight : height);
        lp.height = dp;
        view.setMinimumHeight(dp);
    }

    /**
     * Set the background colour of the row from the PresetField value
     * 
     * @param row the row
     * @param field the field
     */
    static void setBackgroundColor(@NonNull View row, @NonNull PresetField field) {
        int background = field.getBackgroundColour();
        if (background != 0) {
            row.setBackgroundColor(background);
        }
    }
}