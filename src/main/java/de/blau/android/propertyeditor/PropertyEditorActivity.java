package de.blau.android.propertyeditor;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * An Activity to edit OSM-Tags. Sends the edited Tags as Result to its caller-Activity (normally {@link Main}).
 * 
 * The Editor does not directly edit the original tags or relation memberships which makes the code fairly and perhaps
 * unnecessarily complex
 * 
 * @author mb
 * @author simon
 */
public class PropertyEditorActivity<M extends Map<String, String> & Serializable, L extends List<PresetElementPath> & Serializable, T extends List<Map<String, String>> & Serializable>
        extends AppCompatActivity implements ControlListener {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PropertyEditorActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = PropertyEditorActivity.class.getSimpleName().substring(0, TAG_LEN);

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
    public static <M extends Map<String, String> & Serializable, L extends List<PresetElementPath> & Serializable> void start(@NonNull Activity activity,
            @NonNull PropertyEditorData[] dataClass, boolean predictAddressTags, boolean showPresets, M extraTags, L presetItems, int requestCode) {
        Log.d(DEBUG_TAG, "start");
        try {
            final Intent intent = buildIntent(activity, dataClass, predictAddressTags, showPresets, extraTags, presetItems);
            if (App.getPreferences(activity).useSplitWindowForPropertyEditor()) {
                activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT));
            } else if (App.getPreferences(activity).useNewTaskForPropertyEditor()) {
                activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                activity.startActivityForResult(intent, requestCode);
            }
        } catch (RuntimeException rex) {
            Log.e(DEBUG_TAG, rex.getMessage());
            ScreenMessage.toastTopError(activity, R.string.toast_error_element_too_large);
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
    static <M extends Map<String, String> & Serializable, L extends List<PresetElementPath> & Serializable> Intent buildIntent(@NonNull Activity activity,
            @NonNull PropertyEditorData[] dataClass, boolean predictAddressTags, boolean showPresets, M extraTags, L presetItems) {
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
            App.getDataStyle(this);
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
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @Override
    public void onTopResumedActivityChanged(boolean topResumed) {
        Log.d(DEBUG_TAG, "onTopResumedActivityChanged " + topResumed);
        if (topResumed) {
            PropertyEditorFragment<M, L, T> top = peekBackStack(getSupportFragmentManager());
            if (top != null) {
                top.onHiddenChanged(false);
            }
        }
    }

    /**
     * Add the Fragment from an Intent
     * 
     * @param intent the Intent holding the required data
     */
    private void addFromIntent(@NonNull final Intent intent) {
        Log.d(DEBUG_TAG, "Adding from intent");

        PropertyEditorData[] loadData = PropertyEditorData
                .deserializeArray(de.blau.android.util.Util.getSerializableExtra(intent, PropertyEditorFragment.TAGEDIT_DATA, PropertyEditorData[].class));
        boolean applyLastAddressTags = getPrimitiveBoolean(
                de.blau.android.util.Util.getSerializableExtra(intent, PropertyEditorFragment.TAGEDIT_LAST_ADDRESS_TAGS, Boolean.class));
        boolean showPresets = getPrimitiveBoolean(
                de.blau.android.util.Util.getSerializableExtra(intent, PropertyEditorFragment.TAGEDIT_SHOW_PRESETS, Boolean.class));

        M extraTags = (M) intent.getSerializableExtra(PropertyEditorFragment.TAGEDIT_EXTRA_TAGS);
        L presetsToApply = (L) intent.getSerializableExtra(PropertyEditorFragment.TAGEDIT_PRESETSTOAPPLY);
        Boolean usePaneLayout = de.blau.android.util.Util.getSerializableExtra(intent, PropertyEditorFragment.PANELAYOUT, Boolean.class);

        // if we have a preset to auto apply it doesn't make sense to show the Preset tab except if a group is
        // selected
        if (presetsToApply != null && !presetsToApply.isEmpty()) {
            final PresetElementPath path = presetsToApply.get(0);
            if (path != null) {
                PresetElement alternativeRootElement = Preset.getElementByPath(App.getCurrentRootPreset(this).getRootGroup(), path);
                showPresets = alternativeRootElement instanceof PresetGroup;
            } else {
                Log.e(DEBUG_TAG, "Preset path is null");
            }
        }

        Log.d(DEBUG_TAG, "... done.");

        // sanity check
        if (loadData.length == 0) {
            abort("loadData is empty");
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
    public void addFragment(@NonNull FragmentManager fm, int viewRes, @NonNull PropertyEditorData[] data, boolean predictAddressTags, boolean showPresets,
            @Nullable M extraTags, @Nullable L presetsToApply, @Nullable Boolean usePaneLayout) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment existing = peekBackStack(fm);
        if (existing != null) {
            ft.hide(existing);
        }
        PropertyEditorFragment<M, L, T> fragment = PropertyEditorFragment.newInstance(data, predictAddressTags, showPresets, extraTags, presetsToApply,
                usePaneLayout);
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
        ScreenMessage.toastTopError(this, R.string.toast_inconsistent_state);
        Log.e(DEBUG_TAG, "Inconsistent state because " + cause);
        ACRA.getErrorReporter().putCustomData("CAUSE", cause);
        ACRA.getErrorReporter().handleException(null);
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.recreate();
        App.setConfiguration(newConfig);
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

    /**
     * Handle the back button/key being pressed
     */
    private OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {

        @Override
        public void handleOnBackPressed() {
            Log.d(DEBUG_TAG, "onBackPressed");
            PropertyEditorFragment<M, L, T> top = peekBackStack(getSupportFragmentManager());
            if (top != null && top.hasChanges()) {
                ThemeUtils.getAlertDialogBuilder(PropertyEditorActivity.this).setNeutralButton(R.string.cancel, null)
                        .setNegativeButton(R.string.tag_menu_revert, (dialog, which) -> top.doRevert())
                        .setPositiveButton(R.string.tag_menu_exit_no_save, (dialog, which) -> finished(null)).create().show();
                return;
            }
            finished(null);
        }
    };

    @Override
    public void finished(@Nullable Fragment finishedFragment) {
        final FragmentManager fm = getSupportFragmentManager();
        int count = backStackCount(fm);
        // calling activity is not waiting for us
        final boolean notWaiting = getCallingActivity() == null;
        final boolean multiWindow = Util.isInMultiWindowModeCompat(this);
        if (count > 1) {
            fm.popBackStackImmediate();
            PropertyEditorFragment<M, L, T> top = peekBackStack(fm);
            if (top != null) { // still have a fragment on the stack
                FragmentTransaction ft = fm.beginTransaction();
                ft.show(top);
                ft.commit();
                if (notWaiting && multiWindow) {
                    startActivity(getIntent(Main.ACTION_POP_SELECTION));
                    return;
                }
            }
            if (notWaiting && multiWindow) {
                startActivity(getIntent(Main.ACTION_MAP_UPDATE));
            }
            return;
        }
        finish();
        if (notWaiting) {
            startActivity(getIntent(Main.ACTION_CLEAR_SELECTION_STACK));
        }
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
        PropertyEditorFragment<M, L, T> top = peekBackStack(fm);
        if (top != null && getCallingActivity() == null && Util.isInMultiWindowModeCompat(this)) {
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
        PropertyEditorFragment<M, L, T> top = peekBackStack(getSupportFragmentManager());
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
    @Nullable
    PropertyEditorFragment<M, L, T> peekBackStack(@NonNull FragmentManager fm) {
        int count = fm.getBackStackEntryCount();
        if (count > 0) {
            FragmentManager.BackStackEntry topBackStackEntry = fm.getBackStackEntryAt(count - 1);
            String tag = topBackStackEntry.getName();
            if (tag != null) {
                Fragment f = fm.findFragmentByTag(tag);
                if (f instanceof PropertyEditorFragment) {
                    return (PropertyEditorFragment<M, L, T>) f;
                }
                Log.e(DEBUG_TAG, "Unexpected fragment " + (f != null ? f.getClass().getCanonicalName() : " is null"));
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
