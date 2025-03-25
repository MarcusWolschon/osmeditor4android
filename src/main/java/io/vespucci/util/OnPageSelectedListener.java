package io.vespucci.util;

import androidx.annotation.Px;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

/**
 * We only ever need onPageSelected
 * 
 * @author simon
 *
 */
public interface OnPageSelectedListener extends OnPageChangeListener {
    @Override
    default void onPageScrollStateChanged(int state) {
        // UNUSED
    }

    @Override
    default void onPageScrolled(int position, float positionOffset, @Px int positionOffsetPixels) {
        // UNUSED
    }
}
