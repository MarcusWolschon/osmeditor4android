package de.blau.android.prefs;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerDatabaseView;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.validation.ValidatorRulesUI;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditorFragment extends ExtendedPreferenceFragment {

    private static String DEBUG_TAG = PrefEditorFragment.class.getSimpleName();

    private Resources r;
    private String    KEY_MAPBG;
    private String    KEY_MAPOL;
    private String    KEY_CUSTOM_LAYERS;
    private String    KEY_MAPPROFILE;
    private String    KEY_PREFPRESET;
    private String    KEY_ADVPREFS;
    private String    KEY_VALIDATOR;
    private String    KEY_CONNECTED;

    private BoundingBox viewBox = null;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        viewBox = (BoundingBox) getArguments().getSerializable(PrefEditor.CURRENT_VIEWBOX);
        setPreferencesFromResource(R.xml.preferences, rootKey);
        r = getResources();
        KEY_MAPBG = r.getString(R.string.config_backgroundLayer_key);
        KEY_MAPOL = r.getString(R.string.config_overlayLayer_key);
        KEY_CUSTOM_LAYERS = r.getString(R.string.config_customlayers_key);
        KEY_MAPPROFILE = r.getString(R.string.config_mapProfile_key);
        KEY_ADVPREFS = r.getString(R.string.config_advancedprefs_key);
        KEY_PREFPRESET = r.getString(R.string.config_presetbutton_key);
        KEY_VALIDATOR = r.getString(R.string.config_validatorprefs_key);
        KEY_CONNECTED = r.getString(R.string.config_connectedNodeTolerance_key);
        setPreferenceListeners();
        setTitle();
    }

    @Override
    public void onResume() {
        Log.d(DEBUG_TAG, "onResume");
        super.onResume();
        Util.setListPreferenceSummary(this, R.string.config_scale_key);
        setTitle();
        Log.d(DEBUG_TAG, "onResume done");
    }

    /** Perform initialization of the advanced preference buttons (API/Presets) */
    private void setPreferenceListeners() {
        Preferences prefs = new Preferences(getActivity());

        // remove any problematic imagery URLs
        TileLayerServer.applyBlacklist(prefs.getServer().getCachedCapabilities().getImageryBlacklist());

        ListPreference mapbgpref = (ListPreference) getPreferenceScreen().findPreference(KEY_MAPBG);
        if (mapbgpref != null) {
            String[] ids = TileLayerServer.getIds(viewBox, true);
            mapbgpref.setEntries(TileLayerServer.getNames(ids));
            mapbgpref.setEntryValues(ids);
            OnPreferenceChangeListener l = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(DEBUG_TAG, "onPreferenceChange background");
                    String id = (String) newValue;
                    String[] ids = TileLayerServer.getIds(null, false); // r.getStringArray(R.array.renderer_ids);
                    String[] names = TileLayerServer.getNames(ids); // r.getStringArray(R.array.renderer_names);
                    for (int i = 0; i < ids.length; i++) {
                        if (ids[i].equals(id)) {
                            preference.setSummary(names[i]);
                            break;
                        }
                    }
                    App.getDelegator().setImageryRecorded(false);
                    return true;
                }
            };
            mapbgpref.setOnPreferenceChangeListener(l);
            l.onPreferenceChange(mapbgpref, prefs.backgroundLayer());
        }

        ListPreference mapolpref = (ListPreference) getPreferenceScreen().findPreference(KEY_MAPOL);
        if (mapolpref != null) {
            String[] overlayIds = TileLayerServer.getOverlayIds(viewBox, true);
            mapolpref.setEntries(TileLayerServer.getOverlayNames(overlayIds));
            mapolpref.setEntryValues(overlayIds);
            OnPreferenceChangeListener ol = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(DEBUG_TAG, "onPreferenceChange overlay");
                    String id = (String) newValue;
                    String[] ids = TileLayerServer.getOverlayIds(null, false); // r.getStringArray(R.array.renderer_ids);
                    String[] names = TileLayerServer.getOverlayNames(ids); // r.getStringArray(R.array.renderer_names);
                    for (int i = 0; i < ids.length; i++) {
                        if (ids[i].equals(id)) {
                            preference.setSummary(names[i]);
                            break;
                        }
                    }
                    App.getDelegator().setImageryRecorded(false);
                    return true;
                }
            };
            mapolpref.setOnPreferenceChangeListener(ol);
            ol.onPreferenceChange(mapolpref, prefs.overlayLayer());
        }

        Preference customLayersPref = getPreferenceScreen().findPreference(KEY_CUSTOM_LAYERS);
        if (customLayersPref != null) {
            customLayersPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(DEBUG_TAG, "onPreferenceClick custom layers");
                    TileLayerDatabaseView ui = new TileLayerDatabaseView();
                    ui.manageLayers(getActivity());
                    return true;
                }
            });
        }

        ListPreference mapProfilePref = (ListPreference) getPreferenceScreen().findPreference(KEY_MAPPROFILE);
        if (mapProfilePref != null) {
            String[] profileList = DataStyle.getStyleList(getActivity());
            mapProfilePref.setEntries(profileList);
            mapProfilePref.setEntryValues(profileList);
            OnPreferenceChangeListener p = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(DEBUG_TAG, "onPreferenceChange mapProfile");
                    String id = (String) newValue;
                    String[] profileList = DataStyle.getStyleList(getActivity());
                    String[] ids = profileList;
                    String[] names = profileList;
                    for (int i = 0; i < ids.length; i++) {
                        if (ids[i].equals(id)) {
                            preference.setSummary(names[i]);
                            break;
                        }
                    }
                    return true;
                }
            };
            mapProfilePref.setOnPreferenceChangeListener(p);
            p.onPreferenceChange(mapProfilePref, prefs.getMapProfile());
        }

        Preference presetPref = getPreferenceScreen().findPreference(KEY_PREFPRESET);
        if (presetPref != null) {
            presetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(DEBUG_TAG, "onPreferenceClick");
                    PresetEditorActivity.start(getActivity());
                    return true;
                }
            });
        }

        Preference advPrefs = getPreferenceScreen().findPreference(KEY_ADVPREFS);
        if (advPrefs != null) {
            advPrefs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(DEBUG_TAG, "onPreferenceClick advanced");
                    startActivity(new Intent(getActivity(), AdvancedPrefEditor.class));
                    return true;
                }
            });
        }

        Preference validatorPref = getPreferenceScreen().findPreference(KEY_VALIDATOR);
        if (validatorPref != null) {
            validatorPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(DEBUG_TAG, "onPreferenceClick validator");
                    ValidatorRulesUI ui = new ValidatorRulesUI();
                    ui.manageRulesetContents(getContext());
                    return true;
                }
            });
        }
        
        Preference connectedPref = getPreferenceScreen().findPreference(KEY_CONNECTED);
        if (connectedPref != null) {
            OnPreferenceChangeListener p = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(DEBUG_TAG, "onPreferenceChange connected tolerance");
                    Context context = PrefEditorFragment.this.getContext();
                    App.getDefaultValidator(context).reset(context);
                    App.getDelegator().resetProblems();
                    return true;
                }
            };
            connectedPref.setOnPreferenceChangeListener(p);
        }
    }
}
