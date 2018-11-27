package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetFixedField;
import de.blau.android.propertyeditor.Address;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.TaskFragment;
import de.blau.android.util.ElementSearch;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SearchIndexUtils;
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
    private static final int    MENUITEM_NEWNODE_NAME    = 8;
    private static final int    MENUITEM_NEWNODE_VOICE   = 9;
    private float               startX;
    private float               startY;
    private int                 startLon;
    private int                 startLat;
    private float               x;
    private float               y;
    LocationManager             locationManager          = null;
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
        // mode.setTitleOptionalHint(true);
        // show crosshairs
        logic.showCrosshairs(x, y);
        startX = x;
        startY = y;
        startLon = logic.xToLonE7(x);
        startLat = logic.yToLatE7(y);
        // return isNeeded();
        // always required for paste
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
        if (prefs.tagFormEnabled()) {
            menu.add(Menu.NONE, MENUITEM_NEWNODE_NAME, Menu.NONE, R.string.menu_set_name).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.name));
        }
        menu.add(Menu.NONE, MENUITEM_NEWNODEWAY, Menu.NONE, R.string.openstreetbug_new_nodeway)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_append));
        if (!logic.clipboardIsEmpty()) {
            menu.add(Menu.NONE, MENUITEM_PASTE, Menu.NONE, R.string.menu_paste).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_paste))
                    .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_paste));
        }
        // check if GPS is enabled
        locationManager = (LocationManager) main.getSystemService(android.content.Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            menu.add(Menu.NONE, MENUITEM_NEWNODE_GPS, Menu.NONE, R.string.menu_newnode_gps).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_gps));
        }
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_help))
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
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
    public boolean needsCustomContextMenu() {
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu) {
        if (clickedNonClosedWays != null && !clickedNonClosedWays.isEmpty()) {
            int id = 0;
            menu.add(Menu.NONE, id++, Menu.NONE, R.string.split_all_ways).setOnMenuItemClickListener(this);
            for (Way w : clickedNonClosedWays) {
                menu.add(Menu.NONE, id++, Menu.NONE, w.getDescription(main)).setOnMenuItemClickListener(this);
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();

        List<Way> ways = new ArrayList<>();
        if (itemId == 0) {
            ways = clickedNonClosedWays;
        } else {
            ways.add(clickedNonClosedWays.get(itemId - 1));
        }
        try {
            Node splitPosition = logic.performAddOnWay(main, ways, startX, startY, false);
            if (splitPosition != null) {
                for (Way way : ways) {
                    if (way.hasNode(splitPosition)) {
                        logic.performSplit(main, way, logic.getSelectedNode());
                    }
                }
            }
        } catch (OsmIllegalOperationException e) {
            Snack.barError(main, e.getLocalizedMessage());
            Log.d(DEBUG_TAG, "Caught exception " + e);
        }
        manager.finish();
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        switch (item.getItemId()) {
        case MENUITEM_OSB:
            mode.finish();
            Note note = logic.makeNewBug(x, y);
            logic.setSelectedBug(note);
            TaskFragment.showDialog(main, note);
            logic.hideCrosshairs();
            return true;
        case MENUITEM_NEWNODEWAY:
            main.startSupportActionMode(new PathCreationActionModeCallback(manager, x, y));
            logic.hideCrosshairs();
            return true;
        case MENUITEM_SPLITWAY:
            if (clickedNonClosedWays.size() > 1) {
                main.getMap().showContextMenu();
            } else {
                Way way = clickedNonClosedWays.get(0);
                ArrayList<Way> ways = new ArrayList<>();
                ways.add(way);
                try {
                    Node node = logic.performAddOnWay(main, ways, startX, startY, false);
                    if (node != null) {
                        logic.performSplit(main, way, node);
                    }
                } catch (OsmIllegalOperationException e) {
                    Snack.barError(main, e.getLocalizedMessage());
                    Log.d(DEBUG_TAG, "Caught exception " + e);
                }
                manager.finish();
            }
            return true;
        case MENUITEM_NEWNODE_ADDRESS:
        case MENUITEM_NEWNODE_PRESET:
        case MENUITEM_NEWNODE_NAME:
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
                main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, lastSelectedNode));
                main.performTagEdit(lastSelectedNode, null, item.getItemId() == MENUITEM_NEWNODE_ADDRESS, item.getItemId() == MENUITEM_NEWNODE_PRESET,
                        item.getItemId() == MENUITEM_NEWNODE_NAME); // show preset screen or add addresses
            }
            return true;
        case MENUITEM_PASTE:
            logic.pasteFromClipboard(main, startX, startY);
            logic.hideCrosshairs();
            mode.finish();
            return true;
        case MENUITEM_NEWNODE_GPS:
            logic.hideCrosshairs();
            try {
                logic.setSelectedNode(null);
                logic.performAdd(main, x, y);
                Node node = logic.getSelectedNode();
                if (locationManager != null && node != null) {
                    Location location = null;
                    try {
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    } catch (SecurityException sex) {
                        // can be safely ignored, this is only called when GPS is enabled
                    }
                    if (location != null) {
                        double lon = location.getLongitude();
                        double lat = location.getLatitude();
                        if (Util.notZero(lon) || Util.notZero(lat)) {
                            if (lon >= -180 && lon <= 180 && lat >= -GeoMath.MAX_LAT && lat <= GeoMath.MAX_LAT) {
                                logic.performSetPosition(main, node, lon, lat);
                                TreeMap<String, String> tags = new TreeMap<>(node.getTags());
                                if (location.hasAltitude()) {
                                    tags.put(Tags.KEY_ELE, String.format(Locale.US, "%.1f", location.getAltitude()));
                                    tags.put(Tags.KEY_ELE_MSL, String.format(Locale.US, "%.1f", location.getAltitude()));
                                    tags.put(Tags.KEY_SOURCE_ELE, Tags.VALUE_GPS);
                                }
                                tags.put(Tags.KEY_SOURCE, Tags.VALUE_GPS);
                                logic.setTags(main, node, tags);
                            }
                        } else {
                            Snack.barError(main, R.string.toast_null_island);
                        }
                    }
                }
                manager.finish();
            } catch (OsmIllegalOperationException e) {
                Snack.barError(main, e.getLocalizedMessage());
                Log.d(DEBUG_TAG, "Caught exception " + e);
            }
            return true;
        case MENUITEM_NEWNODE_VOICE:
            logic.hideCrosshairs();
            logic.setSelectedNode(null);
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            try {
                main.startActivityForResult(intent, Main.VOICE_RECOGNITION_REQUEST_CODE);
            } catch (Exception ex) {
                Log.d(DEBUG_TAG, "Caught exception " + ex);
                Snack.barError(main, R.string.toast_no_voice_recognition);
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

    /**
     * Handle the result from starting an activity via an Intent
     * 
     * This is currently only used for experimental voice commands
     * 
     * FIXME This is still very hackish with lots of code duplication
     * 
     * @param requestCode the Intent request code
     * @param resultCode the Intent result code
     * @param data any Intent data
     */
    void handleActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == Main.VOICE_RECOGNITION_REQUEST_CODE) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //
            StorageDelegator storageDelegator = App.getDelegator();
            for (String v : matches) {
                String[] words = v.split("\\s+", 2);
                if (words.length > 0) {
                    //
                    String first = words[0];
                    try {
                        int number = Integer.parseInt(first);
                        // worked if there is a further word(s) simply add it/them
                        Snack.barInfoShort(main, +number + (words.length == 2 ? words[1] : ""));
                        Node node = logic.performAddNode(main, startLon / 1E7D, startLat / 1E7D);
                        if (node != null) {
                            TreeMap<String, String> tags = new TreeMap<>(node.getTags());
                            tags.put(Tags.KEY_ADDR_HOUSENUMBER, Integer.toString(number) + (words.length == 3 ? words[2] : ""));
                            tags.put(Commands.SOURCE_ORIGINAL_TEXT, v);
                            Map<String, ArrayList<String>> map = Address.predictAddressTags(main, Node.NAME, node.getOsmId(),
                                    new ElementSearch(new int[] { node.getLon(), node.getLat() }, true), Util.getArrayListMap(tags), Address.NO_HYSTERESIS);
                            tags = new TreeMap<>();
                            for (Entry<String, ArrayList<String>> entry : map.entrySet()) {
                                tags.put(entry.getKey(), entry.getValue().get(0));
                            }
                            logic.setTags(main, node, tags);
                            main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, node));
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        // ok wasn't a number, just ignore
                    } catch (OsmIllegalOperationException e) {
                        // FIXME something went seriously wrong
                        Log.e(DEBUG_TAG, "handleActivityResult got exception " + e.getMessage());
                    }

                    List<PresetElement> presetItems = SearchIndexUtils.searchInPresets(main, first, ElementType.NODE, 2, 1);

                    if (presetItems != null && presetItems.size() == 1) {
                        Node node = addNode(logic.performAddNode(main, startLon / 1E7D, startLat / 1E7D), words.length == 2 ? words[1] : null,
                                (PresetItem) presetItems.get(0), logic, v);
                        if (node != null) {
                            main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, node));
                            return;
                        }
                    }

                    // search in names
                    NameAndTags nt = SearchIndexUtils.searchInNames(main, v, 2);
                    if (nt != null) {
                        HashMap<String, String> map = new HashMap<>();
                        map.putAll(nt.getTags());
                        PresetItem pi = Preset.findBestMatch(App.getCurrentPresets(main), map);
                        if (pi != null) {
                            Node node = addNode(logic.performAddNode(main, startLon / 1E7D, startLat / 1E7D), nt.getName(), pi, logic, v);
                            if (node != null) {
                                // set tags from name suggestions
                                Map<String, String> tags = new TreeMap<>(node.getTags());
                                for (Entry<String, String> entry : map.entrySet()) {
                                    tags.put(entry.getKey(), entry.getValue());
                                }
                                storageDelegator.setTags(node, tags); // note doesn't create a new undo checkpoint,
                                                                      // performAddNode has already done that
                                main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, node));
                                return;
                            }
                        }
                    }
                }
            }
            logic.showCrosshairs(startX, startY); // re-show the cross hairs nothing found/something went wrong
        }
    }

    /**
     * Add a Node using a PresetItem and select it
     * 
     * @param node an existing Node
     * @param name a name or null
     * @param pi the PresetITem
     * @param logic the current instance of Logic
     * @param original the source string used to create the Node
     * @return the Node or null
     */
    @Nullable
    Node addNode(@NonNull Node node, @Nullable String name, @NonNull PresetItem pi, @NonNull Logic logic, @NonNull String original) {
        if (node != null) {
            try {
                Snack.barInfo(main, pi.getName() + (name != null ? " name: " + name : ""));
                TreeMap<String, String> tags = new TreeMap<>(node.getTags());
                for (Entry<String, PresetFixedField> tag : pi.getFixedTags().entrySet()) {
                    PresetFixedField field = tag.getValue();
                    tags.put(tag.getKey(), field.getValue().getValue());
                }
                if (name != null) {
                    tags.put(Tags.KEY_NAME, name);
                }
                tags.put(Commands.SOURCE_ORIGINAL_TEXT, original);
                logic.setTags(main, node, tags);
                logic.setSelectedNode(node);
                return node;
            } catch (OsmIllegalOperationException e) {
                Log.e(DEBUG_TAG, "addNode got exception " + e.getMessage());
                Snack.barError(main, e.getLocalizedMessage());
                return null;
            }
        }
        return null;
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
