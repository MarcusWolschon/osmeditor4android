# Generating MBTiles files with custom imagery
_by Manfred Stock_

Current Vespucci versions [support the addition of custom imagery sources](custom_imagery.md), including [sources based on MBTiles](custom_imagery.md#mbtiles). This tutorial will show a few approaches to create your own [MBTiles](https://github.com/mapbox/mbtiles-spec) files that can then be used with Vespucci.

All approaches support either regular WMS or tile servers as imagery sources, some of them also both or local files in e.g. GeoTIFF format. The [JOSM Imagery Sources](https://josm.openstreetmap.de/wiki/Maps), which is also used by several editors including Vespucci, provides many imagery sources that can be used for OpenStreetMap. The XML files in the [Maps section of the JOSM wiki](https://josm.openstreetmap.de/wiki/Maps) usually contain all the information required to access these imagery sources.

Other services that are useful in this context are the following:

* [OSM Admin Boundaries Map](https://wambachers-osm.website/boundaries/): This allows to retrieve boundaries in various formats which can be useful to limit the extent of the resulting MBTiles file to e.g. a single town.
* [Tile Calculator](https://tools.geofabrik.de/calc/#type=geofabrik_standard&tab=1&proj=EPSG:4326&places=4): This allows you to select rectangular bounding boxes and get the coordinates, which is also useful to select a smaller area.

Since MBTiles files are just regular [SQLite](https://sqlite.org/) databases, they can be inspected and modified using e.g. the `sqlite3` command line tool and normal SQL commands like [`SELECT`](https://en.wikipedia.org/wiki/Select_(SQL)), [`INSERT`](https://en.wikipedia.org/wiki/Insert_(SQL)) and [`UPDATE`](https://en.wikipedia.org/wiki/Update_(SQL)). See the end of the [MapProxy](#mapproxy) section for a simple example of this using `INSERT` commands.

*A word of warning*: Using these tools can result in a _large_ number of requests to the services that provide the imagery, even for small areas (like smaller cities) when using the higher zoom levels (e.g. 19 and up). So please try to download only small areas, configure and/or use caching and don't run multiple instances of the tools in parallel. Otherwise, those who provide the services may block you or even consider to stop providing the public service if they for example think that it is being abused. Using smaller areas also results in smaller files so they will take up less space on your mobile phone or tablet.

## Mobile Atlas Creator

[MOBAC](https://mobac.sourceforge.io/) is a Java-based GUI application which can also generate MBTiles files. By default, it doesn't provide many pre-configured data sources (and those might not even be legal sources for OpenStreetMap), but one can add [custom map sources](https://mobac.sourceforge.io/MOBAC/README.HTM#CustomMapSource) using XML files like regular [tile](https://mobac.sourceforge.io/wiki/index.php/Custom_XML_Map_Sources#Simple_custom_map_sources) [servers](https://mobac.sourceforge.io/wiki/index.php/Custom_XML_Map_Sources#customMapSource) and [WMS](https://mobac.sourceforge.io/wiki/index.php/Custom_XML_Map_Sources#Custom_WMS_map_sources) [servers](https://mobac.sourceforge.io/wiki/index.php/Custom_XML_Map_Sources#customWmsMapSource). For example, the following XML file can be used to use the WMS of the canton of Zurich and is based on information from the [JOSM Imagery Sources](https://josm.openstreetmap.de/wiki/Maps/Switzerland#KantonZrichOrthophoto201810cm):

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<customWmsMapSource>
    <!-- Map source name as it appears in the map sources list -->
    <name>Kanton Zurich, Orthofoto ZH Sommer 2018 RGB 10cm</name>
    <!-- Available zoom levels -->
    <minZoom>8</minZoom>
    <maxZoom>21</maxZoom>
    <!-- Tile format (PNG, JPG or GIF) -->
    <tileType>PNG</tileType>
    <!-- WMS version -->
    <version>1.1.1</version>
    <!-- WMS layer parameter -->
    <layers>ortho</layers>
    <!-- WMS base URL -->
    <url>http://wms.zh.ch/OGDOrthoZH?</url>
    <!-- Currently only EPSG:4326 is supported -->
    <coordinatesystem>EPSG:4326</coordinatesystem>
    <!-- Transparent background -->
    <backgroundColor>#000000</backgroundColor>
</customWmsMapSource>
```

## GDAL

[GDAL (Geospatial Data Abstraction Library)](http://www.gdal.org/) is a library for reading and writing geospatial data formats. In addition to processing local files in various formats, it can also directly access a WMS using the [WMS raster driver](https://gdal.org/drivers/raster/wms.html). In order to do this, a WMS service description XML file is required, which can for example be [generated](https://gdal.org/drivers/raster/wms.html#generation-of-wms-service-description-xml-file) as follows:

```bash
# Fetch available subdatasets:
gdalinfo "WMS:http://wms.zh.ch/OGDOrthoZH"
# Generate description file (I left out the BBOX from the output of the
# above command and changed the SRS to EPSG:4326 though, otherwise, I got
# errors later):
gdal_translate "WMS:http://wms.zh.ch/OGDOrthoZH?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&LAYERS=ortho&SRS=EPSG:4326" wms.xml -of WMS
# Generate MBTiles file (-projwin takes coordinates as <ulx uly lrx lry> in
# the given SRS, so depending on where you got the BBOX from, you might
# have to switch positions of some of the values):
gdal_translate -of MBTILES wms.xml zurich-mainstation.mbtiles -projwin 8.5334 47.3807 8.5426 47.3763 -projwin_srs EPSG:4326 -co TILE_FORMAT=JPEG -co QUALITY=100 -co TYPE=baselayer
# Build overview images for lower zoom levels:
gdaladdo zurich-mainstation.mbtiles 2 4 8 16 32 64 128 256 512 1024
```

The [XML description file](https://gdal.org/drivers/raster/wms.html#xml-description-file) supports various options that can be set, and it is likely a good idea to configure a `Cache` and to lower the `MaxConnections` since this should reduce the load on the WMS service. The [MBTiles](https://gdal.org/drivers/raster/mbtiles.html) raster driver also supports other options in addition to the above `TILE_FORMAT=JPEG`, `QUALITY=100` and `TYPE=baselayer` which might be useful.

A local file with geodata (like a GeoTIFF file) can also be used as input by replacing the `wms.xml` parameter in the second to last command (`-projwin` and `-projwin_srs` are then usually not required, except if only a part of the file should be extracted). If the data is split into multiple local files, then they can be combined to a single [VRT](https://gdal.org/drivers/raster/vrt.html) file using [`gdalbuildvrt`](https://gdal.org/programs/gdalbuildvrt.html) which can then be used as input to `gdal_translate` (if the input files don't specify a reference system, then it should be provided using the `-a_srs` parameter of `gdalbuildvrt`).

## MapProxy

[MapProxy](http://mapproxy.org/) is an open source proxy for geospatial data. The [`export`](https://mapproxy.org/docs/nightly/mapproxy_util.html#export) subcommand of its [`mapproxy-util`](https://mapproxy.org/docs/nightly/mapproxy_util.html) command line tool also supports exports to the MBTiles format. This requires a working [MapProxy configuration](https://mapproxy.org/docs/nightly/configuration.html), but this allows you to reuse an existing MapProxy cache which will also be seeded with the additional data that is downloaded while creating a MBTiles export. A fairly minimal configuration for the WMS of the canton of Zurich may look as follows:

```yaml
---
services:
    wms:
        md:
            title: "Mapproxy"
sources:
    zh_wms_source:
        type: wms
        req:
            url: http://wms.zh.ch/OGDOrthoZH?
            layers: ortho
            transparent: true
layers:
    - name: zh_wms_layer
      title: Kanton Zurich, Orthofoto ZH Sommer 2018 RGB 10cm
      sources: [zh_wms_cache]
caches:
    zh_wms_cache:
        grids: [webmercator]
        sources: [zh_wms_source]
grids:
    webmercator:
        name: webmercator
        base: GLOBAL_WEBMERCATOR
        num_levels: 22
    mercator:
        name: mercator
        base: GLOBAL_MERCATOR
        num_levels: 22
```

Using this configuration, a command like the following can be used to generated a MBTiles file with imagery of Zurich mainstation:

```bash
mapproxy-util export -f mapproxy.yaml --grid mercator --source zh_wms_cache \
    --dest ./zurich-mainstation.mbtiles --type mbtile --levels 8..20 \
    --coverage 8.5334,47.3763,8.5426,47.3807 --srs EPSG:4326 --fetch-missing-tiles
```

The `--coverage` parameter [can also take e.g. a GeoJSON file](https://mapproxy.org/docs/nightly/coverages.html), but be sure to also specify the `--srs` parameter with a reference system that matches the `--coverage` data. Unfortunately, the `mapproxy-util export` command does not add any metadata to the MBTiles file, so this has to be done manually using e.g. the `sqlite3` command line tool:

```bash
# Open the MBTiles file:
sqlite3 zurich-mainstation.mbtiles
```
```sql
-- Add 'format', by default, this is 'png', but this depends on your cache format:
INSERT INTO metadata ('name', 'value') VALUES ('format', 'png');
-- Add 'name', used as default for the layer name by Vespucci:
INSERT INTO metadata ('name', 'value') VALUES ('name', 'Zurich Mainstation @ Kanton Zurich, Orthofoto ZH Sommer 2018 RGB 10cm');
-- Add zoom range
INSERT INTO metadata ('name', 'value') VALUES ('maxzoom', (SELECT MAX(zoom_level) FROM tiles));
INSERT INTO metadata ('name', 'value') VALUES ('minzoom', (SELECT MIN(zoom_level) FROM tiles));
```

## Other tools and resources

* [TileHuria](https://github.com/HumanitarianStuff/tilehuria) is a set of minimal utilities to download imagery and create MBtiles files.
* [gdal2mbtiles](https://github.com/ecometrica/gdal2mbtiles) can convert GDAL-readable datasets into an MBTiles file.
* [Exporting rasters to Mbtiles using GDAL](https://pvanb.wordpress.com/2017/03/06/raster2mbtiles/)
* [Creating OSM Offline tiles using Maperitive](https://osedok.wordpress.com/2015/02/25/creating-osm-offline-tiles-using-maperitive/)
* [Creating .mbtiles (hotosm/toolbox)](https://github.com/hotosm/toolbox/wiki/4.5-Creating-.mbtiles)
