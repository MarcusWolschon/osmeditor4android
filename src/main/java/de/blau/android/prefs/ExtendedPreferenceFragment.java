package de.blau.android.prefs;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.MultiSelectListPreferenceDialogFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;
import ch.poole.android.numberpickerpreference.NumberPickerPreference;
import ch.poole.android.numberpickerpreference.NumberPickerPreferenceFragment;
import de.blau.android.R;
import de.blau.android.util.Snack;

public abstract class ExtendedPreferenceFragment extends PreferenceFragmentCompat {

    protected static final String DEBUG_TAG = "ExtendedPref...Frag..";

    @Override
    public abstract void onCreatePreferences(Bundle savedInstanceState, String rootKey);

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment fragment;
        if (preference instanceof LoginDataPreference) {
            fragment = LoginDataPreferenceFragment.newInstance(preference);
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.LOGINDATA");
        } else if (preference instanceof MultiSelectListPreference) {
            fragment = MultiSelectListPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.MULTISELECTLIST");
        } else if (preference instanceof NumberPickerPreference) {
            fragment = NumberPickerPreferenceFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.NUMBERPICKER");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    /**
     * Set the action bar title of the activity calling us to the PreferenceScreen title
     */
    protected void setTitle() {
        AppCompatActivity activity = ((AppCompatActivity) getActivity());
        if (activity != null) {
            activity.getSupportActionBar().setTitle(getPreferenceScreen().getTitle());
        }
    }

    /**
     * Set the summary of a list preference from its current value and add an OnPreferenceChangeListener
     * 
     * @param keyResource resource id for the key of the ListPreference
     * @param restart if true point the user to the fact that he needs to restart
     */
    public void setListPreferenceSummary(int keyResource, boolean restart) {
        ListPreference listPref = (ListPreference) getPreferenceScreen().findPreference(getString(keyResource));
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
                    if (restart) {
                        Snack.toastTopInfo(getContext(), R.string.toast_restart_required);
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
     * @param keyResource resource id for the key of the ListPreference
     * @param restart if true point the user to the fact that he needs to restart
     */
    public void setEditTextPreferenceSummary(int keyResource, boolean restart) {
        Preference editTextPref = getPreferenceScreen().findPreference(getString(keyResource));
        if (editTextPref != null) {
            CharSequence currentValue = editTextPref.getSharedPreferences().getString(getString(keyResource), null);
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
                    if (restart) {
                        Snack.toastTopInfo(getContext(), R.string.toast_restart_required);
                    }
                    return true;
                }
            };
            editTextPref.setOnPreferenceChangeListener(p);
        }
    }

    /**
     * Set a OnPreferenceChangeListener that will show a toast asking the user to restart
     * 
     * @param keyResource resource id for the key of the Preference
     */
    public void setRestartRequiredMessage(int keyResource) {
        Preference pref = getPreferenceScreen().findPreference(getString(keyResource));
        if (pref != null) {
            OnPreferenceChangeListener p = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Snack.toastTopInfo(getContext(), R.string.toast_restart_required);
                    return true;
                }
            };
            pref.setOnPreferenceChangeListener(p);
        }
    }
}
