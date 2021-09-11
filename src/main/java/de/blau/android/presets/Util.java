package de.blau.android.presets;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.Preferences;
import de.blau.android.taginfo.TaginfoServer;
import de.blau.android.taginfo.TaginfoServer.ValueResult;
import de.blau.android.util.StringWithDescription;

public final class Util {

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
     * This can be used by presets do not remove
     * 
     * @param key the key we want values for
     * @return a List of the values or null if nothing could be found
     */
    @Nullable
    public static StringWithDescription[] getValuesFromTaginfo(@NonNull String key) {
        StringWithDescription[] result = null;
        try {
            String server = new Preferences(App.getCurrentInstance()).getTaginfoServer();
            List<ValueResult> temp = TaginfoServer.keyValues(null, server, key, 50);
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

    /**
     * Group keys so that i18n follow the base key (which should always come first in map)
     * 
     * Note: this only works if map preserves insert order
     * 
     * @param <V> object containing the tag value(s)
     * @param i18nKeys List of keys that potentially have i18n variants
     * @param map a Map containing PresetFields and their current values
     */
    public static <V> void groupI18nKeys(List<String> i18nKeys, Map<PresetField, V> map) {
        Map<PresetField, V> temp = new LinkedHashMap<>();
        List<PresetField> keys = new ArrayList<>(map.keySet());
        while (!keys.isEmpty()) {
            PresetField field = keys.get(0);
            String key = field.getKey();
            keys.remove(0);
            if (i18nKeys.contains(key)) {
                temp.put(field, map.get(field));
                int i = 0;
                while (!keys.isEmpty() && i < keys.size()) {
                    PresetField i18nKeyField = keys.get(i);
                    String i18nKey = i18nKeyField.getKey();
                    if (i18nKey.startsWith(key + ":")) {
                        temp.put(keys.get(i), map.get(i18nKeyField));
                        keys.remove(i);
                    } else {
                        i++;
                    }
                }
            } else {
                temp.put(field, map.get(field));
            }
        }
        map.clear();
        map.putAll(temp);
    }

    /**
     * Group address tags
     * 
     * Note: this only works if map preserves insert order
     * 
     * @param <V> object containing the tag value(s)
     * @param map map that preserves insert order
     */
    public static <V> void groupAddrKeys(Map<PresetField, V> map) {
        List<Entry<PresetField, V>> temp = new ArrayList<>();
        for (Entry<PresetField, V> entry : new HashSet<>(map.entrySet())) { // needs a copy since we are modifying map
            PresetField field = entry.getKey();
            String key = field.getKey();
            if (key.startsWith(Tags.KEY_ADDR_BASE) && Tags.KEY_ADDR_HOUSENUMBER.equals(key)) {
                temp.add(entry);
                map.remove(field);
            }
        }
        Collections.sort(temp, (e0, e1) -> Tags.ADDRESS_SORT_ORDER.get(e0.getValue()).compareTo(Tags.ADDRESS_SORT_ORDER.get(e1.getValue())));
        for (Entry<PresetField, V> entry : temp) {
            map.put(entry.getKey(), entry.getValue());
        }
    }
}
