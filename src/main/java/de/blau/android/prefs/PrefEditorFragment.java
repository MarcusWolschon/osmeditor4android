package de.blau.android.prefs;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
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
 */
public class PrefEditorFragment extends ExtendedPreferenceFragment {

    private static final String DEBUG_TAG = PrefEditorFragment.class.getSimpleName();

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
    private void setPreferenceListeners(final Resources r) {

        Preferences prefs = new Preferences(getActivity());

        ListPreference mapProfilePref = (ListPreference) getPreferenceScreen().findPreference(r.getString(R.string.config_mapProfile_key));
        if (mapProfilePref != null) {
            String[] profileList = DataStyle.getStyleList(getActivity());
            mapProfilePref.setEntries(profileList);
            mapProfilePref.setEntryValues(profileList);
            OnPreferenceChangeListener p = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(DEBUG_TAG, "onPreferenceChange mapProfile");
                    String id = (String) newValue;
                    String[] profileList = DataStyle.getStyleList(getActivity());
                    String[] ids = profileList;
                    String[] names = profileList;
                    for (int i = 0; i < ids.length; i++) {
                        if (ids[i].equals(id)) {
                            preference.setSummary(names[i]);
                            break;
                        }
                    }
                    return true;
                }
            };
            mapProfilePref.setOnPreferenceChangeListener(p);
            p.onPreferenceChange(mapProfilePref, prefs.getMapProfile());
        }

        Preference customLayersPref = getPreferenceScreen().findPreference(r.getString(R.string.config_customlayers_key));
        if (customLayersPref != null) {
            customLayersPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(DEBUG_TAG, "onPreferenceClick custom layers");
                    TileLayerDatabaseView ui = new TileLayerDatabaseView();
                    ui.manageLayers(getActivity());
                    return true;
                }
            });
        }

        Preference presetPref = getPreferenceScreen().findPreference(r.getString(R.string.config_presetbutton_key));
        if (presetPref != null) {
            presetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(DEBUG_TAG, "onPreferenceClick");
                    PresetEditorActivity.start(getActivity());
                    return true;
                }
            });
        }

        Preference advPrefs = getPreferenceScreen().findPreference(r.getString(R.string.config_advancedprefs_key));
        if (advPrefs != null) {
            advPrefs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(DEBUG_TAG, "onPreferenceClick advanced");
                    startActivity(new Intent(getActivity(), AdvancedPrefEditor.class));
                    return true;
                }
            });
        }

        Preference validatorPref = getPreferenceScreen().findPreference(r.getString(R.string.config_validatorprefs_key));
        if (validatorPref != null) {
            validatorPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(DEBUG_TAG, "onPreferenceClick validator");
                    ValidatorRulesUI ui = new ValidatorRulesUI();
                    ui.manageRulesetContents(getContext());
                    return true;
                }
            });
        }

        Preference connectedPref = getPreferenceScreen().findPreference(r.getString(R.string.config_connectedNodeTolerance_key));
        if (connectedPref != null) {
            OnPreferenceChangeListener p = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(DEBUG_TAG, "onPreferenceChange connected tolerance");
                    Context context = PrefEditorFragment.this.getContext();
                    App.getDefaultValidator(context).reset(context);
                    App.getDelegator().resetProblems();
                    return true;
                }
            };
            connectedPref.setOnPreferenceChangeListener(p);
        }

        Preference openingHoursPref = getPreferenceScreen().findPreference(r.getString(R.string.config_opening_hours_key));
        if (openingHoursPref != null) {
            openingHoursPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(DEBUG_TAG, "onPreferenceClick opening hours");
                    TemplateMangementDialog.showDialog(PrefEditorFragment.this, true, null, null, null, "");
                    return true;
                }
            });
        }
    }
}
