package io.vespucci;

import java.util.List;
import java.util.WeakHashMap;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.vespucci.R;
import io.vespucci.dialogs.ElementInfo;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.ViewBox;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetItem;
import io.vespucci.resources.DataStyle.FeatureStyle;
import io.vespucci.util.Density;
import io.vespucci.util.ThemeUtils;
import io.vespucci.validation.Validator;

public class PoiListAdapter extends RecyclerView.Adapter<PoiListAdapter.PoiViewHolder> {

    static final int         ROW_MARGIN  = 20;
    private static final int ROW_PADDING = 5;

    private final Context          ctx;
    private final List<OsmElement> elements;

    private final WeakHashMap<java.util.Map<String, String>, BitmapDrawable> iconCache        = new WeakHashMap<>();
    private final WeakHashMap<java.util.Map<String, String>, String>         descriptionCache = new WeakHashMap<>();

    private int                   width;
    private final OnClickListener onClickListener;
    private final int             defaultTextColor;
    private final int             defaultBackgroundColor;
    private final int             iconSize;

    private static final BitmapDrawable NO_ICON = new BitmapDrawable(); // NOSONAR

    public static class PoiViewHolder extends RecyclerView.ViewHolder {
        TextView tv;

        /**
         * Create a new ViewHolder
         *
         * @param v the RadioButton that will be displayed
         */
        public PoiViewHolder(@NonNull View v) {
            super(v);
            tv = (TextView) v;
        }
    }

    /**
     * Create a new adapter
     * 
     * @param ctx an Android Context
     * @param map the current Map instance
     * @param layout the RecyclerView we are adding Pois to
     * @param elements a List of OsmElements to display
     */
    public PoiListAdapter(@NonNull final Context ctx, @NonNull final Map map, @NonNull final RecyclerView layout, @NonNull final List<OsmElement> elements) {
        this.ctx = ctx;
        this.elements = elements;

        int spans = ((GridLayoutManager) layout.getLayoutManager()).getSpanCount();
        width = (layout.getWidth() - ROW_MARGIN) / spans - ROW_MARGIN;

        defaultTextColor = ThemeUtils.getStyleAttribColorValue(ctx, R.attr.textColor, R.color.black);
        defaultBackgroundColor = ThemeUtils.getStyleAttribColorValue(ctx, R.attr.colorSecondary, R.color.ccc_white);

        // note currently the rendering code assumes that the dimensions are in DP not PX
        iconSize = Density.pxToDp(ctx, Math.round(ctx.getResources().getDimension(R.dimen.poi_list_icon_size)));

        onClickListener = clickedView -> {
            OsmElement element = (OsmElement) clickedView.getTag();
            ViewBox box = new ViewBox(element.getBounds());
            double[] elementCenter = box.getCenter();
            // the following maintains the zoom level
            map.getViewBox().moveTo(map, (int) (elementCenter[0] * 1E7D), (int) (elementCenter[1] * 1E7D));
            if (ctx instanceof Main) {
                final Main main = (Main) ctx;
                if (App.getLogic().isLocked()) {
                    map.invalidate();
                    ElementInfo.showDialog(main, element);
                } else {
                    main.edit(element);
                }
            }
            layout.scrollToPosition(0);
        };
    }

    @Override
    public PoiListAdapter.PoiViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView tv = new TextView(ctx);
        tv.setMaxLines(1);
        tv.setPadding(ROW_PADDING, ROW_PADDING, ROW_PADDING, ROW_PADDING);
        tv.setMaxWidth(width);
        tv.setMinWidth(width);
        tv.setMinHeight(iconSize + 2 * ROW_PADDING);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setCompoundDrawablePadding(ROW_MARGIN);
        tv.setOnClickListener(onClickListener);
        return new PoiViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(PoiViewHolder holder, int position) {
        final OsmElement e = elements.get(position);
        holder.tv.setTag(e);

        if (e.hasProblem(ctx, App.getDefaultValidator(ctx)) != Validator.OK) {
            final FeatureStyle validationStyle = App.getDataStyle(ctx).getValidationStyle(e.getCachedProblems());
            int validationColor = validationStyle.getPaint().getColor();
            holder.tv.setBackgroundColor(validationColor);
            holder.tv.setTextColor(validationStyle.getTextColor());
        } else {
            holder.tv.setBackgroundColor(defaultBackgroundColor);
            holder.tv.setTextColor(defaultTextColor);
        }
        BitmapDrawable icon = getIcon(ctx, iconCache, iconSize, e);
        holder.tv.setCompoundDrawables(icon != NO_ICON ? icon : null, null, null, null);

        String description = e.getFromCache(descriptionCache);
        if (description == null) {
            description = e.getDescription(ctx);
            e.addToCache(descriptionCache, description);
        }
        holder.tv.setText(description);
    }

    /**
     * Get an icon for an element caching it if necessary
     * 
     * @param ctx an Android Context
     * @param iconCache the cache
     * @param iconSize the size of the icon
     * @param e the OsmElement
     * @return a BitmapDrawable
     */
    @NonNull
    private static BitmapDrawable getIcon(@NonNull Context ctx, @NonNull java.util.Map<java.util.Map<String, String>, BitmapDrawable> iconCache, int iconSize,
            @NonNull final OsmElement e) {
        BitmapDrawable icon = e.getFromCache(iconCache);
        if (icon == null) {
            icon = NO_ICON;
            PresetItem item = Preset.findBestMatch(App.getCurrentPresets(ctx), e.getTags(), null, null);
            if (item != null) {
                Drawable tempIcon = item.getIcon(ctx, iconSize);
                if (tempIcon instanceof BitmapDrawable) {
                    icon = (BitmapDrawable) tempIcon;
                }
                e.addToCache(iconCache, icon);
            }
        }
        return icon;
    }

    @Override
    public int getItemCount() {
        return elements.size();
    }
}