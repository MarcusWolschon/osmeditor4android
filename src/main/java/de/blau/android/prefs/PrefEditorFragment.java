package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import ch.poole.openinghoursfragment.templates.TemplateMangementDialog;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerDatabaseView;
import de.blau.android.validation.ValidatorRulesUI;

/**
 * Simple class for Android's standard-Preference Activity
 * 
 * @author mb
 * @author Simon
 */
public class PrefEditorFragment extends ExtendedPreferenceFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PrefEditorFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = PrefEditorFragment.class.getSimpleName().substring(0, TAG_LEN);

    private boolean resetValidationFlag;

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
        setListPreferenceSummary(R.string.config_scale_key, false);
        setTitle();
        Log.d(DEBUG_TAG, "onResume done");
    }

    /**
     * Perform initialization of the advanced preference buttons (API/Presets)
     * 
     * @param r the current resources
     */
    private void setPreferenceListeners(@NonNull final Resources r) {

        Preferences prefs = new Preferences(getActivity());

        ListPreference mapProfilePref = (ListPreference) getPreferenceScreen().findPreference(r.getString(R.string.config_mapProfile_key));
        if (mapProfilePref != null) {
            DataStyle styles = App.getDataStyle(getActivity());
            final String[] styleList = styles.getStyleList(getActivity());
            final String[] styleListTranslated = styles.getStyleListTranslated(getActivity(), styleList);
            mapProfilePref.setEntryValues(styleList);
            mapProfilePref.setEntries(styleListTranslated);
            OnPreferenceChangeListener p = (preference, newValue) -> {
                Log.d(DEBUG_TAG, "onPreferenceChange mapProfile");
                String id = (String) newValue;
                for (int i = 0; i < styleList.length; i++) {
                    if (id.equals(styleList[i])) {
                        preference.setSummary(styleListTranslated[i]);
                        break;
                    }
                }
                return true;
            };
            mapProfilePref.setOnPreferenceChangeListener(p);
            p.onPreferenceChange(mapProfilePref, prefs.getDataStyle(styles));
        }

        Preference customLayersPref = getPreferenceScreen().findPreference(r.getString(R.string.config_customlayers_key));
        if (customLayersPref != null) {
            customLayersPref.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick custom layers");
                TileLayerDatabaseView.showDialog(getActivity());
                return true;
            });
        }

        Preference presetPref = getPreferenceScreen().findPreference(r.getString(R.string.config_presetbutton_key));
        if (presetPref != null) {
            presetPref.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick");
                PresetEditorActivity.start(getActivity());
                return true;
            });
        }

        Preference advPrefs = getPreferenceScreen().findPreference(r.getString(R.string.config_advancedprefs_key));
        if (advPrefs != null) {
            advPrefs.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick advanced");
                startActivity(new Intent(getActivity(), AdvancedPrefEditor.class));
                return true;
            });
        }

        Preference validatorPref = getPreferenceScreen().findPreference(r.getString(R.string.config_validatorprefs_key));
        if (validatorPref != null) {
            validatorPref.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick validator");
                ValidatorRulesUI ui = new ValidatorRulesUI();
                ui.manageRulesetContents(getContext());
                return true;
            });
        }

        Preference connectedPref = getPreferenceScreen().findPreference(r.getString(R.string.config_connectedNodeTolerance_key));
        OnPreferenceChangeListener resetValidation = (preference, newValue) -> {
            Log.d(DEBUG_TAG, "onPreferenceChange reset validation");
            resetValidationFlag = true;
            return true;
        };
        if (connectedPref != null) {
            connectedPref.setOnPreferenceChangeListener(resetValidation);
        }

        Preference enabledValidationsPref = getPreferenceScreen().findPreference(r.getString(R.string.config_enabledValidations_key));
        if (enabledValidationsPref != null) {
            enabledValidationsPref.setOnPreferenceChangeListener(resetValidation);
        }

        Preference openingHoursPref = getPreferenceScreen().findPreference(r.getString(R.string.config_opening_hours_key));
        if (openingHoursPref != null) {
            openingHoursPref.setOnPreferenceClickListener(preference -> {
                Log.d(DEBUG_TAG, "onPreferenceClick opening hours");
                TemplateMangementDialog.showDialog(PrefEditorFragment.this, true, null, null, null, "");
                return true;
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (resetValidationFlag) { // we only want to this once, and when the preset have actually been changed
            App.getDefaultValidator(getContext()).reset(getContext());
            App.getDelegator().resetProblems();
        }
    }
}
