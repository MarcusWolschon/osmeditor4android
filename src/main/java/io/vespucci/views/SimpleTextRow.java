package io.vespucci.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.osm.Server;
import io.vespucci.presets.PresetItem;
import io.vespucci.presets.PresetTagField;
import io.vespucci.presets.PresetTextField;
import io.vespucci.propertyeditor.InputTypeUtil;
import io.vespucci.propertyeditor.SanitizeTextWatcher;
import io.vespucci.propertyeditor.tagform.LayoutListener;
import io.vespucci.propertyeditor.tagform.TextRow;
import io.vespucci.propertyeditor.tagform.TagFormFragment.EditableLayout;

/**
 * An editable text only row for a tag with a dropdown containg suggestions
 * 
 * @author simon
 *
 */
public class SimpleTextRow extends TextRow {

    protected static final String DEBUG_TAG = SimpleTextRow.class.getSimpleName().substring(0, Math.min(23, SimpleTextRow.class.getSimpleName().length()));

    /**
     * Construct a editable text row for a tag
     * 
     * @param context Android Context
     */
    public SimpleTextRow(Context context) {
        super(context);
    }

    /**
     * Construct a editable text row for a tag
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public SimpleTextRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Get a row for an unstructured text value for use outside of the property editor
     * 
     * @param context the calling TagFormFragment instance
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the row
     * @param preset the best matched PresetItem for the key
     * @param field the PresetField
     * @param value any existing value for the tag
     * @param adapter an optional ArrayAdapter with autocomplete values
     * @return a TagTextRow instance
     */
    public static TextRow getRow(@NonNull final Context context, @NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout,
            @Nullable final PresetItem preset, @NonNull final PresetTagField field, @Nullable final String value, @Nullable final ArrayAdapter<?> adapter) {
        final TextRow row = (TextRow) inflater.inflate(R.layout.simple_text_row, rowLayout, false);
        final String key = field.getKey();
        final String hint = preset != null ? field.getHint() : null;
        row.setValueType(preset != null ? preset.getValueType(key) : null);
        final TextView ourKeyView = row.getKeyView();
        ourKeyView.setText(hint != null ? hint : key);
        ourKeyView.setTag(key);
        final CustomAutoCompleteTextView ourValueView = row.getValueView();

        if (field instanceof PresetTextField) {
            final int length = ((PresetTextField) field).length();
            if (length > 0) { // if it isn't set don't bother
                ourValueView.getViewTreeObserver().addOnGlobalLayoutListener(new LayoutListener(ourKeyView, ourValueView, length));
            }
        }
        ourValueView.setText(value);

        // set empty value to be on the safe side
        ourValueView.setAdapter(new ArrayAdapter<>(context, R.layout.autocomplete_row, new String[0]));

        setHint(field, ourValueView);
        ourValueView.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(DEBUG_TAG, "onFocusChange");
            String rowValue = row.getValue();
            if (!hasFocus && !rowValue.equals(value)) {
                if (rowLayout instanceof EditableLayout) {
                    ((EditableLayout) rowLayout).putTag(key, rowValue);
                }
            } else if (hasFocus) {
                setAdapter(ourValueView, adapter);
                if (row.getValueType() == null) {
                    InputTypeUtil.enableTextSuggestions(ourValueView);
                }
                InputTypeUtil.setInputTypeFromValueType(ourValueView, row.getValueType());
            }
        });
        ourValueView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(DEBUG_TAG, "onItemClicked value");
            setOrReplaceText(ourValueView, parent.getItemAtPosition(position));
        });
        Server server = App.getPreferences(context).getServer();
        ourValueView.addTextChangedListener(new SanitizeTextWatcher(context, server.getCachedCapabilities().getMaxStringLength()));
        return row;
    }
}
