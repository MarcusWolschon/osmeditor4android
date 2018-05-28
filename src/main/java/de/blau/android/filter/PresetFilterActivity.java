package de.blau.android.filter;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, PresetFilterActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        Preset[] presets = App.getCurrentPresets(this);
        rootGroup = presets[0].getRootGroup(); // FIXME this assumes that we have at least one active preset
        if (presets.length > 1) {
            // a bit of a hack ... this adds the elements from other presets to the root group of the first one
            List<PresetElement> rootElements = rootGroup.getElements();
            for (Preset p : presets) {
                if (p != null) {
                    for (PresetElement e : p.getRootGroup().getElements()) {
                        if (!rootElements.contains(e)) { // only do this if not already present
                            rootGroup.addElement(e);
                        }
                    }
                }
            }
        }
        PresetElement element = filter.getPresetElement();
        PresetGroup parent = element != null ? element.getParent() : null;
        currentGroup = parent != null ? parent : rootGroup;

        presetView = getPresetView(currentGroup, element);
        setContentView(presetView);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayHomeAsUpEnabled(true);
    }

    private ScrollView getPresetView(PresetGroup group, PresetElement element) {
        View view = group.getGroupView(this, this, null, element);
        // view.setBackgroundColor(getActivity().getResources().getColor(R.color.abs__background_holo_dark));
        // view.setOnKeyListener(this);
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
        menu.findItem(R.id.preset_menu_invert).setChecked(((PresetFilter) filter).isInverted()).setVisible(false); // don't
                                                                                                                   // show
                                                                                                                   // for
                                                                                                                   // now
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
                currentGroup.getGroupView(this, presetView, this, null, null);
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
                    currentGroup.getGroupView(this, presetView, this, null, null);
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
        currentGroup = group;
        currentGroup.getGroupView(this, presetView, this, null, null);
        presetView.invalidate();
        supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onGroupLongClick(PresetGroup group) {
        onPresetElementSelected(group);
        return true;
    }

    void onPresetElementSelected(PresetElement element) {
        Log.d(DEBUG_TAG, element.toString());
        Filter filter = App.getLogic().getFilter();
        if (filter instanceof PresetFilter) {
            ((PresetFilter) filter).setPresetElement(element.getPath(rootGroup));
            Snack.toastTopInfo(this, element.getName());
            Log.d(DEBUG_TAG, "parent " + element.getParent());
        }
        finish();
    }
}
