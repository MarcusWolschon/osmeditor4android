package io.vespucci.presets;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import ch.poole.poparser.Po;

public class PresetItemSeparatorField extends PresetFormattingField implements FieldHeight {
    private static final long serialVersionUID = 1L;

    private int height;

    /**
     * Construct a new instance
     * 
     * @param height in pixels
     */
    PresetItemSeparatorField(int height) {
        super();
        this.height = height;
    }

    PresetItemSeparatorField(@NonNull PresetItemSeparatorField field) {
        super(field);
        height = field.height;
    }

    @Override
    public PresetField copy() {
        return new PresetItemSeparatorField(this);
    }

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", PresetParser.ITEM_SEPARATOR);
        s.attribute("", PresetParser.HEIGHT_ATTR, Integer.toString(height));
        s.endTag("", PresetParser.ITEM_SEPARATOR);
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
