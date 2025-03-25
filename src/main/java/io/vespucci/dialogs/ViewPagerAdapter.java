package io.vespucci.dialogs;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class ViewPagerAdapter extends PagerAdapter {
    final Context context;
    final View    layout;
    final int[]   pageRes;
    final int[]   titleRes;

    /**
     * Construct a new Adapter for the tab/page names
     * 
     * @param context an Android Context
     * @param layout the View holding the tabs/pages
     * @param pageRes an array holding the resource ids of the pages
     * @param titleRes an array holding the resource ids of the page titles
     */
    public ViewPagerAdapter(@NonNull Context context, @NonNull View layout, @NonNull int[] pageRes, @NonNull int[] titleRes) {
        super();
        this.context = context;
        this.layout = layout;
        this.pageRes = pageRes;
        this.titleRes = titleRes;
        if (pageRes.length != titleRes.length) {
            throw new IllegalArgumentException("Resource arrays need to have the same length");
        }
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        if (position >= 0 && position < pageRes.length) {
            return layout.findViewById(pageRes[position]);
        } else {
            throw new IllegalArgumentException("position needs to be between 0 and " + pageRes.length);
        }
    }

    @Override
    public int getCount() {
        return pageRes.length;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position >= 0 && position < pageRes.length) {
            return context.getString(titleRes[position]);
        } else {
            throw new IllegalArgumentException("position needs to be between 0 and " + pageRes.length);
        }
    }
}
