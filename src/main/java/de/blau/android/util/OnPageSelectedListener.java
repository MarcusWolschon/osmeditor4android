package de.blau.android.util;

import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

/**
 * We only ever need onPageSelected
 * 
 * @author simon
 *
 */
public abstract class OnPageSelectedListener implements OnPageChangeListener {
    
    @Override
    public void onPageScrollStateChanged(int arg0) {
        // UNUSED
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
        // UNUSED
    }
}
