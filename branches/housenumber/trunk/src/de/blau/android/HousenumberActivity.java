/*
 * Created on 07.03.2009
 */
package de.blau.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * A simple number pad used to enter house numbers. TODO: only supports plain numbers, not housenumbers like "2a"
 * 
 * @author Daniel Naber
 */
public class HousenumberActivity extends Activity {

	static final String HOUSENUMBER = "housenumber";

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.number_pad);

		final TextView numberView = (TextView) findViewById(R.id.number);

		addListener(numberView, R.id.button0, "0");
		addListener(numberView, R.id.button1, "1");
		addListener(numberView, R.id.button2, "2");
		addListener(numberView, R.id.button3, "3");
		addListener(numberView, R.id.button4, "4");
		addListener(numberView, R.id.button5, "5");
		addListener(numberView, R.id.button6, "6");
		addListener(numberView, R.id.button7, "7");
		addListener(numberView, R.id.button8, "8");
		addListener(numberView, R.id.button9, "9");
		addListener(numberView, R.id.button0, "0");

		findViewById(R.id.okButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtras(new Bundle(1));
				intent.putExtra(HOUSENUMBER, numberView.getText().toString());
				setResult(RESULT_OK, intent);
				finish();
			}
		});

	}

	private void addListener(final TextView numberView, final int button, final String number) {
		findViewById(button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				numberView.setText(numberView.getText() + number);
			}
		});

	}

}
