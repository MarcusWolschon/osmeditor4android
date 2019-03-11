package de.blau.android.filter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.Snack;

/**
 * Activity for editing filter entries. Due to the diffiulties in using a ListView for editable items, this is a rather
 * hackish and inefficient, but given that we are only going to have a small number of items likely OK.
 * 
 * @author simon
 *
 */
public class PresetFilterActivity extends AppCompatActivity implements PresetClickHandler {
    private static final String DEBUG_TAG = "PresetFilterActivity";

    PresetItem  currentItem  = null;
    PresetGroup rootGroup    = null;
    PresetGroup currentGroup = null;
    ScrollView  presetView   = null;

    PresetFilter filter = null;

    /**
     * Start a new instance of this activity
     * 
     * @param context Android Context
     */
    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, PresetFilterActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customActionBar_Light);
        }
        super.onCreate(savedInstanceState);
        try {
            filter = (PresetFilter) App.getLogic().getFilter();
        } catch (ClassCastException ccex) {
        }
        if (filter == null) {
            Snack.barError(this, "illegal state " + filter);
            finish();
        }

        Preset preset = App.getCurrentRootPreset(this);
        rootGroup = preset.getRootGroup();

        PresetElement element = filter.getPresetElement();
        Log.e(DEBUG_TAG, "filter element " + element);
        PresetGroup parent = element != null ? element.getParent() : null;
        Log.e(DEBUG_TAG, "parent " + parent);
        currentGroup = parent != null ? parent : rootGroup;

        presetView = getPresetView(currentGroup, element);
        setContentView(presetView);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Get a tabular view of the presets
     * 
     * @param group the PresetGroup to display
     * @param element the PresetELement to highlight or null
     * @return a View of the PresetGroup
     */
    private ScrollView getPresetView(@NonNull PresetGroup group, @Nullable PresetElement element) {
        View view = group.getGroupView(this, this, null, element);
        view.setId(R.id.preset_view);
        return (ScrollView) view;
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.presetfilter_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.preset_menu_top).setEnabled(currentGroup != rootGroup);
        menu.findItem(R.id.preset_menu_up).setEnabled(currentGroup != rootGroup);
        Filter filter = App.getLogic().getFilter();
        if (!(filter instanceof PresetFilter)) {
            Log.e(DEBUG_TAG, "filter null or not a PresetFilter");
            return true;
        }
        menu.findItem(R.id.preset_menu_waynodes).setChecked(((PresetFilter) filter).includeWayNodes());
        // don't show for now
        menu.findItem(R.id.preset_menu_invert).setChecked(((PresetFilter) filter).isInverted()).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Filter filter = App.getLogic().getFilter();
        if (!(filter instanceof PresetFilter)) {
            Log.e(DEBUG_TAG, "filter null or not a PresetFilter");
            return true;
        }
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        case R.id.preset_menu_top:
            if (rootGroup != null) {
                currentGroup = rootGroup;
                currentGroup.getGroupView(this, presetView, this, null, ((PresetFilter) filter).getPresetElement());
                presetView.invalidate();
                supportInvalidateOptionsMenu();
                return true;
            }
            return true;
        case R.id.preset_menu_up:
            if (currentGroup != null) {
                PresetGroup group = currentGroup.getParent();
                if (group != null) {
                    currentGroup = group;
                    currentGroup.getGroupView(this, presetView, this, null, ((PresetFilter) filter).getPresetElement());
                    presetView.invalidate();
                    supportInvalidateOptionsMenu();
                    return true;
                }
            }
            return true;
        case R.id.preset_menu_waynodes:
            item.setChecked(!((PresetFilter) filter).includeWayNodes());
            ((PresetFilter) filter).setIncludeWayNodes(item.isChecked());
            ((PresetFilter) filter).clear();
            break;
        case R.id.preset_menu_invert:
            item.setChecked(!((PresetFilter) filter).isInverted());
            ((PresetFilter) filter).setInverted(item.isChecked());
            ((PresetFilter) filter).clear();
            break;
        case R.id.menu_help:
            HelpViewer.start(this, R.string.help_presetfilter);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /**
     * Handle clicks on icons representing an item (closing the dialog with the item as a result)
     */
    @Override
    public void onItemClick(PresetItem item) {
        onPresetElementSelected(item);
    }

    /**
     * for now do the same
     */
    @Override
    public boolean onItemLongClick(PresetItem item) {
        onPresetElementSelected(item);
        return true;
    }

    /**
     * Handle clicks on icons representing a group (changing to that group)
     */
    @Override
    public void onGroupClick(PresetGroup group) {
        Filter filter = App.getLogic().getFilter();
        if (!(filter instanceof PresetFilter)) {
            Log.e(DEBUG_TAG, "filter null or not a PresetFilter");
            return;
        }
        currentGroup = group;
        currentGroup.getGroupView(this, presetView, this, null, ((PresetFilter) filter).getPresetElement());
        presetView.invalidate();
        supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onGroupLongClick(PresetGroup group) {
        onPresetElementSelected(group);
        return true;
    }

    /**
     * Select element for the filter and display a toast
     * 
     * @param element the selected PresetElement
     */
    void onPresetElementSelected(PresetElement element) {
        Log.d(DEBUG_TAG, element.toString());
        Filter filter = App.getLogic().getFilter();
        if (filter instanceof PresetFilter) {
            ((PresetFilter) filter).setPresetElement(element.getPath(rootGroup));
            currentGroup.getGroupView(this, presetView, this, null, element);
            presetView.invalidate();
        }
        finish();
    }
}
