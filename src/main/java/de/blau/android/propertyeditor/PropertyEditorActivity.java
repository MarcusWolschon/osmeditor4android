package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.HashMap;

import org.acra.ACRA;

import com.zeugmasolutions.localehelper.LocaleAwareCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetGroup;
import de.blau.android.util.Screen;
import de.blau.android.util.Snack;

/**
 * An Activity to edit OSM-Tags. Sends the edited Tags as Result to its caller-Activity (normally {@link Main}).
 * 
 * The Editor does not directly edit the original tags or relation memberships which makes the code fairly and perhaps
 * unnecessarily complex
 * 
 * @author mb
 * @author simon
 */
public class PropertyEditorActivity extends LocaleAwareCompatActivity implements ControlListener {

    private static final String DEBUG_TAG = PropertyEditorActivity.class.getSimpleName();

    /**
     * Start a PropertyEditor activity
     * 
     * @param activity calling activity
     * @param dataClass the tags and relation memberships that should be edited
     * @param predictAddressTags try to predict address tags
     * @param showPresets show the preset tab first
     * @param extraTags additional tags that should be added
     * @param presetItems presets that should be applied
     * @param requestCode request code for the response
     */
    public static void startForResult(@NonNull Activity activity, @NonNull PropertyEditorData[] dataClass, boolean predictAddressTags, boolean showPresets,
            HashMap<String, String> extraTags, ArrayList<PresetElementPath> presetItems, int requestCode) {
        Log.d(DEBUG_TAG, "startForResult");
        try {
            activity.startActivityForResult(buildIntent(activity, dataClass, predictAddressTags, showPresets, extraTags, presetItems), requestCode);
        } catch (RuntimeException rex) {
            Log.e(DEBUG_TAG, rex.getMessage());
            Snack.toastTopError(activity, R.string.toast_error_element_too_large);
        }
    }

    /**
     * Build the intent to start the PropertyEditor
     * 
     * @param activity calling activity
     * @param dataClass the tags and relation memberships that should be edited
     * @param predictAddressTags try to predict address tags
     * @param showPresets show the preset tab first
     * @param extraTags additional tags that should be added
     * @param presetItems presets that should be applied
     * @return a suitable Intent
     */
    @NonNull
    static Intent buildIntent(@NonNull Activity activity, @NonNull PropertyEditorData[] dataClass, boolean predictAddressTags, boolean showPresets,
            HashMap<String, String> extraTags, ArrayList<PresetElementPath> presetItems) {
        Intent intent = new Intent(activity, PropertyEditorActivity.class);
        intent.putExtra(PropertyEditorFragment.TAGEDIT_DATA, dataClass);
        intent.putExtra(PropertyEditorFragment.TAGEDIT_LAST_ADDRESS_TAGS, Boolean.valueOf(predictAddressTags));
        intent.putExtra(PropertyEditorFragment.TAGEDIT_SHOW_PRESETS, Boolean.valueOf(showPresets));
        intent.putExtra(PropertyEditorFragment.TAGEDIT_EXTRA_TAGS, extraTags);
        intent.putExtra(PropertyEditorFragment.TAGEDIT_PRESETSTOAPPLY, presetItems);
        return intent;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        int currentItem = -1; // used when restoring
        Logic logic = App.getLogic();
        if (logic == null) {
            super.onCreate(savedInstanceState); // have to call through first
            // cause for this is currently unknown, but it isn't recoverable
            abort("Logic is null");
            return;
        }
        Preferences prefs = logic.getPrefs();
        if (prefs == null) {
            Log.e(DEBUG_TAG, "prefs was null creating new");
            prefs = new Preferences(this);
            logic.setPrefs(prefs);
        }
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customTagEditor_Light);
        }

        super.onCreate(savedInstanceState);

        // tags
        // if (savedInstanceState == null) {
        // No previous state to restore - get the state from the intent
        Log.d(DEBUG_TAG, "Initializing from intent");
        PropertyEditorData[] loadData = PropertyEditorData.deserializeArray(getIntent().getSerializableExtra(PropertyEditorFragment.TAGEDIT_DATA));
        boolean applyLastAddressTags = (Boolean) getIntent().getSerializableExtra(PropertyEditorFragment.TAGEDIT_LAST_ADDRESS_TAGS);
        boolean showPresets = (Boolean) getIntent().getSerializableExtra(PropertyEditorFragment.TAGEDIT_SHOW_PRESETS);
        HashMap<String, String> extraTags = (HashMap<String, String>) getIntent().getSerializableExtra(PropertyEditorFragment.TAGEDIT_EXTRA_TAGS);
        ArrayList<PresetElementPath> presetsToApply = (ArrayList<PresetElementPath>) getIntent()
                .getSerializableExtra(PropertyEditorFragment.TAGEDIT_PRESETSTOAPPLY);
        Boolean tempUsePaneLayout = (Boolean) getIntent().getSerializableExtra(PropertyEditorFragment.PANELAYOUT);
        boolean usePaneLayout = tempUsePaneLayout != null ? tempUsePaneLayout : Screen.isLandscape(this);

        // if we have a preset to auto apply it doesn't make sense to show the Preset tab except if a group is
        // selected
        if (presetsToApply != null && !presetsToApply.isEmpty()) {
            PresetElement alternativeRootElement = Preset.getElementByPath(App.getCurrentRootPreset(this).getRootGroup(), presetsToApply.get(0));
            showPresets = alternativeRootElement instanceof PresetGroup;
        }
        // }
        Log.d(DEBUG_TAG, "... done.");

        // sanity check
        StorageDelegator delegator = App.getDelegator();
        if (delegator == null || loadData == null) {
            abort(delegator == null ? "Delegator null" : "loadData null");
            return;
        }

        FragmentManager fm = getSupportFragmentManager();

        FragmentTransaction ft = fm.beginTransaction();
        PropertyEditorFragment fragment = PropertyEditorFragment.newInstance(loadData, applyLastAddressTags, showPresets, extraTags, presetsToApply);
        ft.add(android.R.id.content, fragment, "PROPERTYEDITOR");

        ft.commit();

    }

    /**
     * Abort this activity
     * 
     * @param cause String showing a cause for this
     */
    private void abort(@NonNull String cause) {
        Snack.toastTopError(this, R.string.toast_inconsistent_state);
        Log.e(DEBUG_TAG, "Inconsistent state because " + cause);
        ACRA.getErrorReporter().putCustomData("CAUSE", cause);
        ACRA.getErrorReporter().handleException(null);
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.recreate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Due to a problem of not being able to intercept android.R.id.home in fragments on older android versions
        // we start passing the event to the currently displayed fragment.
        // REF: http://stackoverflow.com/questions/21938419/intercepting-actionbar-home-button-in-fragment
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("PROPERTYEDITOR");
        if (item.getItemId() == android.R.id.home && fragment != null && fragment.getView() != null && fragment.onOptionsItemSelected(item)) {
            Log.d(DEBUG_TAG, "called fragment onOptionsItemSelected");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.d(DEBUG_TAG, "onBackPressed");
        PropertyEditorFragment fragment = (PropertyEditorFragment) getSupportFragmentManager().findFragmentByTag("PROPERTYEDITOR");
        if (fragment.hasChanges()) {
            new AlertDialog.Builder(this).setNeutralButton(R.string.cancel, null)
                    .setNegativeButton(R.string.tag_menu_revert, (dialog, which) -> fragment.doRevert())
                    .setPositiveButton(R.string.tag_menu_exit_no_save, (dialog, which) -> PropertyEditorActivity.super.onBackPressed()).create().show();
        } else {
            PropertyEditorActivity.super.onBackPressed();
        }
    }

    @Override
    /**
     * Workaround for bug mentioned below
     */
    public ActionMode startSupportActionMode(@NonNull final ActionMode.Callback callback) {
        // Fix for bug https://code.google.com/p/android/issues/detail?id=159527
        final ActionMode mode = super.startSupportActionMode(callback);
        if (mode != null) {
            mode.invalidate();
        }
        return mode;
    }

    @Override
    public void finished(Fragment finishedFragment) {
        finish();
    }
}
