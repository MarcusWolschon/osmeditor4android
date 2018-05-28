package de.blau.android.prefs;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import ch.poole.android.numberpickerpreference.NumberPickerPreference;
import ch.poole.android.numberpickerpreference.NumberPickerPreferenceFragment;

public abstract class ExtendedPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public abstract void onCreatePreferences(Bundle savedInstanceState, String rootKey);

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment fragment;
        if (preference instanceof LoginDataPreference) {
            fragment = LoginDataPreferenceFragment.newInstance(preference);
            fragment.setTargetFragment(this, 0);
            fragment.show(getChildFragmentManager(), "android.support.v7.preference.PreferenceFragment.LOGINDATA");
        } else if (preference instanceof MultiSelectListPreference) {
            fragment = MultiSelectListPreferenceDialogFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getChildFragmentManager(), "android.support.v7.preference.PreferenceFragment.MULTISELECTLIST");
        } else if (preference instanceof NumberPickerPreference) {
            fragment = NumberPickerPreferenceFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getChildFragmentManager(), "android.support.v7.preference.PreferenceFragment.NUMBERPICKER");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
