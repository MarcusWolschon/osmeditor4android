package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;

import android.support.annotation.NonNull;
import crosby.binary.file.BlockInputStream;
import de.blau.android.exception.UnsupportedFormatException;
import de.blau.android.services.util.MBTileProviderDataBase;
import de.blau.android.services.util.MapTile;

public class MapSplitSource {
    
    /**
     * Private contrstuctor
     */
    private MapSplitSource() {
        // avoid instantiation
    }

    /**
     * Read data for the specified BoundingBox from a tiled OSM datasource
     * 
     * Currently this assumes that the area is covered by tiles at the max zoom, it would be fairly straightforward to
     * zoom out if no data was found as we do for imagery, perhaps including skipping tiles covered by the higher zoom
     * one.
     * 
     * @param mbTiles a MBTileProviderDataBase instance
     * @param box the BoundingBox
     * @return a Storage instance containing the OSM objects
     * @throws IOException if reading the data caused issues
     */
    public static Storage readBox(@NonNull MBTileProviderDataBase mbTiles, @NonNull BoundingBox box) throws IOException {

        final double lonLeft = box.getLeft() / 1E7d;
        final double lonRight = box.getRight() / 1E7d;
        final double latTop = Math.toRadians(box.getTop() / 1E7d);
        final double latBottom = Math.toRadians(box.getBottom() / 1E7d);

        int[] minMaxZoom = mbTiles.getMinMaxZoom();
        if (minMaxZoom == null) {
            throw new UnsupportedFormatException("MapSplit sources must have min and max zoom set");
        }
        int minZoom = minMaxZoom[0];
        int maxZoom = minMaxZoom[1];

        final double n = Math.pow(2d, maxZoom);
        final int xTileLeft = (int) Math.floor(((lonLeft + 180d) / 360d) * n);
        final int xTileRight = (int) Math.floor(((lonRight + 180d) / 360d) * n);
        final int yTileTop = (int) Math.floor((1d - Math.log(Math.tan(latTop) + 1d / Math.cos(latTop)) / Math.PI) * n / 2d);
        final int yTileBottom = (int) Math.floor((1d - Math.log(Math.tan(latBottom) + 1d / Math.cos(latBottom)) / Math.PI) * n / 2d);

        final int tileNeededLeft = Math.min(xTileLeft, xTileRight);
        final int tileNeededRight = Math.max(xTileLeft, xTileRight);
        final int tileNeededTop = Math.min(yTileTop, yTileBottom);
        final int tileNeededBottom = Math.max(yTileTop, yTileBottom);

        Storage storage = new Storage();
        MapTile mapTile = new MapTile(null, maxZoom, 0, 0);
        for (int x = tileNeededLeft; x <= tileNeededRight; x++) {
            for (int y = tileNeededBottom; y >= tileNeededTop; y--) {
                mapTile.zoomLevel = maxZoom;
                mapTile.x = x;
                mapTile.y = y;
                InputStream is = mbTiles.getTileStream(mapTile);
                if (is != null) {
                    OsmPbfParser parser = new OsmPbfParser(storage, box);
                    new BlockInputStream(is, parser).process();
                } else {
                    // tile doesn't exist try ones further out
                    // assumption there will only always be one tile that
                    // covers an area
                    int skipped = 2;
                    while (mapTile.zoomLevel < minZoom) {
                        mapTile.x >>= 1;
                        mapTile.y >>= 1;
                        --mapTile.zoomLevel;
                        is = mbTiles.getTileStream(mapTile);
                        if (is != null) {
                            OsmPbfParser parser = new OsmPbfParser(storage, box);
                            new BlockInputStream(is, parser).process();
                            x += skipped;
                            y += skipped;
                            break;
                        }
                        skipped *= 2;
                    }
                }
            }
        }
        if (box != null) {
            // remove all unreferenced nodes that are not in the bounding box
            storage.removeUnreferencedNodes(box);
        }
        return storage;
    }

    /**
     * Check if a BoundingBox overlaps with the tiles in the source
     * 
     * @param mbTiles a MBTileProviderDataBase instance
     * @param box the BoundingBox
     * @return true if there is an intersect or the BoundingBoxes were not available
     */
    public static boolean intersects(@NonNull MBTileProviderDataBase mbTiles, @NonNull BoundingBox box) {
        BoundingBox mbTilesBounds = mbTiles.getBounds();
        return mbTilesBounds == null || mbTilesBounds.intersects(box);
    }
}
