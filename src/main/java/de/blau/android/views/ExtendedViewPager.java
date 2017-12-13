package de.blau.android.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import de.blau.android.R;

/**
 * 
 * @see <a href="https://blog.svpino.com/2011/08/29/disabling-pagingswiping-on-android">disabling paging/swiping on
 *      android</a>
 *
 */
public class ExtendedViewPager extends ViewPager {

    private static final String DEBUG_TAG = ExtendedViewPager.class.getName();

    private boolean enabled;

    public ExtendedViewPager(Context context) {
        super(context);
        this.enabled = true;
    }

    public ExtendedViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        return false;
    }

    public void setPagingEnabled(boolean enabled) {
        Log.d(DEBUG_TAG, "Setting paging enabled to " + enabled);
        this.enabled = enabled;
        try {
            ExtendedPagerTabStrip tabStrip = (ExtendedPagerTabStrip) findViewById(R.id.pager_header);
            if (tabStrip != null) {
                tabStrip.setPagingEnabled(enabled);
            }
        } catch (Exception ex) {
            Log.d(DEBUG_TAG, "Exception in setPAgingEnabled " + ex);
        }
    }
}
