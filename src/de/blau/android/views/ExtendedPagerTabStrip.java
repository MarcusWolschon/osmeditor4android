package de.blau.android.views;

import android.R;
import android.content.Context;
import android.support.v4.view.PagerTabStrip;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import de.blau.android.util.ThemeUtils;

public class ExtendedPagerTabStrip extends PagerTabStrip {

	private static final String DEBUG_TAG = ExtendedPagerTabStrip.class.getName();

    private boolean enabled;
    
	public ExtendedPagerTabStrip(Context context) {
		super(context);
		// FIXME move to stlye
		setTabIndicatorColorResource(ThemeUtils.getStyleAttribColorValue(context, R.attr.colorAccent, R.attr.colorAccent));
		this.enabled = true;
	}
	
	public ExtendedPagerTabStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
		setTabIndicatorColorResource(ThemeUtils.getStyleAttribColorValue(context, R.attr.colorAccent, R.attr.colorAccent));
		this.enabled = true;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (this.enabled) {
			return super.onTouchEvent(event);
		}
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (this.enabled) {
			return super.onInterceptTouchEvent(event);
		}
		return true;
	}

	public void setPagingEnabled(boolean enabled) {
		Log.d(DEBUG_TAG,"Setting paging enabled to " + enabled);
		this.enabled = enabled;
	}
}
