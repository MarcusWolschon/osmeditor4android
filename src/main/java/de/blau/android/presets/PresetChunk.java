package de.blau.android.presets;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.StringWithDescription;

public class PresetChunk extends PresetItem {

    private static final long serialVersionUID = 2L;

    private List<StringWithDescription> listValues = null;

    /**
     * Construct a new PresetChunk
     * 
     * @param preset the Preset this belongs to
     * @param parent parent group (or null if this is the root group)
     * @param name name of the element or null
     * @param iconpath the icon path (either "http://" URL or "presets/" local image reference) or null
     * @param types comma separated list of types of OSM elements this applies to or null for all
     */
    public PresetChunk(@NonNull Preset preset, @Nullable PresetGroup parent, @Nullable String name, @Nullable String iconpath, @Nullable String types) {
        super(preset, parent, name, iconpath, types);
    }

    /**
     * Get the current list values
     * 
     * @return the list values
     */
    @Nullable
    List<StringWithDescription> getListValues() {
        return listValues;
    }

    /**
     * Set the current list values
     * 
     * @param listValues the list values
     */
    void setListValues(@Nullable List<StringWithDescription> listValues) {
        this.listValues = listValues;
    }

    /**
     * This is typically never used as chunks will be expanded
     */
    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", PresetParser.CHUNK);
        if (getListValues() != null) {
            PresetComboField.valuesToXml(s, getListValues().toArray(new StringWithDescription[getListValues().size()]));
        } else {
            itemToXml(s);
        }
        s.endTag("", PresetParser.CHUNK);
    }
}
