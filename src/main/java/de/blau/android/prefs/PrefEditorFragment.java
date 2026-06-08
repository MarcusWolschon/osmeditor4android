package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import ch.poole.openinghoursfragment.templates.TemplateMangementDialog;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.resources.DataStyleManager;
import de.blau.android.resources.TileLayerDatabaseView;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 * @author Simon
 */
public class PrefEditorFragment extends ExtendedPreferenceFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PrefEditorFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = PrefEditorFragment.class.getSimpleName().substring(0, TAG_LEN);

    private Preference stylePref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.preferences, rootKey);
        setPreferenceListeners(getResources());
        setTitle();
    }

    @Override
    public void onResume() {
        Log.d(DEBUG_TAG, "onResume");
        super.onResume();
        setTitle();
        if (stylePref != null) {
            final FragmentActivity activity = requireActivity();
            Preferences prefs = new Preferences(activity);
            final DataStyleManager styles = App.getDataStyleManager(activity);
            stylePref.setSummary(styles.translate(prefs.getDataStyle(styles)));
        }
        Log.d(DEBUG_TAG, "onResume done");
    }

    /**
     * Perform initialization of the advanced preference buttons (API/Presets)
     * 
     * @param r the current resources
     */
    private void setPreferenceListeners(@NonNull final Resources r) {
        stylePref = getPreferenceScreen().findPreference(r.getString(R.string.config_mapProfile_key));
        if (stylePref != null) {
            stylePref.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick");
                StyleConfigurationEditorActivity.start(requireActivity());
                return true;
            });
        }

        Preference customLayersPref = getPreferenceScreen().findPreference(r.getString(R.string.config_customlayers_key));
        if (customLayersPref != null) {
            customLayersPref.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick custom layers");
                TileLayerDatabaseView.showDialog(requireActivity());
                return true;
            });
        }

        Preference presetPref = getPreferenceScreen().findPreference(r.getString(R.string.config_presetbutton_key));
        if (presetPref != null) {
            presetPref.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick");
                PresetConfigurationEditorActivity.start(requireActivity());
                return true;
            });
        }

        // And add a preference item in your preferences XML that calls this

        Preference advPrefs = getPreferenceScreen().findPreference(r.getString(R.string.config_advancedprefs_key));
        if (advPrefs != null) {
            advPrefs.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick advanced");
                startActivity(new Intent(getActivity(), AdvancedPrefEditor.class));
                return true;
            });
        }

        Preference openingHoursPref = getPreferenceScreen().findPreference(r.getString(R.string.config_opening_hours_key));
        if (openingHoursPref != null) {
            openingHoursPref.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick opening hours");
                TemplateMangementDialog.showDialog(PrefEditorFragment.this, true, null, null, null, "",
                        App.getPreferences(requireContext()).lightThemeEnabled() ? R.style.Theme_AlertDialogLight : R.style.Theme_AlertDialog);
                return true;
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
