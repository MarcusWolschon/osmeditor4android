package io.vespucci;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import io.vespucci.R;
import io.vespucci.gpx.WayPoint;
import io.vespucci.layer.ClickedObject;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Relation;
import io.vespucci.osm.Way;
import io.vespucci.photos.Photo;
import io.vespucci.tasks.MapRouletteTask;
import io.vespucci.tasks.Note;
import io.vespucci.tasks.OsmoseBug;
import io.vespucci.tasks.Todo;
import io.vespucci.util.Screen;
import io.vespucci.util.ThemeUtils;
import io.vespucci.util.TypefaceSpanCompat;
import io.vespucci.util.Util;

/**
 * Menu to disambiguate between nearby objects
 * 
 * @author simon
 *
 */
public class DisambiguationMenu {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, DisambiguationMenu.class.getSimpleName().length());
    private static final String DEBUG_TAG = DisambiguationMenu.class.getSimpleName().substring(0, TAG_LEN);

    public enum Type {
        NODE, WAY, RELATION, NOTE, BUG, MAPROULETTE, TODO, GEOJSON, GPX, MAPILLARY, PANORAMAX, MVT, IMAGE
    }

    public interface OnMenuItemClickListener {
        public void onItemClick(int position);
    }

    private final View                          anchor;
    private final List<DisambiguationMenuItem>  items            = new ArrayList<>();
    private final List<OnMenuItemClickListener> onClickListeners = new ArrayList<>();
    private View                                header;
    private boolean                             rtl;
    private final TypefaceSpanCompat            monospaceSpan;

    /**
     * Create a new menu to disambiguate between nearby objects
     * 
     * @param anchor the anchor View
     */
    public DisambiguationMenu(@NonNull View anchor) {
        this.anchor = anchor;
        rtl = Util.isRtlScript(anchor.getContext());
        monospaceSpan = new TypefaceSpanCompat(ResourcesCompat.getFont(anchor.getContext(), R.font.b612mono));
    }

    /**
     * Add a header to the menu
     * 
     * @param titleRes resource id of the title string
     */
    public void setHeaderTitle(int titleRes) {
        LayoutInflater inflator = ThemeUtils.getLayoutInflater(anchor.getContext());
        header = inflator.inflate(R.layout.disambiguation_menu_header, null);
        TextView title = header.findViewById(R.id.header_title);
        title.setText(titleRes);
    }

    /**
     * Add a menu item
     * 
     * @param id an id (unused)
     * @param element the OsmElement it is for
     * @param text a descriptive text
     * @param selected true if the element is selected
     * @param listener callback when the menu item is selected
     */
    public void add(int id, @NonNull OsmElement element, @NonNull String text, boolean selected, @NonNull OnMenuItemClickListener listener) {
        Type type = null;
        switch (element.getName()) {
        case Node.NAME:
            type = Type.NODE;
            break;
        case Way.NAME:
            type = Type.WAY;
            break;
        case Relation.NAME:
            type = Type.RELATION;
            break;
        default:
            Log.e(DEBUG_TAG, "Unknown element " + element.getName());
            return;
        }
        add(id, type, text, selected, listener);
    }

    /**
     * Add a menu item
     * 
     * @param id an id (unused)
     * @param clicked the clicked object on a layer
     * @param text a descriptive text
     * @param selected true if the object is selected
     * @param listener callback when the menu item is selected
     */
    public void add(int id, ClickedObject<?> clicked, SpannableString description, boolean selected, OnMenuItemClickListener listener) {
        add(id, typeFromObject(clicked), description, selected, listener);
    }

    /**
     * Get the object "type"
     * 
     * @param clicked the clicked object on a layer
     * @return the Type
     */
    private Type typeFromObject(@NonNull ClickedObject<?> clicked) {
        final Object object = clicked.getObject();
        if (object instanceof OsmoseBug) {
            return Type.BUG;
        }
        if (object instanceof Note) {
            return Type.NOTE;
        }
        if (object instanceof MapRouletteTask) {
            return Type.MAPROULETTE;
        }
        if (object instanceof Todo) {
            return Type.TODO;
        }
        if (object instanceof com.mapbox.geojson.Feature) {
            return Type.GEOJSON;
        }
        if (object instanceof WayPoint) {
            return Type.GPX;
        }
        if (object instanceof io.vespucci.util.mvt.VectorTileDecoder.Feature) {
            if (clicked.getLayer() instanceof io.vespucci.layer.streetlevel.mapillary.MapillaryOverlay) {
                return Type.MAPILLARY;
            } else if (clicked.getLayer() instanceof io.vespucci.layer.streetlevel.panoramax.PanoramaxOverlay) {
                return Type.PANORAMAX;
            }
            return Type.MVT;
        }
        if (object instanceof Photo) {
            return Type.IMAGE;
        }
        return null;
    }

    /**
     * Add a menu item
     * 
     * @param id an id (unused)
     * @param the Type of object
     * @param text a descriptive text
     * @param selected true if the element is selected
     * @param listener callback when the menu item is selected
     */
    public void add(int id, @Nullable Type type, @NonNull String text, boolean selected, @NonNull final OnMenuItemClickListener listener) { // NOSONAR
        SpannableString s = new SpannableString(text);
        if (text.length() > 0) {
            s.setSpan(monospaceSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (selected) {
                s.setSpan(new ForegroundColorSpan(ThemeUtils.getStyleAttribColorValue(anchor.getContext(), R.attr.colorAccent, 0)), 1, s.length(), 0);
            }
        }
        add(new DisambiguationMenuItem(type, s), listener);
    }

    /**
     * Add a menu item
     * 
     * @param id an id (unused)
     * @param the Type of object
     * @param text a descriptive text
     * @param listener callback when the menu item is selected
     */
    public void add(int id, @Nullable Type type, @NonNull String text, @NonNull final OnMenuItemClickListener listener) { // NOSONAR
        SpannableString s = new SpannableString(text);
        add(new DisambiguationMenuItem(type, s), listener);
    }

    /**
     * Add a menu item
     * 
     * @param id an id (unused)
     * @param the Type of object
     * @param textRes a resource id for the text
     * @param selected true if the element is selected
     * @param listener callback when the menu item is selected
     */
    public void add(int id, @Nullable Type type, @NonNull int textRes, boolean selected, @NonNull final OnMenuItemClickListener listener) { // NOSONAR
        add(new DisambiguationMenuItem(type, anchor.getContext().getString(textRes)), listener);
    }

    /**
     * Add a menu item
     * 
     * @param id an id (unused)
     * @param the Type of object
     * @param text a descriptive text
     * @param selected true if the element is selected
     * @param listener callback when the menu item is selected
     */
    public void add(int id, @Nullable Type type, @NonNull SpannableString text, boolean selected, @NonNull final OnMenuItemClickListener listener) { // NOSONAR
        add(new DisambiguationMenuItem(type, text), listener);
    }

    /**
     * Add a menu item
     * 
     * @param item the menu item
     * @param listener callback when the menu item is selected
     */
    private void add(@NonNull DisambiguationMenuItem item, @NonNull final OnMenuItemClickListener listener) {
        items.add(item);
        onClickListeners.add(listener);
    }

    /**
     * Display the menu
     */
    public void show() {
        final Context context = anchor.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View menuView = inflater.inflate(R.layout.disambiguation_menu, null, false);
        final PopupWindow menuWindow = new PopupWindow(menuView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        menuWindow.setFocusable(true);
        menuWindow.setBackgroundDrawable(new ColorDrawable());
        menuWindow.setOutsideTouchable(true);

        ListView itemsView = menuView.findViewById(R.id.menu_listView);
        itemsView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            onClickListeners.get(position).onItemClick(position);
            menuWindow.dismiss();
        });
        final DisambiguationMenuAdapter adapter = new DisambiguationMenuAdapter();
        itemsView.setAdapter(adapter);
        int padding = ThemeUtils.getDimensionFromAttribute(context, android.R.attr.dialogPreferredPadding) / 2;
        // add header
        LinearLayout ll = menuView.findViewById(R.id.menu_container);
        if (header != null) {
            header.setPadding(padding, padding, 0, 0);
            ll.addView(header, 0);
        }
        View scrollView = menuView.findViewById(R.id.menu_scrollView);
        scrollView.setPadding(padding, header == null ? padding : 0, padding, padding);

        // restrict the width on devices in landscape mode with some heuristics, simply looks better
        final int offset = 2 * padding + 10; // magic incantation
        if (context instanceof Activity && Screen.isLandscape((Activity) context)) {
            ViewGroup.LayoutParams params = ll.getLayoutParams();
            int measuredWidth = adapter.width;
            if (measuredWidth > 0) {
                params.width = Math.min(measuredWidth + offset, (int) (Screen.getScreenSmallDimension((Activity) context) * 0.9));
                ll.setLayoutParams(params);
            }
        }

        menuWindow.showAtLocation(anchor, Gravity.CENTER, 0, 0);
        // workaround the HSV not being quite on the left / right
        scrollView.post(() -> scrollView.setScrollX(!rtl ? 0 : adapter.width - scrollView.getWidth() + offset));
    }

    class DisambiguationMenuItem {
        private Type            type;
        private SpannableString title;

        /**
         * Construct a new item
         * 
         * @param type the item type
         * @param title the text to display
         */
        public DisambiguationMenuItem(@Nullable Type type, @NonNull SpannableString title) {
            this.type = type;
            this.title = title;
        }

        /**
         * Construct a new item
         * 
         * @param type the item type
         * @param title the text to display
         */
        public DisambiguationMenuItem(@Nullable Type type, @NonNull String title) {
            this.type = type;
            this.title = new SpannableString(title);
        }

        /**
         * Get tht type of the item
         * 
         * @return the type
         */
        @Nullable
        public Type getType() {
            return type;
        }

        /**
         * Get the title text for the menu item
         * 
         * @return the title
         */
        @NonNull
        public SpannableString getTitle() {
            return title;
        }
    }

    private class DisambiguationMenuAdapter implements android.widget.ListAdapter {

        private static final int DRAWABLE_PADDING = 10;
        int                      width            = -1;

        DisambiguationMenuAdapter() {
            fakeMeasure();
        }

        @Override
        public View getView(int index, View view, ViewGroup viewGroup) {
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) viewGroup.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.disambiguation_menu_item, viewGroup, false);
            }

            DisambiguationMenuItem item = (DisambiguationMenuItem) getItem(index);
            final TextView title = view.findViewById(R.id.item_title);
            Drawable drawable = getDrawable(item);
            if (drawable != null) {
                title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                Util.setCompoundDrawableWithIntrinsicBounds(rtl, title, drawable);
                title.setCompoundDrawablePadding(DRAWABLE_PADDING);
            }
            title.setText(item.getTitle());
            if (width > 0) {
                view.setMinimumWidth(width);
            }
            return view;
        }

        /**
         * Get an appropriate icon for a menu item
         * 
         * @param item the menu item
         * @return a Drawable or null
         */
        private Drawable getDrawable(DisambiguationMenuItem item) {
            final Context context = anchor.getContext();
            final Type type = item.getType();
            if (type != null) {
                switch (type) {
                case NODE:
                    return ContextCompat.getDrawable(context, R.drawable.element_node);
                case WAY:
                    return ContextCompat.getDrawable(context, R.drawable.element_way);
                case RELATION:
                    return ContextCompat.getDrawable(context, R.drawable.element_relation);
                case BUG:
                    return ContextCompat.getDrawable(context, R.drawable.bug_small);
                case NOTE:
                    return ContextCompat.getDrawable(context, R.drawable.note_small);
                case TODO:
                    return ContextCompat.getDrawable(context, R.drawable.todo_small);
                case MAPROULETTE:
                    return ContextCompat.getDrawable(context, R.drawable.maproulette_small);
                case GEOJSON:
                    return ContextCompat.getDrawable(context, R.drawable.geojson);
                case GPX:
                    return ContextCompat.getDrawable(context, R.drawable.gpx);
                case IMAGE:
                    return ContextCompat.getDrawable(context, R.drawable.photo_small);
                case MAPILLARY:
                    return ContextCompat.getDrawable(context, R.drawable.mapillary_small);
                case PANORAMAX:
                    return ContextCompat.getDrawable(context, R.drawable.mapillary_small);
                default:
                }
            }
            return null;
        }

        FrameLayout fakeParent = new FrameLayout(anchor.getContext());
        View        fakeView   = null;

        /**
         * Measure the width of an item
         * 
         * @param position the items position
         * @param view a dummy view
         * @return a dummy view
         */
        private View fakeMeasure(int position, View view) {
            view = getView(position, view, fakeParent);
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int tempWidth = view.getMeasuredWidth();
            if (tempWidth > width) {
                width = tempWidth;
            }
            return view;
        }

        /**
         * Measure the width of all items
         */
        private void fakeMeasure() {
            int count = getCount();
            for (int i = 0; i < count; i++) {
                fakeView = fakeMeasure(i, fakeView);
            }
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            // nothing
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            // nothing
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return items.isEmpty();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }
    }
}
