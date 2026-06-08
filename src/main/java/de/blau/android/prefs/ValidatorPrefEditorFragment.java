package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.validation.ValidatorRulesUI;

/**
 * Fragment for validator preferences.
 */
public class ValidatorPrefEditorFragment extends ExtendedPreferenceFragment {

	private static final int TAG_LEN = Math.min(LOG_TAG_LEN, ValidatorPrefEditorFragment.class.getSimpleName().length());
	private static final String DEBUG_TAG = ValidatorPrefEditorFragment.class.getSimpleName().substring(0, TAG_LEN);

	private boolean resetValidationFlag;

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
		setPreferencesFromResource(R.xml.validator_preferences, rootKey);

		Preference validatorPref = findPreference(getString(R.string.config_validatorprefs_key));
		if (validatorPref != null) {
			validatorPref.setOnPreferenceClickListener(preference -> {
				Log.d(DEBUG_TAG, "onPreferenceClick validator");
				ValidatorRulesUI ui = new ValidatorRulesUI();
				ui.manageRulesetContents(requireContext());
				return true;
			});
		}

		OnPreferenceChangeListener resetValidation = (preference, newValue) -> {
			Log.d(DEBUG_TAG, "onPreferenceChange reset validation");
			resetValidationFlag = true;
			return true;
		};

		Preference connectedPref = findPreference(getString(R.string.config_connectedNodeTolerance_key));
		if (connectedPref != null) {
			connectedPref.setOnPreferenceChangeListener(resetValidation);
		}

		Preference enabledValidationsPref = findPreference(getString(R.string.config_enabledValidations_key));
		if (enabledValidationsPref != null) {
			enabledValidationsPref.setOnPreferenceChangeListener(resetValidation);
		}

		setTitle();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (resetValidationFlag) {
			App.getDefaultValidator(requireContext()).reset(getContext());
			App.getDelegator().resetProblems();
		}
	}
}
