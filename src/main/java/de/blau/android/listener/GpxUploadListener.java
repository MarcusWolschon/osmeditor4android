package de.blau.android.listener;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.gpx.Track;
import de.blau.android.osm.Server;
import de.blau.android.osm.Server.Visibility;

/**
 * @author mb
 * @author simon
 */
public class GpxUploadListener implements OnClickListener {

    private static final String DEBUG_TAG = "GpxUploadListener";

    private final FragmentActivity caller;
    private final Track            track;
    private final EditText         descriptionField;
    private final EditText         tagsField;
    private final Spinner          visibilitySpinner;

    /**
     * Construct a new listener from parameters
     * 
     * @param caller instance of Main that will be used
     * @param track the track to upload
     * @param descriptionField EditText holding the OSM GPX API description text
     * @param tagsField EditText holding the OSM GPX API tags
     * @param visibilitySpinner Spinner with the OSM GPX API visibility values
     */
    public GpxUploadListener(@NonNull final FragmentActivity caller, @NonNull Track track, @NonNull final EditText descriptionField,
            @NonNull final EditText tagsField, @NonNull final Spinner visibilitySpinner) {
        this.caller = caller;
        this.descriptionField = descriptionField;
        this.tagsField = tagsField;
        this.visibilitySpinner = visibilitySpinner;
        this.track = track;
    }

    /**
     * note: the current code will only work if the string array in strings.xml is not changed
     */
    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        Visibility visibility = Visibility.PRIVATE;
        switch (visibilitySpinner.getSelectedItemPosition()) {
        case 0:
            // already set to private
            break;
        case 1:
            visibility = Visibility.PUBLIC;
            break;
        case 2:
            visibility = Visibility.TRACKABLE;
            break;
        case 3:
            visibility = Visibility.IDENTIFIABLE;
            break;
        default:
            Log.e(DEBUG_TAG, "Unknown spinner value " + visibilitySpinner.getSelectedItemPosition());
        }

        final Logic logic = App.getLogic();
        final Server server = logic.getPrefs().getServer();

        if (server.isLoginSet()) {
            logic.uploadTrack(caller, track, descriptionField.getText().toString(), tagsField.getText().toString(), visibility);
            logic.checkForMail(caller, server);
        } else {
            ErrorAlert.showDialog(caller, ErrorCodes.NO_LOGIN_DATA);
        }
    }
}
