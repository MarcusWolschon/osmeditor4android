package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.res.Configuration;
import android.util.Log;
import android.view.ViewGroup.MarginLayoutParams;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.WindowInsetsCompat;
import de.blau.android.App;

public abstract class ConfigurationChangeAwareActivity extends AppCompatActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ConfigurationChangeAwareActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = ConfigurationChangeAwareActivity.class.getSimpleName().substring(0, TAG_LEN);

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

    /**
     * Standard insets listener
     */
    public static final OnApplyWindowInsetsListener onApplyWindowInsetslistener = (v, windowInsets) -> {
        Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
        MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
        mlp.leftMargin = insets.left;
        mlp.bottomMargin = insets.bottom;
        mlp.rightMargin = insets.right;
        mlp.topMargin = insets.top;
        v.setLayoutParams(mlp);
        return WindowInsetsCompat.CONSUMED;
    };
}
