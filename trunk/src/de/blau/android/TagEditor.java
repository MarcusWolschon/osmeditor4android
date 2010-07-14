package de.blau.android;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.blau.android.presets.TagKeyAutocompletionAdapter;
import de.blau.android.presets.TagValueAutocompletionAdapter;

/**
 * An Activity to edit OSM-Tags. Sends the edited Tags as Result to its caller-Activity (normally {@link Main}).
 * 
 * @author mb
 */
public class TagEditor extends Activity {

    public static final String TAGS = "tags";

    public static final String TYPE = "type";

    public static final String OSM_ID = "osm_id";

    private LinearLayout verticalLayout = null;

    private static final LinearLayout.LayoutParams layoutParamValue = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);

    /**
     * The tag we use for Android-logging.
     */
//    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = TagEditor.class.getName();

    /**
     * One of the input-elements for the user to enter a tag-key. 
     */
    private AutoCompleteTextView lastEditKey;

    /**
     * One of the input-elements for the user to enter a tag-value. 
     */
    private EditText lastEditValue;

    private long osmId;

    private String type;

    private boolean modified = false;

    /**
     * Insert a new row of key+value -edit-widgets if some text is entered into
     * the current one.
     */
    private final OnKeyListener myKeyListener = new MyKeyListener();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        osmId = getIntent().getLongExtra(OSM_ID, 0);
        type = getIntent().getStringExtra(TYPE);

        //Not yet implemented by Google
        //getWindow().requestFeature(Window.FEATURE_CUSTOM_TITLE);
        //getWindow().setTitle(getResources().getString(R.string.tag_title) + " " + type + " " + osmId);

        // Disabled because it slows down the Motorola Milestone/Droid
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        
        setContentView(R.layout.tag_view);

        verticalLayout = (LinearLayout) findViewById(R.id.vertical_layout);

        extrasToEdits();
        insertNewEdits("", "");

        createOkButton();
    }

    private void createOkButton() {
        Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (modified) {
                    sendResultAndFinish();
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tag_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.tag_menu_mapfeatures:
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources()
                    .getString(R.string.link_mapfeatures)));
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (modified) {
                sendResultAndFinish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 
     */
    protected void sendResultAndFinish() {
        Intent intent = new Intent();
        intent.putExtras(getKeyValueFromEdits());
        intent.putExtra(OSM_ID, osmId);
        intent.putExtra(TYPE, type);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    protected void extrasToEdits() {
        ArrayList<String> tags = (ArrayList<String>) getIntent().getSerializableExtra(TAGS);
        for (int i = 0, size = tags.size(); i < size; i+=2) {
            insertNewEdits(tags.get(i), tags.get(i + 1));
        }
    }

    /**
     * Insert a new row with one key and one value to edit.
     * @param aTagKey the key-value to start with
     * @param aTagValue the value to start with.
     */
    protected void insertNewEdits(final String aTagKey, final String aTagValue) {
        LinearLayout horizontalLayout = new LinearLayout(this);
        TextView textKey = new TextView(this);
        TextView textValue = new TextView(this);

        textKey.setText(R.string.key);
        textKey.setTextColor(Color.BLACK);
        final AutoCompleteTextView keyEdit = new AutoCompleteTextView(this); 
        lastEditKey = keyEdit;
        lastEditKey.setOnKeyListener(myKeyListener);
        lastEditKey.setSingleLine(true);
        //TODO: we may parse JOSM-stylesheets for these values and even add an ArrayAdapter for the values
        //ArrayAdapter<String> knownTagNamesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.known_tags));
        ArrayAdapter<String> knownTagNamesAdapter;
        try {
            knownTagNamesAdapter = new TagKeyAutocompletionAdapter(this, android.R.layout.simple_dropdown_item_1line);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "cannot create TagKeyAutocompletionAdapter", e);
            knownTagNamesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.known_tags));
        }
        lastEditKey.setAdapter(knownTagNamesAdapter);
        lastEditKey.setThreshold(3); // give suggestions after 3 characters
        horizontalLayout.addView(textKey);
        horizontalLayout.addView(lastEditKey, layoutParamValue);


        textValue.setText(R.string.value);
        textValue.setTextColor(Color.BLACK);
        final AutoCompleteTextView valueEdit = new AutoCompleteTextView(this);
        lastEditValue = valueEdit;
        lastEditValue.setOnKeyListener(myKeyListener);
        lastEditValue.setSingleLine(true);
        horizontalLayout.addView(textValue);
        horizontalLayout.addView(lastEditValue, layoutParamValue);

        // change auto-completion -values for the tag-value if the tag-key changes.
        //TODO: if the rule is <combo only provide a list
        lastEditKey.addTextChangedListener(new TextWatcher() {
            
            @Override
            public void onTextChanged(final CharSequence aS,
            		                  final int aStart,
                                      final int aBefore,
                                      final int aCount) {
                setAutocompletion();
            }
            
            @Override
            public void beforeTextChanged(final CharSequence aS,
            		                      final int aStart,
                                          final int aCount,
                                          final int aAfter) {
                setAutocompletion();   
            }
            
            @Override
            public void afterTextChanged(final Editable aS) {
                setAutocompletion();
            }

            /**
             * add an adapter to valueEdit, that gives autocompletion-suggestions
             * based on the value of keyEdit.getText().toString().
             */
            private void setAutocompletion() {
                ArrayAdapter<String> knownTagValuesAdapter = null;
                try {
                    knownTagValuesAdapter = new TagValueAutocompletionAdapter(TagEditor.this, android.R.layout.simple_dropdown_item_1line, keyEdit.getText().toString());
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "cannot create TagValueAutocompletionAdapter forkey \"" + keyEdit.getText().toString() + "\"", e);
                    knownTagValuesAdapter = new ArrayAdapter<String>(TagEditor.this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.known_tags));
                }
                valueEdit.setAdapter(knownTagValuesAdapter);
            }
        });

        lastEditKey.setText(aTagKey);
        lastEditValue.setText(aTagValue);

        verticalLayout.addView(horizontalLayout, verticalLayout.getChildCount() - 1);
    }

    /**
     * Collect all key-value pairs into a bundle
     * to return them.
     * @return
     */
    private Bundle getKeyValueFromEdits() {
        Bundle bundle = new Bundle(1);
        ArrayList<String> tags = new ArrayList<String>();
        final int size = verticalLayout.getChildCount();
        for (int i = 0; i < size; ++i) {
            View view = verticalLayout.getChildAt(i);

            if (view instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) view;
                if (row.getChildCount() == 4) { // 2 labels, 2 EditText
                    View keyView = row.getChildAt(1);
                    View valueView = row.getChildAt(3);
                    if (keyView instanceof EditText && valueView instanceof EditText) {
                        String key   = ((EditText) keyView  ).getText().toString().trim();
                        String value = ((EditText) valueView).getText().toString().trim();
                        if (!"".equals(key) && !"".equals(value)) {
                            tags.add(key);
                            tags.add(value);
                        }
                    }
                }
            }

        }
        bundle.putSerializable(TAGS, tags);
        return bundle;
    }

    /**
     * Insert a new row of key+value -edit-widgets if some text is entered into
     * the current one.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private class MyKeyListener implements OnKeyListener {
        @Override
        public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) {
                if (view instanceof EditText) {
                    modified = true;
                    String key = lastEditKey.getText().toString();
                    String value = lastEditValue.getText().toString();
                    if (!"".equals(key.trim()) && !"".equals(value.trim())) {
                        insertNewEdits("", "");
                    }

                    //on Enter -> goto next EditText
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        View nextView = view.focusSearch(View.FOCUS_RIGHT);
                        if (!(nextView instanceof EditText)) {
                            nextView = view.focusSearch(View.FOCUS_LEFT);
                            if (nextView != null) {
                                nextView = nextView.focusSearch(View.FOCUS_DOWN);
                            }
                        }

                        if (nextView != null && nextView instanceof EditText) {
                            nextView.requestFocus();
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    protected EditText getLastEditKey() {
        return lastEditKey;
    }

    protected void setLastEditKey(final AutoCompleteTextView lastEditKey) {
        this.lastEditKey = lastEditKey;
    }

    protected EditText getLastEditValue() {
        return lastEditValue;
    }

    protected void setLastEditValue(final EditText lastEditValue) {
        this.lastEditValue = lastEditValue;
    }

    protected LinearLayout getVerticalLayout() {
        return verticalLayout;
    }

    public long getOsmId() {
        return osmId;
    }

    public void setOsmId(long osmId) {
        this.osmId = osmId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    protected OnKeyListener getKeyListener() {
        return myKeyListener;
    }
}
