package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import de.blau.android.R;

public class KeyboardShortcut {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, KeyboardShortcut.class.getSimpleName().length());
    private static final String DEBUG_TAG = KeyboardShortcut.class.getSimpleName().substring(0, TAG_LEN);

    public static final String ACTION_ZOOM_OUT      = "action_zoom_out";
    public static final String ACTION_ZOOM_IN       = "action_zoom_in";
    public static final String ACTION_BUG_DOWNLOAD  = "action_bug_download";
    public static final String ACTION_DOWNLOAD      = "action_download";
    public static final String ACTION_GPS_GOTO      = "action_gps_goto";
    public static final String ACTION_GPS_FOLLOW    = "action_gps_follow";
    public static final String ACTION_UNDO          = "action_undo";
    public static final String ACTION_HELP          = "action_help";
    public static final String ACTION_ELEMENT_PASTE = "action_element_paste";
    public static final String ACTION_COPY          = "action_copy";
    public static final String ACTION_CUT           = "action_cut";
    public static final String ACTION_INFO          = "action_info";
    public static final String ACTION_TAGEDIT       = "action_tagedit";
    public static final String ACTION_PASTE_TAGS    = "action_paste_tags";
    public static final String ACTION_DELETE        = "action_delete";
    public static final String ACTION_SQUARE        = "action_square";
    public static final String ACTION_FOLLOW        = "action_follow";
    public static final String ACTION_ADDRESS       = "action_address";
    public static final String ACTION_MERGE         = "action_merge";
    public static final String ACTION_KEEP          = "action_keep";

    public static class Action {
        final int             descriptionResource;
        public final Runnable action;

        public Action(int res, Runnable action) {
            descriptionResource = res;
            this.action = action;
        }
    }

    public enum MetaKey {
        NONE, ALT, CTRL, META;

        public static MetaKey fromState(int state) {
            if ((state & KeyEvent.META_CTRL_MASK) != 0) {
                return CTRL;
            }
            if ((state & KeyEvent.META_ALT_MASK) != 0) {
                return ALT;
            }
            if ((state & KeyEvent.META_META_MASK) != 0) {
                return META;
            }
            return NONE;
        }
    }

    private final java.util.Map<KeyboardShortcut.MetaKey, java.util.Map<Character, String>> keyMap = new EnumMap<>(KeyboardShortcut.MetaKey.class);

    public KeyboardShortcut(@NonNull Context ctx) {
        java.util.Map<Character, String> noneMap = new HashMap<>();
        noneMap.put(Util.getShortCut(ctx, R.string.shortcut_zoom_in), KeyboardShortcut.ACTION_ZOOM_IN);
        noneMap.put(Util.getShortCut(ctx, R.string.shortcut_zoom_out), KeyboardShortcut.ACTION_ZOOM_OUT);
        keyMap.put(MetaKey.NONE, noneMap);
        java.util.Map<Character, String> ctrlMap = new HashMap<>();
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_help), KeyboardShortcut.ACTION_HELP);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_undo), KeyboardShortcut.ACTION_UNDO);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_gps_follow), KeyboardShortcut.ACTION_GPS_FOLLOW);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_gps_goto), KeyboardShortcut.ACTION_GPS_GOTO);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_download), KeyboardShortcut.ACTION_DOWNLOAD);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_bugs_download), KeyboardShortcut.ACTION_BUG_DOWNLOAD);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_paste), KeyboardShortcut.ACTION_ELEMENT_PASTE);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_paste_tags), KeyboardShortcut.ACTION_PASTE_TAGS);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_tagedit), KeyboardShortcut.ACTION_TAGEDIT);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_info), KeyboardShortcut.ACTION_INFO);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_copy), KeyboardShortcut.ACTION_COPY);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_cut), KeyboardShortcut.ACTION_CUT);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_square), KeyboardShortcut.ACTION_SQUARE);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_follow), KeyboardShortcut.ACTION_FOLLOW);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_address), KeyboardShortcut.ACTION_ADDRESS);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_remove), KeyboardShortcut.ACTION_DELETE);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_merge), KeyboardShortcut.ACTION_MERGE);
        ctrlMap.put(Util.getShortCut(ctx, R.string.shortcut_keep), KeyboardShortcut.ACTION_KEEP);
        keyMap.put(MetaKey.ALT, ctrlMap);
    }

    public boolean executeShortcut(@NonNull KeyEvent event, @NonNull Character shortcut, @NonNull Map<String, KeyboardShortcut.Action> actionMap) {
        return executeShortcut(KeyboardShortcut.MetaKey.fromState(event.getMetaState()), shortcut, actionMap);
    }

    public boolean executeShortcut(@NonNull MetaKey metaKey, @NonNull Character shortcut, @NonNull Map<String, KeyboardShortcut.Action> actionMap) {
        Log.d(DEBUG_TAG, "shortcut " + shortcut + " " + metaKey);
        java.util.Map<Character, String> shortcutMap = keyMap.get(metaKey);
        if (shortcutMap != null) {
            String actionString = shortcutMap.get(shortcut);
            if (actionString != null) {
                KeyboardShortcut.Action action = actionMap.get(actionString);
                if (action != null) {
                    action.action.run();
                    return true;
                }
            }
        }
        return false;
    }
}
