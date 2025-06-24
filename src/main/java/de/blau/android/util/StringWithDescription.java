package de.blau.android.util;

import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.presets.Regionalizable;
import de.blau.android.presets.ValueWithCount;


@SuppressWarnings("EqualsOverridable")
public class StringWithDescription extends Regionalizable implements Comparable<StringWithDescription>, Serializable, Value {
    private static final long serialVersionUID = 3L; // Keep version as updated before

    private final String value;
    private String       description;
    private List<String> linkedKeys;

    /**
     * Construct a new instance
     * 
     * @param value the value
     */
    public StringWithDescription(@NonNull final String value) {
        this.value = value;
        this.linkedKeys = null;
    }

    public StringWithDescription(@NonNull final String value, @Nullable final String description) {
        this.value = value;
        this.description = description;
        this.linkedKeys = null;
    }

    /**
     * Construct a new instance from object of a known type.
     * Handles StringWithDescription (and its subclasses), ValueWithCount, and String.
     * Copies linkedKeys if available in the source object.
     */
    public StringWithDescription(@NonNull Object o) {
        if (o instanceof ValueWithCount) {
            ValueWithCount source = (ValueWithCount) o;
            value = source.getValue();
            description = source.getDescription();
            this.linkedKeys = safeCopyList(source.getLinkedKeys());
        } else if (o instanceof StringWithDescription) {
            StringWithDescription source = (StringWithDescription) o;
            value = source.getValue();
            description = source.getDescription();

            this.linkedKeys = safeCopyList(source.getLinkedKeys());
        } else if (o instanceof String) {
            value = (String) o;
            description = value;
            this.linkedKeys = null;
        } else {
            value = "";
            description = "";
            this.linkedKeys = null;
        }
    }

    @Override
    @NonNull
    public String getValue() {
        return value;
    }

    /**
     * @return the description can be null
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Set the description field
     * 
     * @param description the description
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    /**
     * Sets the list of keys linked to this value. Called by the PresetParser.
     * Makes a defensive copy of the list. Pass null or empty list to clear.
     *
     * @param keys A list of linked keys, or null. Accepts any List implementation.
     */
    public void setLinkedKeys(@Nullable List<String> keys) { // <-- CHANGED parameter to List
        this.linkedKeys = safeCopyList(keys); // Use the updated helper
    }

    /**
     * Gets a defensive copy of the list of linked keys associated with this value.
     *
     * @return A new List containing the linked keys (specifically an ArrayList),
     *         or null if no keys are linked.
     */
    @Nullable
    public List<String> getLinkedKeys() { // <-- CHANGED return type to List
        // safeCopyList now returns List, but creates an ArrayList internally
        return safeCopyList(this.linkedKeys);
    }

    /**
     * Checks if this value has any linked keys associated with it.
     *
     * @return true if linkedKeys is not null and not empty, false otherwise.
     */
    public boolean hasLinkedKeys() {
        return this.linkedKeys != null && !this.linkedKeys.isEmpty();
    }

    /**
     * Helper method to safely copy a list or return null.
     * Accepts any List, returns an ArrayList copy or null.
     */
    @Nullable
    private List<String> safeCopyList(@Nullable List<String> source) { // <-- CHANGED parameter AND return type to List
        // Return null if source is null/empty. Otherwise, return a NEW ArrayList copy.
        return (source == null || source.isEmpty()) ? null : new ArrayList<>(source);
    }
    @Override
    public String toString() {

        String descPart = (description != null && !description.equals(value)) ? " - " + description : "";
        return value + descPart;
    }

    @Override
    public int compareTo(@NonNull StringWithDescription s) {
        // Comparison remains based on value
        return value.compareTo(s.getValue());
    }

    /**
     * Check for equality with a String value
     * 
     * This is likely bad style
     * 
     * @param s String to compare with
     * @return true if the value of this object is the same as s
     */
    public boolean equals(@Nullable String s) {
        return this.value.equals(s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringWithDescription)) return false;
        StringWithDescription other = (StringWithDescription) o;
        return value.equals(other.value) &&
                Objects.equals(description, other.description) &&
                Objects.equals(linkedKeys, other.linkedKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, description, linkedKeys);
    }

    /**
     * 
     * @author simon
     *
     */
    public static class LocaleComparator implements Comparator<StringWithDescription> {

        Collator defaultLocaleCollator = Collator.getInstance();

        @Override
        public int compare(StringWithDescription lhs, StringWithDescription rhs) {
            String lhsDescription = lhs.getDescription();
            if (lhsDescription == null || "".equals(lhsDescription)) {
                lhsDescription = lhs.getValue();
            }
            String rhsDescription = rhs.getDescription();
            if (rhsDescription == null || "".equals(rhsDescription)) {
                rhsDescription = rhs.getValue();
            }
            return defaultLocaleCollator.compare(lhsDescription, rhsDescription);
        }
    }
}
