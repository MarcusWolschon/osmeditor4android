package de.blau.android.views;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;
import de.blau.android.R;

public class Controls extends LinearLayout {

	private static final String DEBUG_TAG = Controls.class.getName();
	
	private final FloatingActionButton follow;
	private final FloatingActionButton zoomIn;
	private final FloatingActionButton zoomOut;
    
	public Controls(Context context) {
		this(context,null);
	}
	
	public Controls(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFocusable(false);
		LayoutInflater inflater = (LayoutInflater) (new ContextThemeWrapper(context,R.style.Theme_AppCompat_Light).getSystemService(Context.LAYOUT_INFLATER_SERVICE));
		inflater.inflate(R.layout.controls, this, true);
		follow = (FloatingActionButton)findViewById(R.id.follow);
		zoomIn = (FloatingActionButton)findViewById(R.id.zoom_in);
		zoomOut = (FloatingActionButton)findViewById(R.id.zoom_out);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}
	
	public void setOnFollowClickListener (View.OnClickListener listener) {
		follow.setOnClickListener(listener);
	}
	
	public void setOnZoomInClickListener (View.OnClickListener listener) {
		zoomIn.setOnClickListener(listener);
	}
	
	public void setOnZoomOutClickListener (View.OnClickListener listener) {
		zoomOut.setOnClickListener(listener);
	}
	
	public void show() {
		this.setVisibility(View.VISIBLE);
	}
	
	public void hide() {
		this.setVisibility(View.GONE);
	}
	
	public void setIsZoomInEnabled (boolean isEnabled) {
		zoomIn.setEnabled(isEnabled);
	}
	
	public void setIsZoomOutEnabled (boolean isEnabled) {
		zoomOut.setEnabled(isEnabled);
	}
}
