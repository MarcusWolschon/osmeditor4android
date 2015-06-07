package de.blau.android;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;


import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
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
import de.blau.android.util.ThemeUtils;


/**
 * Minimal system for viewing help files
 * Currently only html format is supported directly
 * @author simon
 *
 */
public class HelpViewer extends SherlockActivity {
	
	class HelpItem implements Comparable<HelpItem> {
		String language;
		int order;
		String topic;
		
		@Override
		public int compareTo(HelpItem another) {
			if (order > another.order) {
				return +1;
			} else if (order < another.order) {
				return -1;
			}
			return 0;
		}
		
		@Override
		public String toString() {
			return topic + " (" + language + ")";
		}
	}
	
	
	public static final String TOPIC = "topic";
	WebView helpView;
	
	// drawer that will be our ToC
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	ArrayAdapter<HelpItem> tocAdapter;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_customHelpViewer_Light);
		}
		
		super.onCreate(savedInstanceState);
		String topic = (String)getIntent().getSerializableExtra(TOPIC);

		ActionBar actionbar = getSupportActionBar();
		if (actionbar == null) {
			Log.d("HelpViewer", "No actionbar"); // fail?
			return;
		}
		actionbar.setDisplayShowHomeEnabled(true);
		actionbar.setTitle(getString(R.string.menu_help) + ": " + topic);
		actionbar.setDisplayShowTitleEnabled(true);
		actionbar.show();

		setContentView(R.layout.help_drawer);
		
		// add our content
		FrameLayout fl =  (FrameLayout) findViewById(R.id.content_frame);
		helpView = new WebView(this);
		WebSettings helpSettings = helpView.getSettings();
		helpSettings.setDefaultFontSize(12);
		helpSettings.setSupportZoom(true);
		helpSettings.setBuiltInZoomControls(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			helpSettings.setDisplayZoomControls(false); // don't display +-
		}
		fl.addView(helpView);
		
		// set up the drawer
		mDrawerLayout = (DrawerLayout) findViewById(R.id.help_drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.help_left_drawer);
		
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayHomeAsUpEnabled(true);
		ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, ThemeUtils.getResIdFromAttribute(this,R.attr.drawer), R.string.okay, R.string.okay);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		try {
			HashMap <String,HelpItem> tocList = new HashMap<String,HelpItem>();
			String[] languages = {Locale.getDefault().toString(),Locale.getDefault().getLanguage(),"en"};
			for (String l:languages) { 		
				for (String s:getResources().getAssets().list("help/"+l)) {
					if (s.equals("") || Character.isLowerCase(s.charAt(0)) || s.length() < 6) {
						continue; // skip everything that isn't a help file
					}
					String n = s.replace(".html", "");
					HelpItem h = new HelpItem();
					h.language = l;
					if (Character.isDigit(n.charAt(0))) {
						String[] s1 = n.split(" ",2);
						if (s1.length != 2) {
							continue;
						}
						h.topic = s1[1];
						h.order = Integer.parseInt(s1[0]);
					} else {
						h.topic = n;
						h.order = 999;
						if (!tocList.containsKey(h.topic)) {
							tocList.put(h.topic,h);
						}
					}
				}	
			}
			
			HelpItem[] toc = new HelpItem[tocList.size()];
			tocList.values().toArray(toc);
			
			tocAdapter = new ArrayAdapter<HelpItem>(this, R.layout.help_drawer_item,R.id.help_drawer_item, toc);
			
			mDrawerList.setAdapter(tocAdapter);
			mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

			
			String helpFile = "help/" + Locale.getDefault() + "/"  + topic + ".html";
			Log.d("HelpViewer","1 Looking for help file: " + helpFile);
			if (!Arrays.asList(getResources().getAssets().list("help/" + Locale.getDefault())).contains(topic + ".html")) {
				helpFile = "help/" + Locale.getDefault().getLanguage() + "/"  + topic + ".html";
				Log.d("HelpViewer","2 Looking for help file: " + helpFile);
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
			HelpItem helpItem = tocAdapter.getItem(position);
			helpView.loadUrl("file:///android_asset/help/" + helpItem.language + "/" + (helpItem.order != 999 ? helpItem.order + " " : "") + helpItem.topic +".html");
			mDrawerLayout.closeDrawer(mDrawerList);
			mDrawerList.setSelected(false);
			getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + helpItem.topic);
		}
	}
}
