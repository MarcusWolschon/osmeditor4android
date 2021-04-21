package de.blau.android.util;

import java.io.IOException;
import java.io.Serializable;

import android.graphics.Paint;
import androidx.annotation.NonNull;

/**
 * Serialize and restore some properties of a Paint object
 * 
 * BlendMode not saved color saved ColorFilter not saved elegant height metrics flag not saved endHyphen not saved flags
 * saved font feature settings not saved TrueType or OpenType font variation settings not saved hinting mode not saved
 * letter-spacing for text not saved MaskFilter not saved PathEffect not saved Shader not saved shadow layer not saved
 * hyphen edit start not saved Paint.Cap saved Paint.Join saved miter saved stroke width saved Paint.Style saved
 * subpixelText not saved text locale not saved text scale X not saved textSize not saved text skew X not saved Typeface
 * not saved underline Text not saved word spacing not saved Xfermode not saves
 * 
 * @author Simon
 *
 */
public class SerializablePaint extends Paint implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Copy constructor
     * 
     * @param paint the Paint to copy
     */
    public SerializablePaint(@NonNull SerializablePaint paint) {
        super(paint);
    }

    /**
     * Copy constructor
     * 
     * @param paint the Paint to copy
     */
    public SerializablePaint(@NonNull Paint paint) {
        super(paint);
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
    }

    /**
     * Recreate the object for serialized state
     * 
     * @param in ObjectInputStream to write from
     * @throws IOException if reading fails
     * @throws ClassNotFoundException a problem in the serialized data occured
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        setFlags(in.readInt());
        setColor(in.readInt());
        setStrokeWidth(in.readFloat());
        setStyle((Style) in.readObject());
        setStrokeCap((Cap) in.readObject());
        setStrokeJoin((Join) in.readObject());
        setStrokeMiter(in.readFloat());
    }
}
