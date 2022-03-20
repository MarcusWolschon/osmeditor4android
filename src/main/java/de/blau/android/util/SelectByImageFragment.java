package de.blau.android.util;

import java.util.ArrayList;
import java.util.List;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.ActionMenuView.OnMenuItemClickListener;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;

/**
 * Show images and allow the user to select one
 * 
 * This is a simplified version of the PhotoViewerFragment there is some potential to reduce code duplication by a
 * common parent but not much.
 * 
 * @author simon
 *
 */
public class SelectByImageFragment extends ImmersiveDialogFragment implements OnMenuItemClickListener {
    private static final String DEBUG_TAG = SelectByImageFragment.class.getName();

    public static final String TAG = "fragment_combo_image_viewer";

    static final String IMAGE_LIST_KEY   = "image_list";
    static final String START_POS_KEY    = "start_pos";
    static final String IMAGE_LOADER_KEY = "loader";

    private static final int MENUITEM_BACK    = 0;
    private static final int MENUITEM_FORWARD = 1;

    private List<String> imageList = null;

    SubsamplingScaleImageView photoView = null;

    private ImageLoader imageLoader;

    private ViewPager viewPager;

    /**
     * Show a dialog containing a view for the list of images of which one can be selected
     * 
     * @param activity the calling Activity
     * @param imageList list of Uris
     * @param startPos starting position in the list
     * @param loader callback for loading images etc
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull ArrayList<String> imageList, int startPos, @NonNull ImageLoader loader) { // NOSONAR
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            SelectByImageFragment photoViewerFragment = newInstance(imageList, startPos, loader, true);
            photoViewerFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Show a dialog containing a view for the list of images of which one can be selected
     * 
     * @param fragment the calling Fragment
     * @param imageList list of Uris
     * @param startPos starting position in the list
     * @param loader callback for loading images etc
     */
    public static void showDialog(@NonNull Fragment fragment, @NonNull ArrayList<String> imageList, int startPos, @NonNull ImageLoader loader) { // NOSONAR
        de.blau.android.dialogs.Util.dismissDialog(fragment, TAG);
        try {
            FragmentManager fm = fragment.getChildFragmentManager();
            SelectByImageFragment photoViewerFragment = newInstance(imageList, startPos, loader, true);
            photoViewerFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * 
     * @param imageList list of Uris
     * @param startPos starting position in the list
     * @param loader callback for loading images etc.
     * @param wrap if true wrap around when last/first photo is reached
     * @return a new instance of PhotoViwerFragment
     */
    @NonNull
    public static SelectByImageFragment newInstance(@NonNull ArrayList<String> imageList, int startPos, @NonNull ImageLoader loader, boolean wrap) { // NOSONAR
        SelectByImageFragment f = new SelectByImageFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(IMAGE_LIST_KEY, imageList);
        args.putInt(START_POS_KEY, startPos);
        args.putSerializable(IMAGE_LOADER_KEY, loader);
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setNeutralButton(R.string.done, doNothingListener);
        builder.setPositiveButton(R.string.select, (DialogInterface dialog, int which) -> imageLoader.onSelected(getCurrentPosition()));
        builder.setNegativeButton(R.string.clear, (dialog, which) -> imageLoader.clearSelection());
        builder.setView(createView(savedInstanceState));
        return builder.create();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!getShowsDialog()) {
            return createView(savedInstanceState);
        }
        return null;
    }

    /**
     * Create the view we want to display
     * 
     * @param savedInstanceState the saved state if any
     * @return the View
     */
    @NonNull
    private View createView(@Nullable Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);
        int startPos = 0;
        if (savedInstanceState == null) {
            Log.d(DEBUG_TAG, "Initializing from intent");
            imageList = getArguments().getStringArrayList(IMAGE_LIST_KEY);
            startPos = getArguments().getInt(START_POS_KEY);
            imageLoader = (ImageLoader) getArguments().getSerializable(IMAGE_LOADER_KEY);
            getArguments().remove(IMAGE_LOADER_KEY);
        } else {
            Log.d(DEBUG_TAG, "Initializing from saved state");
            imageList = savedInstanceState.getStringArrayList(IMAGE_LIST_KEY);
            startPos = savedInstanceState.getInt(START_POS_KEY);
            imageLoader = (ImageLoader) savedInstanceState.getSerializable(IMAGE_LOADER_KEY);
        }

        View layout = themedInflater.inflate(R.layout.photo_viewer, null);

        ImagePagerAdapter imagePagerAdapter = new ImagePagerAdapter(activity, imageLoader);

        viewPager = (ViewPager) layout.findViewById(R.id.pager);
        viewPager.setAdapter(imagePagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setCurrentItem(startPos);
        viewPager.addOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // UNUSED
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // UNUSED
            }

            @Override
            public void onPageSelected(int page) {
                if (imageLoader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode()) {
                    // doing this in single window mode is very annoying so we don't
                    imageLoader.showOnMap(getContext(), page);
                }
            }

        });

        ActionMenuView menuView = (ActionMenuView) layout.findViewById(R.id.photoMenuView);
        Menu menu = menuView.getMenu();

        MenuItem itemBackward = menu.add(Menu.NONE, MENUITEM_BACK, Menu.NONE, R.string.back).setIcon(R.drawable.ic_arrow_back_white_36dp);
        itemBackward.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemBackward.setEnabled(true);
        itemBackward.setIcon(R.drawable.ic_arrow_back_white_36dp);
        MenuItem itemForward = menu.add(Menu.NONE, MENUITEM_FORWARD, Menu.NONE, R.string.forward).setIcon(R.drawable.ic_arrow_forward_white_36dp);
        itemForward.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemForward.setEnabled(true);
        itemForward.setIcon(R.drawable.ic_arrow_forward_white_36dp);
        menuView.setOnMenuItemClickListener(this);
        return layout;
    }

    class ImagePagerAdapter extends PagerAdapter {

        final Context     mContext;
        LayoutInflater    mLayoutInflater;
        final ImageLoader loader;

        /**
         * Construct a new adapter
         * 
         * @param context an Android Context
         * @param loader the PhotoLoader to use
         */
        public ImagePagerAdapter(@NonNull Context context, @NonNull ImageLoader loader) {
            mContext = context;
            this.loader = loader;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return imageList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((LinearLayout) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = mLayoutInflater.inflate(R.layout.image_viewer_item, container, false);
            loader.setTitle(itemView.findViewById(R.id.imageViewTitle), position);
            loader.setDescription(itemView.findViewById(R.id.imageViewDescription), position);
            SubsamplingScaleImageView view = itemView.findViewById(R.id.imageView);
            view.setOnClickListener((View v) -> {
                loader.onSelected(position);
                Dialog dialog = getDialog();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }

            );
            loader.load(view, imageList.get(position));
            container.addView(itemView);
            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }

        @Override
        public int getItemPosition(Object item) {
            return POSITION_NONE; // hack so that everything gets updated on notifyDataSetChanged
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        imageLoader.setParentFragment(getParentFragment());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int size = imageList.size();
        int pos = viewPager.getCurrentItem();
        if (!imageList.isEmpty() && pos < size) {
            switch (item.getItemId()) {
            case MENUITEM_BACK:
                pos = pos - 1;
                if (pos == -1) {
                    pos = size - 1;
                }
                viewPager.setCurrentItem(pos);
                break;
            case MENUITEM_FORWARD:
                pos = (pos + 1) % size;
                viewPager.setCurrentItem(pos);
                break;
            default:
                // do nothing
            }
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putStringArrayList(IMAGE_LIST_KEY, (ArrayList<String>) imageList);
        outState.putInt(START_POS_KEY, viewPager.getCurrentItem());
        outState.putSerializable(SelectByImageFragment.IMAGE_LOADER_KEY, imageLoader);
    }

    /**
     * Get the current position of the ViewPager
     * 
     * @return the current position
     */
    int getCurrentPosition() {
        return viewPager.getCurrentItem();
    }

    /**
     * Set the ViewPager to the this position
     * 
     * @param pos the new position
     */
    void setCurrentPosition(int pos) {
        viewPager.setCurrentItem(pos);
    }
}
