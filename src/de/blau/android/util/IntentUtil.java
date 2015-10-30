package de.blau.android.util;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import de.blau.android.BoxPicker;
import de.blau.android.HelpViewer;
import de.blau.android.Main;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.PresetEditorActivity;
import de.blau.android.prefs.URLListEditActivity;
import de.blau.android.propertyeditor.PropertyEditor;
import de.blau.android.propertyeditor.PropertyEditorData;
import de.blau.android.services.TrackerService;

public abstract class IntentUtil {

    public static
    @NonNull
    Intent getBoxPickerIntent(@NonNull Context context) {
        return getIntent(context, BoxPicker.class);
    }

    public static
    @NonNull
    Intent getHelpViewerIntent(@NonNull Context context, @StringRes int topic) {
        Intent intent = getIntent(context, HelpViewer.class);
        intent.putExtra(HelpViewer.TOPIC, topic);
        return intent;
    }

    public static
    @NonNull
    Intent getMainIntent(@NonNull Context context) {
        return getIntent(context, Main.class);
    }

    public static
    @NonNull
    Intent getPrefEditorIntent(@NonNull Context context) {
        return getIntent(context, PrefEditor.class);
    }

    public static
    @NonNull
    Intent getPresetEditorActivityIntent(@NonNull Context context,
                                         @NonNull String presetName,
                                         @NonNull String presetUrl) {
        Intent intent = getPresetEditorActivityIntent(context);
        intent.setAction(URLListEditActivity.ACTION_NEW);
        intent.putExtra(URLListEditActivity.EXTRA_NAME, presetName);
        intent.putExtra(URLListEditActivity.EXTRA_VALUE, presetUrl);
        return intent;
    }

    public static
    @NonNull
    Intent getPresetEditorActivityIntent(@NonNull Context context) {
        return getIntent(context, PresetEditorActivity.class);
    }

    public static
    @NonNull
    Intent getPropertyEditorIntent(@NonNull Context context,
                                   @NonNull PropertyEditorData[] dataClass,
                                   boolean applyLastTags,
                                   boolean showPresets) {
        Intent intent = getIntent(context, PropertyEditor.class);
        intent.putExtra(PropertyEditor.TAGEDIT_DATA, dataClass);
        intent.putExtra(PropertyEditor.TAGEDIT_LAST_ADDRESS_TAGS, Boolean.valueOf(applyLastTags));
        intent.putExtra(PropertyEditor.TAGEDIT_SHOW_PRESETS, Boolean.valueOf(showPresets));
        return intent;
    }

    public static
    @NonNull
    Intent getTrackerServiceIntent(@NonNull Context context) {
        return getIntent(context, TrackerService.class);
    }

    private static
    @NonNull
    Intent getIntent(@NonNull Context context, @NonNull Class<?> clazz) {
        return new Intent(context, clazz);
    }

}
