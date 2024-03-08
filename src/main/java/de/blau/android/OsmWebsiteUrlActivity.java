package de.blau.android;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import de.blau.android.RemoteControlUrlActivity.RemoteControlUrlData;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.tasks.Note;
import de.blau.android.util.Util;

/**
 * Start vespucci with OSM website url
 * 
 * openstreetmap.org/node/nnn openstreetmap.org/way/nnn openstreetmap.org/relation/nnn openstreetmap.org/note/nnn
 */
public class OsmWebsiteUrlActivity extends UrlActivity {
    private static final String DEBUG_TAG = OsmWebsiteUrlActivity.class.getSimpleName();

    @Override
    boolean setIntentExtras(Intent intent, Uri data) {
        try {
            String path = data.getPath();
            if (Util.isEmpty(path)) {
                Log.e(DEBUG_TAG, "Empty path");
                return false;
            }
            String[] parts = path.split("/");
            if (parts.length != 3) {
                Log.e(DEBUG_TAG, "Invalid path " + path + " split in to " + parts.length);
                return false;
            }
            final String elementType = parts[1];
            final String elementId = parts[2];
            Log.d(DEBUG_TAG, "Element: " + elementType + " id: " + elementId);
            RemoteControlUrlData rcData = new RemoteControlUrlData();
            long id = Long.parseLong(elementId.split("#")[0]); // strip off any map hash
            switch (elementType) {
            case Node.NAME:
                rcData.getNodes().add(id);
                break;
            case Way.NAME:
                rcData.getWays().add(id);
                break;
            case Relation.NAME:
                rcData.getRelations().add(id);
                break;
            case Note.NOTE_ELEMENT:
                rcData.getNotes().add(id);
                break;
            default:
                Log.e(DEBUG_TAG, "Unknown type");
                return false;
            }
            intent.putExtra(RemoteControlUrlActivity.RCDATA, rcData);
            return true;
        } catch (Exception ex) { // avoid crashing on getting called with stuff that can't be parsed
            Log.e(DEBUG_TAG, "Exception: " + ex + " " + ex.getMessage());
            return false;
        }
    }
}
