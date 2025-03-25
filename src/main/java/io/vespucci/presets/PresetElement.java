package io.vespucci.presets;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.osm.OsmElement.ElementType;

/**
 * Represents an element (group or item) in a preset data structure
 */
public abstract class PresetElement extends Regionalizable implements Serializable {

    private static final long serialVersionUID = 2L;

    private static final int VIEW_PADDING     = 4;
    private static final int VIEW_SIDE_LENGTH = 72;
    public static final int  ICON_SIZE_DP     = 36;

    protected final Preset   preset;
    String                   name;
    String                   nameContext = null;
    private String           iconpath;
    transient Drawable       icon;
    transient BitmapDrawable mapIcon;
    private String           imagePath;
    PresetGroup              parent;
    boolean                  appliesToWay;
    boolean                  appliesToNode;
    boolean                  appliesToClosedway;
    boolean                  appliesToRelation;
    boolean                  appliesToArea;
    private boolean          deprecated  = false;
    private String           mapFeatures;

    /**
     * Creates the element, setting parent, name and icon, and registers with the parent
     * 
     * @param preset the Preset this belongs to
     * @param parent parent ParentGroup (or null if this is the root group)
     * @param name name of the element or null
     * @param iconpath the icon path (either "http://" URL or "presets/" local image reference) or null
     */
    protected PresetElement(@NonNull Preset preset, @Nullable PresetGroup parent, @Nullable String name, @Nullable String iconpath) {
        this.preset = preset;
        this.parent = parent;
        this.name = name;
        this.iconpath = iconpath;
        icon = null;
        mapIcon = null;
        if (parent != null) {
            parent.addElement(this);
        }
    }

    /**
     * Construct a new PresetElement in this preset from an existing one
     * 
     * @param preset the Preset this belongs to
     * @param group PresetGroup this should be added, null if none
     * @param item the PresetElement to copy
     */
    protected PresetElement(@NonNull Preset preset, @Nullable PresetGroup group, @NonNull PresetElement item) {
        super(item);
        this.preset = preset;
        this.name = item.name;
        if (group != null) {
            group.addElement(this);
        }
        this.iconpath = item.iconpath;
        icon = null;
        mapIcon = null;
        if (item.appliesToNode) {
            setAppliesToNode();
        }
        if (item.appliesToWay) {
            setAppliesToWay();
        }
        if (item.appliesToClosedway) {
            setAppliesToClosedway();
        }
        if (item.appliesToArea) {
            setAppliesToArea();
        }
        if (item.appliesToRelation) {
            setAppliesToRelation();
        }
        this.deprecated = item.deprecated;
        this.mapFeatures = item.mapFeatures;
    }

    /**
     * Get the name of this element
     * 
     * @return the name if set or if null an empty String
     */
    @NonNull
    public String getName() {
        return name != null ? name : "";
    }

    /**
     * Return the name of this preset element, potentially translated
     * 
     * @return the name
     */
    @NonNull
    public String getTranslatedName() {
        return preset.translate(name, nameContext);
    }

    /**
     * Return the name of this preset element, potentially translated and including if it is deprecated
     * 
     * @param ctx and Android Context
     * @return the name
     */
    @NonNull
    public String getDisplayName(@NonNull Context ctx) {
        return deprecated ? ctx.getString(R.string.deprecated, getTranslatedName()) : getTranslatedName();
    }

    /**
     * Return the icon for the preset or a place holder
     * 
     * The icon is cached in the preset
     * 
     * @param context an Android Context
     * @return a Drawable with the icon or a place holder for it
     */
    @NonNull
    public Drawable getIcon(@NonNull Context context) {
        if (icon == null) {
            icon = getIcon(context, iconpath, (int) (ICON_SIZE_DP * App.getConfiguration().fontScale));
        }
        return icon;
    }

    /**
     * Return the icon for the preset or a place holder in a specific size
     * 
     * @param context an Android Context
     * @param size in DIP
     * @return a Drawable with the icon or a place holder for it
     */
    @NonNull
    public Drawable getIcon(@NonNull Context context, int size) {
        return getIcon(context, iconpath, (int) (size * App.getConfiguration().fontScale));
    }

    /**
     * Return the icon from the preset or a place holder
     * 
     * @param context an Android Context
     * @param path path to the icon
     * @param iconSize size of the sides of the icon in DP
     * @return a Drawable with the icon or a place holder for it
     */
    @NonNull
    private Drawable getIcon(@NonNull Context context, @Nullable String path, int iconSize) {
        if (path != null) {
            return preset.getIconManager(context).getDrawableOrPlaceholder(path, iconSize);
        } else {
            return preset.getIconManager(context).getPlaceHolder(iconSize);
        }
    }

    /**
     * Return the icon from the preset if it exists
     * 
     * @param context an Android Context
     * @param path path to the icon
     * @return a Drawable with the icon or or null if it can't be found
     */
    @Nullable
    public Drawable getIconIfExists(@NonNull Context context, @Nullable String path) {
        if (path != null) {
            return preset.getIconManager(context).getDrawable(path, ICON_SIZE_DP);
        }
        return null;
    }

    /**
     * Get an icon suitable for drawing on the map
     * 
     * @param context an Android Context
     * @return a small icon
     */
    @Nullable
    public BitmapDrawable getMapIcon(@NonNull Context context) {
        if (mapIcon == null && iconpath != null) {
            mapIcon = preset.getIconManager(context).getDrawable(iconpath, io.vespucci.Map.ICON_SIZE_DP);
        }
        return mapIcon;
    }

    /**
     * Remove the references to the icons
     */
    public void clearIcons() {
        icon = null;
        mapIcon = null;
    }

    /**
     * Set the path for a large image
     * 
     * @param imagePath the path
     */
    public void setImage(@Nullable String imagePath) {
        this.imagePath = imagePath;
    }

    /**
     * Get the path for a large image
     * 
     * @return the image path or null
     */
    @Nullable
    public String getImage() {
        return this.imagePath;
    }

    /**
     * Get the parent of this PresetElement
     * 
     * @return the parent PresetGroup or null if none
     */
    @Nullable
    public PresetGroup getParent() {
        return parent;
    }

    /**
     * Set the parent PresetGroup
     * 
     * @param pg the parent to set
     */
    public void setParent(@Nullable PresetGroup pg) {
        parent = pg;
    }

    /**
     * Returns a basic view representing the current element (i.e. a button with icon and name). Can (and should) be
     * used when implementing {@link #getView(PresetClickHandler)}.
     * 
     * @param ctx Android Context
     * @param selected if true highlight the background
     * @return the view
     */
    protected TextView getBaseView(@NonNull Context ctx, boolean selected) {
        Resources res = ctx.getResources();
        TextView v = new TextView(ctx);
        float density = res.getDisplayMetrics().density;
        v.setText(getTranslatedName());
        v.setTextColor(ContextCompat.getColor(ctx, R.color.preset_text));
        v.setMaxLines(3);
        TextSize.setIconTextSize(v);
        v.setEllipsize(TextUtils.TruncateAt.END);
        float scale = App.getConfiguration().fontScale * density;
        float padding = VIEW_PADDING * scale;
        v.setPadding((int) padding, (int) padding, (int) padding, (int) padding);
        Drawable viewIcon = getIcon(ctx);
        v.setCompoundDrawables(null, viewIcon, null, null);
        // this seems to be necessary to work around
        // https://issuetracker.google.com/issues/37003658

        float sideLength = VIEW_SIDE_LENGTH * scale;
        v.setLayoutParams(new LinearLayout.LayoutParams((int) sideLength, (int) sideLength));
        v.setWidth((int) sideLength);
        v.setHeight((int) sideLength);
        v.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        v.setSaveEnabled(false);
        return v;
    }

    /**
     * Returns a view representing this element (i.e. a button with icon and name) Implement this in subtypes
     * 
     * @param ctx Android Context
     * @param handler handler to handle clicks on the element (may be null)
     * @param selected highlight this element
     * @return a view ready to display to represent this element
     */
    public abstract View getView(@NonNull Context ctx, @Nullable final PresetClickHandler handler, boolean selected);

    /**
     * Test what kind of elements this PresetElement applies to
     * 
     * @param type the ElementType to check for
     * @return true if applicable
     */
    public boolean appliesTo(@Nullable ElementType type) {
        if (type == null) {
            return true;
        }
        switch (type) {
        case NODE:
            return appliesToNode;
        case WAY:
            return appliesToWay;
        case CLOSEDWAY:
            return appliesToClosedway;
        case RELATION:
            return appliesToRelation;
        case AREA:
            return appliesToArea;
        default:
            return true;
        }
    }

    /**
     * Get a list of ElementTypes this PresetItem applies to
     * 
     * @return a List of ElementType
     */
    @NonNull
    public List<ElementType> appliesTo() {
        List<ElementType> result = new ArrayList<>();
        if (appliesToNode) {
            result.add(ElementType.NODE);
        }
        if (appliesToWay) {
            result.add(ElementType.WAY);
        }
        if (appliesToClosedway) {
            result.add(ElementType.CLOSEDWAY);
        }
        if (appliesToRelation) {
            result.add(ElementType.RELATION);
        }
        if (appliesToArea) {
            result.add(ElementType.AREA);
        }
        return result;
    }

    /**
     * Recursively sets the flag indicating that this element is relevant for nodes
     */
    void setAppliesToNode() {
        if (!appliesToNode) {
            appliesToNode = true;
            if (parent != null) {
                parent.setAppliesToNode();
            }
        }
    }

    /**
     * Recursively sets the flag indicating that this element is relevant for nodes
     */
    void setAppliesToWay() {
        if (!appliesToWay) {
            appliesToWay = true;
            if (parent != null) {
                parent.setAppliesToWay();
            }
        }
    }

    /**
     * Recursively sets the flag indicating that this element is relevant for nodes
     */
    void setAppliesToClosedway() {
        if (!appliesToClosedway) {
            appliesToClosedway = true;
            if (parent != null) {
                parent.setAppliesToClosedway();
            }
        }
    }

    /**
     * Recursively sets the flag indicating that this element is relevant for relations
     */
    void setAppliesToRelation() {
        if (!appliesToRelation) {
            appliesToRelation = true;
            if (parent != null) {
                parent.setAppliesToRelation();
            }
        }
    }

    /**
     * Recursively sets the flag indicating that this element is relevant for an area
     */
    void setAppliesToArea() {
        if (!appliesToArea) {
            appliesToArea = true;
            if (parent != null) {
                parent.setAppliesToArea();
            }
        }
    }

    /**
     * Set the OSM wiki (or other) documentation URL for this PresetElement
     * 
     * @param url the URL to set
     */
    public void setMapFeatures(@Nullable String url) {
        if (url != null) {
            mapFeatures = url;
        }
    }

    /**
     * Get the documentation URL (typically from the OSM wiki) for this PresetELement
     * 
     * @return a String containing the full or partial URL for the page
     */
    @Nullable
    public String getMapFeatures() {
        return mapFeatures;
    }

    /**
     * Set the translation context for the name field of this PresetElement
     * 
     * @param context the translation context
     */
    void setNameContext(@Nullable String context) {
        nameContext = context;
    }

    /**
     * Check if the deprecated flag is set
     * 
     * @return true if the PresetELement is deprecated
     */
    public boolean isDeprecated() {
        return deprecated;
    }

    /**
     * Set the deprecated flag
     * 
     * @param deprecated the value to set
     */
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    /**
     * Get an object documenting where in the hierarchy this element is.
     * 
     * This is essentially the only unique way of identifying a specific preset
     * 
     * @param root PresetGroup that this is relative to
     * @return an object containing the path elements
     */
    @Nullable
    public PresetElementPath getPath(@NonNull PresetGroup root) {
        for (PresetElement e : new ArrayList<>(root.getElements())) { // prevent CCME
            if (e.equals(this)) {
                PresetElementPath result = new PresetElementPath();
                result.getPath().add(e.getName());
                return result;
            } else {
                if (e instanceof PresetGroup) {
                    PresetElementPath result = getPath((PresetGroup) e);
                    if (result != null) {
                        result.getPath().add(0, e.getName());
                        return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @return the iconpath
     */
    public String getIconpath() {
        return iconpath;
    }

    /**
     * Get the Preset object this is an element of
     * 
     * @return the Preset
     */
    @NonNull
    public Preset getPreset() {
        return preset;
    }

    /**
     * Filter a List of PresetElement by region
     * 
     * @param elements the input PresetElements
     * @param regions a list of regions
     * @return a List of PresetElement, potentially empty
     */
    @NonNull
    protected static List<PresetElement> filterElementsByRegion(@NonNull List<PresetElement> elements, @Nullable List<String> regions) {
        List<PresetElement> result = new ArrayList<>();
        for (PresetElement pe : elements) {
            if (pe.appliesIn(regions)) {
                result.add(pe);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return name + " " + iconpath + " " + appliesToWay + " " + appliesToNode + " " + appliesToClosedway + " " + appliesToRelation + " " + appliesToArea;
    }

    /**
     * Serialize the element to XML
     * 
     * @param s the XmlSerializer
     * @throws IllegalArgumentException if the serializer encountered an illegal argument
     * @throws IllegalStateException if the serializer detects an illegal state
     * @throws IOException if writing to the serializer fails
     */
    public abstract void toXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException;
}