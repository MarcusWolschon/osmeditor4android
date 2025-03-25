package io.vespucci.presets;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class Regionalizable {

    private List<String> regions        = null;
    private boolean      excludeRegions = false;

    /**
     * Default constructor
     */
    protected Regionalizable() {
        // nothing
    }

    /**
     * Copy constructor
     * 
     * @param source source object
     */
    protected Regionalizable(@NonNull Regionalizable source) {
        this.regions = source.regions;
        this.excludeRegions = source.excludeRegions;
    }

    /**
     * Set the ISO codes for the regions this object is intended for
     * 
     * @param regions the ISO codes separated by commas or null if none should be set
     */
    protected void setRegions(@Nullable String regions) {
        if (regions != null) {
            String[] temp = regions.split(",");
            this.regions = new ArrayList<>();
            for (String r : temp) {
                this.regions.add(r.trim().toUpperCase());
            }
        } else {
            this.regions = null;
        }
    }

    /**
     * Check if an object is applicable for a region
     * 
     * @param region the region
     * @return true if the object is in use
     */
    public boolean appliesIn(@Nullable String region) {
        if (regions != null && !regions.isEmpty() && region != null) {
            if (regions.contains(region)) {
                return !excludeRegions;
            }
            return excludeRegions;
        }
        return true;
    }

    /**
     * Check if an object is applicable for a region in a list
     * 
     * @param regionList list of regions the object is used in
     * @return true if the object is in use
     */
    public boolean appliesIn(@Nullable List<String> regionList) {
        if (regions != null && !regions.isEmpty() && regionList != null) {
            for (String r : regionList) {
                if (regions.contains(r)) {
                    return !excludeRegions;
                }
            }
            return excludeRegions;
        }
        return true;
    }

    /**
     * Set if the object shouldn't be used in the listed regions
     * 
     * @param excludeRegions if true the function of the regions list will be inverted
     */
    protected void setExcludeRegions(boolean excludeRegions) {
        this.excludeRegions = excludeRegions;
    }
}
