package de.blau.android.photos;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.ActionMenuView.OnMenuItemClickListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.MimeTypes;
import de.blau.android.contract.Ui;
import de.blau.android.dialogs.ImageInfo;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ConfigurationChangeAwareActivity;
import de.blau.android.util.ContentResolverUtil;
import de.blau.android.util.ImageLoader;
import de.blau.android.util.ImagePagerAdapter;
import de.blau.android.util.OnPageSelectedListener;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SizedDynamicDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Very simple photo viewer
 * 
 * @author simon
 *
 */
public class PhotoViewerFragment<T extends Serializable> extends SizedDynamicDialogFragment implements OnMenuItemClickListener {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PhotoViewerFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = PhotoViewerFragment.class.getSimpleName().substring(0, TAG_LEN);

    public static final String TAG = "fragment_photo_viewer";

    static final String        PHOTO_LIST_KEY   = "photo_list";
    static final String        START_POS_KEY    = "start_pos";
    public static final String PHOTO_LOADER_KEY = "loader";
    public static final String WRAP_KEY         = "wrap";

    private static final int MENUITEM_BACK    = 0;
    private static final int MENUITEM_SHARE   = 1;
    private static final int MENUITEM_GOTO    = 2;
    private static final int MENUITEM_INFO    = 3;
    private static final int MENUITEM_DELETE  = 4;
    private static final int MENUITEM_FORWARD = 5;

    private List<T> photoList = null;

    SubsamplingScaleImageView photoView = null;

    private PhotoPagerAdapter<T> photoPagerAdapter;
    private ImageLoader          photoLoader;

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
    public static <L extends List<V> & Serializable, V extends Serializable> void showDialog(@NonNull FragmentActivity activity, @NonNull L photoList,
            int startPos, @Nullable ImageLoader loader) { // NOSONAR
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            PhotoViewerFragment<V> photoViewerFragment = newInstance(photoList, startPos, loader, true);
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
     * @param <V>
     * @param photoList list of Uris
     * @param startPos starting position in the list
     * @param loader callback for loading images etc.
     * @param wrap if true wrap around when last/first photo is reached
     * @return a new instance of PhotoViwerFragment
     */
    @NonNull
    public static <L extends List<V> & Serializable, V extends Serializable> PhotoViewerFragment<V> newInstance(@NonNull L photoList, int startPos,
            @Nullable ImageLoader loader, boolean wrap) { // NOSONAR
        PhotoViewerFragment<V> f = new PhotoViewerFragment<>();

        Bundle args = new Bundle();
        args.putSerializable(PHOTO_LIST_KEY, photoList);
        args.putInt(START_POS_KEY, startPos);
        args.putSerializable(PHOTO_LOADER_KEY, loader);
        args.putBoolean(WRAP_KEY, wrap);
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = ThemeUtils.getAlertDialogBuilder(getActivity());
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

    ImageLoader defaultLoader = new ImageLoader() {
        private static final long serialVersionUID = 2L;

        @Override
        public void load(SubsamplingScaleImageView view, String uri) {
            load(view, uri, ExifInterface.ORIENTATION_UNDEFINED);
        }

        @Override
        public void load(SubsamplingScaleImageView view, String uri, int exifOrientation) {
            final Uri parsedUri = Uri.parse(uri);
            if (MimeTypes.HEIC.equals(view.getContext().getContentResolver().getType(parsedUri))) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    Log.w(DEBUG_TAG, "Can't load " + uri + " HEIC is not supported in this Android version");
                    return;
                }
                SubsamplingScaleImageView.setPreferredBitmapConfig(Config.ARGB_8888);
            } else {
                SubsamplingScaleImageView.setPreferredBitmapConfig(Config.RGB_565); // default
            }
            int orientation = SubsamplingScaleImageView.ORIENTATION_0;
            switch (exifOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                orientation = SubsamplingScaleImageView.ORIENTATION_90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                orientation = SubsamplingScaleImageView.ORIENTATION_180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                orientation = SubsamplingScaleImageView.ORIENTATION_270;
                break;
            default: // leave as is
            }
            view.setOrientation(orientation);
            view.setImage(ImageSource.uri(uri));
        }

        @Override
        public void showOnMap(Context context, int index) {
            try {
                Photo p = new Photo(context, getUri(index), "");
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
                    Intent intent = new Intent(context, Main.class);
                    intent.setData(p.getRefUri(context));
                    getContext().startActivity(intent);
                }
            } catch (NumberFormatException | IOException | IndexOutOfBoundsException e) {
                ScreenMessage.toastTopError(context, context.getString(R.string.toast_error_accessing_photo, Integer.toString(index)));
            }
        }

        @Override
        public void share(Context context, String uri) {
            de.blau.android.layer.photos.Util.startExternalPhotoViewer(context, Uri.parse(uri));
            if (getShowsDialog() && photoList.size() == 1) {
                getDialog().dismiss();
            }
        }

        @Override
        public boolean supportsInfo() {
            return true;
        }

        @Override
        public void info(@NonNull FragmentActivity activity, @NonNull String uri) {
            ImageInfo.showDialog(activity, uri, true);
        }

        @Override
        public boolean supportsDelete() {
            return true;
        }

        @Override
        public void delete(@NonNull final Context context, @NonNull final String uri) {
            if (viewPager == null) {
                return;
            }
            int position = getCurrentPosition();
            final int size = photoList.size();
            if (position < 0 || position > size) { // avoid crashes from bouncing
                return;
            }
            ContentResolver resolver = context.getContentResolver();
            Uri photoUri = getUri(position);
            Log.d(DEBUG_TAG, "deleting original uri " + photoUri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && deleteImageViaMediaStore(context, resolver, photoUri)) {
                // rest is handled in calling activity
                return;
            }
            ThemeUtils.getAlertDialogBuilder(getContext()).setTitle(R.string.photo_viewer_delete_title)
                    .setPositiveButton(R.string.photo_viewer_delete_button, (dialog, which) -> {
                        try {
                            if (resolver.delete(photoUri, null, null) >= 1) {
                                removeCurrentImage();
                            } else {
                                // didn't delete
                                Log.e(DEBUG_TAG, "... deleting failed");
                                return;
                            }
                        } catch (java.lang.SecurityException sex) {
                            Log.e(DEBUG_TAG, "Error deleting: " + sex.getMessage() + " " + sex.getClass().getName());
                            ScreenMessage.toastTopError(context, getString(R.string.toast_permission_denied, sex.getMessage()));
                        }
                    }).setNeutralButton(R.string.cancel, null).show();
        }

        /**
         * Delete the current image, this will show a modal for us
         * 
         * @param context an Android Context
         * @param resolver a ContentResolver
         * @param photoUri the Uri of the image
         * @return true if we were able to start the deletion process
         */
        @SuppressWarnings("unchecked")
        @TargetApi(30)
        public boolean deleteImageViaMediaStore(@NonNull final Context context, ContentResolver resolver, Uri photoUri) {
            try {
                if (!MediaStore.AUTHORITY.equals(photoUri.getAuthority())) {
                    long mediaId = ContentResolverUtil.imageFilePathToMediaID(resolver, photoUri.toString());
                    if (mediaId == 0) {
                        Log.e(DEBUG_TAG, "Can't convert " + photoUri + " to media Uri");
                        return false;
                    }
                    photoUri = ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(ContentResolverUtil.VOLUME_EXTERNAL), mediaId);
                }
                Log.d(DEBUG_TAG, "deleting " + photoUri);
                IntentSenderRequest.Builder builder = new IntentSenderRequest.Builder(MediaStore.createTrashRequest(resolver, Arrays.asList(photoUri), true));
                ((PhotoViewerActivity<T>) getActivity()).getDeleteRequestLauncher().launch(builder.build());
            } catch (java.lang.SecurityException sex) {
                Log.e(DEBUG_TAG, "Error deleting: " + sex.getMessage() + " " + sex.getClass().getName());
                ScreenMessage.toastTopError(context, getString(R.string.toast_permission_denied, sex.getMessage()));
                return false;
            }
            return true;
        }

    };

    /**
     * Remove image from the pager, index etc
     */
    public void removeCurrentImage() {
        int position = getCurrentPosition();
        final int size = photoList.size();
        Uri uri = getUri(position);
        photoList.remove(position);
        position = Math.min(position, size - 1); // this will set pos to -1 if empty,
        // but we will exit in that case in any case
        Context context = getContext();
        if (getShowsDialog()) {
            // delete from in memory and on device index
            try (PhotoIndex index = new PhotoIndex(context)) {
                index.deletePhoto(context, uri);
            }
            Map map = (context instanceof Main) ? ((Main) context).getMap() : null;
            final de.blau.android.layer.photos.MapOverlay overlay = map != null ? map.getPhotoLayer() : null;
            if (overlay != null) {
                // as the Photo was selected before calling this it will still have a
                // reference in the layer
                overlay.deselectObjects();
                overlay.invalidate();
            }
            if (photoList.isEmpty()) {
                // in fragment mode we want to stay around
                getDialog().dismiss();
            }
        } else {
            photoPagerAdapter.notifyDataSetChanged();
            viewPager.setCurrentItem(position);
            if (size == 1 && itemBackward != null && itemForward != null) {
                itemBackward.setEnabled(false);
                itemForward.setEnabled(false);
            }
            Intent intent = new Intent(context, Main.class);
            intent.setAction(Main.ACTION_DELETE_PHOTO);
            intent.setData(uri);
            context.startActivity(intent);
        }
    }

    /**
     * Get the URI in String format for the item
     * 
     * @param index
     * @return
     */
    @NonNull
    protected Uri getUri(int index) {
        T item = photoList.get(index);
        return item instanceof Photo ? ((Photo) item).getRefUri(getContext()) : Uri.parse((String) item);
    }

    /**
     * Create the view we want to display
     * 
     * @param savedInstanceState the saved state if any
     * @return the View
     */
    @SuppressWarnings("unchecked")
    @NonNull
    private View createView(@Nullable Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);
        int startPos = 0;
        if (savedInstanceState == null) {
            Log.d(DEBUG_TAG, "Initializing from intent");
            photoList = Util.getSerializeable(getArguments(), PhotoViewerFragment.PHOTO_LIST_KEY, ArrayList.class);
            startPos = getArguments().getInt(START_POS_KEY);
            photoLoader = Util.getSerializeable(getArguments(), PHOTO_LOADER_KEY, ImageLoader.class);
            getArguments().remove(PHOTO_LOADER_KEY);
            wrap = getArguments().getBoolean(WRAP_KEY, true);
        } else {
            Log.d(DEBUG_TAG, "Initializing from saved state");
            String photoListFilename = savedInstanceState.getString(PhotoViewerFragment.PHOTO_LIST_KEY);
            photoList = new SavingHelper<ArrayList<T>>().load(getContext(), photoListFilename, true);
            startPos = savedInstanceState.getInt(START_POS_KEY);
            photoLoader = Util.getSerializeable(savedInstanceState, PHOTO_LOADER_KEY, ImageLoader.class);
            wrap = savedInstanceState.getBoolean(WRAP_KEY);
        }
        if (photoLoader == null) {
            photoLoader = defaultLoader;
        }

        View layout = themedInflater.inflate(R.layout.photo_viewer, null);
        // sanity check
        if (photoList == null || photoList.isEmpty() || (startPos + 1) > photoList.size()) {
            Log.e(DEBUG_TAG, "List empty or start position out of bounds");
            ScreenMessage.toastTopError(activity, R.string.toast_no_photo_found);
            return layout;
        }
        photoPagerAdapter = new PhotoPagerAdapter<>(activity, photoLoader, photoList);

        viewPager = (ViewPager) layout.findViewById(R.id.pager);
        viewPager.setAdapter(photoPagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setCurrentItem(startPos);
        viewPager.addOnPageChangeListener((OnPageSelectedListener) page -> {
            if (photoLoader != null && Util.isInMultiWindowModeCompat(activity)) {
                // doing this in single window mode is very annoying so we don't
                photoLoader.showOnMap(requireContext(), page);
            }
        });

        menuView = (ActionMenuView) layout.findViewById(R.id.photoMenuView);
        Menu menu = menuView.getMenu();
        itemBackward = menu.add(Menu.NONE, MENUITEM_BACK, Menu.NONE, R.string.back).setIcon(R.drawable.ic_arrow_back_white_36dp);
        itemBackward.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENUITEM_SHARE, Menu.NONE, R.string.share).setIcon(R.drawable.ic_share_white_36dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENUITEM_GOTO, Menu.NONE, R.string.photo_viewer_goto).setIcon(R.drawable.ic_map_white_36dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        if (photoLoader.supportsInfo()) {
            menu.add(Menu.NONE, MENUITEM_INFO, Menu.NONE, R.string.menu_information).setIcon(R.drawable.outline_info_white_48dp)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if (photoLoader.supportsDelete()) {
            menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, R.string.delete).setIcon(R.drawable.ic_delete_forever_white_36dp)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        itemForward = menu.add(Menu.NONE, MENUITEM_FORWARD, Menu.NONE, R.string.forward).setIcon(R.drawable.ic_arrow_forward_white_36dp);
        itemForward.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuView.setOnMenuItemClickListener(this);
        prepareMenu();
        ViewGroupCompat.installCompatInsetsDispatch(layout);
        ViewCompat.setOnApplyWindowInsetsListener(layout, ConfigurationChangeAwareActivity.onApplyWindowInsetslistener);
        return layout;
    }

    /**
     * Prepare the menu for display
     */
    void prepareMenu() {
        final int size = photoList.size();
        boolean multiple = size > 1;
        int pos = getCurrentPosition();
        final boolean forwardEnabled = multiple && (wrap || pos < size - 1);
        itemForward.setEnabled(forwardEnabled);
        // for whatever reason drawables with state lists don't seem to work in menus
        itemForward.setIcon(forwardEnabled ? R.drawable.ic_arrow_forward_white_36dp : R.drawable.ic_arrow_forward_dimmed_36dp);
        final boolean backwardEnabled = multiple && (wrap || pos > 0);
        itemBackward.setEnabled(backwardEnabled);
        itemBackward.setIcon(backwardEnabled ? R.drawable.ic_arrow_back_white_36dp : R.drawable.ic_arrow_back_dimmed_36dp);
    }

    private class PhotoPagerAdapter<S extends Serializable> extends ImagePagerAdapter<S> {

        /**
         * Construct a new adapter
         * 
         * @param context an Android Context
         * @param loader the PhotoLoader to use
         * @param images list of images
         */
        public PhotoPagerAdapter(@NonNull Context context, @NonNull ImageLoader loader, @NonNull List<S> images) {
            super(context, loader, images);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = mLayoutInflater.inflate(R.layout.photo_viewer_item, container, false);
            SubsamplingScaleImageView view = itemView.findViewById(R.id.photoView);
            try {
                S item = images.get(position);
                if (item instanceof Photo) {
                    loader.load(view, ((Photo) item).getRef(), ((Photo) item).getOrientation());
                } else if (item instanceof String) {
                    loader.load(view, (String) item);
                } else {
                    Log.e(DEBUG_TAG, "Unexpecteded element " + item);
                }
            } catch (IndexOutOfBoundsException e) {
                Log.e(DEBUG_TAG, e.getMessage());
            }
            container.addView(itemView);
            return itemView;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int size = photoList.size();
        FragmentActivity caller = getActivity();
        int pos = getCurrentPosition();
        if (photoList == null || photoList.isEmpty() || pos >= size) {
            return false;
        }
        try {
            final boolean loaderPresent = photoLoader != null;
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
                if (loaderPresent) {
                    photoLoader.showOnMap(caller, pos);
                }
                break;
            case MENUITEM_SHARE:
                if (loaderPresent) {
                    photoLoader.share(caller, getUri(pos).toString());
                }
                break;
            case MENUITEM_INFO:
                if (loaderPresent) {
                    photoLoader.info(caller, getUri(pos).toString());
                }
                break;
            case MENUITEM_DELETE:
                if (loaderPresent) {
                    photoLoader.delete(caller, getUri(pos).toString());
                }
                break;
            default:
                // do nothing
            }
        } finally {
            prepareMenu();
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        String listFilename = photoListFilename();
        new SavingHelper<ArrayList<T>>().save(getContext(), listFilename, new ArrayList<>(photoList), true);
        outState.putString(PhotoViewerFragment.PHOTO_LIST_KEY, listFilename);
        // there seems to be a situation in which this is called before viewPager is created
        outState.putInt(START_POS_KEY, viewPager != null ? viewPager.getCurrentItem() : 0);
        if (!photoLoader.equals(defaultLoader)) {
            outState.putSerializable(PhotoViewerFragment.PHOTO_LOADER_KEY, photoLoader);
        }
        outState.putBoolean(WRAP_KEY, wrap);
    }

    /**
     * @return the file name under which we save the list
     */
    String photoListFilename() {
        return photoLoader.getClass().getCanonicalName() + ".res";
    }

    /**
     * Get the current position of the ViewPager
     * 
     * @return the current position
     */
    int getCurrentPosition() {
        return viewPager != null ? viewPager.getCurrentItem() : 0;
    }

    /**
     * Set the ViewPager to the this position
     * 
     * @param pos the new position
     */
    void setCurrentPosition(int pos) {
        if (viewPager != null) {
            viewPager.setCurrentItem(pos);
        }
    }

    /**
     * Add a photo at the end of the list and switch to it
     * 
     * @param photo the photo Uri as a String or Photo object
     */
    void addPhoto(@NonNull T photo) {
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
    void replacePhotos(@NonNull List<T> list, int pos) {
        photoList.clear();
        photoList.addAll(list);
        photoPagerAdapter.notifyDataSetChanged();
        setCurrentPosition(pos);
    }
}
