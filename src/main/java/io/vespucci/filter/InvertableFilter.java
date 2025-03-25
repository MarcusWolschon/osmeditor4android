package io.vespucci.filter;

/**
 * Common methods for inverting a Filter
 * 
 * @author simon
 *
 */
public abstract class InvertableFilter extends Filter {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected boolean inverted = false;

    /**
     * @return is the filter inverted?
     */
    public boolean isInverted() {
        return inverted;
    }

    /**
     * Invert the filter
     * 
     * @param inverted invert the filter if true
     */
    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }
}
