package io.vespucci.presets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;

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
     * @param path a List of Strings representing the path
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
        StringBuilder result = new StringBuilder();
        for (String s : path) {
            result.append(s);
            result.append('|');
        }
        return result.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PresetElementPath)) {
            return false;
        }
        PresetElementPath other = (PresetElementPath) obj;
        return path.equals(other.path);
    }
}
