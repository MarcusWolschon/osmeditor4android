package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.view.ActionMode;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.easyedit.turnrestriction.FromElementWithViaNodeActionModeCallback;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.MergeResult;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class NodeSelectionActionModeCallback extends ElementSelectionActionModeCallback implements android.view.MenuItem.OnMenuItemClickListener {
    private static final int MENUITEM_APPEND       = 10;
    private static final int MENUITEM_JOIN         = 11;
    private static final int MENUITEM_UNJOIN       = 12;
    private static final int MENUITEM_EXTRACT      = 13;
    private static final int MENUITEM_RESTRICTION  = 14;
    /** */
    private static final int MENUITEM_SET_POSITION = 16;
    private static final int MENUITEM_ADDRESS      = 17;

    private List<OsmElement> joinableElements = null;
    private List<Way>        highways         = new ArrayList<>();

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
        // mode.setTitleOptionalHint(true); // no need to display the title, only available in 4.1 up
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);

        super.onPrepareActionMode(mode, menu);
        SortedMap<String, String> tags = ((Node) element).getTags();
        if (!tags.containsKey(Tags.KEY_ADDR_HOUSENUMBER) && !tags.containsKey(Tags.KEY_HIGHWAY)) {
            // exclude some stuff that typically doesn't have an address
            menu.add(Menu.NONE, MENUITEM_ADDRESS, Menu.NONE, R.string.tag_menu_address).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_address));
        }
        if (logic.isEndNode((Node) element)) {
            menu.add(Menu.NONE, MENUITEM_APPEND, Menu.NONE, R.string.menu_append).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_append));
        }
        joinableElements = logic.findJoinableElements((Node) element);
        if (!joinableElements.isEmpty()) {
            menu.add(Menu.NONE, MENUITEM_JOIN, Menu.NONE, R.string.menu_join).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_merge))
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_merge));
        }
        List<Way> ways = logic.getFilteredWaysForNode((Node) element);
        int wayMembershipCount = ways.size();
        if (wayMembershipCount > 1) {
            menu.add(Menu.NONE, MENUITEM_UNJOIN, Menu.NONE, R.string.menu_unjoin).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_unjoin));
        }
        if (wayMembershipCount > 0) {
            menu.add(Menu.NONE, MENUITEM_EXTRACT, Menu.NONE, R.string.menu_extract).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_extract_node));
        }
        for (Way w : ways) {
            if (w.hasTagKey(Tags.KEY_HIGHWAY)) {
                highways.add(w);
            }
        }
        if (highways.size() >= 2) {
            menu.add(Menu.NONE, MENUITEM_RESTRICTION, Menu.NONE, R.string.actionmode_restriction)
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_add_restriction));
        }

        menu.add(Menu.NONE, MENUITEM_SET_POSITION, Menu.CATEGORY_SYSTEM, R.string.menu_set_position)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_gps));
        arrangeMenu(menu);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!super.onActionItemClicked(mode, item)) {
            switch (item.getItemId()) {
            case MENUITEM_APPEND:
                main.startSupportActionMode(new PathCreationActionModeCallback(manager, (Node) element));
                break;
            case MENUITEM_JOIN:
                if (joinableElements.size() > 1) {
                    manager.showContextMenu();
                } else {
                    mergeNodeWith(joinableElements);
                }
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
                main.startSupportActionMode(new FromElementWithViaNodeActionModeCallback(manager, new HashSet<>(highways), (Node) element));
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
            MergeResult result = target.get(0) instanceof Way ? logic.performJoinNodeToWays(main, target, (Node) element)
                    : logic.performMergeNodes(main, target, (Node) element);
            if (result != null) {
                manager.invalidate(); // button will remain enabled
                if (!result.getElement().equals(element)) { // only re-select if not already selected
                    manager.editElement(result.getElement());
                }
                if (result.hasIssue()) {
                    showConflictAlert(result);
                } else {
                    Snack.toastTopInfo(main, R.string.toast_merged);
                }
            }
        } catch (OsmIllegalOperationException e) {
            Snack.barError(main, e.getLocalizedMessage());
        }
    }

    @Override
    public boolean onCreateContextMenu(ContextMenu menu) {
        if (joinableElements.size() > 1) {
            menu.setHeaderTitle(R.string.merge_context_title);
            int id = 0;
            menu.add(Menu.NONE, id++, Menu.NONE, joinableElements.get(0) instanceof Way ? R.string.merge_with_all_ways : R.string.merge_with_all_nodes)
                    .setOnMenuItemClickListener(this);
            for (OsmElement e : joinableElements) {
                menu.add(Menu.NONE, id++, Menu.NONE, main.descriptionForContextMenu(e)).setOnMenuItemClickListener(this);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == 0) {
            mergeNodeWith(joinableElements);
        } else {
            mergeNodeWith(Util.wrapInList(joinableElements.get(itemId - 1)));
        }
        return true;
    }

    @Override
    protected void menuDelete(final ActionMode mode) {
        if (element.hasParentRelations()) {
            new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.deletenode_relation_description)
                    .setPositiveButton(R.string.deletenode, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            logic.performEraseNode(main, (Node) element, true);
                            if (mode != null) {
                                mode.finish();
                            }
                        }
                    }).show();
        } else {
            logic.performEraseNode(main, (Node) element, true);
            mode.finish();
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
        TextView datum = (TextView) layout.findViewById(R.id.set_position_datum); // TODO add conversion to/from
                                                                                  // other datums
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
        return new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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
            }
        };
    }
}
