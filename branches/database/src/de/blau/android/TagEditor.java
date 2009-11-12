package de.blau.android;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

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

	private static final LinearLayout.LayoutParams paramValue = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);

	@SuppressWarnings("unused")
	private static final String DEBUG_TAG = "TagActivity";

	private EditText lastEditKey;

	private EditText lastEditValue;

	private long osmId;

	private String type;

	private boolean modified = false;

	private final OnKeyListener myKeyListener = new MyKeyListener();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		osmId = getIntent().getLongExtra(OSM_ID, 0);
		type = getIntent().getStringExtra(TYPE);

		//Not yet implemented by Google
		//getWindow().requestFeature(Window.FEATURE_CUSTOM_TITLE);
		//getWindow().setTitle(getResources().getString(R.string.tag_title) + " " + type + " " + osmId);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		setContentView(R.layout.tag_view);

		verticalLayout = (LinearLayout) findViewById(R.id.vertical_layout);

		extrasToEdits();
		insertNewEdits();

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
	private void sendResultAndFinish() {
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
	private void extrasToEdits() {
		ArrayList<String> tags = (ArrayList<String>) getIntent().getSerializableExtra(TAGS);
		for (int i = 0, size = tags.size(); i < size; i+=2) {
			insertNewEdits();
			lastEditKey.setText(tags.get(i));
			lastEditValue.setText(tags.get(i+1));
		}
	}

	private void insertNewEdits() {
		LinearLayout horizontalLayout = new LinearLayout(this);
		TextView textKey = new TextView(this);
		TextView textValue = new TextView(this);

		textKey.setText(R.string.key);
		textKey.setTextColor(Color.BLACK);
		lastEditKey = new EditText(this);
		lastEditKey.setOnKeyListener(myKeyListener);
		lastEditKey.setSingleLine(true);
		horizontalLayout.addView(textKey);
		horizontalLayout.addView(lastEditKey, paramValue);

		textValue.setText(R.string.value);
		textValue.setTextColor(Color.BLACK);
		lastEditValue = new EditText(this);
		lastEditValue.setOnKeyListener(myKeyListener);
		lastEditValue.setSingleLine(true);
		horizontalLayout.addView(textValue);
		horizontalLayout.addView(lastEditValue, paramValue);

		verticalLayout.addView(horizontalLayout, verticalLayout.getChildCount() - 1);
	}

	private Bundle getKeyValueFromEdits() {
		Bundle bundle = new Bundle(1);
		ArrayList<String> tags = new ArrayList<String>();
		for (int i = 0, size = verticalLayout.getChildCount(); i < size; ++i) {
			View view = verticalLayout.getChildAt(i);
			if (view instanceof LinearLayout) {
				LinearLayout row = (LinearLayout) view;
				if (row.getChildCount() == 4) {
					View keyView = row.getChildAt(1);
					View valueView = row.getChildAt(3);
					if (keyView instanceof EditText && valueView instanceof EditText) {
						String key = ((EditText) keyView).getText().toString().trim();
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

	private class MyKeyListener implements OnKeyListener {
		@Override
		public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
			if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
				if (view instanceof EditText) {
					modified = true;
					String key = lastEditKey.getText().toString();
					String value = lastEditValue.getText().toString();
					if (!"".equals(key.trim()) && !"".equals(value.trim())) {
						insertNewEdits();
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
}
