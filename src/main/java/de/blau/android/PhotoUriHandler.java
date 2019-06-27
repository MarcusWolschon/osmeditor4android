package de.blau.android;

import java.io.IOException;
import java.util.Collection;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.photos.Photo;
import de.blau.android.photos.PhotoIndex;
import de.blau.android.util.SelectFile;
import de.blau.android.util.Snack;

class PhotoUriHandler extends PostAsyncActionHandler {

    private static final String DEBUG_TAG = "PhotoIndexHandler";

    final Main main;
    final Uri  uri;

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
            Photo photo = new Photo(main, uri);
            PhotoIndex pi = new PhotoIndex(main);
            // check if this is an existing indexed photo
            boolean exists = false;
            Collection<Photo> existing = pi.getPhotos(photo.getBounds());
            String displayName = SelectFile.getDisplaynameColumn(main, uri);
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
                    main.getContentResolver().takePersistableUriPermission(uri, intentFlags);
                    pi.addPhoto(photo);
                } else { // can't persist, add just to the in memory index
                    PhotoIndex.addToIndex(photo);
                }
            }
            main.setFollowGPS(false);
            Map map = main.getMap();
            de.blau.android.layer.photos.MapOverlay layer = map.getPhotoLayer();
            if (layer != null) {
                layer.setSelected(photo);
            }
            map.setFollowGPS(false);
            App.getLogic().setZoom(map, Main.ZOOM_FOR_ZOOMTO);
            map.getViewBox().moveTo(map, photo.getLon(), photo.getLat());
            map.invalidate();
        } catch (NumberFormatException | IOException e) {
            Snack.toastTopError(main, main.getString(R.string.toast_error_accessing_photo, uri));
        }
    }

    @Override
    public void onError() {
        // Nothing
    }
};
