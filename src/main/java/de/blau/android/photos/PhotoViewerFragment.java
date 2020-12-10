package de.blau.android.photos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.Ui;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

/**
 * Very simple photo viewer
 * 
 * @author simon
 *
 */
public class PhotoViewerFragment extends ImmersiveDialogFragment implements OnMenuItemClickListener {
    private static final String DEBUG_TAG = PhotoViewerFragment.class.getName();

    public static final String TAG = "fragment_photo_viewer";

    static final String PHOTO_LIST_KEY   = "photo_list";
    static final String START_POS_KEY    = "start_pos";
    static final String PHOTO_LOADER_KEY = "loader";
    static final String WRAP_KEY         = "wrap";

    private static final int MENUITEM_BACK    = 0;
    private static final int MENUITEM_SHARE   = 1;
    private static final int MENUITEM_GOTO    = 2;
    private static final int MENUITEM_DELETE  = 3;
    private static final int MENUITEM_FORWARD = 4;

    private List<String> photoList = null;

    SubsamplingScaleImageView photoView = null;

    private PhotoPagerAdapter photoPagerAdapter;
    private PhotoLoader       photoLoader;

    private ViewPager viewPager;

    private MenuItem itemBackward = null;
    private MenuItem itemForward  = null;
    private boolean  wrap         = true;
    ActionMenuView   menuView;

    /**
     * Show a dialog containing a view for the list of photos
     * 
     * @param activity the calling Activity
     * @param photoList list of Uris
     * @param startPos starting position in the list
     * @param loader callback for loading images etc
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull ArrayList<String> photoList, int startPos, @Nullable PhotoLoader loader) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            PhotoViewerFragment photoViewerFragment = newInstance(photoList, startPos, loader, true);
            photoViewerFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * 
     * @param photoList list of Uris
     * @param startPos starting position in the list
     * @param loader callback for loading images etc.
     * @return a new instance of PhotoViwerFragment
     */
    @NonNull
    public static PhotoViewerFragment newInstance(@NonNull ArrayList<String> photoList, int startPos, @Nullable PhotoLoader loader, boolean wrap) {
        PhotoViewerFragment f = new PhotoViewerFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(PHOTO_LIST_KEY, photoList);
        args.putInt(START_POS_KEY, startPos);
        args.putSerializable(PHOTO_LOADER_KEY, loader);
        args.putBoolean(WRAP_KEY, wrap);
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
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

    PhotoLoader defaultLoader = new PhotoLoader() {
        private static final long serialVersionUID = 1L;

        @Override
        public void load(SubsamplingScaleImageView view, String uri) {
            view.setOrientation(SubsamplingScaleImageView.ORIENTATION_90);
            view.setImage(ImageSource.uri(uri));
        }

        @Override
        public void showOnMap(Context context, int index) {
            try {
                Photo p = new Photo(context, Uri.parse(photoList.get(index)));
                if (getShowsDialog()) {
                    Map map = (context instanceof Main) ? ((Main) context).getMap() : null;
                    final de.blau.android.layer.photos.MapOverlay overlay = map != null ? map.getPhotoLayer() : null;
                    if (map != null && overlay != null) {
                        App.getLogic().setZoom(map, Ui.ZOOM_FOR_ZOOMTO);
                        map.getViewBox().moveTo(map, p.getLon(), p.getLat());
                        overlay.setSelected(p); // this isn't the same instance as in the layer but should work
                        map.invalidate();
                    }
                    getDialog().dismiss();
                } else if (!App.isPropertyEditorRunning()) {
                    System.out.println("PropertyEditor not running");
                    Intent intent = new Intent(context, Main.class);
                    intent.setData(p.getRefUri(context));
                    getContext().startActivity(intent);
                }
            } catch (NumberFormatException | IOException e) {
                Snack.toastTopError(context, context.getString(R.string.toast_error_accessing_photo, Integer.toString(index)));
            }
        }

        @Override
        public void share(Context context, String uri) {
            de.blau.android.layer.photos.Util.startExternalPhotoViewer(context, Uri.parse(uri));
            if (getShowsDialog() && photoList.size() == 1) {
                getDialog().dismiss();
            }
        }
    };

    /**
     * Create the view we want to display
     * 
     * @return the View
     */
    private View createView(@Nullable Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);
        int startPos = 0;
        if (savedInstanceState == null) {
            Log.d(DEBUG_TAG, "Initializing from intent");
            photoList = getArguments().getStringArrayList(PHOTO_LIST_KEY);
            startPos = getArguments().getInt(START_POS_KEY);
            photoLoader = (PhotoLoader) getArguments().getSerializable(PHOTO_LOADER_KEY);
            getArguments().remove(PHOTO_LOADER_KEY);
            wrap = getArguments().getBoolean(WRAP_KEY, true);
        } else {
            Log.d(DEBUG_TAG, "Initializing from saved state");
            photoList = savedInstanceState.getStringArrayList(PHOTO_LIST_KEY);
            startPos = savedInstanceState.getInt(START_POS_KEY);
            photoLoader = (PhotoLoader) savedInstanceState.getSerializable(PHOTO_LOADER_KEY);
            wrap = savedInstanceState.getBoolean(WRAP_KEY);
        }
        if (photoLoader == null) {
            photoLoader = defaultLoader;
        }

        View layout = themedInflater.inflate(R.layout.photo_viewer, null);
        photoPagerAdapter = new PhotoPagerAdapter(activity, photoLoader);

        viewPager = (ViewPager) layout.findViewById(R.id.pager);
        viewPager.setAdapter(photoPagerAdapter);
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
                if (photoLoader != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode()) {
                        // doing this in single window mode is very annoying so we don't
                        photoLoader.showOnMap(getContext(), page);
                    }
                }
            }

        });

        menuView = (ActionMenuView) layout.findViewById(R.id.photoMenuView);
        Menu menu = menuView.getMenu();
        itemBackward = menu.add(Menu.NONE, MENUITEM_BACK, Menu.NONE, R.string.back).setIcon(R.drawable.ic_arrow_back_white_36dp);
        itemBackward.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENUITEM_SHARE, Menu.NONE, R.string.share).setIcon(R.drawable.ic_share_white_36dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENUITEM_GOTO, Menu.NONE, R.string.photo_viewer_goto).setIcon(R.drawable.ic_map_white_36dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        if (getString(R.string.content_provider).equals(Uri.parse(photoList.get(startPos)).getAuthority())) {
            // we can only delete stuff that is provided by our provider, currently this is a bit of a hack
            menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, R.string.delete).setIcon(R.drawable.ic_delete_forever_white_36dp)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        itemForward = menu.add(Menu.NONE, MENUITEM_FORWARD, Menu.NONE, R.string.forward).setIcon(R.drawable.ic_arrow_forward_white_36dp);
        itemForward.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuView.setOnMenuItemClickListener(this);
        prepareMenu();
        return layout;
    }

    /**
     * Prepare the menu for display
     */
    void prepareMenu() {
        final int size = photoList.size();
        boolean multiple = size > 1;
        int pos = viewPager.getCurrentItem();
        final boolean forwardEnabled = multiple && (wrap || pos < size - 1);
        itemForward.setEnabled(forwardEnabled);
        // for whatever reason drawables with state lists don't seem to work in menus
        itemForward.setIcon(forwardEnabled ? R.drawable.ic_arrow_forward_white_36dp : R.drawable.ic_arrow_forward_dimmed_36dp);
        final boolean backwardEnabled = multiple && (wrap || pos > 0);
        itemBackward.setEnabled(backwardEnabled);
        itemBackward.setIcon(backwardEnabled ? R.drawable.ic_arrow_back_white_36dp : R.drawable.ic_arrow_back_dimmed_36dp);
    }

    class PhotoPagerAdapter extends PagerAdapter {

        final Context     mContext;
        LayoutInflater    mLayoutInflater;
        final PhotoLoader loader;

        /**
         * Construct a new adapter
         * 
         * @param context an Android Context
         */
        public PhotoPagerAdapter(@NonNull Context context, @NonNull PhotoLoader loader) {
            mContext = context;
            this.loader = loader;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return photoList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((LinearLayout) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = mLayoutInflater.inflate(R.layout.photo_viewer_item, container, false);
            SubsamplingScaleImageView view = itemView.findViewById(R.id.photoView);
            loader.load(view, photoList.get(position));
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
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int size = photoList.size();
        FragmentActivity caller = getActivity();
        Map map = (caller instanceof Main) ? ((Main) caller).getMap() : null;
        final de.blau.android.layer.photos.MapOverlay overlay = map != null ? map.getPhotoLayer() : null;

        int pos = viewPager.getCurrentItem();
        if (photoList != null && !photoList.isEmpty() && pos < size) {
            try {
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
                case MENUITEM_GOTO:
                    if (photoLoader != null) {
                        photoLoader.showOnMap(caller, pos);
                    }
                    break;
                case MENUITEM_SHARE:
                    if (photoLoader != null) {
                        photoLoader.share(caller, photoList.get(pos));
                    }
                    break;
                case MENUITEM_DELETE:
                    // TODO This is not generic and only works for the photo layer
                    new AlertDialog.Builder(getContext()).setTitle(R.string.photo_viewer_delete_title)
                            .setPositiveButton(R.string.photo_viewer_delete_button, (dialog, which) -> {
                                int position = viewPager.getCurrentItem();
                                if (position >= 0 && position < photoList.size()) { // avoid crashes from bouncing
                                    Uri photoUri = Uri.parse(photoList.get(position));
                                    try {
                                        // delete from in memory and on device index
                                        try (PhotoIndex index = new PhotoIndex(getContext())) {
                                            index.deletePhoto(getContext(), photoUri);
                                        }
                                        if (getShowsDialog() && overlay != null) {
                                            // as the Photo was selected before calling this it will still have a
                                            // reference in the layer
                                            overlay.setSelected(null);
                                            overlay.invalidate();
                                        } else {
                                            Intent intent = new Intent(getContext(), Main.class);
                                            intent.setAction(Main.ACTION_DELETE_PHOTO);
                                            intent.setData(photoUri);
                                            getContext().startActivity(intent);
                                        }
                                        // actually delete
                                        if (getContext().getContentResolver().delete(photoUri, null, null) >= 1) {
                                            photoList.remove(position);
                                            position = Math.min(position, size - 1); // this will set pos to -1 if
                                                                                     // empty,
                                            // but we will exit in that case in any case
                                            if (getShowsDialog() && photoList.isEmpty()) {
                                                // in fragment mode we want to stay around
                                                getDialog().dismiss();
                                            } else {
                                                photoPagerAdapter.notifyDataSetChanged();
                                                viewPager.setCurrentItem(position);
                                                if (photoList.size() == 1 && itemBackward != null && itemForward != null) {
                                                    itemBackward.setEnabled(false);
                                                    itemForward.setEnabled(false);
                                                }
                                            }
                                        }
                                    } catch (java.lang.SecurityException sex) {
                                        Log.e(DEBUG_TAG, "Error deleting: " + sex.getMessage() + " " + sex.getClass().getName());
                                        Snack.toastTopError(getContext(), getString(R.string.toast_permission_denied, sex.getMessage()));
                                    }
                                }
                            }).setNeutralButton(R.string.cancel, null).show();
                    break;
                default:
                    // do nothing
                }
            } finally {
                prepareMenu();
            }
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putStringArrayList(PHOTO_LIST_KEY, (ArrayList<String>) photoList);
        outState.putInt(START_POS_KEY, viewPager.getCurrentItem());
        if (!photoLoader.equals(defaultLoader)) {
            outState.putSerializable(PhotoViewerFragment.PHOTO_LOADER_KEY, photoLoader);
        }
        outState.putBoolean(WRAP_KEY, wrap);
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

    /**
     * Add a photo at the end of the list and switch to it
     * 
     * @param photo the photo Uri as a String
     */
    void addPhoto(@NonNull String photo) {
        photoList.add(photo);
        photoPagerAdapter.notifyDataSetChanged();
        setCurrentPosition(photoList.size() - 1);
    }

    /**
     * Replace the existing list of photos
     * 
     * @param list the new list
     * @param pos the new position in list
     */
    void replacePhotos(@NonNull ArrayList<String> list, int pos) {
        photoList.clear();
        photoList.addAll(list);
        photoPagerAdapter.notifyDataSetChanged();
        setCurrentPosition(pos);
    }
}
