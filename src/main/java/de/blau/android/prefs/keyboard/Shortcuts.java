package de.blau.android.prefs.keyboard;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.NonNull;

public class Shortcuts {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Shortcuts.class.getSimpleName().length());
    private static final String DEBUG_TAG = Shortcuts.class.getSimpleName().substring(0, TAG_LEN);

    public static class Action {
        final int             descriptionResource;
        public final Runnable runnable;

        public Action(int res, Runnable action) {
            descriptionResource = res;
            this.runnable = action;
        }
    }

    public enum Modifier {
        NONE, ALT, CTRL, META;

        public static Modifier fromState(int state) {
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

    public static class Shortcut {

        private Modifier modifier;
        private char     character;
        private String   actionRef;

        /**
         * Construct a shortcut
         * 
         * @param modifier the modifier key
         * @param codepoint the codepoint for the character
         * @param actionRef a reference for an action
         */
        public Shortcut(@NonNull Modifier modifier, int codepoint, @NonNull String actionRef) {
            this.modifier = modifier;
            setCharacter((char) codepoint);
            this.actionRef = actionRef;
        }

        /**
         * @return the action
         */
        public String getActionRef() {
            return actionRef;
        }

        /**
         * @param actionRef the action to set
         */
        public void setActionRef(String actionRef) {
            this.actionRef = actionRef;
        }

        /**
         * @return the character
         */
        public char getCharacter() {
            return character;
        }

        /**
         * @param character the character to set
         */
        public void setCharacter(char character) {
            this.character = character;
        }

        /**
         * @return the metaKey
         */
        public Modifier getModifier() {
            return modifier;
        }

        /**
         * @param modifier the metaKey to set
         */
        public void setModifier(Modifier modifier) {
            this.modifier = modifier;
        }

    }

    private final java.util.Map<Shortcuts.Modifier, java.util.Map<Character, String>> keyMap = new EnumMap<>(Shortcuts.Modifier.class);

    public Shortcuts(@NonNull Context ctx) {

        //
        java.util.Map<Character, String> noneMap = new HashMap<>();
        java.util.Map<Character, String> ctrlMap = new HashMap<>();
        java.util.Map<Character, String> altMap = new HashMap<>();
        java.util.Map<Character, String> metaMap = new HashMap<>();
        keyMap.put(Modifier.NONE, noneMap);
        keyMap.put(Modifier.CTRL, ctrlMap);
        keyMap.put(Modifier.ALT, altMap);
        keyMap.put(Modifier.META, metaMap);

        try (ShortcutsHelper helper = new ShortcutsHelper(ctx); SQLiteDatabase db = helper.getReadableDatabase()) {
            for (Shortcut s : helper.getShortcuts(db)) {
                switch (s.getModifier()) {
                case NONE:
                    noneMap.put(s.getCharacter(), s.getActionRef());
                    break;
                case CTRL:
                    ctrlMap.put(s.getCharacter(), s.getActionRef());
                    break;
                case ALT:
                    altMap.put(s.getCharacter(), s.getActionRef());
                    break;
                case META:
                    metaMap.put(s.getCharacter(), s.getActionRef());
                    break;
                }
            }
        }
    }

    /**
     * Execute the associated action for a shortcut
     * 
     * @param event the KeyEvent
     * @param shortcut the shortcut char
     * @param actionMap the map holding the actions
     * @return true if an action was found and executed
     */
    public boolean execute(@NonNull KeyEvent event, @NonNull Character shortcut, @NonNull Map<String, Shortcuts.Action> actionMap) {
        return execute(Shortcuts.Modifier.fromState(event.getMetaState()), shortcut, actionMap);
    }

    /**
     * Execute the associated action for a shortcut
     * 
     * @param metaKey the MetaKey
     * @param shortcut the shortcut char
     * @param actionMap the map holding the actions
     * @return true if an action was found and executed
     */
    public boolean execute(@NonNull Modifier metaKey, @NonNull Character shortcut, @NonNull Map<String, Shortcuts.Action> actionMap) {
        Log.d(DEBUG_TAG, "shortcut " + shortcut + " " + metaKey);
        java.util.Map<Character, String> shortcutMap = keyMap.get(metaKey);
        if (shortcutMap != null) {
            String actionString = shortcutMap.get(shortcut);
            if (actionString != null) {
                Shortcuts.Action action = actionMap.get(actionString);
                if (action != null) {
                    action.runnable.run();
                    return true;
                }
            }
        }
        return false;
    }
}
