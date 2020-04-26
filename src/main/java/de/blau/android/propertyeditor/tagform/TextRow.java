package de.blau.android.propertyeditor.tagform;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Build;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.Tags;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.ValueType;
import de.blau.android.presets.PresetComboField;
import de.blau.android.presets.PresetField;
import de.blau.android.presets.PresetTextField;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.propertyeditor.InputTypeUtil;
import de.blau.android.propertyeditor.SanitizeTextWatcher;
import de.blau.android.propertyeditor.TagEditorFragment;
import de.blau.android.propertyeditor.tagform.TagFormFragment.EditableLayout;
import de.blau.android.util.StringWithDescription;
import de.blau.android.views.CustomAutoCompleteTextView;

/**
 * An editable text only row for a tag with a dropdown containg suggestions
 * 
 * @author simon
 *
 */
public class TextRow extends LinearLayout implements KeyValueRow {

    protected static final String DEBUG_TAG = "TextRow";

    private TextView                   keyView;
    private CustomAutoCompleteTextView valueView;
    private ValueType                  valueType;

    /**
     * Construct a editable text row for a tag
     * 
     * @param context Android Context
     */
    public TextRow(Context context) {
        super(context);
    }

    /**
     * Construct a editable text row for a tag
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public TextRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (isInEditMode()) {
            return; // allow visual editor to work
        }
        setKeyView((TextView) findViewById(R.id.textKey));
        valueView = (CustomAutoCompleteTextView) findViewById(R.id.textValue);
    }

    /**
     * Set the text via id of the key view
     * 
     * @param k a string resource id for the key
     */
    public void setKeyText(int k) {
        getKeyView().setText(k);
    }

    /**
     * Set the ArrayAdapter for values
     * 
     * @param a the ArrayAdapter
     */
    public void setValueAdapter(ArrayAdapter<?> a) {
        valueView.setAdapter(a);
    }

    @Override
    public String getKey() {
        return (String) getKeyView().getTag();
    }

    @Override
    public String getValue() {
        return valueView.getText().toString();
    }

    /**
     * Get the current ValueType if any
     * 
     * @return the ValueType or null
     */
    @Nullable
    ValueType getValueType() {
        return valueType;
    }

    /**
     * Get the AutoCompleteTextView for the values
     * 
     * @return a CustomAutoCompleteTextView
     */
    public CustomAutoCompleteTextView getValueView() {
        return valueView;
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

    /**
     * Get a row for an unstructured text value
     * 
     * @param caller the calling TagFormFragment instance
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the row
     * @param preset the best matched PresetITem for the key
     * @param field the PresetField
     * @param value any existing value for the tag
     * @param values a list of all the predefined values in the PresetItem for the key
     * @param allTags a Map of the tags currently being edited
     * @return a TagTextRow instance
     */
    static TextRow getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout,
            @Nullable final PresetItem preset, @NonNull final PresetField field, @Nullable final String value, @Nullable final List<String> values,
            final Map<String, String> allTags) {
        final TextRow row = (TextRow) inflater.inflate(R.layout.tag_form_text_row, rowLayout, false);
        final String key = field.getKey();
        final String hint = preset != null ? field.getHint() : null;
        final String defaultValue = field.getDefaultValue();
        row.valueType = preset != null ? preset.getValueType(key) : null;
        final boolean isMPHSpeed = Tags.isSpeedKey(key) && App.getGeoContext(rowLayout.getContext()).imperial(caller.propertyEditorListener.getElement());
        final TextView ourKeyView = row.getKeyView();
        ourKeyView.setText(hint != null ? hint : key);
        ourKeyView.setTag(key);
        final CustomAutoCompleteTextView ourValueView = row.getValueView();

        if (field instanceof PresetTextField) {
            final int length = ((PresetTextField) field).length();
            if (length > 0) { // if it isn't set don't bother
                ourValueView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public void onGlobalLayout() {
                        ViewTreeObserver observer = ourValueView.getViewTreeObserver();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            observer.removeOnGlobalLayoutListener(this);
                        } else {
                            observer.removeGlobalOnLayoutListener(this); // NOSONAR
                        }
                        float aM = ourValueView.getPaint().measureText("M"); // FIXME cache this
                        int lines = Math.min((int) (length / aM), 4);
                        if (lines > 1) {
                            ourValueView.setLines(lines);
                            ourValueView.setMaxLines(lines);
                            ourValueView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) ourValueView.getLayoutParams();
                            layoutParams.height = LayoutParams.WRAP_CONTENT;
                            ourValueView.setLayoutParams(layoutParams);
                            ourValueView.setGravity(Gravity.TOP);
                            layoutParams = (LinearLayout.LayoutParams) ourKeyView.getLayoutParams();
                            layoutParams.height = LayoutParams.MATCH_PARENT;
                            ourKeyView.setLayoutParams(layoutParams);
                            ourKeyView.setGravity(Gravity.TOP);
                            ourValueView.requestLayout();
                        }
                    }
                });
            }
        }
        ourValueView.setText(value);

        // set empty value to be on the safe side
        ourValueView.setAdapter(new ArrayAdapter<>(caller.getActivity(), R.layout.autocomplete_row, new String[0]));

        if (field instanceof PresetComboField && ((PresetComboField) field).isMultiSelect() && preset != null) {
            // FIXME this should be somewhere better since it creates a non obvious side effect
            ourValueView.setTokenizer(new CustomAutoCompleteTextView.SingleCharTokenizer(preset.getDelimiter(key)));
        }
        if (field instanceof PresetTextField) {
            ourValueView.setHint(R.string.tag_value_hint);
        } else {
            ourValueView.setHint(R.string.tag_autocomplete_value_hint);
        }
        ourValueView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(DEBUG_TAG, "onFocusChange");
                String rowValue = row.getValue();
                if (!hasFocus && !rowValue.equals(value)) {
                    caller.tagListener.updateSingleValue(key, rowValue);
                    if (rowLayout instanceof EditableLayout) {
                        ((EditableLayout) rowLayout).putTag(key, rowValue);
                    }
                } else if (hasFocus) {
                    ArrayAdapter<?> adapter = caller.getValueAutocompleteAdapter(key, values, preset, null, allTags);
                    if (adapter != null && !adapter.isEmpty()) {
                        ourValueView.setAdapter(adapter);
                    }
                    if (isMPHSpeed) {
                        TagEditorFragment.initMPHSpeed(rowLayout.getContext(), ourValueView, caller.propertyEditorListener);
                    } else if (row.getValueType() == null) {
                        InputTypeUtil.enableTextSuggestions(ourValueView);
                    }
                    InputTypeUtil.setInputTypeFromValueType(ourValueView, row.getValueType());
                }
            }
        });
        ourValueView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(DEBUG_TAG, "onItemClicked value");
                Object o = parent.getItemAtPosition(position);
                if (o instanceof Names.NameAndTags) {
                    ourValueView.setOrReplaceText(((NameAndTags) o).getName());
                    caller.tagListener.applyTagSuggestions(((NameAndTags) o).getTags(), new Runnable() {
                        @Override
                        public void run() {
                            caller.update();
                        }

                    });
                    caller.update();
                    return;
                } else if (o instanceof ValueWithCount) {
                    ourValueView.setOrReplaceText(((ValueWithCount) o).getValue());
                } else if (o instanceof StringWithDescription) {
                    ourValueView.setOrReplaceText(((StringWithDescription) o).getValue());
                } else if (o instanceof String) {
                    ourValueView.setOrReplaceText((String) o);
                }
                caller.tagListener.updateSingleValue(key, row.getValue());
                if (rowLayout instanceof EditableLayout) {
                    ((EditableLayout) rowLayout).putTag(key, row.getValue());
                }
            }
        });
        ourValueView.addTextChangedListener(new SanitizeTextWatcher(caller.getActivity(), caller.maxStringLength));
        return row;
    }
}
