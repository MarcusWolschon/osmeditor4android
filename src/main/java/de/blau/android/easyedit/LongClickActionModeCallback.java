package de.blau.android.easyedit;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.location.LocationManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.DisambiguationMenu;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.GnssPositionInfo;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Result;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.prefs.keyboard.Shortcuts;
import de.blau.android.tasks.NoteFragment;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.voice.Commands;

public class LongClickActionModeCallback extends EasyEditActionModeCallback implements DisambiguationMenu.OnMenuItemClickListener {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, LongClickActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = LongClickActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENUITEM_OSB             = 1;
    private static final int MENUITEM_NEWNODEWAY      = 2;
    private static final int MENUITEM_SPLITWAY        = 3;
    private static final int MENUITEM_PASTE           = 4;
    private static final int MENUITEM_NEWNODE_GPS     = 5;
    private static final int MENUITEM_NEWNODE_ADDRESS = 6;
    private static final int MENUITEM_NEWNODE_PRESET  = 7;
    private static final int MENUITEM_NEWNODE_VOICE   = 9;
    private float            startX;
    private float            startY;
    private int              startLon;
    private int              startLat;
    private float            x;
    private float            y;
    private List<OsmElement> clickedNodes;
    private List<Way>        clickedNonClosedWays;

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

        actionMap.put(main.getString(R.string.ACTION_ELEMENT_PASTE), new Shortcuts.Action(R.string.action_help, () -> {
            logic.hideCrosshairs();
            SimpleActionModeCallback.paste(main, manager, startX, startY);
        }));
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
        Preferences prefs = App.getPreferences(main);
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
    public boolean onCreateDisambiguationMenu(DisambiguationMenu menu) {
        if (clickedNonClosedWays != null && !clickedNonClosedWays.isEmpty()) {
            menu.setHeaderTitle(R.string.split_context_title);
            int id = 0;
            menu.add(id++, (DisambiguationMenu.Type) null, R.string.split_all_ways, false, this);
            for (Way w : clickedNonClosedWays) {
                menu.add(id++, w, w.getDescription(main), false, this);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(int position) {
        int itemId = position;

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
                            List<Result> result = logic.performSplit(main, way, logic.getSelectedNode(), true);
                            checkSplitResult(way, result);
                        }
                    }
                    manager.finish();
                });
            }
        } catch (OsmIllegalOperationException e) {
            finishOnException(e);
        }
    }

    /**
     * Display a toast and finish the action mode
     * 
     * @param e the Exception
     */
    private void finishOnException(@NonNull Exception e) {
        ScreenMessage.barError(main, e.getLocalizedMessage());
        Log.d(DEBUG_TAG, "Caught exception " + e);
        manager.finish();
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        switch (item.getItemId()) {
        case MENUITEM_OSB:
            mode.finish();
            de.blau.android.layer.tasks.MapOverlay layer = main.getMap().getTaskLayer();
            if (layer == null) {
                ScreenMessage.toastTopError(main, R.string.toast_task_layer_disabled);
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
                manager.showDisambiguationMenu();
            } else {
                Way way = clickedNonClosedWays.get(0);
                try {
                    List<Way> ways = Util.wrapInList(way);
                    Node node = logic.performAddOnWay(main, ways, startX, startY, false);
                    if (node != null) {
                        splitSafe(ways, () -> {
                            try {
                                List<Result> result = logic.performSplit(main, way, node, true);
                                checkSplitResult(way, result);
                            } catch (OsmIllegalOperationException | StorageException ex) {
                                // toast has already been displayed
                            } finally {
                                manager.finish();
                            }
                        });
                    }
                } catch (OsmIllegalOperationException e) {
                    finishOnException(e);
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
                ScreenMessage.barError(main, e1.getLocalizedMessage());
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
            logic.hideCrosshairs();
            SimpleActionModeCallback.paste(main, manager, x, y);
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
}
