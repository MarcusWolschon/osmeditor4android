package io.vespucci.util;

import java.io.IOException;
import java.io.Serializable;

import android.graphics.Paint;
import android.text.TextPaint;
import androidx.annotation.NonNull;

/**
 * Serialize and restore some properties of a Paint object
 * 
 * BlendMode not saved
 * 
 * color saved
 * 
 * ColorFilter not saved
 * 
 * elegant height metrics flag not saved
 * 
 * endHyphen not saved flags
 * 
 * saved font feature settings not saved
 * 
 * TrueType or OpenType font variation settings not saved
 * 
 * hinting mode not saved
 * 
 * letter-spacing for text saved
 * 
 * MaskFilter not saved
 * 
 * PathEffect not saved
 * 
 * Shader not saved
 * 
 * shadow layer saved
 * 
 * hyphen edit start not saved
 * 
 * Paint.Cap saved
 * 
 * Paint.Join saved
 * 
 * miter saved
 * 
 * stroke width saved
 * 
 * Paint.Style saved
 * 
 * subpixelText not saved
 * 
 * text locale not saved
 * 
 * text scale X not saved
 * 
 * textSize saved
 * 
 * text align saved
 * 
 * text skew X not saved
 * 
 * Typeface not saved
 * 
 * underline Text not saved
 * 
 * word spacing not saved
 * 
 * Xfermode not saves
 * 
 * @author Simon
 *
 */
public class SerializableTextPaint extends TextPaint implements Serializable {

    private static final long serialVersionUID = 1L;

    private float shadowRadius;
    private float shadowDx;
    private float shadowDy;
    private int   shadowColor;

    /**
     * Default constructor
     */
    public SerializableTextPaint() {
        super();
    }

    /**
     * Copy constructor
     * 
     * @param paint the Paint to copy
     */
    public SerializableTextPaint(@NonNull SerializableTextPaint paint) {
        super(paint);
    }

    /**
     * Copy constructor
     * 
     * @param paint the Paint to copy
     */
    public SerializableTextPaint(@NonNull Paint paint) {
        super(paint);
    }

    @Override
    public void setShadowLayer(float radius, float dx, float dy, int shadowColor) {
        shadowRadius = radius;
        shadowDx = dx;
        shadowDy = dy;
        this.shadowColor = shadowColor;
        super.setShadowLayer(radius, dx, dy, shadowColor);
    }

    /**
     * Get the shadow layer radius
     * 
     * Older Android version don't have this
     * 
     * @return the radius
     */
    public float getShadowLayerRadius() { // NOSONAR
        return shadowRadius;
    }

    /**
     * Get the shadow layer horizontal offset
     * 
     * Older Android version don't have this
     * 
     * @return the horizontal offset
     */
    public float getShadowLayerDx() { // NOSONAR
        return shadowDx;
    }

    /**
     * Get the shadow layer vertical offset
     * 
     * Older Android version don't have this
     * 
     * @return the vertical offset
     */
    public float getShadowLayerDy() { // NOSONAR
        return shadowDy;
    }

    /**
     * Get the shadow layer color
     * 
     * Older Android version don't have this
     * 
     * @return the color
     */
    public int getShadowLayerColor() { // NOSONAR
        return shadowColor;
    }

    /**
     * Serialize this object
     * 
     * @param out ObjectOutputStream to write to
     * @throws IOException if writing fails
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(getFlags());
        out.writeInt(getColor());
        out.writeFloat(getStrokeWidth());
        out.writeObject(getStyle());
        out.writeObject(getStrokeCap());
        out.writeObject(getStrokeJoin());
        out.writeFloat(getStrokeMiter());
        out.writeFloat(getTextSize());
        out.writeObject(getTextAlign());
        out.writeFloat(shadowRadius);
        out.writeFloat(shadowDx);
        out.writeFloat(shadowDy);
        out.writeInt(shadowColor);
        out.writeFloat(getLetterSpacing());
        out.writeInt(baselineShift);
        out.writeInt(bgColor);
        out.writeFloat(density);
        out.writeObject(drawableState);
        out.writeInt(linkColor);
    }

    /**
     * Recreate the object for serialized state
     * 
     * @param in ObjectInputStream to write from
     * @throws IOException if reading fails
     * @throws ClassNotFoundException a problem in the serialized data occurred
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        setFlags(in.readInt());
        setColor(in.readInt());
        setStrokeWidth(in.readFloat());
        setStyle((Style) in.readObject());
        setStrokeCap((Cap) in.readObject());
        setStrokeJoin((Join) in.readObject());
        setStrokeMiter(in.readFloat());
        setTextSize(in.readFloat());
        setTextAlign((Align) in.readObject());
        shadowRadius = in.readFloat();
        shadowDx = in.readFloat();
        shadowDy = in.readFloat();
        shadowColor = in.readInt();
        if (shadowRadius > 0) {
            super.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
        }
        setLetterSpacing(in.readFloat());
        baselineShift = in.readInt();
        bgColor = in.readInt();
        density = in.readFloat();
        drawableState = (int[]) in.readObject();
        linkColor = in.readInt();
    }
}
