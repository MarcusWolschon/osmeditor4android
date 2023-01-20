package de.blau.android.presets;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.StringWithDescription;

public class PresetChunk extends PresetItem {

    List<StringWithDescription> listValues = null;

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

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", Preset.CHUNK);
        itemToXml(s);
        s.endTag("", Preset.CHUNK);
    }
}
