package de.blau.android.util;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetItem;

public class StringWithDescriptionAndIcon extends StringWithDescription {
    private static final long serialVersionUID = 1L;

    private transient Drawable icon;
    private final String       iconPath;

    /**
     * Construct a new instance
     * 
     * @param value the value
     * @param iconPath the path of the associated icon
     */
    public StringWithDescriptionAndIcon(@NonNull final String value, @NonNull final String iconPath) {
        super(value);
        this.iconPath = iconPath;
    }

    /**
     * Construct a new instance
     * 
     * @param value the value
     * @param description the description of the value
     * @param iconPath the path of the associated icon
     */
    public StringWithDescriptionAndIcon(@NonNull final String value, @Nullable final String description, @NonNull final String iconPath) {
        super(value, description);
        this.iconPath = iconPath;
    }

    /**
     * Construct a new instance from object of a known type
     * 
     * @param object one of StringWithDescriptionAndIcon, StringWithDescription, ValueWIihCOunt or String
     */
    public StringWithDescriptionAndIcon(@NonNull final Object object) {
        super(object);
        if (object instanceof StringWithDescriptionAndIcon) {
            this.iconPath = ((StringWithDescriptionAndIcon) object).iconPath;
            this.icon = ((StringWithDescriptionAndIcon) object).icon;
        } else {
            iconPath = null;
        }
    }

    /**
     * Get the Icon
     * 
     * @param preset the current PresetItem
     * @return a Drawable with the icon
     */
    @Nullable
    public Drawable getIcon(PresetItem preset) {
        if (icon == null) {
            icon = preset.getIconIfExists(iconPath);
            if (icon != null) {
                Bitmap bitmap = Util.drawableToBitmap(icon);
                int size = Density.dpToPx(PresetElement.ICON_SIZE_DP);
                icon = new BitmapDrawable(App.resources(), Bitmap.createScaledBitmap(bitmap, size, size, true));
            }
        }
        return icon;
    }
}
