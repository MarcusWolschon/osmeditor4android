package io.vespucci.osm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.contract.Schemes;
import io.vespucci.prefs.Preferences;
import io.vespucci.presets.PresetItem;
import io.vespucci.util.ExecutorTask;

public final class Wiki {
    private static final String DEBUG_TAG = Wiki.class.getSimpleName().substring(0, Math.min(23, Wiki.class.getSimpleName().length()));

    private static final String API_ATTR_MISSING = "missing";
    private static final String API_ELEMENT_PAGE = "page";

    private static final String MAP_FEATURES = "Map_Features";
    private static final String DOUBLE_COLON = ":";

    /**
     * Private default constructor
     */
    private Wiki() {
        // stop class from being instantiated
    }

    /**
     * Check if a specific language - page combination exists on the OSM wiki
     * 
     * @param context an Android Context
     * @param baseUrl the base url for the wiki
     * @param parser an XmlPullParser instance
     * @param page the page name
     * @param language the language
     * @return true if the page could be found
     * @throws IOException if anything goes wrong
     */
    private static boolean checkLanguage(@NonNull final Context context, @NonNull String baseUrl, @NonNull final XmlPullParser parser, @NonNull String page,
            @NonNull String language) throws IOException {
        // https://wiki.openstreetmap.org/w/api.php?action=query&titles=DE:Use_OpenStreetMap&prop=info&format=xml
        // see https://wiki.openstreetmap.org/w/api.php
        String wikiApiUrl = baseUrl + "w/api.php?action=query&prop=info&format=xml&titles=" + language.toUpperCase(Locale.US) + DOUBLE_COLON + page;
        try (InputStream is = Server.openConnection(context, new URL(wikiApiUrl))) {
            parser.setInput(is, null);
            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG && API_ELEMENT_PAGE.equals(tagName) && parser.getAttributeValue(null, API_ATTR_MISSING) != null) {
                    Log.d(DEBUG_TAG, "displayMapFeatures " + language + DOUBLE_COLON + page + " doesn't exist");
                    return false;
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
     * Display the map features wiki page for a PresetItem
     * 
     * If it finds a valid url it will cache it in the PresetItem
     * 
     * @param context the calling Android Activity
     * @param prefs a Preferences instance
     * @param p the PresetItem
     */
    @NonNull
    public static void displayMapFeatures(@NonNull final Context context, @NonNull final Preferences prefs, @Nullable final PresetItem p) {
        String url = null;
        if (p != null) {
            url = p.getMapFeatures();
            if (url != null && !url.startsWith(Schemes.HTTP)) {
                // build full url from wiki page name, locale or language and check if it exists
                url = getUrl(context, prefs, url);
                if (url != null) {
                    p.setMapFeatures(url); // cache full url
                }
            }
        }
        if (url == null) {
            url = getUrl(context, prefs, MAP_FEATURES);
        }
        if (url != null) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ex) {
                Log.d(DEBUG_TAG, url + " is a invalid map feature url");
            }
        }
    }

    /**
     * Retrieve a valid url for a map features page
     * 
     * @param context an Android Context
     * @param prefs a Preference instance
     * @param path path for the page
     * @return an url, if no page found this will fallback to the EN map features page
     */
    private static String getUrl(@NonNull final Context context, @NonNull final Preferences prefs, @NonNull String path) {
        final Logic logic = App.getLogic();
        final Locale locale = Locale.getDefault();
        final Server server = prefs.getServer();
        final String baseUrl = prefs.getOsmWiki();
        ExecutorTask<String, Void, String> task = new ExecutorTask<String, Void, String>(logic.getExecutorService(), logic.getHandler()) {
            @Override
            protected String doInBackground(String path) {
                try {
                    String l = mapLocale(locale);
                    if (checkLanguage(context, baseUrl, server.getXmlParser(), path, l)) {
                        return toWikiUrl(baseUrl, path, l);
                    } else {
                        l = locale.getLanguage().toUpperCase(Locale.US);
                        if (checkLanguage(context, baseUrl, server.getXmlParser(), path, l)) {
                            return toWikiUrl(baseUrl, path, l);
                        }
                        return toWikiUrl(baseUrl, path, null);
                    }
                } catch (IOException | XmlPullParserException e) {
                    Log.e(DEBUG_TAG, "getLangUrl " + e.getMessage());
                }
                return null;
            }
        };

        task.execute(path);
        try {
            path = task.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            Log.w(DEBUG_TAG, "Checking wiki url failed");
        }
        return path;
    }

    /**
     * Create a wiki url
     * 
     * @param baseUrl the base url for the wiki
     * @param path the path
     * @param lang optional language string
     * @return the full wiki Url
     */
    @NonNull
    private static String toWikiUrl(@NonNull String baseUrl, @NonNull String path, @Nullable String lang) {
        return baseUrl + (lang != null ? lang + DOUBLE_COLON : "") + path;
    }
}
