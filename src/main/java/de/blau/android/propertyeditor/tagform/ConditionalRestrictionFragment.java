package de.blau.android.propertyeditor.tagform;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import ch.poole.conditionalrestrictionparser.Condition;
import ch.poole.conditionalrestrictionparser.Condition.CompOp;
import ch.poole.conditionalrestrictionparser.ConditionalRestrictionParser;
import ch.poole.conditionalrestrictionparser.Conditions;
import ch.poole.conditionalrestrictionparser.ParseException;
import ch.poole.conditionalrestrictionparser.Restriction;
import ch.poole.conditionalrestrictionparser.TokenMgrError;
import ch.poole.conditionalrestrictionparser.Util;
import ch.poole.openinghoursfragment.OnSaveListener;
import ch.poole.openinghoursfragment.OpeningHoursFragment;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.Tags;
import de.blau.android.presets.PresetItem;
import de.blau.android.propertyeditor.PropertyEditorListener;
import de.blau.android.propertyeditor.TagEditorFragment;
import de.blau.android.util.AfterTextChangedWatcher;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;

public class ConditionalRestrictionFragment extends DialogFragment implements OnSaveListener {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ConditionalRestrictionFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = ConditionalRestrictionFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String FRAGMENT_OPENING_HOURS_TAG = "fragment_opening_hours";

    private static final int LINEARLAYOUT_ID = 12345;

    /**
     * For now hardwired defaults in lieu of moving this to a DB
     */
    private static final String DESTINATION_MO_FR_19_00_07_00_SA_SU = "destination @ Mo-Fr 19:00-07:00,Sa,Su";

    private static final Map<String, String> DEFAULT_VALUES = new HashMap<>();

    private static final String KEY_KEY               = "key";
    private static final String VALUE_KEY             = "value";
    private static final String TEMPLATES_KEY         = "templates";
    private static final String OH_TEMPLATES_KEY      = "oh_templates";
    private static final String MAX_STRING_LENGTH_KEY = "maxStringLength";

    static {
        DEFAULT_VALUES.put(Tags.KEY_MAXSPEED, "50 @ Mo-Fr 19:00-07:00,Sa,Su");
        DEFAULT_VALUES.put(Tags.KEY_ACCESS, DESTINATION_MO_FR_19_00_07_00_SA_SU);
        DEFAULT_VALUES.put(Tags.KEY_VEHICLE, DESTINATION_MO_FR_19_00_07_00_SA_SU);
        DEFAULT_VALUES.put(Tags.KEY_BICYCLE, DESTINATION_MO_FR_19_00_07_00_SA_SU);
        DEFAULT_VALUES.put(Tags.KEY_MOTORCAR, DESTINATION_MO_FR_19_00_07_00_SA_SU);
        DEFAULT_VALUES.put(Tags.KEY_MOTORCYCLE, DESTINATION_MO_FR_19_00_07_00_SA_SU);
        DEFAULT_VALUES.put(Tags.KEY_MOTOR_VEHICLE, DESTINATION_MO_FR_19_00_07_00_SA_SU);
    }

    private LayoutInflater inflater = null;

    private String            key;
    private String            conditionalRestrictionValue;
    private ArrayList<String> templates;
    private ArrayList<String> ohTemplates;

    private List<Restriction> restrictions = null;

    private EditText text;

    /**
     * Lists of possible values generated from templates
     */
    private Set<String> restrictionValues         = new LinkedHashSet<>();
    private Set<String> simpleConditionValues     = new LinkedHashSet<>();
    private Set<String> expressionConditionValues = new LinkedHashSet<>();

    private ScrollView sv;

    private OnSaveListener         saveListener           = null;
    private PropertyEditorListener propertyEditorListener = null;

    private OnSaveListener realOnSaveListener = null;

    private transient boolean loadedDefault = false;

    private int maxStringLength;

    /**
     * Create a new instance
     * 
     * @param key the key for the conditional restriction
     * @param value a already present value or null
     * @param templates templates for the restriction values
     * @param ohTemplates opening hour templates
     * @param maxStringLength maximum length the value can have
     * @return an instance of ConditionalRestrictionFragment
     */
    @NonNull
    public static ConditionalRestrictionFragment newInstance(@NonNull String key, @Nullable String value, @NonNull ArrayList<String> templates, // NOSONAR
            @NonNull ArrayList<String> ohTemplates, int maxStringLength) { // NOSONAR bundle requires ArrayLists
        ConditionalRestrictionFragment f = new ConditionalRestrictionFragment();

        Bundle args = new Bundle();
        args.putSerializable(KEY_KEY, key);
        args.putSerializable(VALUE_KEY, value);
        args.putSerializable(TEMPLATES_KEY, templates);
        args.putSerializable(OH_TEMPLATES_KEY, ohTemplates);
        args.putInt(MAX_STRING_LENGTH_KEY, maxStringLength);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(DEBUG_TAG, "onAttach");
        saveListener = (OnSaveListener) de.blau.android.util.Util.getParentFragmentWithInterface(this, OnSaveListener.class);
        propertyEditorListener = (PropertyEditorListener) de.blau.android.util.Util.getParentFragmentWithInterface(this, PropertyEditorListener.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreateView");
        Context context = ThemeUtils.getThemedContext(getActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert, R.style.Theme_AppCompat_Dialog_Alert);
        this.inflater = ThemeUtils.getLayoutInflater(context);

        LinearLayout conditionalRestrictionLayout = (LinearLayout) inflater.inflate(R.layout.conditionalrestriction, null);

        if (savedInstanceState == null) {
            key = getArguments().getString(KEY_KEY);
            conditionalRestrictionValue = getArguments().getString(VALUE_KEY);
            templates = getArguments().getStringArrayList(TEMPLATES_KEY);
            ohTemplates = getArguments().getStringArrayList(OH_TEMPLATES_KEY);
            maxStringLength = getArguments().getInt(MAX_STRING_LENGTH_KEY);
        } else {
            key = savedInstanceState.getString(KEY_KEY);
            conditionalRestrictionValue = savedInstanceState.getString(VALUE_KEY);
            templates = savedInstanceState.getStringArrayList(TEMPLATES_KEY);
            ohTemplates = savedInstanceState.getStringArrayList(OH_TEMPLATES_KEY);
            maxStringLength = savedInstanceState.getInt(MAX_STRING_LENGTH_KEY);
        }

        String nonConditionalKey = null;
        if (conditionalRestrictionValue == null || "".equals(conditionalRestrictionValue)) {
            nonConditionalKey = key.replace(Tags.KEY_CONDITIONAL_SUFFIX, "");
            conditionalRestrictionValue = DEFAULT_VALUES.get(nonConditionalKey);
            loadedDefault = conditionalRestrictionValue != null;
        }
        // generate the list of possible values from the templates
        for (String t : templates) {
            Log.d(DEBUG_TAG, "Parsing template " + t);
            ConditionalRestrictionParser parser = new ConditionalRestrictionParser(new ByteArrayInputStream(t.getBytes()));
            try {
                List<Restriction> list = parser.restrictions();
                for (Restriction r : list) {
                    String v = r.getValue();
                    try {
                        // noinspection ResultOfMethodCallIgnored
                        Integer.parseInt(v);
                    } catch (NumberFormatException nfex) {
                        // not a number add it to list
                        restrictionValues.add(v);
                    }
                    for (Condition c : r.getConditions()) {
                        if (c.isExpression()) {
                            try {
                                // noinspection ResultOfMethodCallIgnored
                                Integer.parseInt(c.term1());
                                expressionConditionValues.add(c.term2());
                            } catch (NumberFormatException nfex) {
                                // not a number add it to list
                                expressionConditionValues.add(c.term1());
                            }
                        } else if (!c.isOpeningHours()) {
                            simpleConditionValues.add(c.term1());
                        }
                    }
                }
            } catch (Exception ex) {
                Log.e(DEBUG_TAG, "Parsing template " + t + " raised " + ex);
            }
        }

        if (Tags.isSpeedKey(nonConditionalKey) && propertyEditorListener != null) {
            String[] speedLimits = TagEditorFragment.getSpeedLimits(getContext(), propertyEditorListener);
            if (speedLimits != null) {
                restrictionValues.addAll(Arrays.asList(speedLimits));
            }
        }

        buildLayout(conditionalRestrictionLayout, conditionalRestrictionValue);

        // add callbacks for the buttons
        AppCompatButton cancel = (AppCompatButton) conditionalRestrictionLayout.findViewById(R.id.cancel);
        cancel.setOnClickListener(v -> dismiss());

        AppCompatButton save = (AppCompatButton) conditionalRestrictionLayout.findViewById(R.id.save);
        save.setOnClickListener(v -> {
            saveListener.save(key, text.getText().toString());
            dismiss();
        });

        return conditionalRestrictionLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume");
        if (loadedDefault) {
            ScreenMessage.toastTopWarning(getContext(), getString(R.string.loaded_default));
        }
    }

    private final TextWatcher watcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            text.removeCallbacks(rebuild);
            text.postDelayed(rebuild, 500);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // unused
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // unused
        }
    };

    private final Runnable rebuild = () -> {
        text.removeTextChangedListener(watcher); // avoid infinite loop
        Editable t = text.getText();
        if (t != null) {
            de.blau.android.util.Util.sanitizeString(getContext(), t, maxStringLength);
        }
        ConditionalRestrictionParser parser = new ConditionalRestrictionParser(new ByteArrayInputStream(t.toString().getBytes()));
        try {
            restrictions = parser.restrictions();
            removeHighlight(text);
        } catch (ParseException pex) {
            Log.d(DEBUG_TAG, "rebuild got " + pex.getMessage());
            highlightParseError(text, pex);
        } catch (TokenMgrError err) { // NOSONAR JavaCC parsers with return an Error for unknown tokens
            // we currently can't do anything reasonable here except ignore
            Log.e(DEBUG_TAG, "rebuild got " + err.getMessage());
        }
        text.addTextChangedListener(watcher);
        if (restrictions == null) { // couldn't parse anything
            restrictions = new ArrayList<>();
        }
        buildForm(sv, restrictions);
    };

    /**
     * Initial setup of layout
     * 
     * @param conditionalRestrictionLayout the layout that will hold our views
     * @param conditionalRestrictionValue the current restriction value
     */
    private void buildLayout(@NonNull final LinearLayout conditionalRestrictionLayout, @NonNull final String conditionalRestrictionValue) {
        text = (EditText) conditionalRestrictionLayout.findViewById(R.id.conditional_restriction_string_edit);
        sv = (ScrollView) conditionalRestrictionLayout.findViewById(R.id.conditional_restriction_view);

        if (text != null && sv != null) {
            text.addTextChangedListener(watcher);
            text.setText(conditionalRestrictionValue);
            sv.removeAllViews();
            final FloatingActionButton fab = (FloatingActionButton) conditionalRestrictionLayout.findViewById(R.id.add);
            fab.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), fab);

                // menu items for adding rules
                MenuItem addRestriction = popup.getMenu().add(R.string.tag_restriction_add_restriction);
                addRestriction.setOnMenuItemClickListener(item -> {
                    LinearLayout ll = (LinearLayout) conditionalRestrictionLayout.findViewById(LINEARLAYOUT_ID);
                    ArrayList<Condition> c = new ArrayList<>();
                    c.add(new Condition("", false));
                    Restriction r = new Restriction("", new Conditions(c, false));
                    restrictions.add(r);
                    addRestriction(ll, r);
                    return true;
                });

                MenuItem refresh = popup.getMenu().add(R.string.refresh);
                refresh.setOnMenuItemClickListener(item -> {
                    updateString();
                    watcher.afterTextChanged(null); // hack to force rebuild of form
                    return true;
                });
                MenuItem clear = popup.getMenu().add(R.string.clear);
                clear.setEnabled(restrictions != null && !restrictions.isEmpty());
                clear.setOnMenuItemClickListener(item -> {
                    if (restrictions != null) {
                        restrictions.clear();
                        updateString();
                        watcher.afterTextChanged(null); // hack to force rebuild of form
                    } else {
                        text.setText("");
                        watcher.afterTextChanged(null);
                    }
                    return true;
                });
                popup.show();// showing popup menu
            });
            // initial build
            text.removeCallbacks(rebuild);
            text.post(rebuild);
        }
    }

    /**
     * Highlight the position of a parse error
     * 
     * @param text the EditText
     * @param pex the ParseException
     */
    private synchronized void highlightParseError(@NonNull EditText text, @NonNull ParseException pex) {
        if (text.length() > 0) {
            int c = pex.currentToken.next.beginColumn - 1; // starts at 1
            int pos = text.getSelectionStart();
            Spannable spannable = new SpannableString(text.getText());
            spannable.setSpan(new ForegroundColorSpan(Color.RED), c, Math.max(c, Math.min(c + 1, spannable.length())), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setText(spannable, TextView.BufferType.SPANNABLE);
            text.setSelection(Math.min(pos, spannable.length()));
            ScreenMessage.toastTopError(getActivity(), pex.getLocalizedMessage());
        }
    }

    /**
     * Remove all highlighting from an EditText
     * 
     * @param text the EditText
     */
    private synchronized void removeHighlight(@NonNull EditText text) {
        int pos = text.getSelectionStart();
        int prevLen = text.length();
        if (restrictions != null) {
            String t = Util.restrictionsToString(restrictions);
            text.setText(t);
        }
        text.setSelection(prevLen < text.length() ? text.length() : Math.min(pos, text.length()));
    }

    /**
     * Build the complete form from a List of Restrictions
     * 
     * @param sv the ScrollView everything is displayed in
     * @param restrictions the List of Restrictions
     */
    private synchronized void buildForm(@NonNull ScrollView sv, @NonNull List<Restriction> restrictions) {
        sv.removeAllViews();
        Activity a = getActivity();
        if (a == null) {
            return;
        }
        LinearLayout ll = new LinearLayout(a);
        ll.setId(LINEARLAYOUT_ID);
        ll.setPadding(0, 0, 0, dpToPixels(64));
        ll.setOrientation(LinearLayout.VERTICAL);
        sv.addView(ll);

        for (Restriction r : restrictions) {
            addRestriction(ll, r);
        }
    }

    /**
     * Add one Restriction to a LinearLayout
     * 
     * @param ll the LinearLayout
     * @param r the Restriction
     */
    private void addRestriction(@NonNull LinearLayout ll, @NonNull final Restriction r) {
        LinearLayout groupHeader = (LinearLayout) inflater.inflate(R.layout.restriction_header, null);
        TextView header = (TextView) groupHeader.findViewById(R.id.header);
        header.setText(R.string.tag_restriction_header);
        final AutoCompleteTextView value = (AutoCompleteTextView) groupHeader.findViewById(R.id.editValue);
        String v = r.getValue().trim();
        value.setText(v);
        if (!restrictionValues.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.autocomplete_row, new ArrayList<>(restrictionValues));
            if (!restrictionValues.contains(v)) {
                adapter.insert(v, 0);
            }
            setAdapterAndListeners(value, adapter);
        }
        TextWatcher valueWatcher = (AfterTextChangedWatcher) ((Editable edited) -> {
            r.setValue(value.getText().toString().trim());
            Runnable ourRebuild = () -> updateRestrictionStringFromView(value, r);
            value.removeCallbacks(ourRebuild);
            value.postDelayed(ourRebuild, 500);
        });
        value.addTextChangedListener(valueWatcher);
        addMenuItems(groupHeader, r, null);
        ll.addView(groupHeader);
        boolean first = true;
        for (int i = 0; i < r.getConditions().size(); i++) {
            Condition c = r.getConditions().get(i);
            if (c != null) {
                final int index = i;
                if (c.isExpression()) {
                    LinearLayout expression = (LinearLayout) inflater.inflate(R.layout.expression, null);
                    final TextView expressionText = (TextView) expression.findViewById(R.id.text);
                    if (first) {
                        expressionText.setText(R.string.tag_restriction_when);
                        first = false;
                    } else {
                        expressionText.setText(R.string.tag_restriction_and);
                    }
                    final AutoCompleteTextView term1 = (AutoCompleteTextView) expression.findViewById(R.id.editTerm1);
                    term1.setText(c.term1());
                    final AutoCompleteTextView term2 = (AutoCompleteTextView) expression.findViewById(R.id.editTerm2);
                    term2.setText(c.term2());
                    AutoCompleteTextView term = null;
                    if (!expressionConditionValues.isEmpty()) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.autocomplete_row, new ArrayList<>(expressionConditionValues));
                        try {
                            // noinspection ResultOfMethodCallIgnored
                            Integer.parseInt(c.term1());
                            if (!expressionConditionValues.contains(c.term2())) {
                                adapter.insert(c.term2(), 0);
                            }
                            term = term2;
                        } catch (NumberFormatException nfex) {
                            if (!expressionConditionValues.contains(c.term1())) {
                                adapter.insert(c.term1(), 0);
                            }
                            term = term1;
                        }
                        setAdapterAndListeners(term, adapter);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.support_simple_spinner_dropdown_item, Condition.compOpStrings);
                    adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
                    final Spinner operator = (Spinner) expression.findViewById(R.id.operator);
                    operator.setAdapter(adapter);
                    operator.setSelection(Condition.compOpStrings.indexOf(Condition.opToString(c.operator())));
                    final Runnable ourRebuild = () -> updateRestrictionStringFromView(null, r);
                    operator.setOnItemSelectedListener(new OnItemSelectedListener() {
                        boolean isLoaded = false;

                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (isLoaded) { // avoid spurious first call
                                List<Condition> list = r.getConditions();
                                final String c = term1.getText().toString().trim() + operator.getSelectedItem() + term2.getText().toString().trim();
                                list.set(index, new Condition(c, false));
                                term1.removeCallbacks(ourRebuild);
                                term1.post(ourRebuild);
                            }
                            isLoaded = true;
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            // unused
                        }
                    });
                    TextWatcher expressionWatcher = new TextWatcher() {
                        @Override
                        public void afterTextChanged(Editable s) {
                            List<Condition> list = r.getConditions();
                            final String c = term1.getText().toString().trim() + operator.getSelectedItem() + term2.getText().toString().trim();
                            list.set(index, new Condition(c, false));
                            term1.removeCallbacks(ourRebuild);
                            term1.postDelayed(ourRebuild, 500);
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            // unused
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            // unused
                        }
                    };
                    term1.addTextChangedListener(expressionWatcher);
                    term2.addTextChangedListener(expressionWatcher);
                    addMenuItems(expression, r, c);
                    ll.addView(expression);
                } else if (c.isOpeningHours()) {
                    OpeningHoursDialogRow conditionOh = (OpeningHoursDialogRow) inflater.inflate(R.layout.condition_oh, null);
                    final TextView conditionOhText = (TextView) conditionOh.findViewById(R.id.text);
                    if (first) {
                        conditionOhText.setText(R.string.tag_restriction_when);
                        first = false;
                    } else {
                        conditionOhText.setText(R.string.tag_restriction_and);
                    }
                    addOpeningHoursDialogField(conditionOh, key, r, c);
                    addMenuItems(conditionOh, r, c);
                    ll.addView(conditionOh);
                } else {
                    LinearLayout condition = (LinearLayout) inflater.inflate(R.layout.condition, null);
                    final TextView conditionText = (TextView) condition.findViewById(R.id.text);
                    if (first) {
                        conditionText.setText(R.string.tag_restriction_when);
                        first = false;
                    } else {
                        conditionText.setText(R.string.tag_restriction_and);
                    }
                    final AutoCompleteTextView term = (AutoCompleteTextView) condition.findViewById(R.id.editCondition);
                    term.setText(c.term1());

                    if (!simpleConditionValues.isEmpty()) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.autocomplete_row, new ArrayList<>(simpleConditionValues));
                        if (!simpleConditionValues.contains(c.term1())) {
                            adapter.insert(c.term1(), 0);
                        }
                        setAdapterAndListeners(term, adapter);
                    }
                    TextWatcher conditionWatcher = new TextWatcher() {
                        @Override
                        public void afterTextChanged(Editable s) {
                            List<Condition> list = r.getConditions();
                            String c = term.getText().toString().trim();
                            boolean needsParentheses = c.indexOf(';') >= 0;
                            if (needsParentheses) {
                                r.setInParen();
                            }
                            list.set(index, new Condition(c, false));
                            Runnable ourRebuild = () -> updateRestrictionStringFromView(term, r);
                            term.removeCallbacks(ourRebuild);
                            term.postDelayed(ourRebuild, 500);
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            // unused
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            // unused
                        }
                    };
                    term.addTextChangedListener(conditionWatcher);
                    addMenuItems(condition, r, c);
                    ll.addView(condition);
                }
            }
        }
    }

    /**
     * Set the ArrayAdapter for the dropdown on an AutoCompleteTextView
     * 
     * @param atv the AutoCompleteTextView
     * @param adapter the ArrayAdapter
     */
    private void setAdapterAndListeners(@NonNull AutoCompleteTextView atv, @NonNull ArrayAdapter<String> adapter) {
        atv.setAdapter(adapter);
        atv.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ((AutoCompleteTextView) v).showDropDown();
            }
        });
        atv.setOnClickListener(v -> {
            if (v.hasFocus()) {
                ((AutoCompleteTextView) v).showDropDown();
            }
        });
    }

    /**
     * Update the text display of the restriction
     * 
     * @param view an EditText containing ?
     * @param r the object containing the restriction
     */
    private synchronized void updateRestrictionStringFromView(@Nullable EditText view, @NonNull Restriction r) {
        text.removeTextChangedListener(watcher);
        int pos = 0;
        if (view != null) {
            pos = view.getSelectionStart();
        }
        conditionalRestrictionValue = Util.restrictionsToString(restrictions);
        text.setText(conditionalRestrictionValue);
        if (text.getText() != null) {
            de.blau.android.util.Util.sanitizeString(getContext(), text.getText(), maxStringLength);
        }
        String myText = text.getText().toString();
        int textPos = conditionalRestrictionValue.lastIndexOf(myText);
        // make what we are currently editing visible, this is a bit of a hack
        if (textPos < conditionalRestrictionValue.length() / 2) {
            text.setSelection(textPos);
        } else {
            text.setSelection(textPos + myText.length());
        }
        if (view != null) {
            view.setSelection(Math.min(pos, text.length()));
        }
        text.addTextChangedListener(watcher);
    }

    /**
     * Add menu items to the restriction menu to add various types of conditions
     * 
     * @param menu the menu
     * @param item item to add
     * @param r the restriction
     * @param c the new condition
     */
    private void addConditionItem(@NonNull Menu menu, int item, @NonNull final Restriction r, @NonNull final Condition c) {
        MenuItem mi = menu.add(item);
        mi.setOnMenuItemClickListener(unused -> {
            Log.d(DEBUG_TAG, "onMenuItemClick");
            r.getConditions().add(c);
            buildForm(sv, restrictions);
            // don't generate the string here as empty items shouldn't change anything
            return true;
        });
        mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    /**
     * Add the standard menu items to restrictions or condition rows
     * 
     * @param row the Layout holding the row
     * @param r the restriction
     * @param c the condition, if null we assume that we are adding to the restriction row
     * @return the created Menu
     */
    @NonNull
    private Menu addMenuItems(@NonNull LinearLayout row, @NonNull final Restriction r, @Nullable final Condition c) {
        ActionMenuView amv = (ActionMenuView) row.findViewById(R.id.menu);
        Menu menu = amv.getMenu();
        if (c == null) {
            addConditionItem(menu, R.string.tag_restriction_add_simple_condition, r, new Condition("", false));
            addConditionItem(menu, R.string.tag_restriction_add_expression, r, new Condition("", CompOp.EQ, ""));
            addConditionItem(menu, R.string.tag_restriction_add_opening_hours, r, new Condition("", true));
        }
        MenuItem mi = menu.add(R.string.delete);
        mi.setOnMenuItemClickListener(unused -> {
            Log.d(DEBUG_TAG, "onMenuItemClick");
            if (c == null) {
                restrictions.remove(r);
            } else {
                r.getConditions().remove(c);
                if (r.getConditions().isEmpty()) {
                    r.clearInParen();
                }
            }
            updateString();
            watcher.afterTextChanged(null);
            return true;
        });
        mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return menu;
    }

    private Runnable updateStringRunnable = () -> {
        int pos = text.getSelectionStart();
        int prevLen = text.length();
        text.removeTextChangedListener(watcher);
        if (restrictions != null) {
            String r = Util.restrictionsToString(restrictions, false);
            text.setText(r);
            text.setSelection(prevLen < text.length() ? text.length() : Math.min(pos, text.length()));
            text.addTextChangedListener(watcher);
        }
    };

    /**
     * Update the actual CR string
     */
    private void updateString() {
        text.removeCallbacks(updateStringRunnable);
        text.postDelayed(updateStringRunnable, 100);
    }

    /**
     * Conditional restrictions can contain OH fields, this add a special UI element to dispaly and start editing them
     * 
     * @param row an OpeningHoursDialogRow
     * @param key the key
     * @param r the restrictions
     * @param c the condition
     */
    private void addOpeningHoursDialogField(@NonNull final OpeningHoursDialogRow row, @NonNull final String key, @NonNull final Restriction r,
            @NonNull final Condition c) {

        boolean strictSucceeded = false;
        boolean lenientSucceeded = false;
        List<Rule> rules = null;

        String value = c.term1();

        OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(value.getBytes()));

        try {
            rules = parser.rules(true, false);
            strictSucceeded = true;
        } catch (Exception e) {
            parser = new OpeningHoursParser(new ByteArrayInputStream(value.getBytes()));
            try {
                rules = parser.rules(false);
                value = ch.poole.openinghoursparser.Util.rulesToOpeningHoursString(rules);
                lenientSucceeded = true;
            } catch (Exception e1) {
                // failed
                rules = null;
            }
        }

        row.setValue(value, rules);

        if (value != null && !"".equals(value) && !strictSucceeded) {
            int toast = lenientSucceeded ? R.string.toast_openinghours_autocorrected : R.string.toast_openinghours_invalid;
            row.post(() -> ScreenMessage.toastTopWarning(getActivity(), getString(toast, key)));
        }

        row.valueView.setHint(R.string.tag_tap_to_edit_hint);
        final String finalValue = value;
        row.setOnClickListener(v -> {
            FragmentManager fm = getChildFragmentManager();
            de.blau.android.propertyeditor.Util.removeChildFragment(fm, FRAGMENT_OPENING_HOURS_TAG);
            realOnSaveListener = (k, val) -> {
                c.setTerm1(val);
                if (val.contains(";")) { // hack but saves us re-parsing
                    r.setInParen();
                }
                updateString();
                watcher.afterTextChanged(null);
            };
            OpeningHoursFragment openingHoursDialog = OpeningHoursFragment.newInstanceForFragment(key, finalValue,
                    App.getPreferences(getActivity()).lightThemeEnabled() ? R.style.Theme_AppCompat_Light_Dialog_Alert : R.style.Theme_AppCompat_Dialog_Alert,
                    -1);
            openingHoursDialog.show(fm, FRAGMENT_OPENING_HOURS_TAG);
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putSerializable(KEY_KEY, key);
        outState.putSerializable(VALUE_KEY, text.getText().toString());
        outState.putSerializable(TEMPLATES_KEY, templates);
        outState.putSerializable(OH_TEMPLATES_KEY, ohTemplates);
        outState.putInt(MAX_STRING_LENGTH_KEY, maxStringLength);
    }

    /**
     * Return the view we have our rows in and work around some android craziness
     * 
     * @return the View holding the UI elements or null
     */
    @Nullable
    public View getOurView() {
        // android.support.v4.app.NoSaveStateFrameLayout
        View v = getView();
        if (v != null) {
            if (v.getId() == R.id.conditional_restriction_layout) {
                Log.d(DEBUG_TAG, "got correct view in getView");
                return v;
            } else {
                v = v.findViewById(R.id.conditional_restriction_layout);
                if (v == null) {
                    Log.d(DEBUG_TAG, "didn't find R.id.openinghours_layout");
                } else {
                    Log.d(DEBUG_TAG, "Found R.id.openinghours_layoutt");
                }
                return v;
            }
        } else {
            Log.d(DEBUG_TAG, "got null view in getView");
        }
        return null;
    }

    /**
     * Convert density independent pixels to screen pixels
     * 
     * @param dp the density independent pixel value
     * @return the corresponding number of screen pixels
     */
    private int dpToPixels(int dp) {
        Resources r = getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    /**
     * Row that displays opening_hours values and allows changing them via a dialog
     */
    public static class OpeningHoursDialogRow extends LinearLayout {
        TextView   keyView;
        TextView   valueView;
        PresetItem preset;

        OnClickListener listener;

        LinearLayout         valueList;
        final LayoutInflater inflater;
        int                  errorTextColor = ThemeUtils.getStyleAttribColorValue(getContext(), R.attr.error, R.color.material_red);

        /**
         * Construct a new row holding an OH value
         * 
         * @param context an Android Context
         */
        public OpeningHoursDialogRow(@NonNull Context context) {
            super(context);
            inflater = LayoutInflater.from(context);
        }

        /**
         * Construct a new row holding an OH value
         * 
         * @param context an Android Context
         * @param attrs an AttributeSet
         */
        public OpeningHoursDialogRow(@NonNull Context context, AttributeSet attrs) {
            super(context, attrs);
            inflater = LayoutInflater.from(context);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            valueList = (LinearLayout) findViewById(R.id.valueList);
            valueView = (TextView) findViewById(R.id.textValue);
        }

        /**
         * Set the onclicklistener for every value
         */
        @Override
        public void setOnClickListener(final OnClickListener listener) {
            this.listener = listener;
            for (int pos = 0; pos < valueList.getChildCount(); pos++) {
                View v = valueList.getChildAt(pos);
                if (v instanceof TextView) {
                    ((TextView) v).setOnClickListener(listener);
                }
            }
        }

        /**
         * Set the value
         * 
         * @param value the value
         * @param description the description
         */
        public void setValue(@NonNull String value, @NonNull String description) {
            valueView.setText(description);
            valueView.setTag(value);
        }

        /**
         * Set the value and decription to the same String
         * 
         * @param s the value
         */
        public void setValue(@NonNull String s) {
            setValue(s, s);
        }

        /**
         * Set the OH value for the row
         * 
         * @param ohValue the original opening hours value
         * @param rules rules parsed from the value
         */
        public void setValue(String ohValue, @Nullable List<Rule> rules) {
            de.blau.android.propertyeditor.Util.resetValueLayout(valueList, () -> setValue("", ""));
            boolean first = true;
            if (rules != null && !rules.isEmpty()) {
                for (Rule r : rules) {
                    if (first) {
                        setValue(r.toString());
                        first = false;
                    } else {
                        Log.d(DEBUG_TAG, "adding view for " + r);
                        TextView extraValue = (TextView) inflater.inflate(R.layout.condition_oh_value, valueList, false);
                        extraValue.setText(r.toString());
                        extraValue.setTag(r.toString());
                        valueList.addView(extraValue);
                    }
                }
            } else {
                setValue(ohValue);
                if (ohValue != null && !"".equals(ohValue)) {
                    valueView.setTextColor(errorTextColor);
                }
            }
            setOnClickListener(listener);
        }
    }

    @Override
    public void save(String key, String value) {
        if (realOnSaveListener != null) {
            realOnSaveListener.save(key, value);
        }
    }
}
