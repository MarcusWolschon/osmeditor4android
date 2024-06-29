package de.blau.android.propertyeditor;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.blau.android.osm.Capabilities;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.Preset;

/**
 * Interface for communicating with the PropertyEditor activity
 */
public interface PropertyEditorListener {

    /**
     * Check if we are actually visible to the user
     * 
     * @param me the calling Fragment
     * @return true if shown
     */
    boolean onTop(Fragment me);

    /**
     * Are we connected to a network?
     * 
     * @return true if connected
     */
    boolean isConnected();

    /**
     * Are we connected or connecting to a network?
     * 
     * @return true if connected or in the process of connecting
     */
    boolean isConnectedOrConnecting();

    /**
     * Get a List of ISO country codes for the current object
     * 
     * @return a List of ISO country code Strings or null if none found
     */
    @Nullable
    List<String> getIsoCodes();

    /**
     * Get the country ISO code
     * 
     * @return the ISO country code or null if not found
     */
    @Nullable
    String getCountryIsoCode();

    /**
     * Get the current OsmElement
     * 
     * @return an OsmELement
     */
    @NonNull
    OsmElement getElement();

    /**
     * Get the current set of Presets
     * 
     * @return an array of Preset
     */
    @NonNull
    Preset[] getPresets();

    /**
     * Get current contents of editor for saving
     * 
     * @return list containing the tag maps or null if something went wrong
     */
    @Nullable
    public List<Map<String, String>> getUpdatedTags();

    /**
     * Get original tags
     * 
     * @return list containing the tag maps or null if something went wrong
     */
    @Nullable
    public List<Map<String, String>> getOriginalTags();

    /**
     * Re-create the RecentPrestsFragement view
     */
    public void updateRecentPresets();

    /**
     * Update the data and terminate
     */
    public void updateAndFinish();

    /**
     * Allow ViewPager to work
     */
    public void enablePaging();

    /**
     * Disallow ViewPAger to work
     */
    public void disablePaging();

    /**
     * Check if paging is enabled
     * 
     * @return true if paging is enabled
     */
    public boolean isPagingEnabled();

    /**
     * Allow presets to be applied
     */
    public void enablePresets();

    /**
     * Disallow presets to be applied
     */
    public void disablePresets();

    /**
     * Get the current API-Servers Capabilities object
     * 
     * @return a Capabilities instance
     */
    @NonNull
    public Capabilities getCapabilities();
}