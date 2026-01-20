package de.blau.android.resources;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.poparser.Po;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Paths;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StyleableFeature;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.StyleConfiguration;
import de.blau.android.presets.Preset;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.Util;
import de.blau.android.util.XmlFile;
import de.blau.android.util.collections.MultiHashMap;

/**
 * Manage the datastyles (in memory)
 */
public class DataStyleManager {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, DataStyleManager.class.getSimpleName().length());
    private static final String DEBUG_TAG = DataStyleManager.class.getSimpleName().substring(0, TAG_LEN);

    private static final String I18N_DATASTYLE = "i18n/datastyle_";

    private static final String BUILTIN_STYLE_NAME = "Built-in (minimal)";
    private static final String BUILTIN_STYLE_ID   = "builtin-minimal";

    private DataStyle                  currentStyle;
    private HashMap<String, DataStyle> availableStyles = new HashMap<>();
    private Po                         po;

    /**
     * Loop over all styles and apply processor
     * 
     * @param processor the actions to carry out on the styles
     */
    public void processCurrentStyle(@NonNull ProcessStyle processor) {
        for (FeatureStyle style : currentStyle.internalStyles.values()) {
            if (style != null) {
                processor.process(style);
            }
        }
        if (currentStyle.wayStyles != null) {
            processor.process(currentStyle.wayStyles);
            if (currentStyle.wayStyles.cascadedStyles != null) {
                processRecursive(currentStyle.wayStyles.cascadedStyles, processor);
            }
        }
        if (currentStyle.relationStyles != null) {
            processor.process(currentStyle.relationStyles);
            if (currentStyle.relationStyles.cascadedStyles != null) {
                processRecursive(currentStyle.relationStyles.cascadedStyles, processor);
            }
        }
        if (currentStyle.validationStyles != null) {
            for (FeatureStyle style : currentStyle.validationStyles.values()) {
                if (style != null) {
                    processor.process(style);
                }
            }
        }
    }

    /**
     * Traverse recursively all the styles provided
     * 
     * @param styles a List of FeatureStyle
     * @param processor the processor
     */
    private static void processRecursive(@NonNull List<FeatureStyle> styles, @NonNull ProcessStyle processor) {
        for (FeatureStyle style : styles) {
            if (style != null) {
                processor.process(style);
                if (style.cascadedStyles != null) {
                    processRecursive(style.cascadedStyles, processor);
                }
            }
        }
    }

    /**
     * Set the anti-aliasing flag on all styles
     * 
     * @param aa the boolean value to set
     */
    public void setAntiAliasing(final boolean aa) {
        processCurrentStyle(style -> style.getPaint().setAntiAlias(aa));
    }

    interface ProcessStyle {
        /**
         * Process a FeatureStyle
         * 
         * @param style the style to work on
         */
        void process(@NonNull FeatureStyle style);
    }

    /**
     * Sets the stroke width of all styles with update enabled corresponding to the width of the viewbox (=zoomfactor).
     * 
     * @param newStrokeWidth the new width to set
     */
    public void updateStrokes(final float newStrokeWidth) {
        processCurrentStyle(style -> {
            if (style.updateWidth) {
                style.setStrokeWidth(newStrokeWidth);
            }
        });

        // hardwired (for now)
        DataStyle.WAY_DIRECTION_PATH.rewind();
        float wayDirectionPathOffset = newStrokeWidth * 2.0f;
        DataStyle.WAY_DIRECTION_PATH.moveTo(-wayDirectionPathOffset, -wayDirectionPathOffset);
        DataStyle.WAY_DIRECTION_PATH.lineTo(0, 0);
        DataStyle.WAY_DIRECTION_PATH.lineTo(-wayDirectionPathOffset, +wayDirectionPathOffset);
    }

    /**
     * Get the internal FeatureStyle specified by key from current profile
     * 
     * @param key the key for the style
     * @return the style or null if not found
     */
    @Nullable
    public FeatureStyle getInternal(@NonNull final String key) {
        return currentStyle.internalStyles.get(key);
    }

    /**
     * Get the validation FeatureStyle specified by code from current profile
     * 
     * @param code the validation code for the object
     * @return the style or the default problem style if not found
     */
    @Nullable
    public FeatureStyle getValidationStyle(int code) {
        FeatureStyle style = currentStyle.validationStyles.get(code);
        if (style == null) {
            return getInternal(DataStyle.PROBLEM_WAY);
        }
        return style;
    }

    /**
     * Get the current DataStyle
     * 
     * @return the current DataStyle
     */
    public DataStyle getCurrent() {
        return currentStyle;
    }

    /**
     * return specific named profile
     * 
     * @param name name of the profile
     * @return the DataStyle object or null if it couldn't be found
     */
    @Nullable
    public DataStyle getStyle(@NonNull String name) {
        Log.i(DEBUG_TAG, "getStyle " + name);
        if (availableStyles == null) {
            return null;
        }
        return availableStyles.get(name);
    }

    /**
     * Get the list of available Styles
     * 
     * @param context an Android Context
     * @return list of available Styles (Default entry first, rest sorted)
     */
    @NonNull
    public String[] getStyleList(@NonNull Context context) {
        if (availableStyles.size() == 0) { // shouldn't happen
            Log.e(DEBUG_TAG, "getStyleList called before initialized");
            addDefaultStyle(context);
        }
        // creating the default style object will set availableStyles
        Map<String, DataStyle> sortedMap = new TreeMap<>();
        for (Entry<String, DataStyle> entry : availableStyles.entrySet()) {
            final DataStyle value = entry.getValue();
            final String key = entry.getKey();
            if (key != null && value != null) {
                sortedMap.put(key, value);
            } else {
                Log.e(DEBUG_TAG, "Style object missing for style " + key);
            }
        }
        List<String> res = new ArrayList<>();
        res.addAll(sortedMap.keySet());
        if (!BUILTIN_STYLE_NAME.equals(res.get(0))) {
            res.add(0, BUILTIN_STYLE_NAME);
        }
        return res.toArray(new String[0]);
    }

    /**
     * Translate a string using the style specific translation files
     * 
     * @param original the untranslated string
     * @return the translated string (or the original if no translation was found)
     */
    @NonNull
    public String translate(@NonNull String original) {
        if (po != null) {
            return po.t(original);
        }
        return original;
    }

    /**
     * Get a stream for the translation
     * 
     * @param context an Android Context
     * @param locale the relevant Locale
     * @return an InputStream
     * @throws IOException if no file could be opened or read
     */
    @NonNull
    private static InputStream getPoFileStream(Context context, Locale locale) throws IOException {
        AssetManager assetManager = context.getAssets();
        try {
            return assetManager.open(I18N_DATASTYLE + locale + "." + FileExtensions.PO);
        } catch (IOException ioex) {
            return assetManager.open(I18N_DATASTYLE + locale.getLanguage() + "." + FileExtensions.PO);
        }
    }

    /**
     * switch to Style with name n
     * 
     * @param n name of the style
     * @return true if successful
     */
    public boolean switchTo(@NonNull String n) {
        DataStyle p = getStyle(n);
        if (p != null) {
            currentStyle = p;
            Log.i(DEBUG_TAG, "Switching to " + n);
            return true;
        }
        return false;
    }

    /**
     * Query the config database for styles files and creates new styles from them
     * 
     * @param ctx Android Context
     */
    @SuppressLint("NewApi")
    public void getStylesFromFiles(@NonNull Context ctx) {
        if (availableStyles.size() == 0) {
            Log.i(DEBUG_TAG, "No styles found");
            // no files, need to install a default
            addDefaultStyle(ctx);
        }
        AssetManager assetManager = ctx.getAssets();
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ctx)) {
            for (StyleConfiguration styleConf : db.getStyles()) {
                final String url = styleConf.url;
                if (Util.isEmpty(url)) {
                    continue;
                }
                if (!styleConf.custom) {
                    Log.i(DEBUG_TAG, "Creating style from file in assets directory " + url);
                    try (InputStream is = assetManager.open(Paths.DIRECTORY_PATH_STYLES + Paths.DELIMITER + url)) {
                        DataStyle style = new DataStyle(ctx, is, null);
                        addStyle(db, styleConf, style);
                    } catch (Exception ex) {
                        // this shouldn't happen with styles included with the APK, so no need to toast
                        Log.e(DEBUG_TAG, "Reading " + url + " failed");
                    }
                } else {
                    try {
                        // overwrites profile with same name, use name from configuration not from style
                        DataStyle style = addStyleFromFile(ctx, db, styleConf);
                        addStyle(db, styleConf, style);
                    } catch (Exception ex) { // never crash
                        Log.e(DEBUG_TAG, ex.toString());
                        ScreenMessage.toastTopError(ctx, ctx.getString(R.string.toast_invalid_style_file, styleConf.url, ex.getMessage()));
                    }
                }
            }
        }
        Locale locale = Locale.getDefault();
        try (InputStream poFileStream = getPoFileStream(ctx, locale)) {
            po = de.blau.android.util.Util.parsePoFile(poFileStream);
        } catch (IOException ioex) {
            Log.w(DEBUG_TAG, "No translations found for " + locale);
        }
    }

    /**
     * Make the style available and set the current style if it is active
     * 
     * @param db the AdvancedPrefDatabase
     * @param styleConf the StyleConfiguration
     * @param style the actual DataStyle
     */
    private void addStyle(AdvancedPrefDatabase db, StyleConfiguration styleConf, DataStyle style) {
        availableStyles.put(styleConf.name, style);
        if (styleConf.isActive()) {
            currentStyle = style;
        }
        setAdditionalFieldsFromStyle(db, styleConf, style);
    }

    /**
     * Read in a custom style file and add it to the list of available ones
     * 
     * @param ctx Android Context
     * @param db the pref DB
     * @param style the StyleCOnfiguration
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * 
     */
    @Nullable
    public static DataStyle addStyleFromFile(@NonNull Context ctx, @NonNull AdvancedPrefDatabase db, @NonNull StyleConfiguration style)
            throws SAXException, IOException, ParserConfigurationException {
        // style is in directory with id as name
        Log.i(DEBUG_TAG, "Creating style for " + style.url);
        File styleDir = db.getResourceDirectory(style.id);
        File f = new File(styleDir, XmlFile.getFileName(styleDir));
        Log.i(DEBUG_TAG, "... file " + f.getAbsolutePath());

        try (InputStream is = new FileInputStream(f)) {
            return new DataStyle(ctx, is, f.getParent());
        }
    }

    /**
     * Set description and version fields if they have changed
     * 
     * @param db an AdvancedPrefDatabase instance
     * @param styleConf the StyleCOnfiguration
     * @param style the parsed style
     */
    private void setAdditionalFieldsFromStyle(@NonNull AdvancedPrefDatabase db, @NonNull StyleConfiguration styleConf, @NonNull DataStyle style) {
        boolean versionChanged = style.getVersion() != null && !style.getVersion().equals(styleConf.version);
        boolean descriptionChanged = style.getDescription() != null && !style.getDescription().equals(styleConf.description);
        if (versionChanged || descriptionChanged) {
            db.setStyleAdditionalFields(styleConf.id, style.getVersion(), style.getDescription());
        }
    }

    /**
     * Get a list of styles from the assets
     * 
     * @param assetManager an AssertManager instance
     * @return a array of names or null
     */
    @Nullable
    private String[] getAssetStyleList(@NonNull AssetManager assetManager) {
        try {
            return assetManager.list(Paths.DIRECTORY_PATH_STYLES);
        } catch (IOException ex) {
            Log.i(DEBUG_TAG, ex.toString());
            return null;
        }
    }

    /**
     * Add the builtin minimal style so that we always have something to fall back too
     * 
     * @param ctx an Android Context
     */
    private void addDefaultStyle(@NonNull Context ctx) {
        DataStyle p = new DataStyle(ctx);
        p.setName(BUILTIN_STYLE_NAME);
        currentStyle = p;
        availableStyles.put(BUILTIN_STYLE_NAME, p);
    }

    /**
     * Reset contents
     * 
     * @param ctx an Android Context
     * @param reInit if true reread files
     */
    public void reset(@NonNull Context ctx, boolean reInit) {
        availableStyles.clear();
        addDefaultStyle(ctx);
        if (reInit) {
            getStylesFromFiles(ctx);
        }
    }

    /**
     * Get the name of the builtin style
     * 
     * @return the name of the builtin style
     */
    @NonNull
    public static String getBuiltinStyleName() {
        return BUILTIN_STYLE_NAME;
    }

    /**
     * Get the id of the builtin style
     * 
     * @return the id of the builtin style
     */
    @NonNull
    public static String getBuiltinStyleId() {
        return BUILTIN_STYLE_ID;
    }

    /**
     * Return the cached style for the element, or determine the style to use and cache it in the object
     * 
     * @param element the OsmElement we need the style for
     * @param <T> an OsmElement
     * @return the style
     */
    @NonNull
    public <T extends OsmElement> FeatureStyle matchStyle(@NonNull final T element) {
        final boolean styleable = element instanceof StyleableFeature;
        FeatureStyle style = styleable ? ((StyleableFeature) element).getStyle() : null;
        if (style == null) {
            if (element instanceof Way) {
                style = DataStyle.matchRecursive(currentStyle.wayStyles, element.getTags(), ((Way) element).isClosed());
            } else if (element instanceof Node) {
                style = DataStyle.matchRecursive(currentStyle.nodeStyles, element.getTags(), false);
            } else {
                style = DataStyle.matchRecursive(currentStyle.relationStyles, element.getTags(), false);
            }
            if (styleable) {
                ((StyleableFeature) element).setStyle(style);
            }
        }
        return style;
    }

    /**
     * Generate a taginfo project file for the current style
     * 
     * @param output File to write to
     * @return true if successful
     */
    public boolean generateTaginfoJson(@NonNull File output) {
        MultiHashMap<String, String> tagMap = new MultiHashMap<>(true);
        addRecursive(tagMap, currentStyle.nodeStyles, "node");
        addRecursive(tagMap, currentStyle.wayStyles, "way");
        addRecursive(tagMap, currentStyle.relationStyles, "relation");

        try (FileOutputStream fout = new FileOutputStream(output); PrintStream outputStream = new PrintStream(new BufferedOutputStream(fout))) {
            Preset.tagInfoHeader(outputStream, "Vespucci map style",
                    "https://raw.githubusercontent.com/MarcusWolschon/osmeditor4android/master/taginfo-style.json",
                    "Default map style for Vespucci. The default rendering for Nodes is to use the icon from the matching preset item.");
            outputStream.println("\"tags\":[");
            boolean firstTag = true;
            for (String tag : tagMap.getKeys()) {
                String[] keyValue = tag.split("=");
                final String key = keyValue[0];
                if (Util.isEmpty(key)) {
                    continue;
                }
                if (!firstTag) {
                    outputStream.println(",");
                }
                outputStream.print("{\"description\":\"Data rendering\",");
                outputStream.print("\"key\": \"" + key + "\"" + (keyValue.length == 1 || "*".equals(keyValue[1]) ? "" : ",\"value\": \"" + keyValue[1] + "\""));
                outputStream.print(",\"object_types\": [");
                boolean firstGeometry = true;
                for (String geometry : tagMap.get(tag)) {
                    if (!firstGeometry) {
                        outputStream.print(",");
                    }
                    outputStream.print("\"" + geometry + "\"");
                    firstGeometry = false;
                }
                outputStream.print("]");
                firstTag = false;
                outputStream.print("}");
            }
            outputStream.print("]}");
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Export failed - " + output.getAbsolutePath() + " exception " + e);
            return false;
        }
        return true;
    }

    /**
     * Add tags from a style recursively to a map
     * 
     * @param tagMap the Map
     * @param style the input style
     * @param geometry the geometry value
     */
    private static void addRecursive(@NonNull MultiHashMap<String, String> tagMap, @NonNull FeatureStyle style, @NonNull String geometry) {
        if (style.isArea()) {
            geometry = "area";
        }
        for (Entry<String, String> entry : style.getTags().entrySet()) {
            tagMap.add(entry.getKey() + "=" + entry.getValue(), geometry);
        }
        if (style.cascadedStyles != null) {
            for (FeatureStyle subStyle : style.cascadedStyles) {
                addRecursive(tagMap, subStyle, geometry);
            }
        }
    }
}
