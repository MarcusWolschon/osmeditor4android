package io.vespucci.util;

import android.content.res.Configuration;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import io.vespucci.App;

public abstract class ConfigurationChangeAwareActivity extends AppCompatActivity {

    private static final String DEBUG_TAG = ConfigurationChangeAwareActivity.class.getSimpleName().substring(0, Math.min(23, ConfigurationChangeAwareActivity.class.getSimpleName().length()));

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
        final Configuration oldConfig = App.getConfiguration();
        App.setConfiguration(newConfig);
        if (Util.themeChanged(App.getPreferences(this), oldConfig, newConfig)) {
            Log.d(DEBUG_TAG, "recreating activity " + this.getClass().getCanonicalName());
            recreate();
        }
        Util.clearCaches(this, oldConfig, newConfig);
    }
}
