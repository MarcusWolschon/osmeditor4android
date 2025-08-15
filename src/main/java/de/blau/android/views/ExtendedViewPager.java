package de.blau.android.views;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import de.blau.android.R;

/**
 * 
 * @author Simon Poole
 * @see <a href="https://blog.svpino.com/2011/08/29/disabling-pagingswiping-on-android">disabling paging/swiping on
 *      android</a>
 *
 */
public class ExtendedViewPager extends ViewPager {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ExtendedViewPager.class.getSimpleName().length());
    private static final String DEBUG_TAG = ExtendedViewPager.class.getSimpleName().substring(0, TAG_LEN);

    private boolean enabled;

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     */
    public ExtendedViewPager(@NonNull Context context) {
        super(context);
        this.enabled = true;
    }

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     * @param attrs the attributes of the XML tag that is inflating the view. This value may be null.
     */
    public ExtendedViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
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
            try {
                return super.onInterceptTouchEvent(event);
            } catch (IllegalArgumentException iaex) {
                Log.d(DEBUG_TAG, "Exception in onInterceptTouchEvent " + iaex);
            }
        }
        return false;
    }

    /**
     * Enable/disable paging
     * 
     * @param enabled if true paging is enabled otherwise disabled
     */
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

    /**
     * Check if paging is enabled
     * 
     * @return true if paging is enabled
     */
    public boolean isPagingEnabled() {
        return enabled;
    }
}
