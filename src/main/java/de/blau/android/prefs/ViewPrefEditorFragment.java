package de.blau.android.prefs;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import java.util.Locale;

import de.blau.android.R;
import de.blau.android.prefs.keyboard.ShortcutsActivity;
import de.blau.android.util.LocaleUtils;
import de.blau.android.util.Util;

/**
 * Fragment for view preferences.
 */
public class ViewPrefEditorFragment extends ExtendedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.view_preferences, rootKey);

        ListPreference appLocalePref = findPreference(getString(R.string.config_appLocale_key));
        if (appLocalePref != null) {
            setupAppLocalePref(appLocalePref);
        }

        setListPreferenceSummary(R.string.config_theme_key, true);
        setListPreferenceSummary(R.string.config_mapOrientation_key, false);
        setListPreferenceSummary(R.string.config_followGPSbutton_key, true);
        setListPreferenceSummary(R.string.config_nameCap_key, false);

        setRestartRequiredMessage(R.string.config_enableLightTheme_key);
        setRestartRequiredMessage(R.string.config_splitActionBarEnabled_key);
        setRestartRequiredMessage(R.string.config_supportPresetLabels_key);

        Preference keyboadShortcutsPref = findPreference(getString(R.string.config_keyboard_shortcuts_key));
        if (keyboadShortcutsPref != null) {
            keyboadShortcutsPref.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick keyboard shortcut");
                Intent intent = new Intent(getActivity(), ShortcutsActivity.class);
                startActivity(intent);
                return true;
            });
        }
        
        setTitle();
    }

    private void setupAppLocalePref(@NonNull ListPreference appLocalePref) {
        appLocalePref.setPersistent(false); // stored by the device
        Locale currentLocale = Locale.getDefault();
        LocaleListCompat currentAppLocales = AppCompatDelegate.getApplicationLocales();
        boolean hasAppLocale = !currentAppLocales.isEmpty();
        if (hasAppLocale) {
            currentLocale = currentAppLocales.get(0);
        }
        LocaleListCompat appLocales = LocaleUtils.getSupportedLocales(requireContext());
        final String[] entries = new String[appLocales.size() + 1];
        String[] values = new String[appLocales.size() + 1];
        entries[0] = requireContext().getString(R.string.config_appLocale_device_language);
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
}
