package io.vespucci.presets;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;

/**
 * Represents a separator in a preset group
 */
public class PresetSeparator extends PresetElement {
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new separator
     * 
     * @param preset the Preset this belongs to
     * @param parent the parent PresetGroup
     */
    public PresetSeparator(@NonNull Preset preset, PresetGroup parent) {
        super(preset, parent, "", null);
    }

    @Override
    public View getView(Context ctx, PresetClickHandler handler, boolean selected) {
        View v = new View(ctx);
        v.setMinimumHeight(1);
        v.setMinimumWidth(99999); // for WrappingLayout
        // this seems to be necessary to work around
        // https://issuetracker.google.com/issues/37003658
        v.setLayoutParams(new LinearLayout.LayoutParams(99999, 1));
        v.setSaveEnabled(false);
        return v;
    }

    @Override
    public void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", PresetParser.SEPARATOR);
        s.endTag("", PresetParser.SEPARATOR);
    }
}
