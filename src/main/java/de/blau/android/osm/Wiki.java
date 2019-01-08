package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset.PresetItem;

public final class Wiki {

    private static final String DEBUG_TAG = "Wiki";

    /**
     * Private default constructor
     */
    private Wiki() {
        // stop class from being instantiated
    }

    /**
     * Check if a specific language - page combination exists on the OSM wiki
     * 
     * @param activity the calling Activity
     * @param prefs a Preferences object
     * @param page the page name
     * @param language the language
     * @return true if the page could be found
     * @throws IOException if ynthing goes wrong
     */
    private static boolean checkLanguage(@NonNull final Activity activity, @NonNull final Preferences prefs, @NonNull String page, @NonNull String language)
            throws IOException {
        // https://wiki.openstreetmap.org/w/api.php?action=query&titles=DE:Use_OpenStreetMap&prop=info&format=xml
        // see https://wiki.openstreetmap.org/w/api.php
        String wikiApiUrl = Urls.OSM_WIKI + "w/api.php?action=query&prop=info&format=xml&titles=" + language.toUpperCase(Locale.US) + ":" + page;
        try (InputStream is = Server.openConnection(activity, new URL(wikiApiUrl))) {
            XmlPullParser parser = prefs.getServer().getXmlParser();
            parser.setInput(is, null);
            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG && "page".equals(tagName)) {
                    if (parser.getAttributeValue(null, "missing") != null) {
                        Log.d(DEBUG_TAG, "displayMapFeatures " + language + ":" + page + " doesn't exist");
                        return false;
                    }
                }
            }
        } catch (final IOException | XmlPullParserException iox) {
            Log.e(DEBUG_TAG, "Parse wiki api " + iox.getMessage());
            throw new IOException(iox.getMessage());
        }
        return true;
    }

    /**
     * Try to map locales to language strings mediawiki expects
     * 
     * @param locale the Locale
     * @return a language string
     */
    private static String mapLocale(@NonNull Locale locale) {
        String l = locale.toString();
        int hash = l.indexOf("_#");
        if (hash >= 0) {
            l = l.substring(0, hash);
        }
        l = l.replace('_', '-');
        return l.toUpperCase(Locale.US);
    }

    /**
     * Display the mapfeatures wiki page for a PresetItem
     * 
     * @param activity the calling Android Activity
     * @param prefs a Preferences instance
     * @param p the PresetItem
     */
    @NonNull
    public static void displayMapFeatures(@NonNull final Activity activity, @NonNull final Preferences prefs, @Nullable final PresetItem p) {
        Uri uri = null;
        if (p != null) {
            String url = p.getMapFeatures();
            if (url != null) {
                if (!url.startsWith("http")) { // build full url from wiki page name, locale or language and check if it
                                               // exists
                    new AsyncTask<Void, Void, String>() {
                        @Override
                        protected String doInBackground(Void... params) {
                            try {
                                Locale locale = Locale.getDefault();
                                String l = mapLocale(locale);
                                if (checkLanguage(activity, prefs, url, l)) {
                                    return Urls.OSM_WIKI + l + ":" + url;
                                } else {
                                    l = locale.getLanguage().toUpperCase(Locale.US);
                                    if (checkLanguage(activity, prefs, url, l)) {
                                        return Urls.OSM_WIKI + l + ":" + url;
                                    } else {
                                        return Urls.OSM_WIKI + url;
                                    }
                                }
                            } catch (IOException e) {
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(String wikiUrl) {
                            Uri uri;
                            if (wikiUrl == null) {
                                uri = Uri.parse(activity.getString(R.string.link_mapfeatures));
                            } else {
                                p.setMapFeatures(wikiUrl); // only check once if we have a result
                                uri = Uri.parse(wikiUrl);
                            }
                            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        }
                    }.execute();
                    return;
                }
                try {
                    uri = Uri.parse(url);
                } catch (Exception ex) {
                    Log.d(DEBUG_TAG, "Preset " + p.getName() + " has no/invalid map feature uri");
                }
            }
        }
        if (uri == null) {
            uri = Uri.parse(activity.getString(R.string.link_mapfeatures));
        }
        activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }
}
