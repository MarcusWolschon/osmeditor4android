package de.blau.android;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;


import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.prefs.Preferences;


/**
 * Minimal system for viewing help files
 * Currently only html format is supported directly
 * @author simon
 *
 */
public class HelpViewer extends SherlockActivity {
	
	public static final String TOPIC = "topic";
	WebView helpView;
	
	// drawer that will be our ToC
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	ArrayAdapter<String> tocAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_customHelpViewer_Light);
		}
		
		super.onCreate(savedInstanceState);
		String topic = (String)getIntent().getSerializableExtra(TOPIC);
		setTheme(R.style.Theme_customHelpViewer);
		ActionBar actionbar = getSupportActionBar();
		if (actionbar == null) {
			Log.d("HelpViewer", "No actionbar"); // fail?
		}
		ColorDrawable c = new ColorDrawable(getResources().getColor(R.color.actionbar_bg));
		actionbar.setBackgroundDrawable(c);
		actionbar.setSplitBackgroundDrawable(c);
		actionbar.setStackedBackgroundDrawable(c); // this probably isn't ever necessary
		actionbar.setDisplayShowHomeEnabled(true);
		actionbar.setTitle(getString(R.string.menu_help) + ": " + topic);
		actionbar.setDisplayShowTitleEnabled(true);
		actionbar.show();

		setContentView(R.layout.help_drawer);
		
		// add our real content
		FrameLayout fl =  (FrameLayout) findViewById(R.id.content_frame);
		helpView = new WebView(this);
		fl.addView(helpView);
		
		// set up the drawer
		mDrawerLayout = (DrawerLayout) findViewById(R.id.help_drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.help_left_drawer);
		
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayHomeAsUpEnabled(true);
		ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.okay, R.string.okay);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		try {
			ArrayList<String> tocList = new ArrayList<String>();
			String[] languages = {Locale.getDefault().toString(),Locale.getDefault().getLanguage(),"en"};
			for (String l:languages) { 		
				for (String s:getResources().getAssets().list("help/"+l)) {
					if (s.equals("") || !Character.isUpperCase(s.charAt(0))) {
						continue;
					}
					String n = s.replace(".html", "");
					if (!tocList.contains(n)) {
						tocList.add(n);
					}
				}	
			}
			
			String[] toc = new String[tocList.size()];
			tocList.toArray(toc);
			
			tocAdapter = new ArrayAdapter<String>(this, R.layout.help_drawer_item,R.id.help_drawer_item, toc);
			
			mDrawerList.setAdapter(tocAdapter);
			mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

			
			String helpFile = "help/" + Locale.getDefault() + "/"  + topic + ".html";
			Log.d("HelpViwer","1 Looking for help file: " + helpFile);
			if (!Arrays.asList(getResources().getAssets().list("help/" + Locale.getDefault())).contains(topic + ".html")) {
				helpFile = "help/" + Locale.getDefault().getLanguage() + "/"  + topic + ".html";
				Log.d("HelpViwer","2 Looking for help file: " + helpFile);
				if (!Arrays.asList(getResources().getAssets().list("help/" + Locale.getDefault().getLanguage())).contains(topic + ".html")) {
					helpFile = "help/en/"  + topic + ".html";
					if (!Arrays.asList(getResources().getAssets().list("help/en")).contains(topic + ".html")) {
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
		case android.R.id.home:
			if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
				mDrawerLayout.closeDrawer(mDrawerList);
			} else {
				mDrawerLayout.openDrawer(mDrawerList);
			}
			return true;
		}
		return false;
	}
	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			String topic = tocAdapter.getItem(position);
			helpView.loadUrl("file:///android_asset/help/en/" + topic +".html");
			mDrawerLayout.closeDrawer(mDrawerList);
			mDrawerList.setSelected(false);
			getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + topic);
		}

	}
}
