package de.blau.android;

import java.io.IOException;
import java.util.Collection;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.contract.Ui;
import de.blau.android.photos.Photo;
import de.blau.android.photos.PhotoIndex;
import de.blau.android.util.ContentResolverUtil;
import de.blau.android.util.Snack;

class PhotoUriHandler implements PostAsyncActionHandler {

    private static final String DEBUG_TAG = PhotoUriHandler.class.getSimpleName();

    private final Main main;
    private final Uri  uri;

    /**
     * Construct a new handler for received Uris
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
            String displayName = ContentResolverUtil.getDisplaynameColumn(main, uri);
            Photo photo = new Photo(main, uri, displayName);
            try (PhotoIndex pi = new PhotoIndex(main)) {
                // check if this is an existing indexed photo
                boolean exists = false;
                Collection<Photo> existing = pi.getPhotos(photo.getBounds());
                if (displayName != null) {
                    for (Photo p : existing) {
                        if (displayName.equals(ContentResolverUtil.getDisplaynameColumn(main, p.getRefUri(main)))) {
                            photo = p;
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) {
                    if (ContentResolverUtil.persistPermissions(main, main.getIntent().getFlags(), uri)) {
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
