package de.blau.android.imagestorage;

import java.io.File;
import java.util.Map;

import android.content.Context;
import androidx.annotation.NonNull;

public interface ImageStorage {

    /**
     * Authorize access to the store
     * 
     * Note that if this needs network access it has to run its internals in a thread
     * 
     * @param context an Android context
     * @return true if successful
     */
    public boolean authorize(@NonNull Context context);

    /**
     * Check if we are authorized
     * 
     * Note that if this needs network access it has to run its internals in a thread
     * 
     * @param context an Android context
     * @return true if authorized
     */
    public boolean checkAuthorized(@NonNull Context context);

    /**
     * Upload the contents of imageFile
     * 
     * Note assumption here is that the caller will run it in thread
     * 
     * @param context an Androd context
     * @param imageFile the file to upload
     * @return an UploadReult object with the URL or an error
     */
    @NonNull
    public UploadResult upload(@NonNull Context context, @NonNull File imageFile);

    /**
     * Add an appropriate image tag
     * 
     * @param url the url/whatever
     * @param tags the tags
     */
    public void addTag(@NonNull String url, @NonNull Map<String, String> tags);

}
