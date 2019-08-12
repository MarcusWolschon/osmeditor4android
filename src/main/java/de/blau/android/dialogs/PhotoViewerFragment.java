package de.blau.android.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.ActionMenuView.OnMenuItemClickListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.photos.Photo;
import de.blau.android.photos.PhotoIndex;
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

    private static final String PHOTO_LIST_KEY = "photo_list";
    private static final String START_POS_KEY  = "start_pos";

    private static final int MENUITEM_BACK    = 0;
    private static final int MENUITEM_SHARE   = 1;
    private static final int MENUITEM_GOTO    = 2;
    private static final int MENUITEM_DELETE  = 3;
    private static final int MENUITEM_FORWARD = 4;

    private List<String> photoList = null;

    SubsamplingScaleImageView photoView = null;

    private PhotoPagerAdapter photoPagerAdapter;

    private ViewPager viewPager;

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param photoList list of Uris
     * @param startPos starting position in the list
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull ArrayList<String> photoList, int startPos) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            PhotoViewerFragment photoViewerFragment = newInstance(photoList, startPos);
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
     * @return a n new instance of PhotoViwerFragment
     */
    @NonNull
    public static PhotoViewerFragment newInstance(@NonNull ArrayList<String> photoList, int startPos) {
        PhotoViewerFragment f = new PhotoViewerFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(PHOTO_LIST_KEY, photoList);
        args.putInt(START_POS_KEY, startPos);

        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
        builder.setView(createView());
        return builder.create();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!getShowsDialog()) {
            return createView();
        }
        return null;
    }

    /**
     * Create the view we want to display
     * 
     * @param container parent view or null
     * @return the View
     */
    private View createView() {
        FragmentActivity activity = getActivity();
        LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);
        photoList = getArguments().getStringArrayList(PHOTO_LIST_KEY);
        int startPos = getArguments().getInt(START_POS_KEY);
        View layout = themedInflater.inflate(R.layout.photo_viewer, null);
        photoPagerAdapter = new PhotoPagerAdapter(activity);

        viewPager = (ViewPager) layout.findViewById(R.id.pager);
        viewPager.setAdapter(photoPagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setCurrentItem(startPos);
        ActionMenuView menuView = (ActionMenuView) layout.findViewById(R.id.photoMenuView);
        Menu menu = menuView.getMenu();
        boolean multiple = photoList.size() > 1;
        if (multiple) {
            menu.add(Menu.NONE, MENUITEM_BACK, Menu.NONE, R.string.back).setIcon(R.drawable.ic_arrow_back_white_36dp)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        menu.add(Menu.NONE, MENUITEM_SHARE, Menu.NONE, R.string.share).setIcon(R.drawable.ic_share_white_36dp)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        if (multiple) {
            menu.add(Menu.NONE, MENUITEM_GOTO, Menu.NONE, R.string.photo_viewer_goto).setIcon(R.drawable.ic_map_white_36dp)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if (Uri.parse(photoList.get(startPos)).getAuthority().equals(getString(R.string.content_provider))) {
            // we can only delete stuff that is provided by our provider
            menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, R.string.delete).setIcon(R.drawable.ic_delete_forever_white_36dp)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if (multiple) {
            menu.add(Menu.NONE, MENUITEM_FORWARD, Menu.NONE, R.string.forward).setIcon(R.drawable.ic_arrow_forward_white_36dp)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        menuView.setOnMenuItemClickListener(this);
        return layout;
    }

    class PhotoPagerAdapter extends PagerAdapter {

        Context        mContext;
        LayoutInflater mLayoutInflater;

        /**
         * Construct a new adapter
         * 
         * @param context an Android Context
         */
        public PhotoPagerAdapter(@NonNull Context context) {
            mContext = context;
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
            view.setOrientation(SubsamplingScaleImageView.ORIENTATION_90);
            view.setImage(ImageSource.uri(photoList.get(position)));
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
                try {
                    Photo p = new Photo(caller, Uri.parse(photoList.get(pos)));
                    if (map != null && overlay != null) {
                        App.getLogic().setZoom(map, 19);
                        map.getViewBox().moveTo(map, p.getLon(), p.getLat());
                        overlay.setSelected(p); // this isn't the same instance as in the layer but should work
                        map.invalidate();
                    }
                    if (getShowsDialog()) {
                        getDialog().dismiss();
                    }
                } catch (NumberFormatException | IOException e) {
                    Log.e(DEBUG_TAG, "Invalid photo for " + photoList.get(pos));
                }
                break;
            case MENUITEM_SHARE:
                de.blau.android.layer.photos.Util.startExternalPhotoViewer(getContext(), Uri.parse(photoList.get(pos)));
                if (getShowsDialog() && size == 1) {
                    getDialog().dismiss();
                }
                break;
            case MENUITEM_DELETE:
                new AlertDialog.Builder(getContext()).setTitle(R.string.photo_viewer_delete_title)
                        .setPositiveButton(R.string.photo_viewer_delete_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int pos = viewPager.getCurrentItem();
                                if (pos >= 0) { // avoid crashes from bouncing
                                    Uri photoUri = Uri.parse(photoList.get(pos));
                                    try {
                                        // delete from in memory and on device index
                                        PhotoIndex index = new PhotoIndex(getContext());
                                        index.deletePhoto(getContext(), photoUri);
                                        // as the Photo was selected before calling this it will still have a
                                        // reference in the layer
                                        if (overlay != null) {
                                            overlay.setSelected(null);
                                            overlay.invalidate();
                                        }
                                        // actually delete
                                        if (getContext().getContentResolver().delete(photoUri, null, null) >= 1) {
                                            photoList.remove(pos);
                                            pos = Integer.min(pos, size - 1); // this will set pos to -1 but
                                                                              // we will exit in that case
                                            if (getShowsDialog() && photoList.isEmpty()) { // in fragment mode we want
                                                                                           // to do something else
                                                getDialog().dismiss();
                                            } else {
                                                photoPagerAdapter.notifyDataSetChanged();
                                                viewPager.setCurrentItem(pos);
                                            }
                                        }
                                    } catch (java.lang.SecurityException sex) {
                                        Snack.toastTopError(getContext(), getString(R.string.toast_permission_denied, sex.getMessage()));
                                    }
                                }
                            }
                        }).setNeutralButton(R.string.cancel, null).show();

                break;
            default:
                // do nothing
            }
        }
        return false;
    }
}
