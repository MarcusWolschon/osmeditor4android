package de.blau.android.presets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.support.annotation.NonNull;

/**
 * Container for the path to a specific PresetElement
 * 
 * @author simon
 *
 */
public class PresetElementPath implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<String> path;

    /**
     * Construct an empty PresetElementPath
     */
    public PresetElementPath() {
        path = new ArrayList<>();
    }

    /**
     * Construct an PresetElementPath from a List of path element
     * 
     * @param path a List of Strings representign the path
     */
    public PresetElementPath(@NonNull List<String> path) {
        this.path = new ArrayList<>(path);
    }

    /**
     * Construct a PresetElementPath from an existing one
     * 
     * @param existingPath the existing PresetElementPath
     */
    public PresetElementPath(PresetElementPath existingPath) {
        path = new ArrayList<>(existingPath.path);
    }

    /**
     * Get the elements of the path as a List
     * 
     * @return a List of the path elements
     */
    @NonNull
    List<String> getPath() {
        return path;
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
