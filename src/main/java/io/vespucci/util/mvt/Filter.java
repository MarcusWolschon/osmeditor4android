package io.vespucci.util.mvt;

import java.util.Set;

import androidx.annotation.NonNull;

/**
 * A filter which can be passed to a VectorTile decoder to optimize performance by only decoding layers of interest.
 */
public abstract class Filter {

    /**
     * Test if a layer should be included
     * 
     * @param layerName the layer name
     * @return true if the layer should be included
     */
    public abstract boolean include(@NonNull String layerName);

    public static final Filter ALL = new Filter() {

        @Override
        public boolean include(String layerName) {
            return true;
        }
    };

    /**
     * A filter that only lets a single named layer be decoded.
     */
     public static final class Single extends Filter {

        private final String layerName;

        /**
         * Construct a filter for a single layer
         * 
         * @param layerName the layer name
         */
        public Single(@NonNull String layerName) {
            this.layerName = layerName;
        }

        @Override
        public boolean include(String layerName) {
            return this.layerName.equals(layerName);
        }
    }

    /**
     * A filter that only allows the named layers to be decoded.
     */
    public static final class Any extends Filter {

        private final Set<String> layerNames;

        /**
         * Construct a filter for a set of layers
         * 
         * @param layerNames a Set holding the layer names
         */
        public Any(@NonNull Set<String> layerNames) {
            this.layerNames = layerNames;
        }

        @Override
        public boolean include(String layerName) {
            return this.layerNames.contains(layerName);
        }
    }
}
