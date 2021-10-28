package de.blau.android.prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.zeugmasolutions.localehelper.LocaleAwareCompatActivity;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import de.blau.android.R;
import de.blau.android.util.LocaleUtils;
import de.blau.android.util.Util;

public class AdvancedPrefEditorFragment extends ExtendedPreferenceFragment {

    private static final String DEBUG_TAG = "AdvancedPrefEditor";

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

        setListPreferenceSummary(R.string.config_selectCameraApp_key, false);
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
        setTitle();
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

        Preference disableTranslationsPref = getPreferenceScreen().findPreference(r.getString(R.string.config_disableTranslations_key));
        if (disableTranslationsPref != null) {
            disableTranslationsPref.setOnPreferenceClickListener(preference -> {
                LocaleAwareCompatActivity lac = ((LocaleAwareCompatActivity) getActivity());
                String savedLocaleKey = lac.getString(R.string.config_savedLocale_key);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(lac);
                if (((CheckBoxPreference) disableTranslationsPref).isChecked()) {
                    if (!prefs.contains(savedLocaleKey)) {
                        prefs.edit().putString(savedLocaleKey, LocaleUtils.toLanguageTag(Locale.getDefault())).commit();
                    }
                    lac.updateLocale(Locale.ENGLISH);
                } else {
                    lac.updateLocale(LocaleUtils.forLanguageTag(prefs.getString(savedLocaleKey, Locale.ENGLISH.toString())));
                }
                return true;
            });
        }
    }
}
