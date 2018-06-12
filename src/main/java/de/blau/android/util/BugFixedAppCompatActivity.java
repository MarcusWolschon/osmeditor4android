package de.blau.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;

public abstract class BugFixedAppCompatActivity extends AppCompatActivity {

    /**
     * See
     * http://stackoverflow.com/questions/32294607/call-requires-api-level-11current-min-is-9-android-app-activityoncreateview
     * This is a workaround google refusing to fix bugs (even trivial ones) in a timely manner
     */
    @SuppressLint("NewApi")
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (Build.VERSION.SDK_INT >= 11) {
            return super.onCreateView(parent, name, context, attrs);
        }
        return null;
    }
}
