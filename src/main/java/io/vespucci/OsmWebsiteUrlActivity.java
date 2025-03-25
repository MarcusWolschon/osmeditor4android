package io.vespucci;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import io.vespucci.RemoteControlUrlActivity.RemoteControlUrlData;
import io.vespucci.osm.Node;
import io.vespucci.osm.Relation;
import io.vespucci.osm.Way;
import io.vespucci.tasks.Note;
import io.vespucci.util.Util;

/**
 * Start vespucci with OSM website url
 * 
 * openstreetmap.org/node/nnn openstreetmap.org/way/nnn openstreetmap.org/relation/nnn openstreetmap.org/note/nnn
 */
public class OsmWebsiteUrlActivity extends UrlActivity {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, OsmWebsiteUrlActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = OsmWebsiteUrlActivity.class.getSimpleName().substring(0, TAG_LEN);

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
