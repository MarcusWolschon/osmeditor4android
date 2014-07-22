package de.blau.android;



import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;


import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;


/**
 * Minimal system for viewing help files
 * Currently only html format is supported directly
 * @author simon
 *
 */
public class HelpViewer extends SherlockActivity {
	
	public static final String TOPIC = "topic";
	WebView helpView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String topic = (String)getIntent().getSerializableExtra(TOPIC);
		setTheme(R.style.Theme_customMain);
		ActionBar actionbar = getSupportActionBar();
		if (actionbar != null) {
			ColorDrawable c = new ColorDrawable(Application.mainActivity.getResources().getColor(R.color.actionbar_bg));
			actionbar.setBackgroundDrawable(c);
			actionbar.setSplitBackgroundDrawable(c);
			actionbar.setStackedBackgroundDrawable(c); // this probably isn't ever necessary
			actionbar.setDisplayShowHomeEnabled(true);
			actionbar.setTitle(getString(R.string.menu_help) + ": " + topic);
			actionbar.setDisplayShowTitleEnabled(true);
			actionbar.show();

		} else {
			Log.d("HelpViewer", "No actionbar");
		}
		helpView = new WebView(this);
		setContentView(helpView);
		
		String helpFile = "help/" + Locale.getDefault() + "/"  + topic + ".html";
		try {
			if (!Arrays.asList(getResources().getAssets().list("")).contains("helpFile")) {
				helpFile = "help/" + Locale.getDefault().getLanguage() + "/"  + topic + ".html";
				if (!Arrays.asList(getResources().getAssets().list("")).contains("helpFile")) {
					helpFile = "help/en/"  + topic + ".html";
					if (!Arrays.asList(getResources().getAssets().list("")).contains("helpFile")) {
						helpFile = "help/en/no_help.html";
					}
				} 
			}
			helpView.loadUrl("file:///android_asset/" + helpFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates the menu from the XML file "main_menu.xml".<br> {@inheritDoc}
	 */
 	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.help_menu, menu);
		return true;
 	}
 	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		Log.d("Main", "onOptionsItemSelected");
		switch (item.getItemId()) {
		case R.id.help_menu_back:
			if (helpView.canGoBack()) {
				helpView.goBack();
			} else {
				onBackPressed(); // return to caller
			}
			return true;
			
		case R.id.help_menu_forward:
			if (helpView.canGoForward()) {
				helpView.goForward();
			}
			return true;
		}
		return false;
	}
	

}
