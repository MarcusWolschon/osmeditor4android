package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;

import android.annotation.SuppressLint;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.dialogs.TagConflictDialog;
import de.blau.android.easyedit.turnrestriction.FromElementWithViaNodeActionModeCallback;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Result;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Snack;
import de.blau.android.util.Sound;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class NodeSelectionActionModeCallback extends ElementSelectionActionModeCallback implements android.view.MenuItem.OnMenuItemClickListener {
    private static final String DEBUG_TAG             = "NodeSelectionAction...";
    private static final int    MENUITEM_APPEND       = LAST_REGULAR_MENUITEM + 1;
    private static final int    MENUITEM_JOIN         = LAST_REGULAR_MENUITEM + 2;
    private static final int    MENUITEM_UNJOIN       = LAST_REGULAR_MENUITEM + 3;
    private static final int    MENUITEM_EXTRACT      = LAST_REGULAR_MENUITEM + 4;
    private static final int    MENUITEM_RESTRICTION  = LAST_REGULAR_MENUITEM + 5;
    /** */
    private static final int    MENUITEM_SET_POSITION = LAST_REGULAR_MENUITEM + 6;
    private static final int    MENUITEM_ADDRESS      = LAST_REGULAR_MENUITEM + 7;

    private List<OsmElement> joinableElements = null;
    private List<Way>        appendableWays   = null;
    private List<Way>        highways         = new ArrayList<>();
    private MenuItem         joinItem;
    private MenuItem         appendItem;
    private MenuItem         unjoinItem;
    private MenuItem         extractItem;
    private MenuItem         restrictionItem;
    private int              action;

    /**
     * Construct a callback for Node selection
     * 
     * @param manager the EasyEditManager instance
     * @param node the selected Node
     */
    NodeSelectionActionModeCallback(EasyEditManager manager, Node node) {
        super(manager, node);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_nodeselection;
        super.onCreateActionMode(mode, menu);
        logic.setSelectedNode((Node) element);
        main.invalidateMap();
        mode.setTitle(R.string.actionmode_nodeselect);
        mode.setSubtitle(null);

        menu = replaceMenu(menu, mode, this);
        SortedMap<String, String> tags = ((Node) element).getTags();
        if (!tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER) && !tags.containsKey(Tags.KEY_HIGHWAY)) {
            // exclude some stuff that typically doesn't have an address
            menu.add(Menu.NONE, MENUITEM_ADDRESS, Menu.NONE, R.string.tag_menu_address).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_address));
        }

        appendItem = menu.add(Menu.NONE, MENUITEM_APPEND, Menu.NONE, R.string.menu_append).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_append));

        joinItem = menu.add(Menu.NONE, MENUITEM_JOIN, Menu.NONE, R.string.menu_join).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_merge));

        unjoinItem = menu.add(Menu.NONE, MENUITEM_UNJOIN, Menu.NONE, R.string.menu_unjoin).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_unjoin));

        extractItem = menu.add(Menu.NONE, MENUITEM_EXTRACT, Menu.NONE, R.string.menu_extract)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_extract_node));

        restrictionItem = menu.add(Menu.NONE, MENUITEM_RESTRICTION, Menu.NONE, R.string.actionmode_restriction)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_add_restriction));

        menu.add(Menu.NONE, MENUITEM_SET_POSITION, Menu.CATEGORY_SYSTEM, R.string.menu_set_position)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_gps));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        boolean updated = super.onPrepareActionMode(mode, menu);
        Log.d(DEBUG_TAG, "onPrepareActionMode");

        joinableElements = logic.findJoinableElements((Node) element);
        updated |= setItemVisibility(!joinableElements.isEmpty(), joinItem, false);

        List<Way> ways = logic.getFilteredWaysForNode((Node) element);
        appendableWays = findAppendableWays(ways, (Node) element);
        updated |= setItemVisibility(!appendableWays.isEmpty(), appendItem, false);

        int wayMembershipCount = ways.size();
        updated |= setItemVisibility(wayMembershipCount > 1, unjoinItem, false);
        updated |= setItemVisibility(wayMembershipCount > 0, extractItem, false);

        for (Way w : ways) {
            if (w.hasTagKey(Tags.KEY_HIGHWAY)) {
                highways.add(w);
            }
        }
        updated |= setItemVisibility(highways.size() >= 2, restrictionItem, false);

        if (updated) {
            arrangeMenu(menu);
        }
        return updated;
    }

    /**
     * Get a list of ways node is an end node of
     * 
     * @param ways the List of Way
     * @param node the Node
     * @return true is an end node in any of them
     */
    @NonNull
    private List<Way> findAppendableWays(@NonNull List<Way> ways, @NonNull Node node) {
        List<Way> result = new ArrayList<>();
        for (Way w : ways) {
            if (w.isEndNode(node)) {
                result.add(w);
            }
        }
        return result;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!super.onActionItemClicked(mode, item)) {
            action = item.getItemId();
            switch (action) {
            case MENUITEM_APPEND:
                if (appendableWays.size() > 1) {
                    manager.showContextMenu();
                } else {
                    main.startSupportActionMode(new PathCreationActionModeCallback(manager, appendableWays.get(0), (Node) element));
                }
                break;
            case MENUITEM_JOIN:
                mergeNode(joinableElements.size());
                break;
            case MENUITEM_UNJOIN:
                logic.performUnjoinWays(main, (Node) element);
                mode.finish();
                break;
            case MENUITEM_EXTRACT:
                logic.performExtract(main, (Node) element);
                manager.invalidate();
                break;
            case MENUITEM_RESTRICTION:
                main.startSupportActionMode(new FromElementWithViaNodeActionModeCallback(manager, new HashSet<>(highways), (Node) element, null));
                break;
            case MENUITEM_SET_POSITION:
                setPosition();
                break;
            case MENUITEM_ADDRESS:
                main.performTagEdit(element, null, true, false);
                break;
            case MENUITEM_SHARE_POSITION:
                double[] lonLat = new double[2];
                lonLat[0] = ((Node) element).getLon() / 1E7;
                lonLat[1] = ((Node) element).getLat() / 1E7;
                Util.sharePosition(main, lonLat, main.getMap().getZoomLevel());
                break;
            default:
                return false;
            }
        }
        return true;
    }

    /**
     * Merge the selected node with an OsmELement
     * 
     * @param target the target OsmElement
     * 
     */
    private void mergeNodeWith(@NonNull List<OsmElement> target) {
        try {
            List<Result> result = target.get(0) instanceof Way ? logic.performJoinNodeToWays(main, target, (Node) element)
                    : logic.performMergeNodes(main, target, (Node) element);
            if (!result.isEmpty()) {
                manager.invalidate(); // button will remain enabled
                OsmElement newElement = result.get(0).getElement();
                if (!newElement.equals(element)) { // only re-select if not already selected
                    manager.editElement(newElement);
                }
                if (result.size() > 1 || result.get(0).hasIssue()) {
                    TagConflictDialog.showDialog(main, result);
                } else {
                    Snack.toastTopInfo(main, R.string.toast_merged);
                }
            }
        } catch (OsmIllegalOperationException | IllegalStateException e) {
            Snack.barError(main, e.getLocalizedMessage());
        }
    }

    @Override
    public boolean onCreateContextMenu(@NonNull ContextMenu menu) {
        if (action == MENUITEM_JOIN && joinableElements.size() > 1) {
            menu.setHeaderTitle(R.string.merge_context_title);
            menu.add(Menu.NONE, 0, Menu.NONE, joinableElements.get(0) instanceof Way ? R.string.merge_with_all_ways : R.string.merge_with_all_nodes)
                    .setOnMenuItemClickListener(this);
            addElementsToContextMenu(menu, 1, joinableElements);
            return true;
        } else if ((action == MENUITEM_APPEND && appendableWays.size() > 1)) {
            menu.setHeaderTitle(R.string.append_context_title);
            addElementsToContextMenu(menu, 0, appendableWays);
            return true;
        }
        return false;
    }

    /**
     * Add elements to the context menu
     * 
     * @param <T> the type of element
     * @param menu the menu
     * @param startIndex the index to use for the item
     * @param elements list of elements to add
     */
    private <T extends OsmElement> void addElementsToContextMenu(@NonNull ContextMenu menu, int startIndex, @NonNull List<T> elements) {
        for (OsmElement e : elements) {
            menu.add(Menu.NONE, startIndex++, Menu.NONE, main.descriptionForContextMenu(e)).setOnMenuItemClickListener(this);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (action == MENUITEM_JOIN) {
            if (itemId == 0) {
                mergeNodeWith(joinableElements);
            } else {
                mergeNodeWith(Util.wrapInList(joinableElements.get(itemId - 1)));
            }
        } else if (action == MENUITEM_APPEND) {
            main.startSupportActionMode(new PathCreationActionModeCallback(manager, appendableWays.get(itemId), (Node) element));
        }
        return true;
    }

    @Override
    protected void menuDelete(final ActionMode mode) {
        if (element.hasParentRelations()) {
            new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.deletenode_relation_description)
                    .setPositiveButton(R.string.deletenode, (dialog, which) -> deleteNode(mode)).show();
        } else {
            deleteNode(mode);
        }
    }

    /**
     * Delete the Node
     * 
     * @param mode the current ActionMode
     */
    private void deleteNode(@Nullable final ActionMode mode) {
        List<Relation> origParents = element.hasParentRelations() ? new ArrayList<>(element.getParentRelations()) : null;
        logic.performEraseNode(main, (Node) element, true);
        if (mode != null) {
            mode.finish();
        }
        checkEmptyRelations(main, origParents);
    }

    @Override
    public boolean processShortcut(Character c) {
        if (c == Util.getShortCut(main, R.string.shortcut_merge)) {
            int count = joinableElements.size();
            if (count > 0) {
                mergeNode(count);
            } else {
                Sound.beep();
            }
            return true;
        }
        return super.processShortcut(c);
    }

    /**
     * Merge the selected node with any other available elements
     * 
     * @param count the number of elements available for merge
     */
    void mergeNode(int count) {
        if (count > 1) {
            manager.showContextMenu();
        } else {
            mergeNodeWith(joinableElements);
        }
    }

    /**
     * Show a dialog to set the location of the selected Node
     */
    private void setPosition() {
        if (element instanceof Node) {
            // show dialog to set lon/lat
            createSetPositionDialog(((Node) element).getLon(), ((Node) element).getLat()).show();
        }
    }

    /**
     * Build a dialog with the current coordinates
     * 
     * @param lonE7 longitude in 1E7 format
     * @param latE7 latitude in 1E7 format
     * @return the Dialog
     */
    @SuppressLint("InflateParams")
    AppCompatDialog createSetPositionDialog(int lonE7, int latE7) {
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(main);
        Builder dialog = new AlertDialog.Builder(main);
        dialog.setTitle(R.string.menu_set_position);

        View layout = inflater.inflate(R.layout.set_position, null);
        dialog.setView(layout);
        // TODO add conversion to/from other datums
        TextView datum = (TextView) layout.findViewById(R.id.set_position_datum);
        datum.setText(R.string.WGS84);
        EditText lon = (EditText) layout.findViewById(R.id.set_position_lon);
        lon.setText(String.format(Locale.US, "%.7f", lonE7 / 1E7d));
        EditText lat = (EditText) layout.findViewById(R.id.set_position_lat);
        lat.setText(String.format(Locale.US, "%.7f", latE7 / 1E7d));

        dialog.setPositiveButton(R.string.set, createSetButtonListener(lon, lat, (Node) element));
        dialog.setNegativeButton(R.string.cancel, null);

        return dialog.create();
    }

    /**
     * Create an onClick listener that sets the coordinates in the node
     * 
     * @param lonField the EditText with the longitude
     * @param latField the EditText with the latitude
     * @param node the Node we want to change the location of
     * @return the OnClickListnener
     */
    private OnClickListener createSetButtonListener(final EditText lonField, final EditText latField, final Node node) {
        return (dialog, which) -> {
            double lon = Double.parseDouble(lonField.getText().toString());
            double lat = Double.parseDouble(latField.getText().toString());
            if (GeoMath.coordinatesInCompatibleRange(lon, lat)) {
                try {
                    logic.performSetPosition(main, node, lon, lat);
                } catch (OsmIllegalOperationException ex) {
                    Snack.barError(main, ex.getLocalizedMessage()); // this "can't" happen
                }
                manager.invalidate();
            } else {
                createSetPositionDialog((int) (lon * 1E7), (int) (lat * 1E7)).show();
                Snack.barWarning(main, R.string.coordinates_out_of_range);
            }
        };
    }
}
