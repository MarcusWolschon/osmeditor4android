package de.blau.android.presets;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.poparser.Po;

public class PresetLabelField extends PresetField {

    private String label;

    /**
     * Construct a new instance
     * 
     * @param label the label
     * @param textContext optional translation context
     */
    PresetLabelField(@NonNull String label, @Nullable String textContext) {
        super();
        this.label = label;
        setTextContext(textContext);
    }

    /**
     * Get the label string
     * 
     * @return the label
     */
    @NonNull
    public String getLabel() {
        return label;
    }

    @Override
    public void translate(Po po) {
        label = translate(label, po, getTextContext());
    }

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", PresetParser.LABEL);
        s.attribute("", PresetParser.TEXT, label);
        s.endTag("", PresetParser.LABEL);
    }
}
