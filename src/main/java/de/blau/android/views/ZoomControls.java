package de.blau.android.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class ZoomControls extends LinearLayout {

    private static final String DEBUG_TAG = ZoomControls.class.getName();

    private final FloatingActionButton zoomIn;
    private final FloatingActionButton zoomOut;

    private final Context context;

    public ZoomControls(Context context) {
        this(context, null);
    }

    public ZoomControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setFocusable(false);
        LayoutInflater inflater = (LayoutInflater) (new ContextThemeWrapper(context, R.style.Theme_AppCompat_Light)
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        inflater.inflate(R.layout.zoom_controls, this, true);
        zoomIn = (FloatingActionButton) findViewById(R.id.zoom_in);
        zoomOut = (FloatingActionButton) findViewById(R.id.zoom_out);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // currently can't be set in layout, ColorStateList not supported in Lollipop and higher
            ColorStateList zoomTint = ContextCompat.getColorStateList(context, R.color.zoom);
            Util.setBackgroundTintList(zoomIn, zoomTint);
            Util.setBackgroundTintList(zoomOut, zoomTint);
        }
        Util.setAlpha(zoomIn, Main.FABALPHA);
        Util.setAlpha(zoomOut, Main.FABALPHA);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    public void setOnZoomInClickListener(View.OnClickListener listener) {
        zoomIn.setOnClickListener(listener);
    }

    public void setOnZoomOutClickListener(View.OnClickListener listener) {
        zoomOut.setOnClickListener(listener);
    }

    public void show() {
        this.setVisibility(View.VISIBLE);
    }

    public void hide() {
        this.setVisibility(View.GONE);
    }

    public void setIsZoomInEnabled(boolean isEnabled) {
        zoomIn.setEnabled(isEnabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            zoomIn.setBackgroundColor(
                    ThemeUtils.getStyleAttribColorValue(context, isEnabled ? R.attr.colorControlNormal : R.attr.colorPrimary, R.color.dark_grey));
        }
    }

    public void setIsZoomOutEnabled(boolean isEnabled) {
        zoomOut.setEnabled(isEnabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            zoomOut.setBackgroundColor(
                    ThemeUtils.getStyleAttribColorValue(context, isEnabled ? R.attr.colorControlNormal : R.attr.colorPrimary, R.color.dark_grey));
        }
    }
}
