package de.blau.android;

import java.io.Serializable;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.osm.BoundingBox;

/**
 * Start vespucci with JOSM style remote control url
 */
public class RemoteControlUrlActivity extends UrlActivity {

    private static final String DEBUG_TAG = "RemoteControlUrlAct...";
    public static final String  RCDATA    = "de.blau.android.RemoteControlActivity";

    public static class RemoteControlUrlData implements Serializable {
        private static final long serialVersionUID = 1L;
        private boolean           load             = false;
        private BoundingBox       box;
        private String            select           = null;

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

            Log.d(DEBUG_TAG, "Command: " + command);
            Log.d(DEBUG_TAG, "Query: " + data.getQuery());
            boolean loadAndZoom = "load_and_zoom".equals(command);
            if (loadAndZoom || "zoom".equals(command)) {
                RemoteControlUrlData rcData = new RemoteControlUrlData();
                rcData.setLoad(loadAndZoom);
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
                        Log.d(DEBUG_TAG, "Invalid bounding box parameter", e);
                        return false;
                    }
                }
                String select = data.getQueryParameter("select");
                if (rcData.load() && select != null) {
                    rcData.setSelect(select);
                }
                intent.putExtra(RCDATA, rcData);
                return true;
            } else {
                Log.d(DEBUG_TAG, "Unknown RC command: " + command);
                return false;
            }

        } catch (Exception ex) { // avoid crashing on getting called with stuff that can't be parsed
            Log.d(DEBUG_TAG, "Exception: " + ex);
            return false;
        }
    }
}
