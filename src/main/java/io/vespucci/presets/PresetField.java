package io.vespucci.presets;

import java.io.IOException;
import java.io.Serializable;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.poparser.Po;

public abstract class PresetField extends Regionalizable implements Serializable {

    private static final long serialVersionUID = 3L;

    /**
     * Translation contexts
     */
    protected String textContext;

    /**
     * Is this field is optional
     */
    protected boolean optional = false;

    /**
     * Colour value for the background
     */
    private int background = 0;

    /**
     * Construct a new PresetField
     * 
     * @param key the key
     */
    protected PresetField() {
        super();
    }

    /**
     * Copy constructor
     * 
     * @param field PresetField to copy
     */
    protected PresetField(@NonNull PresetField field) {
        super(field);
        this.textContext = field.textContext;
        this.optional = field.optional;
        this.background = field.background;
    }

    /**
     * 
     * Method that creates a copy of the element
     * 
     * @return a PresetField instance
     */
    public abstract PresetField copy();

    /**
     * Set the text translation context
     * 
     * @param textContext the translation context
     */
    public void setTextContext(@Nullable String textContext) {
        this.textContext = textContext;
    }

    /**
     * Get the text translation context
     * 
     * @return the textContext
     */
    @Nullable
    String getTextContext() {
        return textContext;
    }

    /**
     * @return true if optional
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * @param optional make this field optional
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * @return the background colour
     */
    public int getBackgroundColour() {
        return background;
    }

    /**
     * @param backgroundColour the colour to set
     */
    public void setBackgroundColour(int backgroundColour) {
        this.background = backgroundColour;
    }

    /**
     * Translate the translatable parts of this PresetField
     * 
     * Note this cannot be undone
     * 
     * @param po the object holding the translations
     */
    public abstract void translate(@NonNull Po po);

    /**
     * Translate a String
     * 
     * @param text the text to translate
     * @param po the translation object
     * @param context a translation context of null
     * @return the translated String
     */
    String translate(@NonNull String text, @NonNull Po po, @Nullable String context) {
        return context != null ? po.t(context, text) : po.t(text);
    }

    /**
     * Output the field in XML format
     * 
     * @param s an XmlSerialzer instance
     * @throws IllegalArgumentException if the serializer encountered an illegal argument
     * @throws IllegalStateException if the serializer detects an illegal state
     * @throws IOException if writing to the serializer fails
     */
    public abstract void toXml(@NonNull XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException;
}
