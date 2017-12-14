package de.blau.android.prefs;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.DebugInformation;
import de.blau.android.LicenseViewer;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.validation.ValidatorRulesUI;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 */
public class PrefEditorFragment extends PreferenceFragmentCompat {

    private static String DEBUG_TAG = PrefEditorFragment.class.getSimpleName();

    private Resources r;
    private String    KEY_MAPBG;
    private String    KEY_MAPOL;
    private String    KEY_MAPPROFILE;
    private String    KEY_ADVPREFS;
    private String    KEY_VALIDATOR;
    private String    KEY_LICENSE;
    private String    KEY_DEBUG;

    private BoundingBox viewBox = null;

    @Override
    public void onCreatePreferences(Bundle arg0, String arg1) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + arg1);
        viewBox = (BoundingBox) getArguments().getSerializable(PrefEditor.CURRENT_VIEWBOX);
        setPreferencesFromResource(R.xml.preferences, arg1);
        r = getResources();
        KEY_MAPBG = r.getString(R.string.config_backgroundLayer_key);
        KEY_MAPOL = r.getString(R.string.config_overlayLayer_key);
        KEY_MAPPROFILE = r.getString(R.string.config_mapProfile_key);
        KEY_ADVPREFS = r.getString(R.string.config_advancedprefs_key);
        KEY_VALIDATOR = r.getString(R.string.config_validatorprefs_key);
        KEY_LICENSE = r.getString(R.string.config_licensebutton_key);
        KEY_DEBUG = r.getString(R.string.config_debugbutton_key);
        fixUpPrefs();
    }

    @Override
    public void onResume() {
        Log.d(DEBUG_TAG, "onResume");
        super.onResume();
        Log.d(DEBUG_TAG, "onResume done");
    }

    /** Perform initialization of the advanced preference buttons (API/Presets) */
    private void fixUpPrefs() {
        Preferences prefs = new Preferences(getActivity());

        // remove any problematic imagery URLs
        TileLayerServer.applyBlacklist(prefs.getServer().getCachedCapabilities().getImageryBlacklist());

        ListPreference mapbgpref = (ListPreference) getPreferenceScreen().findPreference(KEY_MAPBG);
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

        ListPreference mapolpref = (ListPreference) getPreferenceScreen().findPreference(KEY_MAPOL);
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

        ListPreference mapProfilePref = (ListPreference) getPreferenceScreen().findPreference(KEY_MAPPROFILE);
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

        Preference advprefs = getPreferenceScreen().findPreference(KEY_ADVPREFS);
        advprefs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(DEBUG_TAG, "onPreferenceClick advanced");
                startActivity(new Intent(getActivity(), AdvancedPrefEditor.class));
                return true;
            }
        });

        Preference validatorpref = getPreferenceScreen().findPreference(KEY_VALIDATOR);
        validatorpref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(DEBUG_TAG, "onPreferenceClick validator");
                ValidatorRulesUI ui = new ValidatorRulesUI();
                ui.manageRulesetContents(getContext());
                return true;
            }
        });

        Preference licensepref = getPreferenceScreen().findPreference(KEY_LICENSE);
        licensepref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(DEBUG_TAG, "onPreferenceClick licence");
                startActivity(new Intent(getActivity(), LicenseViewer.class));
                return true;
            }
        });

        Preference debugpref = getPreferenceScreen().findPreference(KEY_DEBUG);
        debugpref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(DEBUG_TAG, "onPreferenceClick debug");
                startActivity(new Intent(getActivity(), DebugInformation.class));
                return true;
            }
        });
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment fragment;
        if (preference instanceof MultiSelectListPreference) {
            fragment = MultiSelectListPreferenceDialogFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.MULTISELECTLIST");
        } else
            super.onDisplayPreferenceDialog(preference);
    }
}
