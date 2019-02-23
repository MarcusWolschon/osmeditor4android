package de.blau.android.prefs;

import android.preference.EditTextPreference;
import android.support.annotation.NonNull;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import de.blau.android.R;

public final class Util {

    private static final String DEBUG_TAG = "Util";

    /**
     * Set the summary of a list preference from its current value and add an OnPreferenceChangeListener
     * 
     * @param prefFragment PreferenceFragmentCompat this is being called from
     * @param keyResource resource id for the key of the ListPreference
     */
    public static void setListPreferenceSummary(@NonNull PreferenceFragmentCompat prefFragment, int keyResource) {
        ListPreference listPref = (ListPreference) prefFragment.getPreferenceScreen().findPreference(prefFragment.getString(keyResource));
        if (listPref != null) {
            CharSequence currentEntry = listPref.getEntry();
            if (currentEntry != null) {
                listPref.setSummary(currentEntry);
            }
            OnPreferenceChangeListener p = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    try {
                        int i = ((ListPreference) preference).findIndexOfValue((String) newValue);
                        CharSequence currentEntry = ((ListPreference) preference).getEntries()[i];
                        if (currentEntry != null) {
                            preference.setSummary(currentEntry);
                        }
                    } catch (Exception ex) {
                        Log.d(DEBUG_TAG, "onPreferenceChange " + ex);
                    }
                    return true;
                }
            };
            listPref.setOnPreferenceChangeListener(p);
        }
    }

    /**
     * Set the summary of a list preference from its current value and add an OnPreferenceChangeListener
     * 
     * @param prefFragment PreferenceFragmentCompat this is being called from
     * @param keyResource resource id for the key of the ListPreference
     */
    public static void setEditTextPreferenceSummary(@NonNull PreferenceFragmentCompat prefFragment, int keyResource) {
        Preference editTextPref = prefFragment.getPreferenceScreen().findPreference(prefFragment.getString(keyResource));
        if (editTextPref != null) {
            CharSequence currentValue = editTextPref.getSharedPreferences().getString(prefFragment.getString(keyResource), null);
            if (currentValue != null && !"".equals(currentValue)) {
                editTextPref.setSummary(currentValue);
            }
            OnPreferenceChangeListener p = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    try {
                        if (newValue != null) {
                            preference.setSummary((CharSequence) newValue);
                        }
                    } catch (Exception ex) {
                        Log.d(DEBUG_TAG, "onPreferenceChange " + ex);
                    }
                    return true;
                }
            };
            editTextPref.setOnPreferenceChangeListener(p);
        }
    }
}
