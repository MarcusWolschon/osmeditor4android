package de.blau.android.dialogs;

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

    private static final String DEBUG_TAG = ImageInfo.class.getSimpleName().substring(0, Math.min(23, ImageInfo.class.getSimpleName().length()));

    private static final String URI_KEY = "uri";

    private static final String TAG = "fragment_image_info";

    private Uri              uri        = null;
    private SimpleDateFormat dateFormat = DateFormatter.getUtcFormat("yyyy-MM-dd HH:mm:ssZZ");

    /**
     * Show an info dialog for an image
     * 
     * @param activity the calling Activity
     * @param uriString the uri of the image as a string
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull String uriString) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            ImageInfo elementInfoFragment = newInstance(uriString);
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
     * @param feature Feature to display the info on
     * 
     * @return an instance of ImageInfo
     */
    @NonNull
    private static ImageInfo newInstance(@NonNull String uriString) {
        ImageInfo f = new ImageInfo();

        Bundle args = new Bundle();
        args.putString(URI_KEY, uriString);

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

        if (uri != null) {
            tl.setColumnShrinkable(1, true);
            try {
                FragmentActivity activity = getActivity();
                Photo image = new Photo(getContext(), uri, null);
                String path = ContentResolverUtil.getPath(getContext(), uri);
                tl.addView(TableLayoutUtils.createRow(activity, path == null ? uri.toString() : path, null, tp));
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
        }
        return sv;
    }
}
