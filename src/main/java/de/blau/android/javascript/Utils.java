package de.blau.android.javascript;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.mapbox.geojson.Feature;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.BuildConfig;
import de.blau.android.Logic;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetItem;
import de.blau.android.tasks.Todo;

/**
 * Various JS related utility methods
 * 
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Scopes_and_Contexts">https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Scopes_and_Contexts</a>
 * @see <a href=
 *      "https://dxr.mozilla.org/mozilla/source/js/rhino/examples/DynamicScopes.java">https://dxr.mozilla.org/mozilla/source/js/rhino/examples/DynamicScopes.java</a>
 * @author simon
 *
 */
public final class Utils {

    private static final int    TAG_LEN          = Math.min(LOG_TAG_LEN, Utils.class.getSimpleName().length());
    private static final String DEBUG_TAG        = Utils.class.getSimpleName().substring(0, TAG_LEN);
    private static final String LOGIC            = "logic";
    private static final String KEY2_PRESET_ITEM = "key2PresetItem";
    private static final String VALUE            = "value";
    private static final String TAGS             = "tags";
    private static final String ORIGINAL_TAGS    = "originalTags";
    private static final String VERSION_CODE     = "versionCode";
    private static final String TODO             = "todo";
    private static final String FEATURE          = "feature";

    /**
     * Empty private constructor
     */
    private Utils() {
        // don't allow instantiating of this class
    }

    /**
     * Evaluate JS
     * 
     * @param ctx android context
     * @param scriptName name for error reporting
     * @param script the javascript
     * @return whatever the JS returned as a string
     */
    @Nullable
    public static String evalString(Context ctx, String scriptName, String script) {
        Log.d(DEBUG_TAG, "Eval " + script);
        org.mozilla.javascript.Context rhinoContext = App.getRhinoHelper(ctx).enterContext();
        try {
            Scriptable restrictedScope = App.getRestrictedRhinoScope(ctx);
            Scriptable scope = rhinoContext.newObject(restrictedScope);
            scope.setPrototype(restrictedScope);
            scope.setParentScope(null);
            Object result = rhinoContext.evaluateString(scope, script, scriptName, 1, null);
            return org.mozilla.javascript.Context.toString(result);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    /**
     * Evaluate JS associated with a key in a preset
     * 
     * @param ctx android context
     * @param scriptName name for error reporting
     * @param script the javascript
     * @param originalTags original tags the property editor was called with
     * @param tags the current tags
     * @param value any value associated with the key
     * @param key2PresetItem map from key to PresetItem
     * @param presets the currently available Presets
     * @return the value that should be assigned to the tag or null if no value should be set
     */
    @Nullable
    public static String evalString(@NonNull Context ctx, @NonNull String scriptName, @NonNull String script, @NonNull Map<String, List<String>> originalTags,
            @NonNull Map<String, List<String>> tags, @NonNull String value, @NonNull Map<String, PresetItem> key2PresetItem, @NonNull Preset[] presets) {
        org.mozilla.javascript.Context rhinoContext = App.getRhinoHelper(ctx).enterContext();
        try {
            Map<String, List<String>> savedTags = deepCopy(tags);
            Scriptable restrictedScope = App.getRestrictedRhinoScope(ctx);
            Scriptable scope = rhinoContext.newObject(restrictedScope);
            scope.setPrototype(restrictedScope);
            scope.setParentScope(null);
            Object wrappedOut = org.mozilla.javascript.Context.javaToJS(BuildConfig.VERSION_CODE, scope);
            ScriptableObject.putProperty(scope, VERSION_CODE, wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(originalTags, scope);
            ScriptableObject.putProperty(scope, ORIGINAL_TAGS, wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(tags, scope);
            ScriptableObject.putProperty(scope, TAGS, wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(value, scope);
            ScriptableObject.putProperty(scope, VALUE, wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(key2PresetItem, scope);
            ScriptableObject.putProperty(scope, KEY2_PRESET_ITEM, wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(presets, scope);
            ScriptableObject.putProperty(scope, "presets", wrappedOut);
            Log.d(DEBUG_TAG, "Eval (preset): " + script);
            Object result = rhinoContext.evaluateString(scope, script, scriptName, 1, null);
            // check that we haven't stored something in currentValues that isn't a String as this will crash things
            // as soon as it is accessed
            try {
                for (Entry<String, List<String>> e : tags.entrySet()) {
                    for (@SuppressWarnings("unused")
                    String s : e.getValue()) { // NOSONAR
                        // do nothing
                    }
                }
            } catch (ClassCastException cce) {
                // undo all changes
                tags.clear();
                for (Entry<String, List<String>> entry : savedTags.entrySet()) {
                    tags.put(entry.getKey(), savedTags.get(entry.getKey()));
                }
                throw cce;
            }
            if (result != null) {
                return org.mozilla.javascript.Context.toString(result);
            }
            return null;
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    /**
     * Set fields in a Todo from a GeoJosn Feature // NOSONAR
     * 
     * @param ctx android context
     * @param scriptName name for error reporting
     * @param script the javascript
     * @param feature the Feature to convert
     * @param todo the pre-allocated Todo // NOSONAR
     * @return whatever the JS returned as a string
     */
    @Nullable
    public static String evalString(@NonNull Context ctx, @NonNull String scriptName, @NonNull String script, @NonNull Feature feature, @NonNull Todo todo) {
        org.mozilla.javascript.Context rhinoContext = App.getRhinoHelper(ctx).enterContext();
        try {
            Scriptable restrictedScope = App.getRestrictedRhinoScope(ctx);
            Scriptable scope = rhinoContext.newObject(restrictedScope);
            scope.setPrototype(restrictedScope);
            scope.setParentScope(null);
            Object wrappedOut = org.mozilla.javascript.Context.javaToJS(feature, scope);
            ScriptableObject.putProperty(scope, FEATURE, wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(todo, scope);
            ScriptableObject.putProperty(scope, TODO, wrappedOut);
            Object result = rhinoContext.evaluateString(scope, script, scriptName, 1, null);
            if (result != null) {
                return org.mozilla.javascript.Context.toString(result);
            }
            return null;
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    /**
     * Make a deep copy of the tag map
     * 
     * @param tags the tag map
     * @return a deep copy of the map
     */
    @NonNull
    private static Map<String, List<String>> deepCopy(Map<String, List<String>> tags) {
        Map<String, List<String>> copy = new HashMap<>();
        for (Entry<String, List<String>> entry : tags.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Evaluate a script making the current logic available, this essentially allows access to all data
     * 
     * @param ctx android context
     * @param scriptName name for error reporting
     * @param script the javascript
     * @param logic an instance of Logic
     * @return result of evaluating the JS as a string
     */
    @Nullable
    public static String evalString(@NonNull Context ctx, @NonNull String scriptName, @NonNull String script, @NonNull Logic logic) {
        org.mozilla.javascript.Context rhinoContext = App.getRhinoHelper(ctx).enterContext();
        try {
            Scriptable restrictedScope = App.getRestrictedRhinoScope(ctx);
            Scriptable scope = rhinoContext.newObject(restrictedScope);
            scope.setPrototype(restrictedScope);
            scope.setParentScope(null);
            Object wrappedOut = org.mozilla.javascript.Context.javaToJS(BuildConfig.VERSION_CODE, scope);
            ScriptableObject.putProperty(scope, VERSION_CODE, wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(logic, scope);
            ScriptableObject.putProperty(scope, LOGIC, wrappedOut);
            Log.d(DEBUG_TAG, "Eval (logic): " + script);
            Object result = rhinoContext.evaluateString(scope, script, scriptName, 1, null);
            if (result == null) {
                return null;
            } else {
                return org.mozilla.javascript.Context.toString(result);
            }
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }
}
