package de.blau.android.presets;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.taginfo.TaginfoServer;
import de.blau.android.taginfo.TaginfoServer.ValueResult;
import de.blau.android.util.StringWithDescription;

public class Util {

    private static final String DEBUG_TAG = Util.class.getSimpleName();

    /**
     * Empty private constructor
     */
    private Util() {
        // nothing to do
    }

    /**
     * Invoke a static method
     * 
     * @param spec spec in the form class@method
     * @param arg argument for the method or null
     * @return whatever the method returned, null if invoking the method didn't work for whatever reason
     */
    @Nullable
    static Object invokeMethod(@NonNull String spec, @Nullable String arg) {
        Object result = null;
        String[] classAndMethod = spec.split("#");
        if (classAndMethod.length == 2) {
            try {
                Class<?> c = Class.forName(classAndMethod[0]);
                java.lang.reflect.Method method;
                try {
                    // try without parameter
                    method = c.getMethod(classAndMethod[1]);
                } catch (NoSuchMethodException nsme) {
                    // try with parameter
                    method = c.getMethod(classAndMethod[1], String.class);
                }
                int modifiers = method.getModifiers();
                if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
                    int paramCount = method.getParameterTypes().length;
                    if (paramCount == 0) {
                        result = method.invoke(null);
                    } else if (paramCount == 1 && method.getParameterTypes()[0].equals(String.class) && arg != null) {
                        result = method.invoke(null, arg);
                    }
                }
            } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                Log.e(DEBUG_TAG, "invokeMethod " + e.getClass().getSimpleName() + " " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Get values for key from Taginfo Limit to the top 20
     * 
     * @param key the key we want values for
     * @return a List of the values or null if nothing could be found
     */
    @Nullable
    public static StringWithDescription[] getValuesFromTaginfo(@NonNull String key) {
        StringWithDescription[] result = null;
        try {
            List<ValueResult> temp = TaginfoServer.keyValues(App.getCurrentInstance().getBaseContext(), key, 50);
            if (temp != null) {
                result = new StringWithDescription[temp.size()];
                for (int i = 0; i < temp.size(); i++) {
                    result[i] = temp.get(i);
                }
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "getValuesFromTaginfo got " + e.getMessage());
        }
        return result;
    }
}
