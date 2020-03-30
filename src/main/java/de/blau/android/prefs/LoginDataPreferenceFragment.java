package de.blau.android.prefs;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.EditText;
import de.blau.android.R;

/**
 * A dialog preference that allows the user to set username and password in one dialog. Changes get saved to the
 * {@link AdvancedPrefDatabase}
 */
public class LoginDataPreferenceFragment extends PreferenceDialogFragmentCompat {

    private EditText userEdit;
    private EditText passwordEdit;

    /**
     * Get a new instance of the Fragment
     * 
     * @param preference the preference we are modifying
     * @return a LoginDataPreferenceFragment
     */
    public static LoginDataPreferenceFragment newInstance(@NonNull Preference preference) {
        LoginDataPreferenceFragment fragment = new LoginDataPreferenceFragment();
        Bundle bundle = new Bundle(1);
        bundle.putString("key", preference.getKey());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        userEdit = (EditText) view.findViewById(R.id.loginedit_editUsername);
        passwordEdit = (EditText) view.findViewById(R.id.loginedit_editPassword);
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(getActivity())) {
            API api = db.getCurrentAPI();
            userEdit.setText(api.user);
            passwordEdit.setText(api.pass);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(getActivity())) {
                db.setCurrentAPILogin(userEdit.getText().toString(), passwordEdit.getText().toString());
            }
        }
    }
}
