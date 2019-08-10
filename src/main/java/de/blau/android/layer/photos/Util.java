package de.blau.android.layer.photos;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import de.blau.android.contract.MimeTypes;

public final class Util {

    /**
     * Private constructor
     */
    private Util() {
        // do nothing
    }

    /**
     * Start an external app for viewing photos
     * 
     * @param context an Android Context
     * @param photoUri the URI for the photo
     */
    public static void startExternalPhotoViewer(@NonNull Context context, @NonNull Uri photoUri) {
        Intent myIntent = new Intent(Intent.ACTION_VIEW);
        int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flags = flags | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            flags = flags | Intent.FLAG_ACTIVITY_CLEAR_TASK;
        }
        myIntent.setFlags(flags);
        // black magic only works this way
        myIntent.setDataAndType(photoUri, MimeTypes.JPEG);
        context.startActivity(myIntent);
    }
}