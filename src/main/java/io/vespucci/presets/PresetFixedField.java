package io.vespucci.presets;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.poparser.Po;
import io.vespucci.osm.Tags;
import io.vespucci.util.StringWithDescription;

public class PresetFixedField extends PresetTagField {
    private static final long serialVersionUID = 2L;
    
    private final StringWithDescription value;
    private Boolean                     isObject;

    /**
     * Constructor
     * 
     * @param key the key
     * @param value the value
     */
    public PresetFixedField(@NonNull String key, @NonNull StringWithDescription value) {
        super(key);
        this.value = value;
    }

    /**
     * Copy constructor
     * 
     * @param field PresetFixedField to copy
     */
    public PresetFixedField(@NonNull PresetFixedField field) {
        super(field);
        this.value = field.value;
    }

    /**
     * Get the value of this field
     * 
     * @return the fixed value
     */
    @NonNull
    public StringWithDescription getValue() {
        return value;
    }

    /**
     * Set if this is an object or not, null undefined
     * 
     * @param isObject if true / false this is an object or not overriding other settings
     */
    public void setIsObject(@Nullable Boolean isObject) {
        this.isObject = isObject;
    }

    /**
     * Check if this tag defines an object
     * 
     * @param objectKeys List of keys considered to be objects
     * @return true if an object
     */
    public boolean isObject(@NonNull List<String> objectKeys) {
        return isObject != null ? isObject : objectKeys.contains(key) || Tags.IMPORTANT_TAGS.contains(key);
    }

    @Override
    public PresetTagField copy() {
        return new PresetFixedField(this);
    }

    @Override
    public void translate(@NonNull Po po) {
        super.translate(po);
        if (value.getDescription() != null) {
            value.setDescription(translate(value.getDescription(), po, getValueContext()));
        }
    }

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", PresetParser.KEY_ATTR);
        s.attribute("", PresetParser.KEY_ATTR, key);
        StringWithDescription v = getValue();
        s.attribute("", PresetParser.VALUE, v.getValue());
        String description = v.getDescription();
        if (description != null && !"".equals(description)) {
            s.attribute("", PresetParser.TEXT, description);
        }
        if (isObject != null) {
            s.attribute("", PresetParser.OBJECT, Boolean.toString(isObject));
        }
        s.endTag("", PresetParser.KEY_ATTR);
    }

    @Override
    public String toString() {
        return super.toString() + " value: " + value;
    }
}
