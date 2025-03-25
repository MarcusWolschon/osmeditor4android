package io.vespucci.osm;

import androidx.annotation.Nullable;
import io.vespucci.resources.DataStyle.FeatureStyle;

public abstract class StyledOsmElement extends OsmElement implements StyleableFeature{

    private static final long serialVersionUID = 1L;

    /**
     * Construct a new styled osm element
     * 
     * @param osmId the id
     * @param osmVersion version
     * @param timestamp timestamp
     * @param state state
     */
    StyledOsmElement(long osmId, long osmVersion, long timestamp, byte state) {
        super(osmId, osmVersion, timestamp, state);
    }

    protected transient FeatureStyle style = null; // FeatureProfile is currently not serializable
    
    @Override
    void updateState(final byte newState) {
        style = null; // force recalc of style
        super.updateState(newState);
    }

    @Override
    void setState(final byte newState) {
        style = null; // force recalc of style
        super.setState(newState);
    }

    @Override
    public FeatureStyle getStyle() {
        return style;
    }

    @Override
    public void setStyle(@Nullable FeatureStyle fp) {
        style = fp;
    }
}
