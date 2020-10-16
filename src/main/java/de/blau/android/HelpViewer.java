package de.blau.android;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.osm.OsmXml;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Minimal system for viewing help files Currently only html format is supported directly
 * 
 * @author Simon Poole
 *
 */
public class HelpViewer extends AppCompatActivity {

    private static final String HTML_SUFFIX = ".html";
    private static final String DEBUG_TAG   = HelpViewer.class.getName();

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
    private DrawerLayout               mDrawerLayout;
    private ListView                   mDrawerList;
    private HighlightAdapter<HelpItem> tocAdapter;
    private boolean                    rtl      = false;
    private int                        selected = 0;

    private List<String> defaultList;
    private List<String> enList;
    private HelpItem[]   toc;

    private int unselectedItemBackground;
    private int selectedItemBackground;

    /**
     * Start this Activity
     * 
     * @param activity calling activity
     * @param topic string resource id of the help topic
     */
    public static void start(@NonNull FragmentActivity activity, @StringRes int topic) {
        if (!Util.supportsWebView(activity)) {
            ErrorAlert.showDialog(activity, ErrorCodes.REQUIRED_FEATURE_MISSING, "WebView");
            return;
        }
        Intent intent = new Intent(activity, HelpViewer.class);
        intent.putExtra(TOPIC, topic);
        activity.startActivity(intent);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customHelpViewer_Light);
        }
        unselectedItemBackground = ThemeUtils.getStyleAttribColorValue(this, R.attr.unselected_item_background, R.color.light_grey);
        selectedItemBackground = ThemeUtils.getStyleAttribColorValue(this, R.attr.selected_item_background, R.color.dark_grey);
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

        ActionBar actionbar = getSupportActionBar();
        if (actionbar == null) {
            Log.d(DEBUG_TAG, "No actionbar"); // fail?
            return;
        }
        actionbar.setDisplayShowHomeEnabled(true);
        actionbar.setTitle(getString(R.string.help_title, topic));
        actionbar.setDisplayShowTitleEnabled(true);
        actionbar.show();

        // add our content
        FrameLayout fl = (FrameLayout) findViewById(R.id.content_frame);
        helpView = new WebView(this);
        WebSettings helpSettings = helpView.getSettings();
        helpSettings.setDefaultFontSize(12);
        helpSettings.setSupportZoom(true);
        helpSettings.setBuiltInZoomControls(true);
        helpSettings.setDisplayZoomControls(false); // don't display +-
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
            defaultList = Arrays.asList(getResources().getAssets().list("help/" + Locale.getDefault().getLanguage()));
            enList = Arrays.asList(getResources().getAssets().list("help/en"));
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
            Collections.sort(items, (one, two) -> {
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
            });
            toc = new HelpItem[items.size()];
            items.toArray(toc);

            String helpFile = getHelpFile(topic, defaultList, enList, toc);

            tocAdapter = new HighlightAdapter<>(this, R.layout.help_drawer_item, R.id.help_drawer_item, toc);

            mDrawerList.setAdapter(tocAdapter);
            mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
            mDrawerList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

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
     * Get the actual HTML help file
     * 
     * @param topic the topic
     * @param defaultList list of files in the default language
     * @param enList list of files in English
     * @param toc the table of contents as a array of HelpItems
     * @return the path to the help file
     */
    String getHelpFile(@NonNull String topic, @NonNull List<String> defaultList, @NonNull List<String> enList, @NonNull HelpItem[] toc) {
        String topicFile = "no_help";
        HelpItem tempTopic = tocList.get(topic);
        if (tempTopic != null) {
            String tempTopicFile = tempTopic.fileName;
            if (tempTopicFile != null) {
                topicFile = tempTopicFile;
            }
        }

        String topicFileHtml = topicFile + HTML_SUFFIX;
        String helpFile = "help/" + Locale.getDefault().getLanguage() + "/" + topicFileHtml;
        Log.d(DEBUG_TAG, "1 Looking for help file: " + helpFile);
        if (!defaultList.contains(topicFileHtml)) {
            helpFile = "help/en/" + topicFileHtml;
            if (!enList.contains(topicFileHtml)) {
                helpFile = "help/en/no_help.html";
                mDrawerLayout.openDrawer(mDrawerList);
            }
        } else {
            for (int i = 0; i < toc.length; i++) {
                if (toc[i].equals(tempTopic)) {
                    selected = i;
                    break;
                }
            }
        }
        return helpFile;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Serializable s = intent.getSerializableExtra(TOPIC);
        int topicId = R.string.help_introduction;
        if (s != null) {
            try {
                topicId = (Integer) s;
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "casting topic raised " + e);
            }
        } else {
            Log.d(DEBUG_TAG, "Falling back to default topic");
        }
        helpView.loadUrl("file:///android_asset/" + getHelpFile(getString(topicId), defaultList, enList, toc));
        tocAdapter.notifyDataSetChanged();
    }

    class HighlightAdapter<T> extends ArrayAdapter<T> {

        /**
         * Construct a new adapter
         * 
         * @param context The current context.
         * @param resource The resource ID for a layout file containing a layout to use when instantiating views
         * @param textViewResourceId The id of the TextView within the layout resource to be populated
         * @param objects The objects to represent in the ListView.
         */
        public HighlightAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (position == selected) {
                view.setBackgroundColor(selectedItemBackground);
            } else {
                view.setBackgroundColor(unselectedItemBackground);
            }
            return view;
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
                item.getIcon().mutate().setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.GRAY, BlendModeCompat.SRC_IN));
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
        Util.clearCaches(this, newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        helpView.saveState(outState);
    }

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            HelpItem helpItem = tocAdapter.getItem(position);
            helpView.loadUrl("file:///android_asset/help/" + helpItem.language + "/" + helpItem.fileName + HTML_SUFFIX);
            mDrawerLayout.closeDrawer(mDrawerList);
            mDrawerList.setSelected(false);
            setTitle(helpItem.topic);
            mDrawerList.setSelection(position);
            selected = position;
            tocAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Set the action bar title
     * 
     * @param topic the topic
     */
    private void setTitle(@NonNull String topic) {
        getSupportActionBar().setTitle(getString(R.string.help_title, topic));
    }

    private class HelpViewWebViewClient extends WebViewClient {

        /**
         * @deprecated since API 24
         */
        @Deprecated
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // WebViewClient is slightly bizarre because there is no way to indicate to the webview that you would like
            // if to process the url in its default way, its either handling it yourself or loading it directly into the
            // webview
            if (url != null && url.startsWith(FileUtil.FILE_SCHEME_PREFIX)) {
                Log.d(DEBUG_TAG, "orig " + url);
                setTitle(getTopic(url));
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
                setTitle(getTopic(url));
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
            for (Entry<String, HelpItem> entry : tocList.entrySet()) { // could use a HashMap here but probably not
                                                                       // worth it
                if (fileName.equals(entry.getValue().fileName)) {
                    return entry.getKey();
                }
            }
            return "";
        }
    }
}
