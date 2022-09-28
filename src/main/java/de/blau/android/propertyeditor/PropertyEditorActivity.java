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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.Selection;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetGroup;
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
    public static void start(@NonNull Activity activity, @NonNull PropertyEditorData[] dataClass, boolean predictAddressTags, boolean showPresets,
            HashMap<String, String> extraTags, ArrayList<PresetElementPath> presetItems, int requestCode) {
        Log.d(DEBUG_TAG, "startFor");
        try {
            final Intent intent = buildIntent(activity, dataClass, predictAddressTags, showPresets, extraTags, presetItems);
            if (App.getPreferences(activity).useSplitWindowForPropertyEditor()) {
                activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT));
            } else {
                activity.startActivityForResult(intent, requestCode);
            }
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
        Log.d(DEBUG_TAG, "onCreate savedUnstanceState " + (savedInstanceState != null));

        Logic logic = App.getLogic();
        boolean reloadData = false;
        if (logic == null) {
            logic = App.newLogic();
            Log.i(DEBUG_TAG, "onCreate - creating new logic");
            reloadData = true;
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

        PostAsyncActionHandler postLoadData = () -> {
            if (savedInstanceState == null) { // adding to the backstack implies that restoring will happen
                                              // automatically
                addFromIntent(getIntent());
            }
        };

        if (reloadData && StorageDelegator.isStateAvailable(this) && !App.getDelegator().isDirty()) {
            logic.loadStateFromFile(this, postLoadData);
        } else {
            postLoadData.onSuccess();
        }
    }

    /**
     * Add the Fragment from an Intent
     * 
     * @param intent the Intent holding the required data
     */
    private void addFromIntent(@NonNull final Intent intent) {
        Log.d(DEBUG_TAG, "Adding from intent");

        PropertyEditorData[] loadData = PropertyEditorData.deserializeArray(intent.getSerializableExtra(PropertyEditorFragment.TAGEDIT_DATA));
        boolean applyLastAddressTags = getPrimitiveBoolean((Boolean) intent.getSerializableExtra(PropertyEditorFragment.TAGEDIT_LAST_ADDRESS_TAGS));
        boolean showPresets = getPrimitiveBoolean((Boolean) intent.getSerializableExtra(PropertyEditorFragment.TAGEDIT_SHOW_PRESETS));

        HashMap<String, String> extraTags = (HashMap<String, String>) intent.getSerializableExtra(PropertyEditorFragment.TAGEDIT_EXTRA_TAGS);
        ArrayList<PresetElementPath> presetsToApply = (ArrayList<PresetElementPath>) intent.getSerializableExtra(PropertyEditorFragment.TAGEDIT_PRESETSTOAPPLY);
        Boolean usePaneLayout = (Boolean) intent.getSerializableExtra(PropertyEditorFragment.PANELAYOUT);

        // if we have a preset to auto apply it doesn't make sense to show the Preset tab except if a group is
        // selected
        if (presetsToApply != null && !presetsToApply.isEmpty()) {
            PresetElement alternativeRootElement = Preset.getElementByPath(App.getCurrentRootPreset(this).getRootGroup(), presetsToApply.get(0));
            showPresets = alternativeRootElement instanceof PresetGroup;
        }

        Log.d(DEBUG_TAG, "... done.");

        // sanity check
        if (loadData == null) {
            abort("loadData null");
            return;
        }

        addFragment(getSupportFragmentManager(), android.R.id.content, loadData, applyLastAddressTags, showPresets, extraTags, presetsToApply, usePaneLayout);
    }

    /**
     * Get a boolean value from a potentially null Boolean object
     * 
     * @param value the Boolean
     * @return true or false
     */
    private boolean getPrimitiveBoolean(@Nullable Boolean value) {
        return value != null && value;
    }

    /**
     * Add an instance of the PropertyEditorFragment to an activity
     * 
     * @param fm the FragementManager for the activity
     * @param viewRes resource id for the view the fragment should be used for
     * @param data the tags and relation memberships that should be edited
     * @param predictAddressTags try to predict address tags
     * @param showPresets show the preset tab first
     * @param extraTags additional tags that should be added
     * @param presetsToApply presets that should be applied
     * @param usePaneLayout optional layout control
     */
    public static void addFragment(@NonNull FragmentManager fm, int viewRes, @NonNull PropertyEditorData[] data, boolean predictAddressTags,
            boolean showPresets, @Nullable HashMap<String, String> extraTags, @Nullable ArrayList<PresetElementPath> presetsToApply,
            @Nullable Boolean usePaneLayout) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment existing = peekBackStack(fm);
        if (existing != null) {
            ft.hide(existing);
        }
        PropertyEditorFragment fragment = PropertyEditorFragment.newInstance(data, predictAddressTags, showPresets, extraTags, presetsToApply, usePaneLayout);
        String tag = java.util.UUID.randomUUID().toString();
        ft.add(viewRes, fragment, tag);
        ft.addToBackStack(tag);
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
        Fragment fragment = peekBackStack(getSupportFragmentManager());
        if (item.getItemId() == android.R.id.home && fragment != null && fragment.getView() != null && fragment.onOptionsItemSelected(item)) {
            Log.d(DEBUG_TAG, "called fragment onOptionsItemSelected");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(DEBUG_TAG, "onNewIntent");
        addFromIntent(intent);
    }

    @Override
    public void onBackPressed() {
        Log.d(DEBUG_TAG, "onBackPressed");
        PropertyEditorFragment top = peekBackStack(getSupportFragmentManager());
        if (top != null && top.hasChanges()) {
            new AlertDialog.Builder(this).setNeutralButton(R.string.cancel, null).setNegativeButton(R.string.tag_menu_revert, (dialog, which) -> top.doRevert())
                    .setPositiveButton(R.string.tag_menu_exit_no_save, (dialog, which) -> finished(null)).create().show();
        } else {
            finished(null);
        }
    }

    @Override
    public void finished(@Nullable Fragment finishedFragment) {
        final FragmentManager fm = getSupportFragmentManager();
        int count = backStackCount(fm);
        if (count > 1) {
            fm.popBackStackImmediate();
            PropertyEditorFragment top = peekBackStack(fm);
            final boolean notWaiting = getCallingActivity() == null;
            if (top != null) { // still have a fragment on the stack
                FragmentTransaction ft = fm.beginTransaction();
                ft.show(top);
                if (notWaiting) { // calling activity is not waiting for us
                    startActivity(getIntent(Main.ACTION_POP_SELECTION));
                    return;
                }
            }
            if (notWaiting) { // calling activity is not waiting for us
                startActivity(getIntent(Main.ACTION_MAP_UPDATE));
            }
            return;
        }
        finish();
    }

    /**
     * Get an Intent suitable for sending to Main
     * 
     * @param action the action to carry out
     * @return an Intent
     */
    @NonNull
    private Intent getIntent(@NonNull String action) {
        Intent intent = new Intent(this, Main.class);
        intent.setAction(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    @Override
    public void addPropertyEditor(@NonNull OsmElement element) {
        final FragmentManager fm = getSupportFragmentManager();
        PropertyEditorFragment top = peekBackStack(fm);
        if (top != null && getCallingActivity() == null) {
            Intent intent = getIntent(Main.ACTION_PUSH_SELECTION);
            Selection selection = new Selection();
            selection.add(element);
            intent.putExtra(Selection.SELECTION_KEY, selection.getIds());
            startActivity(intent);
        }
        addFragment(fm, android.R.id.content, new PropertyEditorData[] { new PropertyEditorData(element, null) }, false, false, null, null,
                top != null && top.usingPaneLayout());
    }

    /**
     * Check if we are using the pane layout
     * 
     * @return true if we are using the pane layout
     */
    public boolean usingPaneLayout() {
        PropertyEditorFragment top = peekBackStack(getSupportFragmentManager());
        return top != null && top.usingPaneLayout();
    }

    /**
     * Get the top of the back stack
     * 
     * Only works if backstack name and fragment tag are the same
     * 
     * @param fm a FragmentManager
     * @return the Fragment or null
     */
    static PropertyEditorFragment peekBackStack(@NonNull FragmentManager fm) {
        int count = fm.getBackStackEntryCount();
        if (count > 0) {
            FragmentManager.BackStackEntry topBackStackEntry = fm.getBackStackEntryAt(count - 1);
            String tag = topBackStackEntry.getName();
            if (tag != null) {
                Fragment f = fm.findFragmentByTag(tag);
                if (f instanceof PropertyEditorFragment) {
                    return (PropertyEditorFragment) f;
                }
                Log.e(DEBUG_TAG, "Unexpected fragment " + f.getClass().getCanonicalName());
            }
        }
        return null;
    }

    /**
     * Get a count of propertyeditors on the backstack
     * 
     * Only works if backstack name and fragment tag are the same
     * 
     * @param fm a FragmentManager
     * @return a count of property editors on the backstack
     */
    static int backStackCount(@NonNull FragmentManager fm) {
        int count = fm.getBackStackEntryCount();
        int result = 0;
        for (int i = 1; i <= count; i++) {
            FragmentManager.BackStackEntry topBackStackEntry = fm.getBackStackEntryAt(count - i);
            String tag = topBackStackEntry.getName();
            if (tag != null) {
                Fragment f = fm.findFragmentByTag(tag);
                if (f instanceof PropertyEditorFragment) {
                    result++;
                }
            }
        }
        return result;
    }
}
