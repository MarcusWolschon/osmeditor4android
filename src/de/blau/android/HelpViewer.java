package de.blau.android;



import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.TypedArray;
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
		boolean displayLanguage = false;
		String language;
		int order;
		String topic;
		
		@Override
		public int compareTo(HelpItem another) {
			if (order < Integer.MAX_VALUE) {
				if (order > another.order) {
					return 1;
				} else if (order < another.order) {
					return -1;
				}
			}
			return topic.compareTo(another.topic); // sort the rest alphabetically
		}
		
		@Override
		public String toString() {
			return topic + (displayLanguage ? " (" + language + ")": "");
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
		int topicId = (Integer)getIntent().getSerializableExtra(TOPIC);
		String topic = getString(topicId); // this assumes that the resources are the same, which is probably safe

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
		helpView.setWebViewClient(new HelpViewWebViewClient());
		fl.addView(helpView);
		
		// set up the drawer
		mDrawerLayout = (DrawerLayout) findViewById(R.id.help_drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.help_left_drawer);
		
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayHomeAsUpEnabled(true);
		ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, ThemeUtils.getResIdFromAttribute(this,R.attr.drawer), R.string.okay, R.string.okay);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		

		
		try {
			List<String> defaultList = Arrays.asList(getResources().getAssets().list("help/" + Locale.getDefault().getLanguage()));
			List<String> enList = Arrays.asList(getResources().getAssets().list("help/en"));
			String defaultLanguage = Locale.getDefault().getLanguage();
			
			TypedArray tocRes = getResources().obtainTypedArray(R.array.help_tableofcontents);
			
			HashMap <String,HelpItem> tocList = new HashMap<String,HelpItem>();
					
			for (int i=0;i<tocRes.length();i++) {
				String tocTopic = tocRes.getString(i);
				// Log.d("HelpViewer", "TOC " + tocTopic); 
				if (defaultList.contains(tocTopic + ".html")) {
					// Log.d("HelpViewer", "TOC " + locale + " " + tocTopic); 
					HelpItem h = new HelpItem();
					h.language = defaultLanguage;
					h.topic = tocTopic;
					h.order = i;	
					if (!tocList.containsKey(h.topic)) {
						tocList.put(h.topic,h);
					}
				} else if (enList.contains(tocTopic + ".html")){
					// Log.d("HelpViewer", "TOC en " + tocTopic);
					HelpItem h = new HelpItem();
					h.language = "en";
					h.displayLanguage = true;
					h.topic = tocTopic;
					h.order = i;	
					if (!tocList.containsKey(h.topic)) {
						tocList.put(h.topic,h);
					}
				}
			}
			tocRes.recycle();
			
			List<HelpItem> items = new ArrayList<HelpItem>(tocList.values());
			Collections.sort(items);
			HelpItem[] toc = new HelpItem[items.size()];
			items.toArray(toc);
			
			tocAdapter = new ArrayAdapter<HelpItem>(this, R.layout.help_drawer_item,R.id.help_drawer_item, toc);
			
			mDrawerList.setAdapter(tocAdapter);
			mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

			String helpFile = "help/" + Locale.getDefault().getLanguage() + "/"  + topic + ".html";
			Log.d("HelpViewer","1 Looking for help file: " + helpFile);
			if (!defaultList.contains(topic + ".html")) {
				helpFile = "help/en/"  + topic + ".html";
				if (!enList.contains(topic + ".html")) {
					helpFile = "help/en/no_help.html";
					mDrawerLayout.openDrawer(mDrawerList);
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
				// getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + getTopic(helpView.getUrl()));
			} else {
				onBackPressed(); // return to caller
			}
			return true;
			
		case R.id.help_menu_forward:
			if (helpView.canGoForward()) {
				helpView.goForward();
				// getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + getTopic(helpView.getUrl()));
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
			helpView.loadUrl("file:///android_asset/help/" + helpItem.language + "/" + helpItem.topic +".html");
			mDrawerLayout.closeDrawer(mDrawerList);
			mDrawerList.setSelected(false);
			getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + helpItem.topic);
		}
	}
	
	private class HelpViewWebViewClient extends WebViewClient {
		
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	if (url.startsWith("file:")) {
	    		Log.d("HelpViewer","orig " + url);
	    		getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + getTopic(url));
	    		if (url.endsWith(".md")) { // on device we have pre-generated html
	    			url = url.substring(0,url.length()-".md".length()) + ".html";
	    			Log.d("HelpViewer","new " + url);
	    		}
	    		view.loadUrl(url);
	    		return true;
	    	} else {
	    		return false;
	    	}
	    }
	    
	    @Override
	    public void onPageFinished (WebView view, String url) {
	    	super.onPageFinished(view, url);
	    	getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + getTopic(url));
	    }
	}
	
	private String getTopic(String url) {
		
		url = URLDecoder.decode(url);
		int lastSlash = url.lastIndexOf('/');
		int lastDot = url.lastIndexOf('.');
		if (lastSlash < 0 || lastDot < 0) {
			return "Error, got: " + url;
		}
		String topic = url.substring(lastSlash+1,lastDot); 
		return topic;
	}
}
