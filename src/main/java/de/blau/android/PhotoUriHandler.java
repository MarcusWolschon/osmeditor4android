package de.blau.android;

import java.io.IOException;
import java.util.Collection;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.contract.Ui;
import de.blau.android.photos.Photo;
import de.blau.android.photos.PhotoIndex;
import de.blau.android.util.SelectFile;
import de.blau.android.util.Snack;

class PhotoUriHandler implements PostAsyncActionHandler {

    private static final String DEBUG_TAG = "PhotoUriHandler";

    private final Main main;
    private final Uri  uri;

    /**
     * Construct a new handler
     * 
     * @param main the current Main instance
     * @param uri the Uri to an image to process
     */
    PhotoUriHandler(@NonNull Main main, @NonNull Uri uri) {
        this.main = main;
        this.uri = uri;
    }

    @Override
    public void onSuccess() {
        try {
            String displayName = SelectFile.getDisplaynameColumn(main, uri);
            Photo photo = new Photo(main, uri, displayName);
            try (PhotoIndex pi = new PhotoIndex(main)) {
                // check if this is an existing indexed photo
                boolean exists = false;
                Collection<Photo> existing = pi.getPhotos(photo.getBounds());
                if (displayName != null) {
                    for (Photo p : existing) {
                        if (displayName.equals(SelectFile.getDisplaynameColumn(main, p.getRefUri(main)))) {
                            photo = p;
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) {
                    int intentFlags = main.getIntent().getFlags();
                    if ((intentFlags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0) {
                        Log.d(DEBUG_TAG, "Persisting permissions for " + uri);
                        main.getContentResolver().takePersistableUriPermission(uri,
                                intentFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                        pi.addPhoto(photo);
                    } else { // can't persist, add just to the in memory index
                        PhotoIndex.addToIndex(photo);
                    }
                }
            }
            main.setFollowGPS(false);
            Map map = main.getMap();
            de.blau.android.layer.photos.MapOverlay layer = map.getPhotoLayer();
            if (layer != null) {
                layer.setSelected(photo);
            }
            map.setFollowGPS(false);
            App.getLogic().setZoom(map, Ui.ZOOM_FOR_ZOOMTO);
            map.getViewBox().moveTo(map, photo.getLon(), photo.getLat());
            map.invalidate();
        } catch (NumberFormatException | IOException e) {
            Log.e(DEBUG_TAG, e.getMessage());
            Snack.toastTopError(main, main.getString(R.string.toast_error_accessing_photo, uri));
        }
    }
}
