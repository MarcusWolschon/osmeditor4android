package de.blau.android.filter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
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
 * Activity for editing filter entries. Due to the difficulties in using a ListView for editable items, this is a rather
 * hackish and inefficient, but given that we are only going to have a small number of items likely OK.
 * 
 * @author simon
 *
 */
public class PresetFilterActivity extends AppCompatActivity implements PresetClickHandler {

    private static final String DEBUG_TAG = "PresetFilterActivity";

    private static final String FILTER_NULL_OR_NOT_A_PRESET_FILTER = "filter null or not a PresetFilter";

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
            // IGNORE
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
        View view = group.getGroupView(this, this, null, element, null);
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
        if (!(App.getLogic().getFilter() instanceof PresetFilter)) {
            Log.e(DEBUG_TAG, FILTER_NULL_OR_NOT_A_PRESET_FILTER);
            return true;
        }
        filter = (PresetFilter) App.getLogic().getFilter();
        menu.findItem(R.id.preset_menu_waynodes).setChecked(filter.includeWayNodes());
        // don't show for now
        menu.findItem(R.id.preset_menu_invert).setChecked(filter.isInverted()).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!(App.getLogic().getFilter() instanceof PresetFilter)) {
            Log.e(DEBUG_TAG, FILTER_NULL_OR_NOT_A_PRESET_FILTER);
            return true;
        }
        filter = (PresetFilter) App.getLogic().getFilter();

        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        case R.id.preset_menu_top:
            if (rootGroup != null) {
                currentGroup = rootGroup;
                currentGroup.getGroupView(this, presetView, this, null, filter.getPresetElement(), null);
                presetView.invalidate();
                invalidateOptionsMenu();
                return true;
            }
            return true;
        case R.id.preset_menu_up:
            if (currentGroup != null) {
                PresetGroup group = currentGroup.getParent();
                if (group != null) {
                    currentGroup = group;
                    currentGroup.getGroupView(this, presetView, this, null, filter.getPresetElement(), null);
                    presetView.invalidate();
                    invalidateOptionsMenu();
                    return true;
                }
            }
            return true;
        case R.id.preset_menu_waynodes:
            item.setChecked(!filter.includeWayNodes());
            filter.setIncludeWayNodes(item.isChecked());
            filter.clear();
            break;
        case R.id.preset_menu_invert:
            item.setChecked(!filter.isInverted());
            filter.setInverted(item.isChecked());
            filter.clear();
            break;
        case R.id.menu_help:
            HelpViewer.start(this, R.string.help_presetfilter);
            break;
        }
        return super.onOptionsItemSelected(item);
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
        if (!(App.getLogic().getFilter() instanceof PresetFilter)) {
            Log.e(DEBUG_TAG, FILTER_NULL_OR_NOT_A_PRESET_FILTER);
            return;
        }
        filter = (PresetFilter) App.getLogic().getFilter();
        currentGroup = group;
        currentGroup.getGroupView(this, presetView, this, null, filter.getPresetElement(), null);
        presetView.invalidate();
        invalidateOptionsMenu();
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
        Filter tempFilter = App.getLogic().getFilter();
        if (tempFilter instanceof PresetFilter) {
            filter = (PresetFilter) tempFilter;
            filter.setPresetElement(element.getPath(rootGroup));
            currentGroup.getGroupView(this, presetView, this, null, element, null);
            presetView.invalidate();
        }
        finish();
    }
}
