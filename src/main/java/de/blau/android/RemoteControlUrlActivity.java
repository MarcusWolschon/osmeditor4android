package de.blau.android;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.Nullable;
import de.blau.android.layer.LayerType;
import de.blau.android.osm.BoundingBox;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;
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
         * Check if we should load data
         * 
         * @return true if we should load data
         */
        public boolean load() {
            return load;
        }

        /**
         * Set if we should load data
         * 
         * @param load if true, load data
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
                    String title = data.getQueryParameter("title");
                    if (title == null) {
                        try {
                            title = new URL(url).getHost() + " " + DateFormatter.getFormattedString("YYYY-MM-DD HH:mm");
                        } catch (MalformedURLException e) {
                            Log.e(DEBUG_TAG, "Invalid url " + url);
                            return false;
                        }
                    }
                    List<String> ids = Arrays.asList(TileLayerSource.getIds(null, false, null, null));
                    String id = TileLayerSource.nameToId(title);
                    TileLayerSource existing = null;
                    if (ids.contains(id)) {
                        existing = TileLayerSource.get(this, id, false);
                    }
                    String type = data.getQueryParameter("type");
                    if (!TileLayerSource.TYPE_TMS.equals(type) && !TileLayerSource.TYPE_WMS.equals(type)) {
                        Log.e(DEBUG_TAG, "Unsupported type " + type);
                        return false;
                    }
                    // unused and undocumented String cookies = data.getQueryParameter("cookies");
                    int minZoom = TileLayerSource.DEFAULT_MIN_ZOOM;
                    try { // NOSONAR
                        minZoom = Integer.parseInt(data.getQueryParameter("min_zoom"));
                    } catch (Exception e) {
                        // ignore
                    }
                    int maxZoom = TileLayerSource.DEFAULT_MAX_ZOOM;
                    try { // NOSONAR
                        maxZoom = Integer.parseInt(data.getQueryParameter("max_zoom"));
                    } catch (Exception e) {
                        // ignore
                    }
                    int tileSize = TileLayerSource.DEFAULT_TILE_SIZE;
                    try { // NOSONAR
                        tileSize = Integer.parseInt(data.getQueryParameter("tileSize"));
                    } catch (Exception e) {
                        // ignore
                    }
                    try (TileLayerDatabase tlDb = new TileLayerDatabase(this); SQLiteDatabase db = tlDb.getWritableDatabase()) {
                        TileLayerSource.addOrUpdateCustomLayer(this, db, title, existing, -1, -1, title, new TileLayerSource.Provider(), null, null, null,
                                minZoom, maxZoom, tileSize, false, url);
                    }
                    de.blau.android.layer.Util.addLayer(this, LayerType.IMAGERY, id);
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
