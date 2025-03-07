package de.blau.android.presets;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import ch.poole.poparser.Po;

public class PresetSpaceField extends PresetFormattingField implements FieldHeight {
    private static final long serialVersionUID = 2L;

    private int height;

    /**
     * Construct a new instance
     * 
     * @param height in pixels
     */
    PresetSpaceField(int height) {
        super();
        this.height = height;
    }

    PresetSpaceField(@NonNull PresetSpaceField field) {
        super(field);
        height = field.height;
    }

    @Override
    public PresetField copy() {
        return new PresetSpaceField(this);
    }

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", PresetParser.SPACE);
        s.attribute("", PresetParser.HEIGHT_ATTR, Integer.toString(height));
        s.endTag("", PresetParser.SPACE);
    }

    @Override
    public void translate(Po po) {
        // nothing to translate
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }
}
