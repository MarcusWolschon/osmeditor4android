package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.photos.Photo;
import de.blau.android.util.ContentResolverUtil;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.InfoDialogFragment;

/**
 * Very simple dialog fragment to display some info on an image
 * 
 * @author simon
 *
 */
public class ImageInfo extends InfoDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ImageInfo.class.getSimpleName().length());
    private static final String DEBUG_TAG = ImageInfo.class.getSimpleName().substring(0, TAG_LEN);

    private static final String URI_KEY      = "uri";
    private static final String SHOW_URI_KEY = "showUri";
    private static final String TAG          = "fragment_image_info";

    private Uri              uri        = null;
    private SimpleDateFormat dateFormat = DateFormatter.getUtcFormat("yyyy-MM-dd HH:mm:ssZZ");
    private boolean          showUri;

    /**
     * Show an info dialog for an image
     * 
     * @param activity the calling Activity
     * @param uriString the uri of the image as a string
     * @param showUri if true display the uri besides the file path
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull String uriString, boolean showUri) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            ImageInfo elementInfoFragment = newInstance(uriString, showUri);
            elementInfoFragment.show(fm, TAG);
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
     * Create a new instance of the ImageInfo dialog
     * 
     * @param uriString the uri of the image as a string
     * @param showUri if true display the uri besides the file path
     * @return an instance of ImageInfo
     */
    @NonNull
    private static ImageInfo newInstance(@NonNull String uriString, boolean showUri) {
        ImageInfo f = new ImageInfo();

        Bundle args = new Bundle();
        args.putString(URI_KEY, uriString);
        args.putBoolean(SHOW_URI_KEY, showUri);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        String uriString = getArguments().getString(URI_KEY);
        try {
            uri = Uri.parse(uriString);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Unable to parse uri " + uriString);
        }
        showUri = getArguments().getBoolean(SHOW_URI_KEY, true);
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
        builder.setTitle(R.string.image_information_title);
        builder.setView(createView(null));
        return builder.create();
    }

    @Override
    protected View createView(@Nullable ViewGroup container) {
        ScrollView sv = createEmptyView(container);
        TableLayout tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);
        TableLayout.LayoutParams tp = getTableLayoutParams();

        if (uri == null) {
            return sv;
        }
        tl.setColumnShrinkable(1, true);
        try {
            FragmentActivity activity = getActivity();
            Photo image = new Photo(getContext(), uri, null);
            String path = ContentResolverUtil.getPath(getContext(), uri);
            Log.i(DEBUG_TAG, "path " + path + " uri " + uri.toString());
            if (path != null) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.File, path, tp));
            }
            if (showUri || path == null) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.URI, uri.toString(), tp));
            }
            long size = ContentResolverUtil.getSizeColumn(getContext(), uri);
            if (size > -1) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.file_size, getString(R.string.file_size_kB, size / 1024), tp));
            }
            String creator = image.getCreator();
            if (creator != null) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.created_by, creator, tp));
            }
            Long captureDate = image.getCaptureDate();
            if (captureDate != null) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.capture_date, dateFormat.format(new Date(captureDate)), tp));
            }
            tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lon_label, prettyPrintCoord(image.getLon() / 1E7D), tp));
            tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lat_label, prettyPrintCoord(image.getLat() / 1E7D), tp));
            if (image.hasDirection()) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.direction, String.format(Locale.US, "%3d°", image.getDirection()), tp));
            }
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "Exception displaying image meta data " + ex.getMessage());
        }
        return sv;
    }
}
