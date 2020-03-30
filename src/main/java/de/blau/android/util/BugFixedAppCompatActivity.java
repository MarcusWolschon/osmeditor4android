package de.blau.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;

public abstract class BugFixedAppCompatActivity extends AppCompatActivity {

    /**
     * See
     * http://stackoverflow.com/questions/32294607/call-requires-api-level-11current-min-is-9-android-app-activityoncreateview
     * This is a workaround google refusing to fix bugs (even trivial ones) in a timely manner
     *
     * @param parent the parent View
     * @param name name
     * @param context Android Context
     * @param attrs an AttributeSet
     * @return a View or null
     */
    @SuppressLint("NewApi")
    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }
}
