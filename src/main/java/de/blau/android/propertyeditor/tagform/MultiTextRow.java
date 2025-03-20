package de.blau.android.propertyeditor.tagform;

import java.util.List;

import com.google.android.material.snackbar.BaseTransientBottomBar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetComboField;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetTagField;
import de.blau.android.presets.ValueType;
import de.blau.android.propertyeditor.InputTypeUtil;
import de.blau.android.propertyeditor.TagChanged;
import de.blau.android.propertyeditor.tagform.TagFormFragment.EditableLayout;
import de.blau.android.util.GeoContext;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.views.CustomAutoCompleteTextView;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil.PhoneNumberFormat;
import io.michaelrocks.libphonenumber.android.Phonenumber.PhoneNumber;

/**
 * A view that supports multiple value tags that can be freely edited
 * 
 * Has formating support of phone numbers.
 * 
 * @author simon
 *
 */
public class MultiTextRow extends LinearLayout implements KeyValueRow, TagChanged {

    protected static final String DEBUG_TAG = MultiTextRow.class.getSimpleName().substring(0, Math.min(23, MultiTextRow.class.getSimpleName().length()));

    /**
     * Inline value display with multiple editable text fields
     */

    private TextView              keyView;
    protected LinearLayout        valueLayout;
    protected final Context       context;
    private Preferences           prefs;
    private char                  delimiter  = ';';
    private String                country;
    private LayoutInflater        inflater;
    private ArrayAdapter<?>       adapter;
    private ValueType             valueType;
    private OnFocusChangeListener listener;
    private String                valueCountKey;
    private int                   valueCount = 0;

    private List<String> values;

    private TagFormFragment caller;

    /**
     * Construct a row that will multiple values to be selected
     * 
     * @param context Android Context
     */
    public MultiTextRow(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    /**
     * Construct a row that will multiple values to be selected
     * 
     * @param context Android Context
     * @param attrs and AttriuteSet
     */
    public MultiTextRow(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (isInEditMode()) {
            return; // allow visual editor to work
        }
        keyView = (TextView) findViewById(R.id.textKey);
        valueLayout = (LinearLayout) findViewById(R.id.valueGroup);
    }

    @Override
    public String getKey() {
        return (String) keyView.getTag();
    }

    /**
     * Get the Layout containing the CheckBoxes for the values
     * 
     * @return a LinearLayout
     */
    public LinearLayout getValueGroup() {
        return valueLayout;
    }

    /**
     * Return all non-empty values concatenated with the required delimiter
     * 
     * @return a String containing an OSM style list of values
     */
    @Override
    public String getValue() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < valueLayout.getChildCount(); i++) {
            EditText editText = (EditText) valueLayout.getChildAt(i);
            String text = editText.getText().toString();
            boolean notEmpty = text != null && !"".equals(text);
            if ((valueCount > 0 && i > 0) || (result.length() > 0 && notEmpty)) {
                result.append(delimiter);
            }
            if (notEmpty) {
                result.append(text);
            }
        }
        return result.toString();
    }

    class MyTextWatcher implements TextWatcher {
        private boolean               wasEmpty;
        private EditText              editText  = null;
        private OnFocusChangeListener listener  = null;
        private ValueType             valueType = null;
        private ArrayAdapter<?>       adapter;

        /**
         * Construct a new TextWatcher
         * 
         * Will try to support formating phone numbers correctly
         * 
         * @param editText the EditText that we are watching
         * @param listener an OnFocusChangeListener for updating the other Fragments
         * @param valueType the ValueType of the tag
         * @param adapter an optional adapter for values
         */
        MyTextWatcher(@NonNull final EditText editText, @NonNull final View.OnFocusChangeListener listener, final @Nullable ValueType valueType,
                @Nullable final ArrayAdapter<?> adapter) {
            this.editText = editText;
            this.listener = listener;
            this.valueType = valueType;
            this.adapter = adapter;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // nop
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            wasEmpty = s.length() == 0;
        }

        private Runnable splitText = () -> {
            editText.removeTextChangedListener(MyTextWatcher.this);
            ViewParent parent = editText.getParent();
            if (parent instanceof ViewGroup) {
                int pos = ((ViewGroup) parent).indexOfChild(editText);
                List<String> bits = Preset.splitValues(Util.wrapInList(editText.getText().toString()), MultiTextRow.this.delimiter);
                int size = bits.size();
                if (size > 0) {
                    View last = editText;
                    editText.setText(bits.get(0));
                    for (int i = 1; i < size; i++) {
                        last = addEditText(bits.get(i), listener, valueType, adapter, pos + i);
                    }
                    if (size == 1) { // delimiter must have been at the end
                        last = addEditText("", listener, valueType, adapter, pos + 1);
                    }
                    if (valueCount > 0) {
                        for (int i = size; i < valueCount; i++) {
                            last = addEditText("", listener, valueType, adapter, -1);
                        }
                    }
                    last.requestFocus();
                } else {
                    editText.setText("");
                }
            } else {
                Log.e(DEBUG_TAG, "Parent is not a ViewGroup");
            }
            editText.addTextChangedListener(MyTextWatcher.this);
        };

        private Runnable formatPhoneNumber = () -> {
            editText.removeTextChangedListener(MyTextWatcher.this);
            if (editText.getSelectionStart() == editText.length()) {
                editText.setText(formatPhoneNumber(editText.getText().toString()));
                editText.setSelection(editText.length());
            }
            editText.addTextChangedListener(MyTextWatcher.this);
        };

        @Override
        public void afterTextChanged(Editable s) {
            int length = s.length();
            int index = valueLayout.indexOfChild(editText);
            int count = valueLayout.getChildCount();
            if (valueCount <= 0 && wasEmpty == (length > 0) && (index == count - 1)) {
                addEditText("", listener, valueType, adapter, -1);
            }
            // format and split text if necessary
            if (valueType != null) {
                switch (valueType) {
                case PHONE:
                    editText.removeCallbacks(formatPhoneNumber);
                    editText.postDelayed(formatPhoneNumber, 100);
                    break;
                default:
                    // do nothing
                }
            }
            // split the text if the delimiter is entered

            int delPos = s.toString().indexOf(MultiTextRow.this.delimiter);
            if (delPos >= 0) {
                // on insertion of certain special characters, afterTextChange will be
                // called multiple times with different arguments, this tries to work
                // around the issue
                editText.removeCallbacks(splitText);
                editText.postDelayed(splitText, 50);
            }
        }
    }

    /**
     * Add an TextBox to this row
     * 
     * @param value the value to display
     * @param listener called when focus for the view has changed
     * @param valueType the Preset ValueType for the key
     * @param adapter an optional adapter for values
     * @param position the position where to insert the view or -1 for at the end
     * @return the EditText for further use
     */
    private CustomAutoCompleteTextView addEditText(final @NonNull String value, @NonNull final View.OnFocusChangeListener listener,
            @Nullable final ValueType valueType, @Nullable final ArrayAdapter<?> adapter, int position) {

        CustomAutoCompleteTextView editText = (CustomAutoCompleteTextView) inflater.inflate(R.layout.form_dialog_multitext_value, valueLayout, false);

        final TextWatcher textWatcher = new MyTextWatcher(editText, listener, valueType, adapter);
        Log.e(DEBUG_TAG, "addEditText " + value + " pos " + position);
        editText.setText(value);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        InputTypeUtil.setInputTypeFromValueType(editText, valueType);
        editText.setHint(R.string.tag_value_hint);
        editText.setOnFocusChangeListener(listener);
        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable icon = editText.getCompoundDrawables()[DRAWABLE_RIGHT];
                int[] screenPos = new int[2];
                editText.getLocationOnScreen(screenPos);
                if (icon != null && event.getRawX() >= (screenPos[0] + editText.getRight() - icon.getBounds().width())) {
                    int index = valueLayout.indexOfChild(editText);
                    int count = valueLayout.getChildCount();
                    if (valueCount == 0) {
                        removeValue(count > 1 && index != (count - 1), valueLayout, editText, textWatcher);
                    } else {
                        removeValue(count > valueCount, valueLayout, editText, textWatcher);
                    }
                    listener.onFocusChange(editText, false);
                    return true;
                }
            }
            return false;
        });
        editText.setOnItemClickListener((parent, view, pos, id) -> {
            Log.d(DEBUG_TAG, "onItemClicked value");
            Object o = parent.getItemAtPosition(pos);
            if (o instanceof StringWithDescription) {
                editText.setText(((StringWithDescription) o).getValue());
            } else if (o instanceof String) {
                editText.setText((String) o);
            }
            listener.onFocusChange(editText, false);
        });
        editText.addTextChangedListener(textWatcher);
        if (adapter != null) {
            editText.setAdapter(adapter);
        }
        editText.setOnKeyListener(new MyKeyListener());
        if (position == -1) {
            valueLayout.addView(editText);
        } else {
            valueLayout.addView(editText, position);
        }
        return editText;
    }

    /**
     * Remove the EditText for a value
     * 
     * @param condition if true completely remove the value EditText, else just remove the content
     * @param layout the layout holding the EditText
     * @param editText the EditTExt
     * @param textWatcher the TextWatcher
     */
    void removeValue(boolean condition, @NonNull LinearLayout layout, @NonNull EditText editText, @NonNull TextWatcher textWatcher) {
        if (condition) {
            valueLayout.removeView(editText);
        } else { // don't delete last one
            editText.removeTextChangedListener(textWatcher);
            editText.setText("");
            editText.addTextChangedListener(textWatcher);
        }
    }

    /**
     * Get the TextView for the key
     * 
     * @return the TextView for the key
     */
    public TextView getKeyView() {
        return keyView;
    }

    /**
     * Set the regions and the country for this object
     * 
     * @param regions a list of iso codes
     */
    private void setRegions(@Nullable List<String> regions) {
        country = GeoContext.getCountryIsoCode(regions);
    }

    /**
     * Format a phone number
     * 
     * @param s input String
     * @return a formated String or s unchanged
     */
    @NonNull
    private String formatPhoneNumber(@NonNull String s) {
        if (prefs.autoformatPhoneNumbers()) {
            PhoneNumberUtil phone = App.getPhoneNumberUtil(getContext());
            if (phone != null && country != null) {
                try {
                    PhoneNumber number = phone.parse(s, country);
                    s = phone.format(number, PhoneNumberFormat.INTERNATIONAL);
                } catch (NumberParseException e) {
                    // NOSONAR ignore
                }
            }
        }
        return s;
    }

    /**
     * Add a row for a multi-select with inline AutoCompleteTextViews
     * 
     * @param caller the calling TagFormFragment
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the row
     * @param preset the best matched PresetITem for the key
     * @param hint a textual description of what the key is
     * @param key the key
     * @param values existing values for the tag
     * @param delimiter non-standard value delimiter (default is ;)
     * @param valueCountKey the key holding the value count or null
     * @param valueCountValue the value for valueCountKey or null
     * @param adapter an optional adapter for values
     * @return a TagMultiselectRow instance
     */
    static MultiTextRow getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout,
            @NonNull final PresetItem preset, @Nullable final String hint, final String key, @Nullable final List<String> values, @Nullable String delimiter,
            @Nullable String valueCountKey, @Nullable String valueCountValue, @Nullable final ArrayAdapter<?> adapter) {
        final MultiTextRow row = (MultiTextRow) inflater.inflate(R.layout.tag_form_multitext_row, rowLayout, false);
        row.inflater = inflater;
        row.adapter = adapter;
        row.prefs = caller.prefs;
        row.values = values;
        row.caller = caller;
        PresetTagField field = preset.getField(key);
        if (field instanceof PresetComboField) {
            row.delimiter = preset.getDelimiter(key);
        }
        if (delimiter != null && delimiter.length() > 0) {
            row.delimiter = delimiter.charAt(0);
        }
        List<String> regions = caller.propertyEditorListener.getIsoCodes();
        row.setRegions(regions);
        row.getKeyView().setText(hint != null ? hint : key);
        row.getKeyView().setTag(key);
        String value = values != null && !values.isEmpty() ? values.get(0) : null;
        List<String> splitValues = Preset.splitValues(values, row.delimiter);
        row.valueType = preset.getValueType(key);
        final String finalValue = value;
        row.listener = (v, hasFocus) -> {
            Log.d(DEBUG_TAG, "onFocusChange");
            String rowValue = row.getValue();
            if (!hasFocus && !rowValue.equals(finalValue)) {
                caller.updateSingleValue(key, rowValue);
                if (rowLayout instanceof EditableLayout) {
                    ((EditableLayout) rowLayout).putTag(key, rowValue);
                }
            }
        };
        row.valueCountKey = valueCountKey;
        if (valueCountValue != null && !"".equals(valueCountValue)) {
            try {
                row.valueCount = Integer.parseInt(valueCountValue);
            } catch (NumberFormatException nfex) {
                ScreenMessage.toastTopError(caller.getContext(), caller.getString(R.string.toast_invalid_number_format, nfex.getMessage()));
            }
        } else {
            row.valueCount = 0;
        }
        addValues(caller, key, adapter, row, splitValues);

        return row;
    }

    /**
     * Add values to the MultiTextRow
     * 
     * @param caller the calling fragment
     * @param key the key
     * @param adapter the adapter holding the values
     * @param row the MultiTestRow itself
     * @param splitValues a list of the split values
     */
    static void addValues(@NonNull final TagFormFragment caller, @NonNull final String key, @Nullable final ArrayAdapter<?> adapter,
            @NonNull final MultiTextRow row, @Nullable List<String> splitValues) {
        final boolean notEmpty = splitValues != null && !splitValues.isEmpty() && !(splitValues.size() == 1 && "".equals(splitValues.get(0)));
        if (notEmpty) {
            int phoneNumberReformatted = 0;
            int count = 1;
            for (String v : splitValues) {
                String orig = v;
                if (row.valueType == ValueType.PHONE) {
                    v = row.formatPhoneNumber(v);
                    if (!orig.equals(v)) {
                        phoneNumberReformatted++;
                    }
                }
                TextView tv = row.addEditText(v, row.listener, row.valueType, adapter, -1);
                if (row.valueCount > 0 && row.valueCount < count) {
                    SpannableString s = new SpannableString(tv.getText());
                    s.setSpan(new ForegroundColorSpan(ThemeUtils.getStyleAttribColorValue(caller.getContext(), R.attr.error, R.color.material_red)), 0,
                            s.length(), 0);
                    tv.setText(s);
                }
                count++;
            }
            if (phoneNumberReformatted > 0) {
                caller.updateSingleValue(key, row.getValue());
                row.post(() -> ScreenMessage.barWarning(row, R.string.toast_phone_number_reformatted, BaseTransientBottomBar.LENGTH_LONG));
            }
        }
        int currentCount = splitValues.size();
        int start = notEmpty ? currentCount : 0;
        boolean hasValueCount = row.valueCount > 0 && row.valueCountKey != null;
        int end = hasValueCount ? row.valueCount : (notEmpty ? currentCount + 1 : 1);
        for (int i = start; i < end; i++) {
            row.addEditText("", row.listener, row.valueType, row.adapter, -1);
        }
    }

    /**
     * Move from text field to text field
     */
    private final class MyKeyListener implements OnKeyListener {
        @Override
        public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                View nextView = view.focusSearch(View.FOCUS_DOWN);
                if (nextView != null && nextView.isFocusable()) {
                    nextView.requestFocus();
                }
            }
            return false;
        }
    }

    @Override
    public void changed(String key, String value) {
        if (key != null && key.equals(valueCountKey)) {
            if (value != null && !"".equals(value)) {
                try {
                    valueCount = Integer.parseInt(value);
                    values = Preset.splitValues(Util.wrapInList(getValue()), delimiter);
                    valueLayout.removeAllViews();
                    addValues(caller, key, adapter, this, values);
                } catch (NumberFormatException nfex) {
                    ScreenMessage.toastTopError(context, context.getString(R.string.toast_invalid_number_format, nfex.getMessage()));
                }
            } else {
                valueCount = 0;
            }
        }
    }
}
