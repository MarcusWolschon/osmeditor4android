package de.blau.android.resources;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class ImageryOffsetIdTest {
// @formatter:off
    List<String> inputUrls = Arrays.asList("https://tile.waymarkedtrails.org/mtb/{zoom}/{x}/{y}.png",
            "https://{switch:a,b,c}.tile-cyclosm.openstreetmap.fr/cyclosm/{zoom}/{x}/{y}.png",
            "https://tiles{switch:1,2,3,4}-4001b3692e229e3215c9b7a73e528198.skobblermaps.com/TileService/tiles/2.0/00021210101/0/{zoom}/{x}/{y}.png",
            "http://kartan.linkoping.se/wms?LAYERS=Kommun_2010_25cm&STYLES=&FORMAT=image/jpeg&TRANSPARENT=TRUE&CRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}&VERSION=1.3.0&SERVICE=WMS&REQUEST=GetMap&SERVICENAME=wms_ortofoto",
            "https://imagico.de/map/osmim_tiles.php?layer=ndvina&z={zoom}&x={x}&y={-y}",
            "https://api.mapbox.com/styles/v1/openstreetmap/ckasmteyi1tda1ipfis6wqhuq/tiles/256/{zoom}/{x}/{y}?access_token={apikey}",
            "https://basemaps.linz.govt.nz/v1/tiles/aerial/EPSG:3857/{zoom}/{x}/{y}.jpg?api=d01egend5f8dv4zcbfj6z2t7rs3",
            "https://{switch:a,b,c}.tile.thunderforest.com/outdoors/{zoom}/{x}/{y}.png?apikey={apikey}",
            "https://test.{switch:a,b,c}.tile.thunderforest.com/outdoors/{zoom}/{x}/{y}.png?apikey={apikey}",
            "https://api.mapbox.com/styles/v1/openstreetmap/ckasmteyi1tda1ipfis6wqhuq/tiles/256/{zoom}/{x}/{y}?access_token=12345",
            "https://tile.waymarkedtrails.org:8080/mtb/{zoom}/{x}/{y}.png");

    List<String> outputIds = Arrays.asList("tile.waymarkedtrails.org/mtb", 
            "tile-cyclosm.openstreetmap.fr/cyclosm",
            "tiles-4001b3692e229e3215c9b7a73e528198.skobblermaps.com/TileService/tiles/2.0/00021210101/0",
            "kartan.linkoping.se/wms?format=image/jpeg&layers=Kommun_2010_25cm&request=GetMap&service=WMS&servicename=wms_ortofoto&styles=null&transparent=TRUE&version=1.3.0",
            "imagico.de/map/osmim_tiles.php?layer=ndvina", 
            "api.mapbox.com/styles/v1/openstreetmap/ckasmteyi1tda1ipfis6wqhuq/tiles/256",
            "basemaps.linz.govt.nz/v1/tiles/aerial/EPSG:3857?api=d01egend5f8dv4zcbfj6z2t7rs3", 
            "tile.thunderforest.com/outdoors",
            "test.tile.thunderforest.com/outdoors",
            "api.mapbox.com/styles/v1/openstreetmap/ckasmteyi1tda1ipfis6wqhuq/tiles/256", 
            "tile.waymarkedtrails.org:8080/mtb");
// @formatter:on

    /**
     * Test the id based transformations
     */
    @Test
    public void idBased() {
        assertEquals(ImageryOffsetId.BING, ImageryOffsetId.generate("bInG", ""));
        assertEquals(ImageryOffsetId.MAPBOX, ImageryOffsetId.generate("MaPboX", ""));
    }

    @Test
    public void urlTransformation() {
        for (int i = 0; i < inputUrls.size(); i++) {
            assertEquals(outputIds.get(i), ImageryOffsetId.generate("", inputUrls.get(i)));
        }
    }
}
