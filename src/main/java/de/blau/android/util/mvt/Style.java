package de.blau.android.util.mvt;

import java.io.Serializable;

import android.graphics.Paint;
import android.graphics.Path;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.graphics.Paint.FontMetrics;
import de.blau.android.resources.DataStyle;
import de.blau.android.util.SerializablePaint;

public class Style implements Serializable {

    private static final long serialVersionUID = 4L;

    private int                   minZoom = 0;
    private int                   maxZoom = -1;
    private SerializablePaint     pointPaint;
    private SerializablePaint     linePaint;
    private SerializablePaint     polygonPaint;
    private String                symbolName;
    private transient Path        symbolPath;
    private String                labelKey;
    private SerializablePaint     labelPaint;
    private transient FontMetrics labelFontMetrics;
    private SerializablePaint     labelBackground;
    float                         labelStrokeWidth;

    /**
     * Default constructor
     */
    public Style() {
        // empty
    }

    /**
     * Copy constructor
     * 
     * @param other another Style
     */
    public Style(@NonNull Style other) {
        this.pointPaint = new SerializablePaint(other.pointPaint);
        this.linePaint = new SerializablePaint(other.linePaint);
        this.polygonPaint = new SerializablePaint(other.polygonPaint);
        this.symbolName = other.symbolName;
        this.symbolPath = other.symbolPath;
        this.labelKey = other.labelKey;
        this.labelPaint = new SerializablePaint(other.labelPaint);
        this.labelFontMetrics = other.labelFontMetrics;
        this.labelBackground = new SerializablePaint(other.labelBackground);
        this.labelStrokeWidth = other.labelStrokeWidth;
    }

    /**
     * Create a rudimentary style from Paint objects for the geometries and labels
     * 
     * @param paint the Paint to use for the geometries
     * @param labelPaint the Paint to use for labels
     * @param symbolPath a Path to use for point symbols
     * @return a Style
     */
    @NonNull
    public static Style FromPaint(@NonNull Paint paint, @NonNull Paint labelPaint, @Nullable String symbolName) {
        Style style = new Style();
        style.pointPaint = new SerializablePaint(paint);
        style.pointPaint.setStyle(Paint.Style.STROKE);
        style.symbolName = symbolName;
        setSymbolPathFromName(style);
        style.linePaint = new SerializablePaint(labelPaint);
        style.linePaint.setAntiAlias(true);
        style.linePaint.setStyle(Paint.Style.STROKE);
        style.polygonPaint = new SerializablePaint(paint);
        style.polygonPaint.setAntiAlias(true);
        style.polygonPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        style.labelPaint = new SerializablePaint(labelPaint);
        style.labelFontMetrics = labelPaint.getFontMetrics();
        style.labelBackground = new SerializablePaint(DataStyle.getInternal(DataStyle.LABELTEXT_BACKGROUND).getPaint());
        style.labelStrokeWidth = labelPaint.getStrokeWidth();
        return style;
    }

    /**
     * Set the symbol Path from the name
     * 
     * @param style the style
     */
    public static void setSymbolPathFromName(@NonNull Style style) {
        Path symbolPath = DataStyle.getCurrent().getSymbol(style.symbolName);
        style.symbolPath = symbolPath != null ? symbolPath : null;
    }

    /**
     * @return the minZoom
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
     * @param minZoom the minZoom to set
     */
    public void setMinZoom(int minZoom) {
        this.minZoom = minZoom;
    }

    /**
     * @return the maxZoom
     */
    public int getMaxZoom() {
        return maxZoom;
    }

    /**
     * @param maxZoom the maxZoom to set
     */
    public void setMaxZoom(int maxZoom) {
        this.maxZoom = maxZoom;
    }

    /**
     * @return the pointPaint
     */
    public SerializablePaint getPointPaint() {
        return pointPaint;
    }

    /**
     * @param pointPaint the pointPaint to set
     */
    public void setPointPaint(SerializablePaint pointPaint) {
        this.pointPaint = pointPaint;
    }

    /**
     * @return the linePaint
     */
    public SerializablePaint getLinePaint() {
        return linePaint;
    }

    /**
     * @param linePaint the linePaint to set
     */
    public void setLinePaint(SerializablePaint linePaint) {
        this.linePaint = linePaint;
    }

    /**
     * @return the polygonPaint
     */
    public SerializablePaint getPolygonPaint() {
        return polygonPaint;
    }

    /**
     * @param polygonPaint the polygonPaint to set
     */
    public void setPolygonPaint(SerializablePaint polygonPaint) {
        this.polygonPaint = polygonPaint;
    }

    /**
     * @return the symbolName
     */
    public String getSymbolName() {
        return symbolName;
    }

    /**
     * @param symbolName the symbolName to set
     */
    public void setSymbolName(String symbolName) {
        this.symbolName = symbolName;
    }

    /**
     * @return the symbolPath
     */
    public Path getSymbolPath() {
        return symbolPath;
    }

    /**
     * @param symbolPath the symbolPath to set
     */
    public void setSymbolPath(Path symbolPath) {
        this.symbolPath = symbolPath;
    }

    /**
     * @return the labelKey
     */
    public String getLabelKey() {
        return labelKey;
    }

    /**
     * @param labelKey the labelKey to set
     */
    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    /**
     * @return the labelPaint
     */
    public SerializablePaint getLabelPaint() {
        return labelPaint;
    }

    /**
     * @param labelPaint the labelPaint to set
     */
    public void setLabelPaint(SerializablePaint labelPaint) {
        this.labelPaint = labelPaint;
        this.labelFontMetrics = labelPaint.getFontMetrics();
    }

    /**
     * Get the FontMetrics for labels
     * 
     * @return FontMetrics
     */
    @Nullable
    public FontMetrics getLabelFontMetrics() {
        if (labelFontMetrics == null) {
            labelFontMetrics = labelPaint.getFontMetrics();
        }
        return labelFontMetrics;
    }

    /**
     * @return the labelBackground
     */
    public SerializablePaint getLabelBackground() {
        return labelBackground;
    }

    /**
     * @param labelBackground the labelBackground to set
     */
    public void setLabelBackground(SerializablePaint labelBackground) {
        this.labelBackground = labelBackground;
    }
}
