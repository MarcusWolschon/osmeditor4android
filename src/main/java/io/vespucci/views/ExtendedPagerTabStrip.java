package io.vespucci.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

// this undocumented annotation is required for ViewPAger to detect that the strip is present
@ViewPager.DecorView
public class ExtendedPagerTabStrip extends PagerTabStrip {

    private static final String DEBUG_TAG = ExtendedPagerTabStrip.class.getSimpleName().substring(0, Math.min(23, ExtendedPagerTabStrip.class.getSimpleName().length()));

    private boolean enabled;

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     */
    public ExtendedPagerTabStrip(@NonNull Context context) {
        super(context);
        this.enabled = true;
    }

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     * @param attrs the attributes of the XML tag that is inflating the view. This value may be null.
     */
    public ExtendedPagerTabStrip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.enabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.enabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return !this.enabled || super.onInterceptTouchEvent(event);
    }

    /**
     * Enable/disable paging
     * 
     * @param enabled if true paging is enabled otherwise disabled
     */
    public void setPagingEnabled(boolean enabled) {
        Log.d(DEBUG_TAG, "Setting paging enabled to " + enabled);
        this.enabled = enabled;
    }
}
