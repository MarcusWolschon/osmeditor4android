package de.blau.android.util;

import android.content.Intent;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import de.blau.android.Authorize;

/**
 * This holds all the relevant bits for dialogs that need to authorize against an OSM API instance
 * 
 * It is essentially the same as the activity version.
 * 
 */
public abstract class AuthorisationEnabledDialogFragment extends CancelableDialogFragment {

    private ActivityResultCallback<ActivityResult> callback = result -> {
        // nothing
    };

    private final ActivityResultLauncher<Intent> authorisationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            callback);

    /**
     * If we actually want to do something with the callback do it here
     * 
     * @param callback the callback to set
     */
    public void setCallback(ActivityResultCallback<ActivityResult> callback) {
        this.callback = callback;
    }

    public void startAuthorisation() {
        authorisationLauncher.launch(new Intent(getContext(), Authorize.class));
    }
}
