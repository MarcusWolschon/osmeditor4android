package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;

import android.support.annotation.NonNull;
import crosby.binary.file.BlockInputStream;
import de.blau.android.services.util.MBTileProviderDataBase;
import de.blau.android.services.util.MapTile;

public class MapSplitSource {

    /**
     * Read data for the specified BoundingBox from a tiled OSM datasource
     * 
     * @param mbTiles a MBTileProviderDataBase instance
     * @param box the BoundingBox
     * @return a Storage instance containing the OSM objects
     * @throws IOException if reading the data caused issues
     */
    public static Storage readBox(@NonNull MBTileProviderDataBase mbTiles, BoundingBox box) throws IOException {

        final double lonLeft = box.getLeft() / 1E7d;
        final double lonRight = box.getRight() / 1E7d;
        final double latTop = Math.toRadians(box.getTop() / 1E7d);
        final double latBottom = Math.toRadians(box.getBottom() / 1E7d);

        int[] minMaxZoom = mbTiles.getMinMaxZoom();

        final double n = Math.pow(2d, minMaxZoom[0]); // currently this should be a constant
        final int xTileLeft = (int) Math.floor(((lonLeft + 180d) / 360d) * n);
        final int xTileRight = (int) Math.floor(((lonRight + 180d) / 360d) * n);
        final int yTileTop = (int) Math.floor((1d - Math.log(Math.tan(latTop) + 1d / Math.cos(latTop)) / Math.PI) * n / 2d);
        final int yTileBottom = (int) Math.floor((1d - Math.log(Math.tan(latBottom) + 1d / Math.cos(latBottom)) / Math.PI) * n / 2d);

        final int tileNeededLeft = Math.min(xTileLeft, xTileRight);
        final int tileNeededRight = Math.max(xTileLeft, xTileRight);
        final int tileNeededTop = Math.min(yTileTop, yTileBottom);
        final int tileNeededBottom = Math.max(yTileTop, yTileBottom);

        Storage storage = new Storage();
        MapTile mapTile = new MapTile(null, minMaxZoom[0], 0, 0);
        for (int x = tileNeededLeft; x <= tileNeededRight; x++) {
            for (int y = tileNeededBottom; y >= tileNeededTop; y--) {
                mapTile.x = x;
                mapTile.y = y;
                InputStream is = mbTiles.getTileStream(mapTile);
                if (is != null) {
                    OsmPbfParser parser = new OsmPbfParser(storage, box);
                    new BlockInputStream(is, parser).process();
                }
            }
        }
        return storage;
    }
}
