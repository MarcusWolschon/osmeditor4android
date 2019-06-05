package de.blau.android;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import de.blau.android.osm.OsmXml;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.BugFixedAppCompatActivity;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ThemeUtils;

/**
 * Minimal system for viewing help files Currently only html format is supported directly
 * 
 * @author Simon Poole
 *
 */
public class HelpViewer extends BugFixedAppCompatActivity {

    private static final String HTML_SUFFIX = ".html";
    private static String       DEBUG_TAG   = HelpViewer.class.getName();

    class HelpItem {
        boolean displayLanguage = false;
        String  language;
        int     order;
        String  topic;
        String  fileName;

        @Override
        public String toString() {
            return topic + (displayLanguage ? " (" + language + ")" : "");
        }
    }

    private static final String       TOPIC   = "topic";
    private WebView                   helpView;
    private HashMap<String, HelpItem> tocList = new HashMap<>();

    private ActionBarDrawerToggle mDrawerToggle;
    // drawer that will be our ToC
    private DrawerLayout           mDrawerLayout;
    private ListView               mDrawerList;
    private ArrayAdapter<HelpItem> tocAdapter;
    private boolean                rtl = false;

    /**
     * Start this Activity
     * 
     * @param context Android Context
     * @param topic string resource id of the help topic
     */
    public static void start(@NonNull Context context, @StringRes int topic) {
        Intent intent = new Intent(context, HelpViewer.class);
        intent.putExtra(TOPIC, topic);
        context.startActivity(intent);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customHelpViewer_Light);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Configuration config = getResources().getConfiguration();
            rtl = config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        }
        super.onCreate(savedInstanceState);
        int topicId = R.string.help_introduction;
        Serializable s = getIntent().getSerializableExtra(TOPIC);
        if (s != null) {
            try {
                topicId = (Integer) s;
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "casting topic raised " + e);
            }
        } else {
            Log.d(DEBUG_TAG, "Falling back to default topic");
        }
        String topic = getString(topicId); // this assumes that the resources are the same, which is probably safe

        setContentView(R.layout.help_drawer);

        // // Find the toolbar view inside the activity layout
        // Toolbar toolbar = (Toolbar) findViewById(R.id.helpToolbar);
        // // Sets the Toolbar to act as the ActionBar for this Activity window.
        // // Make sure the toolbar exists in the activity and is not null
        // setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        if (actionbar == null) {
            Log.d(DEBUG_TAG, "No actionbar"); // fail?
            return;
        }
        actionbar.setDisplayShowHomeEnabled(true);
        actionbar.setTitle(getString(R.string.menu_help) + ": " + topic);
        actionbar.setDisplayShowTitleEnabled(true);
        actionbar.show();

        // add our content
        FrameLayout fl = (FrameLayout) findViewById(R.id.content_frame);
        helpView = new WebView(this);
        WebSettings helpSettings = helpView.getSettings();
        helpSettings.setDefaultFontSize(12);
        helpSettings.setSupportZoom(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            helpSettings.setDisplayZoomControls(false); // don't display +-
        } else {
            helpSettings.setBuiltInZoomControls(true);
        }
        helpView.setWebViewClient(new HelpViewWebViewClient());
        fl.addView(helpView);

        // set up the drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.help_drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.help_left_drawer);

        actionbar.setHomeButtonEnabled(true);
        actionbar.setDisplayHomeAsUpEnabled(true);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.okay, R.string.okay);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        try {
            List<String> defaultList = Arrays.asList(getResources().getAssets().list("help/" + Locale.getDefault().getLanguage()));
            List<String> enList = Arrays.asList(getResources().getAssets().list("help/en"));
            String defaultLanguage = Locale.getDefault().getLanguage();

            TypedArray tocRes = getResources().obtainTypedArray(R.array.help_tableofcontents);
            TypedArray fileRes = getResources().obtainTypedArray(R.array.help_files);

            for (int i = 0; i < tocRes.length(); i++) {
                String fileName = fileRes.getString(i);
                // Log.d("HelpViewer", "TOC " + tocTopic);
                if (defaultList.contains(fileName + HTML_SUFFIX)) {
                    // Log.d("HelpViewer", "TOC " + locale + " " + tocTopic);
                    HelpItem h = new HelpItem();
                    h.language = defaultLanguage;
                    h.topic = tocRes.getString(i);
                    h.order = i;
                    h.fileName = fileName;
                    if (!tocList.containsKey(h.topic)) {
                        tocList.put(h.topic, h);
                    }
                } else if (enList.contains(fileName + HTML_SUFFIX)) {
                    // Log.d("HelpViewer", "TOC en " + tocTopic);
                    HelpItem h = new HelpItem();
                    h.language = "en";
                    h.displayLanguage = true;
                    h.topic = tocRes.getString(i);
                    h.order = i;
                    h.fileName = fileName;
                    if (!tocList.containsKey(h.topic)) {
                        tocList.put(h.topic, h);
                    }
                }
            }
            tocRes.recycle();
            fileRes.recycle();

            List<HelpItem> items = new ArrayList<>(tocList.values());
            Collections.sort(items, new Comparator<HelpItem>() {
                @Override
                public int compare(HelpItem one, HelpItem two) {
                    if (one.order < Integer.MAX_VALUE) {
                        if (one.order > two.order) {
                            return 1;
                        } else if (one.order < two.order) {
                            return -1;
                        }
                    }
                    if (one.topic == null) {
                        if (two.topic == null) {
                            return 0;
                        } else {
                            return -1;
                        }
                    }
                    return one.topic.compareTo(two.topic); // sort the rest alphabetically
                }
            });
            HelpItem[] toc = new HelpItem[items.size()];
            items.toArray(toc);

            tocAdapter = new ArrayAdapter<>(this, R.layout.help_drawer_item, R.id.help_drawer_item, toc);

            mDrawerList.setAdapter(tocAdapter);
            mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

            String topicFile = "no_help";
            HelpItem tempTopic = tocList.get(topic);
            if (tempTopic != null) {
                String tempTopicFile = tempTopic.fileName;
                if (tempTopicFile != null) {
                    topicFile = tempTopicFile;
                }
            }

            String helpFile = "help/" + Locale.getDefault().getLanguage() + "/" + topicFile + HTML_SUFFIX;
            Log.d(DEBUG_TAG, "1 Looking for help file: " + helpFile);
            if (!defaultList.contains(topicFile + HTML_SUFFIX)) {
                helpFile = "help/en/" + topicFile + HTML_SUFFIX;
                if (!enList.contains(topicFile + HTML_SUFFIX)) {
                    helpFile = "help/en/no_help.html";
                    mDrawerLayout.openDrawer(mDrawerList);
                }
            }
            if (savedInstanceState != null) {
                helpView.restoreState(savedInstanceState);
            } else {
                helpView.loadUrl("file:///android_asset/" + helpFile);
            }
        } catch (IOException e) {
            Log.d(DEBUG_TAG, "Caught exception " + e);
        }
    }

    /**
     * Creates the menu from the XML file "main_menu.xml".<br>
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.help_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        MenuItem item = menu.findItem(R.id.help_menu_forward);
        if (item != null) {
            boolean canGoForward = helpView.canGoForward();
            item.setEnabled(canGoForward);
            item.setIcon(ThemeUtils.getResIdFromAttribute(this, rtl ? R.attr.menu_back : R.attr.menu_forward));
            if (!canGoForward) {
                item.getIcon().mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        Log.d(DEBUG_TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
        case R.id.help_menu_back:
            if (helpView.canGoBack()) {
                helpView.goBack();
                invalidateOptionsMenu();
            } else {
                onBackPressed(); // return to caller
            }
            return true;

        case R.id.help_menu_forward:
            if (helpView.canGoForward()) {
                helpView.goForward();
                invalidateOptionsMenu();
            }
            return true;
        default:
            Log.e(DEBUG_TAG, "Unknown menu item " + item.getTitle());
        }
        return false;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        helpView.saveState(outState);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            HelpItem helpItem = tocAdapter.getItem(position);
            helpView.loadUrl("file:///android_asset/help/" + helpItem.language + "/" + helpItem.fileName + HTML_SUFFIX);
            mDrawerLayout.closeDrawer(mDrawerList);
            mDrawerList.setSelected(false);
            getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + helpItem.topic);
        }
    }

    private class HelpViewWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // WebViewClient is slightly bizarre because there is no way to indicate to the webview that you would like
            // if to process the url in its default way, its either handling it yourself or loading it directly into the
            // webview
            if (url != null && url.startsWith(FileUtil.FILE_SCHEME_PREFIX)) {
                Log.d(DEBUG_TAG, "orig " + url);
                getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + getTopic(url));
                if (url.endsWith(".md")) { // on device we have pre-generated html
                    url = url.substring(0, url.length() - ".md".length()) + HTML_SUFFIX;
                    Log.d(DEBUG_TAG, "new " + url);
                }
                view.loadUrl(url);
            } else {
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (url.startsWith(FileUtil.FILE_SCHEME_PREFIX)) {
                getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + getTopic(url));
            }
        }
    }

    /**
     * Get the topic from an url
     * 
     * @param url the url
     * @return a String with the topic if it could be determined
     */
    @NonNull
    private String getTopic(@NonNull String url) {

        try {
            url = URLDecoder.decode(url, OsmXml.UTF_8);
        } catch (UnsupportedEncodingException e) {
            return "Error, got: " + url;
        }
        int lastSlash = url.lastIndexOf('/');
        int lastDot = url.lastIndexOf('.');
        if (lastSlash < 0 || lastDot < 0) {
            return "Error, got: " + url;
        }
        String fileName = url.substring(lastSlash + 1, lastDot);
        for (Entry<String, HelpItem> entry : tocList.entrySet()) { // could use a HashMap here but probably not worth it
            if (fileName.equals(entry.getValue().fileName)) {
                return entry.getKey();
            }
        }
        return "";
    }
}
