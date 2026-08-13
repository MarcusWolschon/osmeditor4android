package de.blau.android.photos;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Paths;
import de.blau.android.photos.ImageAction.Action;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ScreenMessage;

public class TakePicture extends ActivityResultContract<ImageAction, Boolean> {

    private static final String UNICODE_WHITE_SPACE = "\\p{IsWhiteSpace}+";
    private static final String UNDERSCORE          = "_";
    private static final int    TAG_LEN             = Math.min(LOG_TAG_LEN, TakePicture.class.getSimpleName().length());
    private static final String DEBUG_TAG           = TakePicture.class.getSimpleName().substring(0, TAG_LEN);

    private static final String HEIC_MAGIC = "ftypheic";

    /**
     * Date pattern used for the image file name.
     */
    private static final String DATE_PATTERN_IMAGE_FILE_NAME_PART = "yyyyMMdd_HHmmss";

    private final Preferences prefs;
    private final Context     context;
    private File              imageFile;
    private ImageAction       action;

    public TakePicture(@NonNull Context context, @NonNull Preferences prefs) {
        this.prefs = prefs;
        this.context = context;
    }

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull ImageAction action) {
        this.setAction(action);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, getUri())
                .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        String cameraApp = prefs.getCameraApp();
        if (!"".equals(cameraApp)) {
            intent.setPackage(cameraApp);
        }
        return intent;
    }

    @Override
    public final SynchronousResult<Boolean> getSynchronousResult(@NonNull Context context, @NonNull ImageAction action) {
        return null;
    }

    @NonNull
    @Override
    public final Boolean parseResult(int resultCode, @Nullable Intent intent) {
        return resultCode == Activity.RESULT_OK;
    }

    /**
     * Get a new File for storing an image
     * 
     * @return a File object
     * @throws IOException if reading the file went wrong
     */
    @NonNull
    private File generateImageFile() throws IOException {
        File outDir = FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_PICTURES);
        String imageFilename = DateFormatter.getFormattedString(DATE_PATTERN_IMAGE_FILE_NAME_PART);
        String actionFilename = action.getFilename();
        if (actionFilename != null) {
            imageFilename = actionFilename.replaceAll(UNICODE_WHITE_SPACE, UNDERSCORE).toLowerCase() + UNDERSCORE + imageFilename;
        }
        // this forces the extension to jpg, however it could be a HEIC image, but we can rename once the image has been
        // taken
        return File.createTempFile(imageFilename, "." + FileExtensions.JPG, outDir);
    }

    /**
     * If an image has successfully been captured by a camera app, index the file, otherwise delete
     * 
     * @param resultCode the result code from the intent
     */
    public void processImage(final boolean result) {
        if (imageFile == null) {
            Log.e(DEBUG_TAG, "Unexpected state imageFile == null");
            return;
        }
        try {
            Log.d(DEBUG_TAG, "imageFile " + imageFile.getAbsolutePath() + " " + imageFile.length());
            // result is not consistently true so we need to test if something was written here
            if (result || imageFile.length() > 0L) {
                // check for heic and change extension if necessary
                boolean heic = checkForHeic();
                if (!heic && context instanceof FragmentActivity && action.getAction() != Action.NOTHING) {
                    UploadImage.dialog(context, prefs, getAction(), imageFile);
                    return;
                }
                indexLocal(context, prefs, imageFile);
            } else {
                Log.e(DEBUG_TAG, "image capture canceled, deleting image");
                imageFile.delete(); // NOSONAR
            }
        } catch (SecurityException e) {
            Log.e(DEBUG_TAG, "Access denied for delete to " + imageFile.getAbsolutePath());
        } catch (IOException ioex) {
            Log.e(DEBUG_TAG, "Exception accessing " + imageFile.getAbsolutePath());
        } finally {
            imageFile = null; // reset
        }
    }

    /**
     * Check if the image is actually in HEIC format
     * 
     * Side effect rename the file if it is in HEIC formant
     * 
     * @return true if in HEIC format
     * @throws IOException if accessing or renaming the file fails
     */
    private boolean checkForHeic() throws IOException {
        boolean heic = false;
        byte[] magic = new byte[8];
        try (InputStream is = new FileInputStream(imageFile)) {
            if (is.read(magic) == magic.length && HEIC_MAGIC.equals(new String(magic))) {
                File heicFile = new File(imageFile.getAbsolutePath().replace("." + FileExtensions.JPG, "." + FileExtensions.HEIC));
                if (imageFile.renameTo(heicFile)) {
                    imageFile = heicFile;
                }
                heic = true;
            }
        }
        return heic;
    }

    /**
     * Index the image for local use
     * 
     * @param context Android Context
     * @param prefs current Preferences
     * @param imageFile the image to index
     */
    static void indexLocal(@NonNull Context context, @NonNull Preferences prefs, @NonNull File imageFile) {
        try (PhotoIndex pi = new PhotoIndex(context)) {
            if (pi.addPhoto(imageFile) == null) {
                Log.e(DEBUG_TAG, "No image available");
                ScreenMessage.toastTopError(context, R.string.toast_photo_failed);
                return;
            }
        }
        if (prefs.addToMediaStore()) {
            PhotoIndex.addImageToMediaStore(context.getContentResolver(), imageFile.getAbsolutePath());
        }
        Map map = context instanceof Main ? ((Main) context).getMap() : null;
        if (map != null && map.getPhotoLayer() != null) {
            map.invalidate();
        }
    }

    /**
     * Get an appropriate Uri for the file to save to
     * 
     * @return an URI
     * @throws IOException if creating the File goes wrong
     */
    public Uri getUri() {
        try {
            imageFile = generateImageFile();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create image file");
        }
        return FileProvider.getUriForFile(context, context.getString(R.string.content_provider), imageFile);
    }

    /**
     * Set the image file from a file name
     * 
     * @param fileName
     */
    public void setImageFileName(@NonNull String fileName) {
        Log.d(DEBUG_TAG, "setting imageFIleName to " + fileName);
        imageFile = new File(fileName);
    }

    /**
     * Get the image file name
     * 
     * @return the file name or null
     */
    @Nullable
    public String getImageFileName() {
        return imageFile != null ? imageFile.getAbsolutePath() : null;
    }

    /**
     * @return the action
     */
    public ImageAction getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(ImageAction action) {
        this.action = action;
    }
}
