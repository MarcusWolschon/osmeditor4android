package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import de.blau.android.R;
import de.blau.android.util.LocaleUtils;
import de.blau.android.util.Util;

public class AdvancedPrefEditorFragment extends ExtendedPreferenceFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, AdvancedPrefEditorFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = AdvancedPrefEditorFragment.class.getSimpleName().substring(0, TAG_LEN);

    private Resources    r;
    AdvancedPrefDatabase db;
    private String       apiPrefKey;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.advancedpreferences, rootKey);
        r = getResources();
        apiPrefKey = r.getString(R.string.config_api_button_key);
        setOnPreferenceClickListeners();
        setTitle();
        db = new AdvancedPrefDatabase(getActivity());
    }

    @Override
    public void onResume() {
        Log.d(DEBUG_TAG, "onResume");
        super.onResume();
        Preference apiPref = getPreferenceScreen().findPreference(apiPrefKey);
        if (apiPref != null) {
            API current = db.getCurrentAPI();
            if (current.id.equals(AdvancedPrefDatabase.ID_DEFAULT)) {
                apiPref.setSummary(R.string.config_apibutton_summary);
            } else {
                apiPref.setSummary("".equals(current.name) ? current.url : current.name);
            }
            Preference loginpref = getPreferenceScreen().findPreference(r.getString(R.string.config_loginbutton_key));
            if (loginpref != null) {
                loginpref.setSummary(current.user != null && !"".equals(current.user) ? current.user : r.getString(R.string.config_username_summary));
            }
        }

        ListPreference cameraAppPref = getPreferenceScreen().findPreference(r.getString(R.string.config_selectCameraApp_key));
        if (cameraAppPref != null) {
            setupCameraPref(cameraAppPref);
        }

        ListPreference appLocalePref = (ListPreference) getPreferenceScreen().findPreference(r.getString(R.string.config_appLocale_key));
        if (appLocalePref != null) {
            setupAppLocalePref(appLocalePref);
        }

        setListPreferenceSummary(R.string.config_selectCameraApp_key, false);
        setListPreferenceSummary(R.string.config_theme_key, true);
        setListPreferenceSummary(R.string.config_fullscreenMode_key, true);
        setListPreferenceSummary(R.string.config_mapOrientation_key, false);
        setListPreferenceSummary(R.string.config_gps_source_key, false);
        setEditTextPreferenceSummary(R.string.config_offsetServer_key, false);
        setEditTextPreferenceSummary(R.string.config_osmoseServer_key, false);
        setEditTextPreferenceSummary(R.string.config_taginfoServer_key, false);
        setRestartRequiredMessage(R.string.config_enableLightTheme_key);
        setRestartRequiredMessage(R.string.config_splitActionBarEnabled_key);
        setListPreferenceSummary(R.string.config_followGPSbutton_key, true);
        setRestartRequiredMessage(R.string.config_preferRemovableStorage_key);
        setRestartRequiredMessage(R.string.config_mapillary_min_zoom_key);
        setListPreferenceSummary(R.string.config_nameCap_key, false);
        setRestartRequiredMessage(R.string.config_autosaveSaveState_key);
        setRestartRequiredMessage(R.string.config_autosaveSaveChanges_key);
        setRestartRequiredMessage(R.string.config_autosaveInterval_key);
        setRestartRequiredMessage(R.string.config_autosaveChanges_key);
        setRestartRequiredMessage(R.string.config_autosaveMaxFiles_key);
        setRestartRequiredMessage(R.string.config_indexMediaStore_key);
        setRestartRequiredMessage(R.string.config_supportPresetLabels_key);
        setRestartRequiredMessage(R.string.config_enableHwAcceleration_key);
        setTitle();
    }

    /**
     * Setup the app locale preference
     * 
     * @param appLocalePref the preference
     * 
     */
    private void setupAppLocalePref(@NonNull ListPreference appLocalePref) {
        appLocalePref.setPersistent(false); // stored by the device
        Locale currentLocale = Locale.getDefault();
        LocaleListCompat currentAppLocales = AppCompatDelegate.getApplicationLocales();
        boolean hasAppLocale = !currentAppLocales.isEmpty();
        if (hasAppLocale) {
            currentLocale = currentAppLocales.get(0);
        }
        LocaleListCompat appLocales = LocaleUtils.getSupportedLocales(getContext());
        final String[] entries = new String[appLocales.size() + 1];
        String[] values = new String[appLocales.size() + 1];
        entries[0] = getContext().getString(R.string.config_appLocale_device_language);
        values[0] = "";
        for (int i = 0; i < appLocales.size(); i++) {
            Locale l = appLocales.get(i);
            entries[i + 1] = l.getDisplayName(currentLocale);
            values[i + 1] = l.toString();
        }
        appLocalePref.setEntryValues(values);
        appLocalePref.setEntries(entries);
        final String currentLocaleString = currentLocale.toString();
        appLocalePref.setValue(hasAppLocale ? currentLocaleString : "");
        appLocalePref.setSummary(hasAppLocale ? currentLocale.getDisplayName() : entries[0]);
        OnPreferenceChangeListener p = (preference, newValue) -> {
            // Note google is very confused about Android with _ and proper, with - locale values, we try to circumvent
            // that here
            if (Util.notEmpty((String) newValue)) {
                Locale newLocale = LocaleUtils.localeFromAndroidLocaleTag((String) newValue);
                LocaleListCompat newAppList = LocaleListCompat.create(newLocale);
                AppCompatDelegate.setApplicationLocales(newAppList);
                preference.setSummary(newLocale.getDisplayName());
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
                preference.setSummary(entries[0]);
            }
            return true;
        };
        appLocalePref.setOnPreferenceChangeListener(p);
    }

    /**
     * Setup the possible camera apps for selection
     * 
     * @param cameraAppPref the preference
     */
    public void setupCameraPref(@NonNull ListPreference cameraAppPref) {
        // remove not installed apps
        List<CharSequence> entries = new ArrayList<>();
        Collections.addAll(entries, cameraAppPref.getEntryValues());
        List<CharSequence> values = new ArrayList<>();
        Collections.addAll(values, cameraAppPref.getEntries());
        PackageManager pm = getContext().getPackageManager();
        CharSequence[] temp = cameraAppPref.getEntryValues();
        int removed = 0;
        for (int i = 0; i < temp.length; i++) {
            String p = temp[i].toString();
            if (!"".equals(p) && !Util.isPackageInstalled(p, pm)) {
                entries.remove(i - removed); // NOSONAR
                values.remove(i - removed); // NOSONAR
                removed++;
            }
        }
        cameraAppPref.setEntryValues(entries.toArray(new CharSequence[entries.size()]));
        cameraAppPref.setEntries(values.toArray(new CharSequence[values.size()]));
    }

    /**
     * Set listeners on special Preference entries
     * 
     * If we are just showing a sub-PreferenceScreen some of the keys may not be accessible
     */
    private void setOnPreferenceClickListeners() {
        Preference apiPref = getPreferenceScreen().findPreference(apiPrefKey);
        if (apiPref != null) {
            apiPref.setOnPreferenceClickListener(preference -> {
                APIEditorActivity.start(getActivity());
                return true;
            });
        }

        Preference geocoderPref = getPreferenceScreen().findPreference(r.getString(R.string.config_geocoder_button_key));
        if (geocoderPref != null) {
            geocoderPref.setOnPreferenceClickListener(preference -> {
                GeocoderEditorActivity.start(getActivity());
                return true;
            });
        }
    }
}
