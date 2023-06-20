package de.blau.android.util;

import com.zeugmasolutions.localehelper.LocaleAwareCompatActivity;

import android.content.res.Configuration;
import android.util.Log;
import de.blau.android.App;

public abstract class ConfigurationChangeAwareActivity extends LocaleAwareCompatActivity {
    
    private static final String DEBUG_TAG = ConfigurationChangeAwareActivity.class.getSimpleName();

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
        App.setConfiguration(newConfig);
        final Configuration oldConfig = getResources().getConfiguration();
        if (Util.themeChanged(App.getPreferences(this), oldConfig, newConfig)) {
            Log.d(DEBUG_TAG, "recreating activity " + this.getClass().getCanonicalName());
            recreate();
        }
        Util.clearCaches(this, oldConfig, newConfig);    
    }
}
