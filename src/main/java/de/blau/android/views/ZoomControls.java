package de.blau.android.views;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class ZoomControls extends LinearLayout {

    private static final String DEBUG_TAG = ZoomControls.class.getSimpleName();

    private final FloatingActionButton zoomIn;
    private final FloatingActionButton zoomOut;

    private final Context context;

    private float elevationIn  = -1f;
    private float elevationOut = -1f;

    /**
     * Construct a new zoom control view
     * 
     * @param context Android Context
     */
    public ZoomControls(Context context) {
        this(context, null);
    }

    /**
     * Construct a new zoom control view
     * 
     * @param context Android COntext
     * @param attrs an AttributeSet
     */
    public ZoomControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setFocusable(false);
        LayoutInflater inflater = (LayoutInflater) (new ContextThemeWrapper(context, R.style.Theme_AppCompat_Light)
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        inflater.inflate(R.layout.zoom_controls, this, true);
        zoomIn = (FloatingActionButton) findViewById(R.id.zoom_in);
        zoomOut = (FloatingActionButton) findViewById(R.id.zoom_out);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // currently can't be set in layout, ColorStateList not supported in Lollipop and higher
            ColorStateList zoomTint = ContextCompat.getColorStateList(context, R.color.zoom);
            Util.setBackgroundTintList(zoomIn, zoomTint);
            Util.setBackgroundTintList(zoomOut, zoomTint);
        }
        zoomIn.setAlpha(Main.FABALPHA);
        zoomOut.setAlpha(Main.FABALPHA);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    /**
     * Set the OnClickListener for the zoom in button
     * 
     * @param listener the listener to set or null
     */
    public void setOnZoomInClickListener(@Nullable View.OnClickListener listener) {
        zoomIn.setOnClickListener(listener);
    }

    /**
     * Set the OnClickListener for the zoom in button
     * 
     * @param listener the listener to set or null
     */
    public void setOnZoomOutClickListener(@Nullable View.OnClickListener listener) {
        zoomOut.setOnClickListener(listener);
    }

    /**
     * Show the zoom controls
     */
    public void show() {
        this.setVisibility(View.VISIBLE);
    }

    /**
     * Hide the zoom controls
     */
    public void hide() {
        this.setVisibility(View.GONE);
    }

    /**
     * Enable/disable the zoom in button
     * 
     * @param isEnabled enabled status to set
     */
    public void setIsZoomInEnabled(boolean isEnabled) {
        setEnabled(zoomIn, isEnabled);
    }

    /**
     * Enable/disable the zoom out button
     * 
     * @param isEnabled enabled status to set
     */
    public void setIsZoomOutEnabled(boolean isEnabled) {
        setEnabled(zoomOut, isEnabled);
    }

    /**
     * Enable/disable a zoom button
     * 
     * @param fab the FloatingActionButton
     * @param isEnabled enabled status to set
     */
    private void setEnabled(@NonNull FloatingActionButton fab, boolean isEnabled) {
        fab.setEnabled(isEnabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fab.setBackgroundColor(
                    ThemeUtils.getStyleAttribColorValue(context, isEnabled ? R.attr.colorControlNormal : R.attr.colorPrimary, R.color.dark_grey));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (elevationIn < 0f) {
            elevationIn = zoomIn.getElevation();
            elevationOut = zoomOut.getElevation();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        // this is a workaround for https://github.com/MarcusWolschon/osmeditor4android/issues/965
        Log.d(DEBUG_TAG, "onConfigurationChanged " + elevationIn + " " + elevationOut);
        zoomIn.setElevation(elevationIn);
        zoomOut.setElevation(elevationOut);
    }
}