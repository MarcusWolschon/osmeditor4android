package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Intent;
import android.util.Log;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import de.blau.android.Authorize;

/**
 * This holds all the relevant bits for activities that need to authorize against an OSM API instance
 */
public abstract class AuthorisationEnabledActivity extends ConfigurationChangeAwareActivity {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, AuthorisationEnabledActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = AuthorisationEnabledActivity.class.getSimpleName().substring(0, TAG_LEN);

    private ActivityResultLauncher<Intent> authorisationLauncher;

    private ActivityResultCallback<ActivityResult> callback = result -> {
        // nothing
    };

    @Override
    protected void onStart() {
        Log.d(DEBUG_TAG, "onStart");
        super.onStart();
        authorisationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), callback);
    }

    /**
     * If we actually want to do something with the callback do it here
     * 
     * @param callback the callback to set
     */
    public void setCallback(ActivityResultCallback<ActivityResult> callback) {
        this.callback = callback;
    }

    public void startAuthorisation() {
        if (authorisationLauncher != null) {
            authorisationLauncher.launch(new Intent(this, Authorize.class));
        }
    }
}
