package de.blau.android.javascript;

import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.BuildConfig;
import de.blau.android.Logic;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetItem;

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

    private static final String DEBUG_TAG = "javascript.Utils";

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
            Scriptable restrictedScope = App.getRestrictedRhinoScope(ctx);
            Scriptable scope = rhinoContext.newObject(restrictedScope);
            scope.setPrototype(restrictedScope);
            scope.setParentScope(null);
            Object wrappedOut = org.mozilla.javascript.Context.javaToJS(BuildConfig.VERSION_CODE, scope);
            ScriptableObject.putProperty(scope, "versionCode", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(originalTags, scope);
            ScriptableObject.putProperty(scope, "originalTags", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(tags, scope);
            ScriptableObject.putProperty(scope, "tags", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(value, scope);
            ScriptableObject.putProperty(scope, "value", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(key2PresetItem, scope);
            ScriptableObject.putProperty(scope, "key2PresetItem", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(presets, scope);
            ScriptableObject.putProperty(scope, "presets", wrappedOut);
            Log.d(DEBUG_TAG, "Eval (preset): " + script);
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
            ScriptableObject.putProperty(scope, "versionCode", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(logic, scope);
            ScriptableObject.putProperty(scope, "logic", wrappedOut);
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
