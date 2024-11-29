package de.blau.android.easyedit;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.HashMap;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.SerializableState;
import de.blau.android.util.ThemeUtils;

public class WaySegmentModifyActionModeCallback extends NonSimpleActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, WaySegmentModifyActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = WaySegmentModifyActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENUITEM_BRIDGE  = 24;
    private static final int MENUITEM_TUNNEL  = 25;
    private static final int MENUITEM_CULVERT = 26;
    private static final int MENUITEM_STEPS   = 27;

    private final Way way;

    /**
     * Construct a new ActionModeCallback
     * 
     * @param manager the EasyEditManager instance
     * @param way the selected Way
     */
    WaySegmentModifyActionModeCallback(EasyEditManager manager, Way way) {
        super(manager);
        Log.d(DEBUG_TAG, "constructor");
        this.way = way;
    }

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public WaySegmentModifyActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager);
        way = getSavedWay(state);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_extractsegment;
        super.onCreateActionMode(mode, menu);
        Log.d(DEBUG_TAG, "onCreateActionMode");
        logic.setSelectedWay(way);
        main.invalidateMap();
        mode.setTitle(R.string.menu_extract_segment);
        mode.setSubtitle(R.string.actionmode_extract_segment_set_tags);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        super.onPrepareActionMode(mode, menu);
        Log.d(DEBUG_TAG, "onPrepareActionMode");
        menu.add(Menu.NONE, ElementSelectionActionModeCallback.MENUITEM_TAG, Menu.NONE, R.string.menu_tags)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_tags));
        if (way.hasTagKey(Tags.KEY_HIGHWAY)) {
            menu.add(Menu.NONE, MENUITEM_BRIDGE, Menu.NONE, R.string.bridge).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_bridge));
            menu.add(Menu.NONE, MENUITEM_TUNNEL, Menu.NONE, R.string.tunnel).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_tunnel));
            if (way.hasTagWithValue(Tags.KEY_HIGHWAY, Tags.VALUE_FOOTWAY) || way.hasTagWithValue(Tags.KEY_HIGHWAY, Tags.VALUE_PATH)
                    || way.hasTagWithValue(Tags.KEY_HIGHWAY, Tags.VALUE_CYCLEWAY)) {
                menu.add(Menu.NONE, MENUITEM_STEPS, Menu.NONE, R.string.steps).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_steps));
            }
        }
        if (way.hasTagKey(Tags.KEY_WATERWAY)) {
            menu.add(Menu.NONE, MENUITEM_CULVERT, Menu.NONE, R.string.culvert).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_culvert));
        }
        arrangeMenu(menu);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!super.onActionItemClicked(mode, item)) {
            manager.finish();
            main.startSupportActionMode(new WaySelectionActionModeCallback(manager, way));
            HashMap<String, String> tags = new HashMap<>();
            switch (item.getItemId()) {
            case ElementSelectionActionModeCallback.MENUITEM_TAG:
                main.performTagEdit(way, null, false, false);
                break;
            case MENUITEM_BRIDGE:
                tags.put(Tags.KEY_BRIDGE, Tags.VALUE_YES);
                tags.put(Tags.KEY_LAYER, "1");
                main.performTagEdit(way, null, tags, false);
                break;
            case MENUITEM_TUNNEL:
            case MENUITEM_CULVERT:
                tags.put(Tags.KEY_TUNNEL, item.getItemId() == MENUITEM_CULVERT ? Tags.VALUE_CULVERT : Tags.VALUE_YES);
                tags.put(Tags.KEY_LAYER, "-1");
                main.performTagEdit(way, null, tags, false);
                break;
            case MENUITEM_STEPS:
                tags.put(Tags.KEY_HIGHWAY, Tags.VALUE_STEPS);
                main.performTagEdit(way, null, tags, false);
                break;
            default:
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onCloseClicked() {
        onBackPressed();
    }

    @Override
    public boolean onBackPressed() {
        new AlertDialog.Builder(main).setTitle(R.string.abort_action_title).setPositiveButton(R.string.yes, (dialog, which) -> {
            logic.rollback();
            super.onBackPressed();
        }).setNeutralButton(R.string.cancel, null).show();
        return false;
    }

    @Override
    public void saveState(SerializableState state) {
        state.putLong(WAY_ID_KEY, way.getOsmId());
    }
}
