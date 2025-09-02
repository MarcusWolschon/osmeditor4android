package de.blau.android.photos;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.Log;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.imagestorage.ImageStorage;
import de.blau.android.imagestorage.PanoramaxStorage;
import de.blau.android.imagestorage.UploadResult;
import de.blau.android.imagestorage.WikimediaCommonsStorage;
import de.blau.android.osm.OsmElement;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.ImageStorageConfiguration;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ContentResolverUtil;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;

public class UploadImage {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, UploadImage.class.getSimpleName().length());
    private static final String DEBUG_TAG = UploadImage.class.getSimpleName().substring(0, TAG_LEN);

    private static final String PROGRESS_TAG = "upload";

    // flag for determining if an upload button had been pressed
    private static boolean uploading;

    /**
     * Private constructor
     */
    private UploadImage() {
        // empty
    }

    /**
     * Show the upload modal
     * 
     * As we can't directly upload contents from a stream we need to create a copy of the file first
     * 
     * @param context Android Context
     * @param prefs current preferences
     * @param fileUri the Uri of the image we want to upload
     */
    public static void dialog(@NonNull Context context, @NonNull Preferences prefs, @NonNull ImageAction action, @NonNull Uri fileUri) {
        try (InputStream in = context.getContentResolver().openInputStream(fileUri)) {
            String filename = ContentResolverUtil.getDisplaynameColumn(context, fileUri);
            File dest = new File(context.getCacheDir(), filename);
            FileUtil.copy(in, dest);
            dialog(context, prefs, action, dest, true);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Copy stream to file failed " + e.getMessage());
            ScreenMessage.toastTopError(context, context.getString(R.string.image_upload_failed, e.getMessage()));
        }

    }

    /**
     * Show the upload modal
     * 
     * @param context Android Context
     * @param prefs current preferences
     * @param imageFile the image file to upload
     */
    public static void dialog(@NonNull Context context, @NonNull Preferences prefs, @NonNull ImageAction action, @NonNull File imageFile) {
        dialog(context, prefs, action, imageFile, false);
    }

    /**
     * Show the upload modal
     * 
     * @param context Android Context
     * @param prefs current preferences
     * @param imageFile the image file to upload
     * @param alwaysRemove if true always remove the file
     */
    private static void dialog(@NonNull Context context, @NonNull Preferences prefs, @NonNull ImageAction action, @NonNull File imageFile,
            boolean alwaysRemove) {
        AlertDialog.Builder builder = ThemeUtils.getAlertDialogBuilder(context, prefs);
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(context)) {
            final ImageStorageConfiguration[] configurations = db.getImageStores();
            String[] names = new String[configurations.length];
            int active = 0;
            for (int i = 0; i < configurations.length; i++) {
                names[i] = configurations[i].name;
                if (configurations[i].active) {
                    active = i;
                }
            }
            builder.setSingleChoiceItems(names, active, (DialogInterface dialog, int which) -> db.setImageStoreState(configurations[which].id, true));

            builder.setPositiveButton(alwaysRemove ? R.string.image_upload : R.string.image_upload_and_remove, (DialogInterface dialog, int which) -> {
                uploading = true;
                ListView lv = ((AlertDialog) dialog).getListView();
                upload(context, prefs, true, configurations[lv.getCheckedItemPosition()], action, imageFile, alwaysRemove);
            });
            if (!alwaysRemove) {
                builder.setNegativeButton(R.string.image_upload, (DialogInterface dialog, int which) -> {
                    uploading = true;
                    ListView lv = ((AlertDialog) dialog).getListView();
                    upload(context, prefs, false, configurations[lv.getCheckedItemPosition()], action, imageFile, alwaysRemove);
                });
            }
            builder.setNeutralButton(R.string.cancel, null);
            Dialog dialog = builder.create();
            dialog.setOnDismissListener((DialogInterface d) -> {
                if (alwaysRemove && !uploading) {
                    imageFile.delete(); // NOSONAR
                }
            });
            dialog.show();
        }

    }

    /**
     * Actually upload
     * 
     * @param context Android Context
     * @param prefs current preferences
     * @param imageFile the image file to upload
     * @param alwaysRemove if true always remove the file
     */
    private static void upload(@NonNull Context context, @NonNull Preferences prefs, final boolean remove, @NonNull final ImageStorageConfiguration configuration,
            @NonNull ImageAction action, @NonNull final File imageFile, boolean alwaysRemove) {

        final ImageStorage imageStore = getImageStore(configuration);

        new ExecutorTask<Void, Void, UploadResult>() {

            @Override
            protected void onPreExecute() {
                if (context instanceof FragmentActivity) {
                    Progress.showDialog((FragmentActivity) context, Progress.PROGRESS_UPLOADING, PROGRESS_TAG);
                }
            }

            @Override
            protected UploadResult doInBackground(Void id) {
                Log.d(DEBUG_TAG, "Uploading");
                return imageStore.upload(context, imageFile);
            }

            @Override
            protected void onPostExecute(UploadResult result) {
                if (context instanceof FragmentActivity) {
                    Progress.dismissDialog((FragmentActivity) context, Progress.PROGRESS_UPLOADING, PROGRESS_TAG);
                }

                try {
                    String url = result.getUrl();
                    if (url != null && ErrorCodes.OK == result.getError()) {
                        ScreenMessage.toastTopInfo(context, context.getString(R.string.toast_upload_to_success, configuration.name));
                        switch (action.getAction()) {
                        case ADDTOELEMENT:
                            OsmElement element = App.getDelegator().getOsmElement(action.getElementType(), action.getId());
                            Map<String, String> tags = new HashMap<>(element.getTags());
                            imageStore.addTag(url, tags);
                            App.getLogic().setTags((Activity) context, element, tags);
                            break;
                        case ADDTONOTE:
                        default:
                            // nothing
                        }
                        if (remove) {
                            imageFile.delete(); // NOSONAR
                        } else {
                            TakePicture.indexLocal(context, prefs, imageFile);
                        }
                        return;
                    }
                    Log.d(DEBUG_TAG, "Upload failed " + result.toString());
                    switch (result.getError()) {
                    case ErrorCodes.FORBIDDEN:
                        ScreenMessage.toastTopError(context, R.string.image_upload_not_authorized);
                        return;
                    case ErrorCodes.UPLOAD_PROBLEM:
                        String message = result.getMessage();
                        int httpCode = result.getHttpError();
                        ScreenMessage.toastTopError(context,
                                context.getString(R.string.image_upload_failed_due_to, httpCode, (message != null ? message : ""), url));
                        return;
                    default:
                        // fall through
                    }
                    ScreenMessage.toastTopError(context, context.getString(R.string.image_upload_failed, result.getMessage()));
                } finally {
                    if (alwaysRemove) {
                        imageFile.delete(); // NOSONAR
                    }
                }
            }
        }.execute();
    }

    private static ImageStorage getImageStore(ImageStorageConfiguration configuration) {
        switch (configuration.type) {
        case PANORAMAX:
            return new PanoramaxStorage(configuration);
        case WIKIMEDIA_COMMONS:
            return new WikimediaCommonsStorage(configuration);
        default:
            // fall through
        }
        throw new IllegalArgumentException("Unknown store type " + configuration.type);
    }
}
