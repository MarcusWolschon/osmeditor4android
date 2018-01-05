package de.blau.android.prefs;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import ch.poole.android.numberpickerpreference.NumberPickerPreference;
import ch.poole.android.numberpickerpreference.NumberPickerPreferenceFragment;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.DataLossActivity;
import de.blau.android.util.Util;

public class AdvancedPrefEditorFragment extends PreferenceFragmentCompat {

    private static final String DEBUG_TAG = "AdvancedPrefEditor";

    private Resources r;
    private String    KEY_PREFAPI;
    private String    KEY_PREFPRESET;
    private String    KEY_PREFFULLSCREEN;
    private String    KEY_PREFLOGIN;
    private String    KEY_PREFGEOCODER;
    private String    KEY_PREFGPSSOURCE;

    @Override
    public void onCreatePreferences(Bundle arg0, String arg1) {
        addPreferencesFromResource(R.xml.advancedpreferences);
        r = getResources();
        KEY_PREFAPI = r.getString(R.string.config_api_button_key);
        KEY_PREFPRESET = r.getString(R.string.config_presetbutton_key);
        KEY_PREFFULLSCREEN = r.getString(R.string.config_fullscreenMode_key);
        KEY_PREFLOGIN = r.getString(R.string.config_loginbutton_key);
        KEY_PREFGEOCODER = r.getString(R.string.config_geocoder_button_key);
        KEY_PREFGPSSOURCE = r.getString(R.string.config_gps_source_key);
        fixUpPrefs();
    }

    @Override
    public void onResume() {
        Log.d(DEBUG_TAG, "onResume");
        super.onResume();
        Preference apipref = getPreferenceScreen().findPreference(KEY_PREFAPI);
        AdvancedPrefDatabase db = new AdvancedPrefDatabase(getActivity());
        API current = db.getCurrentAPI();
        if (current.id.equals(AdvancedPrefDatabase.ID_DEFAULT)) {
            apipref.setSummary(R.string.config_apibutton_summary);
        } else {
            apipref.setSummary(current.name.equals("") ? current.url : current.name);
        }
        Preference loginpref = getPreferenceScreen().findPreference(KEY_PREFLOGIN);
        loginpref.setSummary(current.user != null && !"".equals(current.user) ? current.user : r.getString(R.string.config_username_summary));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Util.setListPreferenceSummary(this, KEY_PREFFULLSCREEN);
        }
        Util.setListPreferenceSummary(this, KEY_PREFGPSSOURCE);
    }

    /** Perform initialization of the advanced preference buttons (API/Presets) */
    private void fixUpPrefs() {

        Preference presetPref = getPreferenceScreen().findPreference(KEY_PREFPRESET);
        presetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(DEBUG_TAG, "onPreferenceClick");
                PresetEditorActivity.start(getActivity());
                return true;
            }
        });

        Preference apiPref = getPreferenceScreen().findPreference(KEY_PREFAPI);
        apiPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(DEBUG_TAG, "onPreferenceClick 2");
                Intent intent = new Intent(getActivity(), APIEditorActivity.class);
                if (Main.hasChanges()) {
                    DataLossActivity.showDialog(getActivity(), intent, -1);
                } else {
                    startActivity(intent);
                }
                return true;
            }
        });

        Preference geocoderPref = getPreferenceScreen().findPreference(KEY_PREFGEOCODER);
        geocoderPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(DEBUG_TAG, "onPreferenceClick");
                GeocoderEditorActivity.start(getActivity());
                return true;
            }
        });
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment fragment;
        if (preference instanceof LoginDataPreference) {
            fragment = LoginDataPreferenceFragment.newInstance(preference);
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.LOGINDATA");
        } else if (preference instanceof MultiSelectListPreference) {
            fragment = MultiSelectListPreferenceDialogFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.MULTISELECTLIST");
        } else if (preference instanceof NumberPickerPreference) {
            fragment = NumberPickerPreferenceFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.NUMBERPICKER");
        } else
            super.onDisplayPreferenceDialog(preference);
    }
}
