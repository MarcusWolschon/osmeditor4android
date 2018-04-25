package de.blau.android.easyedit;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class NodeSelectionActionModeCallback extends ElementSelectionActionModeCallback {
    private static final int MENUITEM_APPEND  = 9;
    private static final int MENUITEM_JOIN    = 10;
    private static final int MENUITEM_UNJOIN  = 11;
    private static final int MENUITEM_EXTRACT = 12;

    private static final int MENUITEM_SET_POSITION = 15;
    private static final int MENUITEM_ADDRESS      = 16;

    private OsmElement joinableElement = null;

    /**
     * Construct a call back for Node selection
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
        logic.setSelectedWay(null);
        logic.setSelectedRelationWays(null);
        logic.setSelectedRelationNodes(null);
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
        if (((Node) element).getTags().containsKey(Tags.KEY_ENTRANCE) && !((Node) element).getTags().containsKey(Tags.KEY_ADDR_HOUSENUMBER)) {
            menu.add(Menu.NONE, MENUITEM_ADDRESS, Menu.NONE, R.string.tag_menu_address).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_address));
        }
        if (logic.isEndNode((Node) element)) {
            menu.add(Menu.NONE, MENUITEM_APPEND, Menu.NONE, R.string.menu_append).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_append));
        }
        joinableElement = logic.findJoinableElement((Node) element);
        if (joinableElement != null) {
            menu.add(Menu.NONE, MENUITEM_JOIN, Menu.NONE, R.string.menu_join).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_merge))
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_merge));
        }
        int wayMembershipCount = logic.getFilteredWaysForNode((Node) element).size();
        if (wayMembershipCount > 1) {
            menu.add(Menu.NONE, MENUITEM_UNJOIN, Menu.NONE, R.string.menu_unjoin).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_unjoin));
        }
        if (wayMembershipCount > 0) {
            menu.add(Menu.NONE, MENUITEM_EXTRACT, Menu.NONE, R.string.menu_extract).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_extract_node));
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
                try {
                    if (!logic.performJoin(main, joinableElement, (Node) element)) {
                        Snack.barWarning(main, R.string.toast_merge_tag_conflict);
                        main.performTagEdit(element, null, false, false, false);
                    } else {
                        mode.finish();
                    }
                } catch (OsmIllegalOperationException e) {
                    Snack.barError(main, e.getLocalizedMessage());
                }
                break;
            case MENUITEM_UNJOIN:
                logic.performUnjoin(main, (Node) element);
                mode.finish();
                break;
            case MENUITEM_EXTRACT:
                logic.performExtract(main, (Node) element);
                manager.invalidate();
                break;
            case MENUITEM_SET_POSITION:
                setPosition();
                break;
            case MENUITEM_ADDRESS:
                main.performTagEdit(element, null, true, false, false);
                break;
            case MENUITEM_SHARE_POSITION:
                double[] lonLat = new double[2];
                lonLat[0] = ((Node) element).getLon() / 1E7;
                lonLat[1] = ((Node) element).getLat() / 1E7;
                Util.sharePosition(main, lonLat);
                break;
            default:
                return false;
            }
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
        datum.setText("WGS84");
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
                if (lon >= -180 && lon <= 180 && lat >= -GeoMath.MAX_LAT && lat <= GeoMath.MAX_LAT) {
                    logic.performSetPosition(main, node, lon, lat);
                    manager.invalidate();
                } else {
                    createSetPositionDialog((int) (lon * 1E7), (int) (lat * 1E7)).show();
                    Snack.barWarning(main, R.string.coordinates_out_of_range);
                }
            }
        };
    }
}
