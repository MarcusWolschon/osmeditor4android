package io.vespucci.layer.photos;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.contract.MimeTypes;

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
        setFlags(myIntent);
        // black magic only works this way
        myIntent.setDataAndType(photoUri, MimeTypes.JPEG);
        context.startActivity(myIntent);
    }

    /**
     * Set required intent flags
     * 
     * @param intent the Intent
     */
    private static void setFlags(@NonNull Intent intent) {
        int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flags = flags | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;
        } else {
            flags = flags | Intent.FLAG_ACTIVITY_CLEAR_TASK;
        }
        intent.setFlags(flags);
    }

    /**
     * Share photo with optional alternative text (id etc)
     * 
     * @param context an Android Context
     * @param text alternative text
     * @param photoUri the Uri of the photo
     * @param mimeType its type
     */
    public static void sharePhoto(@NonNull Context context, @Nullable String text, @NonNull Uri photoUri, @NonNull String mimeType) {
        Intent sendIntent = new Intent(text != null ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND);
        setFlags(sendIntent);
        sendIntent.setType(mimeType);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.putExtra(Intent.EXTRA_STREAM, photoUri);
        sendIntent.setType(mimeType);
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        setFlags(viewIntent);
        viewIntent.setType(mimeType);
        viewIntent.putExtra(Intent.EXTRA_STREAM, photoUri);
        Intent chooserIntent = Intent.createChooser(sendIntent, null);
        setFlags(chooserIntent);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { viewIntent });
        context.startActivity(chooserIntent);
    }
}