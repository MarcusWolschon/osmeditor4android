package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class ViewPagerAdapter extends PagerAdapter {
    
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ViewPagerAdapter.class.getSimpleName().length());
    private static final String DEBUG_TAG = ViewPagerAdapter.class.getSimpleName().substring(0, TAG_LEN);
    
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
    
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // of this isn't overridden the default we throw a runtime exception
        Log.e(DEBUG_TAG, "destroyItem unexpectedly called for position " + position);
    }
}
