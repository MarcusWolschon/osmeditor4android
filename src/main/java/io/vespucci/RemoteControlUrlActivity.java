package io.vespucci;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.contract.Schemes;
import io.vespucci.layer.LayerType;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Node;
import io.vespucci.osm.Relation;
import io.vespucci.osm.Way;
import io.vespucci.resources.TileLayerDatabase;
import io.vespucci.resources.TileLayerSource;
import io.vespucci.util.DateFormatter;
import io.vespucci.util.Util;

/**
 * Start vespucci with JOSM style remote control url
 */
public class RemoteControlUrlActivity extends UrlActivity {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, RemoteControlUrlActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = RemoteControlUrlActivity.class.getSimpleName().substring(0, TAG_LEN);

    public static final String RCDATA = "io.vespucci.RemoteControlActivity";

    private static final String IMAGERY_COMMAND             = "imagery";
    public static final String  LOAD_AND_ZOOM_COMMAND       = "load_and_zoom";
    public static final String  LOAD_OBJECTS_COMMAND        = "load_object";
    private static final String ZOOM_COMMAND                = "zoom";
    private static final String TILE_SIZE_PARAMETER         = "tileSize";
    private static final String MAX_ZOOM_PARAMETER          = "max_zoom";
    private static final String MIN_ZOOM_PARAMETER          = "min_zoom";
    private static final String TYPE_PARAMETER              = "type";
    private static final String TITLE_PARAMETER             = "title";
    private static final String URL_PARAMETER               = "url";
    public static final String  SELECT_PARAMETER            = "select";
    private static final String OBJECTS_PARAMETER           = "objects";
    private static final String RELATION_MEMBERS_PARAMETER  = "relation_members";
    private static final String REFERRERS_PARAMETER         = "referrers";
    private static final String CHANGESET_SOURCE_PARAMETER  = "changeset_source";
    private static final String CHANGESET_COMMENT_PARAMETER = "changeset_comment";
    public static final String  TOP_PARAMETER               = "top";
    public static final String  BOTTOM_PARAMETER            = "bottom";
    public static final String  RIGHT_PARAMETER             = "right";
    public static final String  LEFT_PARAMETER              = "left";

    public static class RemoteControlUrlData implements Serializable {
        private static final long serialVersionUID = 3L;
        private boolean           load             = false;
        private boolean           select           = false;
        private boolean           relationMembers  = false;
        private boolean           referrers        = false;
        private BoundingBox       box;
        private List<Long>        nodes            = new ArrayList<>();
        private List<Long>        ways             = new ArrayList<>();
        private List<Long>        relations        = new ArrayList<>();
        private List<Long>        notes            = new ArrayList<>();
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
        public boolean select() {
            return select;
        }

        /**
         * Set the string indicating which elements to select
         * 
         * @param select a String in JOSM format indicating which elements should be selected
         */
        public void setSelect(boolean select) {
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

        /**
         * @return the referrers
         */
        public boolean referrers() {
            return referrers;
        }

        /**
         * @param referrers the referrers to set
         */
        public void setReferrers(boolean referrers) {
            this.referrers = referrers;
        }

        /**
         * @return the relationMembers
         */
        public boolean relationMembers() {
            return relationMembers;
        }

        /**
         * @param relationMembers the relationMembers to set
         */
        public void setRelationMembers(boolean relationMembers) {
            this.relationMembers = relationMembers;
        }

        /**
         * @return the nodes
         */
        public List<Long> getNodes() {
            return nodes;
        }

        /**
         * @return the ways
         */
        public List<Long> getWays() {
            return ways;
        }

        /**
         * @return the relations
         */
        public List<Long> getRelations() {
            return relations;
        }

        /**
         * @return the notes
         */
        public List<Long> getNotes() {
            return notes;
        }

        /**
         * Do we have objects
         * 
         * @return true if we have object ids
         */
        public boolean hasObjects() {
            return !(nodes.isEmpty() && ways.isEmpty() && relations.isEmpty() && notes.isEmpty());
        }
    }

    @Override
    boolean setIntentExtras(Intent intent, Uri data) {
        try {
            // extract command
            String command = data.getPath();
            if (Schemes.JOSM.equals(data.getScheme())) { // extract command from scheme specific part
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
            case ZOOM_COMMAND:
            case LOAD_AND_ZOOM_COMMAND:
                RemoteControlUrlData rcData = new RemoteControlUrlData();
                rcData.setLoad(LOAD_AND_ZOOM_COMMAND.equals(command));
                String leftParam = data.getQueryParameter(LEFT_PARAMETER);
                String rightParam = data.getQueryParameter(RIGHT_PARAMETER);
                String bottomParam = data.getQueryParameter(BOTTOM_PARAMETER);
                String topParam = data.getQueryParameter(TOP_PARAMETER);

                if (leftParam != null && rightParam != null && bottomParam != null && topParam != null) {
                    Double left = Double.valueOf(leftParam);
                    Double right = Double.valueOf(rightParam);
                    Double bottom = Double.valueOf(bottomParam);
                    Double top = Double.valueOf(topParam);
                    rcData.setBox(new BoundingBox(left, bottom, right, top));
                    Log.d(DEBUG_TAG, "bbox " + rcData.getBox() + " load " + rcData.load());
                }

                rcData.setChangesetComment(data.getQueryParameter(CHANGESET_COMMENT_PARAMETER));
                rcData.setChangesetSource(data.getQueryParameter(CHANGESET_SOURCE_PARAMETER));

                String select = data.getQueryParameter(SELECT_PARAMETER);
                if (rcData.load() && select != null) {
                    rcData.setSelect(true);
                    parseSelectString(rcData, select);
                }
                intent.putExtra(RCDATA, rcData);
                return true;
            case LOAD_OBJECTS_COMMAND:
                rcData = new RemoteControlUrlData();
                String objects = data.getQueryParameter(OBJECTS_PARAMETER);
                if (objects == null) {
                    Log.e(DEBUG_TAG, "load_object without object list");
                    return false;
                }
                parseLoadString(rcData, objects);
                rcData.setReferrers(data.getBooleanQueryParameter(REFERRERS_PARAMETER, false));
                rcData.setRelationMembers(data.getBooleanQueryParameter(RELATION_MEMBERS_PARAMETER, false));
                intent.putExtra(RCDATA, rcData);
                return true;
            case IMAGERY_COMMAND:
                String url = data.getQueryParameter(URL_PARAMETER);
                if (url == null) {
                    Log.e(DEBUG_TAG, "Missing url parameter " + data.toString());
                    return false;
                }
                String title = data.getQueryParameter(TITLE_PARAMETER);
                if (title == null) {
                    title = new URL(url).getHost() + " " + DateFormatter.getFormattedString("YYYY-MM-DD HH:mm");
                }
                List<String> ids = Arrays.asList(TileLayerSource.getIds(null, false, null, null));
                String id = TileLayerSource.nameToId(title);
                TileLayerSource existing = null;
                if (ids.contains(id)) {
                    existing = TileLayerSource.get(this, id, false);
                }
                String type = data.getQueryParameter(TYPE_PARAMETER);
                if (!TileLayerSource.TYPE_TMS.equals(type) && !TileLayerSource.TYPE_WMS.equals(type)) {
                    Log.e(DEBUG_TAG, "Unsupported type " + type);
                    return false;
                }
                // unused and undocumented String cookies = data.getQueryParameter("cookies"); NOSONAR
                int minZoom = TileLayerSource.DEFAULT_MIN_ZOOM;
                try { // NOSONAR
                    minZoom = Integer.parseInt(data.getQueryParameter(MIN_ZOOM_PARAMETER));
                } catch (Exception e) {
                    // ignore
                }
                int maxZoom = TileLayerSource.DEFAULT_MAX_ZOOM;
                try { // NOSONAR
                    maxZoom = Integer.parseInt(data.getQueryParameter(MAX_ZOOM_PARAMETER));
                } catch (Exception e) {
                    // ignore
                }
                int tileSize = TileLayerSource.DEFAULT_TILE_SIZE;
                try { // NOSONAR
                    tileSize = Integer.parseInt(data.getQueryParameter(TILE_SIZE_PARAMETER));
                } catch (Exception e) {
                    // ignore
                }
                try (TileLayerDatabase tlDb = new TileLayerDatabase(this); SQLiteDatabase db = tlDb.getWritableDatabase()) {
                    TileLayerSource.addOrUpdateCustomLayer(this, db, title, existing, -1, -1, title, new TileLayerSource.Provider(), null, null, null, minZoom,
                            maxZoom, tileSize, false, url);
                }
                io.vespucci.layer.Util.addLayer(this, LayerType.IMAGERY, id);
                intent.setAction(Main.ACTION_UPDATE);
                return true;
            default:
                Log.e(DEBUG_TAG, "Unknown RC command: " + data.toString());
                return false;
            }
        } catch (Exception ex) { // avoid crashing on getting called with stuff that can't be parsed
            Log.e(DEBUG_TAG, "Exception: " + ex + " " + ex.getMessage());
            return false;
        }
    }

    /**
     * Parse a select parameter value in to a RemoteControlUrlData object, long version
     * 
     * @param rcData RemoteControlUrlData object
     * @param selectString the select parameter
     */
    private void parseSelectString(@NonNull RemoteControlUrlData rcData, @NonNull String selectString) {
        for (String s : selectString.split(",")) {
            // see http://wiki.openstreetmap.org/wiki/JOSM/Plugins/RemoteControl
            if (Util.isEmpty(s)) {
                continue;
            }
            Log.d(DEBUG_TAG, "rc select: " + s);
            try {
                if (s.startsWith(Node.NAME)) {
                    rcData.getNodes().add(Long.parseLong(s.substring(Node.NAME.length())));
                } else if (s.startsWith(Way.NAME)) {
                    rcData.getWays().add(Long.parseLong(s.substring(Way.NAME.length())));
                } else if (s.startsWith(Relation.NAME)) {
                    rcData.getRelations().add(Long.parseLong(s.substring(Relation.NAME.length())));
                }
            } catch (NumberFormatException nfe) {
                Log.d(DEBUG_TAG, "Parsing " + s + " caused " + nfe);
                // not much more we can do here
            }
        }
    }

    /**
     * Parse an objects parameter value in to a RemoteControlUrlData object, short version
     * 
     * @param rcData RemoteControlUrlData object
     * @param objectsString the objects parameter
     */
    static void parseLoadString(@NonNull RemoteControlUrlData rcData, @NonNull String objectsString) {
        for (String s : objectsString.split(",")) {
            // see http://wiki.openstreetmap.org/wiki/JOSM/Plugins/RemoteControl
            if (Util.isEmpty(s) || s.length() < 2) {
                continue;
            }
            Log.d(DEBUG_TAG, "rc objects: " + s);
            try {
                long id = Long.parseLong(s.substring(1));
                switch (s.substring(0, 1)) {
                case Node.ABBREV:
                    rcData.getNodes().add(id);
                    break;
                case Way.ABBREV:
                    rcData.getWays().add(id);
                    break;
                case Relation.ABBREV:
                    rcData.getRelations().add(id);
                    break;
                default:
                    Log.e(DEBUG_TAG, "Unknown element letter");
                }
            } catch (NumberFormatException nfe) {
                Log.d(DEBUG_TAG, "Parsing " + s + " caused " + nfe);
                // not much more we can do here
            }
        }
    }
}
