package de.blau.android.presets;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Container for the path to a specific PresetElement
 * 
 * @author simon
 *
 */
public class PresetElementPath implements Serializable {
    private static final long serialVersionUID = 1L;
    final ArrayList<String>   path;

    /**
     * Construct an empty PresetElementPath
     */
    public PresetElementPath() {
        path = new ArrayList<>();
    }

    /**
     * Construct a PresetElementPath from an existing one
     * 
     * @param existingPath the existing PresetElementPath
     */
    public PresetElementPath(PresetElementPath existingPath) {
        path = new ArrayList<>(existingPath.path);
    }

    @Override
    public String toString() {
        String result = "";
        for (String s : path) {
            result += s + "|";
        }
        return result;
    }
}
