package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.List;

import android.location.LocationManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.GnssPositionInfo;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Result;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.NoteFragment;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.voice.Commands;

public class LongClickActionModeCallback extends EasyEditActionModeCallback implements android.view.MenuItem.OnMenuItemClickListener {
    private static final String DEBUG_TAG                = "LongClickActionMode...";
    private static final int    MENUITEM_OSB             = 1;
    private static final int    MENUITEM_NEWNODEWAY      = 2;
    private static final int    MENUITEM_SPLITWAY        = 3;
    private static final int    MENUITEM_PASTE           = 4;
    private static final int    MENUITEM_NEWNODE_GPS     = 5;
    private static final int    MENUITEM_NEWNODE_ADDRESS = 6;
    private static final int    MENUITEM_NEWNODE_PRESET  = 7;
    private static final int    MENUITEM_NEWNODE_VOICE   = 9;
    private float               startX;
    private float               startY;
    private int                 startLon;
    private int                 startLat;
    private float               x;
    private float               y;
    private List<OsmElement>    clickedNodes;
    private List<Way>           clickedNonClosedWays;

    /**
     * Construct a callback for when a long click has occurred
     * 
     * @param manager the EasyEditManager instance
     * @param x screen x coordinate
     * @param y screen y coordinate
     */
    public LongClickActionModeCallback(EasyEditManager manager, float x, float y) {
        super(manager);
        this.x = x;
        this.y = y;
        clickedNodes = logic.getClickedNodes(x, y);
        clickedNonClosedWays = logic.getClickedWays(false, x, y); //
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_longclick;
        super.onCreateActionMode(mode, menu);
        mode.setTitle(R.string.menu_add);
        mode.setSubtitle(null);
        // show crosshairs
        logic.showCrosshairs(x, y);
        startX = x;
        startY = y;
        startLon = logic.xToLonE7(x);
        startLat = logic.yToLatE7(y);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        super.onPrepareActionMode(mode, menu);
        menu.clear();
        menuUtil.reset();
        Preferences prefs = new Preferences(main);
        if (prefs.voiceCommandsEnabled()) {
            menu.add(Menu.NONE, MENUITEM_NEWNODE_VOICE, Menu.NONE, R.string.menu_voice_commands).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.mic))
                    .setEnabled(main.isConnectedOrConnecting());
        }
        menu.add(Menu.NONE, MENUITEM_NEWNODE_ADDRESS, Menu.NONE, R.string.tag_menu_address)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_address));
        menu.add(Menu.NONE, MENUITEM_NEWNODE_PRESET, Menu.NONE, R.string.tag_menu_preset).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_preset));
        menu.add(Menu.NONE, MENUITEM_OSB, Menu.NONE, R.string.openstreetbug_new_bug).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_bug));
        if ((clickedNonClosedWays != null && !clickedNonClosedWays.isEmpty()) && (clickedNodes == null || clickedNodes.isEmpty())) {
            menu.add(Menu.NONE, MENUITEM_SPLITWAY, Menu.NONE, R.string.menu_split).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_split));
        }
        menu.add(Menu.NONE, MENUITEM_NEWNODEWAY, Menu.NONE, R.string.openstreetbug_new_nodeway)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_append));
        if (!logic.clipboardIsEmpty()) {
            menu.add(Menu.NONE, MENUITEM_PASTE, Menu.NONE, R.string.menu_paste).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_paste));
        }
        // check if GPS is enabled
        if (((LocationManager) main.getSystemService(android.content.Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            menu.add(Menu.NONE, MENUITEM_NEWNODE_GPS, Menu.NONE, R.string.menu_newnode_gps).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_gps));
        }
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        arrangeMenu(menu);
        return true;
    }

    /**
     * if we get a short click go to path creation mode
     */
    @Override
    public boolean handleClick(float x, float y) {
        PathCreationActionModeCallback pcamc = new PathCreationActionModeCallback(manager, logic.lonE7ToX(startLon), logic.latE7ToY(startLat));
        main.startSupportActionMode(pcamc);
        pcamc.handleClick(x, y);
        logic.hideCrosshairs();
        return true;
    }

    @Override
    public boolean onCreateContextMenu(ContextMenu menu) {
        if (clickedNonClosedWays != null && !clickedNonClosedWays.isEmpty()) {
            menu.setHeaderTitle(R.string.split_context_title);
            int id = 0;
            menu.add(Menu.NONE, id++, Menu.NONE, R.string.split_all_ways).setOnMenuItemClickListener(this);
            for (Way w : clickedNonClosedWays) {
                menu.add(Menu.NONE, id++, Menu.NONE, w.getDescription(main)).setOnMenuItemClickListener(this);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();

        final List<Way> ways = new ArrayList<>();
        if (itemId == 0) {
            ways.addAll(clickedNonClosedWays);
        } else {
            ways.add(clickedNonClosedWays.get(itemId - 1));
        }
        try {
            Node splitPosition = logic.performAddOnWay(main, ways, startX, startY, false);
            if (splitPosition != null) {
                splitSafe(ways, () -> {
                    for (Way way : ways) {
                        if (way.hasNode(splitPosition)) {
                            List<Result> result = logic.performSplit(main, way, logic.getSelectedNode());
                            checkSplitResult(way, result);
                        }
                    }
                    manager.finish();
                });
            }
        } catch (OsmIllegalOperationException e) {
            Snack.barError(main, e.getLocalizedMessage());
            Log.d(DEBUG_TAG, "Caught exception " + e);
            manager.finish();
        }
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        switch (item.getItemId()) {
        case MENUITEM_OSB:
            mode.finish();
            de.blau.android.layer.tasks.MapOverlay layer = main.getMap().getTaskLayer();
            if (layer == null) {
                Snack.toastTopError(main, R.string.toast_task_layer_disabled);
            } else {
                NoteFragment.showDialog(main, logic.makeNewNote(x, y));
                logic.hideCrosshairs();
            }
            return true;
        case MENUITEM_NEWNODEWAY:
            main.startSupportActionMode(new PathCreationActionModeCallback(manager, x, y));
            logic.hideCrosshairs();
            return true;
        case MENUITEM_SPLITWAY:
            if (clickedNonClosedWays.size() > 1) {
                manager.showContextMenu();
            } else {
                Way way = clickedNonClosedWays.get(0);
                try {
                    List<Way> ways = Util.wrapInList(way);
                    Node node = logic.performAddOnWay(main, ways, startX, startY, false);
                    if (node != null) {
                        splitSafe(ways, () -> {
                            List<Result> result = logic.performSplit(main, way, node);
                            checkSplitResult(way, result);
                            manager.finish();
                        });
                    }
                } catch (OsmIllegalOperationException e) {
                    Snack.barError(main, e.getLocalizedMessage());
                    Log.d(DEBUG_TAG, "Caught exception " + e);
                    manager.finish();
                }
            }
            return true;
        case MENUITEM_NEWNODE_ADDRESS:
        case MENUITEM_NEWNODE_PRESET:
            logic.hideCrosshairs();
            try {
                logic.setSelectedNode(null);
                logic.performAdd(main, x, y);
            } catch (OsmIllegalOperationException e1) {
                Snack.barError(main, e1.getLocalizedMessage());
                Log.d(DEBUG_TAG, "Caught exception " + e1);
            }
            Node lastSelectedNode = logic.getSelectedNode();
            if (lastSelectedNode != null) {
                main.edit(lastSelectedNode);
                // show preset screen or add addresses
                main.performTagEdit(lastSelectedNode, null, item.getItemId() == MENUITEM_NEWNODE_ADDRESS, item.getItemId() == MENUITEM_NEWNODE_PRESET);
            }
            return true;
        case MENUITEM_PASTE:
            List<OsmElement> elements = App.getLogic().pasteFromClipboard(main, x, y);
            logic.hideCrosshairs();
            if (elements != null && !elements.isEmpty()) {
                if (elements.size() > 1) {
                    manager.finish();
                    App.getLogic().setSelection(elements);
                    manager.editElements();
                } else {
                    manager.editElement(elements.get(0));
                }
            } else {
                manager.finish();
            }
            return true;
        case MENUITEM_NEWNODE_GPS:
            logic.hideCrosshairs();
            logic.setSelectedNode(null);
            GnssPositionInfo.showDialog(main, main.getTracker());
            return true;
        case MENUITEM_NEWNODE_VOICE:
            logic.hideCrosshairs();
            logic.setSelectedNode(null);
            if (!Commands.startVoiceRecognition(main, Main.VOICE_RECOGNITION_REQUEST_CODE, startLon, startLat)) {
                logic.showCrosshairs(startX, startY);
            }
            return true;
        default:
            Log.e(DEBUG_TAG, "Unknown menu item");
            break;
        }
        return false;
    }

    /**
     * Path creation action mode is ending
     */
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setSelectedNode(null);
        super.onDestroyActionMode(mode);
    }

    @Override
    public boolean processShortcut(Character c) {
        if (c == Util.getShortCut(main, R.string.shortcut_paste)) {
            logic.pasteFromClipboard(main, startX, startY);
            logic.hideCrosshairs();
            manager.finish();
            return true;
        }
        return false;
    }
}
