package de.blau.android.prefs;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.blau.android.R;
import de.blau.android.util.Util;

/**
 * Fragment for camera and images preferences.
 */
public class CameraAndImagesPrefEditorFragment extends ExtendedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(DEBUG_TAG, "onCreatePreferences " + rootKey);
        setPreferencesFromResource(R.xml.camera_and_images_preferences, rootKey);

        ListPreference cameraAppPref = findPreference(getString(R.string.config_selectCameraApp_key));
        if (cameraAppPref != null) {
            setupCameraPref(cameraAppPref);
        }

        setListPreferenceSummary(R.string.config_selectCameraApp_key, false);
        setListPreferenceSummary(R.string.config_imageLicence_key, false);
        setRestartRequiredMessage(R.string.config_indexMediaStore_key);

        Preference imageStorePref = findPreference(getString(R.string.config_imageStores_key));
        if (imageStorePref != null) {
            imageStorePref.setOnPreferenceClickListener(preference -> {
                ImageStorageEditorActivity.start(requireActivity());
                return true;
            });
        }
        
        setTitle();
    }

    private void setupCameraPref(@NonNull ListPreference cameraAppPref) {
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
}
