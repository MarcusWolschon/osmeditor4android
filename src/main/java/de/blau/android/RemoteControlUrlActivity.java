package de.blau.android;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.DateFormatter;

/**
 * Start vespucci with JOSM style remote control url
 */
public class RemoteControlUrlActivity extends UrlActivity {

    private static final String DEBUG_TAG = "RemoteControlUrlAct...";
    public static final String  RCDATA    = "de.blau.android.RemoteControlActivity";

    public static class RemoteControlUrlData implements Serializable {
        private static final long serialVersionUID = 2L;
        private boolean           load             = false;
        private BoundingBox       box;
        private String            select           = null;
        private String            changesetComment = null;
        private String            changesetSource  = null;

        /**
         * @return the box
         */
        public BoundingBox getBox() {
            return box;
        }

        /**
         * Get the string with elements to select
         * 
         * @return a String in JOSM format indicating which elements should be selected
         */
        @Nullable
        public String getSelect() {
            return select;
        }

        /**
         * Set the string indicating which elements to select
         * 
         * @param select a String in JOSM format indicating which elements should be selected
         */
        public void setSelect(@Nullable String select) {
            this.select = select;
        }

        /**
         * @param box the box to set
         */
        public void setBox(BoundingBox box) {
            this.box = box;
        }

        /**
         * @return the load
         */
        public boolean load() {
            return load;
        }

        /**
         * @param load the load to set
         */
        public void setLoad(boolean load) {
            this.load = load;
        }

        /**
         * @return the changesetComment
         */
        @Nullable
        public String getChangesetComment() {
            return changesetComment;
        }

        /**
         * @param changesetComment the changesetComment to set
         */
        private void setChangesetComment(@Nullable String changesetComment) {
            this.changesetComment = changesetComment;
        }

        /**
         * @return the changesetSource
         */
        @Nullable
        public String getChangesetSource() {
            return changesetSource;
        }

        /**
         * @param changesetSource the changesetSource to set
         */
        private void setChangesetSource(@Nullable String changesetSource) {
            this.changesetSource = changesetSource;
        }
    }

    @Override
    boolean setIntentExtras(Intent intent, Uri data) {
        try {
            // extract command
            String command = data.getPath();
            if ("josm".equals(data.getScheme())) { // extract command from scheme specific part
                command = data.getSchemeSpecificPart();
                if (command != null) {
                    int q = command.indexOf('?');
                    if (q > 0) {
                        command = command.substring(0, q);
                    }
                }
            }
            if (command != null && command.startsWith("/")) { // remove any
                command = command.substring(1);
            }
            if (command == null) {
                Log.e(DEBUG_TAG, "Null RC command");
                return false;
            }

            Log.d(DEBUG_TAG, "Command: " + command);
            Log.d(DEBUG_TAG, "Query: " + data.getQuery());
            switch (command) {
            case "zoom":
            case "load_and_zoom":
                RemoteControlUrlData rcData = new RemoteControlUrlData();
                rcData.setLoad("load_and_zoom".equals(command));
                String leftParam = data.getQueryParameter("left");
                String rightParam = data.getQueryParameter("right");
                String bottomParam = data.getQueryParameter("bottom");
                String topParam = data.getQueryParameter("top");

                if (leftParam != null && rightParam != null && bottomParam != null && topParam != null) {
                    try {
                        Double left = Double.valueOf(leftParam);
                        Double right = Double.valueOf(rightParam);
                        Double bottom = Double.valueOf(bottomParam);
                        Double top = Double.valueOf(topParam);
                        rcData.setBox(new BoundingBox(left, bottom, right, top));
                        Log.d(DEBUG_TAG, "bbox " + rcData.getBox() + " load " + rcData.load());
                    } catch (NumberFormatException e) {
                        Log.e(DEBUG_TAG, "Invalid bounding box parameter " + data.toString());
                        return false;
                    }
                }

                rcData.setChangesetComment(data.getQueryParameter("changeset_comment"));
                rcData.setChangesetSource(data.getQueryParameter("changeset_source"));

                String select = data.getQueryParameter("select");
                if (rcData.load() && select != null) {
                    rcData.setSelect(select);
                }
                intent.putExtra(RCDATA, rcData);
                return true;
            case "imagery":
                String url = data.getQueryParameter("url");
                if (url != null) {
                    Preferences prefs = new Preferences(this);
                    String title = data.getQueryParameter("title");
                    if (title == null) {
                        try {
                            title = new URL(url).getHost() + " " + DateFormatter.getFormattedString("YYYY-MM-DD HH:mm");
                        } catch (MalformedURLException e) {
                            Log.e(DEBUG_TAG, "Invalid url " + url);
                            return false;
                        }
                    }
                    List<String> ids = Arrays.asList(TileLayerServer.getIds(null, false));
                    String id = TileLayerServer.nameToId(title);
                    TileLayerServer existing = null;
                    if (ids.contains(id)) {
                        existing = TileLayerServer.get(this, id, false);
                    }
                    String type = data.getQueryParameter("type");
                    if (!TileLayerServer.TYPE_TMS.equals(type) && !TileLayerServer.TYPE_WMS.equals(type)) {
                        Log.e(DEBUG_TAG, "Unsupported type " + type);
                        return false;
                    }
                    // unused and undocumented String cookies = data.getQueryParameter("cookies");
                    int minZoom = TileLayerServer.DEFAULT_MIN_ZOOM;
                    try {
                        minZoom = Integer.parseInt(data.getQueryParameter("min_zoom"));
                    } catch (Exception e) {
                        // ignore
                    }
                    int maxZoom = TileLayerServer.DEFAULT_MAX_ZOOM;
                    try {
                        maxZoom = Integer.parseInt(data.getQueryParameter("max_zoom"));
                    } catch (Exception e) {
                        // ignore
                    }
                    final SQLiteDatabase db = new TileLayerDatabase(this).getWritableDatabase();
                    TileLayerServer.addOrUpdateCustomLayer(this, db, title, existing, -1, -1, title, new TileLayerServer.Provider(), minZoom, maxZoom, false,
                            url);
                    prefs.setBackGroundLayer(id);
                    intent.setAction(Main.ACTION_UPDATE);
                    return true;
                }
                Log.e(DEBUG_TAG, "Missing url parameter " + data.toString());
                return false;
            default:
                Log.e(DEBUG_TAG, "Unknown RC command: " + data.toString());
                return false;
            }

        } catch (Exception ex) { // avoid crashing on getting called with stuff that can't be parsed
            Log.e(DEBUG_TAG, "Exception: " + ex);
            return false;
        }
    }
}
