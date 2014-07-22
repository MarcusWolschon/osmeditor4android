package de.blau.android;



import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;


import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import us.feras.mdv.MarkdownView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.prefs.PrefEditor;


public class HelpViewer extends SherlockActivity {
	
	MarkdownView markdownView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(R.style.Theme_customMain);
		ActionBar actionbar = getSupportActionBar();
		if (actionbar != null) {
			ColorDrawable c = new ColorDrawable(Application.mainActivity.getResources().getColor(R.color.actionbar_bg));
			actionbar.setBackgroundDrawable(c);
			actionbar.setSplitBackgroundDrawable(c);
			actionbar.setStackedBackgroundDrawable(c); // this probably isn't ever necessary
			actionbar.setDisplayShowHomeEnabled(true);
			actionbar.setTitle(Application.mainActivity.getResources().getString(R.string.menu_help));
			actionbar.setDisplayShowTitleEnabled(true);
			actionbar.show();

		} else {
			Log.d("HelpViewer", "No actionbar");
		}
		markdownView = new MarkdownView(this);
		setContentView(markdownView);
		markdownView.loadMarkdownFile("file:///android_asset/help/intro.md");
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
			if (markdownView.canGoBack()) {
				markdownView.goBack();
			}
			return true;
			
		case R.id.help_menu_forward:
			if (markdownView.canGoForward()) {
				markdownView.goForward();
			}
			return true;
		}
		return false;
	}
	

}
