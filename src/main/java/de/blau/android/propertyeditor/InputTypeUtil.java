package de.blau.android.propertyeditor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.widget.AutoCompleteTextView;
import android.widget.ListAdapter;
import android.widget.TextView;
import de.blau.android.names.Names;
import de.blau.android.presets.Preset.ValueType;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.util.StringWithDescription;

public final class InputTypeUtil {

    /**
     * Private default constructor
     */
    private InputTypeUtil() {
        // don't instantiate this class
    }

    /**
     * Set the (keyboard) behaviour from the ValueType
     * 
     * @param view the TextView to set the InputType on
     * @param valueType the ValueType from the PresetItem key
     */
    public static void setInputTypeFromValueType(@NonNull final TextView view, @Nullable final ValueType valueType) {
        if (valueType == null) {
            // do nothing
            return;
        }
        switch (valueType) {
        case INTEGER:
            view.setInputType(view.getInputType() | InputType.TYPE_CLASS_NUMBER);
            break;
        case PHONE:
            view.setInputType(view.getInputType() | InputType.TYPE_CLASS_PHONE);
            break;
        default:
            break;
        }
    }

    /**
     * Set auto-complete and auto-correct for an otherwise empty AutoCompleteTextView
     * 
     * @param view the AutoCompleteTextView
     */
    public static void enableTextSuggestions(@NonNull final AutoCompleteTextView view) {
        ListAdapter adapter = view.getAdapter();
        boolean emptyAdapter = true;
        if (adapter != null) {
            int count = adapter.getCount();
            if (count > 1) {
                return;
            } else if (count == 1) {
                // need to check
                Object o = adapter.getItem(0);
                if (o instanceof Names.NameAndTags) {
                    return;
                } else if (o instanceof ValueWithCount) {
                    emptyAdapter = "".equals(((ValueWithCount) o).getValue());
                } else if (o instanceof StringWithDescription) {
                    emptyAdapter = "".equals(((StringWithDescription) o).getValue());
                } else if (o instanceof String) {
                    emptyAdapter = "".equals((String) o);
                }
            }
        }
        if (emptyAdapter && "".equals(view.getText().toString())) {
            view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        }
    }
}
